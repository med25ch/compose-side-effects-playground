package com.sideeffects.playground.ui.screens.disposableeffect

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import com.sideeffects.playground.ui.components.*
import com.sideeffects.playground.util.currentTime

@Composable
fun DisposableEffectScreen(onBack: () -> Unit) {
    val logs = remember { mutableStateListOf<String>() }
    var showSensor by remember { mutableStateOf(false) }
    var showLifecycle by remember { mutableStateOf(false) }

    fun log(msg: String, type: String = "INFO") {
        logs.add("[${currentTime()}] $type: $msg")
    }

    Scaffold(
        topBar = { PlaygroundTopBar("DisposableEffect", onBack) }
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
            item { GoodImplementationSection() }
            item { HorizontalDivider() }

            item {
                SectionHeader("🔬 Demo 1: Lifecycle Observer")
                LifecycleObserverDemo(
                    show = showLifecycle,
                    onToggle = { showLifecycle = it },
                    onLog = { log(it) }
                )
            }

            item { HorizontalDivider() }

            item {
                SectionHeader("🔬 Demo 2: Simulated Sensor")
                SensorDemo(
                    show = showSensor,
                    onToggle = { showSensor = it },
                    onLog = { log(it) }
                )
            }

            item { HorizontalDivider() }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SectionHeader("📋 Event Log")
                        TextButton(onClick = { logs.clear() }) { Text("Clear") }
                    }
                    LogConsole(
                        logs = logs.map { LogEntry(it) },
                        modifier = Modifier.heightIn(min = 120.dp, max = 280.dp)
                    )
                }
            }

            item { HorizontalDivider() }
            item { PhoneticsAnalogy() }
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
        SectionHeader("📚 DisposableEffect")
        TheoryBox(
            title = "Lifecycle-paired side effects",
            content = """DisposableEffect is for side effects that need explicit CLEANUP.

Contract:
1. Effect runs when composable enters (or key changes)
2. onDispose {} runs BEFORE the next effect execution or when leaving composition
3. You MUST provide onDispose — the compiler enforces it

Use when:
• Registering/unregistering listeners (sensors, broadcast receivers)
• Adding/removing lifecycle observers
• Subscribing/unsubscribing to callbacks
• Opening/closing connections that need cleanup

The onDispose guarantee:
• Called before each re-execution (key change)  
• Called when composable leaves composition
• This is the ONLY hook that guarantees cleanup timing"""
        )
    }
}

@Composable
private fun BadImplementationSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("❌ Anti-Patterns: Missing cleanup")

        BadCodeBox(
            title = "❌ Memory Leak: LaunchedEffect without cleanup",
            code = """@Composable  
fun SensorScreen() {
    LaunchedEffect(Unit) {
        // LEAK: listener is registered but never removed!
        sensorManager.registerListener(
            myListener,
            sensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        // LaunchedEffect has no cleanup mechanism
        // When this composable leaves: listener keeps running
        // Battery drain + potential NPE when listener callbacks fire
    }
}""",
            explanation = "LaunchedEffect has no onDispose. Once registered, the sensor " +
                    "listener runs forever — draining battery, and eventually crashing when " +
                    "it tries to update UI from a disposed composable."
        )

        BadCodeBox(
            title = "❌ Double registration: Wrong key",
            code = """@Composable
fun BroadcastScreen(intentFilter: IntentFilter) {
    // BUG: Every recomposition with a new intentFilter object
    // registers ANOTHER receiver without unregistering the old one!
    DisposableEffect(intentFilter) {
        // intentFilter is a new object reference each recomposition
        // even if the content is identical → effect restarts
        context.registerReceiver(receiver, intentFilter)
        onDispose { context.unregisterReceiver(receiver) }
    }
    // Fix: use a stable key like intentFilter.hashCode() or
    // remember { intentFilter } to stabilize the reference
}""",
            explanation = "If intentFilter is created inline (new object each recomposition), " +
                    "the key changes every time, causing unregister+register on every recomposition. " +
                    "Use stable keys or wrap in remember {}."
        )
    }
}

@Composable
private fun GoodImplementationSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("✅ Correct Patterns")

        GoodCodeBox(
            title = "✅ Sensor with guaranteed cleanup",
            code = """@Composable
fun SensorScreen() {
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                // update state
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }
        
        sensorManager.registerListener(
            listener, 
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        
        onDispose {
            // GUARANTEED to run — no leaks
            sensorManager.unregisterListener(listener)
        }
    }
}""",
            explanation = "onDispose is called by the Compose runtime whenever the effect " +
                    "needs to clean up — whether from key change or composable leaving. " +
                    "The sensor is always properly unregistered."
        )

        GoodCodeBox(
            title = "✅ Lifecycle observer — production pattern",
            code = """@Composable
fun LifecycleAwareComponent(
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current
) {
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> analytics.startSession()
                Lifecycle.Event.ON_PAUSE -> analytics.endSession()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}""",
            explanation = "lifecycleOwner is a stable key (doesn't change unless Activity " +
                    "is recreated). The observer is properly removed when the composable leaves " +
                    "or the lifecycleOwner changes (e.g., activity recreation)."
        )
    }
}

