package com.hrm.codehigh.stream

import com.hrm.codehigh.ast.TokenType
import com.hrm.codehigh.lexer.YamlLexer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IncrementalHighlighterTest {

    private val streamingSample = """
        package com.example

        import androidx.compose.runtime.Composable
        import androidx.compose.material3.Text

        /**
         * 示例 Kotlin 代码，展示主要 Token 类型。
         */
        @Composable
        fun Greeting(name: String) {
            // 单行注释
            val message = "Hello, ${'$'}{name}!"
            val count: Int = 42
            val hex = 0xFF
            println(message)
        }
    """.trimIndent()

    @Test
    fun should_returnAst_when_firstParse() {
        val highlighter = IncrementalHighlighter()
        val ast = highlighter.update("fun hello() {}", "kotlin")
        assertNotNull(ast)
        assertTrue(ast.tokens.isNotEmpty())
        assertEquals("kotlin", ast.language)
    }

    @Test
    fun should_returnCachedAst_when_sameCodeAndLanguage() {
        val highlighter = IncrementalHighlighter()
        val code = "fun hello() {}"
        val ast1 = highlighter.update(code, "kotlin")
        val ast2 = highlighter.update(code, "kotlin")
        // 相同代码应返回缓存结果（同一对象）
        assertEquals(ast1, ast2)
    }

    @Test
    fun should_fullReparse_when_languageChanges() {
        val highlighter = IncrementalHighlighter()
        val code = "def hello(): pass"
        val ast1 = highlighter.update(code, "python")
        val ast2 = highlighter.update(code, "kotlin")
        // 语言变化时应重新解析
        assertEquals("python", ast1.language)
        assertEquals("kotlin", ast2.language)
    }

    @Test
    fun should_incrementalUpdate_when_codeAppended() {
        val highlighter = IncrementalHighlighter()
        val code1 = "fun hello() {"
        val code2 = "fun hello() {\n    println(\"world\")\n}"
        highlighter.update(code1, "kotlin")
        val ast2 = highlighter.update(code2, "kotlin")
        assertNotNull(ast2)
        assertEquals(code2, ast2.source)
    }

    @Test
    fun should_reclassifyKeyword_when_identifierCompletesDuringStreaming() {
        val highlighter = IncrementalHighlighter()

        highlighter.update("fu", "kotlin")
        val ast = highlighter.update("fun", "kotlin")

        assertTrue(ast.tokens.any { it.text == "fun" && it.type == TokenType.KEYWORD })
    }

    @Test
    fun should_reclassifyFunction_when_parenthesisAppendedDuringStreaming() {
        val highlighter = IncrementalHighlighter()

        highlighter.update("println", "kotlin")
        val ast = highlighter.update("println(", "kotlin")

        assertTrue(ast.tokens.any { it.text == "println" && it.type == TokenType.FUNCTION })
    }

    @Test
    fun should_reparseFromMultilineTokenStart_when_commentClosesOnLaterLine() {
        val highlighter = IncrementalHighlighter()

        highlighter.update("/*\nhello", "kotlin")
        val ast = highlighter.update("/*\nhello\n*/\nfun test() {}", "kotlin")

        assertTrue(ast.tokens.any { it.text == "/*\nhello\n*/" && it.type == TokenType.COMMENT })
        assertTrue(ast.tokens.any { it.text == "fun" && it.type == TokenType.KEYWORD })
    }

    @Test
    fun should_coverAllCharacters_when_tokenizing() {
        val highlighter = IncrementalHighlighter()
        val code = "fun hello() {\n    val x = 42\n}"
        val ast = highlighter.update(code, "kotlin")
        val reconstructed = ast.tokens.joinToString("") { it.text }
        assertEquals(code, reconstructed)
    }

    @Test
    fun should_coverAllCharacters_during_characterByCharacterStreaming() {
        val highlighter = IncrementalHighlighter()

        for (index in streamingSample.indices) {
            val partialCode = streamingSample.substring(0, index + 1)
            val ast = highlighter.update(partialCode, "kotlin")
            val reconstructed = ast.tokens.joinToString("") { it.text }
            assertEquals(
                partialCode,
                reconstructed,
                "Token reconstruction mismatch at length ${index + 1}"
            )
        }
    }

    @Test
    fun should_handleEmptyCode_when_emptyInput() {
        val highlighter = IncrementalHighlighter()
        val ast = highlighter.update("", "kotlin")
        assertNotNull(ast)
        assertTrue(ast.tokens.isEmpty())
    }

    @Test
    fun should_invalidateCache_when_invalidateCalled() {
        val highlighter = IncrementalHighlighter()
        val code = "fun hello() {}"
        val ast1 = highlighter.update(code, "kotlin")
        highlighter.invalidate()
        val ast2 = highlighter.update(code, "kotlin")
        // 缓存清除后重新解析，结果应相同但不是同一对象
        assertEquals(ast1.source, ast2.source)
        assertEquals(ast1.language, ast2.language)
    }

    @Test
    fun should_notTreatDirtySubstringStartAsLineStart_when_yamlIncremental() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("a--", "yaml")
        val ast = highlighter.update("a---", "yaml")
        // 行首敏感的 --- 文档分隔符判定必须使用全文上下文，dirty 子串起始处不是行首
        assertTrue(
            ast.tokens.none { it.type == TokenType.KEYWORD && it.text == "---" },
            "增量重解析不应把子串起点误判为行首：tokens=${ast.tokens}"
        )
    }

    @Test
    fun should_notTreatDirtySubstringStartAsLineStart_when_yamlAppendsDocSeparatorMidLine() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("a -", "yaml")
        val ast = highlighter.update("a ---", "yaml")
        // dirty 子串 "---" 的全文位置在 "a " 之后，不是行首，不应识别为文档分隔符
        assertTrue(
            ast.tokens.none { it.type == TokenType.KEYWORD && it.text == "---" },
            "非行首的 --- 不应因增量重解析被误判为文档分隔符：tokens=${ast.tokens}"
        )
    }

    @Test
    fun should_notTreatDirtySubstringStartAsLineStart_when_reparseOffsetExceedsDirtyLength() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("key: -", "yaml")
        val ast = highlighter.update("key: ---", "yaml")
        // startOffset(5) 大于 dirty 子串长度(3)时行首判定不得越界，且非行首 --- 不是分隔符
        assertTrue(
            ast.tokens.none { it.type == TokenType.KEYWORD && it.text == "---" },
            "子串索引越界或误判文档分隔符：tokens=${ast.tokens}"
        )
    }

    @Test
    fun should_recognizeDocSeparator_when_yamlAppendedAtRealLineStart() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("k: v\n", "yaml")
        val ast = highlighter.update("k: v\n---", "yaml")
        // 全文中 --- 前是 \n，是真正的行首，增量结果应与全量一致：识别为文档分隔符
        assertTrue(
            ast.tokens.any { it.type == TokenType.KEYWORD && it.text == "---" },
            "真行首的 --- 应识别为文档分隔符：tokens=${ast.tokens}"
        )
    }

    @Test
    fun should_tokenizeTildeAsBuiltinNull_when_yaml() {
        val highlighter = IncrementalHighlighter()
        val ast = highlighter.update("k: ~", "yaml")
        // nullValues 含 ~，此前落入逐字符 PLAIN 兜底（死配置），应命中 BUILTIN
        assertTrue(
            ast.tokens.any { it.type == TokenType.BUILTIN && it.text == "~" },
            "~ 应识别为 null 值 BUILTIN：tokens=${ast.tokens}"
        )
    }

    @Test
    fun should_returnNoChange_when_cacheHit() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("fun a() {}", "kotlin")
        val result = highlighter.updateDetailed("fun a() {}", "kotlin")
        assertEquals(-1, result.firstChangedLine)
        assertEquals(-1, result.reparseStart)
        assertFalse(result.hasChange)
    }

    @Test
    fun should_reparseFromUnfinishedTokenStart_when_lookbackExceeded() {
        val highlighter = IncrementalHighlighter()
        // 前置内容 + 超过 64 字符回看窗口的未闭合注释（注释起点 11，非 0）
        val prefix = "val a = 1\n"
        val longComment = prefix + "/* " + "x".repeat(200)
        highlighter.update(longComment, "kotlin")
        val result = highlighter.updateDetailed(longComment + "\n*/\nval x = 1", "kotlin")
        assertEquals(prefix.length, result.reparseStart, "未闭合注释应从其起点重解析而非全量回退到 0")
        assertEquals(1, result.firstChangedLine)
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
        assertTrue(result.hasChange)
    }

    @Test
    fun should_recognizeDocSeparator_when_yamlStreamedCharByCharAtLineStart() {
        val highlighter = IncrementalHighlighter()
        // 逐字符流式构建第二行的 ---
        var code = "a\n"
        highlighter.update(code, "yaml")
        for (ch in listOf("-", "-", "-")) {
            code += ch
            val ast = highlighter.update(code, "yaml")
            assertEquals(code, ast.tokens.joinToString("") { it.text }, "len=${code.length}")
        }
        // 最终态：增量结果与全量词法一致
        val incremental = highlighter.update("a\n---", "yaml")
        val reference = IncrementalHighlighter().update("a\n---", "yaml")
        assertEquals(
            reference.tokens.joinToString("|") { "${it.type}:${it.text}" },
            incremental.tokens.joinToString("|") { "${it.type}:${it.text}" },
        )
        // 词法器直测：全量 a\n--- 中 --- 是行首文档分隔符
        assertTrue(
            YamlLexer.tokenize("a\n---").any { it.type == TokenType.KEYWORD && it.text == "---" },
        )
    }

    @Test
    fun should_recognizeDocSeparator_when_yamlJumpAppendAtLineStart() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("a\n-", "yaml")
        val ast = highlighter.update("a\n----", "yaml")
        // 增量与全量词法一致：--- 分隔符 + 多余 -
        val full = IncrementalHighlighter().update("a\n----", "yaml")
        assertEquals(
            full.tokens.joinToString("|") { "${it.type}:${it.text}" },
            ast.tokens.joinToString("|") { "${it.type}:${it.text}" },
        )
    }

    @Test
    fun should_keepLineStartContext_when_reparseStartAtTrueLineStart() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("k: v\n", "yaml")
        val ast = highlighter.update("k: v\nrest: 1", "yaml")
        val full = IncrementalHighlighter().update("k: v\nrest: 1", "yaml")
        assertEquals(
            full.tokens.joinToString("|") { "${it.type}:${it.text}" },
            ast.tokens.joinToString("|") { "${it.type}:${it.text}" },
        )
    }
}
