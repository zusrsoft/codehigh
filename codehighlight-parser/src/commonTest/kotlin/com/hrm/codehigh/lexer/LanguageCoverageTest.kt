package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LanguageCoverageTest {

    /** 每语言最小样本：含注释、字符串、关键字/指令形态 */
    private val samples: Map<String, String> = mapOf(
        "kotlin" to "fun main() { /* c */ val s = \"hi\" }",
        "java" to "class A { // c\nString s = \"hi\"; }",
        "swift" to "func f() { /* c */ let s = \"hi\" }",
        "python" to "def f():\n    # c\n    s = \"hi\"",
        "javascript" to "function f() { // c\nconst s = \"hi\"; }",
        "typescript" to "function f(): string { /* c */ return \"hi\"; }",
        "ruby" to "=begin\ndoc\n=end\ndef f\n  s = \"hi\"\nend",
        "php" to "<?php\n// c\n\$s = \"hi\";",
        "dart" to "void f() { // c\nvar s = 'hi'; }",
        "scala" to "def f(): Unit = { /* c */ val s = \"hi\" }",
        "lua" to "-- c\nfunction f()\n  local s = \"hi\"\nend",
        "haskell" to "-- c\n{- block -}\nf :: String\nf = \"hi\"",
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
            assertEquals(sample, tokens.joinToString("") { it.text }, "lang=$lang 重构不变式破坏")
        }
    }

    @Test
    fun should_haveCommentAndStringTokens_when_allLanguagesTokenized() {
        for ((lang, sample) in samples) {
            val tokens = LanguageRegistry.getOrPlain(lang).tokenize(sample)
            if (lang == "diff") {
                // diff 为行式词法器，语义上无注释/字符串定界概念（非词法器缺口），
                // 改为断言 diff 特有形态：文件头 ANNOTATION 与 hunk 头 FUNCTION
                assertTrue(tokens.any { it.type == TokenType.ANNOTATION }, "lang=$lang 应识别文件头")
                assertTrue(tokens.any { it.type == TokenType.FUNCTION }, "lang=$lang 应识别 hunk 头")
                continue
            }
            if (lang != "json") {
                assertTrue(tokens.any { it.type == TokenType.COMMENT }, "lang=$lang 应识别注释")
            }
            assertTrue(
                tokens.any { it.type == TokenType.STRING || it.type == TokenType.COMMENT },
                "lang=$lang 应识别字符串或注释",
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
        val unclosed = listOf("/* ", "\"", "'", "\"\"\"", "#", "<a href=", "{ \"k\": ")
        for (lang in samples.keys) {
            for (snippet in unclosed) {
                LanguageRegistry.getOrPlain(lang).tokenize(snippet) // 不抛异常即通过
            }
        }
    }
}
