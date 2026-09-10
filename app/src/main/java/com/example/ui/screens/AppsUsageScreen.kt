package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageInfo
import com.example.data.model.UsagePeriod
import com.example.ui.MainViewModel
import com.example.ui.components.PermissionNoticeBanner
import com.example.ui.theme.BlockedRed
import com.example.ui.theme.DownloadColor
import com.example.ui.theme.MobileColor
import com.example.ui.theme.WifiColor
import com.example.util.Localization
import com.example.util.NetworkUtils

@Composable
fun AppsUsageScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val apps by viewModel.appsUsage.collectAsState()
    val isLoading by viewModel.isLoadingApps.collectAsState()
    val period by viewModel.selectedPeriod.collectAsState()
    val lang = viewModel.lang

    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf(SortCriteria.TOTAL) }
    var selectedAppForDetail by remember { mutableStateOf<AppUsageInfo?>(null) }

    val hasUsagePerm = NetworkUtils.hasUsageStatsPermission(context)

    // Filter and Sort
    val filteredApps = remember(apps, searchQuery, sortBy) {
        apps.filter { app ->
            app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)
        }.sortedWith { a, b ->
            when (sortBy) {
                SortCriteria.TOTAL -> b.totalBytes.compareTo(a.totalBytes)
                SortCriteria.MOBILE -> b.mobileBytes.compareTo(a.mobileBytes)
                SortCriteria.WIFI -> b.wifiBytes.compareTo(a.wifiBytes)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // Permission Banner if missing
        if (!hasUsagePerm) {
            PermissionNoticeBanner(
                title = Localization.tr("permission_needed", lang),
                description = Localization.tr("permission_desc", lang),
                buttonText = Localization.tr("grant_permission", lang),
                onGrantClick = {
                    val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                    context.startActivity(intent)
                }
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(Localization.tr("search_apps", lang), fontSize = 14.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        // Period filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UsagePeriod.values().forEach { p ->
                val isSelected = period == p
                val label = when (p) {
                    UsagePeriod.TODAY -> Localization.tr("period_today", lang)
                    UsagePeriod.WEEK -> Localization.tr("period_week", lang)
                    UsagePeriod.MONTH -> Localization.tr("period_month", lang)
                    UsagePeriod.YEAR -> Localization.tr("period_year", lang)
                    UsagePeriod.LIFETIME -> Localization.tr("period_lifetime", lang)
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setPeriod(p) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Sort row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${filteredApps.size} apps found",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SortChip(
                    label = "Total",
                    selected = sortBy == SortCriteria.TOTAL,
                    onClick = { sortBy = SortCriteria.TOTAL }
                )
                SortChip(
                    label = "Mobile",
                    selected = sortBy == SortCriteria.MOBILE,
                    onClick = { sortBy = SortCriteria.MOBILE }
                )
                SortChip(
                    label = "Wi-Fi",
                    selected = sortBy == SortCriteria.WIFI,
                    onClick = { sortBy = SortCriteria.WIFI }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    AppUsageCard(
                        app = app,
                        lang = lang,
                        onClick = { selectedAppForDetail = app },
                        onToggleBlockMobile = {
                            viewModel.toggleAppBlockMobile(app.packageName, app.appName, app.isBlockedMobile)
                        },
                        onToggleBlockWifi = {
                            viewModel.toggleAppBlockWifi(app.packageName, app.appName, app.isBlockedWifi)
                        },
                        onToggleWhitelist = {
                            viewModel.toggleAppWhitelist(app.packageName, app.appName, app.isWhitelisted)
                        }
                    )
                }
            }
        }
    }

    // Detail Modal Dialog
    selectedAppForDetail?.let { app ->
        AppDetailDialog(
            app = app,
            lang = lang,
            onDismiss = { selectedAppForDetail = null }
        )
    }
}

enum class SortCriteria {
    TOTAL,
    MOBILE,
    WIFI
}

@Composable
fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun AppUsageCard(
    app: AppUsageInfo,
    lang: Localization.Lang,
    onClick: () -> Unit,
    onToggleBlockMobile: () -> Unit,
    onToggleBlockWifi: () -> Unit,
    onToggleWhitelist: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            val iconBitmap = remember(app.icon) {
                app.icon?.let { drawableToBitmap(it)?.asImageBitmap() }
            }

            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.appName,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = app.appName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // App name & data split
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total: ${NetworkUtils.formatBytes(app.totalBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Wi-Fi: ${NetworkUtils.formatBytes(app.wifiBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Cell: ${NetworkUtils.formatBytes(app.mobileBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Foreground vs Background badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (app.foregroundBytes > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = DownloadColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "FG: ${NetworkUtils.formatBytes(app.foregroundBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = DownloadColor,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (app.backgroundBytes > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MobileColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "BG: ${NetworkUtils.formatBytes(app.backgroundBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MobileColor,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick block / firewall actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Mobile block icon
                IconButton(
                    onClick = onToggleBlockMobile,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Block,
                        contentDescription = "Block Mobile",
                        tint = if (app.isBlockedMobile) BlockedRed else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Whitelist icon
                IconButton(
                    onClick = onToggleWhitelist,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Whitelist",
                        tint = if (app.isWhitelisted) DownloadColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AppDetailDialog(
    app: AppUsageInfo,
    lang: Localization.Lang,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val iconBitmap = remember(app.icon) {
                    app.icon?.let { drawableToBitmap(it)?.asImageBitmap() }
                }
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Text(text = app.appName, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Usage:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        NetworkUtils.formatBytes(app.totalBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wi-Fi Data:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        NetworkUtils.formatBytes(app.wifiBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WifiColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Mobile Data:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        NetworkUtils.formatBytes(app.mobileBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MobileColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Foreground:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        NetworkUtils.formatBytes(app.foregroundBytes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Background (Hidden):", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        NetworkUtils.formatBytes(app.backgroundBytes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = BlockedRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

fun drawableToBitmap(drawable: Drawable): Bitmap? {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}
