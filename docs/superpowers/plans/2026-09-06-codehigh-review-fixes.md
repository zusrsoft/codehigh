# CodeHigh 评审问题全量修复实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复深度评审发现的全部具体问题：2 个崩溃、错乱级缺陷、流式渲染性能、增量引擎正确性、测试伪覆盖、构建工程化缺口。
**Architecture:** parser 层先行（CodeToken API 破坏性变更为 2.0.0），Lexer 接口增加增量协商契约（`tokenize(code, startOffset)` / `isExtendableToken`），IncrementalHighlighter 消费该契约；render 层随后（异步解析、onTokenClick 实现、增量跳过重建、稳定性注解）；最后构建CI/文档。两个大重构（词法器声明式收敛、内层LazyColumn）经用户决策明确**跳过**。
**Tech Stack:** Kotlin Multiplatform 2.3.20、Compose Multiplatform 1.10.3、Gradle 9.3.1、AGP 9.1.1、vanniktech 0.36。
**约定：**
- 命令均在仓库根`D:\dev-java-learn-2026-ai\huarangmeng-zusrsoft\codehigh` 下以 PowerShell 执行，gradle 命令统一写作 `.\gradlew.bat <task>`。
- 快速回归命令：`.\gradlew.bat :codehighlight-parser:jvmTest :codehighlight-render:jvmTest --console=plain`（下文简称REGRESS）。
- 提交信息风格沿用仓库现状（中文+ `fix:`/`feat:`/`test:`/`chore:` 前缀）。
- 版本号升级为 **2.0.0**（破坏性变更：CodeToken 构造签名、InlineCodeSize 字段名），在 T12 落地。
---

### Task 1: CodeToken 惰性text 重构（破坏性API 变更）
**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/ast/CodeToken.kt`（全文件替换）- Modify: 全部词法器构造点（见步骤 3 清单与替换规则）
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/BaseLexer.kt`（删除死代码 TokenBuilder）- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/HighlightedString.kt:46-107`（删除死代码 buildLineRendersFromOffset/toRelativeTokens）- Modify: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/renderer/CodeLineRenderTest.kt`（构造点迁移）
- [ ] **Step 1: 重写 CodeToken.kt**

```kotlin
package com.hrm.codehigh.ast

/**
 * 代码 Token 数据结构，表示词法分析后的最小语义单元。
 * 惰性持有源文本引用，仅在访问[text] 时切片，避免每Token 全量拷贝源码。 *
 * 等值语义仅比较 [type] 与[range]；text 需要时显式断言。 *
 * @param type Token 类型
 * @param range Token 在源文本中的位置范围（相对[source] 从0 计）
 * @param source 源文本引用。 */
class CodeToken(
    val type: TokenType,
    val range: IntRange,
    val source: CharSequence,
) {
    /** Token 原始文本，惰性切片*/
    val text: String by lazy(LazyThreadSafetyMode.NONE) {
        source.subSequence(range.first, range.last + 1).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CodeToken) return false
        return type == other.type && range == other.range
    }

    override fun hashCode(): Int = 31 * type.hashCode() + range.hashCode()

    override fun toString(): String =
        "CodeToken(type=$type, text=$text, range=$range)"
}
```

- [ ] **Step 2: 全仓库替换构造点（3 种形态）**

用编辑工具或 PowerShell 批量处理，规则（`X`/`A`/`B` 为任意表达式）：
1. `CodeToken(X, code.substring(A, B), A until B)` →`CodeToken(X, A until B, code)`（注意：substring 与until 的两个参数通常相同表达式；若不同（如`start..end`），规则为`CodeToken(X, code.substring(A, B), C)` →`CodeToken(X, C, code)`；2. `CodeToken(type, word, start until pos)` →`CodeToken(type, start until pos, code)`（word 类局部变量形态）
3. `CodeToken(TokenType.PLAIN, c.toString(), pos until pos + 1)` → `CodeToken(TokenType.PLAIN, pos until pos + 1, code)`

涉及文件（grep `CodeToken(` 验证无遗漏）：`lexer/` 目录全部 24 个.kt（KotlinLexer、PythonLexer、JavaLexer、JavaScriptLexer、TypeScriptLexer、GoLexer、RustLexer、SwiftLexer、CLexer、CssLexer、JsonLexer、YamlLexer、TomlLexer、SqlLexer、XmlLexer（含 HtmlLexer）、BashLexer、DiffLexer、DartLexer、ScalaLexer、LuaLexer、HaskellLexer、ElixirLexer、RLangLexer、PhpLexer）、`lexer/ConfigurableLexer.kt`、`lexer/BaseLexer.kt`（TokenBuilder 内部——该文件本任务将删除大部分）、`lexer/LanguageRegistry.kt` 无构造点、`stream/IncrementalHighlighter.kt:104-110`（dirtyTokens map——改为`CodeToken(token.type, (token.range.first + reparseStart)..(token.range.last + reparseStart), newCode)`，不再需要text 传递）。注意：各词法器文件内局部变量名可能是`code` 以外的名字（如`input`），替换时保持源变量名一致。
- [ ] **Step 3: 删除死代码*

`BaseLexer.kt` 全文件替换为：
```kotlin
package com.hrm.codehigh.lexer

/**
 * 词法分析器基础工具类。 * 标记为internal，仅供模块内部使用。 */
internal abstract class BaseLexer : Lexer
```

`HighlightedString.kt`：删除`buildLineRendersFromOffset`（46-86 行）、`toRelativeTokens`（78-107 行）两个函数，`buildLineRenders` 原有参数不变、函数体改为直接执行原FromOffset 版本在startLineIndex=0/startCharOffset=0 时的逻辑（即：遍历sourceLines、对 buildHighlightedString 产物按行切片、resolveLineKind）。
- [ ] **Step 4: 迁移 CodeLineRenderTest 构造点**

将`CodeToken(TokenType.KEYWORD, "fun", 0 until 3)` 等改为`CodeToken(TokenType.KEYWORD, 0 until 3, src)`，并在测试方法开头定义`val src = "fun hello()\nprintln(\"ok\")\nreturn"`3 行源文本拼接，各 token 的text 即为其切片）。断言不变（`.text` 仍可用）。
- [ ] **Step 5: REGRESS 验证通过**

- [ ] **Step 6: Commit** `feat: CodeToken 惰性text 重构，消除每Token 源码拷贝（2.0.0 破坏性变更）`

---

### Task 2: 词法器转义越界修复+ 空白合并 + 参数化回归测试
**Files:**
- Create: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/lexer/EscapeBoundsTest.kt`
- Modify: 14 个词法器文件的转义行（步骤2 清单）与 PLAIN 兜底分支
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/BaseLexer.kt`（加 whitespaceEnd）
- [ ] **Step 1: 先写崩溃回归测试（当前应失败）*

```kotlin
package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EscapeBoundsTest {

    /** 反斜杠结尾的未闭合字符串：历史bug 中IndexOutOfBoundsException */
    private val trailingBackslashCases = listOf("\"a\\", "'a\\", "\"\\", "'\\", "\"C:\\")

    private val languagesWithQuotes = listOf(
        "kotlin", "java", "python", "javascript", "typescript", "go", "rust",
        "swift", "c", "cpp", "css", "json", "yaml", "bash"
    )

    @Test
    fun should_notThrow_when_trailingBackslashInUnclosedString() {
        for (lang in languagesWithQuotes) {
            for (case in trailingBackslashCases) {
                val tokens = LanguageRegistry.getOrPlain(lang).tokenize(case)
                // 全字符覆盖不变式
                assertEquals(case, tokens.joinToString("") { it.text }, "lang=$lang case=${case.replace("\\", "\\\\")}")
            }
        }
    }

    @Test
    fun should_mergeWhitespace_when_plainFallbackRuns() {
        val tokens = KotlinLexer.tokenize("fun a() {}\n\n\nval x = 1")
        val whitespaceTokens = tokens.filter { it.type == TokenType.PLAIN && it.text.all { c -> c.isWhitespace() } }
        // 连续空白应合并为单个 Token（此处含 \n\n\n 三连换行）
        assertTrue(whitespaceTokens.any { it.text.length > 1 }, "应存在合并后的多字符空白 Token")
    }

    @Test
    fun should_mergeCrlf_when_windowsLineEndings() {
        val tokens = KotlinLexer.tokenize("val a = 1\r\nval b = 2")
        assertTrue(tokens.any { it.type == TokenType.PLAIN && it.text == "\r\n" }, "\\r\\n 应合并为单个 PLAIN Token")
    }
}
```

