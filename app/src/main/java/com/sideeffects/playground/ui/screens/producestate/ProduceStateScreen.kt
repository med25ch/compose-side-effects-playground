package com.sideeffects.playground.ui.screens.producestate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sideeffects.playground.domain.model.LoadState
import com.sideeffects.playground.ui.components.*
import com.sideeffects.playground.util.currentTime
import com.sideeffects.playground.util.simulateNetworkCall
import kotlinx.coroutines.delay

@Composable
fun ProduceStateScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { PlaygroundTopBar("produceState", onBack) }
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
            item { ComparisonSection() }
            item { HorizontalDivider() }
            item { SimpleDemo() }
            item { HorizontalDivider() }
            item { LoadingDemo() }
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
        SectionHeader("📚 produceState")
        TheoryBox(
            title = "Convert async sources into Compose State",
            content = """produceState is a convenience wrapper combining remember {} + LaunchedEffect.

Signature:
val state by produceState(
    initialValue = defaultValue,
    key1, key2  // optional keys — same semantics as LaunchedEffect
) {
    // You're in a coroutine scope
    value = someAsyncOperation()
    // 'value' is the mutable state reference
}

Under the hood:
val state = remember { mutableStateOf(initialValue) }
LaunchedEffect(key) {
    // producer block, with 'value' = state
}

When to use:
• Loading data from suspend functions into Compose State
• Converting flows/callbacks to State without a ViewModel
• Creating self-contained data-loading composables
• Stateful composables that manage their own data fetching

The composable starts with initialValue and updates value as async work completes."""
        )
    }
}

@Composable
private fun ComparisonSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("⚖️ produceState vs remember + LaunchedEffect")

        BadCodeBox(
            title = "Without produceState — verbose",
            code = """@Composable
fun UserCard(userId: String) {
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(userId) {
        isLoading = true
        error = null
        try {
            user = repository.getUser(userId)
        } catch (e: Exception) {
            error = e.message
        } finally {
            isLoading = false
        }
    }
    
    when {
        isLoading -> CircularProgressIndicator()
        error != null -> Text("Error: ${'$'}error")
        user != null -> Text(user!!.name)
    }
}""",
            explanation = "This works but is verbose — 3 separate state variables just for " +
                    "one async load. produceState can compress this into a single state."
        )

        GoodCodeBox(
            title = "✅ With produceState — concise",
            code = """sealed class UserState {
    object Loading : UserState()
    data class Success(val user: User) : UserState()
    data class Error(val msg: String) : UserState()
}

@Composable
fun UserCard(userId: String) {
    val userState by produceState<UserState>(
        initialValue = UserState.Loading,
        key1 = userId
    ) {
        value = UserState.Loading
        value = try {
            UserState.Success(repository.getUser(userId))
        } catch (e: Exception) {
            UserState.Error(e.message ?: "Unknown error")
        }
    }
    
    when (val state = userState) {
        is UserState.Loading -> CircularProgressIndicator()
        is UserState.Error -> Text("Error: ${'$'}{state.msg}")
        is UserState.Success -> Text(state.user.name)
    }
}""",
            explanation = "produceState reduces the 3-variable boilerplate to a single " +
                    "sealed state. Cleaner, more testable, and expresses intent clearly."
        )
    }
}

@Composable
private fun SimpleDemo() {
    var key by remember { mutableIntStateOf(1) }

    // Simple produceState demo — ticks every second
    val timerState by produceState(initialValue = 0, key1 = key) {
        // 'value' is the MutableState<Int>
        while (true) {
            delay(1000)
            value++ // Updates the state directly
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Demo 1: Timer via produceState")

        CodeBlock("""val timerState by produceState(initialValue = 0, key1 = key) {
    while (true) {
        delay(1000)
        value++ // 'value' is the mutable state inside produce
    }
}
// Changing 'key' restarts the timer from 0""")

        StatusIndicator(
            label = "Timer",
            value = "${timerState}s (key=$key)",
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { key++ }) {
                Text("Restart Timer (change key)")
            }
        }

        Text(
            "Changing key causes produceState to restart — same as LaunchedEffect key behavior",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

@Composable
private fun LoadingDemo() {
    var userId by remember { mutableIntStateOf(1) }

    // produceState for network loading with LoadState
    val loadState by produceState<LoadState<String>>(
        initialValue = LoadState.Loading,
        key1 = userId
    ) {
        value = LoadState.Loading
        val result = simulateNetworkCall(1500)
        value = result.fold(
            onSuccess = { LoadState.Success("User $userId data: $it") },
            onFailure = { LoadState.Error(it.message ?: "Unknown") }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Demo 2: Network loading")

        CodeBlock("""val loadState by produceState<LoadState<String>>(
    initialValue = LoadState.Loading,
    key1 = userId
) {
    value = LoadState.Loading
    val result = api.loadUser(userId) // suspend
    value = result.fold(
        onSuccess = { LoadState.Success(it) },
        onFailure = { LoadState.Error(it.message) }
    )
}""")

        Text("Current userId: $userId", style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..3).forEach { id ->
                Button(
                    onClick = { userId = id },
                    colors = if (userId == id) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("User $id") }
            }
        }

        when (val state = loadState) {
            is LoadState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Loading user $userId...")
            }
            is LoadState.Success -> StatusIndicator(
                label = "Result", value = state.data, color = Color(0xFF2E7D32)
            )
            is LoadState.Error -> StatusIndicator(
                label = "Error", value = state.message, color = Color(0xFFC62828)
            )
            else -> {}
        }
    }
}

@Composable
private fun ChallengeSection() {
    ChallengeBox(
        challengeText = """Challenge 1:
Implement a composable that shows the current time, updating every second, using produceState:

@Composable
fun LiveClock(): String {
    // Implement using produceState
    // Should show HH:mm:ss, updating every second
}

Challenge 2:
Convert this callback-based API to produceState:

// Legacy callback API
fun observeUserPresence(userId: String, callback: (Boolean) -> Unit): Disposable

@Composable  
fun UserPresenceIndicator(userId: String) {
    // Use produceState with awaitDispose for cleanup
}""",
        hint = "For the callback challenge: use awaitDispose {} inside produceState to " +
                "register the cleanup of the Disposable when the effect ends."
    )
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        InterviewCard(
            question = "What is produceState and how does it differ from remember + LaunchedEffect?",
            answer = "produceState is syntactic sugar that combines remember { mutableStateOf(initialValue) } + LaunchedEffect. The key difference is ergonomics: produceState gives you a 'value' setter inside the coroutine block, making async-to-State conversions more concise. Functionally identical."
        )
        InterviewCard(
            question = "When would you prefer produceState over a ViewModel StateFlow?",
            answer = "produceState is better for composable-local state that doesn't need to survive configuration changes, doesn't need to be shared, and is tightly coupled to a composable's inputs. ViewModel + StateFlow is better for screen-level state that survives rotation, is shared across composables, or has complex business logic."
        )
        InterviewCard(
            question = "How do you handle cleanup in produceState?",
            answer = "Use awaitDispose {} inside the producer block. This is a suspend function that suspends until the effect is disposed, at which point the lambda runs. Example: awaitDispose { disposable.dispose() }. This is the equivalent of DisposableEffect's onDispose."
        )
    }
}
