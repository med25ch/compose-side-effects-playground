package com.sideeffects.playground.ui.screens.snapshotflow

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sideeffects.playground.ui.components.*
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest

@Composable
fun SnapshotFlowScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { PlaygroundTopBar("snapshotFlow", onBack) }
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
            item { SnapshotVsDerivedSection() }
            item { HorizontalDivider() }
            item { DebounceDemo() }
            item { HorizontalDivider() }
            item { ScrollTrackingDemo() }
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
        SectionHeader("📚 snapshotFlow")
        TheoryBox(
            title = "Convert Compose State into a cold Flow",
            content = """snapshotFlow is the bridge between Compose's snapshot system and Kotlin Flows.

It creates a COLD Flow that:
• Emits the initial value immediately when collected
• Emits a new value whenever the Compose State read inside the block changes
• Applies Flow operators (debounce, distinctUntilChanged, filter, etc.)

Signature:
val flow: Flow<T> = snapshotFlow {
    // Read Compose State here
    // Returns the current value
    myState.value
}

Key difference from derivedStateOf:
• derivedStateOf → Compose State (triggers recomposition)
• snapshotFlow → Kotlin Flow (can use all Flow operators)

When to use:
• Debouncing search queries (snapshotFlow + debounce())
• Filtering state changes (only emit when above threshold)
• Applying reactive Flow transformations to Compose State
• Sending Compose state to a non-Compose reactive pipeline"""
        )
    }
}

@Composable
private fun SnapshotVsDerivedSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("⚖️ snapshotFlow vs derivedStateOf")

        TheoryBox(
            title = "Choose based on what you need the result to be",
            content = """derivedStateOf → when you need a Compose State (for UI)
snapshotFlow → when you need a Flow (for reactive pipelines)

derivedStateOf:
  val showFab by remember {
    derivedStateOf { scrollState.firstVisibleItemIndex > 0 }
  }
  // showFab is a Boolean State → directly usable in Compose

snapshotFlow:
  LaunchedEffect(Unit) {
    snapshotFlow { searchText }
      .debounce(300)
      .collect { query -> viewModel.search(query) }
  }
  // Converts state to Flow → use Flow operators → trigger ViewModel
  
Rule of thumb:
• Result is used in UI → derivedStateOf
• Result triggers side effects or needs Flow operators → snapshotFlow"""
        )
    }
}

