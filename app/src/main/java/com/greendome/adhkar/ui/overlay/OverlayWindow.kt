package com.greendome.adhkar.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.greendome.adhkar.data.SettingsRepository
import com.greendome.adhkar.data.model.AppThemeMode
import com.greendome.adhkar.ui.theme.AppArabicFont
import com.greendome.adhkar.ui.theme.GreenDomeTheme

/** مالك دورة حياة للنافذة العائمة — Compose يحتاجه خارج Activity */
private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val vmStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = registry
    override val viewModelStore: ViewModelStore get() = vmStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun onCreate() {
        savedStateController.performRestore(null)
        registry.currentState = Lifecycle.State.CREATED
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun onDestroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        vmStore.clear()
    }
}

/** نافذة عائمة فوق التطبيقات — تعمل والشاشة مفتوحة */
object OverlayWindow {
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun showTasbih(context: Context, text: String) {
        if (!hasPermission(context)) return
        val settings = SettingsRepository(context.applicationContext)
        showInternal(
            context = context,
            themeMode = settings.appThemeMode,
            arabicFontStyle = settings.arabicFontStyle,
            content = {
                DhikrOverlayCard(
                    text = text,
                    appearance = settings.tasbihPopupAppearance(),
                    autoDismissSeconds = settings.tasbihPopupAutoDismissSeconds,
                    onDismiss = { dismiss(context) }
                )
            }
        )
    }

    @Deprecated("Use showTasbih", ReplaceWith("showTasbih(context, text)"))
    fun show(context: Context, text: String) = showTasbih(context, text)

    fun showAzkarPreview(context: Context, sectionTitle: String, text: String) {
        if (!hasPermission(context)) return
        val settings = SettingsRepository(context.applicationContext)
        showAutoAzkar(
            context = context,
            sectionTitle = sectionTitle,
            text = text,
            onDismiss = { dismiss(context) },
            onStopAuto = { dismiss(context) }
        )
    }

    fun showAutoAzkar(
        context: Context,
        sectionTitle: String,
        text: String,
        onDismiss: () -> Unit,
        onStopAuto: () -> Unit
    ) {
        if (!hasPermission(context)) return
        val settings = SettingsRepository(context.applicationContext)
        showInternal(
            context = context,
            themeMode = settings.appThemeMode,
            arabicFontStyle = settings.arabicFontStyle,
            content = {
                AutoAzkarOverlayCard(
                    sectionTitle = sectionTitle,
                    text = text,
                    appearance = settings.azkarPopupAppearance(),
                    autoDismissSeconds = settings.azkarPopupAutoDismissSeconds,
                    onDismiss = {
                        onDismiss()
                        dismiss(context)
                    },
                    onStopAuto = {
                        onStopAuto()
                        dismiss(context)
                    }
                )
            }
        )
    }

    private fun showInternal(
        context: Context,
        themeMode: AppThemeMode,
        arabicFontStyle: com.greendome.adhkar.data.model.ArabicFontStyle,
        content: @Composable () -> Unit
    ) {
        dismiss(context)
        val app = context.applicationContext
        val wm = app.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val owner = OverlayLifecycleOwner().also { it.onCreate() }
        val view = ComposeView(app).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                GreenDomeTheme(themeMode = themeMode) {
                    AppArabicFont(arabicFontStyle) { content() }
                }
            }
        }
        lifecycleOwner = owner
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            dimAmount = 0.5f
        }
        wm.addView(view, params)
        composeView = view
    }

    fun dismiss(context: Context) {
        val view = composeView ?: return
        runCatching {
            (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .removeView(view)
        }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        composeView = null
    }

    fun isShowing(): Boolean = composeView != null
}