运行 `.\gradlew.bat :codehighlight-parser:jvmTest --tests "com.hrm.codehigh.lexer.EscapeBoundsTest" --console=plain`，预期第一个用例抛 IndexOutOfBoundsException（FAIL），后两个FAIL。
- [ ] **Step 2: 修复全部 22 处转义越界*

统一替换：`if (code[pos] == '\\') pos++` →`if (code[pos] == '\\' && pos + 1 < code.length) pos++`（KotlinLexer:90 带注释`// 跳过转义字符` 一并保留注释）。精确清单（文件行）：BashLexer:60,73；CLexer:146,159（CLexer:94 已安全不动）；CssLexer:54；GoLexer:72,85；JavaLexer:90,103；JavaScriptLexer:68,81,94；JsonLexer:25；KotlinLexer:90,103；PythonLexer:93,106,119；RustLexer:119,132,155；SwiftLexer:92；TypeScriptLexer:90,103,116；YamlLexer:72。（ConfigurableLexer:131 已安全不动）

- [ ] **Step 3: 空白合并**

`BaseLexer.kt` 增加：
```kotlin
    /** 返回 [pos] 起连续空白后的首个非空白位置 */
    protected fun whitespaceEnd(code: String, pos: Int): Int {
        var i = pos
        while (i < code.length && code[i].isWhitespace()) i++
        return i
    }
```

14 个手写词法器（BashLexer:157、CLexer:228、CssLexer:168、GoLexer:160、JavaLexer:170、JavaScriptLexer:169、JsonLexer:77、KotlinLexer:177、PythonLexer:194、RustLexer:246、SwiftLexer:160、YamlLexer:141、TypeScriptLexer:186、SqlLexer:177）的 PLAIN 兜底。
```kotlin
            // 其他字符：连续空白合并为单个 PLAIN，其余逐字符兜底            if (c.isWhitespace()) {
                val start = pos
                pos = whitespaceEnd(code, pos)
                tokens.add(CodeToken(TokenType.PLAIN, start until pos, code))
                continue
            }
            tokens.add(CodeToken(TokenType.PLAIN, pos until pos + 1, code))
            pos++
```

注意 CLexer.kt 内含 `CLexer` ，`CppLexer` 两个 object——用 grep `PLAIN, c\.toString\(\)` 找到该文件内全部兜底（清单中 228 行仅一处，若CppLexer 另有则同型替换）。ConfigurableLexer:198 的PLAIN 兜底做同样处理（该类不继承BaseLexer，内含while 循环）。
- [ ] **Step 4: REGRESS 验证（含新测试）通过**

- [ ] **Step 5: Commit** `fix: 修复反斜杠结尾未闭合字符串的越界崩溃，合并连续空白Token`

---

### Task 3: Lexer 接口扩展 + KotlinLexer/PythonLexer 专项修复

**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/Lexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/KotlinLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/PythonLexer.kt`
- Create: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/lexer/LexerContractTest.kt`

- [ ] **Step 1: 扩展 Lexer 接口**

将`Lexer.kt` 接口体内追加（import `com.hrm.codehigh.ast.TokenType`）：

```kotlin
    /**
     * 带全文偏移的词法分析，供增量引擎的token 边界重启解析时提供上下文
     * （如行首敏感判定）。返回Token 的range 仍相对[code] 从0 计，调用方自行偏移。     */
    public fun tokenize(code: String, startOffset: Int): List<CodeToken> = tokenize(code)

    /**
     * 该Token 是否可能继续吸收后续字符（未闭合的多行结构）。
 * 增量引擎用它决定是否从该 Token 起点重解析；短前缀（如单个 `"`）由引擎处理     * 邻近 Token 回退兜底，无需在此穷举。     */
    public fun isExtendableToken(token: CodeToken): Boolean {
        val t = token.text
        return when (token.type) {
            TokenType.COMMENT -> t.length >= 2 && t.startsWith("/*") && !t.endsWith("*/")
            TokenType.STRING -> when {
                t.length >= 3 && t.startsWith("\"\"\"") && !t.endsWith("\"\"\"") -> true
                t.length >= 3 && t.startsWith("'''") && !t.endsWith("'''") -> true
                t.length >= 2 && t.startsWith("\"") && !t.endsWith("\"") -> true
                t.length >= 2 && t.startsWith("'") && !t.endsWith("'") -> true
                t.length >= 2 && t.startsWith("`") && !t.endsWith("`") -> true
                else -> false
            }
            else -> false
        }
    }
```

- [ ] **Step 2: KotlinLexer 专项**

a) 多行注释支持嵌套（替换49-60 行分支）。
```kotlin
            // 多行注释（Kotlin 支持嵌套。            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
                val start = pos
                pos += 2
                var depth = 1
                while (pos < code.length && depth > 0) {
                    if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
                        depth++; pos += 2
                    } else if (pos + 1 < code.length && code[pos] == '*' && code[pos + 1] == '/') {
                        depth--; pos += 2
                    } else {
                        pos++
                    }
                }
                tokens.add(CodeToken(TokenType.COMMENT, start until pos, code))
                continue
            }
```

b) 运算符表修正 + 预计算常量（object 顶部新增（154-167 行分支替换）。
```kotlin
    private val threeCharOps = setOf("..<")
    private val twoCharOps = setOf(
        "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=",
        "%=", "->", "=>", "::", "?.", ".."
    )
```

分支体改为（不再每次 setOf/substring）：

```kotlin
            if (c in "+-*/%=!<>&|^~?:") {
                val start = pos
                val c1 = if (pos + 1 < code.length) code[pos + 1] else ' '
                val c2 = if (pos + 2 < code.length) code[pos + 2] else ' '
                when {
                    "$c$c1$c2" in threeCharOps -> pos += 3
                    "$c$c1" in twoCharOps -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, start until pos, code))
                continue
            }
```

c) 112 行数字分支的死条件化简：`if (c.isDigit() || (c == '0' && ...))` →`if (c.isDigit())`。
d) 标点分支 170 行`c in "{}()[];,.$"` 去掉 `.`（`..` 已进运算符表；单字符 `.` 保留标点：改为兜底前单独处理——具体：标点集合改为 `"{}()[];,."`，`$` 归入兜底 PLAIN）。
- [ ] **Step 3: PythonLexer 专项**

a) 字符串前缀组合 + 三引号（替换 85-99 行分支，object 顶部新增）：

```kotlin
    private val stringPrefixes = setOf("r", "b", "u", "f", "rb", "br", "rf", "fr")

    /** 返回 [pos] 起合法字符串前缀长度（后随引号才算），否则0 */
    private fun stringPrefixLengthAt(code: String, pos: Int): Int {
        for (len in 2 downTo 1) {
            if (pos + len >= code.length) continue
            if (code.substring(pos, pos + len).lowercase() in stringPrefixes) {
                val next = code[pos + len]
                if (next == '"' || next == '\'') return len
            }
        }
        return 0
    }
```

分支体：

```kotlin
            // 字符串前缀（f/r/b/u 与rb、br、rf、fr 组合，支持三引号。
            val prefixLen = stringPrefixLengthAt(code, pos)
            if (prefixLen > 0) {
                val start = pos
                pos += prefixLen
                val quote = code[pos]
                if (pos + 2 < code.length && code[pos + 1] == quote && code[pos + 2] == quote) {
                    pos += 3
                    while (pos + 2 < code.length &&
                        !(code[pos] == quote && code[pos + 1] == quote && code[pos + 2] == quote)
                    ) {
                        if (code[pos] == '\\' && pos + 1 < code.length) pos++
                        pos++
                    }
                    if (pos + 2 < code.length) pos += 3 else pos = code.length
                } else {
                    pos++
                    while (pos < code.length && code[pos] != quote && code[pos] != '\n') {
                        if (code[pos] == '\\' && pos + 1 < code.length) pos++
                        pos++
                    }
                    if (pos < code.length && code[pos] == quote) pos++
                }
                tokens.add(CodeToken(TokenType.STRING, start until pos, code))
                continue
            }
```

b) 运算符表预计算（同KotlinLexer 方式（173-184 行分支）。
```kotlin
    private val threeCharOps = setOf("**=", "//=", ">>=", "<<=")
    private val twoCharOps = setOf(
        "==", "!=", "<=", ">=", "**", "//", "+=", "-=", "*=", "/=", "%=",
        "&=", "|=", "^=", "->", "<<", ">>"
    )
