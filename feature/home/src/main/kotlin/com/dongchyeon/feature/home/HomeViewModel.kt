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
            is HomeIntent.LoadMoreAlbums -> loadMoreAlbums()
            is HomeIntent.Retry -> retry()
            is HomeIntent.NavigateToAlbum -> navigateToAlbum(intent.albumId)
        }
    }
    
    private fun loadAlbums() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            albumRepository.getAlbums(page = 0, limit = PaginationState.PAGE_SIZE)
                .onSuccess { albums ->
                    _uiState.update {
                        it.copy(
                            albums = albums,
                            isLoading = false,
                            error = null,
                            paginationState = PaginationState(
                                currentPage = 0,
                                isLoadingMore = false,
                                hasMoreData = albums.size >= PaginationState.PAGE_SIZE
                            )
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
    
    private fun loadMoreAlbums() {
        val currentState = _uiState.value
        val pagination = currentState.paginationState
        
        // 이미 로딩 중이거나 더 이상 데이터가 없으면 리턴
        if (pagination.isLoadingMore || !pagination.hasMoreData) return
        
        viewModelScope.launch {
            val nextPage = pagination.currentPage + 1
            _uiState.update { 
                it.copy(
                    paginationState = pagination.copy(isLoadingMore = true)
                ) 
            }
            
            albumRepository.getAlbums(page = nextPage, limit = PaginationState.PAGE_SIZE)
                .onSuccess { newAlbums ->
                    _uiState.update {
                        it.copy(
                            albums = it.albums + newAlbums,
                            paginationState = pagination.copy(
                                currentPage = nextPage,
                                isLoadingMore = false,
                                hasMoreData = newAlbums.size >= PaginationState.PAGE_SIZE
                            )
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            paginationState = pagination.copy(isLoadingMore = false),
                            error = exception.message ?: "알 수 없는 오류가 발생했습니다"
                        )
                    }
                }
        }
    }
    
    private fun retry() {
        _uiState.update { 
            it.copy(
                albums = emptyList(),
                paginationState = PaginationState()
            ) 
        }
        loadAlbums()
    }
    
    private fun navigateToAlbum(albumId: String) {
        viewModelScope.launch {
            _sideEffect.send(HomeSideEffect.NavigateToAlbumDetail(albumId))
        }
    }
}
