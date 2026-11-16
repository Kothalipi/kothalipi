package com.kothalipi.android.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import com.kothalipi.android.R
import com.kothalipi.android.data.Mode
import com.kothalipi.android.data.Prefs
import com.kothalipi.android.data.Provider
import com.kothalipi.android.stt.CloudStt
import com.kothalipi.android.stt.Failure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Step { MIC, ENABLE, SELECT, ENGINE }

@Composable
fun KothalipiApp(resumes: Int) {
    KothalipiTheme {
        val ctx = LocalContext.current
        val prefs = remember { Prefs(ctx) }
        var engineChosen by remember { mutableStateOf(prefs.engineChosen) }
        // `resumes` is read so every return from system settings re-evaluates this state.
        val mic = remember(resumes) { SystemState.micGranted(ctx) }
        val enabled = remember(resumes) { SystemState.keyboardEnabled(ctx) }
        val selected = remember(resumes) { SystemState.keyboardSelected(ctx) }
        val step = when {
            !mic -> Step.MIC
            !enabled -> Step.ENABLE
            !selected -> Step.SELECT
            !engineChosen -> Step.ENGINE
            else -> null
        }
        androidx.compose.material3.Surface(
            Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onBackground,
        ) {
          Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { it / 4 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 } + fadeOut())
                },
                label = "step",
            ) { s ->
                if (s == null) Home(prefs) { engineChosen = false }
                else Setup(s, prefs, onEngineDone = { prefs.engineChosen = true; engineChosen = true })
            }
          }
        }
    }
}

// ---------------------------------------------------------------- setup

@Composable
private fun Setup(step: Step, prefs: Prefs, onEngineDone: () -> Unit) {
    val ctx = LocalContext.current
    val index = step.ordinal
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Logo(72)
        Spacer(Modifier.height(10.dp))
        Text("Kothalipi", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("কথালিপি", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { i ->
                Box(
                    Modifier.height(6.dp).width(if (i == index) 34.dp else 18.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (i <= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Step ${index + 1} of 4", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        when (step) {
            Step.MIC -> {
                val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                StepCard("Allow the microphone", "Kothalipi listens only while you hold or tap the mic on the keyboard. Nothing is recorded in the background.") {
                    Button({ ask.launch(android.Manifest.permission.RECORD_AUDIO) }, Modifier.fillMaxWidth()) { Text("Allow microphone") }
                    TextButton({ ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) }) {
                        Text("Already refused? Open app settings")
                    }
                }
            }
            Step.ENABLE -> StepCard("Turn on the keyboard", "Switch on “Kothalipi Voice Keyboard” in the list that opens. Android will show a standard warning for every keyboard; it is safe to continue.") {
                Button({ ctx.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Open keyboard settings") }
            }
            Step.SELECT -> StepCard("Choose Kothalipi", "Pick Kothalipi as your current keyboard. You can switch back any time with the globe key.") {
                Button({ (ctx.getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker() }, Modifier.fillMaxWidth()) { Text("Choose keyboard") }
            }
            Step.ENGINE -> EngineCard(prefs, onDone = onEngineDone)
        }
    }
}

@Composable
private fun StepCard(title: String, body: String, actions: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            actions()
        }
    }
}

@Composable
private fun EngineCard(prefs: Prefs, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(prefs.mode) }
    var gemini by remember { mutableStateOf(prefs.key(Provider.GEMINI).orEmpty()) }
    var openai by remember { mutableStateOf(prefs.key(Provider.OPENAI).orEmpty()) }
    var checking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    StepCard("How should it listen?", "Cloud is the most accurate. On-device is free and stays on your phone.") {
        ModeChoice("Cloud", "Your own Gemini or OpenAI key. Best accuracy for Bangla.", mode == Mode.CLOUD) { mode = Mode.CLOUD }
        ModeChoice("On-device", "Uses Android's built-in speech. Free. Needs the Bangla language pack.", mode == Mode.DEVICE) { mode = Mode.DEVICE }
        if (mode == Mode.CLOUD) {
            KeyField("Gemini API key", gemini) { gemini = it }
            TextButton({ ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))) }) { Text("Get a free Gemini key") }
            KeyField("OpenAI API key (optional)", openai) { openai = it }
            Text("If one engine fails, Kothalipi tries the next, then on-device. Keys stay encrypted on this phone.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
        Button(
            onClick = {
                error = null
                if (mode == Mode.DEVICE) {
                    prefs.mode = Mode.DEVICE; onDone(); return@Button
                }
                if (gemini.isBlank() && openai.isBlank()) { error = "Paste a key, or choose On-device."; return@Button }
                checking = true
                scope.launch {
                    val bad = withContext(Dispatchers.IO) {
                        listOf(Provider.GEMINI to gemini, Provider.OPENAI to openai)
                            .filter { it.second.isNotBlank() }
                            .firstNotNullOfOrNull { (p, k) -> CloudStt.checkKey(p, k.trim())?.let { p to it } }
                    }
                    checking = false
                    if (bad != null) {
                        error = when (bad.second) {
                            Failure.BAD_KEY -> "${bad.first.label} rejected that key. Check it and try again."
                            Failure.NETWORK -> "Couldn't reach ${bad.first.label}. Check your internet."
                            else -> "${bad.first.label} could not verify the key right now."
                        }
                    } else {
                        prefs.setKey(Provider.GEMINI, gemini); prefs.setKey(Provider.OPENAI, openai)
                        prefs.mode = Mode.CLOUD; onDone()
                    }
                }
            },
            enabled = !checking, modifier = Modifier.fillMaxWidth(),
        ) { Text(if (checking) "Checking key…" else "Finish setup") }
    }
}

@Composable
private fun ModeChoice(title: String, sub: String, on: Boolean, click: () -> Unit) {
    OutlinedButton(
        onClick = click, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (on) 2.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun KeyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------- home

@Composable
private fun Home(prefs: Prefs, changeEngine: () -> Unit) {
    val ctx = LocalContext.current
    var test by remember { mutableStateOf("") }
    val pulse = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { pulse.animateTo(1f, androidx.compose.animation.core.tween(600)) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Logo(48)
            Column {
                Text("Kothalipi", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("You're all set", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
        }
        StepCard("Try it", "Tap here, switch to Kothalipi with the globe key if needed, then tap the mic and speak.") {
            OutlinedTextField(
                value = test, onValueChange = { test = it }, placeholder = { Text("আপনার কথা এখানে লেখা হবে…") },
                modifier = Modifier.fillMaxWidth(), minLines = 3,
            )
        }
        StepCard("Engine", when {
            prefs.mode == Mode.DEVICE -> "On-device (Android speech)"
            else -> "Cloud · " + prefs.cloudEngines().joinToString(" → ") { it.first.label } + " → on-device"
        }) {
            OutlinedButton(changeEngine, Modifier.fillMaxWidth()) { Text("Change engine or keys") }
        }
        StepCard("How to use", "Open any app and tap a text field. Tap the mic to start and tap again to finish, or hold it and let go. Use the বাংলা and English chips to pick the language. Long-press the globe key to switch keyboards.") {}
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Free and open source (MIT)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton({ ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://kothalipi.tashanto.com"))) }) { Text("kothalipi.tashanto.com") }
            Text("Made by TA Shanto · tashanto.com", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Logo(size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.size((size * 1.35f).dp))
    }
}
