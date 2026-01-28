package com.auralis.music.ui.component

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auralis.music.R

/**
 * Enhanced update dialog with modern Material 3 styling and animations
 * 
 * @param message Update message from Remote Config
 * @param updateUrl URL to APK or Play Store
 * @param forceUpdate If true, user cannot dismiss and app closes after clicking update
 * @param onDismiss Callback when dialog is dismissed (only called if forceUpdate is false)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    message: String,
    updateUrl: String,
    forceUpdate: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isAnimating by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0.8f,
        animationSpec = tween(durationMillis = 300),
        label = "dialog_scale"
    )
    
    LaunchedEffect(Unit) {
        isAnimating = true
    }
    
    val dialogColor = if (forceUpdate) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val icon = if (forceUpdate) R.drawable.error else R.drawable.download
    val iconColor = if (forceUpdate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    AlertDialog(
        onDismissRequest = {
            // Only allow dismissal if not a force update
            if (!forceUpdate) {
                onDismiss()
            }
        },
        modifier = Modifier.scale(scale)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(
                containerColor = dialogColor
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon with gradient background
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    iconColor.copy(alpha = 0.2f),
                                    iconColor.copy(alpha = 0.1f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = if (forceUpdate) "Required Update" else "Update Available",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (forceUpdate) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Subtitle for force update
                if (forceUpdate) {
                    Text(
                        text = "This update is required to continue using the app",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (forceUpdate) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (forceUpdate) Arrangement.Center else Arrangement.spacedBy(12.dp)
                ) {
                    if (!forceUpdate) {
                        // Later button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(
                                1.dp, 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        ) {
                            Text(
                                text = "Later",
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                    
                    // Update button
                    Button(
                        onClick = {
                            // Open update URL in browser
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                context.startActivity(intent)
                                
                                // If force update, close the app after opening update link
                                if (forceUpdate && context is Activity) {
                                    context.finish()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = if (forceUpdate) Modifier.fillMaxWidth() else Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (forceUpdate) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (forceUpdate) "Update Now" else "Update",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
