package ani.dantotsu.widgets

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.incognitoNotification
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.FeedActivity
import ani.dantotsu.profile.notification.NotificationActivity
import ani.dantotsu.settings.ExtensionsActivity
import ani.dantotsu.settings.SettingsActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.startMainActivity

/**
 * Glass Settings Overlay - renders within the same Compose hierarchy
 * Uses the shared backdrop for true glass/blur effect
 */
@Composable
fun GlassSettingsOverlay(
    visible: Boolean,
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onLogout: () -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
    val dimColor = Color.Transparent // No dimming
    
    val context = LocalContext.current
    var isIncognito by remember { mutableStateOf(PrefManager.getVal<Boolean>(PrefName.Incognito)) }
    var isOfflineMode by remember { mutableStateOf(PrefManager.getVal<Boolean>(PrefName.OfflineMode)) }
    
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Reset offset when overlay becomes visible
    LaunchedEffect(visible) {
        if (visible) {
            offsetY.snapTo(0f)
        }
    }
    
    // Clickable background to dismiss (outside animation)
    if (visible) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable { onDismiss() }
        )
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                Modifier
                    .offset { IntOffset(0, offsetY.value.roundToInt()) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (offsetY.value > 200f) {
                                    onDismiss()
                                } else {
                                    scope.launch { offsetY.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 500f)) }
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                if (offsetY.value + dragAmount >= 0f) {
                                    scope.launch { offsetY.snapTo(offsetY.value + dragAmount) }
                                }
                            }
                        )
                    }
                    .clickable(enabled = false) {}
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousRoundedRectangle(32.dp) },
                        effects = {
                            colorControls(
                                brightness = if (isLightTheme) 0.2f else 0f,
                                saturation = 1.3f // Slightly reduced
                            )
                            blur(if (isLightTheme) 5.dp.toPx() else 8.dp.toPx()) // Reduced blur
                            lens(16.dp.toPx(), 32.dp.toPx(), depthEffect = true) // Reduced lens effect
                        },
                        highlight = { Highlight.Plain },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .fillMaxWidth()
            ) {
                // Drag handle
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .align(Alignment.CenterHorizontally)
                        .size(40.dp, 4.dp)
                        .clip(ContinuousRoundedRectangle(2.dp))
                        .background(contentColor.copy(0.3f))
                )
                
                // User Profile Section
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(Intent(context, ProfileActivity::class.java).apply {
                                putExtra("userId", Anilist.userid)
                            })
                            onDismiss()
                        }
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(ContinuousRoundedRectangle(26.dp))
                            .background(containerColor.copy(0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (Anilist.avatar != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(Anilist.avatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painterResource(R.drawable.ic_round_person_24),
                                contentDescription = "Avatar",
                                tint = contentColor.copy(0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(Modifier.width(16.dp))
                    
                    Column(Modifier.weight(1f)) {
                        BasicText(
                            Anilist.username ?: "Guest",
                            style = TextStyle(contentColor, 18.sp, FontWeight.Medium)
                        )
                        if (Anilist.token != null) {
                            BasicText(
                                "Logout",
                                Modifier.clickable { onLogout() },
                                style = TextStyle(accentColor, 14.sp)
                            )
                        } else {
                            BasicText(
                                "Login",
                                style = TextStyle(accentColor, 14.sp)
                            )
                        }
                    }
                    
                    IconButton(onClick = {
                        context.startActivity(Intent(context, NotificationActivity::class.java))
                        onDismiss()
                    }) {
                        Icon(
                            painterResource(
                                if (Anilist.unreadNotificationCount > 0) 
                                    R.drawable.ic_round_notifications_active_24 
                                else 
                                    R.drawable.ic_round_notifications_none_24
                            ),
                            contentDescription = "Notifications",
                            tint = contentColor.copy(0.7f)
                        )
                    }
                }
                
                HorizontalDivider(color = contentColor.copy(0.1f), thickness = 1.dp)
                
                // Toggle Items
                GlassToggleItem(
                    icon = R.drawable.ic_incognito_24,
                    title = "Incognito Mode",
                    checked = isIncognito,
                    onCheckedChange = { 
                        isIncognito = it
                        PrefManager.setVal(PrefName.Incognito, it)
                        incognitoNotification(context)
                    },
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                
                GlassToggleItem(
                    icon = R.drawable.ic_download_24,
                    title = "Offline Mode",
                    checked = isOfflineMode,
                    onCheckedChange = { 
                        isOfflineMode = it
                        PrefManager.setVal(PrefName.OfflineMode, it)
                    },
                    contentColor = contentColor,
                    accentColor = accentColor
                )
                
                HorizontalDivider(color = contentColor.copy(0.1f), thickness = 1.dp)
                
                // Navigation Items
                GlassNavItem(
                    icon = R.drawable.ic_round_notifications_none_24,
                    title = "Activities",
                    onClick = {
                        context.startActivity(Intent(context, FeedActivity::class.java))
                        onDismiss()
                    },
                    contentColor = contentColor
                )
                
                GlassNavItem(
                    icon = R.drawable.ic_extension,
                    title = "Extensions",
                    onClick = {
                        context.startActivity(Intent(context, ExtensionsActivity::class.java))
                        onDismiss()
                    },
                    contentColor = contentColor
                )
                
                GlassNavItem(
                    icon = R.drawable.ic_round_settings_24,
                    title = "Settings",
                    onClick = {
                        context.startActivity(Intent(context, SettingsActivity::class.java))
                        onDismiss()
                    },
                    contentColor = contentColor
                )
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun GlassToggleItem(
    icon: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentColor: Color,
    accentColor: Color
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(icon),
            contentDescription = title,
            tint = contentColor.copy(0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        BasicText(
            title,
            Modifier.weight(1f),
            style = TextStyle(contentColor, 16.sp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = contentColor.copy(0.5f),
                uncheckedTrackColor = contentColor.copy(0.2f)
            )
        )
    }
}

@Composable
private fun GlassNavItem(
    icon: Int,
    title: String,
    onClick: () -> Unit,
    contentColor: Color
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painterResource(icon),
            contentDescription = title,
            tint = contentColor.copy(0.7f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        BasicText(
            title,
            Modifier.weight(1f),
            style = TextStyle(contentColor, 16.sp)
        )
        Icon(
            painterResource(R.drawable.ic_round_arrow_back_ios_new_24),
            contentDescription = "Navigate",
            tint = contentColor.copy(0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Companion object to control overlay visibility from Fragments
 */
object GlassSettingsController {
    var showSettingsOverlay = mutableStateOf(false)
    
    fun show() {
        showSettingsOverlay.value = true
    }
    
    fun hide() {
        showSettingsOverlay.value = false
    }
}
