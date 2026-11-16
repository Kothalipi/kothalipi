package com.kothalipi.android

import com.kothalipi.android.stt.CloudStt
import com.kothalipi.android.stt.Failure
import com.kothalipi.android.stt.pcmToWav
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSttTest {
    @Test fun geminiRequestCarriesLanguageHintAndAudio() {
        val b = CloudStt.geminiBody(byteArrayOf(1, 2, 3), "bn")
        assertEquals("bn-BD", b.getJSONObject("generationConfig").getJSONObject("audioTranscriptionConfig")
            .getJSONArray("languageCodes").getString(0))
        val data = b.getJSONArray("contents").getJSONObject(0).getJSONArray("parts").getJSONObject(0)
            .getJSONObject("inlineData")
        assertEquals("audio/wav", data.getString("mimeType"))
        assertEquals("AQID", data.getString("data"))
    }

    @Test fun geminiTextJoinsParts() {
        val j = JSONObject("""{"candidates":[{"content":{"parts":[{"text":"আমি "},{"text":"ভাত খাই।"}]}}]}""")
        assertEquals("আমি ভাত খাই।", CloudStt.geminiText(j))
        assertEquals(null, CloudStt.geminiText(JSONObject("{}")))
    }

    @Test fun geminiTranscribeModelReplyIsRead() {
        val j = JSONObject("""{"candidates":[{"content":{"parts":[{"audioTranscription":{"text":"Hello hello."}}],"role":"model"}}]}""")
        assertEquals("Hello hello.", CloudStt.geminiText(j))
    }

    @Test fun geminiDailyLimitIsDecoded() {
        val body = """{"error":{"code":429,"status":"RESOURCE_EXHAUSTED","details":[
            {"violations":[{"quotaId":"GenerateRequestsPerDayPerProjectPerModel-FreeTier","quotaValue":"20"}]},
            {"retryDelay":"46s"}]}}"""
        val (msg, scope) = CloudStt.limitMessage(com.kothalipi.android.data.Provider.GEMINI, body)
        assertEquals("day", scope)
        assertTrue(msg, msg.contains("daily limit") && msg.contains("20"))
    }

    @Test fun openAiNoCreditIsDecoded() {
        val (msg, scope) = CloudStt.limitMessage(com.kothalipi.android.data.Provider.OPENAI, """{"error":{"code":"insufficient_quota"}}""")
        assertEquals("credit", scope)
        assertTrue(msg.contains("no credit"))
    }

    @Test fun statusCodesMapToFailures() {
        assertEquals(Failure.BAD_KEY, CloudStt.classify(401))
        assertEquals(Failure.BAD_KEY, CloudStt.classify(403))
        assertEquals(Failure.RATE_LIMITED, CloudStt.classify(429))
        assertEquals(Failure.SERVER, CloudStt.classify(503))
        assertEquals(Failure.BAD_RESPONSE, CloudStt.classify(400))
    }

    @Test fun openaiMultipartHasModelFileAndLanguage() {
        val body = String(CloudStt.openaiBody(byteArrayOf(9), "bn", "BND"), Charsets.ISO_8859_1)
        assertTrue(body.contains("name=\"model\"\r\n\r\ngpt-transcribe"))
        assertTrue(body.contains("name=\"languages[]\"\r\n\r\nbn"))
        assertTrue(body.contains("filename=\"audio.wav\""))
        assertTrue(body.endsWith("--BND--\r\n"))
    }

    @Test fun wavHeaderIsValid() {
        val w = pcmToWav(ByteArray(3200))
        assertEquals("RIFF", String(w, 0, 4))
        assertEquals("WAVE", String(w, 8, 4))
        assertEquals(3244, w.size)
        assertNotNull(w)
    }
}
