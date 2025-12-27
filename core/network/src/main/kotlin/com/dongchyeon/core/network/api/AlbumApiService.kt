package com.dongchyeon.core.network.api

import com.dongchyeon.core.network.model.AlbumResponse
import com.dongchyeon.core.network.model.PlaylistResponse
import com.dongchyeon.core.network.model.TrackResponse
import com.dongchyeon.core.network.model.TracksResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Audius API Service
 * API Documentation: https://docs.audius.org/developers/api/
 */
interface AlbumApiService {

    @GET("v1/playlists/trending")
    suspend fun getTrendingPlaylists(
        @Query("type") type: String = "album",
        @Query("omit_tracks") omitTracks: Boolean = true,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): PlaylistResponse

    @GET("v1/playlists/{playlist_id}")
    suspend fun getPlaylist(
        @Path("playlist_id") playlistId: String
    ): AlbumResponse

    @GET("v1/playlists/{playlist_id}/tracks")
    suspend fun getPlaylistTracks(
        @Path("playlist_id") playlistId: String
    ): TracksResponse

    @GET("v1/tracks/{track_id}")
    suspend fun getTrack(
        @Path("track_id") trackId: String
    ): TrackResponse
}
