package io.github.stream29.dashvoice.ime

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.stream29.dashvoice.DashVoiceApplication
import io.github.stream29.dashvoice.MainActivity
import io.github.stream29.dashvoice.R
import io.github.stream29.dashvoice.ui.VoiceImeScreen
import io.github.stream29.dashvoice.ui.theme.DashVoiceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class DashVoiceInputMethodService : InputMethodService() {
    private val serviceScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var viewTreeOwners: ImeViewTreeOwners
    private lateinit var viewModel: VoiceImeViewModel

    override fun onCreate() {
        super.onCreate()
        window.window?.setWindowAnimations(0)
        viewTreeOwners = ImeViewTreeOwners().apply { create() }
        viewModel = ViewModelProvider(
            viewTreeOwners,
            (application as DashVoiceApplication).container.viewModelFactory,
        )[VoiceImeViewModel::class.java]
        serviceScope.launch {
            viewModel.effects.collect(::handleEffect)
        }
    }

    override fun onCreateInputView(): View {
        window.window?.setWindowAnimations(0)
        window.window?.decorView?.installViewTreeOwners()
        return ComposeView(this).apply {
            installViewTreeOwners()
            setContent {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                DashVoiceTheme {
                    VoiceImeScreen(
                        state = state,
                        onStop = viewModel::stop,
                        onCancel = viewModel::cancelAndReturn,
                        onOpenSettings = ::openSettings,
                    )
                }
            }
        }
    }

    private fun View.installViewTreeOwners() {
        setViewTreeLifecycleOwner(viewTreeOwners)
        setViewTreeViewModelStoreOwner(viewTreeOwners)
        setViewTreeSavedStateRegistryOwner(viewTreeOwners)
    }

    override fun onStartInputView(
        info: EditorInfo?,
        restarting: Boolean,
    ) {
        super.onStartInputView(info, restarting)
        viewTreeOwners.start()
        viewModel.start()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        viewModel.deactivate()
        clearComposingText()
        viewTreeOwners.stop()
        super.onFinishInputView(finishingInput)
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onDestroy() {
        viewModel.deactivate()
        serviceScope.cancel()
        viewTreeOwners.destroy()
        super.onDestroy()
    }

    private fun handleEffect(effect: VoiceImeEffect) {
        when (effect) {
            is VoiceImeEffect.SetComposingText -> {
                currentInputConnection?.setComposingText(effect.text, 1)
            }

            is VoiceImeEffect.CommitAndReturn -> {
                currentInputConnection?.commitText(effect.text, 1)
                returnToPreviousInputMethod()
            }

            is VoiceImeEffect.FailAndReturn -> {
                clearComposingText()
                Toast.makeText(this, effect.messageRes, Toast.LENGTH_SHORT).show()
                returnToPreviousInputMethod()
            }

            VoiceImeEffect.CancelAndReturn -> {
                clearComposingText()
                returnToPreviousInputMethod()
            }
        }
    }

    private fun clearComposingText() {
        currentInputConnection?.apply {
            setComposingText("", 1)
            finishComposingText()
        }
    }

    private fun openSettings() {
        viewModel.deactivate()
        clearComposingText()
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP,
                ),
        )
        returnToPreviousInputMethod()
    }

    private fun returnToPreviousInputMethod() {
        val switched = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            switchToPreviousInputMethod()
        if (!switched) {
            Toast.makeText(
                this,
                R.string.ime_return_failed,
                Toast.LENGTH_SHORT,
            ).show()
            (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showInputMethodPicker()
        }
    }
}

private class ImeViewTreeOwners :
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    fun create() {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun start() {
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }
        if (!lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    fun stop() {
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }
        if (lifecycleRegistry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    fun destroy() {
        stop()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
