package com.dongchyeon.feature.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongchyeon.domain.model.Track
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
class AlbumViewModel @Inject constructor(
    private val albumRepository: AlbumRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val albumId: String = savedStateHandle["albumId"] ?: ""
    
    private val _uiState = MutableStateFlow(AlbumUiState())
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()
    
    private val _sideEffect = Channel<AlbumSideEffect>()
    val sideEffect = _sideEffect.receiveAsFlow()
    
    init {
        if (albumId.isNotEmpty()) {
            handleIntent(AlbumIntent.LoadAlbum(albumId))
        }
    }
    
    fun handleIntent(intent: AlbumIntent) {
        when (intent) {
            is AlbumIntent.LoadAlbum -> loadAlbum(intent.albumId)
            is AlbumIntent.Retry -> loadAlbum(albumId)
            is AlbumIntent.PlayTrack -> playTrack(intent.track)
            is AlbumIntent.NavigateBack -> navigateBack()
        }
    }
    
    private fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            albumRepository.getAlbumById(albumId)
                .onSuccess { album ->
                    _uiState.update {
                        it.copy(
                            album = album,
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
    
    private fun playTrack(track: Track) {
        viewModelScope.launch {
            _sideEffect.send(AlbumSideEffect.NavigateToPlayer(track))
        }
    }
    
    private fun navigateBack() {
        viewModelScope.launch {
            _sideEffect.send(AlbumSideEffect.NavigateBack)
        }
    }
}
