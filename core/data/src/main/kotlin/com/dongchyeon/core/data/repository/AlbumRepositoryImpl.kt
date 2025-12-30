package com.dongchyeon.core.data.repository

import com.dongchyeon.core.data.mapper.toDomain
import com.dongchyeon.core.network.api.AlbumApiService
import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.repository.AlbumRepository
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val albumApiService: AlbumApiService,
) : AlbumRepository {
    override suspend fun getAlbums(
        page: Int,
        limit: Int
    ): Result<List<Album>> {
        return try {
            val offset = page * limit
            val response =
                albumApiService.getTrendingPlaylists(
                    type = "album",
                    omitTracks = true,
                    limit = limit,
                    offset = offset,
                )

            val albums = response.data?.toDomain() ?: emptyList()
            Result.success(albums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAlbumById(albumId: String): Result<Album> {
        return try {
            val response = albumApiService.getPlaylist(playlistId = albumId)
            val album =
                response.data?.firstOrNull()?.toDomain()
                    ?: return Result.failure(Exception("Album not found"))
            Result.success(album)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTracksByAlbumId(albumId: String): Result<List<Track>> {
        return try {
            val response = albumApiService.getPlaylistTracks(playlistId = albumId)
            val tracks = response.data.toDomain(albumId = albumId)
            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTrackById(trackId: String): Result<Track> {
        return try {
            val response = albumApiService.getTrack(trackId = trackId)
            val track = response.data.toDomain()
            Result.success(track)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
