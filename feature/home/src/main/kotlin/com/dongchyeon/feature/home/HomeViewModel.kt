package com.dongchyeon.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val albumRepository: AlbumRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _sideEffect = Channel<HomeSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()
    
    init {
        handleIntent(HomeIntent.LoadAlbums)
    }
    
    fun handleIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LoadAlbums -> loadAlbums()
            is HomeIntent.Retry -> loadAlbums()
            is HomeIntent.NavigateToAlbum -> navigateToAlbum(intent.albumId)
        }
    }
    
    private fun loadAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            albumRepository.getAlbums()
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(
                            albums = albums,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "알 수 없는 오류가 발생했습니다"
                        )
                    }
                }
        }
    }
    
    private fun navigateToAlbum(albumId: String) {
        viewModelScope.launch {
            _sideEffect.send(HomeSideEffect.NavigateToAlbumDetail(albumId))
        }
    }
}
