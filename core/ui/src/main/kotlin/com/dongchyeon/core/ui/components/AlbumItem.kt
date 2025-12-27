package com.dongchyeon.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.dongchyeon.core.designsystem.theme.AlbumPlayerTheme
import com.dongchyeon.core.designsystem.theme.Spacing

/**
 * 앨범을 표시하는 공통 컴포넌트
 *
 * @param title 앨범 제목
 * @param artist 아티스트 이름
 * @param artworkUrl 앨범 아트워크 이미지 URL
 * @param onClick 앨범 클릭 시 실행될 콜백
 * @param modifier 컴포넌트의 크기 및 스타일을 지정하는 Modifier (기본값: Modifier)
 * @param isSelected 앨범 선택 여부 (기본값: false)
 */
@Composable
fun AlbumItem(
    title: String,
    artist: String,
    artworkUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val fallbackContent = remember(title, artist, isSelected) {
        @Composable {
            Column(
                modifier = Modifier.padding(Spacing.medium)
            ) {
                Text(
                    text = title,
                    style = AlbumPlayerTheme.typography.titleLarge,
                    color = if (isSelected)
                        AlbumPlayerTheme.colorScheme.gray50
                    else
                        AlbumPlayerTheme.colorScheme.gray200
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = artist,
                    style = AlbumPlayerTheme.typography.bodyMedium,
                    color = if (isSelected)
                        AlbumPlayerTheme.colorScheme.gray100
                    else
                        AlbumPlayerTheme.colorScheme.gray400
                )
            }
        }
    }
    
    Box(
        modifier = modifier
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = Color.Gray
                ),
                shape = RoundedCornerShape(4.dp)
            )
            .clip(
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
    ) {
        if (artworkUrl.isNotEmpty()) {
            SubcomposeAsyncImage(
                model = artworkUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                error = { fallbackContent() }
            )
        } else {
            fallbackContent()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.White.copy(alpha = 0.2f)
                )
                .clip(
                    shape = RoundedCornerShape(4.dp)
                )
        )
    }
}
