package ani.dantotsu.widgets

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.util.DampedDragAnimation
import ani.dantotsu.util.InteractiveHighlight
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

@Composable
fun LiquidBottomTabs(
    selectedTabIndex: () -> Int,
    onTabSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    preferSmoothTransitions: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val context = LocalContext.current
    val isLightTheme = !isSystemInDarkTheme()
    val config = remember(context, isLightTheme, preferSmoothTransitions) {
        LiquidGlassRuntimeConfig.create(context, isLightTheme).let {
            if (preferSmoothTransitions) it.preferSmoothTransitions() else it
        }
    }
    val accentColor =
        if (isLightTheme) Color(0xFF0088FF)
        else Color(0xFF0091FF)
    val glassAlpha: (Float) -> Float = { alpha ->
        (alpha * config.highlightStrength).fastCoerceIn(0f, 1f)
    }
    val containerColor = config.containerColor

    val tabsBackdrop = if (config.useSelectionBackdrop) rememberLayerBackdrop() else null
    val selectionBackdrop =
        if (tabsBackdrop != null) rememberCombinedBackdrop(backdrop, tabsBackdrop) else backdrop

    val maxBarWidth = (tabsCount * 84 + 8).dp

    BoxWithConstraints(
        modifier
            .widthIn(max = maxBarWidth)
            .fillMaxWidth()
            .height(64f.dp)
            .clip(ContinuousCapsule),
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8f.dp.toPx()) / tabsCount
        }

        var dragOffset by remember { mutableFloatStateOf(0f) }
        val panelOffset by remember(density) {
            derivedStateOf {
                val fraction = (dragOffset / constraints.maxWidth).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * config.motionScale * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var currentIndex by remember(selectedTabIndex) {
            mutableIntStateOf(selectedTabIndex())
        }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = selectedTabIndex().toFloat(),
                valueRange = 0f..(tabsCount - 1).toFloat(),
                visibilityThreshold = 0.001f,
                initialScale = 1f,
                pressedScale = 1f + 0.08f * config.motionScale,
                onDragStarted = {},
                onDragStopped = {
                    val targetIndex = targetValue.fastRoundToInt().fastCoerceIn(0, tabsCount - 1)
                    currentIndex = targetIndex
                    animateToValue(targetIndex.toFloat())
                    dragOffset = 0f
                },
                onDrag = { _, dragAmount ->
                    updateValue(
                        (targetValue + dragAmount.x / tabWidth * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    dragOffset += dragAmount.x
                }
            )
        }
        LaunchedEffect(selectedTabIndex) {
            snapshotFlow { selectedTabIndex() }
                .collectLatest { index ->
                    currentIndex = index
                }
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { currentIndex }
                .drop(1)
                .collectLatest { index ->
                    dampedDragAnimation.animateToValue(index.toFloat())
                    onTabSelected(index)
                }
        }

        val interactiveHighlight = remember(animationScope) {
            InteractiveHighlight(
                animationScope = animationScope,
                position = { size, offset ->
                    Offset(
                        if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 0.5f) * tabWidth + panelOffset,
                        size.height / 2f
                    )
                },
                intensity = { config.highlightStrength }
            )
        }

        Row(
            Modifier
                .graphicsLayer {
                    translationX = panelOffset
                }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        if (config.adaptiveContrast) {
                            colorControls(
                                brightness = config.backdropBrightness,
                                saturation = config.backdropSaturation
                            )
                        }
                        vibrancy()
                        blur(10f.dp.toPx() * config.effectStrength)
                        lens(
                            18f.dp.toPx() * config.effectStrength,
                            22f.dp.toPx() * config.effectStrength
                        )
                    },
                    highlight = {
                        Highlight.Default.copy(alpha = glassAlpha(if (isLightTheme) 0.12f else 0.18f))
                    },
                    shadow = {
                        Shadow(alpha = glassAlpha(if (isLightTheme) 0.14f else 0.28f))
                    },
                    innerShadow = {
                        InnerShadow(
                            radius = 10f.dp * config.effectStrength,
                            alpha = glassAlpha(if (isLightTheme) 0.16f else 0.28f)
                        )
                    },
                    layerBlock = {
                        val progress = dampedDragAnimation.pressProgress
                        val scale =
                            lerp(1f, 1f + 16f.dp.toPx() / size.width, progress * config.motionScale)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .clip(ContinuousCapsule)
                .liquidGlassRim(
                    isLightTheme = isLightTheme,
                    highlightStrength = config.highlightStrength,
                    contrastBoost = config.contrastBoost
                )
                .then(interactiveHighlight.modifier)
                .zIndex(1f)
                .height(64f.dp)
                .fillMaxWidth()
                .padding(4f.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides
                    config.contentColor
            ) {
                content()
            }
        }

        CompositionLocalProvider(
            LocalLiquidBottomTabScale provides {
                lerp(1f, 1.06f, dampedDragAnimation.pressProgress * config.motionScale)
            }
        ) {
            if (tabsBackdrop != null) {
                Row(
                    Modifier
                        .clearAndSetSemantics {}
                        .alpha(0f)
                        .layerBackdrop(tabsBackdrop)
                        .graphicsLayer {
                            translationX = panelOffset
                        }
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            effects = {
                                val progress = dampedDragAnimation.pressProgress
                                if (config.adaptiveContrast) {
                                    colorControls(
                                        brightness = config.backdropBrightness,
                                        saturation = config.backdropSaturation
                                    )
                                }
                                vibrancy()
                                blur(6f.dp.toPx() * config.effectStrength)
                                lens(
                                    16f.dp.toPx() * progress * config.effectStrength,
                                    16f.dp.toPx() * progress * config.effectStrength
                                )
                            },
                            highlight = {
                                val progress = dampedDragAnimation.pressProgress
                                Highlight.Default.copy(alpha = glassAlpha(progress * 0.85f))
                            },
                            onDrawSurface = { drawRect(containerColor) }
                        )
                        .clip(ContinuousCapsule)
                        .then(interactiveHighlight.modifier)
                        .height(56f.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 4f.dp)
                        .graphicsLayer(colorFilter = ColorFilter.tint(accentColor)),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .drawBackdrop(
                    backdrop = selectionBackdrop,
                    shape = { ContinuousCapsule },
                    effects = {
                        val progress = dampedDragAnimation.pressProgress
                        if (config.adaptiveContrast) {
                            colorControls(
                                brightness = config.backdropBrightness,
                                saturation = config.backdropSaturation
                            )
                        }
                        lens(
                            7f.dp.toPx() * progress * config.effectStrength,
                            10f.dp.toPx() * progress * config.effectStrength,
                            chromaticAberration = config.useChromaticAberration
                        )
                    },
                    highlight = {
                        val progress = dampedDragAnimation.pressProgress
                        Highlight.Default.copy(alpha = glassAlpha(0.18f + 0.62f * progress))
                    },
                    shadow = {
                        val progress = dampedDragAnimation.pressProgress
                        Shadow(alpha = glassAlpha(0.18f + 0.56f * progress))
                    },
                    innerShadow = {
                        val progress = dampedDragAnimation.pressProgress
                        InnerShadow(
                            radius = (3f.dp + 7f.dp * progress) * config.effectStrength,
                            alpha = glassAlpha(0.22f + 0.58f * progress)
                        )
                    },
                    layerBlock = {
                        scaleX = dampedDragAnimation.scaleX
                        scaleY = dampedDragAnimation.scaleY
                        val velocity = dampedDragAnimation.velocity / 12f * config.motionScale
                        scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                        scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                    },
                    onDrawSurface = {
                        val progress = dampedDragAnimation.pressProgress
                        drawRect(
                            if (isLightTheme) Color.White.copy(0.18f)
                            else Color.White.copy(0.11f),
                            alpha = 1f - progress * 0.4f
                        )
                        drawRect(
                            accentColor.copy(alpha = glassAlpha(0.05f + 0.07f * progress)),
                            blendMode = BlendMode.Plus
                        )
                    }
                )
                .clip(ContinuousCapsule)
                .liquidGlassRim(
                    isLightTheme = isLightTheme,
                    accentColor = accentColor,
                    highlightStrength = config.highlightStrength,
                    contrastBoost = config.contrastBoost,
                    pressProgress = { dampedDragAnimation.pressProgress }
                )
                .zIndex(0f)
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )

        Box(
            Modifier
                .padding(horizontal = 4f.dp)
                .graphicsLayer {
                    translationX =
                        if (isLtr) dampedDragAnimation.value * tabWidth + panelOffset
                        else size.width - (dampedDragAnimation.value + 1f) * tabWidth + panelOffset
                }
                .then(interactiveHighlight.gestureModifier)
                .then(dampedDragAnimation.modifier)
                .zIndex(2f)
                .height(56f.dp)
                .fillMaxWidth(1f / tabsCount)
        )
    }
}

