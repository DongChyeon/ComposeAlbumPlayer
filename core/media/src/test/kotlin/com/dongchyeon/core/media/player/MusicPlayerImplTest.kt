package com.dongchyeon.core.media.player

import com.dongchyeon.domain.model.PlaybackState
import com.dongchyeon.domain.model.Track
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MusicPlayerImplTest {
    private lateinit var fakeExoPlayer: FakeExoPlayerManager
    private lateinit var musicPlayer: MusicPlayerImpl

    @Before
    fun setup() {
        fakeExoPlayer = FakeExoPlayerManager()
        musicPlayer = MusicPlayerImpl(fakeExoPlayer)
    }

    @Test
    fun `트랙을 재생하면 재생 상태가 Playing이 된다`() = runTest {
        // Given
        val track = createTrack(id = "track1")
        fakeExoPlayer.setDuration(180000L)

        // When
        musicPlayer.play(track)

        // Then
        val state = musicPlayer.playbackState.first()
        assertEquals(PlaybackState.Playing, state)

        val currentTrack = musicPlayer.currentTrack.first()
        assertEquals(track.id, currentTrack?.id)
    }

    @Test
    fun `재생 중 일시정지하면 상태가 Paused가 된다`() = runTest {
        // Given
        val track = createTrack(id = "track1")
        fakeExoPlayer.setDuration(180000L)
        musicPlayer.play(track)

        // When
        musicPlayer.pause()

        // Then
        val state = musicPlayer.playbackState.first()
        assertEquals(PlaybackState.Paused, state)
    }

    @Test
    fun `일시정지 중 다시 재생하면 상태가 Playing이 된다`() = runTest {
        // Given
        val track = createTrack(id = "track1")
        fakeExoPlayer.setDuration(180000L)
        musicPlayer.play(track)
        musicPlayer.pause()

        // When
        musicPlayer.resume()

        // Then
        val state = musicPlayer.playbackState.first()
        assertEquals(PlaybackState.Playing, state)
    }

    @Test
    fun `seekTo를 호출하면 재생 위치가 변경된다`() = runTest {
        // Given
        val track = createTrack(id = "track1")
        fakeExoPlayer.setDuration(180000L)
        musicPlayer.play(track)
        val seekPosition = 60000L // 1분

        // When
        musicPlayer.seekTo(seekPosition)

        // Then
        val position = musicPlayer.currentPosition.first()
        assertEquals(seekPosition, position)
    }

    @Test
    fun `플레이리스트 설정 후 다음 트랙으로 스킵할 수 있다`() = runTest {
        // Given
        val track1 = createTrack(id = "track1", streamUrl = "url1")
        val track2 = createTrack(id = "track2", streamUrl = "url2")
        val playlist = listOf(track1, track2)

        fakeExoPlayer.setDuration(180000L)
        musicPlayer.setPlaylist(playlist)
        musicPlayer.play(track1)

        // When
        musicPlayer.skipToNext()

        // Then
        val currentTrack = musicPlayer.currentTrack.first()
        assertEquals(track2.id, currentTrack?.id)
    }

    @Test
    fun `stop을 호출하면 재생이 멈추고 상태가 초기화된다`() = runTest {
        // Given
        val track = createTrack(id = "track1")
        fakeExoPlayer.setDuration(180000L)
        musicPlayer.play(track)

        // When
        musicPlayer.stop()

        // Then
        val state = musicPlayer.playbackState.first()
        assertEquals(PlaybackState.Idle, state)

        val currentTrack = musicPlayer.currentTrack.first()
        assertEquals(null, currentTrack)
    }

    @Test
    fun `재생 시점이 5초 이하일 때 skipToPrevious 호출 시 이전 곡으로 이동한다`() = runTest {
        // Given
        val track1 = createTrack(id = "track1", streamUrl = "url1")
        val track2 = createTrack(id = "track2", streamUrl = "url2")
        val playlist = listOf(track1, track2)

        fakeExoPlayer.setDuration(180000L)
        musicPlayer.setPlaylist(playlist)
        musicPlayer.play(track2)

        // 재생 시점을 3초로 설정 (5초 이하)
        musicPlayer.seekTo(3000L)

        // When
        musicPlayer.skipToPrevious()

        // Then - 이전 트랙으로 이동
        val currentTrack = musicPlayer.currentTrack.first()
        assertEquals(track1.id, currentTrack?.id)
    }

    @Test
    fun `재생 시점이 5초 초과일 때 skipToPrevious 호출 시 0초로 이동한다`() = runTest {
        // Given
        val track1 = createTrack(id = "track1", streamUrl = "url1")
        val track2 = createTrack(id = "track2", streamUrl = "url2")
        val playlist = listOf(track1, track2)

        fakeExoPlayer.setDuration(180000L)
        musicPlayer.setPlaylist(playlist)
        musicPlayer.play(track2)

        // 재생 시점을 10초로 설정 (5초 초과)
        musicPlayer.seekTo(10000L)

        // When
        musicPlayer.skipToPrevious()

        // Then - 현재 트랙 유지, 위치만 0초로 이동
        val currentTrack = musicPlayer.currentTrack.first()
        assertEquals(track2.id, currentTrack?.id)

        val position = musicPlayer.currentPosition.first()
        assertEquals(0L, position)
    }

    @Test
    fun `재생 시점이 정확히 5초일 때 skipToPrevious 호출 시 이전 곡으로 이동한다`() = runTest {
        // Given
        val track1 = createTrack(id = "track1", streamUrl = "url1")
        val track2 = createTrack(id = "track2", streamUrl = "url2")
        val playlist = listOf(track1, track2)

        fakeExoPlayer.setDuration(180000L)
        musicPlayer.setPlaylist(playlist)
        musicPlayer.play(track2)

        // 재생 시점을 정확히 5초로 설정 (경계값)
        musicPlayer.seekTo(5000L)

        // When
        musicPlayer.skipToPrevious()

        // Then - 5초 이하이므로 이전 트랙으로 이동
        val currentTrack = musicPlayer.currentTrack.first()
        assertEquals(track1.id, currentTrack?.id)
    }

    @Test
    fun `첫 번째 트랙에서 재생 시점이 5초 이하일 때 skipToPrevious 호출 시 0초로 이동한다`() = runTest {
        // Given - 첫 번째 트랙 (이전 트랙 없음)
        val track1 = createTrack(id = "track1", streamUrl = "url1")
        val track2 = createTrack(id = "track2", streamUrl = "url2")
        val playlist = listOf(track1, track2)

        fakeExoPlayer.setDuration(180000L)
        musicPlayer.setPlaylist(playlist)
        musicPlayer.play(track1) // 첫 번째 트랙 재생

        // 재생 시점을 3초로 설정
        musicPlayer.seekTo(3000L)

        // When
        musicPlayer.skipToPrevious()

        // Then - 이전 트랙이 없으므로 0초로 이동
        val currentTrack = musicPlayer.currentTrack.first()
        assertEquals(track1.id, currentTrack?.id)

        val position = musicPlayer.currentPosition.first()
        assertEquals(0L, position)
    }

    private fun createTrack(
        id: String = "track1",
        title: String = "Track Title",
        artist: String = "Artist Name",
        streamUrl: String = "https://example.com/song.mp3",
        artworkUrl: String = "https://example.com/artwork.png",
        duration: Long = 180000,
        albumId: String = "album1",
    ) = Track(
        id = id,
        title = title,
        artist = artist,
        streamUrl = streamUrl,
        artworkUrl = artworkUrl,
        duration = duration,
        albumId = albumId,
    )
}