@Composable
private fun LifecycleObserverDemo(
    show: Boolean,
    onToggle: (Boolean) -> Unit,
    onLog: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TheoryBox(
            title = "What to observe",
            content = "Toggle the component ON. Then go to your Recents (Home button) and come back. " +
                    "You'll see ON_PAUSE and ON_RESUME events logged. Toggle it OFF — the observer is removed."
        )

        Button(onClick = { onToggle(!show) }) {
            Text(if (show) "Remove Lifecycle Observer" else "Add Lifecycle Observer")
        }

        if (show) {
            LifecycleObservingComponent(onLog = onLog)
        }
    }
}

@Composable
private fun LifecycleObservingComponent(onLog: (String) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        onLog("DisposableEffect: observer REGISTERED")
        val observer = LifecycleEventObserver { _, event ->
            onLog("Lifecycle event: $event")
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            onLog("onDispose: observer REMOVED")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    StatusIndicator(
        label = "Observer",
        value = "Active — watching lifecycle events",
        color = Color(0xFF2E7D32)
    )
}

@Composable
private fun SensorDemo(
    show: Boolean,
    onToggle: (Boolean) -> Unit,
    onLog: (String) -> Unit
) {
    var sensorValue by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TheoryBox(
            title = "Simulated sensor subscription",
            content = "This simulates a sensor with DisposableEffect. Toggle it and watch " +
                    "the log to see when the 'sensor' is registered and unregistered."
        )

        Button(onClick = { onToggle(!show) }) {
            Text(if (show) "Unsubscribe Sensor" else "Subscribe Sensor")
        }

        if (show) {
            SimulatedSensor(
                onValueChange = { sensorValue = it },
                onLog = onLog
            )
            StatusIndicator(
                label = "Sensor value",
                value = "%.2f".format(sensorValue),
                color = Color(0xFF1565C0)
            )
        }
    }
}

@Composable
private fun SimulatedSensor(
    onValueChange: (Float) -> Unit,
    onLog: (String) -> Unit
) {
    // Simulate a sensor that provides values periodically
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(500)
            onValueChange((Math.random() * 10).toFloat())
        }
    }

    DisposableEffect(Unit) {
        onLog("✓ Sensor REGISTERED — receiving updates every 500ms")
        onDispose {
            onLog("✗ Sensor UNREGISTERED — cleanup complete")
        }
    }
}

@Composable
private fun PhoneticsAnalogy() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("🏦 Your Codebase: Phonetics WebView")

        TheoryBox(
            title = "This solves your exact NPE crash!",
            content = "Your Phonetics NPE was: onDestroy fired → SDK dispose() called → " +
                    "binding was null → NullPointerException.\n\nThe root issue: cleanup timing " +
                    "was tied to Activity.onDestroy, not to the composable lifecycle.\n\n" +
                    "With DisposableEffect: the SDK would be initialized when the composable " +
                    "enters, and disposed in onDispose — which the Compose runtime guarantees " +
                    "runs at the right time, before the binding becomes null."
        )

        GoodCodeBox(
            title = "✅ Phonetics-style SDK with DisposableEffect",
            code = """@Composable
fun PhoneticsContainer(sdk: PhoneticsSDK) {
    DisposableEffect(sdk) {
        // Binding is guaranteed to exist here
        sdk.initialize()
        sdk.bind(/* current binding */)
        
        onDispose {
            // Runs BEFORE the composable's binding is released
            // No more NPE!
            sdk.dispose()
        }
    }
    
    AndroidView(factory = { sdk.createWebView(it) })
}""",
            explanation = "DisposableEffect guarantees dispose() runs while the view is still valid, " +
                    "eliminating the ordering problem that caused the NPE."
        )
    }
}

@Composable
private fun ChallengeSection() {
    ChallengeBox(
        challengeText = """Challenge:
Implement a composable that:
1. Registers a BroadcastReceiver for "android.net.conn.CONNECTIVITY_CHANGE"
2. Updates a state variable with the connection status
3. Properly unregisters the receiver when the composable leaves

@Composable
fun NetworkStatusIndicator() {
    var isConnected by remember { mutableStateOf(true) }
    // Your implementation here
    
    Text(if (isConnected) "Connected" else "Offline")
}

BONUS: What key would you use if the IntentFilter could change?""",
        hint = "The receiver + filter should be created inside DisposableEffect. " +
                "The key determines when to re-register. For a fixed filter, Unit is fine."
    )
}

@Composable
private fun InterviewSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader("🎤 Interview Questions")

        InterviewCard(
            question = "What's the difference between LaunchedEffect and DisposableEffect?",
            answer = "LaunchedEffect is for asynchronous work (coroutines). DisposableEffect is for synchronous resource management that needs cleanup. DisposableEffect runs synchronously and requires an onDispose block. Use LaunchedEffect for suspending operations, DisposableEffect for register/unregister pairs."
        )
        InterviewCard(
            question = "When is onDispose called?",
            answer = "onDispose is called in two scenarios: (1) When the key changes — before the effect re-runs with the new key. (2) When the composable leaves composition — before it's removed from the tree. Both guarantee cleanup runs before the next state."
        )
        InterviewCard(
            question = "Can you do async work inside DisposableEffect?",
            answer = "No — DisposableEffect is synchronous. If you need async cleanup, you'd need a separate approach (like storing a reference to cancel). For async effects with cleanup, consider LaunchedEffect with a try/finally or a combine of both."
        )
        InterviewCard(
            question = "What happens if you throw in the DisposableEffect block (before onDispose)?",
            answer = "The onDispose block will NOT be called if an exception is thrown in the effect block before returning it. This is why resource creation should be careful — if setup can fail, handle it gracefully before onDispose."
        )
    }
}
