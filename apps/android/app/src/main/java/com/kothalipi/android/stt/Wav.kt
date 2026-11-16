package com.kothalipi.android.stt

import java.nio.ByteBuffer
import java.nio.ByteOrder

const val SAMPLE_RATE = 16_000

/** Wrap 16-bit mono PCM in a WAV container. */
fun pcmToWav(pcm: ByteArray, sampleRate: Int = SAMPLE_RATE): ByteArray {
    val b = ByteBuffer.allocate(44 + pcm.size).order(ByteOrder.LITTLE_ENDIAN)
    b.put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
    b.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
    b.putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
    b.put("data".toByteArray()).putInt(pcm.size).put(pcm)
    return b.array()
}

fun pcmSeconds(pcm: ByteArray): Float = pcm.size / (SAMPLE_RATE * 2f)
