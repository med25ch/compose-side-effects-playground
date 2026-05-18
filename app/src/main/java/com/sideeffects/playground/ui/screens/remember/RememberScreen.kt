package com.sideeffects.playground.ui.screens.remember

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sideeffects.playground.ui.components.*

@Composable
fun RememberScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = { PlaygroundTopBar("remember & rememberSaveable", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { RememberTheorySection() }
            item { HorizontalDivider() }
            item { RememberDemoSection() }
            item { HorizontalDivider() }
            item { RememberSaveableTheorySection() }
            item { HorizontalDivider() }
            item { RememberSaveableDemoSection() }
            item { HorizontalDivider() }
            item { KeyedRememberSection() }
            item { HorizontalDivider() }
            item { CompositionVsRecompositionSection() }
            item { HorizontalDivider() }
            item { ChallengeSection() }
            item { HorizontalDivider() }
            item { InterviewSection() }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun RememberTheorySection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("📚 remember")
        TheoryBox(
            title = "Survive recomposition, lost on configuration change",
            content = """remember { initialValue } stores a value in the Composition tree.

Lifecycle:
• Created: when the composable FIRST enters composition
• Kept: across all recompositions (same composition slot)
• Lost: when the composable leaves composition
• Lost: on configuration change (rotation, locale change)

remember is NOT state — it's a holder for any value:
• remember { mutableStateOf(0) } — state + holder
• remember { SomeExpensiveObject() } — object lifecycle tied to composition
• remember { listOf(1,2,3) } — stable immutable value

Slot table:
Compose stores remember values in a 'slot table' indexed by position
in the source code. Each call site gets its own slot.
Moving the composable to a different position = different slot = new value."""
        )
    }
}

@Composable
private fun RememberDemoSection() {
    // NOT remembered — recreated on every recomposition
    var notRemembered = 0

    // Remembered — survives recompositions
    var remembered by remember { mutableIntStateOf(0) }

    var triggerRecomp by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Demo: remember vs no remember")

        BadCodeBox(
            title = "❌ Not remembered — resets on every recomposition",
            code = """@Composable
fun Counter() {
    // This is a regular variable — reset to 0 every recomposition!
    var count = 0
    
    Button(onClick = { count++ }) {
        Text("Count: ${'$'}count") // Always shows 0
    }
}""",
            explanation = "A regular variable is initialized fresh on every call to the " +
                    "composable function. Clicking doesn't increment because the next frame " +
                    "resets it to 0."
        )

        GoodCodeBox(
            title = "✅ Remembered — survives recompositions",
            code = """@Composable  
fun Counter() {
    var count by remember { mutableIntStateOf(0) }
    // count survives recompositions — stored in slot table
    
    Button(onClick = { count++ }) {
        Text("Count: ${'$'}count") // Correctly increments
    }
}""",
            explanation = "remember stores the MutableState in the composition's slot table. " +
                    "The same State object is returned on every recomposition, so mutations persist."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Not remembered:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Value: $notRemembered", color = Color(0xFFC62828))
                Text("(always 0)", fontSize = 11.sp, color = Color.Gray)
                Button(onClick = {
                    notRemembered++
                    triggerRecomp++ // Trigger recomposition
                }) { Text("Increment") }
            }
            Column(Modifier.weight(1f)) {
                Text("Remembered:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text("Value: $remembered", color = Color(0xFF2E7D32))
                Text("(persists!)", fontSize = 11.sp, color = Color.Gray)
                Button(onClick = { remembered++ }) { Text("Increment") }
            }
        }
    }
}

@Composable
private fun RememberSaveableTheorySection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("📚 rememberSaveable")
        TheoryBox(
            title = "Survive recomposition AND configuration changes",
            content = """rememberSaveable is like remember but with persistence through:
• Recompositions ✓
• Configuration changes (rotation, dark mode toggle, locale) ✓  
• Process death (with SavedStateHandle) ✓

How it works:
Saves data to the Bundle (same mechanism as onSaveInstanceState).
On restoration, the Bundle is read and the value is restored.

Supported types natively:
• All Bundle-serializable primitives (Int, String, Boolean, etc.)
• Parcelable objects
• Serializable objects

For custom types, provide a Saver:
rememberSaveable(stateSaver = UserSaver) { User() }

When to use rememberSaveable:
• Form fields (text input that user typed)
• Selected tab / scroll position
• Checkbox/toggle state
• ANY UI state the user would be frustrated to lose on rotation"""
        )
    }
}

