package ani.dantotsu.home

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.ZoomOutPageTransformer
import ani.dantotsu.databinding.ActivityNoInternetBinding
import ani.dantotsu.download.anime.OfflineAnimeFragment
import ani.dantotsu.download.manga.OfflineMangaFragment
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.offline.OfflineFragment
import ani.dantotsu.selectedOption
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import ani.dantotsu.widgets.LiquidBottomTabs
import ani.dantotsu.widgets.LiquidBottomTab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import com.kyant.backdrop.backdrops.layerBackdrop
import eightbitlab.com.blurview.BlurView
import com.google.android.material.bottomnavigation.BottomNavigationView


class NoInternet : AppCompatActivity() {
    private lateinit var binding: ActivityNoInternetBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()

        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // LiquidGlassBottomBar handles its own glass background drawing


        var doubleBackToExitPressedOnce = false
        onBackPressedDispatcher.addCallback(this) {
            if (doubleBackToExitPressedOnce) {
                finishAffinity()
            }
            doubleBackToExitPressedOnce = true
            snackString(this@NoInternet.getString(R.string.back_to_exit))
            Handler(Looper.getMainLooper()).postDelayed(
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        binding.root.doOnAttach {
            initActivity(this)
            selectedOption = PrefManager.getVal(PrefName.DefaultStartUpTab)

            binding.includedNavbar.navbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
        }

        // Check if Liquid Glass theme is active
        val isLiquidGlassTheme = PrefManager.getVal<String>(PrefName.Theme) == "LIQUID_GLASS"
        
        if (isLiquidGlassTheme) {
            // Use Compose HorizontalPager for Liquid Glass theme
            binding.viewpager.visibility = View.GONE
            binding.includedNavbar.root.visibility = View.GONE
            binding.composeMainContent.visibility = View.VISIBLE
            
            binding.composeMainContent.setContent {
                val pagerState = rememberPagerState(
                    initialPage = selectedOption,
                    pageCount = { 3 }
                )
                val coroutineScope = rememberCoroutineScope()
                val backdrop = rememberLayerBackdrop()

                // Sync pager to selectedOption changes ONLY
                LaunchedEffect(selectedOption) {
                    if (pagerState.currentPage != selectedOption) {
                        pagerState.scrollToPage(selectedOption)
                    }
                }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content pager (offline fragments)
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop)
                    ) { page ->
                        when (page) {
                            0 -> OfflineAnimePageComposable(supportFragmentManager)
                            1 -> OfflineHomePageComposable(supportFragmentManager)
                            2 -> OfflineMangaPageComposable(supportFragmentManager)
                        }
                    }
                    
                    LiquidBottomTabs(
                        selectedTabIndex = { selectedOption },
                        onTabSelected = { index ->
                            selectedOption = index
                            coroutineScope.launch { pagerState.scrollToPage(index) }
                        },
                        backdrop = backdrop,
                        tabsCount = 3,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                            .padding(12.dp)
                    ) {
                        LiquidBottomTab(onClick = {
                            selectedOption = 0
                            coroutineScope.launch { pagerState.scrollToPage(0) }
                        }) {
                            Icon(painterResource(R.drawable.ic_round_movie_filter_24), contentDescription = stringResource(R.string.anime))
                            Text(stringResource(R.string.anime))
                        }

                        LiquidBottomTab(onClick = {
                            selectedOption = 1
                            coroutineScope.launch { pagerState.scrollToPage(1) }
                        }) {
                            Icon(painterResource(R.drawable.ic_round_home_24), contentDescription = stringResource(R.string.home))
                            Text(stringResource(R.string.home))
                        }

                        LiquidBottomTab(onClick = {
                            selectedOption = 2
                            coroutineScope.launch { pagerState.scrollToPage(2) }
                        }) {
                            Icon(painterResource(R.drawable.ic_round_import_contacts_24), contentDescription = stringResource(R.string.manga))
                            Text(stringResource(R.string.manga))
                        }
                    }
                }
            }
        } else {
            // Use ViewPager2 for other themes
            binding.viewpager.visibility = View.VISIBLE
            binding.includedNavbar.root.visibility = View.VISIBLE
            binding.composeMainContent.visibility = View.GONE
            
            val mainViewPager = binding.viewpager
            val navbar = binding.includedNavbar.navbar
            
            mainViewPager.isUserInputEnabled = false
            mainViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
            mainViewPager.setPageTransformer(ZoomOutPageTransformer())

            navbar.setOnItemSelectedListener { item ->
               when (item.itemId) {
                    R.id.anime -> {
                        selectedOption = 0
                        mainViewPager.setCurrentItem(0, false)
                        true
                    }
                    R.id.home -> {
                        selectedOption = 1
                        mainViewPager.setCurrentItem(1, false)
                        true
                    }
                    R.id.manga -> {
                        selectedOption = 2
                        mainViewPager.setCurrentItem(2, false)
                        true
                    }
                    else -> false
                }
            }

            if (mainViewPager.currentItem != selectedOption) {
                mainViewPager.post {
                    mainViewPager.setCurrentItem(selectedOption, false)
                    navbar.selectedItemId = when(selectedOption) {
                        0 -> R.id.anime
                        1 -> R.id.home
                        2 -> R.id.manga
                        else -> R.id.home
                    }
                }
            }
        }
    }


    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OfflineAnimeFragment()
                2 -> OfflineMangaFragment()
                else -> OfflineFragment()
            }
        }
    }
}