```

分支体使用`"$c$c1$c2" in threeCharOps` / `"$c$c1" in twoCharOps` 字符串模板比较。
- [ ] **Step 4: 接口契约测试**

```kotlin
package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LexerContractTest {

    @Test
    fun should_extend_when_unclosedBlockComment() {
        assertTrue(KotlinLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 20, "/* unclosed comment ")))
    }

    @Test
    fun should_notExtend_when_closedBlockComment() {
        assertFalse(KotlinLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 20, "/* closed */")))
    }

    @Test
    fun should_extend_when_unclosedTripleQuote() {
        assertTrue(KotlinLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 12, "\"\"\"abc\ndef")))
    }

    @Test
    fun should_notExtend_when_closedString() {
        assertFalse(KotlinLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 5, "\"ab\"")))
    }

    @Test
    fun should_tokenizeNestedComment_when_kotlinBlockComment() {
        val tokens = KotlinLexer.tokenize("/* a /* b */ c */ val x = 1")
        assertEquals(1, tokens.count { it.type == TokenType.COMMENT })
        assertEquals("/* a /* b */ c */", tokens.first { it.type == TokenType.COMMENT }.text)
    }

    @Test
    fun should_tokenizeRangeOperator_when_dotDot() {
        val tokens = KotlinLexer.tokenize("1..5")
        assertTrue(tokens.any { it.type == TokenType.OPERATOR && it.text == ".." })
    }

    @Test
    fun should_tokenizeRangeUntil_when_dotDotLess() {
        val tokens = KotlinLexer.tokenize("1..<5")
        assertTrue(tokens.any { it.type == TokenType.OPERATOR && it.text == "..<" })
    }

    @Test
    fun should_tokenizeTripleQuotedFString_when_python() {
        val tokens = PythonLexer.tokenize("msg = f\"\"\"hello {name}\"\"\"")
        assertEquals("\"\"\"hello {name}\"\"\"", tokens.first { it.type == TokenType.STRING }.text)
    }

    @Test
    fun should_tokenizeCombinedPrefix_when_pythonBytes() {
        val tokens = PythonLexer.tokenize("data = rb'raw\\x00'")
        assertEquals("rb'raw\\x00'", tokens.first { it.type == TokenType.STRING }.text)
    }

    @Test
    fun should_keepIdentifier_when_prefixLetterFollowedByNonQuote() {
        val tokens = PythonLexer.tokenize("format(x)")
        assertTrue(tokens.any { it.type == TokenType.IDENTIFIER && it.text == "format" })
    }
}
```

- [ ] **Step 5: REGRESS 通过** ；[x] **Step 6: Commit** `feat: Lexer 接口增加增量协商契约；修复Kotlin 嵌套注释/运算符表与Python 前缀字符串`

---

### Task 4: YamlLexer 行首上下文+ IncrementalHighlighter 重构

**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/YamlLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/stream/IncrementalHighlighter.kt`
- Modify: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/stream/IncrementalHighlighterTest.kt`（追加用例，、T7 完成强化。
- [ ] **Step 1: 先写假行首回归测试（追加至IncrementalHighlighterTest）*

```kotlin
    @Test
    fun should_notTreatDirtySubstringStartAsLineStart_when_yamlIncremental() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("a--", "yaml")
        val ast = highlighter.update("a---", "yaml")
        // 行首敏感的--- 文档分隔符判定必须使用全文上下文，dirty 子串起始处不是行。        assertTrue(
            ast.tokens.none { it.type == TokenType.KEYWORD && it.text == "---" },
            "增量重解析不应把子串起点误判为行首：tokens=${ast.tokens}"
        )
    }

    @Test
    fun should_returnNoChange_when_cacheHit() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("fun a() {}", "kotlin")
        val result = highlighter.updateDetailed("fun a() {}", "kotlin")
        assertEquals(-1, result.firstChangedLine)
        assertEquals(-1, result.reparseStart)
    }

    @Test
    fun should_reparseFromUnfinishedTokenStart_when_lookbackExceeded() {
        val highlighter = IncrementalHighlighter()
        // 超过 64 字符回看窗口的未闭合注释
        val longComment = "/* " + "x".repeat(200)
        highlighter.update(longComment, "kotlin")
        val result = highlighter.updateDetailed(longComment + "\n*/\nval x = 1", "kotlin")
        assertEquals(0, result.reparseStart, "未闭合注释应从其起点重解析而非全量回退到0")
        assertTrue(result.ast.tokens.any { it.type == TokenType.COMMENT && it.text.contains("*/") })
    }

    @Test
    fun should_fullReparse_when_codeEditedNotAppended() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("fun a() {}", "kotlin")
        val result = highlighter.updateDetailed("fun b() {}", "kotlin")
        assertEquals(0, result.firstChangedLine)
        assertEquals(0, result.reparseStart)
        assertEquals("fun b() {}", result.ast.source)
    }
```

运行增量测试，前两个用例预期 FAIL。
- [ ] **Step 2: YamlLexer 支持行首上下文*

将`tokenize(code: String)` 重命名为带偏移实现：

```kotlin
    override fun tokenize(code: String): List<CodeToken> = tokenize(code, 0)

    override fun tokenize(code: String, startOffset: Int): List<CodeToken> {
        // ...原主循环不变，仅文档分隔符分支的行首判定改为：        // if ((code.startsWith("---", pos) || code.startsWith("...", pos)) &&
        //     (startOffset + pos == 0 || code[startOffset + pos - 1] == '\n')) {
```

同时（同文件）：
- 转义修复已在 T2 完成；- `~` 死配置修复：在结构符号分支后新增分支。
```kotlin
            // null 鍊?~
            if (c == '~') {
                tokens.add(CodeToken(TokenType.BUILTIN, pos until pos + 1, code))
                pos++
                continue
            }
```

- [ ] **Step 3: IncrementalHighlighter 重构**

完整替换文件：
```kotlin
package com.hrm.codehigh.stream

import com.hrm.codehigh.ast.CodeAst
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.lexer.LanguageRegistry

/**
 * 增量高亮引擎，用于流式场景下的高效代码高亮更新。 *
 * 核心策略： * 1. 稳定前缀 Token 直接复用，不重新解析
 * 2. 仅对尾部脏区域（从最后一个受影响 Token 到文本末尾）重新解析。 *    并把重解析起点以 startOffset 传给词法器，保证行首等全文上下文判定正确
 * 3. 相同代码字符串和语言命中 AST 缓存，直接返回缓存结果 *
 * [UpdateResult.firstChangedLine] / [UpdateResult.reparseStart] ：-1 表示无变化。 */
class IncrementalHighlighter {
    data class UpdateResult(
        val ast: CodeAst,
        /** 首个变更行号；1 表示本次无变化（缓存命中）*/
        val firstChangedLine: Int,
        /** 重解析起点；-1 表示本次无变化（缓存命中）*/
        val reparseStart: Int,
    )

    private companion object {
        private const val CONTEXT_LOOKBACK_CHARS = 64
    }

    private var cachedAst: CodeAst? = null
    private var lastLanguage: String = ""

    fun update(code: String, language: String): CodeAst {
        return updateDetailed(code, language).ast
    }

    fun updateDetailed(code: String, language: String): UpdateResult {
        if (language != lastLanguage) {
            lastLanguage = language
            cachedAst = null
        }

        val cached = cachedAst
        // 长度短路，避免大字符串逐字符比较        if (cached != null && cached.source.length == code.length && cached.source == code) {
            return UpdateResult(ast = cached, firstChangedLine = -1, reparseStart = -1)
        }

        val update = if (cached != null && code.startsWith(cached.source)) {
            incrementalParse(code, language, cached)
        } else {
            UpdateResult(ast = fullParse(code, language), firstChangedLine = 0, reparseStart = 0)
        }

        cachedAst = update.ast
        return update
    }

    private fun fullParse(code: String, language: String): CodeAst {
        val lexer = LanguageRegistry.getOrPlain(language)
        return CodeAst(lexer.tokenize(code), code, language)
    }

    private fun incrementalParse(newCode: String, language: String, oldAst: CodeAst): UpdateResult {
        val appendedStart = oldAst.source.length
        val reparseStart = determineReparseStart(oldAst, appendedStart)
        val lexer = LanguageRegistry.getOrPlain(language)
        // 按稳定Token 数取前缀视图，O(1) 无拷贝        val stableCount = oldAst.tokens.count { it.range.last < reparseStart }
        val stable = oldAst.tokens.subList(0, stableCount)

        // startOffset 让词法器以全文视角判断行首等上下文；返回 range 仍相对子串。
        val dirtyCode = newCode.substring(reparseStart)
        val dirtyTokens = lexer.tokenize(dirtyCode, reparseStart).map { token ->
            CodeToken(
                type = token.type,
                range = (token.range.first + reparseStart)..(token.range.last + reparseStart),
                source = newCode,
            )
        }

        val allTokens = ArrayList<CodeToken>(stable.size + dirtyTokens.size)
        allTokens.addAll(stable)
        allTokens.addAll(dirtyTokens)

        return UpdateResult(
            ast = CodeAst(allTokens, newCode, language),
            firstChangedLine = newCode.countLinesBefore(reparseStart),
            reparseStart = reparseStart,
        )
    }

    private fun determineReparseStart(oldAst: CodeAst, appendedStart: Int): Int {
        if (appendedStart == 0) return 0

        // 未闭合多行结构：不受回看窗口限制（否则长注释/长字符串导致每次全量重解析，流式累计 O(n²)。        val unfinishedTokenStart = oldAst.tokens
            .asReversed()
            .firstOrNull { it.range.last < appendedStart && LanguageRegistry.getOrPlain(oldAst.language).isExtendableToken(it) }
            ?.range
            ?.first

        // 邻近 Token 回退：吸收因追加而被改变尾部的最后一个短 Token
        val nearbyTokenStart = oldAst.tokens
            .lastOrNull { it.range.last < appendedStart && appendedStart - it.range.first <= CONTEXT_LOOKBACK_CHARS }
            ?.range
            ?.first

        return unfinishedTokenStart ?: nearbyTokenStart ?: 0
    }

    /** 清除缓存，强制下次全量解析*/
    fun invalidate() {
        cachedAst = null
        lastLanguage = ""
    }
}