@Composable
private fun RememberSaveableDemoSection() {
    // remember — lost on rotation
    var regularCount by remember { mutableIntStateOf(0) }

    // rememberSaveable — survives rotation
    var savedCount by rememberSaveable { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔬 Demo: Rotation survival")

        TheoryBox(
            title = "Test this!",
            content = "Increment both counters, then rotate your device (or change dark/light mode). " +
                    "The 'remember' counter resets to 0. The 'rememberSaveable' counter keeps its value."
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("remember", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "$regularCount",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828)
                    )
                    Text("Lost on rotation", fontSize = 11.sp, color = Color.Gray)
                    Button(onClick = { regularCount++ }) { Text("+1") }
                }
            }
            Card(modifier = Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("rememberSaveable", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "$savedCount",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text("Survives rotation!", fontSize = 11.sp, color = Color.Gray)
                    Button(onClick = { savedCount++ }) { Text("+1") }
                }
            }
        }
    }
}

@Composable
private fun KeyedRememberSection() {
    var selectedTab by remember { mutableIntStateOf(0) }

    // Key-based remember — resets when tab changes
    var tabLocalCount by remember(selectedTab) { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🔑 Keyed remember")

        TheoryBox(
            title = "remember(key) — resets when key changes",
            content = """remember can take a key, just like LaunchedEffect:
remember(key) { initialValue }

When the key changes:
• The remembered value is DISCARDED
• A fresh value is created from the lambda

Use case: state that should reset when context changes.
E.g., a text field that should clear when switching tabs."""
        )

        CodeBlock("""// Counter that resets when you switch tabs
var tabLocalCount by remember(selectedTab) { mutableIntStateOf(0) }
// When selectedTab changes → tabLocalCount is re-initialized to 0""")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (0..2).forEach { tab ->
                Button(
                    onClick = { selectedTab = tab },
                    colors = if (selectedTab == tab) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
                ) { Text("Tab $tab") }
            }
        }

        Text(
            "Tab $selectedTab count: $tabLocalCount",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            "(switches tab resets the counter via remember(selectedTab))",
            fontSize = 12.sp,
            color = Color.Gray
        )

        Button(onClick = { tabLocalCount++ }) { Text("Increment this tab's counter") }
    }
}

@Composable
private fun CompositionVsRecompositionSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🧠 Composition vs Recomposition")

        TheoryBox(
            title = "The mental model",
            content = """Composition = first time a composable runs (initial draw)
Recomposition = composable runs again due to state change

remember {} initializes ONCE during composition.
On recompositions, it returns the SAME instance.

This is why:
• remember { ExpensiveObject() } — ExpensiveObject created ONCE
• The same object is returned on every recomposition
• If the composable leaves and re-enters: NEW instance created

The slot table:
Compose maintains a 'slot table' — a list of slots keyed by 
source code position (call site). Each remember {} gets a slot.
Recomposition = same slots = same values.
New composition position = new slot = new value."""
        )
    }
}

@Composable
private fun ChallengeSection() {
    ChallengeBox(
        challengeText = """Challenge 1: 
What does this code do and what's wrong with it?

@Composable
fun FormScreen() {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    
    // User fills in name and email, then rotates device
    // What happens? How would you fix it?
}

---

Challenge 2 (Custom Saver):
Implement a rememberSaveable for this custom class:

data class CartItem(val productId: String, val quantity: Int)

// Create a Saver for CartItem and use it:
val cartItem by rememberSaveable(stateSaver = ???) {
    mutableStateOf(CartItem("prod_1", 1))
}""",
        hint = "For Challenge 2: Saver has save() and restore() functions. " +
                "Convert CartItem to a Bundle-serializable form (a List or Map) in save(), " +
                "and reconstruct from it in restore()."
    )
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        InterviewCard(
            question = "What's the difference between remember and rememberSaveable?",
            answer = "remember survives recompositions but is lost on configuration changes (rotation). rememberSaveable additionally saves the value to the Bundle (equivalent to onSaveInstanceState), so it survives rotation, dark mode toggles, and process death (when combined with SavedStateHandle)."
        )
        InterviewCard(
            question = "What types can rememberSaveable automatically save?",
            answer = "All Bundle-compatible types: primitives (Int, String, Boolean, Float, etc.), Parcelable, and Serializable. For custom types that aren't in this list, you must provide a custom Saver using stateSaver = parameter."
        )
        InterviewCard(
            question = "When would you use remember { mutableStateOf() } vs ViewModel + StateFlow?",
            answer = "remember { mutableStateOf() } is for UI-local state that doesn't need to outlive the composable — things like whether a dialog is open, a text field draft, local toggle state. ViewModel + StateFlow is for screen-level state that survives rotation, needs to be shared, involves business logic, or needs to be observed from multiple composables."
        )
        InterviewCard(
            question = "How does Compose know which remember value belongs to which call site?",
            answer = "Compose uses the 'slot table' — a table indexed by the source code position (group key) of each composable call. Each remember {} call gets a slot determined by its position in the code. This is why you can't call remember {} conditionally — the slot index would be inconsistent across recompositions, causing Compose to read the wrong slot."
        )
    }
}
