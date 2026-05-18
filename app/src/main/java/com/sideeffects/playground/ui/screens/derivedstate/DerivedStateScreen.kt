package com.sideeffects.playground.ui.screens.derivedstate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sideeffects.playground.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun DerivedStateScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { PlaygroundTopBar("derivedStateOf", onBack) }
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
            item { BadVsGoodSection() }
            item { HorizontalDivider() }
            item { ScrollFabDemo() }
            item { HorizontalDivider() }
            item { FilterDemo() }
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
        SectionHeader("📚 derivedStateOf")
        TheoryBox(
            title = "Prevent over-recomposition from derived calculations",
            content = """derivedStateOf computes a value that depends on other State objects,
but only triggers recomposition when the RESULT changes.

Without derivedStateOf:
• Subscribing to a LazyListState reads every pixel of scroll offset
• Every scroll event → recomposition of anything reading the state
• Even if your "show FAB" boolean hasn't changed

With derivedStateOf:
• The calculation runs on every change of the source state
• But recomposition only happens when the calculated VALUE changes
• scrollOffset changes 1000 times → but showFab only flips twice (true/false)
→ only 2 recompositions instead of 1000

Signature:
val derivedValue by remember {
    derivedStateOf { /* calculation from other State */ }
}

MUST be wrapped in remember {} otherwise a new DerivedState is 
created on every recomposition, defeating the purpose entirely."""
        )
    }
}

@Composable
private fun BadVsGoodSection() {
    var badRecompCount by remember { mutableIntStateOf(0) }
    var goodRecompCount by remember { mutableIntStateOf(0) }
    val scrollPosition = remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("⚖️ Bad vs Good: Scroll-based FAB")

        BadCodeBox(
            title = "❌ Over-recomposing: direct state read",
            code = """@Composable
fun BadScrollScreen() {
    val listState = rememberLazyListState()
    
    // This reads listState on EVERY scroll pixel
    // Every scroll offset change → recomposition of this composable
    val showFab = listState.firstVisibleItemIndex > 0
    //            ↑ Direct read = subscription to ALL changes
    
    Box {
        LazyColumn(state = listState) { /* items */ }
        if (showFab) {
            FloatingActionButton(onClick = { /* scroll to top */ }) {
                Icon(Icons.Default.ArrowUpward, null)
            }
        }
    }
}""",
            explanation = "Every scroll event reads listState directly, triggering recomposition " +
                    "of the entire composable. The FAB visibility only changes twice (appears/disappears), " +
                    "but the entire screen recomposes on every pixel scrolled."
        )

        Spacer(Modifier.height(8.dp))

        GoodCodeBox(
            title = "✅ derivedStateOf: recomposes only when FAB visibility changes",
            code = """@Composable
fun GoodScrollScreen() {
    val listState = rememberLazyListState()
    
    // derivedStateOf: reads listState INSIDE the lambda
    // But only causes recomposition when the BOOLEAN result changes
    val showFab by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    // ↑ showFab is only true or false — 
    //   recomposition only on the transition, not every scroll pixel
    
    Box {
        LazyColumn(state = listState) { /* items */ }
        if (showFab) {
            FloatingActionButton(onClick = { /* scroll to top */ }) {
                Icon(Icons.Default.ArrowUpward, null)
            }
        }
    }
}""",
            explanation = "The derivedStateOf lambda still executes on every scroll event, " +
                    "BUT the result (true/false) only changes twice per scroll. " +
                    "Recomposition is triggered ONLY when true↔false flips, not on every pixel."
        )
    }
}

