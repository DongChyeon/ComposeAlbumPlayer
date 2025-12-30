package com.dongchyeon.feature.album

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.dongchyeon.core.media.controller.MediaControllerManager
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Album과 Player 화면에서 공유하는 통합 ViewModel (MVI 패턴)
 * - 앨범 정보 로드 및 관리
 * - 트랙 리스트 관리
 * - 재생 제어 및 상태 관리
 */
@HiltViewModel
class AlbumPlayerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val albumRepository: AlbumRepository,
    private val mediaControllerManager: MediaControllerManager,
) : BaseViewModel<AlbumPlayerUiState, AlbumPlayerIntent, AlbumPlayerSideEffect>(
    initialState = AlbumPlayerUiState(isLoading = true),
) {

    private val albumId: String = savedStateHandle["albumId"] ?: ""

    init {
        loadAlbumData()
        observeMediaController()
    }

    override fun handleIntent(intent: AlbumPlayerIntent) {
        when (intent) {
            // 앨범 관련
            is AlbumPlayerIntent.Retry -> loadAlbumData()

            // 재생 제어
            is AlbumPlayerIntent.PlayTrack -> playTrack(intent.track)
            is AlbumPlayerIntent.TogglePlayPause -> togglePlayPause()
            is AlbumPlayerIntent.SeekTo -> seekTo(intent.position)
            is AlbumPlayerIntent.SkipToNext -> skipToNext()
            is AlbumPlayerIntent.SkipToPrevious -> skipToPrevious()

            // 네비게이션
            is AlbumPlayerIntent.NavigateBack -> sendSideEffect(AlbumPlayerSideEffect.NavigateBack)
            is AlbumPlayerIntent.NavigateToPlayer -> {
                playTrack(intent.track)
                sendSideEffect(AlbumPlayerSideEffect.NavigateToPlayer(intent.track))
            }
        }
    }

    /**
     * 앨범 정보와 트랙 리스트 로드
     */
    private fun loadAlbumData() {
        launchInScope {
            updateState { it.copy(isLoading = true, error = null) }

            try {
                // 앨범 정보와 트랙 목록을 병렬로 로드
                val albumDeferred = async { albumRepository.getAlbumById(albumId) }
                val tracksDeferred = async { albumRepository.getTracksByAlbumId(albumId) }

                val albumResult = albumDeferred.await()
                val tracksResult = tracksDeferred.await()

                if (albumResult.isSuccess && tracksResult.isSuccess) {
                    val albumData = albumResult.getOrNull()!!
                    val tracksList = tracksResult.getOrNull()!!

                    updateState {
                        it.copy(
                            album = albumData.copy(tracks = tracksList),
                            isLoading = false,
                            error = null,
                        )
                    }
                } else {
                    val error = albumResult.exceptionOrNull() ?: tracksResult.exceptionOrNull()
                    updateState {
                        it.copy(
                            isLoading = false,
                            error = error?.message ?: "알 수 없는 오류가 발생했습니다",
                        )
                    }
                }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "알 수 없는 오류가 발생했습니다",
                    )
                }
            }
        }
    }

    /**
     * MediaController 상태 관찰
     */
    private fun observeMediaController() {
        // PlaybackState 관찰
        mediaControllerManager.playbackState
            .onEach { state ->
                updateState {
                    it.copy(
                        playbackState = state,
                        isPlaying = state == PlaybackState.Playing,
                    )
                }
            }
            .launchIn(viewModelScope)

        // CurrentTrack 관찰
        mediaControllerManager.currentTrack
            .onEach { track ->
                updateState { it.copy(currentTrack = track) }
            }
            .launchIn(viewModelScope)

        // CurrentPosition 관찰
        mediaControllerManager.currentPosition
            .onEach { position ->
                updateState { it.copy(currentPosition = position) }
            }
            .launchIn(viewModelScope)

        // Duration 관찰
        mediaControllerManager.duration
            .onEach { duration ->
                updateState { it.copy(duration = duration) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 트랙 재생
     */
    private fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracksList = currentState.album?.tracks ?: emptyList()
            if (tracksList.isNotEmpty()) {
                // 플레이리스트 설정
                mediaControllerManager.setPlaylist(tracksList)
                // 선택한 트랙부터 재생
                mediaControllerManager.play(track)
            }
        }
    }

    /**
     * 재생/일시정지 토글
     */
    private fun togglePlayPause() {
        if (currentState.isPlaying) {
            mediaControllerManager.pause()
        } else {
            mediaControllerManager.resume()
        }
    }

    /**
     * 위치 이동
     */
    private fun seekTo(position: Long) {
        mediaControllerManager.seekTo(position)
    }

    /**
     * 다음 곡
     */
    private fun skipToNext() {
        viewModelScope.launch {
            mediaControllerManager.skipToNext()
        }
    }

    /**
     * 이전 곡
     */
    private fun skipToPrevious() {
        viewModelScope.launch {
            mediaControllerManager.skipToPrevious()
        }
    }
}