private fun Modifier.liquidGlassRim(
    isLightTheme: Boolean,
    accentColor: Color? = null,
    highlightStrength: Float = 1f,
    contrastBoost: Float = 1f,
    pressProgress: () -> Float = { 0f }
): Modifier = drawWithContent {
    drawContent()

    val progress = pressProgress().fastCoerceIn(0f, 1f)
    val highlight = highlightStrength.fastCoerceIn(0.4f, 1.6f)
    val contrast = contrastBoost.fastCoerceIn(1f, 1.35f)
    val cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
    val topGlow = (if (isLightTheme) 0.30f else 0.22f) * highlight * contrast
    val bottomShade = (if (isLightTheme) 0.05f else 0.18f) * contrast

    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = (topGlow + 0.08f * progress * highlight).fastCoerceIn(0f, 1f)),
            0.42f to Color.White.copy(alpha = 0.04f * highlight),
            1f to Color.Black.copy(alpha = bottomShade),
            startY = 0f,
            endY = size.height
        ),
        cornerRadius = cornerRadius
    )

    if (accentColor != null) {
        drawRoundRect(
            color = accentColor.copy(alpha = ((0.05f + 0.08f * progress) * highlight).fastCoerceIn(0f, 1f)),
            cornerRadius = cornerRadius,
            blendMode = BlendMode.Plus
        )
    }

    drawRoundRect(
        color = Color.White.copy(
            alpha = ((if (isLightTheme) 0.42f else 0.24f) * highlight * contrast)
                .fastCoerceIn(0f, 1f)
        ),
        cornerRadius = cornerRadius,
        style = Stroke(width = 1f.dp.toPx()),
        blendMode = BlendMode.Plus
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = (if (isLightTheme) 0.05f else 0.24f) * contrast),
        cornerRadius = cornerRadius,
        style = Stroke(width = 0.7f.dp.toPx())
    )
}