private fun String.countLinesBefore(charIndex: Int): Int {
    if (charIndex <= 0) return 0
    val end = charIndex.coerceAtMost(length)
    var count = 0
    for (i in 0 until end) if (this[i] == '\n') count++
    return count
}
```

注意：dirtyTokens 以`newCode` 为source，惰性text 直接从新文本切片。
- [ ] **Step 4: REGRESS 通过（重点IncrementalHighlighterTest 全绿）*

- [ ] **Step 5: Commit** `fix: 增量引擎消费词法器上下文契约，修复假行首与长未闭合Token 的O(n²) 退化`

---

### Task 5: ConfigurableLexer 排序缓存 + 特殊定界符覆盖
**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/ConfigurableLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/LuaLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/HaskellLexer.kt`

- [ ] **Step 1: 排序/匹配表预计算**

先读 ConfigurableLexer.kt 全文。将 `tokenize` 内每次执行的 `sortedByDescending { it.length }`（约 32-38 行，7 个类别）移到 object 初始化时，`private val`（spec 若为构造参数，在init/属性初始化器中预排序）。模式：

```kotlin
    // 预计算：按长度降序，避免每次 tokenize 重排
    private val sortedFixedTokens = spec.fixedTokens.sortedByDescending { it.length }
    private val sortedOperators = spec.operators.sortedByDescending { it.length }
    // ... 其余类别同理
```

主循环中相应 `sortedByDescending` 调用替换为预计算属性。字段名以实际文件为准适配。
- [ ] **Step 2: ConfigurableLexer 覆写 isExtendableToken**

依据 spec 的多行定界符实现（读文件后按实际字段名适配）：

```kotlin
    override fun isExtendableToken(token: CodeToken): Boolean {
        val t = token.text
        if (token.type == TokenType.COMMENT) {
            val start = spec.blockCommentStart ?: return super.isExtendableToken(token)
            val end = spec.blockCommentEnd ?: return super.isExtendableToken(token)
            return t.startsWith(start) && !t.endsWith(end)
        }
        return super.isExtendableToken(token)
    }
```

（字段名以实际spec 定义为准；若 spec 无块注释定界字段则只保留排序优化。）

- [ ] **Step 3: Lua/Haskell 覆写**

读两文件确认多行定界符（Lua 长字符串 `[[`/`]]`，Haskell 块注释`{-`/`-}`），在各object 内追加：

```kotlin
    // LuaLexer
    override fun isExtendableToken(token: CodeToken): Boolean {
        val t = token.text
        if (token.type == TokenType.STRING) {
            return t.startsWith("[[") && !t.endsWith("]]")
        }
        return super.isExtendableToken(token)
    }
```

```kotlin
    // HaskellLexer
    override fun isExtendableToken(token: CodeToken): Boolean {
        val t = token.text
        if (token.type == TokenType.COMMENT) {
            return t.startsWith("{-") && !t.endsWith("-}")
        }
        return super.isExtendableToken(token)
    }
```

（以实际定界符分支命名为准适配；若词法器不支持该结构则跳过并记录。）

- [ ] **Step 4: REGRESS 通过** ；[x] **Step 5: Commit** `perf: ConfigurableLexer 匹配表预计算；特殊定界符声明可扩展Token`

---

### Task 6: LanguageRegistry 线程安全

**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/LanguageRegistry.kt`

- [ ] **Step 1: 饿汉初始化*

删除 `defaultsRegistered`/`ensureDefaultsRegistered`，`init { registerDefaults() }`。注册完成后注册表仍可通过 `register()` 扩展（保持既有public 行为），但默认语言在类加载时一次写入（object 初始化由 JVM/Native 类加载机制保证线程安全；JS 单线程）。`registry`/`aliases` 保持 `internal` 可变 map（外部自定义语言注册是既有特性）。KDoc 注明：`register()` 非线程安全，建议在应用初始化阶段（单线程）调用。
- [ ] **Step 2: REGRESS 通过**

- [ ] **Step 3: Commit** `fix: LanguageRegistry 默认语言饿汉注册，消除懒初始化竞态`

---

### Task 7: parser 测试升级

**Files:**
- Modify: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/stream/IncrementalHighlighterTest.kt`
- Create: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/lexer/LanguageCoverageTest.kt`

- [ ] **Step 1: 修复伪覆盖断言**

`should_returnCachedAst_when_sameCodeAndLanguage`：`assertEquals(ast1, ast2)` →`assertSame(ast1, ast2)`（import kotlin.test.assertSame）、`should_invalidateCache_when_invalidateCalled`：追加`assertNotSame(ast1, ast2)`（import kotlin.test.assertNotSame）。
- [ ] **Step 2: 全语言覆盖测试（28 语言，含重构不变式）**

```kotlin
package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LanguageCoverageTest {

    /** 每语言最小样本：含注释、字符串、关键字/指令形态*/
    private val samples: Map<String, String> = mapOf(
        "kotlin" to "fun main() { /* c */ val s = \"hi\" }",
        "java" to "class A { // c\nString s = \"hi\"; }",
        "swift" to "func f() { /* c */ let s = \"hi\" }",
        "python" to "def f():\n    # c\n    s = \"hi\"",
        "javascript" to "function f() { // c\nconst s = \"hi\"; }",
        "typescript" to "function f(): string { /* c */ return \"hi\"; }",
        "ruby" to "def f\n  # c\n  s = \"hi\"\nend",
        "php" to "<?php\n// c\n\$s = \"hi\";",
        "dart" to "void f() { // c\nvar s = 'hi'; }",
        "scala" to "def f(): Unit = { /* c */ val s = \"hi\" }",
        "lua" to "function f()\n  -- c\n  local s = \"hi\"\nend",
        "haskell" to "-- c\nf :: String\nf = \"hi\"",
        "elixir" to "def f do\n  # c\n  s = \"hi\"\nend",
        "go" to "func f() { // c\ns := \"hi\"\n}",
        "rust" to "fn f() { /* c */ let s = \"hi\"; }",
        "c" to "int f() { /* c */ char* s = \"hi\"; }",
        "cpp" to "int f() { // c\nauto s = \"hi\"; }",
        "sql" to "SELECT * FROM t WHERE a = 'hi' -- c",
        "json" to "{ \"k\": \"hi\", \"n\": 1 }",
        "yaml" to "# c\nk: hi\nn: 1",
        "r" to "f <- function() {\n  # c\n  s <- \"hi\"\n}",
        "toml" to "[sec]\n# c\nk = \"hi\"",
        "dockerfile" to "FROM alpine\n# c\nENV A=1",
        "bash" to "#!/bin/bash\n# c\ns=\"hi\"",
        "diff" to "diff --git a/a b/a\n@@ -1 +1 @@\n-a\n+b",
        "xml" to "<!-- c --><a b=\"hi\"/>",
        "html" to "<!-- c --><p>hi</p>",
        "css" to "/* c */ a { color: red; }",
    )

    @Test
    fun should_coverAllCharacters_when_allLanguagesTokenized() {
        for ((lang, sample) in samples) {
            val tokens = LanguageRegistry.getOrPlain(lang).tokenize(sample)
            assertEquals(
                sample,
                tokens.joinToString("") { it.text },
                "lang=$lang 重构不变式破坏,
            )
        }
    }

    @Test
    fun should_haveCommentAndStringTokens_when_allLanguagesTokenized() {
        // XML/Dockerfile/Bash/YAML/TOML/R/Diff 各自至少存在注释或元信息 Token；        // JSON 无注释，仅断言字符串        for ((lang, sample) in samples) {
            val tokens = LanguageRegistry.getOrPlain(lang).tokenize(sample)
            if (lang != "json") {
                assertTrue(
                    tokens.any { it.type == TokenType.COMMENT },
                    "lang=$lang 应识别注释,
                )
            }
            assertTrue(
                tokens.any { it.type == TokenType.STRING || it.type == TokenType.COMMENT },
                "lang=$lang 应识别字符串或注释,
            )
        }
    }

    @Test
    fun should_resolveAllRegistryLanguages() {
        val known = setOf(
            "kotlin", "java", "swift", "python", "javascript", "typescript", "ruby", "php",
            "dart", "scala", "lua", "haskell", "elixir", "go", "rust", "c", "cpp", "sql",
            "json", "yaml", "r", "toml", "dockerfile", "bash", "diff", "xml", "html", "css"
        )
        known.forEach { assertNotNull(LanguageRegistry.get(it), "missing lexer for $it") }
    }

    @Test
    fun should_notThrow_when_unclosedConstructsAcrossLanguages() {
        // 流式中间态：各类未闭合结构        val unclosed = listOf("/* ", "\"", "'", "\"\"\"", "#", "<a href=", "{ \"k\": ")
        for (lang in samples.keys) {
            for (snippet in unclosed) {
                LanguageRegistry.getOrPlain(lang).tokenize(snippet) // 不抛异常即通过
            }
        }
    }
}
```

（若某语言样本断言失败，说明该词法器存在真实缺口：先修词法器再回来跑绿——对照失败信息定位，禁止放宽断言骗通过"。）

- [ ] **Step 3: REGRESS 通过** ；[x] **Step 4: Commit** `test: 断言缓存同一性与 28 语言全覆盖不变式`

---

### Task 8: 主题层稳定性+ diff 定制 + CompositionLocal 修正

**Files:**
- Create: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/theme/CodeLineKind.kt`（从 renderer 至public 化迁移）
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/theme/CodeTheme.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/theme/LocalCodeTheme.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/HighlightedString.kt`（CodeLineKind 引用改包。
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/CodeBlock.kt`（import 调整，本任务仅最小改动，重构留待T10；
- Modify: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/theme/CodeThemeTest.kt`

- [ ] **Step 1: CodeLineKind 迁移至theme 包并 public**

新文件：

```kotlin
package com.hrm.codehigh.theme

