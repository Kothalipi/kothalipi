package com.kothalipi.android.stt

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.kothalipi.android.data.Mode
import com.kothalipi.android.data.Prefs

enum class State { IDLE, LISTENING, WORKING }
enum class Notice { NEED_MIC, NEED_KEY, NOTHING_HEARD, CLOUD_FAILED, NO_ENGINE }

/** One dictation at a time: record, transcribe (cloud first, then on-device), hand back text. */
class Dictation(private val context: Context, private val prefs: Prefs, private val listener: Listener) {
    interface Listener {
        fun onState(state: State)
        fun onLevel(level: Float)
        fun onPartial(text: String)
        fun onText(text: String)
        fun onNotice(notice: Notice)
    }

    private val main = Handler(Looper.getMainLooper())
    private var recorder: Recorder? = null
    private var useDevice = false
    private var forceDeviceOnce = false
    private var lang = "bn"
    var state = State.IDLE
        private set

    private val device = DeviceStt(context, object : DeviceStt.Callback {
        override fun onPartial(text: String) = main.post { listener.onPartial(text) }.let {}
        override fun onFinal(text: String) { set(State.IDLE); listener.onText(text) }
        override fun onError(noMatch: Boolean) { set(State.IDLE); listener.onNotice(if (noMatch) Notice.NOTHING_HEARD else Notice.NO_ENGINE) }
    })

    private fun set(s: State) { state = s; listener.onState(s) }

    fun hasMic() = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun start(language: String) {
        if (state != State.IDLE) return
        if (!hasMic()) { listener.onNotice(Notice.NEED_MIC); return }
        lang = language
        val engines = prefs.cloudEngines()
        useDevice = prefs.mode == Mode.DEVICE || forceDeviceOnce
        if (!useDevice && engines.isEmpty()) { listener.onNotice(Notice.NEED_KEY); return }
        forceDeviceOnce = false
        if (useDevice) {
            if (!device.available()) { listener.onNotice(Notice.NO_ENGINE); return }
            set(State.LISTENING)
            device.start(lang)
        } else {
            val r = Recorder { l -> main.post { listener.onLevel(l) } }
            if (!r.start()) { listener.onNotice(Notice.NEED_MIC); return }
            recorder = r
            set(State.LISTENING)
        }
    }

    fun stop() {
        if (state != State.LISTENING) return
        if (useDevice) { set(State.WORKING); device.finish(); return }
        val pcm = recorder?.stop() ?: ByteArray(0)
        recorder = null
        if (pcmSeconds(pcm) < 0.4f) { set(State.IDLE); listener.onNotice(Notice.NOTHING_HEARD); return }
        set(State.WORKING)
        val engines = prefs.cloudEngines()
        val wav = pcmToWav(pcm)
        Thread {
            val result = CloudStt.transcribe(wav, lang, engines)
            main.post {
                set(State.IDLE)
                when (result) {
                    is Outcome.Ok -> listener.onText(result.text)
                    is Outcome.Failed -> { forceDeviceOnce = true; listener.onNotice(Notice.CLOUD_FAILED) }
                }
            }
        }.start()
    }

    fun toggle(language: String) { if (state == State.LISTENING) stop() else start(language) }

    fun release() {
        recorder?.stop(); recorder = null
        device.stop()
        state = State.IDLE
    }
}
