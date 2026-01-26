package ani.dantotsu.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ani.dantotsu.connections.anilist.Anilist

/**
 * Composable wrappers for fragments to enable Compose-based navigation
 * Used only when Liquid Glass theme is selected for backdrop blur effects
 */

@Composable
fun AnimePageComposable(fragmentManager: FragmentManager) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val containerId = remember { android.view.View.generateViewId() }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Fragment will handle its own refresh on resume
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                id = containerId
            }
        },
        update = { container ->
            // Check if fragment already exists
            val existingFragment = fragmentManager.findFragmentById(container.id)
            if (existingFragment == null) {
                fragmentManager.beginTransaction()
                    .replace(container.id, AnimeFragment())
                    .commitNowAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun HomePageComposable(fragmentManager: FragmentManager) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val containerId = remember { android.view.View.generateViewId() }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Fragment will handle its own refresh on resume
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                id = containerId
            }
        },
        update = { container ->
            val existingFragment = fragmentManager.findFragmentById(container.id)
            if (existingFragment == null) {
                val fragment = if (Anilist.token != null) HomeFragment() else LoginFragment()
                fragmentManager.beginTransaction()
                    .replace(container.id, fragment)
                    .commitNowAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun MangaPageComposable(fragmentManager: FragmentManager) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val containerId = remember { android.view.View.generateViewId() }
    
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Fragment will handle its own refresh on resume
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                id = containerId
            }
        },
        update = { container ->
            val existingFragment = fragmentManager.findFragmentById(container.id)
            if (existingFragment == null) {
                fragmentManager.beginTransaction()
                    .replace(container.id, MangaFragment())
                    .commitNowAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// Offline mode Composable pages
@Composable
fun OfflineAnimePageComposable(fragmentManager: FragmentManager) {
    val containerId = remember { android.view.View.generateViewId() }
    
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                id = containerId
            }
        },
        update = { container ->
            val existingFragment = fragmentManager.findFragmentById(container.id)
            if (existingFragment == null) {
                fragmentManager.beginTransaction()
                    .replace(container.id, ani.dantotsu.download.anime.OfflineAnimeFragment())
                    .commitNowAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun OfflineHomePageComposable(fragmentManager: FragmentManager) {
    val containerId = remember { android.view.View.generateViewId() }
    
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                id = containerId
            }
        },
        update = { container ->
            val existingFragment = fragmentManager.findFragmentById(container.id)
            if (existingFragment == null) {
                fragmentManager.beginTransaction()
                    .replace(container.id, ani.dantotsu.offline.OfflineFragment())
                    .commitNowAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun OfflineMangaPageComposable(fragmentManager: FragmentManager) {
    val containerId = remember { android.view.View.generateViewId() }
    
    AndroidView(
        factory = { context ->
            FragmentContainerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                id = containerId
            }
        },
        update = { container ->
            val existingFragment = fragmentManager.findFragmentById(container.id)
            if (existingFragment == null) {
                fragmentManager.beginTransaction()
                    .replace(container.id, ani.dantotsu.download.manga.OfflineMangaFragment())
                    .commitNowAllowingStateLoss()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
