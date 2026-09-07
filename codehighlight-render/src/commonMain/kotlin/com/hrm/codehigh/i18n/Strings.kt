package com.hrm.codehigh.i18n

/**
 * 代码块内置文案（收起/展开/复制等）注入接口。
 * 宿主可实现后通过 [LocalCodeBlockStrings] 覆盖默认文案。
 */
interface CodeBlockStrings {
    fun collapse(): String
    fun expand(hiddenLines: Int): String
    fun copy(): String
    fun copied(): String
}

/** 默认文案：跟随系统语言（中文/英文），语言检测在进程内仅执行一次 */
internal object DefaultCodeBlockStrings : CodeBlockStrings {
    private val languageCode: String by lazy {
        try {
            platformLanguageTag().substringBefore('-').ifBlank { "en" }.lowercase()
        } catch (_: Exception) {
            "en"
        }
    }
    private val isChinese: Boolean get() = languageCode.startsWith("zh")

    override fun collapse(): String = if (isChinese) "▲ 收起" else "▲ Collapse"
    override fun expand(hiddenLines: Int): String {
        val n = maxOf(0, hiddenLines)
        return if (isChinese) "▼ 展开 ($n 行)" else "▼ Expand ($n ${if (n == 1) "line" else "lines"})"
    }
    override fun copy(): String = if (isChinese) "复制" else "Copy"
    override fun copied(): String = if (isChinese) "已复制" else "Copied"
}

/** 平台语言标签（BCP-47，如 zh-Hans-CN），仅取主子标签做语言判断 */
internal expect fun platformLanguageTag(): String
