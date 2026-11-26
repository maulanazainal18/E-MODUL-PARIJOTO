package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.curriculum.ProgressPoint
import com.example.data.curriculum.QuizEngineUtils

@Composable
fun ProgressionLineChartCard(
    title: String,
    subtitle: String,
    points: List<ProgressPoint>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val fillTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
    val fillBottom = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val nodeStrokeColor = MaterialTheme.colorScheme.surface

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.testTag("progression_line_chart_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = title, tint = lineColor)
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (points.isEmpty()) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                    Text(
                        "Grafik perkembangan akan muncul otomatis setelah ada komponen nilai yang dinilai.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                ) {
                    val leftPad = 14.dp.toPx()
                    val rightPad = 14.dp.toPx()
                    val topPad = 12.dp.toPx()
                    val bottomPad = 12.dp.toPx()
                    val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                    val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                    // Horizontal reference lines (0, 50, 75 KKM, 100)
                    listOf(0f, 0.5f, 0.75f, 1f).forEach { ratio ->
                        val py = topPad + chartH * (1f - ratio)
                        drawLine(
                            color = if (ratio == 0.75f) lineColor.copy(alpha = 0.35f) else gridColor.copy(alpha = 0.55f),
                            start = Offset(leftPad, py),
                            end = Offset(size.width - rightPad, py),
                            strokeWidth = if (ratio == 0.75f) 1.5.dp.toPx() else 1.dp.toPx()
                        )
                    }

                    val stepX = if (points.size <= 1) 0f else chartW / (points.size - 1)
                    val coords = points.mapIndexed { idx, pt ->
                        val x = if (points.size == 1) leftPad + chartW / 2f else leftPad + idx * stepX
                        val normY = (pt.score / 100.0).toFloat().coerceIn(0f, 1f)
                        val y = topPad + chartH * (1f - normY)
                        Offset(x, y)
                    }

                    if (coords.size >= 2) {
                        val areaPath = Path().apply {
                            moveTo(coords.first().x, topPad + chartH)
                            coords.forEach { lineTo(it.x, it.y) }
                            lineTo(coords.last().x, topPad + chartH)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(fillTop, fillBottom),
                                startY = topPad,
                                endY = topPad + chartH
                            )
                        )

                        val linePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            coords.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = linePath,
                            color = lineColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    coords.forEach { pt ->
                        drawCircle(color = nodeStrokeColor, radius = 6.dp.toPx(), center = pt)
                        drawCircle(color = lineColor, radius = 4.2.dp.toPx(), center = pt)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    points.forEach { pt ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "${QuizEngineUtils.roundScore(pt.score)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = lineColor
                            )
                            Text(
                                pt.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComponentComparisonBarChartCard(
    quizScore: Double?,
    postTestScore: Double?,
    skillScore: Double?,
    projectScore: Double?,
    attitudeScore: Double?,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        Triple("Kuis", quizScore, MaterialTheme.colorScheme.primary),
        Triple("Post-Test", postTestScore, MaterialTheme.colorScheme.tertiary),
        Triple("Keterampilan", skillScore, MaterialTheme.colorScheme.secondary),
        Triple("Proyek", projectScore, Color(0xFF00796B)),
        Triple("Sikap", attitudeScore, Color(0xFF558B2F))
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.testTag("component_bar_chart_card")
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BarChart, contentDescription = "Komponen Nilai", tint = MaterialTheme.colorScheme.primary)
                Text("Perbandingan Komponen Penilaian", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(2.dp))
            entries.forEach { (label, score, color) ->
                val ratio = ((score ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                        Text(
                            score?.let { "${QuizEngineUtils.roundScore(it)} / 100" } ?: "Belum dinilai",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (score != null) color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Canvas(modifier = Modifier.fillMaxWidth().height(10.dp)) {
                        val corner = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                        drawRoundRect(
                            color = trackColor,
                            size = Size(size.width, size.height),
                            cornerRadius = corner
                        )
                        if (ratio > 0f) {
                            drawRoundRect(
                                color = color,
                                size = Size(size.width * ratio, size.height),
                                cornerRadius = corner
                            )
                        }
                    }
                }
            }
        }
    }
}