/** 代码行渲染种类：普通高亮/diff 各形态*/
enum class CodeLineKind {
    NORMAL, HIGHLIGHTED, DIFF_ADDED, DIFF_REMOVED, DIFF_META_HEADER, DIFF_META_HUNK
}
```

删除 HighlightedString.kt 内的 internal enum CodeLineKind，renderer 内引用改 import（CodeLineRender 保持 internal，其 kind 字段类型改为 theme.CodeLineKind）。
- [ ] **Step 2: CodeTheme 增强**

`CodeTheme.kt` 接口标注 `@Immutable`（import androidx.compose.runtime.Immutable），并追加diff 前景/标记默认属性（将CodeBlock.kt:269-294 硬编码迁入，自定义主题可覆写）：

```kotlin
    /** diff 标记（/-）前景色 */
    fun diffMarkerColor(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_ADDED -> if (isDark) Color(0xFF9BE9A8) else Color(0xFF1F7A38)
        CodeLineKind.DIFF_REMOVED -> if (isDark) Color(0xFFFFA8B5) else Color(0xFFB42318)
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFF9CDCFE) else Color(0xFF0958D9)
        else -> colorFor(TokenType.PLAIN)
    }

    /** diff 标记背景色*/
    fun diffMarkerBackground(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_ADDED -> if (isDark) Color(0xFF224D35) else Color(0xFFD9F5E0)
        CodeLineKind.DIFF_REMOVED -> if (isDark) Color(0xFF5A2730) else Color(0xFFFADADD)
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFF1F4B70) else Color(0xFFDCEEFF)
        else -> Color.Transparent
    }

    /** diff 元信息行正文颜色 */
    fun diffTextColor(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFFBFE3FF) else Color(0xFF0B4F8A)
        else -> colorFor(TokenType.PLAIN)
    }
```

`backgroundForLine`（HighlightedString.kt:167-174 的internal 扩展）保持不动。
- [ ] **Step 3: LocalCodeTheme 鏀?compositionLocalOf**

```kotlin
val LocalCodeTheme = compositionLocalOf<CodeTheme> { OneDarkProTheme }
```

（主题会随系统暗色模式动态切换，static 版不追踪依赖。）

- [ ] **Step 4: CodeThemeTest 升级**

合并 4 个同构用例+ Unspecified 强断言 + safeColorFor 真回退。
```kotlin
    private val allThemes = listOf(OneDarkProTheme, GithubLightTheme, DraculaProTheme, SolarizedLightTheme)

    @Test
    fun should_coverAllTokenTypes_when_builtinThemes() {
        for (theme in allThemes) for (type in TokenType.entries) {
            val color = theme.colorFor(type)
            assertNotEquals(Color.Unspecified, color, "${theme::class.simpleName} $type")
        }
    }

    @Test
    fun should_fallbackToPlainColor_when_themeThrowsForType() {
        val plain = OneDarkProTheme.colorFor(TokenType.PLAIN)
        val broken = object : CodeTheme by OneDarkProTheme {
            override fun colorFor(type: TokenType): Color =
                if (type == TokenType.KEYWORD) error("not implemented") else OneDarkProTheme.colorFor(type)
        }
        assertEquals(plain, broken.safeColorFor(TokenType.KEYWORD))
    }

    @Test
    fun should_provideDiffColors_when_customizable() {
        for (theme in allThemes) {
            assertNotEquals(Color.Unspecified, theme.diffMarkerColor(CodeLineKind.DIFF_ADDED))
            assertNotEquals(Color.Unspecified, theme.diffMarkerBackground(CodeLineKind.DIFF_REMOVED))
            assertNotEquals(Color.Unspecified, theme.diffTextColor(CodeLineKind.DIFF_META_HUNK))
        }
    }
```

删除原4 个`should_coverAllTokenTypes_when_*Theme` 与旧 `should_fallbackToPlain_when_safeColorFor`；isDark 断言并入allThemes 循环（dark 主题 2 个、light 2 个）。import `com.hrm.codehigh.theme.CodeLineKind`（同包无需）。
- [ ] **Step 5: REGRESS 通过** ；[x] **Step 6: Commit** `feat: 主题接口 @Immutable 与diff 颜色定制；修复CompositionLocal 语义`

---

### Task 9: HighlightedString 渲染优化 + 测试

**Files:**
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/HighlightedString.kt`
- Create: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/renderer/HighlightedStringTest.kt`

- [ ] **Step 1: buildHighlightedString 样式缓存 + 相邻合并**

替换 117-143 行实现：

```kotlin
fun buildHighlightedString(
    tokens: List<CodeToken>,
    theme: CodeTheme
): AnnotatedString {
    // 按类型缓存SpanStyle（主题单例、Token 类型有限），相邻同样式合并span
    val styleCache = HashMap<TokenType, SpanStyle>(TokenType.entries.size)
    fun styleFor(type: TokenType): SpanStyle = styleCache.getOrPut(type) {
        SpanStyle(
            color = theme.safeColorFor(type),
            fontWeight = if (type == TokenType.KEYWORD) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (type == TokenType.COMMENT) FontStyle.Italic else FontStyle.Normal,
        )
    }
    return buildAnnotatedString {
        var pushed = false
        var lastStyle: SpanStyle? = null
        for (token in tokens) {
            val style = styleFor(token.type)
            if (style != lastStyle) {
                if (pushed) pop()
                pushStyle(style)
                pushed = true
                lastStyle = style
            }
            append(token.text)
        }
        if (pushed) pop()
    }
}
```

`buildLineRenders` 内（原FromOffset 逻辑内联后）：`language.lowercase()` 提升到函数开头计算一次传给`resolveLineKind`（其签名内部改为接收已归一化language）。
- [ ] **Step 2: 测试**

```kotlin
package com.hrm.codehigh.renderer

