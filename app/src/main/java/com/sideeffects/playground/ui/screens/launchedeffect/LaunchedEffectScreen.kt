package com.sideeffects.playground.ui.screens.launchedeffect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sideeffects.playground.domain.model.LoadState
import com.sideeffects.playground.ui.components.*
import com.sideeffects.playground.util.currentTime

@Composable
fun LaunchedEffectScreen(
    onBack: () -> Unit,
    viewModel: LaunchedEffectViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // CORRECT: LaunchedEffect with userId key — restarts when user changes
    LaunchedEffect(state.userId) {
        viewModel.loadUserData(state.userId)
    }

    Scaffold(
        topBar = { PlaygroundTopBar("LaunchedEffect", onBack) }
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
            item { BadImplementationSection() }
            item { HorizontalDivider() }
            item { CorrectImplementationSection() }
            item { HorizontalDivider() }
            item { LiveDemoSection(state, viewModel) }
            item { HorizontalDivider() }
            item { TimerDemoSection(state, viewModel) }
            item { HorizontalDivider() }
            item { KeyBehaviorSection(state, viewModel) }
            item { HorizontalDivider() }
            item { LogSection(state, viewModel) }
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
        SectionHeader("📚 What is LaunchedEffect?")
        TheoryBox(
            title = "Core Concept",
            content = """LaunchedEffect launches a coroutine that is:
• Scoped to the composition — cancelled when the composable leaves
• Restarted when ANY key changes  
• Automatically cancelled before restarting with a new key

Signature: LaunchedEffect(key1, key2, ...) { /* suspend block */ }

The runtime contract:
1. Enters composition → coroutine launches immediately
2. Key changes → old coroutine cancelled → new coroutine starts
3. Leaves composition → coroutine cancelled

When to use it:
• Load data when a composable appears
• Collect a Flow / StateFlow
• Start an animation triggered by state
• Navigate as a result of state change
• Track screen analytics (LaunchedEffect(Unit))"""
        )
    }
}

@Composable
private fun BadImplementationSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("❌ Anti-Patterns")

        BadCodeBox(
            title = "❌ Anti-Pattern #1: Direct call in composition body",
            code = """@Composable
fun UserProfile(userId: String) {
    // CATASTROPHIC: Runs on EVERY recomposition!
    // A button click elsewhere = another API call
    viewModel.loadUser(userId) 
    
    Text("Profile")
}""",
            explanation = "Any recomposition triggers another loadUser() call. " +
                    "If the API is fast, you'll hammer your server. " +
                    "If it triggers state changes, you get infinite recomposition loops."
        )

        Spacer(Modifier.height(8.dp))

        BadCodeBox(
            title = "❌ Anti-Pattern #2: Wrong key (Unit when you need dynamic key)",
            code = """@Composable
fun UserProfile(userId: String) {
    // BUG: Effect never restarts when userId changes!
    // User 1 navigates to User 2 → still shows User 1 data
    LaunchedEffect(Unit) { // ← Unit never changes
        viewModel.loadUser(userId)
    }
}""",
            explanation = "Using Unit as key means this runs exactly once — " +
                    "when the composable first enters. If the userId parameter changes " +
                    "(navigation param update), the data is never reloaded. Classic bug."
        )

        Spacer(Modifier.height(8.dp))

        BadCodeBox(
            title = "❌ Anti-Pattern #3: Capturing mutable variable (stale closure)",
            code = """@Composable
fun Search(query: String) {
    var results by remember { mutableStateOf(emptyList<String>()) }
    
    LaunchedEffect(Unit) {
        // query is captured at launch time
        // If query changes, effect does NOT restart
        // We're searching for the FIRST query forever
        results = api.search(query) // ← stale 'query'!
    }
}""",
            explanation = "The lambda captures 'query' by value at launch time. " +
                    "If the parent recomposes with a new query, this effect is oblivious. " +
                    "Fix: use LaunchedEffect(query) so it restarts with each new query."
        )
    }
}

