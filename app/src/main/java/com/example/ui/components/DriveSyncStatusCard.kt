package com.example.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.DriveSyncState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Compact, pill-shaped Google Cloud Sync Status badge:
 * [ (☁️) Google Cloud: Up to Date  🔄 Just now (11:46 AM) ]
 */
@Composable
fun GoogleDriveSyncStatusCard(
    syncState: DriveSyncState,
    onSyncNow: () -> Unit,
    onOpenBackupDialog: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleAutoSync: ((Boolean) -> Unit)? = null
) {
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "sync_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Formatted time string like "11:46 AM"
    val shortTime = remember(syncState.lastSyncTimeMillis) {
        if (syncState.lastSyncTimeMillis > 0L) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date(syncState.lastSyncTimeMillis))
        } else {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            sdf.format(Date())
        }
    }

    // Color definitions based on state
    val statusColor = when {
        syncState.isSyncing -> Color(0xFF16A34A)
        syncState.isPending -> Color(0xFFD97706)
        else -> Color(0xFF16A34A)
    }

    val containerBgColor = when {
        syncState.isPending -> Color(0xFFFFFBEB)
        else -> Color(0xFFE8F8EE)
    }

    val borderColor = when {
        syncState.isPending -> Color(0xFFFDE68A)
        else -> Color(0xFFA7F3D0)
    }

    Surface(
        onClick = {
            if (!syncState.isSyncing) {
                onSyncNow()
            }
        },
        shape = RoundedCornerShape(24.dp),
        color = containerBgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .testTag("drive_sync_status_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Side: Cloud Icon in circle + "Google Cloud: Up to Date"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Green cloud circular badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(statusColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (syncState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Google Cloud",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        syncState.isSyncing -> "Google Cloud: Syncing..."
                        syncState.isPending -> "Google Cloud: Pending (${syncState.pendingOperationsCount})"
                        else -> "Google Cloud: Up to Date"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E293B)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right Side: Sync Icon + "Just now (11:46 AM)"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync",
                    tint = statusColor,
                    modifier = Modifier
                        .size(16.dp)
                        .then(if (syncState.isSyncing) Modifier.rotate(rotationAngle) else Modifier)
                )

                Text(
                    text = when {
                        syncState.isSyncing -> "Syncing..."
                        syncState.isPending -> "Tap to Sync"
                        syncState.lastSyncRelative.isNotBlank() -> "${syncState.lastSyncRelative} ($shortTime)"
                        else -> "Just now ($shortTime)"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF374151)
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
