@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dongchyeon.feature.home.component

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 반원 형태로 회전하는 Rotary Wheel Picker
 *
 * @param items 표시할 아이템 리스트
 * @param itemContent 각 아이템을 렌더링하는 컴포저블
 * @param onSelectedIndexChange 선택된 인덱스가 변경될 때 호출되는 콜백
 * @param modifier Modifier
 * @param itemSpacing 아이템 간 간격
 * @param maxRotateDeg 최대 회전 각도 (도)
 * @param maxTranslateXPx 최대 수평 이동 거리 (px)
 * @param horizontalPadding 좌우 패딩
 */
@Composable
fun <T> RotaryWheelPicker(
    items: List<T>,
    itemContent: @Composable (item: T, isSelected: Boolean, modifier: Modifier) -> Unit,
    modifier: Modifier = Modifier,
    onSelectedIndexChange: (Int) -> Unit = {},
    itemSpacing: Dp = 8.dp,
    maxRotateDeg: Float = 90f,
    maxTranslateXPx: Float = 280f,
    horizontalPadding: PaddingValues = PaddingValues(start = 72.dp, end = 24.dp)
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    
    // 첫 번째 아이템의 높이를 측정
    var measuredItemHeight by remember { mutableStateOf(0.dp) }

    // 화면 중앙에 가장 가까운 아이템 = 선택
    val selectedIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            if (layout.visibleItemsInfo.isEmpty()) return@derivedStateOf 0

            val viewportCenter =
                (layout.viewportStartOffset + layout.viewportEndOffset) / 2f

            layout.visibleItemsInfo.minBy { info ->
                val itemCenter = info.offset + info.size / 2f
                abs(itemCenter - viewportCenter)
            }.index
        }
    }
    
    // 선택된 인덱스가 변경되면 콜백 호출
    androidx.compose.runtime.LaunchedEffect(selectedIndex) {
        onSelectedIndexChange(selectedIndex)
    }

    BoxWithConstraints(modifier) {
        val padding = if (measuredItemHeight > 0.dp) {
            ((maxHeight - measuredItemHeight) / 2).coerceAtLeast(0.dp)
        } else {
            maxHeight / 2
        }

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = padding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(
                items = items,
                key = { index, _ -> index }
            ) { index, item ->
                // 현재 아이템의 "중앙에서의 거리" 기반으로 변형 값 계산
                val transform = rememberItemTransform(listState, index)

                val rotation = (transform.norm * maxRotateDeg).coerceIn(-maxRotateDeg, maxRotateDeg)
                val translateX = -(abs(transform.norm) * maxTranslateXPx)
                val scale = (1f - abs(transform.norm) * 0.10f).coerceIn(0.90f, 1f)
                val alpha = (1f - abs(transform.norm) * 0.45f).coerceIn(0.40f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            rotationZ = rotation
                            translationX = translateX
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha

                            // 왼쪽을 중심으로 회전
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .padding(horizontalPadding),
                    contentAlignment = Alignment.CenterStart
                ) {
                    itemContent(
                        item,
                        index == selectedIndex,
                        Modifier.onSizeChanged { size ->
                            // 첫 번째 아이템의 높이를 한 번만 측정
                            if (index == 0 && measuredItemHeight == 0.dp) {
                                measuredItemHeight = with(density) { size.height.toDp() }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Stable
private data class ItemTransform(
    val norm: Float // -1..1 근처 (중앙=0, 위/아래로 갈수록 절대값 증가)
)

/**
 * index 아이템이 viewport 중심에서 얼마나 떨어졌는지 정규화해서 돌려줌.
 */
@Composable
private fun rememberItemTransform(state: LazyListState, index: Int): ItemTransform {
    return remember(state, index) {
        derivedStateOf {
            val layout = state.layoutInfo
            val info = layout.visibleItemsInfo.firstOrNull { it.index == index }
                ?: return@derivedStateOf ItemTransform(norm = 2f)

            val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
            val itemCenter = info.offset + info.size / 2f

            // 화면 높이 기준으로 정규화 (-1..1 근처)
            val half = (layout.viewportEndOffset - layout.viewportStartOffset) / 2f
            val norm = ((itemCenter - viewportCenter) / half).coerceIn(-2f, 2f)

            ItemTransform(norm = norm)
        }
    }.value
}
