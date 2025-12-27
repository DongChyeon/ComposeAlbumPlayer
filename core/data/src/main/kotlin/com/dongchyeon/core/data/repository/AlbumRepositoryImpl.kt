package com.dongchyeon.core.data.repository

import com.dongchyeon.core.data.mapper.toDomain
import com.dongchyeon.core.network.api.AlbumApiService
import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.Track
import com.dongchyeon.domain.repository.AlbumRepository
import javax.inject.Inject

class AlbumRepositoryImpl @Inject constructor(
    private val albumApiService: AlbumApiService
) : AlbumRepository {
    
    override suspend fun getAlbums(): Result<List<Album>> {
        return try {
            val response = albumApiService.getTrendingPlaylists(
                type = "album",
                omitTracks = true,
                limit = 10
            )
            
            val albums = response.data?.toDomain() ?: emptyList()
            Result.success(albums)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getAlbumById(id: String): Result<Album> {
        return try {
            val response = albumApiService.getPlaylist(playlistId = id)
            
            val album = response.data?.toDomain()
            if (album != null) {
                Result.success(album)
            } else {
                Result.failure(Exception("Album not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTracksByAlbumId(albumId: String): Result<List<Track>> {
        return try {
            val response = albumApiService.getPlaylist(playlistId = albumId)
            
            val album = response.data?.toDomain()
            if (album != null) {
                Result.success(album.tracks)
            } else {
                Result.failure(Exception("Album not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
