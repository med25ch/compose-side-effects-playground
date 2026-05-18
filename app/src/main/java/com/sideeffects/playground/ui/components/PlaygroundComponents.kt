package com.sideeffects.playground.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sideeffects.playground.ui.theme.*

// ─── Top App Bar ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaygroundTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        )
    )
}

// ─── Section Headers ────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

// ─── Info / Theory Box ──────────────────────────────────────────────────────

@Composable
fun TheoryBox(
    title: String,
    content: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(InfoBlue)
            .border(1.dp, InfoBlueBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF1565C0),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0),
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 13.sp,
            color = Color(0xFF0D47A1),
            lineHeight = 20.sp
        )
    }
}

// ─── BAD / GOOD code boxes ──────────────────────────────────────────────────

@Composable
fun BadCodeBox(
    title: String = "❌ Anti-Pattern",
    code: String,
    explanation: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BadRed)
            .border(1.dp, BadRedBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFFC62828), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        CodeBlock(code)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Why this fails: $explanation",
            fontSize = 12.sp,
            color = Color(0xFF7F0000),
            lineHeight = 18.sp
        )
    }
}

@Composable
fun GoodCodeBox(
    title: String = "✅ Correct Pattern",
    code: String,
    explanation: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GoodGreen)
            .border(1.dp, GoodGreenBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        CodeBlock(code)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Why this works: $explanation",
            fontSize = 12.sp,
            color = Color(0xFF1A3A1A),
            lineHeight = 18.sp
        )
    }
}

// ─── Code Block ─────────────────────────────────────────────────────────────

@Composable
fun CodeBlock(code: String, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBackground)
            .horizontalScroll(scrollState)
            .padding(12.dp)
    ) {
        Text(
            text = code,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFFCDD6F4),
            lineHeight = 18.sp
        )
    }
}

// ─── Log Console ────────────────────────────────────────────────────────────

@Composable
fun LogConsole(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LogBackground)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                Icons.Default.Terminal,
                contentDescription = null,
                tint = Color(0xFF50FA7B),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text("Composition Log", color = Color(0xFF50FA7B), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        if (logs.isEmpty()) {
            Text(
                "// Waiting for events...",
                color = Color(0xFF6272A4),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        } else {
            logs.takeLast(15).forEach { entry ->
                LogLine(entry)
            }
        }
    }
}

data class LogEntry(
    val message: String,
    val type: LogType = LogType.INFO,
    val timestamp: String = ""
)

enum class LogType { INFO, SUCCESS, WARNING, ERROR, RECOMPOSITION }

@Composable
private fun LogLine(entry: LogEntry) {
    val color = when (entry.type) {
        LogType.INFO -> Color(0xFF8BE9FD)
        LogType.SUCCESS -> Color(0xFF50FA7B)
        LogType.WARNING -> Color(0xFFFFB86C)
        LogType.ERROR -> Color(0xFFFF5555)
        LogType.RECOMPOSITION -> Color(0xFFFF79C6)
    }
    val prefix = when (entry.type) {
        LogType.INFO -> "ℹ "
        LogType.SUCCESS -> "✓ "
        LogType.WARNING -> "⚠ "
        LogType.ERROR -> "✗ "
        LogType.RECOMPOSITION -> "⟳ "
    }
    Row {
        if (entry.timestamp.isNotEmpty()) {
            Text(
                "[${entry.timestamp}] ",
                color = Color(0xFF6272A4),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        Text(
            "$prefix${entry.message}",
            color = color,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

// ─── Recomposition Counter ───────────────────────────────────────────────────

@Composable
fun RecompositionBadge(count: Int, modifier: Modifier = Modifier) {
    val bgColor by animateColorAsState(
        targetValue = if (count > 5) Color(0xFFFF5555) else if (count > 2) Color(0xFFFFB86C) else Color(0xFF50FA7B),
        label = "recomp_color"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor.copy(alpha = 0.2f))
            .border(1.dp, bgColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Refresh,
            contentDescription = null,
            tint = bgColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            "Recompositions: $count",
            color = bgColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─── Challenge Box ───────────────────────────────────────────────────────────

@Composable
fun ChallengeBox(
    challengeText: String,
    hint: String = "",
    modifier: Modifier = Modifier
) {
    var showHint by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WarningAmber)
            .border(1.dp, WarningAmberBorder, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text("Challenge", fontWeight = FontWeight.Bold, color = Color(0xFFE65100), fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(challengeText, fontSize = 13.sp, color = Color(0xFF4E342E), lineHeight = 20.sp)
        if (hint.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { showHint = !showHint }) {
                Text(if (showHint) "Hide hint" else "Show hint", color = Color(0xFFE65100))
            }
            if (showHint) {
                Text(
                    "💡 $hint",
                    fontSize = 12.sp,
                    color = Color(0xFF6D4C41),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// ─── Interview Q&A ───────────────────────────────────────────────────────────

@Composable
fun InterviewCard(
    question: String,
    answer: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        onClick = { expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Q: $question",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    lineHeight = 18.sp
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "A: $answer",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// ─── Playground Button ───────────────────────────────────────────────────────

@Composable
fun PlaygroundButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isDestructive: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = if (isDestructive) ButtonDefaults.buttonColors(
            containerColor = Color(0xFFB00020)
        ) else ButtonDefaults.buttonColors()
    ) {
        Text(text)
    }
}

// ─── Status Indicator ────────────────────────────────────────────────────────

@Composable
fun StatusIndicator(
    label: String,
    value: String,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(color)
        )
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = color)
    }
}