@Composable
private fun CorrectImplementationSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("✅ Correct Patterns")

        GoodCodeBox(
            title = "✅ Pattern #1: Key matches the data dependency",
            code = """@Composable
fun UserProfile(userId: String) {
    val viewModel: UserViewModel = hiltViewModel()
    
    // Restarts whenever userId changes — correct!
    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }
    
    // OR — collect a Flow reactively:
    LaunchedEffect(viewModel) {
        viewModel.userFlow.collect { user ->
            // handles every emission
        }
    }
}""",
            explanation = "Key = the dependency. When userId changes, old coroutine is " +
                    "cancelled and a fresh one starts with the new userId. " +
                    "This is structured concurrency — no leaks, no stale data."
        )

        Spacer(Modifier.height(8.dp))

        GoodCodeBox(
            title = "✅ Pattern #2: Unit key for one-shot actions",
            code = """@Composable
fun HomeScreen() {
    LaunchedEffect(Unit) {
        // Runs exactly once when this screen enters composition
        analytics.trackScreen("Home")
        // Unit never changes = effect never restarts = exactly-once
    }
}""",
            explanation = "Unit is a valid key when you INTENTIONALLY want to run once. " +
                    "The key is that you're making it explicit — 'I want this to run once.'"
        )

        Spacer(Modifier.height(8.dp))

        GoodCodeBox(
            title = "✅ Pattern #3: Multiple keys",
            code = """@Composable
fun FilteredList(
    category: String,
    sortOrder: SortOrder
) {
    // Restarts when EITHER category OR sortOrder changes
    LaunchedEffect(category, sortOrder) {
        viewModel.loadFiltered(category, sortOrder)
    }
}""",
            explanation = "Any key changing triggers a restart. This ensures you always " +
                    "load data for the current combination of filters."
        )
    }
}

@Composable
private fun LiveDemoSection(
    state: LaunchedEffectUiState,
    viewModel: LaunchedEffectViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Live Demo: Key-based restart")

        TheoryBox(
            title = "What's happening",
            content = "Tap a user button → userId changes → LaunchedEffect(userId) detects the key change → " +
                    "cancels any in-flight request → launches a new coroutine → loads the new user's data.\n\n" +
                    "Watch the log to see cancellation in action."
        )

        Text("Current user ID: ${state.userId}", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..4).forEach { id ->
                Button(
                    onClick = { viewModel.changeUser(id) },
                    colors = if (state.userId == id)
                        ButtonDefaults.buttonColors()
                    else
                        ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("User $id")
                }
            }
        }

        when (val loadState = state.loadState) {
            is LoadState.Idle -> Text("Idle", color = Color.Gray)
            is LoadState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Loading user ${state.userId}...", color = Color.Gray)
                }
            }
            is LoadState.Success -> StatusIndicator(
                label = "Result",
                value = loadState.data,
                color = Color(0xFF2E7D32)
            )
            is LoadState.Error -> StatusIndicator(
                label = "Error",
                value = loadState.message,
                color = Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun TimerDemoSection(
    state: LaunchedEffectUiState,
    viewModel: LaunchedEffectViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("⏱ Demo: LaunchedEffect as a timer")

        CodeBlock("""// LaunchedEffect tied to isRunning state
LaunchedEffect(state.isTimerRunning) {
    if (state.isTimerRunning) {
        while (true) {
            delay(1000)
            viewModel.incrementTimer()
        }
    }
    // When isTimerRunning becomes false:
    // → LaunchedEffect restarts with new key (false)
    // → Old while(true) coroutine is CANCELLED
    // → New coroutine body is just empty (if block is false)
}""")

        Text(
            "${state.timerSeconds}",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PlaygroundButton(
                text = if (state.isTimerRunning) "Stop" else "Start",
                onClick = { if (state.isTimerRunning) viewModel.stopTimer() else viewModel.startTimer() }
            )
            PlaygroundButton(
                text = "Reset",
                onClick = { viewModel.resetTimer() },
                isDestructive = true
            )
        }
    }
}

