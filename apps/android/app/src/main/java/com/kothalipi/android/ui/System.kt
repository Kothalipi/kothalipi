package com.kothalipi.android.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.inputmethod.InputMethodManager

/** Reads the three system facts the setup checklist depends on. */
object SystemState {
    fun micGranted(c: Context) =
        c.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun keyboardEnabled(c: Context): Boolean {
        val imm = c.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.enabledInputMethodList.any { it.packageName == c.packageName }
    }

    fun keyboardSelected(c: Context): Boolean {
        val id = Settings.Secure.getString(c.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: return false
        return id.startsWith(c.packageName + "/")
    }
}
