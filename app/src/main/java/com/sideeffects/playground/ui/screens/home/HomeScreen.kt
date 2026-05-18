package com.sideeffects.playground.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sideeffects.playground.navigation.Screen

data class SideEffectEntry(
    val title: String,
    val subtitle: String,
    val category: String,
    val route: String,
    val difficulty: Difficulty,
    val color: Color
)

enum class Difficulty { Beginner, Intermediate, Advanced }

val sideEffects = listOf(
    SideEffectEntry(
        title = "remember & rememberSaveable",
        subtitle = "State survival across recompositions and config changes",
        category = "State Holder",
        route = Screen.Remember.route,
        difficulty = Difficulty.Beginner,
        color = Color(0xFF4CAF50)
    ),
    SideEffectEntry(
        title = "LaunchedEffect",
        subtitle = "Run coroutines scoped to the composition lifecycle",
        category = "Coroutine Effect",
        route = Screen.LaunchedEffect.route,
        difficulty = Difficulty.Intermediate,
        color = Color(0xFF2196F3)
    ),
    SideEffectEntry(
        title = "rememberCoroutineScope",
        subtitle = "Imperative coroutine launching from event handlers",
        category = "Coroutine Scope",
        route = Screen.RememberCoroutineScope.route,
        difficulty = Difficulty.Intermediate,
        color = Color(0xFF9C27B0)
    ),
    SideEffectEntry(
        title = "rememberUpdatedState",
        subtitle = "Capture the latest lambda/value inside long-lived effects",
        category = "State Reference",
        route = Screen.RememberUpdatedState.route,
        difficulty = Difficulty.Advanced,
        color = Color(0xFFFF9800)
    ),
    SideEffectEntry(
        title = "DisposableEffect",
        subtitle = "Register resources with guaranteed cleanup via onDispose",
        category = "Lifecycle Effect",
        route = Screen.DisposableEffect.route,
        difficulty = Difficulty.Intermediate,
        color = Color(0xFFF44336)
    ),
    SideEffectEntry(
        title = "SideEffect",
        subtitle = "Sync Compose state to non-Compose code after recomposition",
        category = "Sync Effect",
        route = Screen.SideEffect.route,
        difficulty = Difficulty.Intermediate,
        color = Color(0xFF607D8B)
    ),
    SideEffectEntry(
        title = "produceState",
        subtitle = "Convert async sources into Compose State elegantly",
        category = "State Producer",
        route = Screen.ProduceState.route,
        difficulty = Difficulty.Intermediate,
        color = Color(0xFF00BCD4)
    ),
    SideEffectEntry(
        title = "derivedStateOf",
        subtitle = "Compute derived state and prevent over-recomposition",
        category = "Derived State",
        route = Screen.DerivedState.route,
        difficulty = Difficulty.Advanced,
        color = Color(0xFFE91E63)
    ),
    SideEffectEntry(
        title = "snapshotFlow",
        subtitle = "Convert Compose State into a cold Flow for reactive pipelines",
        category = "Flow Bridge",
        route = Screen.SnapshotFlow.route,
        difficulty = Difficulty.Advanced,
        color = Color(0xFF795548)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigate: (String) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Side Effects Playground", fontWeight = FontWeight.Bold)
                        Text(
                            "Jetpack Compose Learning Lab",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6650A4),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                IntroCard()
            }
            item {
                Text(
                    "Choose a Side Effect to explore",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(sideEffects) { entry ->
                SideEffectCard(entry = entry, onClick = { onNavigate(entry.route) })
            }
        }
    }
}

@Composable
private fun IntroCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF6650A4))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PlayCircle,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "Interactive Learning",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    "Each section has: theory → bad example → good example → live demo → challenge → interview Q&A",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun SideEffectCard(entry: SideEffectEntry, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(entry.color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(entry.color)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    DifficultyBadge(entry.difficulty)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    entry.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    entry.category,
                    fontSize = 11.sp,
                    color = entry.color,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: Difficulty) {
    val (text, color) = when (difficulty) {
        Difficulty.Beginner -> "Beginner" to Color(0xFF4CAF50)
        Difficulty.Intermediate -> "Intermediate" to Color(0xFFFF9800)
        Difficulty.Advanced -> "Advanced" to Color(0xFFF44336)
    }
    Text(
        text = text,
        fontSize = 10.sp,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