@Composable
private fun DebounceDemo() {
    var searchText by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var searchCount by remember { mutableIntStateOf(0) }
    val logs = remember { mutableStateListOf<String>() }

    // KEY PATTERN: snapshotFlow to debounce search input
    LaunchedEffect(Unit) {
        snapshotFlow { searchText }
            .debounce(500)
            .distinctUntilChanged()
            .filter { it.isNotBlank() }
            .collect { query ->
                searchCount++
                debouncedQuery = query
                logs.add("[$searchCount] API search: \"$query\"")
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Demo 1: Debounced Search")

        GoodCodeBox(
            title = "✅ snapshotFlow + debounce — classic pattern",
            code = """LaunchedEffect(Unit) {
    snapshotFlow { searchText }  // Convert State → Flow
        .debounce(500)            // Wait 500ms after last change
        .distinctUntilChanged()   // Don't search for same query
        .filter { it.isNotBlank() } // Skip empty
        .collect { query ->
            viewModel.search(query) // Only triggers after debounce
        }
}""",
            explanation = "Every keystroke updates searchText immediately (for UI), " +
                    "but the API call only fires after 500ms of no typing. " +
                    "Without snapshotFlow, you can't apply debounce to Compose State."
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Type to search (500ms debounce)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        StatusIndicator(
            label = "Current input",
            value = "\"$searchText\"",
            color = MaterialTheme.colorScheme.secondary
        )
        StatusIndicator(
            label = "Last debounced query",
            value = if (debouncedQuery.isEmpty()) "(none yet)" else "\"$debouncedQuery\"",
            color = Color(0xFF2E7D32)
        )
        StatusIndicator(
            label = "API calls made",
            value = searchCount.toString(),
            color = Color(0xFF1565C0)
        )

        LogConsole(
            logs = logs.map { LogEntry(it, LogType.SUCCESS) },
            modifier = Modifier.heightIn(min = 80.dp, max = 150.dp)
        )

        Text(
            "Notice: typing fast only triggers one search per burst, not one per keystroke",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun ScrollTrackingDemo() {
    val listState = rememberLazyListState()
    var scrollEvents by remember { mutableIntStateOf(0) }
    var significantScrolls by remember { mutableIntStateOf(0) }
    val logs = remember { mutableStateListOf<String>() }

    // snapshotFlow for scroll tracking with threshold
    LaunchedEffect(Unit) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                scrollEvents++
                if (index % 5 == 0 && index > 0) {
                    significantScrolls++
                    logs.add("Crossed item $index milestone")
                }
            }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Demo 2: Scroll Analytics")

        CodeBlock("""LaunchedEffect(Unit) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .distinctUntilChanged()
        .collect { index ->
            // Only fires when ITEM INDEX changes (not every pixel)
            analytics.trackScroll(index)
        }
}""")

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatusIndicator(
                label = "Index changes",
                value = scrollEvents.toString(),
                color = MaterialTheme.colorScheme.primary
            )
            StatusIndicator(
                label = "Milestones",
                value = significantScrolls.toString(),
                color = Color(0xFF2E7D32)
            )
        }

        Box(modifier = Modifier.height(200.dp)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items((1..30).toList()) { i ->
                    Card(Modifier.fillMaxWidth()) {
                        Text("Scroll item $i", Modifier.padding(12.dp))
                    }
                }
            }
        }

        LogConsole(
            logs = logs.map { LogEntry(it, LogType.INFO) },
            modifier = Modifier.heightIn(min = 60.dp, max = 120.dp)
        )
    }
}

@Composable
private fun ChallengeSection() {
    ChallengeBox(
        challengeText = """Challenge 1:
You have a slider that controls a video player position. 
Implement throttling so the player only seeks when the user stops moving the slider for 300ms.

var sliderPosition by remember { mutableFloatStateOf(0f) }
// Use snapshotFlow to debounce the seek operation

---

Challenge 2 (Hard):
Combine snapshotFlow with mapLatest to implement autocomplete:
• On each query change, cancel the previous API call and start a new one
• Show results only from the LATEST query

Hint: snapshotFlow + mapLatest cancels the previous mapLatest block when a new value arrives.""",
        hint = "mapLatest is like flatMapLatest but cancels the previous transform when a new value arrives. " +
                "Combined with snapshotFlow, you get automatic cancellation of stale requests."
    )
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        InterviewCard(
            question = "What is snapshotFlow and when would you use it over derivedStateOf?",
            answer = "snapshotFlow converts Compose State into a Kotlin Flow, enabling all Flow operators (debounce, distinctUntilChanged, mapLatest, etc.). Use it when you need reactive pipeline capabilities (debouncing, throttling, transformation) rather than just a derived Compose State value."
        )
        InterviewCard(
            question = "Is snapshotFlow a hot or cold Flow?",
            answer = "snapshotFlow is a COLD Flow. It only starts observing Compose State when it has a collector. When the collector cancels, the observation stops. Each new collector gets the current value as its first emission."
        )
        InterviewCard(
            question = "How does snapshotFlow know when to emit?",
            answer = "snapshotFlow participates in Compose's snapshot system. It runs the block inside a snapshot observation context. When any Compose State read inside the block changes (the snapshot is invalidated), snapshotFlow re-runs the block and emits the new value."
        )
        InterviewCard(
            question = "What's the memory/lifecycle management for snapshotFlow?",
            answer = "snapshotFlow itself doesn't manage lifecycle — it's a cold Flow. The lifecycle management comes from WHERE you collect it. Typically inside LaunchedEffect, which scopes collection to the composition. When the composable leaves, the LaunchedEffect coroutine is cancelled, stopping collection."
        )
    }
}