import androidx.compose.ui.text.SpanStyle
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.ast.TokenType
import com.hrm.codehigh.theme.OneDarkProTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighlightedStringTest {

    @Test
    fun should_mergeAdjacentSpans_when_sameType() {
        val src = "aaa bbb"
        val tokens = listOf(
            CodeToken(TokenType.PLAIN, 0 until 3, src),
            CodeToken(TokenType.PLAIN, 3 until 7, src), // 含空格，同为 PLAIN
        )
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(src, s.text)
        assertEquals(1, s.spanStyles.size, "相邻同样式应合并为1 个span")
    }

    @Test
    fun should_keepDistinctSpans_when_typeChanges() {
        val src = "fun x"
        val tokens = listOf(
            CodeToken(TokenType.KEYWORD, 0 until 3, src),
            CodeToken(TokenType.PLAIN, 3 until 5, src),
        )
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(2, s.spanStyles.size)
    }

    @Test
    fun should_reuseEqualSpanStyle_when_typeRepeats() {
        val src = "a b c"
        val tokens = listOf(
            CodeToken(TokenType.IDENTIFIER, 0 until 1, src),
            CodeToken(TokenType.PLAIN, 1 until 3, src),
            CodeToken(TokenType.IDENTIFIER, 3 until 5, src),
        )
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(3, s.spanStyles.size)
        assertTrue(s.spanStyles.map { it.item }.distinct().size <= 2, "PLAIN 与IDENTIFIER 同色也应复用相等 SpanStyle")
        // SpanStyle 鐩哥瓑鎬?        assertEquals(SpanStyle(color = OneDarkProTheme.colorFor(TokenType.PLAIN)), s.spanStyles[0].item)
    }
}
```

注：OneDarkPro 的IDENTIFIER 与PLAIN 同色（xFFABB2BF），相邻同样式（含中间不可push 逻辑）依实现应为 3 个span；若断言与实现细节冲突以"span 数、token 数且相邻同类型合并为准修正断言并说明。
- [ ] **Step 3: REGRESS 通过** ；[x] **Step 4: Commit** `perf: 高亮字符数SpanStyle 缓存与相邻合并，span 数量去重`

---

### Task 10: CodeBlock 渲染重构

**Files:**
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/CodeBlock.kt`

- [ ] **Step 1: 入口归一化与记忆化（替换 71-94 行区域）**

```kotlin
    // CRLF 归一化（Windows 剪贴板常见），并记忆化行拆分，流式高频重组下避免 O(n) 重分析    val normalizedCode = remember(code) { if (code.contains('\r')) code.replace("\r\n", "\n").replace("\r", "\n") else code }
    val lines = remember(normalizedCode) { normalizedCode.split("\n") }
    val totalLines = lines.size
    val isCollapsible = maxVisibleLines != null && totalLines > maxVisibleLines
    val visibleLineCount = when {
        !isCollapsible || isExpanded -> totalLines
        else -> maxVisibleLines!! // isCollapsible 为true 时必非空
    }
    val visibleLines = remember(visibleLineCount, lines) { lines.take(visibleLineCount) }
    val visibleCode = remember(visibleLines) { visibleLines.joinToString("\n") }
    val lineCharOffsets = remember(visibleLines) {
        IntArray(visibleLines.size).also { acc ->
            var off = 0
            for (i in visibleLines.indices) { acc[i] = off; off += visibleLines[i].length + 1 }
        }
    }
```

- [ ] **Step 2: 异步解析 + 增量信息消费（替换visibleAst/lineHighlights 两段 remember）*

```kotlin
    val highlighter = remember { IncrementalHighlighter() }
    // 初始 null：首帧先走纯文本回退，解析在 Default 调度器执行后替换
    var lineHighlights by remember { mutableStateOf<List<CodeLineRender>?>(null) }
    var visibleAst by remember { mutableStateOf<CodeAst?>(null) }
    LaunchedEffect(visibleCode, language, theme) {
        val result = withContext(Dispatchers.Default) {
            val detailed = highlighter.updateDetailed(visibleCode, language)
            if (detailed.firstChangedLine < 0) null else { // 无变化时保留现有渲染
                detailed.ast to buildLineRenders(
                    sourceLines = visibleLines,
                    tokens = detailed.ast.tokens,
                    theme = theme,
                    language = language,
                    highlightedLines = highlightedLines,
                )
            }
        }
        if (result != null) {
            visibleAst = result.first
            lineHighlights = result.second
        }
    }
    val resolvedLines = lineHighlights ?: visibleLines.map { CodeLineRender(CodeLineKind.NORMAL, AnnotatedString(it)) }
```

（`highlighter` 不再以language 作key——updateDetailed 内部已处理语言切换。新增import：`com.hrm.codehigh.ast.CodeAst`、`kotlinx.coroutines.Dispatchers`、`kotlinx.coroutines.withContext`。`fallbackToPlainLines` 判定改基于resolvedLines。）

- [ ] **Step 3: 布局修复**

- 行容器：`Modifier.height(codeLineHeightDp)` →`Modifier.heightIn(min = codeLineHeightDp)`（import androidx.compose.foundation.layout.heightIn）。
- 行号宽度动态：`val lineNumberWidth = remember(totalLines) { ((totalLines.toString().length.coerceAtLeast(2)) * 8 + 8).dp }`（91 行）`.width(40.dp)` →`.width(lineNumberWidth)`。
- 正文样式缓存：`val textStylesByKind = remember(theme) { CodeLineKind.entries.associateWith { theme.textStyleForLine(it) } }`；私有扩展`textStyleForLine` 改用 `theme.diffTextColor(kind)`；`diffMarkerBackgroundForLine`/`diffMarkerColorForLine` 私有扩展删除，调用点改`theme.diffMarkerBackground(kind)`/`theme.diffMarkerColor(kind)`、25-228 行BasicText：`style = textStylesByKind.getValue(lineRender.kind)`。
- KDoc 与默认值：`maxVisibleLines: Int? = 500`，KDoc 更新（默认 500，null 不限制）。
- [ ] **Step 4: onTokenClick 实现**

```kotlin
    val tokenClickLayouts = remember { mutableMapOf<Int, TextLayoutResult>() }
```

正文 BasicText（Step 3 修改后）追加参数。
```kotlin
                                BasicText(
                                    text = displayText,
                                    style = textStylesByKind.getValue(lineRender.kind),
                                    onTextLayout = { tokenClickLayouts[index] = it },
                                    modifier = if (onTokenClick != null) {
                                        Modifier.pointerInput(onTokenClick, index) {
                                            detectTapGestures { press ->
                                                val layout = tokenClickLayouts[index] ?: return@detectTapGestures
                                                val charOffset = layout.getOffsetForPosition(press)
                                                val absolute = (lineCharOffsets.getOrElse(index) { 0 }) + charOffset
                                                val ast = visibleAst ?: return@detectTapGestures
                                                ast.tokens.firstOrNull { absolute in it.range }?.let(onTokenClick)
                                            }
                                        }
                                    } else Modifier,
                                )
```

import：`androidx.compose.foundation.gestures.detectTapGestures`、`androidx.compose.ui.input.pointer.pointerInput`、`androidx.compose.ui.text.TextLayoutResult`。
- [ ] **Step 5: 文本可选+ CopyButton**

- 新参数`selectable: Boolean = true`（KDoc：正文是否可选中复制）。行 Column 用`if (selectable) SelectionContainer { Column { ... } } else Column { ... }` 包裹（import androidx.compose.foundation.text.selection.SelectionContainer）。将其158-240 行Column 内容提取为本文件`val linesContent = @Composable { ... }` 避免重复，两种分支均调用 `linesContent()`。- CopyButton 计时修复（连击重置）。
```kotlin
    var copyCount by remember { mutableStateOf(0) }
    val copied = copyCount > 0
    LaunchedEffect(copyCount) {
        if (copyCount > 0) {
            delay(2000)
            copyCount = 0
        }
    }
    // clickable 内：复制后copyCount++
```

