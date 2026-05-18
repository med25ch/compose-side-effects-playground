package com.sideeffects.playground.ui.screens.rememberupdatedstate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sideeffects.playground.ui.components.*
import com.sideeffects.playground.util.currentTime
import kotlinx.coroutines.delay

@Composable
fun RememberUpdatedStateScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { PlaygroundTopBar("rememberUpdatedState", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { TheorySection() }
            item { HorizontalDivider() }
            item { TheProblemSection() }
            item { HorizontalDivider() }
            item { BadDemoSection() }
            item { HorizontalDivider() }
            item { GoodDemoSection() }
            item { HorizontalDivider() }
            item { SplashScreenRealWorldSection() }
            item { HorizontalDivider() }
            item { ChallengeSection() }
            item { HorizontalDivider() }
            item { InterviewSection() }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun TheorySection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("📚 rememberUpdatedState")
        TheoryBox(
            title = "The Stale Lambda Problem",
            content = """rememberUpdatedState solves a specific and tricky problem:

When a lambda/value is captured inside a long-lived effect (LaunchedEffect, DisposableEffect), and that lambda/value changes via recomposition — the effect does NOT restart (key didn't change), so it still holds the OLD lambda.

rememberUpdatedState creates a reference that:
• Always points to the LATEST value
• Does NOT cause the effect to restart when it updates
• Is safe to read inside long-lived coroutines

Signature: val latestValue by rememberUpdatedState(value)

Classic use case: splash screen timer where the "onTimeout" callback might change but you don't want to reset the 5-second countdown."""
        )
    }
}

@Composable
private fun TheProblemSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🧩 The Core Problem")

        TheoryBox(
            title = "Why closures go stale",
            content = """In Kotlin, lambdas capture variables by REFERENCE at the time of creation.

In Compose, a LaunchedEffect block is a lambda captured when the effect starts.
If the effect key doesn't change, the block is NOT re-executed — meaning any 
values it captured are frozen at the time the effect started.

Example problem:
1. LaunchedEffect(Unit) starts — captures onTimeout = callback_v1
2. Parent recomposes — onTimeout becomes callback_v2
3. After 5 seconds, LaunchedEffect calls onTimeout() 
4. It calls callback_v1, NOT callback_v2 ← STALE CLOSURE BUG"""
        )

        CodeBlock("""// The invisible bug:

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Every recomposition, onTimeout is a NEW lambda reference
    // But LaunchedEffect(Unit) never restarts...
    
    LaunchedEffect(Unit) {
        delay(5000)
        onTimeout() // ← This is the onTimeout from composition time
                    // NOT the latest onTimeout!
    }
}

// Caller:
SplashScreen(
    onTimeout = { 
        // If this lambda changes (e.g., captures new state),
        // the SplashScreen's LaunchedEffect won't know
        navController.navigate(currentDestination) 
    }
)""")
    }
}