@Composable
private fun KeyBehaviorSection(
    state: LaunchedEffectUiState,
    viewModel: LaunchedEffectViewModel
) {
    var recompositionCount by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔑 Key Behavior: Recomposition vs Effect restart")

        TheoryBox(
            title = "Critical Distinction",
            content = "Recomposition ≠ Effect restart.\n\n" +
                    "The composable can recompose 100 times, but LaunchedEffect only restarts " +
                    "when its KEY changes. This is the entire point — side effects are isolated " +
                    "from normal recomposition noise."
        )

        // This composable recomposes when badDemoCounter changes
        // but LaunchedEffect(Unit) does NOT restart
        LaunchedEffect(Unit) {
            // This runs ONCE only, regardless of how many times the composable recomposes
        }

        recompositionCount++ // Increments on every recomposition
        RecompositionBadge(count = recompositionCount)

        Text(
            "Counter: ${state.badDemoCounter}",
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "↑ This triggers recomposition, but LaunchedEffect(Unit) doesn't restart",
            fontSize = 12.sp,
            color = Color.Gray
        )

        PlaygroundButton(
            text = "Trigger Recomposition (not effect restart)",
            onClick = { viewModel.triggerBadDemoRecomposition() }
        )
    }
}

@Composable
private fun LogSection(
    state: LaunchedEffectUiState,
    viewModel: LaunchedEffectViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("📋 Composition Log")
            TextButton(onClick = { viewModel.clearLogs() }) {
                Text("Clear")
            }
        }
        LogConsole(
            logs = state.logs.map { LogEntry(it) },
            modifier = Modifier.heightIn(min = 120.dp, max = 250.dp)
        )
    }
}

@Composable
private fun ChallengeSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🎯 Challenges")

        ChallengeBox(
            challengeText = """Challenge 1 (Easy): 
Fix this composable so data reloads when searchQuery changes:

@Composable
fun SearchScreen(searchQuery: String) {
    LaunchedEffect(Unit) {
        viewModel.search(searchQuery) 
    }
}""",
            hint = "The key should match the parameter that drives the effect"
        )

        Spacer(Modifier.height(8.dp))

        ChallengeBox(
            challengeText = """Challenge 2 (Medium): 
This timer is broken — it keeps restarting on every recomposition. Why? How would you fix it?

@Composable  
//fun BadTimer() {
//    var count by remember { mutableIntStateOf(0) }
//    var ticks by remember { mutableIntStateOf(0) }
//    
//    LaunchedEffect(ticks) { // ← ticks changes every second!
//        delay(1000)
//        ticks++
//        count++
//    }
//    
//    Button(onClick = { count++ }) { Text("Count: count, Ticks: ticks") }
//}""",
            hint = "What key would ensure the timer loop runs once and doesn't restart on each tick?"
        )

        Spacer(Modifier.height(8.dp))

        ChallengeBox(
            challengeText = """Challenge 3 (Hard):
You need to load data when userId changes BUT only after a 500ms debounce 
(avoid hitting the API on every keystroke). Implement this with LaunchedEffect.""",
            hint = "delay() inside LaunchedEffect is cancelled if the key changes before the delay completes"
        )
    }
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        val qas = listOf(
            "What happens when the key passed to LaunchedEffect changes?" to
                    "The currently running coroutine is cancelled (cooperative cancellation via CancellationException), then a new coroutine is launched with the block body re-executed from the start.",

            "What's the difference between LaunchedEffect(Unit) and LaunchedEffect(viewModel)?" to
                    "LaunchedEffect(Unit) runs exactly once when the composable enters composition — Unit never changes so the effect never restarts. LaunchedEffect(viewModel) also runs once in practice (ViewModel instance doesn't change across recompositions), but semantically signals the effect is tied to that ViewModel's lifecycle.",

            "Can LaunchedEffect cause an infinite recomposition loop? How?" to
                    "Yes. If the block reads and writes a Compose state, and writing that state triggers recomposition, which changes the key, which restarts the effect... you get a loop. Fix: separate the key from the state being written, or use a ViewModel to manage state mutations.",

            "Is LaunchedEffect cancelled during recomposition?" to
                    "No! Recomposition does not cancel LaunchedEffect unless the key changes or the composable leaves composition. This is by design — effects are stable across recompositions.",

            "When would you use multiple keys in LaunchedEffect?" to
                    "When the effect depends on multiple independent values. E.g., LaunchedEffect(category, sortOrder) ensures the effect restarts whenever either filter changes."
        )

        qas.forEach { (q, a) ->
            InterviewCard(question = q, answer = a)
        }
    }
}
