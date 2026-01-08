package com.dongchyeon.core.media.command

import android.os.Bundle
import androidx.core.os.bundleOf

sealed class MusicCommand(val action: String) {

    data class PreloadAdjacentTracks(
        val currentIndex: Int,
    ) : MusicCommand(ACTION_PRELOAD_ADJACENT) {
        fun toBundle(): Bundle = bundleOf(
            KEY_CURRENT_INDEX to currentIndex,
        )

        companion object {
            fun fromBundle(bundle: Bundle): PreloadAdjacentTracks {
                return PreloadAdjacentTracks(
                    currentIndex = bundle.getInt(KEY_CURRENT_INDEX, -1),
                )
            }
        }
    }

    data class PlayPreloaded(
        val mediaId: String,
    ) : MusicCommand(ACTION_PLAY_PRELOADED) {
        fun toBundle(): Bundle = bundleOf(
            KEY_MEDIA_ID to mediaId,
        )

        companion object {
            fun fromBundle(bundle: Bundle): PlayPreloaded {
                return PlayPreloaded(
                    mediaId = bundle.getString(KEY_MEDIA_ID, ""),
                )
            }
        }
    }

    data object ResetPreload : MusicCommand(ACTION_RESET_PRELOAD)

    data object GetPreloadStatus : MusicCommand(ACTION_GET_PRELOAD_STATUS)

    companion object {
        // Action 상수
        const val ACTION_PRELOAD_ADJACENT = "com.dongchyeon.media.PRELOAD_ADJACENT"
        const val ACTION_PLAY_PRELOADED = "com.dongchyeon.media.PLAY_PRELOADED"
        const val ACTION_RESET_PRELOAD = "com.dongchyeon.media.RESET_PRELOAD"
        const val ACTION_GET_PRELOAD_STATUS = "com.dongchyeon.media.GET_PRELOAD_STATUS"

        // Bundle 키 상수
        private const val KEY_CURRENT_INDEX = "current_index"
        private const val KEY_MEDIA_ID = "media_id"

        fun fromAction(action: String, args: Bundle): MusicCommand? {
            return when (action) {
                ACTION_PRELOAD_ADJACENT -> PreloadAdjacentTracks.fromBundle(args)
                ACTION_PLAY_PRELOADED -> PlayPreloaded.fromBundle(args)
                ACTION_RESET_PRELOAD -> ResetPreload
                ACTION_GET_PRELOAD_STATUS -> GetPreloadStatus
                else -> null
            }
        }
    }
}