@Composable
private fun BadDemoSection() {
    var callbackVersion by remember { mutableIntStateOf(1) }
    val logs = remember { mutableStateListOf<String>() }

    // BAD: captures the callback version at launch time
    LaunchedEffect(Unit) {
        logs.add("[${currentTime()}] ❌ Bad effect started — captured callbackVersion=$callbackVersion")
        delay(3000)
        // This will use the callbackVersion value from when the effect STARTED, not current
        logs.add("[${currentTime()}] ❌ Bad effect fires — sees callbackVersion=$callbackVersion (STALE!)")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("❌ Bad Demo: Stale closure")

        BadCodeBox(
            title = "❌ Stale closure — onTimeout captures old value",
            code = """@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(5000)
        onTimeout() // ← Stale! Captured at launch time
    }
}""",
            explanation = "onTimeout captured at LaunchedEffect start. If the parent " +
                    "recomposes and passes a new onTimeout lambda (e.g., one that captures " +
                    "updated navigation state), the stale version is called."
        )

        Text(
            "Live Bad Demo (3s timer, change version during countdown):",
            fontWeight = FontWeight.Medium
        )
        Text(
            "Current callbackVersion: $callbackVersion",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { callbackVersion++ }) {
                Text("Change Version (currently $callbackVersion)")
            }
        }

        LogConsole(
            logs = logs.map { LogEntry(it, LogType.ERROR) },
            modifier = Modifier.heightIn(min = 80.dp, max = 160.dp)
        )

        Text(
            "Notice: the bad effect logged callbackVersion at start time, not current time",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun GoodDemoSection() {
    var callbackVersion by remember { mutableIntStateOf(1) }
    val logs = remember { mutableStateListOf<String>() }

    // CORRECT: rememberUpdatedState always reflects latest value
    val latestVersion by rememberUpdatedState(callbackVersion)

    LaunchedEffect(Unit) {
        logs.add("[${currentTime()}] ✓ Good effect started — latestVersion ref created")
        delay(3000)
        // latestVersion.value always returns the CURRENT value
        logs.add("[${currentTime()}] ✓ Good effect fires — sees latestVersion=$latestVersion (CURRENT!)")
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("✅ Good Demo: rememberUpdatedState")

        GoodCodeBox(
            title = "✅ rememberUpdatedState — always reads latest value",
            code = """@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // Creates a State<() -> Unit> that always holds the latest onTimeout
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    
    LaunchedEffect(Unit) {
        delay(5000)
        currentOnTimeout() // ← ALWAYS calls the latest version!
        // Even if onTimeout changed 10 times during the 5 seconds,
        // this calls the most recent one.
    }
}""",
            explanation = "rememberUpdatedState wraps the value in a MutableState. " +
                    "On each recomposition, it updates the state with the new value — but does NOT " +
                    "change the State reference itself. The LaunchedEffect reads through the stable " +
                    "State reference and always gets the latest value."
        )

        Text(
            "Current callbackVersion: $callbackVersion",
            color = Color(0xFF2E7D32),
            fontWeight = FontWeight.Bold
        )

        Button(onClick = { callbackVersion++ }) {
            Text("Change Version (currently $callbackVersion)")
        }

        LogConsole(
            logs = logs.map { LogEntry(it, LogType.SUCCESS) },
            modifier = Modifier.heightIn(min = 80.dp, max = 160.dp)
        )

        Text(
            "Notice: the good effect reads the CURRENT version even after the countdown",
            fontSize = 12.sp,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
private fun SplashScreenRealWorldSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🌍 Real-World: Splash Screen Pattern")

        TheoryBox(
            title = "The canonical use case",
            content = "This is the most documented use case for rememberUpdatedState. " +
                    "A splash screen has a fixed 3-second timer. The onTimeout callback " +
                    "navigates somewhere — but navigation targets can depend on state " +
                    "(user logged in? → Home, else → Login).\n\n" +
                    "If that state loads async during the splash, the callback changes " +
                    "mid-countdown. Without rememberUpdatedState, you navigate to the " +
                    "wrong screen (the one that was set when the timer started)."
        )

        GoodCodeBox(
            title = "✅ Production splash screen pattern",
            code = """@Composable
fun SplashScreen(
    onAuthReady: (isLoggedIn: Boolean) -> Unit
) {
    // onAuthReady might change if auth state loads during splash
    val currentOnAuthReady by rememberUpdatedState(onAuthReady)
    
    LaunchedEffect(Unit) {
        delay(3000) // Fixed splash duration
        currentOnAuthReady(/* isLoggedIn from latest state */)
        // Calls the CURRENT callback — which has the CURRENT isLoggedIn
    }
    
    // Splash UI...
}

// Caller — onAuthReady captures isLoggedIn from ViewModel state
// which may not be available until AFTER the timer starts
val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()

SplashScreen(
    onAuthReady = { navController.navigate(
        if (isLoggedIn) "home" else "login"
    )}
)""",
            explanation = "Without rememberUpdatedState, if isLoggedIn loads at second 2 of " +
                    "the 3-second countdown, the LaunchedEffect still calls the original " +
                    "onAuthReady (captured before isLoggedIn was available), sending to the wrong screen."
        )
    }
}

@Composable
private fun ChallengeSection() {
    ChallengeBox(
        challengeText = """Challenge: 
You have a countdown timer component that receives an `onTick` callback. 
The caller changes `onTick` to log to a different destination mid-countdown.

Without rememberUpdatedState: the timer calls the OLD onTick.
With rememberUpdatedState: the timer always calls the CURRENT onTick.

Implement:
@Composable
fun CountdownTimer(
    seconds: Int,
    onTick: (remaining: Int) -> Unit,
    onFinished: () -> Unit
) {
    // Your implementation using rememberUpdatedState
}

BONUS: Why don't you need rememberUpdatedState for the 'seconds' parameter?""",
        hint = "seconds changes the effect behavior (how long it counts) → it should be a key. " +
                "onTick/onFinished change without affecting the duration → use rememberUpdatedState."
    )
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        InterviewCard(
            question = "What problem does rememberUpdatedState solve?",
            answer = "It prevents stale closure bugs in long-lived effects. When a value (callback, state) is captured inside LaunchedEffect/DisposableEffect but the effect shouldn't restart when it changes, rememberUpdatedState provides a stable reference that always returns the latest value."
        )
        InterviewCard(
            question = "How does rememberUpdatedState work internally?",
            answer = "It creates a mutableStateOf(value) wrapped in a remember block, and on each recomposition, it updates the state with the new value via a SideEffect. The State object reference is stable (same remember slot), but its value is always current. Callers reading state.value inside effects always get the latest."
        )
        InterviewCard(
            question = "When should you NOT use rememberUpdatedState?",
            answer = "When changing the value SHOULD restart the effect. In that case, use the value directly as a LaunchedEffect key. rememberUpdatedState is specifically for 'the value changed, but don't restart the effect — just make sure the effect reads the latest value when it eventually runs'."
        )
        InterviewCard(
            question = "What's the difference between rememberUpdatedState and just reading state inside LaunchedEffect?",
            answer = "If you read a MutableStateFlow or remember { mutableStateOf() } inside LaunchedEffect, you're reading the current value — which is correct. rememberUpdatedState is needed specifically when you receive a PARAMETER (lambda, primitive) from outside that Compose doesn't automatically track as state."
        )
    }
}