- 剪贴板API 迁移：`LocalClipboardManager`/`clipboardManager.setText` 迁移至`val clipboard = LocalClipboard.current` + `clipboard.setClipEntry(ClipEntry(AnnotatedString(code)))`（compose.ui 1.9 多平台API）；若该构造方法在当前版本签名不同（编译器会指出），按提示改为等价形式（见`ClipEntry.of(...)`），保持删除两处 `@Suppress("DEPRECATION")`。import `androidx.compose.ui.platform.LocalClipboard`、`androidx.compose.ui.platform.ClipEntry`。
- [ ] **Step 6: REGRESS 通过（render jvmTest + preview 相关编译）*

`.\gradlew.bat :codehighlight-render:jvmTest :codehighlight-preview:compileKotlinJvm --console=plain`

- [ ] **Step 7: Commit** `feat: CodeBlock 异步增量渲染、onTokenClick 点击分发、布局无障碍与选择支持`

---

### Task 11: i18n 注入 + StreamingCursor + InlineCodeSize 单位

**Files:**
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/i18n/Strings.kt`
- Modify/删除: `codehighlight-render/src/*Main/kotlin/com/hrm/codehigh/i18n/PlatformLocale.kt`（ 3 个actual）- Create: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/i18n/LocalCodeBlockStrings.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/CodeBlock.kt`（消费注入）
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/StreamingCursor.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/InlineCodeMeasurer.kt`
- Modify: preview 模块引用点（grep `measureInlineCodeSize|InlineCodeSize|widthDp|heightDp` 定位。
- Modify: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/renderer/InlineCodeStyleTest.kt`

- [ ] **Step 1: Strings 鍙敞鍏?*

`Strings.kt` 重写：
```kotlin
package com.hrm.codehigh.i18n

/**
 * 代码块内置文案（收起/展开/复制等）注入接口。 * 宿主可实现后通过 [LocalCodeBlockStrings] 覆盖默认文案。 */
fun interface CodeBlockStrings {
    fun collapse(): String
    fun expand(hiddenLines: Int): String
    fun copy(): String
    fun copied(): String
}

/** 默认文案：跟随系统语言（中、英文），语言检测在进程内仅执行一次*/
internal object DefaultCodeBlockStrings : CodeBlockStrings {
    private val languageCode: String by lazy {
        try {
            platformLanguageTag().substringBefore('-').ifBlank { "en" }.lowercase()
        } catch (_: Exception) {
            "en"
        }
    }
    private val isChinese: Boolean get() = languageCode.startsWith("zh")

    override fun collapse(): String = if (isChinese) "鈻?鏀惰捣" else "鈻?Collapse"
    override fun expand(hiddenLines: Int): String {
        val n = maxOf(0, hiddenLines)
        return if (isChinese) "鈻?灞曞紑 ($n 琛?" else "鈻?Expand ($n ${if (n == 1) "line" else "lines"})"
    }
    override fun copy(): String = if (isChinese) "复制" else "Copy"
    override fun copied(): String = if (isChinese) "宸插鍒? else "Copied"
}

/** 平台语言标签（BCP-47，如 zh-Hans-CN），仅取主子标签做语言判断 */
internal expect fun platformLanguageTag(): String
```

（删除LocaleInfo、lineNumber() 死代码与旧Strings object。）
新文件：

```kotlin
package com.hrm.codehigh.i18n

import androidx.compose.runtime.compositionLocalOf

