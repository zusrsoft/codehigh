package com.hrm.codehigh.renderer

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

data class InlineCodeSize(
    /** 宽度（像素） */
    val widthPx: Float,
    /** 高度（像素） */
    val heightPx: Float,
) {
    fun width(density: Density): Dp = with(density) { widthPx.toDp() }
    fun height(density: Density): Dp = with(density) { heightPx.toDp() }
}

fun measureInlineCodeSize(
    text: String,
    style: InlineCodeStyle,
    density: Density,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    /** 最大可用宽度（像素） */
    maxWidthPx: Float = Float.POSITIVE_INFINITY,
): InlineCodeSize {
    val annotatedString = AnnotatedString(text)

    return measureAnnotatedStringSize(
        annotatedString = annotatedString,
        textStyle = style.textStyle,
        density = density,
        maxWidthPx = maxWidthPx,
        contentPadding = style.contentPadding,
        borderWidth = style.borderWidth,
        textMeasurer = textMeasurer,
    )
}

internal fun measureAnnotatedStringSize(
    annotatedString: AnnotatedString,
    textStyle: TextStyle,
    density: Density,
    maxWidthPx: Float,
    contentPadding: PaddingValues,
    borderWidth: Dp = 0.dp,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
): InlineCodeSize {
    with(density) {
        val horizontalPaddingPx =
            contentPadding.calculateLeftPadding(LayoutDirection.Ltr).toPx() +
                    contentPadding.calculateRightPadding(LayoutDirection.Ltr).toPx()
        val verticalPaddingPx =
            contentPadding.calculateTopPadding().toPx() +
                    contentPadding.calculateBottomPadding().toPx()
        val horizontalBorderPx = borderWidth.toPx() * 2f
        val verticalBorderPx = borderWidth.toPx() * 2f
        val horizontalDecorationPx = horizontalPaddingPx + horizontalBorderPx
        val verticalDecorationPx = verticalPaddingPx + verticalBorderPx

        val maxWidthWithoutDecoration = maxWidthPx - horizontalDecorationPx

        val constraints =
            if (maxWidthWithoutDecoration.isFinite() && maxWidthWithoutDecoration > 0) {
                androidx.compose.ui.unit.Constraints(
                    maxWidth = maxWidthWithoutDecoration.toInt(),
                )
            } else {
                androidx.compose.ui.unit.Constraints()
            }

        val layoutResult = textMeasurer.measure(
            text = annotatedString,
            style = textStyle,
            overflow = TextOverflow.Clip,
            softWrap = false,
            constraints = constraints,
        )

        return InlineCodeSize(
            widthPx = layoutResult.size.width + horizontalDecorationPx,
            heightPx = layoutResult.size.height + verticalDecorationPx,
        )
    }
}
