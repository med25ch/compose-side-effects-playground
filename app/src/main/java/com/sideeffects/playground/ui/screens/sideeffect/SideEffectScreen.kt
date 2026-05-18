package com.sideeffects.playground.ui.screens.sideeffect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sideeffects.playground.ui.components.*
import com.sideeffects.playground.util.currentTime

// Simulate an analytics SDK that lives outside Compose
object FakeAnalytics {
    var currentScreen: String = "none"
    var userId: String = "anonymous"
    val events = mutableListOf<String>()

    fun setScreen(name: String) {
        currentScreen = name
        events.add("[${currentTime()}] Screen set: $name")
    }
    fun setUser(id: String) {
        userId = id
        events.add("[${currentTime()}] User set: $id")
    }
}

@Composable
fun SideEffectScreen(onBack: () -> Unit) {
    var recompositionTrigger by remember { mutableIntStateOf(0) }
    var currentUser by remember { mutableStateOf("user_001") }
    val analyticsLogs = remember { mutableStateListOf<String>() }

    // CORRECT: SideEffect runs after every SUCCESSFUL recomposition
    // Keeps the analytics SDK in sync with current Compose state
    SideEffect {
        FakeAnalytics.setScreen("SideEffectPlayground")
        FakeAnalytics.setUser(currentUser)
        analyticsLogs.clear()
        analyticsLogs.addAll(FakeAnalytics.events.takeLast(10))
    }

    Scaffold(
        topBar = { PlaygroundTopBar("SideEffect", onBack) }
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
            item { WhenNotToUseSection() }
            item { HorizontalDivider() }
            item { LiveDemoSection(
                recompositionTrigger = recompositionTrigger,
                currentUser = currentUser,
                analyticsLogs = analyticsLogs,
                onTrigger = { recompositionTrigger++ },
                onChangeUser = { currentUser = "user_00${(1..5).random()}" }
            ) }
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
        SectionHeader("📚 SideEffect")
        TheoryBox(
            title = "Sync Compose state to non-Compose code",
            content = """SideEffect runs after EVERY successful recomposition.

Key characteristics:
• Synchronous — runs on the main thread after composition
• No keys — always runs after every successful recomposition
• No cleanup — no onDispose equivalent
• Skipped if composition FAILS (unlike direct body execution)

When to use it:
• Pushing Compose state to a non-Compose SDK
  (analytics, Firebase, custom View, third-party SDK)
• Setting properties on objects that don't use Compose state
• Bridging Compose state to legacy code that uses callbacks

Important: "Successful" means the composition completed without errors.
If Compose aborts a recomposition, SideEffect is skipped — this is a 
safety guarantee you don't get from direct body calls."""
        )
    }
}

@Composable
private fun WhenNotToUseSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("⚠️ When NOT to use SideEffect")

        BadCodeBox(
            title = "❌ Don't use SideEffect for async work",
            code = """SideEffect {
    // WRONG: SideEffect is synchronous
    // This would block the main thread!
    val data = runBlocking { api.loadData() }
    
    // Also wrong: can't launch a coroutine meaningfully here
    scope.launch { /* ... */ }
    // ^ Will launch but SideEffect gives no lifecycle guarantees
    // for work started inside it
}

// CORRECT: Use LaunchedEffect for async work""",
            explanation = "SideEffect is synchronous and runs on the main thread. " +
                    "It's only for synchronous state syncing. For async work, always use LaunchedEffect."
        )

        BadCodeBox(
            title = "❌ Don't use SideEffect to modify Compose state",
            code = """var count by remember { mutableIntStateOf(0) }

SideEffect {
    // INFINITE LOOP!
    // Writing state causes recomposition
    // Recomposition triggers SideEffect
    // SideEffect writes state → recomposition → ...
    count++ 
}""",
            explanation = "Writing Compose state inside SideEffect triggers recomposition, " +
                    "which runs SideEffect again, causing an infinite loop. SideEffect is for " +
                    "pushing state OUT to non-Compose code, not for modifying Compose state."
        )
    }
}

