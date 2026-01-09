package com.dongchyeon.feature.album

import androidx.lifecycle.viewModelScope
import com.dongchyeon.core.ui.base.BaseViewModel
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import com.dongchyeon.domain.model.ShuffleMode
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.player.MusicPlayer
import com.dongchyeon.domain.repository.AlbumRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = AlbumPlayerViewModel.Factory::class)
class AlbumPlayerViewModel @AssistedInject constructor(
    @Assisted private val albumId: String,
    private val albumRepository: AlbumRepository,
    private val musicPlayer: MusicPlayer,
) : BaseViewModel<AlbumPlayerUiState, AlbumPlayerIntent, AlbumPlayerSideEffect>(
    initialState = AlbumPlayerUiState(isLoading = true),
) {

    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AlbumPlayerViewModel
    }

    // 실시간으로 변경되는 값들은 별도 Flow로 노출
    val currentPositionSeconds: StateFlow<Int> = musicPlayer.currentPosition
        .map { (it / 1000).toInt() }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

    val durationSeconds: StateFlow<Int> = musicPlayer.duration
        .map { (it / 1000).toInt() }
        .stateIn(viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0)

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

            // 재생 모드
            is AlbumPlayerIntent.ToggleRepeatMode -> toggleRepeatMode()
            is AlbumPlayerIntent.ToggleShuffle -> toggleShuffle()

            // 네비게이션
            is AlbumPlayerIntent.NavigateBack -> sendSideEffect(AlbumPlayerSideEffect.NavigateBack)
            is AlbumPlayerIntent.NavigateToPlayer -> {
                playTrack(intent.track)
                sendSideEffect(AlbumPlayerSideEffect.NavigateToPlayer(intent.track))
            }
        }
    }

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

                    // 재생 불가능한 트랙 필터링 (streamUrl 없거나 isStreamable = false)
                    val playableTracks = tracksList.filter {
                        it.isStreamable && it.streamUrl.isNotBlank()
                    }

                    // 재생 가능한 트랙이 없으면 메시지와 함께 뒤로 이동
                    if (playableTracks.isEmpty()) {
                        updateState { it.copy(isLoading = false) }
                        sendSideEffect(
                            AlbumPlayerSideEffect.ShowErrorAndNavigateBack(
                                "No playable tracks available in this album",
                            ),
                        )
                        return@launchInScope
                    }

                    updateState {
                        it.copy(
                            album = albumData.copy(tracks = playableTracks),
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

    private fun observeMediaController() {
        // PlaybackState 관찰
        musicPlayer.playbackState
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
        musicPlayer.currentTrack
            .onEach { track ->
                updateState { it.copy(currentTrack = track) }
            }
            .launchIn(viewModelScope)

        // RepeatMode 관찰
        musicPlayer.repeatMode
            .onEach { mode ->
                updateState { it.copy(repeatMode = mode) }
            }
            .launchIn(viewModelScope)

        // ShuffleMode 관찰
        musicPlayer.shuffleMode
            .onEach { mode ->
                updateState { it.copy(shuffleMode = mode) }
            }
            .launchIn(viewModelScope)

        // PlayerError 관찰
        musicPlayer.playerError
            .onEach { error ->
                sendSideEffect(AlbumPlayerSideEffect.ShowPlaybackError(error.message))
            }
            .launchIn(viewModelScope)
    }

    private fun playTrack(track: Track) {
        viewModelScope.launch {
            val tracksList = currentState.album?.tracks ?: emptyList()
            if (tracksList.isNotEmpty()) {
                // 플레이리스트 설정
                musicPlayer.setPlaylist(tracksList)
                // 선택한 트랙부터 재생
                musicPlayer.play(track)
            }
        }
    }

    private fun togglePlayPause() {
        if (currentState.isPlaying) {
            musicPlayer.pause()
        } else {
            musicPlayer.resume()
        }
    }

    private fun seekTo(position: Long) {
        musicPlayer.seekTo(position)
    }

    private fun skipToNext() {
        viewModelScope.launch {
            musicPlayer.skipToNext()
        }
    }

    private fun skipToPrevious() {
        viewModelScope.launch {
            musicPlayer.skipToPrevious()
        }
    }

    private fun toggleRepeatMode() {
        val nextMode = when (currentState.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        musicPlayer.setRepeatMode(nextMode)
    }

    private fun toggleShuffle() {
        val isCurrentlyOn = currentState.shuffleMode == ShuffleMode.ON
        musicPlayer.setShuffleMode(!isCurrentlyOn)
    }
}