@Composable
private fun ScrollFabDemo() {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var badRecompCount by remember { mutableIntStateOf(0) }
    var goodRecompCount by remember { mutableIntStateOf(0) }

    // BAD version (for comparison — counts recompositions)
    val showFabBad = listState.firstVisibleItemIndex > 0 // direct read

    // GOOD version
    val showFabGood by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // Count when this composable recomposes
    badRecompCount++ // Direct read causes this to increment on every scroll

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Live Demo: FAB with scroll")

        TheoryBox(
            title = "Scroll the list below and watch the recomposition counters",
            content = "The bad version reads scroll state directly — recomposes on every pixel. " +
                    "The good version uses derivedStateOf — only recomposes when FAB visibility changes."
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            RecompositionBadge(count = badRecompCount)
        }

        Box(modifier = Modifier.height(250.dp)) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items((1..30).toList()) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "Item $item",
                            Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (showFabGood) {
                FloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Scroll to top", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun FilterDemo() {
    var searchText by remember { mutableStateOf("") }
    var recompCount by remember { mutableIntStateOf(0) }

    val allItems = remember {
        (1..20).map { "Item $it: ${listOf("Apple", "Banana", "Cherry", "Date", "Elderberry").random()} description" }
    }

    // derivedStateOf for expensive filtering
    val filteredItems by remember {
        derivedStateOf {
            if (searchText.isBlank()) allItems
            else allItems.filter { it.contains(searchText, ignoreCase = true) }
        }
    }

    recompCount++

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Demo 2: Filtered list")

        CodeBlock("""val filteredItems by remember {
    derivedStateOf {
        // This expensive filter runs on every searchText change
        // BUT only recomposes the UI when the RESULT changes
        allItems.filter { it.contains(searchText, ignoreCase = true) }
    }
}
// If you type "Item 1" then delete "1" → filter result changes → recomposition
// If you type a character that doesn't change the result → no recomposition""")

        RecompositionBadge(count = recompCount)
        StatusIndicator(
            label = "Filtered count",
            value = "${filteredItems.size} of ${allItems.size}",
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            label = { Text("Search items") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        filteredItems.take(5).forEach { item ->
            Text("• $item", style = MaterialTheme.typography.bodySmall)
        }
        if (filteredItems.size > 5) {
            Text("... and ${filteredItems.size - 5} more", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ChallengeSection() {
    ChallengeBox(
        challengeText = """Challenge 1 (Important):
This code has a bug — why doesn't derivedStateOf work correctly here?

@Composable
fun BuggyScreen() {
    val listState = rememberLazyListState()
    
    // BUG: derivedStateOf without remember {}
    val showFab by derivedStateOf {  // ← Missing remember!
        listState.firstVisibleItemIndex > 0
    }
}

What's the bug and what's the fix?

---

Challenge 2 (Advanced):
Implement a composable that shows a header as "sticky" (elevated) when 
the list has been scrolled past the first item, using derivedStateOf.
Use isScrollInProgress as a secondary condition.""",
        hint = "Without remember {}, a new DerivedState object is created on every recomposition, " +
                "which means the derivedStateOf calculation runs fresh every time — " +
                "you lose all the performance benefits."
    )
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        InterviewCard(
            question = "What is derivedStateOf and why is it a performance optimization?",
            answer = "derivedStateOf creates a computed State whose value is derived from other State objects. The key optimization: the calculation lambda runs whenever source states change, but recomposition is only triggered when the RESULT of the calculation changes. This decouples 'how often the input changes' from 'how often the UI needs to redraw'."
        )
        InterviewCard(
            question = "Why must derivedStateOf be wrapped in remember {}?",
            answer = "Without remember {}, a new DerivedState object is created on every recomposition. The old DerivedState is thrown away, so no observation history is kept. You get no caching benefit — it's equivalent to computing the value directly. The remember {} ensures the same DerivedState object persists across recompositions."
        )
        InterviewCard(
            question = "What's the difference between derivedStateOf and a regular computed property?",
            answer = "A regular computed property (val x = a + b where a and b are State) recomposes every time a or b changes. derivedStateOf(a + b) also reads a and b, but only recomposes when x changes. If a = 1, b = 2, x = 3... if a becomes 2 and b becomes 1 (x is still 3), no recomposition."
        )
        InterviewCard(
            question = "Can derivedStateOf be used with non-Compose state (e.g., a regular variable)?",
            answer = "No. derivedStateOf only tracks Compose State objects (StateFlow collected into State, remember { mutableStateOf() }, etc.). A regular Kotlin variable inside the lambda won't be tracked. The whole system relies on Compose's snapshot-based state observation."
        )
    }
}
