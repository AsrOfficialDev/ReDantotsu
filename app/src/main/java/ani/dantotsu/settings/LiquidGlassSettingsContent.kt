package ani.dantotsu.settings

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.incognitoNotification
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.FeedActivity
import ani.dantotsu.profile.notification.NotificationActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.startMainActivity
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Frosted Glass Settings Dialog using backdrop library
 * Matches the DialogContent.kt style from AndroidLiquidGlass reference
 */
@Composable
fun LiquidGlassSettingsContent(
    backdrop: Backdrop,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    onToggleIncognito: (Boolean) -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit
) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    val accentColor = if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF)
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)
    
    var isIncognito by remember { mutableStateOf(PrefManager.getVal<Boolean>(PrefName.Incognito)) }
    var isOfflineMode by remember { mutableStateOf(PrefManager.getVal<Boolean>(PrefName.OfflineMode)) }

    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 40.dp)
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousRoundedRectangle(32.dp) },
                effects = {
                    colorControls(
                        brightness = if (isLightTheme) 0.2f else 0f,
                        saturation = 1.5f
                    )
                    blur(if (isLightTheme) 16.dp.toPx() else 8.dp.toPx())
                    lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
                },
                highlight = { Highlight.Plain },
                onDrawSurface = { drawRect(containerColor) }
            )
            .fillMaxWidth()
    ) {
        // User Profile Section
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onNavigateToProfile() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder - would need actual image loading
            Box(
                Modifier
                    .size(48.dp)
                    .clip(ContinuousRoundedRectangle(24.dp))
                    .background(containerColor.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.ic_round_person_24),
                    contentDescription = "Avatar",
                    tint = contentColor.copy(0.7f),
                    modifier = Modifier.size(32.dp)
                )
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
            
            // Notification icon
            IconButton(onClick = onNavigateToNotifications) {
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
        
        Divider(color = contentColor.copy(0.1f), thickness = 1.dp)
        
        // Toggle Items
        SettingsToggleItem(
            icon = R.drawable.ic_incognito_24,
            title = "Incognito Mode",
            checked = isIncognito,
            onCheckedChange = { 
                isIncognito = it
                onToggleIncognito(it)
            },
            contentColor = contentColor,
            containerColor = containerColor
        )
        
        SettingsToggleItem(
            icon = R.drawable.ic_download_24,
            title = "Offline Mode",
            checked = isOfflineMode,
            onCheckedChange = { 
                isOfflineMode = it
                onToggleOfflineMode(it)
            },
            contentColor = contentColor,
            containerColor = containerColor
        )
        
        Divider(color = contentColor.copy(0.1f), thickness = 1.dp)
        
        // Navigation Items
        SettingsNavigationItem(
            icon = R.drawable.ic_round_notifications_none_24,
            title = "Activities",
            onClick = onNavigateToActivity,
            contentColor = contentColor
        )
        
        SettingsNavigationItem(
            icon = R.drawable.ic_extension,
            title = "Extensions",
            onClick = onNavigateToExtensions,
            contentColor = contentColor
        )
        
        SettingsNavigationItem(
            icon = R.drawable.ic_round_settings_24,
            title = "Settings",
            onClick = onNavigateToSettings,
            contentColor = contentColor
        )
    }
}

@Composable
private fun SettingsToggleItem(
    icon: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentColor: Color,
    containerColor: Color
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
                checkedTrackColor = Color(0xFF0088FF),
                uncheckedThumbColor = contentColor.copy(0.5f),
                uncheckedTrackColor = contentColor.copy(0.2f)
            )
        )
    }
}

@Composable
private fun SettingsNavigationItem(
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
