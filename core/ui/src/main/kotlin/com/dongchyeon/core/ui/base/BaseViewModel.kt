package com.dongchyeon.core.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel for MVI Architecture
 *
 * @param S UiState type
 * @param I Intent type
 * @param E SideEffect type
 */
abstract class BaseViewModel<S, I, E>(
    initialState: S,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    private val _sideEffect = Channel<E>()
    val sideEffect = _sideEffect.receiveAsFlow()

    abstract fun handleIntent(intent: I)

    protected fun updateState(reducer: (S) -> S) {
        _uiState.value = reducer(_uiState.value)
    }

    protected fun sendSideEffect(effect: E) {
        viewModelScope.launch {
            _sideEffect.send(effect)
        }
    }

    protected fun launchInScope(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(block = block)
    }

    protected val currentState: S
        get() = _uiState.value
}
