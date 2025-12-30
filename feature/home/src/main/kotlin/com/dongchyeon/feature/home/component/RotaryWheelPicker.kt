@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.dongchyeon.feature.home.component

import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 반원 형태로 회전하는 Rotary Wheel Picker
 *
 * @param items 표시할 아이템 리스트
 * @param itemContent 각 아이템을 렌더링하는 컴포저블
 * @param onSelectedIndexChange 선택된 인덱스가 변경될 때 호출되는 콜백
 * @param modifier Modifier
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
    onLoadMore: () -> Unit = {},
    onScrollStarted: () -> Unit = {},
    itemSpacing: Dp = (-80).dp,
    curvatureFactor: Float = 1f,
    yArcBlend: Float = 1f,
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
    LaunchedEffect(selectedIndex) {
        onSelectedIndexChange(selectedIndex)
    }

    // 스크롤 시작 감지
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    onScrollStarted()
                }
            }
    }

    // 리스트의 끝에 도달했는지 확인하고 더 많은 데이터 로드
    LaunchedEffect(listState) {
        snapshotFlow {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index
        }.collect { lastVisibleIndex ->
            if (lastVisibleIndex != null && lastVisibleIndex >= items.size - 5) {
                onLoadMore()
            }
        }
    }

    BoxWithConstraints(modifier) {
        val padding =
            if (measuredItemHeight > 0.dp) {
                ((maxHeight - measuredItemHeight) / 2).coerceAtLeast(0.dp)
            } else {
                maxHeight / 2
            }

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = padding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(
                items = items,
                key = { index, _ -> index },
            ) { index, item ->
                val transform =
                    rememberArcTransform(
                        state = listState,
                        index = index,
                        curvatureFactor = curvatureFactor,
                        yArcBlend = yArcBlend,
                    )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .graphicsLayer {
                            rotationZ = transform.rotationDeg
                            translationX = transform.translationXPx
                            translationY = transform.translationYPx
                            alpha = transform.alpha

                            // 왼쪽을 중심으로 회전(원하는 축이면 바꾸면 됨)
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    itemContent(
                        item,
                        index == selectedIndex,
                        Modifier.onSizeChanged { size ->
                            if (index == 0 && measuredItemHeight == 0.dp) {
                                measuredItemHeight = with(density) { size.height.toDp() }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Stable
private data class ArcTransform(
    val rotationDeg: Float,
    val translationXPx: Float,
    val translationYPx: Float,
    val alpha: Float,
)

/**
 * "보이는 viewport 전체"가 항상 원호를 유지하도록 radius를 동적으로 잡는다.
 *
 * - y = itemCenter - viewportCenter
 * - R = viewportHalf + itemHalf  (=> 보이는 가장자리 아이템도 |y| <= R 보장)
 * - θ = asin(y/R)
 * - rotationZ = θ(deg)
 * - x = -R * (1 - cosθ)
 */
@Composable
private fun rememberArcTransform(
    state: LazyListState,
    index: Int,
    curvatureFactor: Float,
    yArcBlend: Float,
): ArcTransform {
    return remember(state, index, curvatureFactor, yArcBlend) {
        derivedStateOf {
            val layout = state.layoutInfo
            val info =
                layout.visibleItemsInfo.firstOrNull { it.index == index }
                    ?: return@derivedStateOf ArcTransform(0f, 0f, 0f, 0.4f)

            val viewportStart = layout.viewportStartOffset.toFloat()
            val viewportEnd = layout.viewportEndOffset.toFloat()
            val viewportCenter = (viewportStart + viewportEnd) / 2f
            val viewportHalf = (viewportEnd - viewportStart) / 2f

            val itemCenter = info.offset + info.size / 2f
            val y = itemCenter - viewportCenter

            // y 범위 기준(화면 안 전체): [-viewportHalf, viewportHalf]
            val yMax = (viewportHalf + info.size / 2f).coerceAtLeast(1f)
            val yNorm = (y / yMax).coerceIn(-1f, 1f)

            // 선형 θ (가장자리로 갈수록 가속되는 asin 제거)
            val maxTheta = (PI / 2).toFloat()
            val theta = yNorm * maxTheta

            // 가로 반지름(원호 휨 정도)
            val xRadius = (yMax * curvatureFactor).coerceAtLeast(1f)

            // 원호 수식
            val rotationDeg = theta * (180f / PI.toFloat())
            val translationX = -xRadius * (1f - cos(theta.toDouble()).toFloat())

            // 세로도 원호 위로 “재배치”해서 간격을 압축
            val yArc = yMax * sin(theta.toDouble()).toFloat()
            val yBlended = lerp(y, yArc, yArcBlend.coerceIn(0f, 1f))
            val translationY = yBlended - y

            val alpha = (1f - abs(yNorm) * 0.45f).coerceIn(0.40f, 1f)

            ArcTransform(
                rotationDeg = rotationDeg,
                translationXPx = translationX,
                translationYPx = translationY,
                alpha = alpha,
            )
        }
    }.value
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
