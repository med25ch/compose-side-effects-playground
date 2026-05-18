package com.sideeffects.playground.ui.screens.remembercoroutinescope

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sideeffects.playground.ui.components.*
import kotlinx.coroutines.launch

@Composable
fun RememberCoroutineScopeScreen(
    onBack: () -> Unit,
    viewModel: RememberCoroutineScopeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar when message changes
    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        topBar = { PlaygroundTopBar("rememberCoroutineScope", onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                TheoryBox(
                    title = "rememberCoroutineScope",
                    content = """Returns a CoroutineScope bound to the composition.
                        
Key differences from LaunchedEffect:
• LaunchedEffect = DECLARATIVE → launches automatically on composition
• rememberCoroutineScope = IMPERATIVE → YOU decide when to launch

Use rememberCoroutineScope when:
• Launching a coroutine from a click handler / event callback
• You need to call scope.launch {} from a lambda
• Controlling animations imperatively
• Showing Snackbars (snackbarHostState.showSnackbar is a suspend function)

The scope is cancelled when the composable leaves composition."""
                )
            }

            item { HorizontalDivider() }

            item {
                BadCodeBox(
                    title = "❌ Anti-Pattern: Creating a scope manually",
                    code = """@Composable
fun BadButton() {
    // MEMORY LEAK: This scope is NEVER cancelled!
    // CoroutineScope() has no lifecycle awareness
    val scope = CoroutineScope(Dispatchers.Main)
    
    Button(onClick = {
        scope.launch {
            api.doSomething()
        }
    }) { Text("Click") }
    
    // When composable leaves: scope lives on forever
    // Any launched coroutine keeps running
}""",
                    explanation = "CoroutineScope() creates a scope with no lifecycle owner. " +
                            "It's never cancelled when the composable leaves, causing memory leaks " +
                            "and potential crashes from coroutines accessing disposed resources."
                )
            }

            item {
                BadCodeBox(
                    title = "❌ Anti-Pattern: Using LaunchedEffect for click handlers",
                    code = """@Composable
fun BadClickHandler() {
    var clicked by remember { mutableStateOf(false) }
    
    // Awkward state-based trigger — anti-pattern
    LaunchedEffect(clicked) {
        if (clicked) {
            snackbarHostState.showSnackbar("Clicked!")
            clicked = false // Reset... but this triggers another recomposition
        }
    }
    
    Button(onClick = { clicked = true }) { Text("Click") }
}""",
                    explanation = "This abuses LaunchedEffect as an event handler. " +
                            "It requires artificial state to 'signal' the effect, adds recompositions, " +
                            "and has a reset problem. rememberCoroutineScope is the right tool."
                )
            }

            item {
                GoodCodeBox(
                    title = "✅ Correct: rememberCoroutineScope for event handlers",
                    code = """@Composable
fun GoodButton() {
    val scope = rememberCoroutineScope()
    val snackbarState = remember { SnackbarHostState() }
    
    Button(onClick = {
        // Direct, clean, properly scoped
        scope.launch {
            snackbarState.showSnackbar("Saved successfully!")
        }
    }) {
        Text("Save")
    }
    
    SnackbarHost(hostState = snackbarState)
    // When composable leaves: scope.cancel() is called automatically
    // All in-flight coroutines are cancelled — no leaks
}""",
                    explanation = "rememberCoroutineScope creates a scope tied to the composition. " +
                            "Cancelled automatically when the composable leaves. " +
                            "You get full control over WHEN to launch, which is what click handlers need."
                )
            }

            item { HorizontalDivider() }

            item { SectionHeader("🔬 Live Demo: Snackbar + Upload") }

            item {
                TheoryBox(
                    title = "The Snackbar Pattern",
                    content = "snackbarHostState.showSnackbar() is a suspend function. " +
                            "It needs a coroutine scope. Since it's called from a click handler (not " +
                            "composition), rememberCoroutineScope is the correct tool here."
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                // rememberCoroutineScope used in the SCREEN composable above
                                // to launch the upload
                                viewModel.uploadFile()
                            },
                            enabled = !state.isUploading
                        ) {
                            if (state.isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (state.isUploading) "Uploading..." else "Upload File")
                        }

                        // Direct scope.launch in onClick — correct pattern
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Manual snackbar from scope.launch!")
                                }
                            }
                        ) {
                            Text("Show Snackbar")
                        }
                    }

                    state.uploadResult?.let {
                        StatusIndicator(
                            label = "Last upload",
                            value = it,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader("📋 Log")
                        TextButton(onClick = { viewModel.clearLogs() }) { Text("Clear") }
                    }
                    LogConsole(logs = state.logs.map { LogEntry(it) })
                }
            }

            item { HorizontalDivider() }

            item {
                ChallengeBox(
                    challengeText = """Challenge: You have a form with a "Submit" button. 
On submit:
1. Show a loading indicator
2. Call api.submit() (suspend function)
3. Show success snackbar on success
4. Show error snackbar on failure
5. The user can navigate away mid-submit — the request should be cancelled

Implement this using rememberCoroutineScope. 
What happens to the in-flight request if the user navigates away?""",
                    hint = "rememberCoroutineScope is cancelled when the composable leaves — " +
                            "any job launched on it is also cancelled. Your loading state needs to be " +
                            "reset properly though — consider a try/finally block."
                )
            }

            item { HorizontalDivider() }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader("🎤 Interview Questions")
                    InterviewCard(
                        question = "When should you use rememberCoroutineScope vs LaunchedEffect?",
                        answer = "rememberCoroutineScope: when you need to launch a coroutine imperatively from an event callback (onClick, onValueChange). LaunchedEffect: when you want a coroutine to run declaratively as part of composition — tied to state/lifecycle rather than user events."
                    )
                    InterviewCard(
                        question = "What happens to jobs launched on rememberCoroutineScope when the composable is removed?",
                        answer = "The scope is cancelled, which propagates cancellation to all child coroutines via structured concurrency. Any in-flight work (API calls, delays) receives a CancellationException and stops. This is the memory safety guarantee."
                    )
                    InterviewCard(
                        question = "Can I use rememberCoroutineScope to collect a Flow?",
                        answer = "Technically yes, but it's wrong. rememberCoroutineScope.launch { flow.collect {} } would work but you lose the key-restart behavior. Use LaunchedEffect for flow collection so it properly restarts when keys change."
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
