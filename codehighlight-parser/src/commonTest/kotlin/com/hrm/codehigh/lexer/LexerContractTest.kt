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
        assertFalse(KotlinLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 12, "/* closed */")))
    }

    @Test
    fun should_extend_when_unclosedTripleQuote() {
        assertTrue(KotlinLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 10, "\"\"\"abc\ndef")))
    }

    @Test
    fun should_notExtend_when_closedString() {
        assertFalse(KotlinLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 4, "\"ab\"")))
    }

    @Test
    fun should_extend_when_bareTripleQuoteOpener() {
        assertTrue(KotlinLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 3, "\"\"\"")))
        assertTrue(KotlinLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 4, "\"\"\"\"")))
    }

    @Test
    fun should_notExtend_when_closedEmptyTripleQuote() {
        assertFalse(KotlinLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 6, "\"\"\"\"\"\"")))
    }

    @Test
    fun should_notExtend_when_prefixedTripleQuoteClosed() {
        assertFalse(PythonLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 8, "f\"\"\"x\"\"\"")))
    }

    @Test
    fun should_extend_when_prefixedTripleQuoteOpener() {
        assertTrue(PythonLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 4, "f\"\"\"")))
        assertTrue(PythonLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 9, "f\"\"\"abc\"\"")))
    }

    @Test
    fun should_extend_when_unclosedPrefixedString() {
        val tokens = PythonLexer.tokenize("x = f\"abc")
        assertTrue(PythonLexer.isExtendableToken(tokens.last { it.type == TokenType.STRING }))
    }

    @Test
    fun should_notExtend_when_closedPrefixedString() {
        val tokens = PythonLexer.tokenize("x = f\"abc\"")
        assertFalse(PythonLexer.isExtendableToken(tokens.last { it.type == TokenType.STRING }))
    }

    @Test
    fun should_extend_when_luaUnclosedLongString() {
        assertTrue(LuaLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 5, "[[abc")))
        assertTrue(LuaLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 2, "[[")))
    }

    @Test
    fun should_notExtend_when_luaClosedLongString() {
        assertFalse(LuaLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 4, "[[]]")))
        assertFalse(LuaLexer.isExtendableToken(CodeToken(TokenType.STRING, 0 until 7, "[[abc]]")))
    }

    @Test
    fun should_extend_when_luaUnclosedBlockComment() {
        val tokens = LuaLexer.tokenize("--[[ unclosed")
        assertTrue(LuaLexer.isExtendableToken(tokens.last { it.type == TokenType.COMMENT }))
    }

    @Test
    fun should_notExtend_when_luaClosedBlockComment() {
        val tokens = LuaLexer.tokenize("--[[ closed ]]")
        assertFalse(LuaLexer.isExtendableToken(tokens.last { it.type == TokenType.COMMENT }))
    }

    @Test
    fun should_extend_when_haskellUnclosedBlockComment() {
        assertTrue(HaskellLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 6, "{- abc")))
        assertTrue(HaskellLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 2, "{-")))
        assertTrue(HaskellLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 3, "{-}")))
    }

    @Test
    fun should_notExtend_when_haskellClosedBlockComment() {
        assertFalse(HaskellLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 9, "{- abc -}")))
        assertFalse(HaskellLexer.isExtendableToken(CodeToken(TokenType.COMMENT, 0 until 4, "{--}")))
    }

    @Test
    fun should_notExtend_when_haskellLineComment() {
        val tokens = HaskellLexer.tokenize("-- line comment")
        assertFalse(HaskellLexer.isExtendableToken(tokens.last { it.type == TokenType.COMMENT }))
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
        assertEquals("f\"\"\"hello {name}\"\"\"", tokens.first { it.type == TokenType.STRING }.text)
    }

    @Test
    fun should_tokenizeCombinedPrefix_when_pythonBytes() {
        val tokens = PythonLexer.tokenize("data = rb'raw\\x00'")
        assertEquals("rb'raw\\x00'", tokens.first { it.type == TokenType.STRING }.text)
    }

    @Test
    fun should_keepIdentifier_when_prefixLetterFollowedByNonQuote() {
        val tokens = PythonLexer.tokenize("fruits = 1")
        assertTrue(tokens.any { it.type == TokenType.IDENTIFIER && it.text == "fruits" })
    }
}