@Composable
private fun LiveDemoSection(
    recompositionTrigger: Int,
    currentUser: String,
    analyticsLogs: List<String>,
    onTrigger: () -> Unit,
    onChangeUser: () -> Unit
) {
    var recompCount = 0
    recompCount++ // Increments on every recomposition — shows it happening

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Live Demo: Analytics SDK Sync")

        TheoryBox(
            title = "What's happening",
            content = "SideEffect runs after every recomposition, keeping FakeAnalytics " +
                    "in sync with the current Compose state (currentUser, screen name). " +
                    "Trigger recomposition or change the user and watch the analytics log update."
        )

        StatusIndicator(
            label = "Analytics.currentScreen",
            value = FakeAnalytics.currentScreen,
            color = Color(0xFF1565C0)
        )
        StatusIndicator(
            label = "Analytics.userId",
            value = currentUser,
            color = Color(0xFF6A1B9A)
        )
        StatusIndicator(
            label = "Recomposition count",
            value = recompositionTrigger.toString(),
            color = Color(0xFF827717)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onTrigger) {
                Text("Trigger Recomposition")
            }
            OutlinedButton(onClick = onChangeUser) {
                Text("Change User")
            }
        }

        SectionHeader("📋 Analytics Events Log")
        LogConsole(
            logs = analyticsLogs.map { LogEntry(it, LogType.INFO) },
            modifier = Modifier.heightIn(min = 100.dp, max = 200.dp)
        )

        GoodCodeBox(
            title = "✅ The correct SideEffect usage in this screen",
            code = """// At the top of SideEffectScreen composable:
SideEffect {
    FakeAnalytics.setScreen("SideEffectPlayground")
    FakeAnalytics.setUser(currentUser) // Always gets latest currentUser
}

// Why this works:
// 1. currentUser changes → recomposition happens
// 2. SideEffect runs after the successful recomposition
// 3. Analytics is updated with the new currentUser
// 4. Analytics is ALWAYS in sync with Compose state""",
            explanation = "SideEffect gives you 'after every recomposition, do this sync'. " +
                    "The analytics object always reflects what's currently rendered."
        )
    }
}

@Composable
private fun ChallengeSection() {
    ChallengeBox(
        challengeText = """Challenge:
You're integrating a legacy ViewGroup that has a setTheme(isDark: Boolean) method.
The theme comes from Compose state. Use SideEffect to keep it in sync.

@Composable
fun LegacyViewWrapper(isDarkTheme: Boolean, legacyView: LegacyView) {
    // Your implementation
    
    AndroidView(factory = { legacyView })
}

BONUS: Is SideEffect or rememberUpdatedState + DisposableEffect more appropriate here?
When would you choose each?""",
        hint = "SideEffect is simpler if you just need to call a setter on each update. " +
                "DisposableEffect is needed if the view needs to be fully re-initialized (not just updated) " +
                "when the theme changes — and needs cleanup."
    )
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        InterviewCard(
            question = "What does 'successful recomposition' mean for SideEffect?",
            answer = "Compose can abort a recomposition midway (e.g., if new state arrives while composing). SideEffect only runs after a completed, committed recomposition — meaning the UI was actually updated. This prevents partial state leaking to the outside world."
        )
        InterviewCard(
            question = "Can SideEffect replace LaunchedEffect for non-async work?",
            answer = "For truly synchronous one-shot setup, SideEffect would run on every recomposition (potentially hundreds of times). LaunchedEffect(Unit) runs exactly once. Use SideEffect only when you WANT to sync on every recomposition. Use LaunchedEffect(Unit) for one-time initialization."
        )
        InterviewCard(
            question = "What's the execution order: SideEffect vs LaunchedEffect vs DisposableEffect?",
            answer = "All side effects are scheduled after composition completes. SideEffect runs synchronously after each successful composition. LaunchedEffect and DisposableEffect run their setup blocks after composition, but they're managed by the effect system and may be deferred. Generally: composition → SideEffect → effect setups."
        )
    }
}
