package com.kothalipi.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.kothalipi.android.ui.KothalipiApp

class MainActivity : ComponentActivity() {
    /** Bumped on every resume so the setup checklist re-reads the system state. */
    private var resumes by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // API keys are typed or pasted by the user; never let an autofill service fill them in.
        window.decorView.importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        setContent { KothalipiApp(resumes) }
    }

    /** The keyboard picker is a dialog, so the activity does not pause; re-check when focus returns. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) resumes++
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }
}
