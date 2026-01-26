package ani.dantotsu.settings

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ani.dantotsu.R
import ani.dantotsu.connections.anilist.Anilist
import ani.dantotsu.incognitoNotification
import ani.dantotsu.profile.ProfileActivity
import ani.dantotsu.profile.activity.FeedActivity
import ani.dantotsu.profile.notification.NotificationActivity
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.startMainActivity
import ani.dantotsu.util.customAlertDialog
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.highlight.Highlight
import com.kyant.capsule.ContinuousRoundedRectangle
import java.io.File

/**
 * Full-screen Activity for Liquid Glass Settings Dialog
 * Uses backdrop library for true frosted glass effect
 */
class LiquidGlassSettingsActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BACKGROUND_PATH = "background_path"
        const val EXTRA_PAGE_TYPE = "page_type"
        const val EXTRA_ACCENT_COLOR = "accent_color"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make activity transparent
        window.apply {
            requestFeature(Window.FEATURE_NO_TITLE)
            setBackgroundDrawableResource(android.R.color.transparent)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            statusBarColor = android.graphics.Color.TRANSPARENT
            navigationBarColor = android.graphics.Color.TRANSPARENT
            setDimAmount(0f)
        }
        
        val backgroundPath = intent.getStringExtra(EXTRA_BACKGROUND_PATH)
        val pageType = intent.getStringExtra(EXTRA_PAGE_TYPE) ?: "HOME"
        
        setContent {
            LiquidGlassSettingsScreen(
                backgroundPath = backgroundPath,
                pageType = pageType,
                onDismiss = { finish() },
                onNavigateToSettings = {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    finish()
                },
                onNavigateToExtensions = {
                    startActivity(Intent(this, ExtensionsActivity::class.java))
                    finish()
                },
                onNavigateToActivity = {
                    startActivity(Intent(this, FeedActivity::class.java))
                    finish()
                },
                onNavigateToNotifications = {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    finish()
                },
                onNavigateToProfile = {
                    startActivity(Intent(this, ProfileActivity::class.java).apply {
                        putExtra("userId", Anilist.userid)
                    })
                    finish()
                },
                onLogout = {
                    // Show logout confirmation
                    Anilist.removeSavedToken()
                    startMainActivity(this)
                    finish()
                },
                accentColorParam = intent.getIntExtra(EXTRA_ACCENT_COLOR, -1)
            )
        }
    }
}

@Composable
fun LiquidGlassSettingsScreen(
    backgroundPath: String?,
    pageType: String,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToExtensions: () -> Unit,
    onNavigateToActivity: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    accentColorParam: Int = -1
) {
    val isLightTheme = !isSystemInDarkTheme()
    val contentColor = if (isLightTheme) Color.Black else Color.White
    // Use passed color if available, otherwise fallback to default blue
    val accentColor = if (accentColorParam != -1) Color(accentColorParam) else (if (isLightTheme) Color(0xFF0088FF) else Color(0xFF0091FF))
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(0.6f) else Color(0xFF121212).copy(0.4f)

    val dimColor = if (isLightTheme) Color(0xFF29293A).copy(0.15f) else Color(0xFF121212).copy(0.25f)
    
    val context = LocalContext.current
    var isIncognito by remember { mutableStateOf(PrefManager.getVal<Boolean>(PrefName.Incognito)) }
    var isOfflineMode by remember { mutableStateOf(PrefManager.getVal<Boolean>(PrefName.OfflineMode)) }
    
    val backdrop = rememberLayerBackdrop()
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Load background image if available
    val backgroundBitmap = remember(backgroundPath) {
        backgroundPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) {
                    BitmapFactory.decodeFile(path)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .clickable { onDismiss() },  // Dismiss on background click
        contentAlignment = Alignment.BottomCenter
    ) {
        // Background layer - either captured screenshot or fallback
        if (backgroundBitmap != null) {
            Image(
                BitmapPainter(backgroundBitmap),
                contentDescription = null,
                modifier = Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Fallback: just use a solid color with the layer
            Box(
                Modifier
                    .layerBackdrop(backdrop)
                    .fillMaxSize()
                    .background(if (isLightTheme) Color(0xFFF0F0F0) else Color(0xFF1A1A1A))
            )
        }
        
        // Dim overlay
        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    drawRect(dimColor)
                }
        )
        
        // Frosted Glass Settings Content
        Column(
            Modifier
                .padding(horizontal = 0.dp)
                .offset { IntOffset(0, offsetY.value.roundToInt()) }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (offsetY.value > 300f) {
                                onDismiss()
                            } else {
                                scope.launch { offsetY.animateTo(0f) }
                            }
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            if (offsetY.value + dragAmount >= 0f) { // Only allow dragging down
                                scope.launch { offsetY.snapTo(offsetY.value + dragAmount) }
                            }
                        }
                    )
                }
                .clickable(enabled = false) {} // Prevent click through
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousRoundedRectangle(32.dp) },
                    effects = {
                        colorControls(
                            brightness = if (isLightTheme) 0.2f else 0f,
                            saturation = 1.5f
                        )
                        blur(if (isLightTheme) 8.dp.toPx() else 12.dp.toPx())
                        lens(24.dp.toPx(), 48.dp.toPx(), depthEffect = true)
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
                    .clickable { onNavigateToProfile() }
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
            
            HorizontalDivider(color = contentColor.copy(0.1f), thickness = 1.dp)
            
            // Toggle Items
            GlassSettingsToggleItem(
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
            
            GlassSettingsToggleItem(
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
            GlassSettingsNavigationItem(
                icon = R.drawable.ic_round_notifications_none_24,
                title = "Activities",
                onClick = onNavigateToActivity,
                contentColor = contentColor
            )
            
            GlassSettingsNavigationItem(
                icon = R.drawable.ic_extension,
                title = "Extensions",
                onClick = onNavigateToExtensions,
                contentColor = contentColor
            )
            
            GlassSettingsNavigationItem(
                icon = R.drawable.ic_round_settings_24,
                title = "Settings",
                onClick = onNavigateToSettings,
                contentColor = contentColor
            )
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun GlassSettingsToggleItem(
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
private fun GlassSettingsNavigationItem(
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
            // Arrow icon already points right, no rotation needed
        )
    }
}
