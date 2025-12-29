package com.dongchyeon.core.media.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ExoPlayer를 관리하는 실제 구현
 * 생명주기, 상태, 리스너를 관리
 */
class AndroidExoPlayerManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ExoPlayerManager {
        private val player: ExoPlayer = ExoPlayer.Builder(context).build()
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        private val _isPlaying = MutableStateFlow(false)
        override val isPlaying: Flow<Boolean> = _isPlaying.asStateFlow()

        private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
        override val playbackState: Flow<PlaybackState> = _playbackState.asStateFlow()

        private val _currentPosition = MutableStateFlow(0L)
        override val currentPosition: Flow<Long> = _currentPosition.asStateFlow()

        private val _duration = MutableStateFlow(0L)
        override val duration: Flow<Long> = _duration.asStateFlow()

        private val _currentMediaItemIndex = MutableStateFlow(-1)
        override val currentMediaItemIndex: Flow<Int> = _currentMediaItemIndex.asStateFlow()

        private var currentExoPlayerState = Player.STATE_IDLE

        init {
            setupPlayerListener()
            startPositionUpdater()
        }

        private fun setupPlayerListener() {
            player.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        updatePlaybackState()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        currentExoPlayerState = playbackState
                        when (playbackState) {
                            Player.STATE_READY -> {
                                _duration.value = player.duration
                            }
                        }
                        updatePlaybackState()
                    }

                    override fun onMediaItemTransition(
                        mediaItem: MediaItem?,
                        reason: Int,
                    ) {
                        _currentMediaItemIndex.value = player.currentMediaItemIndex
                    }
                },
            )
        }

        private fun updatePlaybackState() {
            _playbackState.value =
                when {
                    currentExoPlayerState == Player.STATE_IDLE -> PlaybackState.Idle
                    currentExoPlayerState == Player.STATE_ENDED -> PlaybackState.Idle
                    _isPlaying.value -> PlaybackState.Playing
                    else -> PlaybackState.Paused
                }
        }

        private fun startPositionUpdater() {
            scope.launch {
                while (isActive) {
                    if (_isPlaying.value) {
                        _currentPosition.value = player.currentPosition
                    }
                    delay(100) // 100ms마다 업데이트
                }
            }
        }

        override fun prepareAndPlay(url: String) {
            val mediaItem = MediaItem.fromUri(url)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
        }

        override fun play() {
            player.play()
        }

        override fun pause() {
            player.pause()
        }

        override fun stop() {
            player.stop()
            player.clearMediaItems()
        }

        override fun seekTo(positionMs: Long) {
            player.seekTo(positionMs)
            _currentPosition.value = positionMs
        }

        override fun setPlaylist(urls: List<String>) {
            val mediaItems = urls.map { MediaItem.fromUri(it) }
            player.setMediaItems(mediaItems)
            player.prepare()
        }

        override fun seekToMediaItem(index: Int) {
            if (index in 0 until player.mediaItemCount) {
                player.seekTo(index, 0)
                _currentMediaItemIndex.value = index
            }
        }

        override fun seekToNext() {
            if (player.hasNextMediaItem()) {
                player.seekToNext()
            }
        }

        override fun seekToPrevious() {
            if (player.hasPreviousMediaItem()) {
                player.seekToPrevious()
            }
        }

        override fun getCurrentPositionSync(): Long {
            return player.currentPosition
        }

        override fun setRepeatMode(mode: RepeatMode) {
            player.repeatMode =
                when (mode) {
                    RepeatMode.NONE -> Player.REPEAT_MODE_OFF
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                }
        }

        override fun release() {
            player.release()
        }
    }
