package com.kothalipi.android.stt

import com.kothalipi.android.data.Provider
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class Failure { BAD_KEY, RATE_LIMITED, SERVER, NETWORK, BAD_RESPONSE }

/** Why one engine failed, with a sentence for the user. */
data class Reason(val provider: Provider, val kind: Failure, val message: String, val scope: String? = null)

sealed class Outcome {
    data class Ok(val text: String, val provider: Provider) : Outcome()
    data class Failed(val reasons: List<Reason>) : Outcome() {
        val summary: String get() = reasons.joinToString(" ") { it.message }
    }
}

/** Gemini and OpenAI speech-to-text with the user's own key. Mirrors the desktop app. */
object CloudStt {
    const val GEMINI_MODEL = "gemini-3.5-transcribe"
    const val OPENAI_MODEL = "gpt-transcribe"
    private const val TIMEOUT_MS = 45_000

    fun geminiLanguage(lang: String): String? = when (lang) { "bn" -> "bn-BD"; "en" -> "en-US"; else -> null }

    fun geminiBody(wav: ByteArray, lang: String): JSONObject {
        val audio = JSONObject().put("inlineData", JSONObject()
            .put("mimeType", "audio/wav")
            .put("data", java.util.Base64.getEncoder().encodeToString(wav)))
        val codes = JSONArray().also { a -> geminiLanguage(lang)?.let { a.put(it) } }
        return JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(audio))))
            .put("generationConfig", JSONObject().put("audioTranscriptionConfig", JSONObject().put("languageCodes", codes)))
    }

    fun geminiText(json: JSONObject): String? {
        val parts = json.optJSONArray("candidates")?.optJSONObject(0)
            ?.optJSONObject("content")?.optJSONArray("parts") ?: return null
        // gemini-3.5-transcribe replies with parts[].audioTranscription.text;
        // general Gemini models use parts[].text.
        return buildString {
            for (i in 0 until parts.length()) {
                val p = parts.getJSONObject(i)
                append(p.optJSONObject("audioTranscription")?.optString("text") ?: p.optString("text"))
            }
        }
    }

    fun classify(status: Int): Failure = when {
        status == 401 || status == 403 -> Failure.BAD_KEY
        status == 429 -> Failure.RATE_LIMITED
        status >= 500 -> Failure.SERVER
        else -> Failure.BAD_RESPONSE
    }

    /** Tries each engine in order; the first transcript wins. */
    fun transcribe(wav: ByteArray, lang: String, engines: List<Pair<Provider, String>>): Outcome {
        val reasons = mutableListOf<Reason>()
        for ((provider, key) in engines) {
            try {
                val text = when (provider) {
                    Provider.GEMINI -> gemini(key, wav, lang)
                    Provider.OPENAI -> openai(key, wav, lang)
                }
                if (text.isNotBlank()) return Outcome.Ok(text.trim(), provider)
                reasons += Reason(provider, Failure.BAD_RESPONSE, "${provider.label} returned no text.")
            } catch (e: HttpFailure) {
                reasons += Reason(provider, e.kind, e.message2 ?: describe(provider, e.kind), e.scope)
            } catch (_: Exception) {
                reasons += Reason(provider, Failure.NETWORK, describe(provider, Failure.NETWORK))
            }
        }
        return Outcome.Failed(reasons)
    }

    private class HttpFailure(val kind: Failure, val message2: String? = null, val scope: String? = null) : Exception()

    fun describe(p: Provider, kind: Failure): String = when (kind) {
        Failure.BAD_KEY -> "${p.label}: the API key was rejected."
        Failure.RATE_LIMITED -> "${p.label}: the usage limit was reached."
        Failure.SERVER -> "${p.label} had a server error."
        Failure.NETWORK -> "Couldn't reach ${p.label}. Check the internet."
        Failure.BAD_RESPONSE -> "${p.label} sent an unexpected reply."
    }

    /** Google's 429 names the quota and the wait; OpenAI's names `insufficient_quota` or a per-minute limit. */
    fun limitMessage(p: Provider, body: String): Pair<String, String> {
        val json = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        var scope = "other"; var limit: String? = null; var retry: Int? = null
        if (p == Provider.GEMINI) {
            val details = json.optJSONObject("error")?.optJSONArray("details")
            for (i in 0 until (details?.length() ?: 0)) {
                val d = details!!.getJSONObject(i)
                d.optJSONArray("violations")?.optJSONObject(0)?.let { v ->
                    val id = v.optString("quotaId")
                    scope = if (id.contains("PerDay")) "day" else if (id.contains("PerMinute")) "minute" else "other"
                    limit = v.optString("quotaValue").ifBlank { null }
                }
                d.optString("retryDelay").takeIf { it.isNotBlank() }?.trimEnd('s')?.toDoubleOrNull()?.let { retry = Math.ceil(it).toInt() }
            }
        } else {
            val err = json.optJSONObject("error")
            val code = err?.optString("code").orEmpty()
            scope = if (code == "insufficient_quota") "credit" else if (code.contains("rate_limit")) "minute" else "other"
            Regex("try again in ([0-9.]+)(m?)s?").find(err?.optString("message").orEmpty())?.let {
                val n = it.groupValues[1].toDoubleOrNull() ?: 0.0
                retry = Math.ceil(if (it.groupValues[2] == "m") n * 60 else n).toInt()
            }
        }
        val wait = retry?.let { " Try again in $it seconds." } ?: ""
        val msg = when (scope) {
            "credit" -> "${p.label} has no credit left on this key. Add credit, or use another engine."
            "day" -> "${p.label}'s daily limit is used up${limit?.let { " ($it requests a day)" } ?: ""}. Google resets it at midnight Pacific Time."
            "minute" -> "${p.label}'s per-minute limit was hit${limit?.let { " ($it a minute)" } ?: ""}.$wait"
            else -> "${p.label}'s usage limit was reached.$wait"
        }
        return msg to scope
    }

    private fun failure(p: Provider, status: Int, body: String): HttpFailure {
        val kind = if (body.contains("API_KEY_INVALID") || body.contains("API key not valid")) Failure.BAD_KEY
        else if (body.contains("RESOURCE_EXHAUSTED") || body.contains("insufficient_quota")) Failure.RATE_LIMITED
        else classify(status)
        return if (kind == Failure.RATE_LIMITED) limitMessage(p, body).let { HttpFailure(kind, it.first, it.second) } else HttpFailure(kind)
    }

    private fun gemini(key: String, wav: ByteArray, lang: String): String {
        val body = geminiBody(wav, lang).toString().toByteArray()
        val (status, resp) = post(
            "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent",
            mapOf("x-goog-api-key" to key, "Content-Type" to "application/json"), body)
        if (status !in 200..299) throw failure(Provider.GEMINI, status, resp)
        return geminiText(JSONObject(resp)) ?: throw HttpFailure(Failure.BAD_RESPONSE)
    }

    fun openaiBody(wav: ByteArray, lang: String, boundary: String): ByteArray {
        val out = ByteArrayOutputStream()
        fun field(name: String, value: String) {
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray())
        }
        out.write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\nContent-Type: audio/wav\r\n\r\n".toByteArray())
        out.write(wav)
        out.write("\r\n".toByteArray())
        field("model", OPENAI_MODEL)
        if (lang == "bn" || lang == "en") field("languages[]", lang)
        out.write("--$boundary--\r\n".toByteArray())
        return out.toByteArray()
    }

    private fun openai(key: String, wav: ByteArray, lang: String): String {
        val boundary = "kothalipi" + UUID.randomUUID().toString().replace("-", "")
        val (status, resp) = post(
            "https://api.openai.com/v1/audio/transcriptions",
            mapOf("Authorization" to "Bearer $key", "Content-Type" to "multipart/form-data; boundary=$boundary"),
            openaiBody(wav, lang, boundary))
        if (status !in 200..299) throw failure(Provider.OPENAI, status, resp)
        return JSONObject(resp).optString("text")
    }

    /** Cheap key check for the setup screen. Returns null when the key works. */
    fun checkKey(provider: Provider, key: String): Failure? = try {
        val url = if (provider == Provider.GEMINI) "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1" else "https://api.openai.com/v1/models"
        val headers = if (provider == Provider.GEMINI) mapOf("x-goog-api-key" to key) else mapOf("Authorization" to "Bearer $key")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15_000; c.readTimeout = 15_000
        headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
        val s = c.responseCode
        c.disconnect()
        if (s in 200..299) null else classify(s)
    } catch (_: Exception) {
        Failure.NETWORK
    }

    private fun post(url: String, headers: Map<String, String>, body: ByteArray): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.connectTimeout = TIMEOUT_MS; c.readTimeout = TIMEOUT_MS
            c.doOutput = true
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            c.setFixedLengthStreamingMode(body.size)
            c.outputStream.use { it.write(body) }
            val status = c.responseCode
            val stream = if (status in 200..299) c.inputStream else c.errorStream
            return status to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            c.disconnect()
        }
    }
}
