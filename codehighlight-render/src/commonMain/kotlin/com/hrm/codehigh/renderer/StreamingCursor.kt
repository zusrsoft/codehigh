package com.hrm.codehigh.renderer

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 流式光标动画组件，仅在 isStreaming = true 时显示。
 * 标记为 internal，仅供 CodeBlock 内部使用。
 *
 * @param isStreaming 是否处于流式输出状态
 * @param color 光标颜色
 * @param cursorHeight 光标高度，默认跟随单行代码高度
 */
@Composable
internal fun StreamingCursor(
    isStreaming: Boolean,
    color: Color = Color.White,
    modifier: Modifier = Modifier,
    cursorHeight: Dp = 16.dp,
) {
    if (!isStreaming) return

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Box(
        modifier = modifier
            .width(2.dp)
            .height(cursorHeight)
            .graphicsLayer { this.alpha = alpha } // draw 阶段读值，避免每帧重组
            .background(color)
    )
}
