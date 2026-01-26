package ani.dantotsu.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.themes.liquidglass.TabItem
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.launch

/**
 * A Compose wrapper that provides glass effect backdrop for an existing XML View.
 * This allows any View to be wrapped with a LiquidBottomTabs glass bottom bar.
 * Only applies glass effect for Liquid Glass theme.
 */
class LiquidGlassWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {
    
    private var tabs by mutableStateOf(listOf<TabItem>())
    private var selectedIndex by mutableIntStateOf(0)
    private var tabSelectListener: OnTabSelectListener? = null
    
    // The content view to wrap
    private var contentView: View? = null
    
    // Cache theme check
    private val isLiquidGlassTheme: Boolean by lazy {
        PrefManager.getVal<String>(PrefName.Theme) == "LIQUID_GLASS"
    }
    
    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }
    
    /**
     * Set the content view to wrap with glass effect
     */
    fun setContentView(view: View) {
        (view.parent as? ViewGroup)?.removeView(view)
        contentView = view
    }
    
    @Composable
    override fun Content() {
        if (isLiquidGlassTheme) {
            LiquidGlassContent()
        } else {
            StandardContent()
        }
    }
    
    @Composable
    private fun LiquidGlassContent() {
        val backdrop = rememberLayerBackdrop()
        val coroutineScope = rememberCoroutineScope()
        
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                contentView?.let { view ->
                    AndroidView(
                        factory = { view },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Keep the bottom tabs stable with key
            key("bottom_tabs") {
                LiquidBottomTabs(
                    selectedTabIndex = { selectedIndex },
                    onTabSelected = { index ->
                        handleTabSelection(index)
                    },
                    backdrop = backdrop,
                    tabsCount = tabs.size.coerceAtLeast(1),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                        .padding(12.dp)
                ) {
                    tabs.forEachIndexed { index, tab ->
                        LiquidBottomTab(onClick = {
                            coroutineScope.launch {
                                handleTabSelection(index)
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = tab.iconRes),
                                contentDescription = tab.title
                            )
                            Text(tab.title)
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun StandardContent() {
        val isDark = isSystemInDarkTheme()
        // Semi-transparent pill-shaped background (same as home navbar)
        val bgColor = if (isDark) Color(0xD9121212) else Color(0xD9F5F5F7)
        val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)
        val activeColor = if (isDark) Color.White else Color.Black
        val inactiveColor = Color.Gray
        val shape = RoundedCornerShape(40.dp) // Pill shape
        
        Box(modifier = Modifier.fillMaxSize()) {
            contentView?.let { view ->
                AndroidView(
                    factory = { view },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(8.dp, shape)
                    .clip(shape)
                    .background(bgColor)
                    .border(1.dp, borderColor, shape),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    IconButton(
                        onClick = { handleTabSelection(index) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.title,
                            tint = if (index == selectedIndex) activeColor else inactiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
    
    private fun handleTabSelection(index: Int) {
        if (index == selectedIndex) return
        
        val oldIndex = selectedIndex
        selectedIndex = index
        
        if (tabs.isNotEmpty()) {
            val oldTab = if (oldIndex >= 0 && oldIndex < tabs.size) {
                val t = tabs[oldIndex]
                LiquidGlassBottomBar.Tab(t.id, t.iconRes, t.title)
            } else null
            
            val newTab = tabs[index]
            tabSelectListener?.onTabSelected(
                oldIndex,
                oldTab,
                index,
                LiquidGlassBottomBar.Tab(newTab.id, newTab.iconRes, newTab.title)
            )
        }
    }
    
    fun addTab(tab: LiquidGlassBottomBar.Tab) {
        val newItem = TabItem(tab.id, tab.iconRes, tab.title)
        tabs = tabs + newItem
    }
    
    fun createTab(@DrawableRes iconRes: Int, title: String, id: Int = View.generateViewId()): LiquidGlassBottomBar.Tab {
        return LiquidGlassBottomBar.Tab(id, iconRes, title)
    }
    
    fun selectTabAt(index: Int) {
        if (index >= 0 && index < tabs.size) {
            selectedIndex = index
        }
    }
    
    fun setOnTabSelectListener(listener: OnTabSelectListener) {
        tabSelectListener = listener
    }
    
    interface OnTabSelectListener {
        fun onTabSelected(oldIndex: Int, oldTab: LiquidGlassBottomBar.Tab?, newIndex: Int, newTab: LiquidGlassBottomBar.Tab)
    }
}

/**
 * LiquidGlassBottomBar - uses LiquidBottomTabs with backdrop for Liquid Glass theme,
 * falls back to standard styling for other themes.
 */
class LiquidGlassBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

    private var tabs by mutableStateOf(listOf<TabItem>())
    private var selectedIndex by mutableIntStateOf(0)
    private var tabSelectListener: OnTabSelectListener? = null
    
    // Cache theme check
    private val isLiquidGlassTheme: Boolean by lazy {
        PrefManager.getVal<String>(PrefName.Theme) == "LIQUID_GLASS"
    }

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    @Composable
    override fun Content() {
        if (isLiquidGlassTheme) {
            // Use key to keep the entire glass content stable
            key("liquid_glass_bar") {
                LiquidGlassContent()
            }
        } else {
            StandardContent()
        }
    }
    
    @Composable
    private fun LiquidGlassContent() {
        // Use rememberLayerBackdrop directly (it's already a remembered composable)
        val backdrop = rememberLayerBackdrop()
        val coroutineScope = rememberCoroutineScope()
        
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            )
            
            LiquidBottomTabs(
                selectedTabIndex = { selectedIndex },
                onTabSelected = { index ->
                    if (index != selectedIndex) {
                        selectTabAtInternal(index)
                    }
                },
                backdrop = backdrop,
                tabsCount = tabs.size.coerceAtLeast(1),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(12.dp)
            ) {
                tabs.forEachIndexed { index, tab ->
                    LiquidBottomTab(onClick = {
                        if (index != selectedIndex) {
                            coroutineScope.launch { selectTabAtInternal(index) }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.title
                        )
                        Text(tab.title)
                    }
                }
            }
        }
    }
    
    @Composable
    private fun StandardContent() {
        val isDark = isSystemInDarkTheme()
        // Semi-transparent pill-shaped background (same as home navbar)
        val bgColor = if (isDark) Color(0xD9121212) else Color(0xD9F5F5F7)
        val borderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f)
        val activeColor = if (isDark) Color.White else Color.Black
        val inactiveColor = Color.Gray
        val shape = RoundedCornerShape(40.dp) // Pill shape
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .shadow(8.dp, shape)
                    .clip(shape)
                    .background(bgColor)
                    .border(1.dp, borderColor, shape),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, tab ->
                    IconButton(
                        onClick = { selectTabAtInternal(index) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = tab.iconRes),
                            contentDescription = tab.title,
                            tint = if (index == selectedIndex) activeColor else inactiveColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Internal selection without triggering listener (for Compose callbacks)
    private fun selectTabAtInternal(index: Int) {
        val currentTabs = tabs
        if (index >= 0 && index < currentTabs.size && index != selectedIndex) {
            val oldIndex = selectedIndex
            selectedIndex = index
            
            val oldTabItem = if (oldIndex >= 0 && oldIndex < currentTabs.size) currentTabs[oldIndex] else null
            val newTabItem = currentTabs[index]
            
            tabSelectListener?.onTabSelected(
                oldIndex,
                if (oldTabItem != null) Tab(oldTabItem.id, oldTabItem.iconRes, oldTabItem.title) else null,
                index,
                Tab(newTabItem.id, newTabItem.iconRes, newTabItem.title)
            )
        }
    }

    data class Tab(val id: Int, val iconRes: Int, val title: String)

    fun createTab(@DrawableRes iconRes: Int, titleRes: Int, id: Int = View.generateViewId()): Tab {
        val title = context.getString(titleRes)
        return Tab(id, iconRes, title)
    }

    fun createTab(@DrawableRes iconRes: Int, title: String, id: Int = View.generateViewId()): Tab {
        return Tab(id, iconRes, title)
    }

    fun addTab(tab: Tab) {
        val newItem = TabItem(tab.id, tab.iconRes, tab.title)
        tabs = tabs + newItem
    }
    
    // External selection (from Activity code like onResume)
    fun selectTabAt(index: Int, animate: Boolean = true) {
        if (index >= 0 && index < tabs.size) {
            selectedIndex = index
        }
    }
    
    fun selectTabById(tabId: Int) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index != -1) {
            selectTabAt(index)
        }
    }
    
    fun setOnTabSelectListener(listener: OnTabSelectListener) {
        tabSelectListener = listener
    }
    
    fun clearTabs() {
        tabs = emptyList()
        selectedIndex = 0
    }
    
    val selectedTab: Tab?
        get() {
            val idx = selectedIndex
            val t = tabs.getOrNull(idx)
            return if (t != null) Tab(t.id, t.iconRes, t.title) else null
        }
        
    val selectedTabId: Int
        get() = selectedTab?.id ?: -1
    
    interface OnTabSelectListener {
        fun onTabSelected(oldIndex: Int, oldTab: Tab?, newIndex: Int, newTab: Tab)
    }
}
