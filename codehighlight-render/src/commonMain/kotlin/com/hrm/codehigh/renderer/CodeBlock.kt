package com.hrm.codehigh.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.codehigh.ast.CodeAst
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType
import com.hrm.codehigh.i18n.Strings
import com.hrm.codehigh.platform.textClipEntry
import com.hrm.codehigh.stream.IncrementalHighlighter
import com.hrm.codehigh.theme.CodeLineKind
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.codehigh.theme.LocalCodeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 代码块渲染组件，唯一对外渲染入口。
 * 对外公开，支持完整的代码高亮功能。
 *
 * @param code 代码字符串
 * @param language 语言标识符（如 "kotlin"、"python"）
 * @param modifier Compose Modifier
 * @param isStreaming 是否处于流式输出状态，为 true 时在末尾显示光标动画
 * @param theme 代码主题，默认使用 LocalCodeTheme
 * @param showLineNumbers 是否显示行号
 * @param showCopyButton 是否显示复制按钮
 * @param maxVisibleLines 最大可见行数，默认 500 行，超出折叠；null 不限制
 * @param onTokenClick Token 点击回调
 * @param selectable 正文是否可选中复制；SelectionContainer 有一定开销，长列表可关闭
 */
@Composable
fun CodeBlock(
    code: String,
    language: String = "",
    title: String = "",
    modifier: Modifier = Modifier,
    isStreaming: Boolean = false,
    theme: CodeTheme = LocalCodeTheme.current,
    showLineNumbers: Boolean = false,
    startLine: Int = 1,
    highlightedLines: Set<Int> = emptySet(),
    showCopyButton: Boolean = true,
    maxVisibleLines: Int? = 500,
    onTokenClick: ((CodeToken) -> Unit)? = null,
    selectable: Boolean = true
) {
    val highlighter = remember { IncrementalHighlighter() }

    // 解析互斥：LaunchedEffect 重启时新旧协程可能并发进入 updateDetailed（无同步可变状态），串行化保护
    val parseMutex = remember { Mutex() }

    var isExpanded by remember { mutableStateOf(false) }

    // CRLF 归一化（Windows 剪贴板常见）；行拆分记忆化，流式高频重组下避免 O(n) 重分配
    val normalizedCode = remember(code) {
        if (code.contains('\r')) code.replace("\r\n", "\n").replace("\r", "\n") else code
    }
    val lines = remember(normalizedCode) { normalizedCode.split("\n") }
    val totalLines = lines.size
    val isCollapsible = maxVisibleLines != null && totalLines > maxVisibleLines
    val visibleLineCount = when {
        !isCollapsible || isExpanded -> totalLines
        else -> maxVisibleLines // isCollapsible 为 true 时必非空（编译器可智能转换）
    }
    val visibleLines = remember(visibleLineCount, lines) { lines.take(visibleLineCount) }
    val visibleCode = remember(visibleLines) { visibleLines.joinToString("\n") }
    // 各行在 visibleCode 中的起始字符偏移（onTokenClick 定位用）
    val lineCharOffsets = remember(visibleLines) {
        IntArray(visibleLines.size).also { acc ->
            var off = 0
            for (i in visibleLines.indices) {
                acc[i] = off
                off += visibleLines[i].length + 1
            }
        }
    }

    // 初始 null：首帧先渲染纯文本，解析在后台线程完成后替换（大文件不阻塞组合）
    var lineHighlights by remember { mutableStateOf<List<CodeLineRender>?>(null) }
    var visibleAst by remember { mutableStateOf<CodeAst?>(null) }
    // 上次行渲染使用的样式输入（theme + 高亮行）：样式输入变化时即使代码未变也需重建行渲染
    var renderedStyleInputs by remember { mutableStateOf<Pair<CodeTheme, Set<Int>>?>(null) }
    LaunchedEffect(visibleCode, language, theme, highlightedLines) {
        val result = withContext(Dispatchers.Default) {
            parseMutex.withLock {
                val detailed = highlighter.updateDetailed(visibleCode, language)
                val lastInputs = renderedStyleInputs
                val styleChanged = lastInputs == null ||
                    lastInputs.first != theme ||
                    lastInputs.second != highlightedLines
                if (!detailed.hasChange && !styleChanged) {
                    null // 无变化（缓存命中且样式输入未变）：保留现有渲染结果
                } else {
                    detailed.ast to buildLineRenders(
                        sourceLines = visibleLines,
                        tokens = detailed.ast.tokens,
                        theme = theme,
                        language = language,
                        highlightedLines = highlightedLines,
                    )
                }
            }
        }
        if (result != null) {
            visibleAst = result.first
            lineHighlights = result.second
            renderedStyleInputs = theme to highlightedLines
        }
    }
    val plainLines = remember(visibleLines) {
        visibleLines.map { CodeLineRender(AnnotatedString(it), CodeLineKind.NORMAL) }
    }
    val resolvedLines = lineHighlights ?: plainLines
    val fallbackToPlainLines = remember(resolvedLines, visibleLines) {
        visibleLines.isNotEmpty() && (
            resolvedLines.isEmpty() ||
                resolvedLines.size < visibleLines.size ||
                resolvedLines.all { it.text.text.isBlank() }
            )
    }
    val showToolbar = title.isNotBlank() || language.isNotBlank() || showCopyButton
    val density = LocalDensity.current
    val codeLineHeight = 20.sp
    val codeLineHeightDp = with(density) { codeLineHeight.toDp() }
    // 行号宽度随最大行号位数动态调整（40.dp 固定宽在 4 位数行号下会截断）
    val lineNumberWidth = remember(totalLines) {
        ((totalLines.toString().length.coerceAtLeast(2)) * 8 + 8).dp
    }
    val lineNumberStyle = remember(theme) {
        TextStyle(
            color = theme.colorFor(TokenType.COMMENT).copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            lineHeight = codeLineHeight,
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
    val diffMarkerStyle = remember(theme) {
        TextStyle(
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            lineHeight = codeLineHeight
        )
    }
    // 每行 diff 样式缓存：避免逐行重复 when 分支与 TextStyle 分配
    val lineKindStyles = remember(theme) {
        CodeLineKind.entries.associateWith { kind ->
            LineKindStyle(
                textStyle = theme.textStyleForLine(kind),
                markerColor = theme.diffMarkerColor(kind),
                markerBackground = theme.diffMarkerBackground(kind),
                lineBackground = theme.backgroundForLine(kind),
            )
        }
    }
    // onTokenClick 定位用：行 -> 文本布局结果
    val tokenClickLayouts = remember { mutableMapOf<Int, TextLayoutResult>() }

    Column(
        modifier = modifier
            .background(theme.background)
            .fillMaxWidth()
    ) {
        if (showToolbar) {
            DisableSelection {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CodeBlockHeaderLabels(
                        title = title,
                        language = language,
                        theme = theme,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    if (showCopyButton) {
                        CopyButton(code = code, theme = theme)
                    }
                }
            }
        }

        // 代码内容区域（逐行渲染，行号与代码行严格对齐）
        val horizontalScrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScrollState)
        ) {
            val linesContent: @Composable () -> Unit = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    val renderedLines = if (fallbackToPlainLines) plainLines else resolvedLines
                    renderedLines.forEachIndexed { index, lineRender ->
                        key(index) {
                            val isLastLine = index == renderedLines.lastIndex
                            val diffMarker = diffMarkerForLine(lineRender.kind)
                            val rawLine = visibleLines.getOrElse(index) { "" }
                            val displayText = if (lineRender.text.text.isEmpty() && rawLine.isNotEmpty()) {
                                AnnotatedString(rawLine)
                            } else {
                                lineRender.text
                            }
                            val kindStyle = lineKindStyles.getValue(lineRender.kind)
                            Row(
                                modifier = Modifier.heightIn(min = codeLineHeightDp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showLineNumbers) {
                                    DisableSelection {
                                        BasicText(
                                            text = (startLine + index).toString(),
                                            style = lineNumberStyle,
                                            modifier = Modifier
                                                .width(lineNumberWidth)
                                                .padding(end = 8.dp),
                                            maxLines = 1
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(codeLineHeightDp)
                                                .background(
                                                    theme.colorFor(TokenType.COMMENT)
                                                        .copy(alpha = 0.3f)
                                                )
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .background(kindStyle.lineBackground)
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (diffMarker != null) {
                                        BasicText(
                                            text = diffMarker,
                                            style = diffMarkerStyle.copy(
                                                color = kindStyle.markerColor
                                            ),
                                            modifier = Modifier
                                                .background(kindStyle.markerBackground)
                                                .padding(horizontal = 6.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    BasicText(
                                        text = displayText,
                                        style = kindStyle.textStyle,
                                        onTextLayout = { tokenClickLayouts[index] = it },
                                        modifier = (if (onTokenClick != null) {
                                            Modifier.pointerInput(onTokenClick, index) {
                                                detectTapGestures { press ->
                                                    val layout = tokenClickLayouts[index]
                                                        ?: return@detectTapGestures
                                                    val charOffset = layout.getOffsetForPosition(press)
                                                    val absolute = (lineCharOffsets.getOrElse(index) { 0 }) + charOffset
                                                    val ast = visibleAst ?: return@detectTapGestures
                                                    ast.tokens.firstOrNull { absolute in it.range }
                                                        ?.let(onTokenClick)
                                                }
                                            }
                                        } else Modifier),
                                    )
                                    if (isLastLine) {
                                        StreamingCursor(
                                            isStreaming = isStreaming,
                                            color = theme.colorFor(TokenType.PLAIN)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selectable) SelectionContainer { linesContent() } else linesContent()
        }

        // 折叠/展开按钮
        if (isCollapsible) {
            DisableSelection {
                BasicText(
                    text = if (isExpanded) Strings.collapse() else Strings.expand(totalLines - visibleLineCount),
                    style = TextStyle(
                        color = theme.colorFor(TokenType.FUNCTION),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clickable { isExpanded = !isExpanded }
                )
            }
        }
    }
}

private fun diffMarkerForLine(kind: CodeLineKind): String? = when (kind) {
    CodeLineKind.DIFF_ADDED -> "+"
    CodeLineKind.DIFF_REMOVED -> "-"
    CodeLineKind.DIFF_META_HEADER -> "⋯"
    CodeLineKind.DIFF_META_HUNK -> "@"
    else -> null
}

private fun CodeTheme.textStyleForLine(kind: CodeLineKind) = when (kind) {
    CodeLineKind.DIFF_META_HEADER, CodeLineKind.DIFF_META_HUNK -> TextStyle(
        color = diffTextColor(kind),
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    )
    else -> TextStyle(
        color = diffTextColor(kind),
        fontSize = 13.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    )
}

private data class LineKindStyle(
    val textStyle: TextStyle,
    val markerColor: Color,
    val markerBackground: Color,
    val lineBackground: Color,
)

/**
 * 复制按钮组件。
 * 标记为 internal，仅供 CodeBlock 内部使用。
 *
 * 复制内容为原始 [code]（未做 CRLF 归一化），与渲染路径的归一化分离是有意为之：
 * 用户复制时期望得到与原始输入逐字节一致的文本。
 */
@Composable
internal fun CopyButton(
    code: String,
    theme: CodeTheme
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    // 计数器而非布尔：快速连点时 LaunchedEffect(copyCount) 重启，计时窗从最后一次点击重新起算
    var copyCount by remember { mutableStateOf(0) }
    val copied = copyCount > 0

    LaunchedEffect(copyCount) {
        if (copyCount > 0) {
            delay(2000)
            copyCount = 0
        }
    }

    DisableSelection {
        BasicText(
            text = if (copied) "✓ ${Strings.copied()}" else Strings.copy(),
            style = TextStyle(
                color = theme.colorFor(TokenType.FUNCTION).copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier
                .padding(4.dp)
                .clickable {
                    scope.launch {
                        clipboard.setClipEntry(textClipEntry(code))
                    }
                    copyCount++
                }
        )
    }
}
