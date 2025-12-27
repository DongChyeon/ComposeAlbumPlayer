package com.dongchyeon.domain.repository

import com.dongchyeon.domain.model.Album
import com.dongchyeon.domain.model.Track

interface AlbumRepository {
    suspend fun getAlbums(page: Int = 0, limit: Int = 10): Result<List<Album>>
    suspend fun getAlbumById(albumId: String): Result<Album>
    suspend fun getTracksByAlbumId(albumId: String): Result<List<Track>>
}
