package com.hrm.codehigh.preview

import com.hrm.codehigh.lexer.LanguageRegistry
import com.hrm.codehigh.preview.data.SampleCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 全部预览样例可被对应词法器完整解析（字符全覆盖不变式） */
class SampleCodeSmokeTest {
    @Test
    fun should_tokenizeAllSamples_withFullCoverage() {
        val samples: List<Pair<String, String>> = listOf(
            "kotlin" to SampleCode.KOTLIN,
            "java" to SampleCode.JAVA,
            "python" to SampleCode.PYTHON,
            "javascript" to SampleCode.JAVASCRIPT,
            "typescript" to SampleCode.TYPESCRIPT,
            "go" to SampleCode.GO,
            "rust" to SampleCode.RUST,
            "c" to SampleCode.C,
            "cpp" to SampleCode.CPP,
            "swift" to SampleCode.SWIFT,
            "sql" to SampleCode.SQL,
            "json" to SampleCode.JSON,
            "yaml" to SampleCode.YAML,
            "bash" to SampleCode.BASH,
            "ruby" to SampleCode.RUBY,
            "php" to SampleCode.PHP,
            "dart" to SampleCode.DART,
            "scala" to SampleCode.SCALA,
            "r" to SampleCode.R,
            "toml" to SampleCode.TOML,
            "dockerfile" to SampleCode.DOCKERFILE,
            "lua" to SampleCode.LUA,
            "haskell" to SampleCode.HASKELL,
            "elixir" to SampleCode.ELIXIR,
            "diff" to SampleCode.DIFF,
            "html" to SampleCode.HTML,
            "css" to SampleCode.CSS,
            "xml" to SampleCode.XML,
        )
        assertTrue(samples.size >= 28, "应覆盖全部预览样例")
        for ((lang, code) in samples) {
            val tokens = LanguageRegistry.getOrPlain(lang).tokenize(code)
            assertEquals(code, tokens.joinToString("") { it.text }, "lang=$lang 字符全覆盖破坏")
        }
    }
}
