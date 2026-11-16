package com.kothalipi.android.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/** Records 16 kHz mono PCM from the microphone. */
class Recorder(private val onLevel: (Float) -> Unit) {
    @Volatile private var running = false
    private var thread: Thread? = null
    private val out = ByteArrayOutputStream()

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        val min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return false
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 2)
        } catch (_: Exception) {
            return false
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }
        out.reset()
        running = true
        rec.startRecording()
        thread = Thread {
            val buf = ByteArray(min)
            val maxBytes = SAMPLE_RATE * 2 * MAX_SECONDS
            while (running && out.size() < maxBytes) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) break
                out.write(buf, 0, n)
                onLevel(rms(buf, n))
            }
            try { rec.stop() } catch (_: Exception) {}
            rec.release()
        }.also { it.start() }
        return true
    }

    /** Stops and returns the PCM captured so far. */
    fun stop(): ByteArray {
        running = false
        thread?.join(1500)
        thread = null
        return out.toByteArray()
    }

    private fun rms(b: ByteArray, n: Int): Float {
        var sum = 0.0
        var i = 0
        while (i + 1 < n) {
            val s = (b[i].toInt() and 0xFF) or (b[i + 1].toInt() shl 8)
            sum += s.toShort().toDouble().let { it * it }
            i += 2
        }
        val count = (n / 2).coerceAtLeast(1)
        return (sqrt(sum / count) / 9000.0).coerceIn(0.0, 1.0).toFloat()
    }

    companion object { const val MAX_SECONDS = 120 }
}
