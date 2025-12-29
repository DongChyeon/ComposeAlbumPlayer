package com.dongchyeon.core.media.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.RepeatMode
import kotlinx.coroutines.flow.Flow

/**
 * ExoPlayer를 관리하는 인터페이스
 * 테스트에서 Fake 구현으로 교체 가능
 */
interface ExoPlayerManager {
    /**
     * 플레이어의 재생 상태
     * true: 재생 중, false: 일시정지 또는 정지
     */
    val isPlaying: Flow<Boolean>

    /**
     * 플레이어의 전체 상태 (Idle, Playing, Paused)
     */
    val playbackState: Flow<PlaybackState>

    /**
     * 현재 재생 위치 (밀리초)
     */
    val currentPosition: Flow<Long>

    /**
     * 현재 트랙의 총 길이 (밀리초)
     */
    val duration: Flow<Long>

    /**
     * 현재 재생 중인 미디어 아이템의 인덱스
     */
    val currentMediaItemIndex: Flow<Int>

    /**
     * URL로 미디어를 준비하고 재생
     */
    fun prepareAndPlay(url: String)

    /**
     * 재생
     */
    fun play()

    /**
     * 일시정지
     */
    fun pause()

    /**
     * 정지 및 리소스 해제
     */
    fun stop()

    /**
     * 특정 위치로 이동
     */
    fun seekTo(positionMs: Long)

    /**
     * 플레이리스트 설정
     */
    fun setPlaylist(urls: List<String>)

    /**
     * 특정 인덱스의 트랙으로 이동
     */
    fun seekToMediaItem(index: Int)

    /**
     * 다음 트랙으로 이동
     */
    fun seekToNext()

    /**
     * 이전 트랙으로 이동
     */
    fun seekToPrevious()

    /**
     * 현재 위치 가져오기 (동기)
     */
    fun getCurrentPositionSync(): Long

    /**
     * 반복 모드 설정
     */
    fun setRepeatMode(mode: RepeatMode)

    /**
     * 리소스 해제
     */
    fun release()
}
