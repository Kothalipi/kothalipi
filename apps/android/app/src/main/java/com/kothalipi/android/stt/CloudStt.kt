package com.kothalipi.android.stt

import com.kothalipi.android.data.Provider
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

enum class Failure { BAD_KEY, RATE_LIMITED, SERVER, NETWORK, BAD_RESPONSE }

sealed class Outcome {
    data class Ok(val text: String, val provider: Provider) : Outcome()
    data class Failed(val reasons: List<Pair<Provider, Failure>>) : Outcome()
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
        return buildString { for (i in 0 until parts.length()) append(parts.getJSONObject(i).optString("text")) }
    }

    fun classify(status: Int): Failure = when {
        status == 401 || status == 403 -> Failure.BAD_KEY
        status == 429 -> Failure.RATE_LIMITED
        status >= 500 -> Failure.SERVER
        else -> Failure.BAD_RESPONSE
    }

    /** Tries each engine in order; the first transcript wins. */
    fun transcribe(wav: ByteArray, lang: String, engines: List<Pair<Provider, String>>): Outcome {
        val reasons = mutableListOf<Pair<Provider, Failure>>()
        for ((provider, key) in engines) {
            try {
                val text = when (provider) {
                    Provider.GEMINI -> gemini(key, wav, lang)
                    Provider.OPENAI -> openai(key, wav, lang)
                }
                if (text.isNotBlank()) return Outcome.Ok(text.trim(), provider)
                reasons += provider to Failure.BAD_RESPONSE
            } catch (e: HttpFailure) {
                reasons += provider to e.kind
            } catch (_: Exception) {
                reasons += provider to Failure.NETWORK
            }
        }
        return Outcome.Failed(reasons)
    }

    private class HttpFailure(val kind: Failure) : Exception()

    private fun gemini(key: String, wav: ByteArray, lang: String): String {
        val body = geminiBody(wav, lang).toString().toByteArray()
        val (status, resp) = post(
            "https://generativelanguage.googleapis.com/v1beta/models/$GEMINI_MODEL:generateContent",
            mapOf("x-goog-api-key" to key, "Content-Type" to "application/json"), body)
        if (status !in 200..299) throw HttpFailure(classify(status))
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
        if (status !in 200..299) throw HttpFailure(classify(status))
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
