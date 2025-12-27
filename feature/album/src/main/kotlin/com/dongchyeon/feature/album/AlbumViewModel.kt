package com.dongchyeon.feature.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
        loadAlbumData()
    }
    
    fun handleIntent(intent: AlbumIntent) {
        when (intent) {
            is AlbumIntent.Retry -> loadAlbumData()
            is AlbumIntent.PlayTrack -> playTrack(intent.track)
            is AlbumIntent.NavigateBack -> navigateBack()
        }
    }
    
    private fun loadAlbumData() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }
            
            try {
                // 앨범 정보와 트랙 목록을 병렬로 로드
                val albumDeferred = async { albumRepository.getAlbumById(albumId) }
                val tracksDeferred = async { albumRepository.getTracksByAlbumId(albumId) }
                
                val albumResult = albumDeferred.await()
                val tracksResult = tracksDeferred.await()
                
                if (albumResult.isSuccess && tracksResult.isSuccess) {
                    val album = albumResult.getOrNull()!!
                    val tracks = tracksResult.getOrNull()!!
                    
                    _uiState.update {
                        it.copy(
                            album = album.copy(tracks = tracks),
                            isLoading = false,
                            error = null
                        )
                    }
                } else {
                    val error = albumResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error?.message ?: "알 수 없는 오류가 발생했습니다"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류가 발생했습니다"
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
