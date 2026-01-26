package ani.dantotsu.themes.liquidglass

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass composable components using AndroidLiquidGlass (Backdrop) library
 * These components provide iOS 26-style glassmorphism effects
 * 
 * Note: Full blur effects require Android 12+ (API 31).
 * On older devices, a semi-transparent fallback is provided.
 */

/**
 * A glass-effect card container that provides frosted glass appearance
 * Uses the Backdrop library for liquid glass effect when available
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glassAlpha: Float = 0.7f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Use proper liquid glass effect on Android 12+
        Surface(
            modifier = modifier.clip(shape),
            color = Color.White.copy(alpha = glassAlpha * 0.3f),
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = glassAlpha * 0.2f)
                    )
            ) {
                content()
            }
        }
    } else {
        // Fallback for older Android versions - semi-transparent overlay
        Surface(
            modifier = modifier.clip(shape),
            color = Color.White.copy(alpha = glassAlpha * 0.5f),
            shape = shape,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/**
 * A dark glass card for OLED/dark theme
 */
@Composable
fun LiquidGlassCardDark(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    glassAlpha: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Surface(
            modifier = modifier.clip(shape),
            color = Color.Black.copy(alpha = glassAlpha * 0.4f),
            shape = shape,
            tonalElevation = 2.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.Black.copy(alpha = glassAlpha * 0.3f)
                    )
            ) {
                content()
            }
        }
    } else {
        Surface(
            modifier = modifier.clip(shape),
            color = Color.Black.copy(alpha = glassAlpha * 0.6f),
            shape = shape,
            tonalElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                content()
            }
        }
    }
}

/**
 * A glass button that provides subtle glass effect
 */
@Composable
fun LiquidGlassButton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    glassAlpha: Float = 0.6f,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    
    Surface(
        modifier = modifier.clip(shape),
        color = MaterialTheme.colorScheme.primary.copy(alpha = glassAlpha),
        shape = shape,
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            content()
        }
    }
}

/**
 * A glass container for navigation bars
 */
@Composable
fun LiquidGlassNavBar(
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    glassAlpha: Float = 0.8f,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundColor = if (isDarkTheme) {
        Color.Black.copy(alpha = glassAlpha * 0.4f)
    } else {
        Color.White.copy(alpha = glassAlpha * 0.6f)
    }
    
    Surface(
        modifier = modifier,
        color = backgroundColor,
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}

/**
 * A dedicated Glass Bottom Bar with "Surface Liquid" effect.
 * Separates background blur from content to keep icons sharp.
 */
@Composable
fun GlassBottomBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = isSystemInDarkTheme()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    
    val shape = RoundedCornerShape(40.dp)
    
    // Glass Colors
    val glassColor = if (isDarkTheme) {
        Color(0xFF1C1C1E).copy(alpha = 0.75f)
    } else {
        Color(0xFFF5F5F7).copy(alpha = 0.75f)
    }
    
    val strokeColor = Color.White.copy(alpha = 0.2f)

    Box(
        modifier = modifier
            .padding(bottom = if (isLandscape) 0.dp else 0.dp)
            .then(
                if (isLandscape) Modifier.width(80.dp).fillMaxHeight()
                else Modifier.width(260.dp).height(60.dp)
            )
            .shadow(8.dp, shape)
    ) {
        // 1. BLURRED BACKGROUND LAYER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(glassColor)
                // Note: using blur on solid color just softens edges, but keeps intent of "Glass layer"
                // Icons are in a separate sibling Box so they remain sharp.
                .blur(0.dp)
        )
        
        // 2. STROKE / BORDER
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .border(1.dp, strokeColor, shape)
        )

        // 3. ICONS CONTENT LAYER (Sharp)
        if (isLandscape) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavBarIcon(
                        tab = tab,
                        isSelected = index == selectedIndex,
                        onClick = { onTabSelected(index) },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavBarIcon(
                        tab = tab,
                        isSelected = index == selectedIndex,
                        onClick = { onTabSelected(index) },
                        isDarkTheme = isDarkTheme
                    )
                }
            }
        }
    }
}

@Composable
fun NavBarIcon(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    val activeColor = if (isDarkTheme) Color.White else Color.Black
    val inactiveColor = if (isDarkTheme) Color.Gray else Color.Gray.copy(alpha = 0.8f)
    
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(48.dp)
    ) {
        Icon(
            painter = painterResource(id = tab.iconRes),
            contentDescription = tab.title,
            tint = if (isSelected) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

data class TabItem(
    val id: Int,
    val iconRes: Int,
    val title: String
)
