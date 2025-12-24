package com.dongchyeon.domain.repository

import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.Track
import kotlinx.coroutines.flow.Flow

interface AlbumRepository {
    suspend fun getAlbums(): Result<List<Album>>
    suspend fun getAlbumById(id: String): Result<Album>
    suspend fun getTracksByAlbumId(albumId: String): Result<List<Track>>
}
