package ani.dantotsu.settings

import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import ani.dantotsu.databinding.ActivitySettingsLiquidGlassBinding
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.restartApp
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.statusBarHeight
import ani.dantotsu.themes.ThemeManager
import com.google.android.material.slider.Slider
import kotlin.math.roundToInt

class SettingsLiquidGlassActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsLiquidGlassBinding
    private var suppressPresetListener = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()
        binding = ActivitySettingsLiquidGlassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivity(this)
        binding.liquidGlassSettingsContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = statusBarHeight
            bottomMargin = navBarHeight
        }

        binding.liquidGlassSettingsBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        setupPresets()

        binding.liquidGlassAutoPerformance.isChecked =
            PrefManager.getVal(PrefName.LiquidGlassAutoPerformance)
        binding.liquidGlassAutoPerformance.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.LiquidGlassAutoPerformance, isChecked)
            restartApp()
        }

        binding.liquidGlassAdaptiveContrast.isChecked =
            PrefManager.getVal(PrefName.LiquidGlassAdaptiveContrast)
        binding.liquidGlassAdaptiveContrast.setOnCheckedChangeListener { _, isChecked ->
            PrefManager.setVal(PrefName.LiquidGlassAdaptiveContrast, isChecked)
            restartApp()
        }

        binding.liquidGlassMotion.isChecked = PrefManager.getVal(PrefName.LiquidGlassMotion)
        binding.liquidGlassMotion.setOnCheckedChangeListener { _, isChecked ->
            if (suppressPresetListener) return@setOnCheckedChangeListener
            PrefManager.setVal(PrefName.LiquidGlassMotion, isChecked)
            restartApp()
        }

        bindRestartingSlider(
            binding.liquidGlassEffectStrength,
            PrefName.LiquidGlassEffectStrength,
            0.5f,
            1.5f
        )
        bindRestartingSlider(
            binding.liquidGlassSurfaceOpacity,
            PrefName.LiquidGlassSurfaceOpacity,
            0.6f,
            1.4f
        )
        bindRestartingSlider(
            binding.liquidGlassHighlightStrength,
            PrefName.LiquidGlassHighlightStrength,
            0.4f,
            1.6f
        )

        binding.liquidGlassReset.setOnClickListener {
            PrefManager.setVal(PrefName.LiquidGlassPreset, PRESET_BALANCED)
            PrefManager.setVal(PrefName.LiquidGlassAutoPerformance, true)
            PrefManager.setVal(PrefName.LiquidGlassAdaptiveContrast, true)
            PrefManager.setVal(PrefName.LiquidGlassMotion, true)
            PrefManager.setVal(PrefName.LiquidGlassEffectStrength, 1f)
            PrefManager.setVal(PrefName.LiquidGlassSurfaceOpacity, 1f)
            PrefManager.setVal(PrefName.LiquidGlassHighlightStrength, 1f)
            restartApp()
        }
    }

    private fun setupPresets() {
        binding.liquidGlassPresetGroup.check(
            when (PrefManager.getVal<Int>(PrefName.LiquidGlassPreset)) {
                PRESET_SOFT -> binding.liquidGlassPresetSoft.id
                PRESET_FROSTED -> binding.liquidGlassPresetFrosted.id
                PRESET_ULTRA -> binding.liquidGlassPresetUltra.id
                else -> binding.liquidGlassPresetBalanced.id
            }
        )

        binding.liquidGlassPresetGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || suppressPresetListener) return@addOnButtonCheckedListener
            val preset = when (checkedId) {
                binding.liquidGlassPresetSoft.id -> PRESET_SOFT
                binding.liquidGlassPresetFrosted.id -> PRESET_FROSTED
                binding.liquidGlassPresetUltra.id -> PRESET_ULTRA
                else -> PRESET_BALANCED
            }
            applyPreset(preset)
        }
    }

    private fun applyPreset(preset: Int) {
        val values = when (preset) {
            PRESET_SOFT -> LiquidGlassPresetValues(0.75f, 0.80f, 0.70f, true)
            PRESET_FROSTED -> LiquidGlassPresetValues(1.25f, 1.10f, 1.20f, true)
            PRESET_ULTRA -> LiquidGlassPresetValues(1.50f, 1.30f, 1.40f, true)
            else -> LiquidGlassPresetValues(1f, 1f, 1f, true)
        }

        PrefManager.setVal(PrefName.LiquidGlassPreset, preset)
        PrefManager.setVal(PrefName.LiquidGlassEffectStrength, values.effectStrength)
        PrefManager.setVal(PrefName.LiquidGlassSurfaceOpacity, values.surfaceOpacity)
        PrefManager.setVal(PrefName.LiquidGlassHighlightStrength, values.highlightStrength)
        PrefManager.setVal(PrefName.LiquidGlassMotion, values.motion)

        suppressPresetListener = true
        binding.liquidGlassEffectStrength.value = values.effectStrength
        binding.liquidGlassSurfaceOpacity.value = values.surfaceOpacity
        binding.liquidGlassHighlightStrength.value = values.highlightStrength
        binding.liquidGlassMotion.isChecked = values.motion
        suppressPresetListener = false

        restartApp()
    }

    private fun bindRestartingSlider(
        slider: Slider,
        prefName: PrefName,
        min: Float,
        max: Float
    ) {
        var shouldRestart = false
        val value = slider.snapToStep(PrefManager.getVal<Float>(prefName).coerceIn(min, max))
        slider.value = value
        PrefManager.setVal(prefName, value)
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                PrefManager.setVal(prefName, value)
                shouldRestart = true
            }
        }
        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) = Unit

            override fun onStopTrackingTouch(slider: Slider) {
                if (shouldRestart) {
                    shouldRestart = false
                    restartApp()
                }
            }
        })
    }

    private fun Slider.snapToStep(value: Float): Float {
        if (stepSize <= 0f) return value.coerceIn(valueFrom, valueTo)
        val steps = ((value - valueFrom) / stepSize).roundToInt()
        return (valueFrom + steps * stepSize).coerceIn(valueFrom, valueTo)
    }

    private data class LiquidGlassPresetValues(
        val effectStrength: Float,
        val surfaceOpacity: Float,
        val highlightStrength: Float,
        val motion: Boolean
    )

    private companion object {
        const val PRESET_SOFT = 0
        const val PRESET_BALANCED = 1
        const val PRESET_FROSTED = 2
        const val PRESET_ULTRA = 3
    }
}