private data class LiquidGlassRuntimeConfig(
    val effectStrength: Float,
    val highlightStrength: Float,
    val motionScale: Float,
    val contrastBoost: Float,
    val backdropBrightness: Float,
    val backdropSaturation: Float,
    val containerColor: Color,
    val contentColor: Color,
    val adaptiveContrast: Boolean,
    val useSelectionBackdrop: Boolean,
    val useChromaticAberration: Boolean
) {
    fun preferSmoothTransitions(): LiquidGlassRuntimeConfig = copy(
        effectStrength = (effectStrength * 0.78f).fastCoerceIn(0.35f, 1.1f),
        highlightStrength = (highlightStrength * 0.92f).fastCoerceIn(0.35f, 1.35f),
        motionScale = (motionScale * 0.75f).fastCoerceIn(0f, 0.82f),
        useSelectionBackdrop = false,
        useChromaticAberration = false
    )

    companion object {
        fun create(context: Context, isLightTheme: Boolean): LiquidGlassRuntimeConfig {
            val adaptiveContrast =
                PrefManager.getVal<Boolean>(PrefName.LiquidGlassAdaptiveContrast)
            val performanceScale =
                if (PrefManager.getVal<Boolean>(PrefName.LiquidGlassAutoPerformance)) {
                    detectPerformanceScale(context)
                } else {
                    1f
                }
            val requestedEffect =
                PrefManager.getVal<Float>(PrefName.LiquidGlassEffectStrength)
                    .fastCoerceIn(0.5f, 1.5f)
            val requestedOpacity =
                PrefManager.getVal<Float>(PrefName.LiquidGlassSurfaceOpacity)
                    .fastCoerceIn(0.6f, 1.4f)
            val requestedHighlight =
                PrefManager.getVal<Float>(PrefName.LiquidGlassHighlightStrength)
                    .fastCoerceIn(0.4f, 1.6f)
            val motionEnabled = PrefManager.getVal<Boolean>(PrefName.LiquidGlassMotion)

            val effectStrength =
                (requestedEffect * performanceScale).fastCoerceIn(0.35f, 1.5f)
            val highlightStrength =
                (requestedHighlight * if (performanceScale < 0.7f) 0.82f else 1f)
                    .fastCoerceIn(0.35f, 1.6f)
            val motionScale = when {
                !motionEnabled -> 0f
                performanceScale < 0.7f -> 0.55f
                performanceScale < 0.9f -> 0.82f
                else -> 1f
            }
            val adaptiveOpacityBoost = if (adaptiveContrast) 0.08f else 0f
            val containerAlpha =
                if (isLightTheme) {
                    (0.34f * requestedOpacity + adaptiveOpacityBoost)
                        .fastCoerceIn(0.18f, 0.74f)
                } else {
                    (0.42f * requestedOpacity + adaptiveOpacityBoost)
                        .fastCoerceIn(0.22f, 0.80f)
                }

            return LiquidGlassRuntimeConfig(
                effectStrength = effectStrength,
                highlightStrength = highlightStrength,
                motionScale = motionScale,
                contrastBoost = if (adaptiveContrast) {
                    if (isLightTheme) 1.12f else 1.22f
                } else {
                    1f
                },
                backdropBrightness = if (adaptiveContrast) {
                    if (isLightTheme) -0.04f else 0.06f
                } else {
                    0f
                },
                backdropSaturation = if (adaptiveContrast) 1.08f else 1f,
                containerColor = if (isLightTheme) {
                    Color(0xFFFAFAFA).copy(alpha = containerAlpha)
                } else {
                    Color(0xFF101214).copy(alpha = containerAlpha)
                },
                contentColor = if (isLightTheme) {
                    Color.Black.copy(alpha = if (adaptiveContrast) 0.74f else 0.60f)
                } else {
                    Color.White.copy(alpha = if (adaptiveContrast) 0.96f else 0.90f)
                },
                adaptiveContrast = adaptiveContrast,
                useSelectionBackdrop = performanceScale >= 0.7f && effectStrength >= 0.55f,
                useChromaticAberration = performanceScale >= 0.9f && requestedEffect >= 1f
            )
        }

        private fun detectPerformanceScale(context: Context): Float {
            val memoryClass =
                (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
                    ?.memoryClass ?: 256
            val processors = Runtime.getRuntime().availableProcessors()
            return when {
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.P ||
                    memoryClass <= 192 ||
                    processors <= 4 -> 0.58f

                Build.VERSION.SDK_INT <= Build.VERSION_CODES.R ||
                    memoryClass <= 256 ||
                    processors <= 6 -> 0.78f

                else -> 1f
            }
        }
    }
}
