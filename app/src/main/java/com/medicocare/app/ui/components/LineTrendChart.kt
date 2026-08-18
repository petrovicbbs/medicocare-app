package com.medicocare.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.medicocare.app.R

/** Jedna linija (serija) na grafikonu — hronološki uređene Y vrednosti + boja + naziv za legendu. */
data class ChartSeries(
    val points: List<Float>,
    val color: Color,
    val label: String
)

/**
 * Jednostavan linijski grafikon kretanja kroz vreme, iscrtan direktno preko Compose Canvas-a
 * (bez spoljnih biblioteka za grafikone, da se ne rizikuju nove build greške). Koristi se za
 * trend pritiska/šećera na VitalsScreen i trend dužine ciklusa na CycleTrackerScreen. Sve
 * prosleđene serije dele istu, automatski skaliranu Y osu (na zajednički min/max svih tačaka).
 */
@Composable
fun LineTrendChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    xAxisStartLabel: String = "",
    xAxisEndLabel: String = "",
    yValueFormatter: (Float) -> String = { v -> v.toInt().toString() }
) {
    val nonEmptySeries = series.filter { it.points.size >= 2 }

    Column(modifier = modifier) {
        if (nonEmptySeries.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                nonEmptySeries.forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(s.color)
                        )
                        Text(
                            s.label,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }
        }

        if (nonEmptySeries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.chart_not_enough_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val allValues = nonEmptySeries.flatMap { it.points }
            val rawMin: Float = allValues.minOrNull() ?: 0f
            val rawMax: Float = allValues.maxOrNull() ?: 0f
            val span: Float = if (rawMax - rawMin > 0.0001f) rawMax - rawMin else 1f
            val pad = span * 0.15f
            val minY = rawMin - pad
            val maxY = rawMax + pad
            val yRange = if (maxY - minY > 0.0001f) maxY - minY else 1f

            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 40.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    val gridColor = Color.Gray.copy(alpha = 0.25f)
                    val gridSteps = 3
                    for (i in 0..gridSteps) {
                        val y = size.height * i.toFloat() / gridSteps.toFloat()
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    nonEmptySeries.forEach { s ->
                        val n = s.points.size
                        val path = Path()
                        s.points.forEachIndexed { idx, v ->
                            val xFrac = idx.toFloat() / (n - 1).toFloat()
                            val x = size.width * xFrac
                            val normalized = (v - minY) / yRange
                            val y = size.height * (1f - normalized)
                            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path = path, color = s.color, style = Stroke(width = 2.5.dp.toPx()))
                        s.points.forEachIndexed { idx, v ->
                            val xFrac = idx.toFloat() / (n - 1).toFloat()
                            val x = size.width * xFrac
                            val normalized = (v - minY) / yRange
                            val y = size.height * (1f - normalized)
                            drawCircle(color = s.color, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .width(40.dp)
                        .fillMaxHeight()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(yValueFormatter(maxY), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(yValueFormatter(minY), style = MaterialTheme.typography.labelSmall)
                }
            }

            if (xAxisStartLabel.isNotBlank() || xAxisEndLabel.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, end = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        xAxisStartLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        xAxisEndLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
