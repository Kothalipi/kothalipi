package com.kothalipi.android.ime

import android.annotation.SuppressLint
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import com.kothalipi.android.MainActivity
import com.kothalipi.android.R
import com.kothalipi.android.data.Mode
import com.kothalipi.android.data.Prefs
import com.kothalipi.android.stt.Dictation
import com.kothalipi.android.stt.Notice
import com.kothalipi.android.stt.State

/**
 * The Kothalipi voice keyboard: a big mic, a Bangla/English switch and a few punctuation keys.
 * Tap the mic to start and tap again to finish, or hold it and release.
 */
class KothalipiIme : InputMethodService(), Dictation.Listener {
    private lateinit var prefs: Prefs
    private lateinit var dictation: Dictation
    private lateinit var root: View
    private lateinit var mic: View
    private lateinit var halo: View
    private lateinit var status: TextView
    private lateinit var bn: TextView
    private lateinit var en: TextView
    private val main = Handler(Looper.getMainLooper())
    private var pressedAt = 0L

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        dictation = Dictation(this, prefs, this)
    }

    override fun onCreateInputView(): View {
        root = LayoutInflater.from(this).inflate(R.layout.keyboard, null)
        mic = root.findViewById(R.id.mic)
        halo = root.findViewById(R.id.halo)
        status = root.findViewById(R.id.status)
        bn = root.findViewById(R.id.lang_bn)
        en = root.findViewById(R.id.lang_en)
        bn.setOnClickListener { setLang("bn") }
        en.setOnClickListener { setLang("en") }
        setLang(prefs.lang)
        bindMic()
        key(R.id.key_comma) { commit(",") }
        key(R.id.key_danda) { commit("।") }
        key(R.id.key_q) { commit("?") }
        key(R.id.key_dot) { commit(".") }
        key(R.id.key_space) { commit(" ") }
        key(R.id.key_enter) { enter() }
        bindDelete(root.findViewById(R.id.key_del))
        val globe = root.findViewById<View>(R.id.key_globe)
        globe.setOnClickListener { tick(it); switchKeyboard() }
        globe.setOnLongClickListener { (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker(); true }
        showIdle()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        if (::root.isInitialized) { setLang(prefs.lang); showIdle() }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        dictation.release()
        if (::root.isInitialized) { resetMic(); showIdle() }
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        dictation.release()
        super.onDestroy()
    }

    // ---- language and mic

    private fun setLang(lang: String) {
        prefs.lang = lang
        bn.isSelected = lang == "bn"
        en.isSelected = lang == "en"
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindMic() {
        mic.setOnTouchListener { v, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    tick(v)
                    pressedAt = System.currentTimeMillis()
                    if (dictation.state == State.LISTENING) dictation.stop() else dictation.start(prefs.lang)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // A long press is hold-to-talk: finish on release. A short tap keeps listening.
                    val held = System.currentTimeMillis() - pressedAt
                    if (e.actionMasked == MotionEvent.ACTION_UP && held > HOLD_MS && dictation.state == State.LISTENING) dictation.stop()
                    true
                }
                else -> true
            }
        }
        mic.contentDescription = getString(R.string.tap_to_speak)
    }

    override fun onState(state: State) {
        when (state) {
            State.LISTENING -> { status.setText(R.string.listening); mic.setBackgroundResource(R.drawable.bg_mic_on) }
            State.WORKING -> { status.setText(R.string.working); resetMic() }
            State.IDLE -> { resetMic(); if (status.text == getString(R.string.listening) || status.text == getString(R.string.working)) showIdle() }
        }
    }

    private fun resetMic() {
        mic.setBackgroundResource(R.drawable.bg_mic)
        mic.scaleX = 1f; mic.scaleY = 1f
        halo.alpha = 0f
    }

    private fun showIdle() {
        val cloud = prefs.mode == Mode.CLOUD
        status.text = if (cloud) "Cloud · ${prefs.cloudEngines().firstOrNull()?.first?.label ?: "add a key"}" else "On-device"
    }

    override fun onLevel(level: Float) {
        halo.alpha = 0.18f + 0.35f * level
        val s = 1f + 0.18f * level
        halo.scaleX = 1f + 0.35f * level; halo.scaleY = halo.scaleX
        mic.scaleX = s; mic.scaleY = s
    }

    override fun onPartial(text: String) { status.text = text }

    override fun onText(text: String) {
        showIdle()
        val ic = currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1, 0)?.toString().orEmpty()
        val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        ic.commitText(lead + text, 1)
    }

    override fun onNotice(notice: Notice) {
        when (notice) {
            Notice.NEED_MIC -> {
                status.setText(R.string.need_mic)
                startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            Notice.NEED_KEY -> status.setText(R.string.need_key)
            Notice.NOTHING_HEARD -> status.setText(R.string.nothing_heard)
            Notice.CLOUD_FAILED -> status.setText(R.string.cloud_failed)
            Notice.NO_ENGINE -> status.text = "On-device speech is not available on this phone."
        }
        main.postDelayed({ if (dictation.state == State.IDLE) showIdle() }, 3500)
    }

    // ---- keys

    private fun key(id: Int, action: () -> Unit) {
        root.findViewById<View>(id).setOnClickListener { tick(it); action() }
    }

    private fun commit(s: String) { currentInputConnection?.commitText(s, 1) }

    private fun enter() {
        val ic = currentInputConnection ?: return
        val action = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION) ?: EditorInfo.IME_ACTION_NONE
        if (action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED &&
            currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0) {
            ic.performEditorAction(action)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindDelete(v: View) {
        val repeat = object : Runnable {
            override fun run() { backspace(); main.postDelayed(this, 55) }
        }
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> { tick(view); backspace(); main.postDelayed(repeat, 380) }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> main.removeCallbacks(repeat)
            }
            true
        }
    }

    private fun backspace() {
        val ic = currentInputConnection ?: return
        val sel = ic.getSelectedText(0)
        if (!sel.isNullOrEmpty()) ic.commitText("", 1) else ic.deleteSurroundingTextInCodePoints(1, 0)
    }

    private fun switchKeyboard() {
        if (!switchToNextInputMethod(false)) {
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()
        }
    }

    private fun tick(v: View) { v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    companion object { private const val HOLD_MS = 450L }
}