/** 宿主注入自定义代码块文案的入口*/
val LocalCodeBlockStrings = compositionLocalOf<CodeBlockStrings> { DefaultCodeBlockStrings }
```

5 个PlatformLocale.kt actual 全部替换为：

```kotlin
package com.hrm.codehigh.i18n
// jvmMain / androidMain: internal actual fun platformLanguageTag(): String = java.util.Locale.getDefault().toLanguageTag()
```

```kotlin
// jsMain:package com.hrm.codehigh.i18n
internal actual fun platformLanguageTag(): String = js("navigator.language || 'en'") as String? ?: "en"
```

```kotlin
// wasmJsMain:package com.hrm.codehigh.i18n
import kotlinx.browser.window
internal actual fun platformLanguageTag(): String = window.navigator.language
```

```kotlin
// iosMain:package com.hrm.codehigh.i18n
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLocalizations
internal actual fun platformLanguageTag(): String {
    // 优先 App 级首选本地化，回退系统区域
    val preferred = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String
    return preferred ?: NSLocale.currentLocale.languageCode ?: "en"
}
```

CodeBlock 内：`val strings = LocalCodeBlockStrings.current`，折叠按钮`Strings.collapse()`→`strings.collapse()` 等；CopyButton 增加 `strings: CodeBlockStrings` 参数。js 的`as String?` 写法若编译器报错（js() 返回动态类型），改为`js("window.navigator.language || 'en'") as String`（跟随编译器提示，两者择一编译通过者）。
- [ ] **Step 2: StreamingCursor 动画下沉 + 高度自适应**

```kotlin
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
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "cursorAlpha",
    )
    Box(
        modifier = modifier
            .width(2.dp)
            .height(cursorHeight)
            .graphicsLayer { this.alpha = alpha } // draw 阶段读值，避免每帧重组
            .background(color)
    )
}
```

import 变更：`androidx.compose.ui.graphics.graphicsLayer`、`androidx.compose.ui.unit.Dp`；`color.copy(alpha=...)` 改为：`color`。CodeBlock 调用处传 `cursorHeight = codeLineHeightDp * 0.8f`。
- [ ] **Step 3: InlineCodeSize 单位明确（破坏性重命名）*

```kotlin
data class InlineCodeSize(
    /** 宽度（像素） */
    val widthPx: Float,
    /** 高度（像素） */
    val heightPx: Float,
) {
    fun width(density: Density): Dp = with(density) { widthPx.toDp() }
    fun height(density: Density): Dp = with(density) { heightPx.toDp() }
}
```

`measureInlineCodeSize` 的`maxWidth` 参数重命名`maxWidthPx: Float = Float.POSITIVE_INFINITY`，KDoc 注明像素单位。grep 全仓库`widthDp(|heightDp(|InlineCodeSize(|measureInlineCodeSize(` 更新调用点（preview 模块内），`x.widthDp(density)` →`x.width(density)`。
- [ ] **Step 4: InlineCodeStyleTest 改相对断言**

删除常量回声断言，改写为：
```kotlin
    @Test
    fun should_adaptColors_when_themeBrightnessDiffers() {
        val dark = InlineCodeDefaults.style(OneDarkProTheme)
        val light = InlineCodeDefaults.style(GithubLightTheme)
        assertTrue(dark.isDarkStyle() && !light.isDarkStyle())
        assertNotEquals(dark.containerColor, light.containerColor)
        assertNotEquals(dark.borderColor, light.borderColor)
        assertTrue(dark.borderWidth > 0.dp && light.borderWidth > 0.dp)
    }

    private fun InlineCodeStyle.isDarkStyle() = theme.isDark
```

（保留原第1个 用例的theme 派生"断言：textStyle.color == theme.colorFor(PLAIN)；fontSize/lineHeight 锚点 13.sp/20.sp 保留。）import assertNotEquals/assertTrue。
- [ ] **Step 5: REGRESS + preview 编译通过**

- [ ] **Step 6: Commit** `feat: 文案可注入与平台语言标签简化；光标动画下沉 draw 层；测量 API 单位显式化`

---

### Task 12: 构建工程收敛

**Files:**
- Modify: 鏍?`build.gradle.kts`銆乣codehighlight-parser/build.gradle.kts`銆乣codehighlight-render/build.gradle.kts`銆乣gradle/libs.versions.toml`銆乣gradle.properties`

- [ ] **Step 1: 版本收敛version catalog，2.0.0**

`libs.versions.toml` `[versions]` 追加 `codehigh = "2.0.0"`；`gradle.properties` 删除 `VERSION=1.1.2`；两个库模块 `rootProject.property("VERSION").toString()` ；`libs.versions.codehigh.get()`。
- [ ] **Step 2: POM 公共块收敛到根构建*

根`build.gradle.kts` 追加：
```kotlin
import com.vanniktech.maven.publish.MavenPublishBaseExtension

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        configure<MavenPublishBaseExtension> {
            publishToMavenCentral(true)
            signAllPublications()
            pom {
                inceptionYear.set("2026")
                url.set("https://github.com/zusrsoft/codehigh")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("zusrsoft")
                        name.set("zusrsoft")
                        url.set("https://github.com/zusrsoft/")
                    }
                }
                scm {
                    url.set("https://github.com/zusrsoft/codehigh")
                    connection.set("scm:git:git://github.com/zusrsoft/codehigh.git")
                    developerConnection.set("scm:git:ssh://git@github.com/zusrsoft/codehigh.git")
                }
            }
        }
    }
}
```

两个库模块的 mavenPublishing 块删至仅留`coordinates(...)` + `pom { name.set / description.set }`（coordinates 、Step 1 （toml 引用）。
- [ ] **Step 3: 库模块去除应用级产物**

parser/render 两模块删除：iOS `binaries.framework { ... }` 整块（6-44 / 36-44 区域）与 js/wasmJs 与`binaries.executable()`（共 4 处）。preview/composeApp 不动（可运行 demo 需要）。
- [ ] **Step 4: explicitApiWarning**

两个库模块`kotlin {` 块首行加 `explicitApiWarning()`（渐进迁移：新改动即时报错提醒，存量不破坏构建）。
- [ ] **Step 5: 验证**

`.\gradlew.bat :codehighlight-parser:assemble :codehighlight-render:assemble --console=plain`（确认POM/坐标配置合法、configuration-cache 不报错）。
- [ ] **Step 6: Commit** `chore: 版本收敛 version catalog，2.0.0）、POM 公共块上移、库模块去应用产物、开启explicitApiWarning`

---

### Task 13: CI + 文档修正 + 烟囱测试

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `HIGHLIGHTER_COVERAGE_ANALYSIS.md`、`README.md`
- Create: `codehighlight-preview/src/commonTest/kotlin/com/hrm/codehigh/preview/SampleCodeSmokeTest.kt`（目录不存在则创建，preview build.gradle.kts 需确认 commonTest 依赖 kotlin-test，若无则补`sourceSets { commonTest.dependencies { implementation(libs.kotlin.test) } }`。- Delete: `composeApp/src/commonTest/kotlin/com/hrm/codehigh/ComposeAppCommonTest.kt`

- [ ] **Step 1: CI 宸ヤ綔娴?*

```yaml
name: CI
on:
  push:
    branches: [main, master]
  pull_request:

jobs:
  test:
    name: JVM / JS / Wasm / Android (ubuntu)
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: 21
      - name: Unit tests (JVM)
        run: ./gradlew jvmTest --console=plain
      - name: Unit tests (JS browser)
        run: ./gradlew jsBrowserTest --console=plain
      - name: Unit tests (Wasm browser)
        run: ./gradlew wasmJsBrowserTest --console=plain
      - name: Android demo build (regresses consumer-rules)
        run: ./gradlew :androidApp:assembleDebug --console=plain

  ios:
    name: iOS (macos)
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'zulu'
          java-version: 21
      - name: iOS tests
        run: ./gradlew :codehighlight-parser:iosSimulatorArm64Test :codehighlight-render:iosSimulatorArm64Test --console=plain
      - name: iOS framework link check
        run: ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 --console=plain
```

本地验证任务名存在：`.\gradlew.bat :codehighlight-parser:tasks --all | Select-String "iosSimulatorArm64Test|jsBrowserTest|wasmJsBrowserTest"`（iOS 任务本地 Windows 不可执行，仅确认任务名存在于任务列表——不存在则改为`:codehighlight-parser:compileKotlinIosSimulatorArm64` 等编译任务）。
- [ ] **Step 2: preview 烟囱测试**

```kotlin
package com.hrm.codehigh.preview

import com.hrm.codehigh.lexer.LanguageRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 全部预览样例可被对应词法器完整解析（字符全覆盖不变式。*/
class SampleCodeSmokeTest {
    @Test
    fun should_tokenizeAllSamples_withFullCoverage() {
        val samples: List<Pair<String, String>> = listOf(
            "kotlin" to SampleCode.kotlin,
            "python" to SampleCode.python,
            // 其余 26 个语言常量逐一加入（打开 SampleCode.kt 按常量名补全。        )
        assertTrue(samples.size >= 28, "应覆盖全部预览样例")
        for ((lang, code) in samples) {
            val tokens = LanguageRegistry.getOrPlain(lang).tokenize(code)
            assertEquals(code, tokens.joinToString("") { it.text }, "lang=$lang 字符全覆盖破坏")
        }
    }
}
```

（执行时打开 `codehighlight-preview/src/commonMain/kotlin/com/hrm/codehigh/preview/data/SampleCode.kt`，按实际常量名补全28 项清单；若个别常量名非语言名，用注释标注映射。）删除 composeApp 占位测试文件。
- [ ] **Step 3: 文档修正**

HIGHLIGHTER_COVERAGE_ANALYSIS.md（按行号定位，逐项更正）：
- :366 `IncrementalHighlighter` 描述 internal →`public`，parser 层流式解析接口，公开"）；
- :373 删除 `AstDiffEngine（internal）` 条目（全仓库不存在）。- :35 `CodeAst` internal →public，- :461-466 剪贴板描述改为基于 Compose `LocalClipboard`/`ClipEntry`（T10 迁移后现状）"。- :486-489 测试清单改为真实文件（parser：KotlinLexerTest、EscapeBoundsTest、LexerContractTest、LanguageCoverageTest、ExtendedLanguageSupportTest、IncrementalHighlighterTest；render：CodeLineRenderTest、HighlightedStringTest、InlineCodeStyleTest、CodeThemeTest；preview：SampleCodeSmokeTest），删除"Java/Python 等各语言词法分析器单元测试的虚构表述；
- :489/:523 `:code-high:jvmTest` →`:codehighlight-parser:jvmTest`。
- :538 依赖图为 parser →render →preview →composeApp/androidApp 三层结构。
- :423/:443 "18 种语言" →"28 种语言"。
README.md，- 徽章：Kotlin `2.3.20`、CMP `1.10.3`、minSdk 徽章链接 `api?level=23`，- grep `CodeToken(|InlineCodeSize(|maxVisibleLines` 同步 2.0.0 API 变更示例（CodeToken 构造第三参数source、maxVisibleLines 默认 500、InlineCodeSize 字段 widthPx/heightPx）；
- 测试命令章节确认 `./gradlew test` 表述与模块名一致。
- [ ] **Step 4: 验证** `.\gradlew.bat :codehighlight-preview:jvmTest --console=plain`

- [ ] **Step 5: Commit** `ci: 增加 PR/push 测试流水线；修正文档与代码不符；preview 样例烟囱测试`

---

### Task 14: 终验

- [ ] **Step 1: 全量 JVM 测试** `.\gradlew.bat jvmTest --console=plain`（全模块， 失败；
- [ ] **Step 2: 全target 编译** `.\gradlew.bat :codehighlight-parser:assemble :codehighlight-render:assemble :composeApp:assemble :androidApp:assembleDebug --console=plain`
- [ ] **Step 3: 明确已知限制并写入README（简短一节）**：手写声明式词法器双轨并存（后续收敛方向）；CodeBlock 内部仍为非懒加载 Column（已通过默认 500 行上限+ 折叠控制成本，LazyColumn 留待专项）。
- [ ] **Step 4: 终验 Commit（如有遗漏微调）** + 汇总报告（修复清单 vs 评审条目对照。
---

## 自查记录

- 覆盖对照：评审#1→T2；#2→T3/T4；#3→T6；#4→T3/T4/T5；#5→T3；#6→T1；#7→T4；#8/#9→T3；#10→T1；#11（双轨收敛）→用户决策跳过；#12→T4；#13→T3；#14→T3；#15→T5；#16→T3/T4；#17→T2；#18→T3；#19→T4（countLinesBefore 保留 O(n)，权衡记录）；#20→T10。render：S1→T10；S2→T10；S3→T10（异步，默认上限；LazyColumn 跳过）；S4→T8；S5→T10；M1→T10；M2→T9；M3→T8；M4→T11；M5→T11；M6→T8；M7→T10；M8→T10；L1→T11；L2→T11；L3→T11；L4→T11；L5→T11；L6→T11；L7→T9/T10。构建测试：#1→T13；#2→T13；#3→T7；#4→T7；#5→T4/T7；#6→T12；#7/8→T12；#9→T12；#10→T1（死代码删除，T7；#11/12→T8；#13→T7；#14→T13；#15/16→T13。
- 类型一致性：CodeToken(type, range, source) 全计划统一；UpdateResult -1 语义在T4 定义、T10 消费；CodeLineKind 在T8 迁移至theme 包，T9/T10 引用一致。