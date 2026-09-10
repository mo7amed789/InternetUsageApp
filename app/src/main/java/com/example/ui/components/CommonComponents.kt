package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayUsagePoint
import com.example.ui.theme.DownloadColor
import com.example.ui.theme.MobileColor
import com.example.ui.theme.UploadColor
import com.example.ui.theme.WifiColor
import com.example.util.NetworkUtils

@Composable
fun UsageBarChart(
    points: List<DayUsagePoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No usage history for this period",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val maxVal = points.maxOfOrNull { it.totalBytes }?.coerceAtLeast(1024L * 1024L) ?: (1024L * 1024L)

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height - 24.dp.toPx()
                val barCount = points.size
                val barSpacing = canvasWidth / (barCount * 2f)
                val barWidth = barSpacing.coerceIn(16.dp.toPx(), 44.dp.toPx())
                val totalStep = canvasWidth / barCount

                points.forEachIndexed { index, point ->
                    val x = index * totalStep + (totalStep - barWidth) / 2f

                    val wifiFrac = (point.wifiBytes.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
                    val mobileFrac = (point.mobileBytes.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)

                    val wifiHeight = wifiFrac * canvasHeight
                    val mobileHeight = mobileFrac * canvasHeight

                    // Background track
                    drawRoundRect(
                        color = Color.Gray.copy(alpha = 0.12f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, canvasHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )

                    // Draw Wi-Fi bar (Cyan)
                    if (wifiHeight > 0) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(WifiColor, WifiColor.copy(alpha = 0.7f))
                            ),
                            topLeft = Offset(x, canvasHeight - wifiHeight),
                            size = Size(barWidth, wifiHeight),
                            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                        )
                    }

                    // Draw Mobile Data bar (Violet)
                    if (mobileHeight > 0) {
                        val startY = canvasHeight - wifiHeight - mobileHeight
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(MobileColor, MobileColor.copy(alpha = 0.7f))
                            ),
                            topLeft = Offset(x, startY.coerceAtLeast(0f)),
                            size = Size(barWidth, mobileHeight.coerceAtMost(canvasHeight)),
                            cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                        )
                    }
                }
            }
        }

        // Labels under bars
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(WifiColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Wi-Fi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)

            Spacer(modifier = Modifier.width(16.dp))

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(MobileColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Mobile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun QuotaProgressIndicator(
    usedBytes: Long,
    totalBytes: Long,
    warningPercent: Int,
    modifier: Modifier = Modifier
) {
    val usedRatio = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = usedRatio,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "quota_progress"
    )

    val isWarning = (usedRatio * 100) >= warningPercent
    val isExceeded = usedRatio >= 1.0f

    val progressColor = when {
        isExceeded -> Color(0xFFEF4444) // Red
        isWarning -> Color(0xFFF59E0B)  // Orange
        else -> DownloadColor           // Emerald Green
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .height(14.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(progressColor.copy(alpha = 0.8f), progressColor)
                        ),
                        shape = RoundedCornerShape(7.dp)
                    )
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${(usedRatio * 100).toInt()}% Used",
                style = MaterialTheme.typography.labelSmall,
                color = progressColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${NetworkUtils.formatBytes(usedBytes)} / ${NetworkUtils.formatBytes(totalBytes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PermissionNoticeBanner(
    title: String,
    description: String,
    buttonText: String,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = buttonText, fontSize = 13.sp)
            }
        }
    }
}
