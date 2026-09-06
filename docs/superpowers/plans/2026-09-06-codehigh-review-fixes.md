# CodeHigh 璇勫闂鍏ㄩ噺淇瀹炴柦璁″垝

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 淇娣卞害璇勫鍙戠幇鐨勫叏閮ㄥ叿浣撻棶棰橈細2 涓穿婧?閿欎贡绾х己闄枫€佹祦寮忔覆鏌撴€ц兘銆佸閲忓紩鎿庢纭€с€佹祴璇曚吉瑕嗙洊銆佹瀯寤哄伐绋嬪寲缂哄彛銆?
**Architecture:** parser 灞傚厛琛岋紙CodeToken API 鐮村潖鎬у彉鏇翠负 2.0.0锛夛紝Lexer 鎺ュ彛澧炲姞澧為噺鍗忓晢濂戠害锛坄tokenize(code, startOffset)` / `isExtendableToken`锛夛紝IncrementalHighlighter 娑堣垂璇ュ绾︼紱render 灞傞殢鍚庯紙寮傛瑙ｆ瀽銆乷nTokenClick 瀹炵幇銆佸閲忚烦杩囬噸寤恒€佺ǔ瀹氭€ф敞瑙ｏ級锛涙渶鍚庢瀯寤?CI/鏂囨。銆備袱涓ぇ閲嶆瀯锛堣瘝娉曞櫒澹版槑寮忔敹鏁涖€佸唴閮?LazyColumn锛夌粡鐢ㄦ埛鍐崇瓥鏄庣‘**璺宠繃**銆?
**Tech Stack:** Kotlin Multiplatform 2.3.20銆丆ompose Multiplatform 1.10.3銆丟radle 9.3.1銆丄GP 9.1.1銆乿anniktech 0.36銆?
**绾﹀畾锛?*
- 鍛戒护鍧囧湪浠撳簱鏍?`D:\dev-java-learn-2026-ai\huarangmeng-zusrsoft\codehigh` 涓嬩互 PowerShell 鎵ц锛実radle 鍛戒护缁熶竴鍐欎綔 `.\gradlew.bat <task>`銆?- 蹇€熷洖褰掑懡浠わ細`.\gradlew.bat :codehighlight-parser:jvmTest :codehighlight-render:jvmTest --console=plain`锛堜笅鏂囩畝绉?REGRESS锛夈€?- 鎻愪氦淇℃伅椋庢牸娌跨敤浠撳簱鐜扮姸锛堜腑鏂?+ `fix:`/`feat:`/`test:`/`chore:` 鍓嶇紑锛夈€?- 鐗堟湰鍙峰崌绾т负 **2.0.0**锛堢牬鍧忔€у彉鏇达細CodeToken 鏋勯€犵鍚嶃€両nlineCodeSize 瀛楁鍚嶏級锛屽湪 T12 钀藉湴銆?
---

### Task 1: CodeToken 鎯版€?text 閲嶆瀯锛堢牬鍧忔€?API 鍙樻洿锛?
**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/ast/CodeToken.kt`锛堝叏鏂囦欢鏇挎崲锛?- Modify: 鍏ㄩ儴璇嶆硶鍣ㄦ瀯閫犵偣锛堣姝ラ 3 娓呭崟涓庢浛鎹㈣鍒欙級
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/BaseLexer.kt`锛堝垹闄ゆ浠ｇ爜 TokenBuilder锛?- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/HighlightedString.kt:46-107`锛堝垹闄ゆ浠ｇ爜 buildLineRendersFromOffset/toRelativeTokens锛?- Modify: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/renderer/CodeLineRenderTest.kt`锛堟瀯閫犵偣杩佺Щ锛?
- [ ] **Step 1: 閲嶅啓 CodeToken.kt**

```kotlin
package com.hrm.codehigh.ast

/**
 * 浠ｇ爜 Token 鏁版嵁缁撴瀯锛岃〃绀鸿瘝娉曞垎鏋愬悗鐨勬渶灏忚涔夊崟鍏冦€? * 鎯版€ф寔鏈夋簮鏂囨湰寮曠敤锛屼粎鍦ㄨ闂?[text] 鏃跺垏鐗囷紝閬垮厤閫?Token 鍏ㄩ噺鎷疯礉婧愮爜銆? *
 * 绛夊€艰涔変粎姣旇緝 [type] 涓?[range]锛泃ext 闇€瑕佹椂鏄惧紡鏂█銆? *
 * @param type Token 绫诲瀷
 * @param range Token 鍦ㄦ簮鏂囨湰涓殑浣嶇疆鑼冨洿锛堢浉瀵?[source] 浠?0 璁★級
 * @param source 婧愭枃鏈紩鐢? */
class CodeToken(
    val type: TokenType,
    val range: IntRange,
    val source: CharSequence,
) {
    /** Token 鍘熷鏂囨湰锛屾儼鎬у垏鐗?*/
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

- [ ] **Step 2: 鍏ㄤ粨搴撴浛鎹㈡瀯閫犵偣锛? 绉嶅舰鎬侊級**

鐢ㄧ紪杈戝伐鍏锋垨 PowerShell 鎵归噺澶勭悊锛岃鍒欙紙`X`/`A`/`B` 涓轰换鎰忚〃杈惧紡锛夛細
1. `CodeToken(X, code.substring(A, B), A until B)` 鈫?`CodeToken(X, A until B, code)`锛堟敞鎰忥細substring 涓?until 鐨勪袱涓弬鏁伴€氬父鐩稿悓琛ㄨ揪寮忥紱鑻ヤ笉鍚岋紙濡?`start..end`锛夛紝瑙勫垯涓?`CodeToken(X, code.substring(A, B), C)` 鈫?`CodeToken(X, C, code)`锛?2. `CodeToken(type, word, start until pos)` 鈫?`CodeToken(type, start until pos, code)`锛坵ord 绫诲眬閮ㄥ彉閲忓舰鎬侊級
3. `CodeToken(TokenType.PLAIN, c.toString(), pos until pos + 1)` 鈫?`CodeToken(TokenType.PLAIN, pos until pos + 1, code)`

娑夊強鏂囦欢锛坓rep `CodeToken(` 楠岃瘉鏃犻仐婕忥級锛歚lexer/` 鐩綍鍏ㄩ儴 24 涓?.kt锛圞otlinLexer銆丳ythonLexer銆丣avaLexer銆丣avaScriptLexer銆乀ypeScriptLexer銆丟oLexer銆丷ustLexer銆丼wiftLexer銆丆Lexer銆丆ssLexer銆丣sonLexer銆乊amlLexer銆乀omlLexer銆丼qlLexer銆乆mlLexer锛堝惈 HtmlLexer锛夈€丅ashLexer銆丏iffLexer銆丏artLexer銆丼calaLexer銆丩uaLexer銆丠askellLexer銆丒lixirLexer銆丷LangLexer銆丳hpLexer锛夈€乣lexer/ConfigurableLexer.kt`銆乣lexer/BaseLexer.kt`锛圱okenBuilder 鍐呴儴鈥斺€旇鏂囦欢鏈换鍔″皢鍒犻櫎澶ч儴鍒嗭級銆乣lexer/LanguageRegistry.kt` 鏃犳瀯閫犵偣銆乣stream/IncrementalHighlighter.kt:104-110`锛坉irtyTokens map鈥斺€旀敼涓?`CodeToken(token.type, (token.range.first + reparseStart)..(token.range.last + reparseStart), newCode)`锛屼笉鍐嶉渶瑕?text 浼犻€掞級銆?娉ㄦ剰锛氬悇璇嶆硶鍣ㄦ枃浠跺唴灞€閮ㄥ彉閲忓悕鍙兘鏄?`code` 浠ュ鐨勫悕瀛楋紙濡?`input`锛夛紝鏇挎崲鏃朵繚鎸佹簮鍙橀噺鍚嶄竴鑷淬€?
- [ ] **Step 3: 鍒犻櫎姝讳唬鐮?*

`BaseLexer.kt` 鍏ㄦ枃浠舵浛鎹负锛?
```kotlin
package com.hrm.codehigh.lexer

/**
 * 璇嶆硶鍒嗘瀽鍣ㄥ熀纭€宸ュ叿绫汇€? * 鏍囪涓?internal锛屼粎渚涙ā鍧楀唴閮ㄤ娇鐢ㄣ€? */
internal abstract class BaseLexer : Lexer
```

`HighlightedString.kt`锛氬垹闄?`buildLineRendersFromOffset`锛?6-86 琛岋級涓?`toRelativeTokens`锛?8-107 琛岋級涓や釜鍑芥暟锛宍buildLineRenders` 鍘熸湁鍙傛暟涓嶅彉銆佸嚱鏁颁綋鏀逛负鐩存帴鎵ц鍘?FromOffset 鐗堟湰鍦?startLineIndex=0/startCharOffset=0 鏃剁殑閫昏緫锛堝嵆锛氶亶鍘?sourceLines銆佸 buildHighlightedString 浜х墿鎸夎鍒囩墖銆乺esolveLineKind锛夈€?
- [ ] **Step 4: 杩佺Щ CodeLineRenderTest 鏋勯€犵偣**

灏?`CodeToken(TokenType.KEYWORD, "fun", 0 until 3)` 绛夋敼涓?`CodeToken(TokenType.KEYWORD, 0 until 3, src)`锛屽苟鍦ㄦ祴璇曟柟娉曞紑澶村畾涔?`val src = "fun hello()\nprintln(\"ok\")\nreturn"`锛? 琛屾簮鏂囨湰鎷兼帴锛屽悇 token 鐨?text 鍗充负鍏跺垏鐗囷級銆傛柇瑷€涓嶅彉锛坄.text` 浠嶅彲鐢級銆?
- [ ] **Step 5: REGRESS 楠岃瘉閫氳繃**

- [ ] **Step 6: Commit** `feat: CodeToken 鎯版€?text 閲嶆瀯锛屾秷闄ら€?Token 婧愮爜鎷疯礉锛?.0.0 鐮村潖鎬у彉鏇达級`

---

### Task 2: 璇嶆硶鍣ㄨ浆涔夎秺鐣屼慨澶?+ 绌虹櫧鍚堝苟 + 鍙傛暟鍖栧洖褰掓祴璇?
**Files:**
- Create: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/lexer/EscapeBoundsTest.kt`
- Modify: 14 涓瘝娉曞櫒鏂囦欢鐨勮浆涔夎锛堟楠?2 娓呭崟锛変笌 PLAIN 鍏滃簳鍒嗘敮
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/BaseLexer.kt`锛堝姞 whitespaceEnd锛?
- [ ] **Step 1: 鍏堝啓宕╂簝鍥炲綊娴嬭瘯锛堝綋鍓嶅簲澶辫触锛?*

```kotlin
package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EscapeBoundsTest {

    /** 鍙嶆枩鏉犵粨灏剧殑鏈棴鍚堝瓧绗︿覆锛氬巻鍙?bug 涓?IndexOutOfBoundsException */
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
                // 鍏ㄥ瓧绗﹁鐩栦笉鍙樺紡
                assertEquals(case, tokens.joinToString("") { it.text }, "lang=$lang case=${case.replace("\\", "\\\\")}")
            }
        }
    }

    @Test
    fun should_mergeWhitespace_when_plainFallbackRuns() {
        val tokens = KotlinLexer.tokenize("fun a() {}\n\n\nval x = 1")
        val whitespaceTokens = tokens.filter { it.type == TokenType.PLAIN && it.text.all { c -> c.isWhitespace() } }
        // 杩炵画绌虹櫧搴斿悎骞朵负鍗曚釜 Token锛堟澶勫惈 \n\n\n 涓夎繛鎹㈣锛?        assertTrue(whitespaceTokens.any { it.text.length > 1 }, "搴斿瓨鍦ㄥ悎骞跺悗鐨勫瀛楃绌虹櫧 Token")
    }

    @Test
    fun should_mergeCrlf_when_windowsLineEndings() {
        val tokens = KotlinLexer.tokenize("val a = 1\r\nval b = 2")
        assertTrue(tokens.any { it.type == TokenType.PLAIN && it.text == "\r\n" }, "\\r\\n 搴斿悎骞朵负鍗曚釜 PLAIN Token")
    }
}
```

杩愯 `.\gradlew.bat :codehighlight-parser:jvmTest --tests "com.hrm.codehigh.lexer.EscapeBoundsTest" --console=plain`锛岄鏈熺涓€涓敤渚嬫姏 IndexOutOfBoundsException锛團AIL锛夛紝鍚庝袱涓?FAIL銆?
- [ ] **Step 2: 淇鍏ㄩ儴 22 澶勮浆涔夎秺鐣?*

缁熶竴鏇挎崲锛歚if (code[pos] == '\\') pos++` 鈫?`if (code[pos] == '\\' && pos + 1 < code.length) pos++`锛圞otlinLexer:90 甯︽敞閲?`// 璺宠繃杞箟瀛楃` 涓€骞朵繚鐣欐敞閲婏級銆?绮剧‘娓呭崟锛堟枃浠?琛岋級锛欱ashLexer:60,73锛汣Lexer:146,159锛圕Lexer:94 宸插畨鍏ㄤ笉鍔級锛汣ssLexer:54锛汫oLexer:72,85锛汮avaLexer:90,103锛汮avaScriptLexer:68,81,94锛汮sonLexer:25锛汯otlinLexer:90,103锛汸ythonLexer:93,106,119锛汻ustLexer:119,132,155锛汼wiftLexer:92锛汿ypeScriptLexer:90,103,116锛沋amlLexer:72銆傦紙ConfigurableLexer:131 宸插畨鍏ㄤ笉鍔級

- [ ] **Step 3: 绌虹櫧鍚堝苟**

`BaseLexer.kt` 澧炲姞锛?
```kotlin
    /** 杩斿洖 [pos] 璧疯繛缁┖鐧藉悗鐨勯涓潪绌虹櫧浣嶇疆 */
    protected fun whitespaceEnd(code: String, pos: Int): Int {
        var i = pos
        while (i < code.length && code[i].isWhitespace()) i++
        return i
    }
```

14 涓墜鍐欒瘝娉曞櫒锛圔ashLexer:157銆丆Lexer:228銆丆ssLexer:168銆丟oLexer:160銆丣avaLexer:170銆丣avaScriptLexer:169銆丣sonLexer:77銆並otlinLexer:177銆丳ythonLexer:194銆丷ustLexer:246銆丼wiftLexer:160銆乊amlLexer:141銆乀ypeScriptLexer:186銆丼qlLexer:177锛夌殑 PLAIN 鍏滃簳锛?
```kotlin
            // 鍏朵粬瀛楃锛氳繛缁┖鐧藉悎骞朵负鍗曚釜 PLAIN锛屽叾浣欓€愬瓧绗﹀厹搴?            if (c.isWhitespace()) {
                val start = pos
                pos = whitespaceEnd(code, pos)
                tokens.add(CodeToken(TokenType.PLAIN, start until pos, code))
                continue
            }
            tokens.add(CodeToken(TokenType.PLAIN, pos until pos + 1, code))
            pos++
```

娉ㄦ剰 CLexer.kt 鍐呭惈 `CLexer` 涓?`CppLexer` 涓や釜 object鈥斺€旂敤 grep `PLAIN, c\.toString\(\)` 鎵惧埌璇ユ枃浠跺唴鍏ㄩ儴鍏滃簳锛堟竻鍗曚腑 228 琛屼粎涓€澶勶紝鑻?CppLexer 鍙︽湁鍒欏悓鍨嬫浛鎹級銆侰onfigurableLexer:198 鐨?PLAIN 鍏滃簳鍋氬悓鏍峰鐞嗭紙璇ョ被涓嶇户鎵?BaseLexer锛屽唴鑱?while 寰幆锛夈€?
- [ ] **Step 4: REGRESS 楠岃瘉锛堝惈鏂版祴璇曪級閫氳繃**

- [ ] **Step 5: Commit** `fix: 淇鍙嶆枩鏉犵粨灏炬湭闂悎瀛楃涓茬殑瓒婄晫宕╂簝锛屽悎骞惰繛缁┖鐧?Token`

---

### Task 3: Lexer 鎺ュ彛鎵╁睍 + KotlinLexer/PythonLexer 涓撻」淇

**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/Lexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/KotlinLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/PythonLexer.kt`
- Create: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/lexer/LexerContractTest.kt`

- [ ] **Step 1: 鎵╁睍 Lexer 鎺ュ彛**

鍦?`Lexer.kt` 鎺ュ彛浣撳唴杩藉姞锛坕mport `com.hrm.codehigh.ast.TokenType`锛夛細

```kotlin
    /**
     * 甯﹀叏鏂囧亸绉荤殑璇嶆硶鍒嗘瀽锛屼緵澧為噺寮曟搸浠?token 杈圭晫閲嶅惎瑙ｆ瀽鏃舵彁渚涗笂涓嬫枃
     * 锛堝琛岄鏁忔劅鍒ゅ畾锛夈€傝繑鍥?Token 鐨?range 浠嶇浉瀵?[code] 浠?0 璁★紝璋冪敤鏂硅嚜琛屽亸绉汇€?     */
    public fun tokenize(code: String, startOffset: Int): List<CodeToken> = tokenize(code)

    /**
     * 璇?Token 鏄惁鍙兘缁х画鍚告敹鍚庣画瀛楃锛堟湭闂悎鐨勫琛岀粨鏋勶級銆?     * 澧為噺寮曟搸鐢ㄥ畠鍐冲畾鏄惁浠庤 Token 璧风偣閲嶈В鏋愶紱鐭墠缂€锛堝鍗曚釜 `"`锛夌敱寮曟搸鐨?     * 閭昏繎 Token 鍥為€€鍏滃簳锛屾棤闇€鍦ㄦ绌蜂妇銆?     */
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

- [ ] **Step 2: KotlinLexer 涓撻」**

a) 澶氳娉ㄩ噴鏀寔宓屽锛堟浛鎹?49-60 琛屽垎鏀級锛?
```kotlin
            // 澶氳娉ㄩ噴锛圞otlin 鏀寔宓屽锛?            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
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

b) 杩愮畻绗﹁〃淇 + 棰勮绠楀父閲忥紙object 椤堕儴鏂板锛?54-167 琛屽垎鏀浛鎹級锛?
```kotlin
    private val threeCharOps = setOf("..<")
    private val twoCharOps = setOf(
        "==", "!=", "<=", ">=", "&&", "||", "++", "--", "+=", "-=", "*=", "/=",
        "%=", "->", "=>", "::", "?.", ".."
    )
```

鍒嗘敮浣撴敼涓猴紙涓嶅啀姣忔 setOf/substring锛夛細

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

c) 112 琛屾暟瀛楀垎鏀殑姝绘潯浠跺寲绠€锛歚if (c.isDigit() || (c == '0' && ...))` 鈫?`if (c.isDigit())`銆?
d) 鏍囩偣鍒嗘敮 170 琛?`c in "{}()[];,.$"` 鍘绘帀 `.`锛坄..` 宸茶繘杩愮畻绗﹁〃锛涘崟涓?`.` 淇濈暀鏍囩偣锛氭敼涓哄厹搴曞墠鍗曠嫭澶勭悊鈥斺€斿叿浣擄細鏍囩偣闆嗗悎鏀逛负 `"{}()[];,."`锛宍$` 褰掑叆鍏滃簳 PLAIN锛夈€?
- [ ] **Step 3: PythonLexer 涓撻」**

a) 瀛楃涓插墠缂€缁勫悎 + 涓夊紩鍙凤紙鏇挎崲 85-99 琛屽垎鏀紝object 椤堕儴鏂板锛夛細

```kotlin
    private val stringPrefixes = setOf("r", "b", "u", "f", "rb", "br", "rf", "fr")

    /** 杩斿洖 [pos] 璧峰悎娉曞瓧绗︿覆鍓嶇紑闀垮害锛堝悗闅忓紩鍙锋墠绠楋級锛屽惁鍒?0 */
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

鍒嗘敮浣擄細

```kotlin
            // 瀛楃涓插墠缂€锛坒/r/b/u 鍙?rb銆乥r銆乺f銆乫r 缁勫悎锛屾敮鎸佷笁寮曞彿锛?            val prefixLen = stringPrefixLengthAt(code, pos)
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

b) 杩愮畻绗﹁〃棰勮绠楋紙鍚?KotlinLexer 鏂瑰紡锛?73-184 琛屽垎鏀級锛?
```kotlin
    private val threeCharOps = setOf("**=", "//=", ">>=", "<<=")
    private val twoCharOps = setOf(
        "==", "!=", "<=", ">=", "**", "//", "+=", "-=", "*=", "/=", "%=",
        "&=", "|=", "^=", "->", "<<", ">>"
    )
```

鍒嗘敮浣撲娇鐢?`"$c$c1$c2" in threeCharOps` / `"$c$c1" in twoCharOps` 瀛楃涓叉ā鏉挎瘮杈冦€?
- [ ] **Step 4: 鎺ュ彛濂戠害娴嬭瘯**

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

- [ ] **Step 5: REGRESS 閫氳繃** 鈥?[x] **Step 6: Commit** `feat: Lexer 鎺ュ彛澧炲姞澧為噺鍗忓晢濂戠害锛涗慨澶?Kotlin 宓屽娉ㄩ噴/杩愮畻绗﹁〃涓?Python 鍓嶇紑瀛楃涓瞏

---

### Task 4: YamlLexer 琛岄涓婁笅鏂?+ IncrementalHighlighter 閲嶆瀯

**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/YamlLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/stream/IncrementalHighlighter.kt`
- Modify: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/stream/IncrementalHighlighterTest.kt`锛堣拷鍔犵敤渚嬶紝瑙?T7 瀹屾垚寮哄寲锛?
- [ ] **Step 1: 鍏堝啓鍋囪棣栧洖褰掓祴璇曪紙杩藉姞鍒?IncrementalHighlighterTest锛?*

```kotlin
    @Test
    fun should_notTreatDirtySubstringStartAsLineStart_when_yamlIncremental() {
        val highlighter = IncrementalHighlighter()
        highlighter.update("a--", "yaml")
        val ast = highlighter.update("a---", "yaml")
        // 琛岄鏁忔劅鐨?--- 鏂囨。鍒嗛殧绗﹀垽瀹氬繀椤讳娇鐢ㄥ叏鏂囦笂涓嬫枃锛宒irty 瀛愪覆璧峰澶勪笉鏄棣?        assertTrue(
            ast.tokens.none { it.type == TokenType.KEYWORD && it.text == "---" },
            "澧為噺閲嶈В鏋愪笉搴旀妸瀛愪覆璧风偣璇垽涓鸿棣栵細tokens=${ast.tokens}"
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
        // 瓒呰繃 64 瀛楃鍥炵湅绐楀彛鐨勬湭闂悎娉ㄩ噴
        val longComment = "/* " + "x".repeat(200)
        highlighter.update(longComment, "kotlin")
        val result = highlighter.updateDetailed(longComment + "\n*/\nval x = 1", "kotlin")
        assertEquals(0, result.reparseStart, "鏈棴鍚堟敞閲婂簲浠庡叾璧风偣閲嶈В鏋愯€岄潪鍏ㄩ噺鍥為€€鍒?0")
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

杩愯澧為噺娴嬭瘯锛屽墠涓や釜鐢ㄤ緥棰勬湡 FAIL銆?
- [ ] **Step 2: YamlLexer 鏀寔琛岄涓婁笅鏂?*

灏?`tokenize(code: String)` 閲嶅懡鍚嶄负甯﹀亸绉诲疄鐜帮細

```kotlin
    override fun tokenize(code: String): List<CodeToken> = tokenize(code, 0)

    override fun tokenize(code: String, startOffset: Int): List<CodeToken> {
        // ...鍘熶富寰幆涓嶅彉锛屼粎鏂囨。鍒嗛殧绗﹀垎鏀殑琛岄鍒ゅ畾鏀逛负锛?        // if ((code.startsWith("---", pos) || code.startsWith("...", pos)) &&
        //     (startOffset + pos == 0 || code[startOffset + pos - 1] == '\n')) {
```

鍚屾椂锛堝悓鏂囦欢锛夛細
- 杞箟淇宸插湪 T2 瀹屾垚锛?- `~` 姝婚厤缃慨澶嶏細鍦ㄧ粨鏋勭鍙峰垎鏀悗鏂板鍒嗘敮锛?
```kotlin
            // null 鍊?~
            if (c == '~') {
                tokens.add(CodeToken(TokenType.BUILTIN, pos until pos + 1, code))
                pos++
                continue
            }
```

- [ ] **Step 3: IncrementalHighlighter 閲嶆瀯**

瀹屾暣鏇挎崲鏂囦欢锛?
```kotlin
package com.hrm.codehigh.stream

import com.hrm.codehigh.ast.CodeAst
import com.hrm.codehigh.ast.CodeToken
import com.hrm.codehigh.lexer.LanguageRegistry

/**
 * 澧為噺楂樹寒寮曟搸锛岀敤浜庢祦寮忓満鏅笅鐨勯珮鏁堜唬鐮侀珮浜洿鏂般€? *
 * 鏍稿績绛栫暐锛? * 1. 绋冲畾鍓嶇紑 Token 鐩存帴澶嶇敤锛屼笉閲嶆柊瑙ｆ瀽
 * 2. 浠呭灏鹃儴鑴忓尯鍩燂紙浠庢渶鍚庝竴涓彈褰卞搷 Token 鍒版枃鏈湯灏撅級閲嶆柊瑙ｆ瀽锛? *    骞舵妸閲嶈В鏋愯捣鐐逛互 startOffset 浼犵粰璇嶆硶鍣紝淇濊瘉琛岄绛夊叏鏂囦笂涓嬫枃鍒ゅ畾姝ｇ‘
 * 3. 鐩稿悓浠ｇ爜瀛楃涓插拰璇█鍛戒腑 AST 缂撳瓨锛岀洿鎺ヨ繑鍥炵紦瀛樼粨鏋? *
 * [UpdateResult.firstChangedLine] / [UpdateResult.reparseStart] 涓?-1 琛ㄧず鏃犲彉鍖栥€? */
class IncrementalHighlighter {
    data class UpdateResult(
        val ast: CodeAst,
        /** 棣栦釜鍙樻洿琛屽彿锛?1 琛ㄧず鏈鏃犲彉鍖栵紙缂撳瓨鍛戒腑锛?*/
        val firstChangedLine: Int,
        /** 閲嶈В鏋愯捣鐐癸紱-1 琛ㄧず鏈鏃犲彉鍖栵紙缂撳瓨鍛戒腑锛?*/
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
        // 闀垮害鐭矾锛岄伩鍏嶅ぇ瀛楃涓查€愬瓧绗︽瘮杈?        if (cached != null && cached.source.length == code.length && cached.source == code) {
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
        // 鎸夌ǔ瀹?Token 鏁板彇鍓嶇紑瑙嗗浘锛孫(1) 鏃犳嫹璐?        val stableCount = oldAst.tokens.count { it.range.last < reparseStart }
        val stable = oldAst.tokens.subList(0, stableCount)

        // startOffset 璁╄瘝娉曞櫒浠ュ叏鏂囪瑙掑垽鏂棣栫瓑涓婁笅鏂囷紱杩斿洖 range 浠嶇浉瀵瑰瓙涓?        val dirtyCode = newCode.substring(reparseStart)
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

        // 鏈棴鍚堝琛岀粨鏋勶細涓嶅彈鍥炵湅绐楀彛闄愬埗锛堝惁鍒欓暱娉ㄩ噴/闀垮瓧绗︿覆瀵艰嚧姣忔鍏ㄩ噺閲嶈В鏋愶紝娴佸紡绱 O(n虏)锛?        val unfinishedTokenStart = oldAst.tokens
            .asReversed()
            .firstOrNull { it.range.last < appendedStart && LanguageRegistry.getOrPlain(oldAst.language).isExtendableToken(it) }
            ?.range
            ?.first

        // 閭昏繎 Token 鍥為€€锛氬惛鏀跺洜杩藉姞鑰岃鏀瑰彉灏鹃儴鐨勬渶鍚庝竴涓煭 Token
        val nearbyTokenStart = oldAst.tokens
            .lastOrNull { it.range.last < appendedStart && appendedStart - it.range.first <= CONTEXT_LOOKBACK_CHARS }
            ?.range
            ?.first

        return unfinishedTokenStart ?: nearbyTokenStart ?: 0
    }

    /** 娓呴櫎缂撳瓨锛屽己鍒朵笅娆″叏閲忚В鏋?*/
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

娉ㄦ剰锛歞irtyTokens 浠?`newCode` 涓?source锛屾儼鎬?text 鐩存帴浠庢柊鏂囨湰鍒囩墖銆?
- [ ] **Step 4: REGRESS 閫氳繃锛堥噸鐐?IncrementalHighlighterTest 鍏ㄧ豢锛?*

- [ ] **Step 5: Commit** `fix: 澧為噺寮曟搸娑堣垂璇嶆硶鍣ㄤ笂涓嬫枃濂戠害锛屼慨澶嶅亣琛岄涓庨暱鏈棴鍚?Token 鐨?O(n虏) 閫€鍖朻

---

### Task 5: ConfigurableLexer 鎺掑簭缂撳瓨 + 鐗规畩瀹氱晫绗﹁鍐?
**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/ConfigurableLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/LuaLexer.kt`
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/HaskellLexer.kt`

- [ ] **Step 1: 鎺掑簭/鍖归厤琛ㄩ璁＄畻**

鍏堣 ConfigurableLexer.kt 鍏ㄦ枃銆傚皢 `tokenize` 鍐呮瘡娆℃墽琛岀殑 `sortedByDescending { it.length }`锛堢害 32-38 琛岋紝7 涓被鍒級绉诲埌 object 鍒濆鍖栨椂鐨?`private val`锛坰pec 鑻ヤ负鏋勯€犲弬鏁帮紝鍦?init/灞炴€у垵濮嬪寲鍣ㄤ腑棰勬帓搴忥級銆傛ā寮忥細

```kotlin
    // 棰勮绠楋細鎸夐暱搴﹂檷搴忥紝閬垮厤姣忔 tokenize 閲嶆帓
    private val sortedFixedTokens = spec.fixedTokens.sortedByDescending { it.length }
    private val sortedOperators = spec.operators.sortedByDescending { it.length }
    // ... 鍏朵綑绫诲埆鍚岀悊
```

涓诲惊鐜腑鐩稿簲 `sortedByDescending` 璋冪敤鏇挎崲涓洪璁＄畻灞炴€с€傚瓧娈靛悕浠ュ疄闄呮枃浠朵负鍑嗛€傞厤銆?
- [ ] **Step 2: ConfigurableLexer 瑕嗗啓 isExtendableToken**

渚濇嵁 spec 鐨勫琛屽畾鐣岀瀹炵幇锛堣鏂囦欢鍚庢寜瀹為檯瀛楁鍚嶉€傞厤锛夛細

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

锛堝瓧娈靛悕浠ュ疄闄?spec 瀹氫箟涓哄噯锛涜嫢 spec 鏃犲潡娉ㄩ噴瀹氱晫瀛楁鍒欏彧淇濈暀鎺掑簭浼樺寲銆傦級

- [ ] **Step 3: Lua/Haskell 瑕嗗啓**

璇讳袱鏂囦欢纭澶氳瀹氱晫绗︼紙Lua 闀垮瓧绗︿覆 `[[`/`]]`锛孒askell 鍧楁敞閲?`{-`/`-}`锛夛紝鍦ㄥ悇鑷?object 鍐呰拷鍔狅細

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

锛堜互瀹為檯瀹氱晫绗﹀垎鏀懡鍚嶄负鍑嗛€傞厤锛涜嫢璇嶆硶鍣ㄤ笉鏀寔璇ョ粨鏋勫垯璺宠繃骞惰褰曘€傦級

- [ ] **Step 4: REGRESS 閫氳繃** 鈥?[x] **Step 5: Commit** `perf: ConfigurableLexer 鍖归厤琛ㄩ璁＄畻锛涚壒娈婂畾鐣岀澹版槑鍙墿灞?Token`

---

### Task 6: LanguageRegistry 绾跨▼瀹夊叏

**Files:**
- Modify: `codehighlight-parser/src/commonMain/kotlin/com/hrm/codehigh/lexer/LanguageRegistry.kt`

- [ ] **Step 1: 楗挎眽鍒濆鍖?*

鍒犻櫎 `defaultsRegistered`/`ensureDefaultsRegistered`锛宍init { registerDefaults() }`銆傛敞鍐屽畬鎴愬悗娉ㄥ唽琛ㄤ粛鍙€氳繃 `register()` 鎵╁睍锛堜繚鎸佹棦鏈?public 琛屼负锛夛紝浣嗛粯璁よ瑷€鍦ㄧ被鍔犺浇鏃朵竴娆″啓鍏ワ紙object 鍒濆鍖栫敱 JVM/Native 绫诲姞杞芥満鍒朵繚璇佺嚎绋嬪畨鍏紱JS 鍗曠嚎绋嬶級銆俙registry`/`aliases` 淇濇寔 `internal` 鍙彉 map锛堝閮ㄨ嚜瀹氫箟璇█娉ㄥ唽鏄棦鏈夌壒鎬э級銆侹Doc 娉ㄦ槑锛歚register()` 闈炵嚎绋嬪畨鍏紝寤鸿鍦ㄥ簲鐢ㄥ垵濮嬪寲闃舵锛堝崟绾跨▼锛夎皟鐢ㄣ€?
- [ ] **Step 2: REGRESS 閫氳繃**

- [ ] **Step 3: Commit** `fix: LanguageRegistry 榛樿璇█楗挎眽娉ㄥ唽锛屾秷闄ゆ噿鍒濆鍖栫珵鎬乣

---

### Task 7: parser 娴嬭瘯鍗囩骇

**Files:**
- Modify: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/stream/IncrementalHighlighterTest.kt`
- Create: `codehighlight-parser/src/commonTest/kotlin/com/hrm/codehigh/lexer/LanguageCoverageTest.kt`

- [ ] **Step 1: 淇浼鐩栨柇瑷€**

`should_returnCachedAst_when_sameCodeAndLanguage`锛歚assertEquals(ast1, ast2)` 鈫?`assertSame(ast1, ast2)`锛坕mport kotlin.test.assertSame锛夈€?`should_invalidateCache_when_invalidateCalled`锛氳拷鍔?`assertNotSame(ast1, ast2)`锛坕mport kotlin.test.assertNotSame锛夈€?
- [ ] **Step 2: 鍏ㄨ瑷€瑕嗙洊娴嬭瘯锛?8 璇█锛屽惈閲嶆瀯涓嶅彉寮忥級**

```kotlin
package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LanguageCoverageTest {

    /** 姣忚瑷€鏈€灏忔牱鏈細鍚敞閲娿€佸瓧绗︿覆銆佸叧閿瓧/鎸囦护褰㈡€?*/
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
                "lang=$lang 閲嶆瀯涓嶅彉寮忕牬鍧?,
            )
        }
    }

    @Test
    fun should_haveCommentAndStringTokens_when_allLanguagesTokenized() {
        // XML/Dockerfile/Bash/YAML/TOML/R/Diff 鍚勮嚜鑷冲皯瀛樺湪娉ㄩ噴鎴栧厓淇℃伅 Token锛?        // JSON 鏃犳敞閲婏紝浠呮柇瑷€瀛楃涓?        for ((lang, sample) in samples) {
            val tokens = LanguageRegistry.getOrPlain(lang).tokenize(sample)
            if (lang != "json") {
                assertTrue(
                    tokens.any { it.type == TokenType.COMMENT },
                    "lang=$lang 搴旇瘑鍒敞閲?,
                )
            }
            assertTrue(
                tokens.any { it.type == TokenType.STRING || it.type == TokenType.COMMENT },
                "lang=$lang 搴旇瘑鍒瓧绗︿覆鎴栨敞閲?,
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
        // 娴佸紡涓棿鎬侊細鍚勭被鏈棴鍚堢粨鏋?        val unclosed = listOf("/* ", "\"", "'", "\"\"\"", "#", "<a href=", "{ \"k\": ")
        for (lang in samples.keys) {
            for (snippet in unclosed) {
                LanguageRegistry.getOrPlain(lang).tokenize(snippet) // 涓嶆姏寮傚父鍗抽€氳繃
            }
        }
    }
}
```

锛堣嫢鏌愯瑷€鏍锋湰鏂█澶辫触锛岃鏄庤璇嶆硶鍣ㄥ瓨鍦ㄧ湡瀹炵己鍙ｏ細鍏堜慨璇嶆硶鍣ㄥ啀鍥炴潵璺戠豢鈥斺€斿鐓уけ璐ヤ俊鎭畾浣嶏紝绂佹鏀惧鏂█鏉?閫氳繃"銆傦級

- [ ] **Step 3: REGRESS 閫氳繃** 鈥?[x] **Step 4: Commit** `test: 鏂█缂撳瓨鍚屼竴鎬т笌 28 璇█鍏ㄨ鐩栦笉鍙樺紡`

---

### Task 8: 涓婚灞傜ǔ瀹氭€?+ diff 瀹氬埗 + CompositionLocal 淇

**Files:**
- Create: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/theme/CodeLineKind.kt`锛堜粠 renderer 鍖?public 鍖栬縼绉伙級
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/theme/CodeTheme.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/theme/LocalCodeTheme.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/HighlightedString.kt`锛圕odeLineKind 寮曠敤鏀瑰寘锛?- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/CodeBlock.kt`锛坕mport 璋冩暣锛屾湰浠诲姟浠呮渶灏忔敼鍔紝閲嶆瀯鍦?T10锛?- Modify: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/theme/CodeThemeTest.kt`

- [ ] **Step 1: CodeLineKind 杩佺Щ鑷?theme 鍖呭苟 public**

鏂版枃浠讹細

```kotlin
package com.hrm.codehigh.theme

/** 浠ｇ爜琛屾覆鏌撶绫伙細鏅€?楂樹寒/diff 鍚勫舰鎬?*/
enum class CodeLineKind {
    NORMAL, HIGHLIGHTED, DIFF_ADDED, DIFF_REMOVED, DIFF_META_HEADER, DIFF_META_HUNK
}
```

鍒犻櫎 HighlightedString.kt 鍐呯殑 internal enum CodeLineKind锛宺enderer 鍐呭紩鐢ㄦ敼 import锛圕odeLineRender 淇濇寔 internal锛屽叾 kind 瀛楁绫诲瀷鏀逛负 theme.CodeLineKind锛夈€?
- [ ] **Step 2: CodeTheme 澧炲己**

`CodeTheme.kt` 鎺ュ彛鏍囨敞 `@Immutable`锛坕mport androidx.compose.runtime.Immutable锛夛紝骞惰拷鍔?diff 鍓嶆櫙/鏍囪榛樿灞炴€э紙鍘?CodeBlock.kt:269-294 纭紪鐮佽縼鍏ワ紝鑷畾涔変富棰樺彲瑕嗗啓锛夛細

```kotlin
    /** diff 鏍囪锛?/-锛夊墠鏅壊 */
    fun diffMarkerColor(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_ADDED -> if (isDark) Color(0xFF9BE9A8) else Color(0xFF1F7A38)
        CodeLineKind.DIFF_REMOVED -> if (isDark) Color(0xFFFFA8B5) else Color(0xFFB42318)
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFFD1D5DB) else Color(0xFF4B5563)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFF9CDCFE) else Color(0xFF0958D9)
        else -> colorFor(TokenType.PLAIN)
    }

    /** diff 鏍囪鑳屾櫙鑹?*/
    fun diffMarkerBackground(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_ADDED -> if (isDark) Color(0xFF224D35) else Color(0xFFD9F5E0)
        CodeLineKind.DIFF_REMOVED -> if (isDark) Color(0xFF5A2730) else Color(0xFFFADADD)
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFF1F4B70) else Color(0xFFDCEEFF)
        else -> Color.Transparent
    }

    /** diff 鍏冧俊鎭姝ｆ枃棰滆壊 */
    fun diffTextColor(kind: CodeLineKind): Color = when (kind) {
        CodeLineKind.DIFF_META_HEADER -> if (isDark) Color(0xFFE5E7EB) else Color(0xFF374151)
        CodeLineKind.DIFF_META_HUNK -> if (isDark) Color(0xFFBFE3FF) else Color(0xFF0B4F8A)
        else -> colorFor(TokenType.PLAIN)
    }
```

`backgroundForLine`锛圚ighlightedString.kt:167-174 鐨?internal 鎵╁睍锛変繚鎸佷笉鍔ㄣ€?
- [ ] **Step 3: LocalCodeTheme 鏀?compositionLocalOf**

```kotlin
val LocalCodeTheme = compositionLocalOf<CodeTheme> { OneDarkProTheme }
```

锛堜富棰樹細闅忕郴缁熸殫鑹叉ā寮忓姩鎬佸垏鎹紝static 鐗堜笉杩借釜渚濊禆銆傦級

- [ ] **Step 4: CodeThemeTest 鍗囩骇**

鍚堝苟 4 涓悓鏋勭敤渚?+ Unspecified 寮烘柇瑷€ + safeColorFor 鐪熷洖閫€锛?
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

鍒犻櫎鍘?4 涓?`should_coverAllTokenTypes_when_*Theme` 涓庢棫 `should_fallbackToPlain_when_safeColorFor`锛沬sDark 鏂█鐢?allThemes 寰幆锛坉ark 涓婚 2 涓€乴ight 2 涓級銆俰mport `com.hrm.codehigh.theme.CodeLineKind`锛堝悓鍖呮棤闇€锛夈€?
- [ ] **Step 5: REGRESS 閫氳繃** 鈥?[x] **Step 6: Commit** `feat: 涓婚鎺ュ彛 @Immutable 涓?diff 棰滆壊瀹氬埗锛涗慨澶?CompositionLocal 璇箟`

---

### Task 9: HighlightedString 娓叉煋浼樺寲 + 娴嬭瘯

**Files:**
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/HighlightedString.kt`
- Create: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/renderer/HighlightedStringTest.kt`

- [ ] **Step 1: buildHighlightedString 鏍峰紡缂撳瓨 + 鐩搁偦鍚堝苟**

鏇挎崲 117-143 琛屽疄鐜帮細

```kotlin
fun buildHighlightedString(
    tokens: List<CodeToken>,
    theme: CodeTheme
): AnnotatedString {
    // 鎸夌被鍨嬬紦瀛?SpanStyle锛堜富棰樺崟渚嬨€乀oken 绫诲瀷鏈夐檺锛夛紝鐩搁偦鍚屾牱寮忓悎骞?span
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

`buildLineRenders` 鍐咃紙鍘?FromOffset 閫昏緫鍐呰仈鍚庯級锛歚language.lowercase()` 鎻愬崌鍒板嚱鏁板紑澶磋绠椾竴娆′紶鍏?`resolveLineKind`锛堝叾绛惧悕鍐呴儴鏀逛负鎺ユ敹宸插綊涓€鍖?language锛夈€?
- [ ] **Step 2: 娴嬭瘯**

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
            CodeToken(TokenType.PLAIN, 3 until 7, src), // 鍚┖鏍硷紝鍚屼负 PLAIN
        )
        val s = buildHighlightedString(tokens, OneDarkProTheme)
        assertEquals(src, s.text)
        assertEquals(1, s.spanStyles.size, "鐩搁偦鍚屾牱寮忓簲鍚堝苟涓?1 涓?span")
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
        assertTrue(s.spanStyles.map { it.item }.distinct().size <= 2, "PLAIN 涓?IDENTIFIER 鍚岃壊涔熷簲澶嶇敤鐩哥瓑 SpanStyle")
        // SpanStyle 鐩哥瓑鎬?        assertEquals(SpanStyle(color = OneDarkProTheme.colorFor(TokenType.PLAIN)), s.spanStyles[0].item)
    }
}
```

娉細OneDarkPro 鐨?IDENTIFIER 涓?PLAIN 鍚岃壊锛?xFFABB2BF锛夛紝鐩搁偦鍚屾牱寮忥紙鍚腑闂翠笉鍚?push 閫昏緫锛変緷瀹炵幇搴斾负 3 娈?span锛涜嫢鏂█涓庡疄鐜扮粏鑺傚啿绐佷互"span 鏁?鈮?token 鏁颁笖鐩搁偦鍚岀被鍨嬪悎骞?涓哄噯淇鏂█骞惰鏄庛€?
- [ ] **Step 3: REGRESS 閫氳繃** 鈥?[x] **Step 4: Commit** `perf: 楂樹寒瀛楃涓?SpanStyle 缂撳瓨涓庣浉閭诲悎骞讹紝span 鏁伴噺鍘婚噸`

---

### Task 10: CodeBlock 娓叉煋閲嶆瀯

**Files:**
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/CodeBlock.kt`

- [ ] **Step 1: 鍏ュ彛褰掍竴鍖栦笌璁板繂鍖栵紙鏇挎崲 71-94 琛屽尯鍩燂級**

```kotlin
    // CRLF 褰掍竴鍖栵紙Windows 鍓创鏉垮父瑙侊級锛屽苟璁板繂鍖栬鎷嗗垎锛屾祦寮忛珮棰戦噸缁勪笅閬垮厤 O(n) 閲嶅垎閰?    val normalizedCode = remember(code) { if (code.contains('\r')) code.replace("\r\n", "\n").replace("\r", "\n") else code }
    val lines = remember(normalizedCode) { normalizedCode.split("\n") }
    val totalLines = lines.size
    val isCollapsible = maxVisibleLines != null && totalLines > maxVisibleLines
    val visibleLineCount = when {
        !isCollapsible || isExpanded -> totalLines
        else -> maxVisibleLines!! // isCollapsible 涓?true 鏃跺繀闈炵┖
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

- [ ] **Step 2: 寮傛瑙ｆ瀽 + 澧為噺淇℃伅娑堣垂锛堟浛鎹?visibleAst/lineHighlights 涓ゆ remember锛?*

```kotlin
    val highlighter = remember { IncrementalHighlighter() }
    // 鍒濆 null锛氶甯у厛璧扮函鏂囨湰鍥為€€锛岃В鏋愬湪 Default 璋冨害鍣ㄦ墽琛屽悗鏇挎崲
    var lineHighlights by remember { mutableStateOf<List<CodeLineRender>?>(null) }
    var visibleAst by remember { mutableStateOf<CodeAst?>(null) }
    LaunchedEffect(visibleCode, language, theme) {
        val result = withContext(Dispatchers.Default) {
            val detailed = highlighter.updateDetailed(visibleCode, language)
            if (detailed.firstChangedLine < 0) null else { // 鏃犲彉鍖栨椂淇濈暀鐜版湁娓叉煋
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

锛坄highlighter` 涓嶅啀浠?language 涓?key鈥斺€攗pdateDetailed 鍐呴儴宸插鐞嗚瑷€鍒囨崲銆傛柊澧?import锛歚com.hrm.codehigh.ast.CodeAst`銆乣kotlinx.coroutines.Dispatchers`銆乣kotlinx.coroutines.withContext`銆俙fallbackToPlainLines` 鍒ゅ畾鏀瑰熀浜?resolvedLines銆傦級

- [ ] **Step 3: 甯冨眬淇**

- 琛屽鍣細`Modifier.height(codeLineHeightDp)` 鈫?`Modifier.heightIn(min = codeLineHeightDp)`锛坕mport androidx.compose.foundation.layout.heightIn锛夈€?- 琛屽彿瀹藉害鍔ㄦ€侊細`val lineNumberWidth = remember(totalLines) { ((totalLines.toString().length.coerceAtLeast(2)) * 8 + 8).dp }`锛?91 琛?`.width(40.dp)` 鈫?`.width(lineNumberWidth)`銆?- 姝ｆ枃鏍峰紡缂撳瓨锛歚val textStylesByKind = remember(theme) { CodeLineKind.entries.associateWith { theme.textStyleForLine(it) } }`锛涚鏈夋墿灞?`textStyleForLine` 鏀圭敤 `theme.diffTextColor(kind)`锛沗diffMarkerBackgroundForLine`/`diffMarkerColorForLine` 绉佹湁鎵╁睍鍒犻櫎锛岃皟鐢ㄧ偣鏀?`theme.diffMarkerBackground(kind)`/`theme.diffMarkerColor(kind)`銆?25-228 琛?BasicText锛歚style = textStylesByKind.getValue(lineRender.kind)`銆?- KDoc 涓庨粯璁ゅ€硷細`maxVisibleLines: Int? = 500`锛孠Doc 鏇存柊锛?榛樿 500锛宯ull 涓嶉檺鍒?锛夈€?
- [ ] **Step 4: onTokenClick 瀹炵幇**

```kotlin
    val tokenClickLayouts = remember { mutableMapOf<Int, TextLayoutResult>() }
```

姝ｆ枃 BasicText锛圫tep 3 淇敼鍚庯級杩藉姞鍙傛暟锛?
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

import锛歚androidx.compose.foundation.gestures.detectTapGestures`銆乣androidx.compose.ui.input.pointer.pointerInput`銆乣androidx.compose.ui.text.TextLayoutResult`銆?
- [ ] **Step 5: 鏂囨湰鍙€?+ CopyButton**

- 鏂板弬鏁?`selectable: Boolean = true`锛圞Doc锛氭鏂囨槸鍚﹀彲閫変腑澶嶅埗锛夈€傝 Column 鐢?`if (selectable) SelectionContainer { Column { ... } } else Column { ... }` 鍖呰９锛坕mport androidx.compose.foundation.text.selection.SelectionContainer锛夈€傚皢鍘?158-240 琛?Column 鍐呭鎻愬彇涓烘湰鍦?`val linesContent = @Composable { ... }` 閬垮厤閲嶅锛屼袱绉嶅垎鏀潎璋冪敤 `linesContent()`銆?- CopyButton 璁℃椂淇锛堣繛鍑婚噸缃級锛?
```kotlin
    var copyCount by remember { mutableStateOf(0) }
    val copied = copyCount > 0
    LaunchedEffect(copyCount) {
        if (copyCount > 0) {
            delay(2000)
            copyCount = 0
        }
    }
    // clickable 鍐咃細澶嶅埗鍚?copyCount++
```

- 鍓创鏉?API 杩佺Щ锛歚LocalClipboardManager`/`clipboardManager.setText` 杩佺Щ鍒?`val clipboard = LocalClipboard.current` + `clipboard.setClipEntry(ClipEntry(AnnotatedString(code)))`锛坈ompose.ui 1.9 澶氬钩鍙?API锛夛紱鑻ヨ鏋勯€?鏂规硶鍦ㄥ綋鍓嶇増鏈鍚嶄笉鍚岋紙缂栬瘧鍣ㄤ細鎸囧嚭锛夛紝鎸夋彁绀烘敼涓虹瓑浠峰舰寮忥紙濡?`ClipEntry.of(...)`锛夛紝淇濇寔鍒犻櫎涓ゅ `@Suppress("DEPRECATION")`銆俰mport `androidx.compose.ui.platform.LocalClipboard`銆乣androidx.compose.ui.platform.ClipEntry`銆?
- [ ] **Step 6: REGRESS 閫氳繃锛坮ender jvmTest + preview 鐩稿叧缂栬瘧锛?*

`.\gradlew.bat :codehighlight-render:jvmTest :codehighlight-preview:compileKotlinJvm --console=plain`

- [ ] **Step 7: Commit** `feat: CodeBlock 寮傛澧為噺娓叉煋銆乷nTokenClick 鐐瑰嚮鍒嗗彂銆佸竷灞€鏃犻殰纰嶄笌閫夋嫨鏀寔`

---

### Task 11: i18n 娉ㄥ叆 + StreamingCursor + InlineCodeSize 鍗曚綅

**Files:**
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/i18n/Strings.kt`
- Modify/鍒犻櫎: `codehighlight-render/src/*Main/kotlin/com/hrm/codehigh/i18n/PlatformLocale.kt`锛? 涓?actual锛?- Create: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/i18n/LocalCodeBlockStrings.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/CodeBlock.kt`锛堟秷璐规敞鍏ワ級
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/StreamingCursor.kt`
- Modify: `codehighlight-render/src/commonMain/kotlin/com/hrm/codehigh/renderer/InlineCodeMeasurer.kt`
- Modify: preview 妯″潡寮曠敤鐐癸紙grep `measureInlineCodeSize|InlineCodeSize|widthDp|heightDp` 瀹氫綅锛?- Modify: `codehighlight-render/src/commonTest/kotlin/com/hrm/codehigh/renderer/InlineCodeStyleTest.kt`

- [ ] **Step 1: Strings 鍙敞鍏?*

`Strings.kt` 閲嶅啓锛?
```kotlin
package com.hrm.codehigh.i18n

/**
 * 浠ｇ爜鍧楀唴缃枃妗堬紙鏀惰捣/灞曞紑/澶嶅埗绛夛級娉ㄥ叆鎺ュ彛銆? * 瀹夸富鍙疄鐜板悗閫氳繃 [LocalCodeBlockStrings] 瑕嗙洊榛樿鏂囨銆? */
fun interface CodeBlockStrings {
    fun collapse(): String
    fun expand(hiddenLines: Int): String
    fun copy(): String
    fun copied(): String
}

/** 榛樿鏂囨锛氳窡闅忕郴缁熻瑷€锛堜腑鏂?鑻辨枃锛夛紝璇█妫€娴嬪湪杩涚▼鍐呬粎鎵ц涓€娆?*/
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
    override fun copy(): String = if (isChinese) "澶嶅埗" else "Copy"
    override fun copied(): String = if (isChinese) "宸插鍒? else "Copied"
}

/** 骞冲彴璇█鏍囩锛圔CP-47锛屽 zh-Hans-CN锛夛紝浠呭彇涓诲瓙鏍囩鍋氳瑷€鍒ゆ柇 */
internal expect fun platformLanguageTag(): String
```

锛堝垹闄?LocaleInfo銆乴ineNumber() 姝讳唬鐮佷笌鏃?Strings object銆傦級
鏂版枃浠讹細

```kotlin
package com.hrm.codehigh.i18n

import androidx.compose.runtime.compositionLocalOf

/** 瀹夸富娉ㄥ叆鑷畾涔変唬鐮佸潡鏂囨鐨勫叆鍙?*/
val LocalCodeBlockStrings = compositionLocalOf<CodeBlockStrings> { DefaultCodeBlockStrings }
```

5 涓?PlatformLocale.kt actual 鍏ㄩ儴鏇挎崲涓猴細

```kotlin
package com.hrm.codehigh.i18n
// jvmMain / androidMain锛?internal actual fun platformLanguageTag(): String = java.util.Locale.getDefault().toLanguageTag()
```

```kotlin
// jsMain锛?package com.hrm.codehigh.i18n
internal actual fun platformLanguageTag(): String = js("navigator.language || 'en'") as String? ?: "en"
```

```kotlin
// wasmJsMain锛?package com.hrm.codehigh.i18n
import kotlinx.browser.window
internal actual fun platformLanguageTag(): String = window.navigator.language
```

```kotlin
// iosMain锛?package com.hrm.codehigh.i18n
import platform.Foundation.NSBundle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.preferredLocalizations
internal actual fun platformLanguageTag(): String {
    // 浼樺厛 App 绾ч閫夋湰鍦板寲锛屽洖閫€绯荤粺鍖哄煙
    val preferred = NSBundle.mainBundle.preferredLocalizations.firstOrNull() as? String
    return preferred ?: NSLocale.currentLocale.languageCode ?: "en"
}
```

CodeBlock 鍐咃細`val strings = LocalCodeBlockStrings.current`锛屾姌鍙犳寜閽?`Strings.collapse()`鈫抈strings.collapse()` 绛夛紱CopyButton 澧炲姞 `strings: CodeBlockStrings` 鍙傛暟銆俲s 鐨?`as String?` 鍐欐硶鑻ョ紪璇戝櫒鎶ラ敊锛坖s() 杩斿洖鍔ㄦ€佺被鍨嬶級锛屾敼涓?`js("window.navigator.language || 'en'") as String`锛堣窡闅忕紪璇戝櫒鎻愮ず锛屼袱鑰呮嫨涓€缂栬瘧閫氳繃鑰咃級銆?
- [ ] **Step 2: StreamingCursor 鍔ㄧ敾涓嬫矇 + 楂樺害鑷€傚簲**

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
            .graphicsLayer { this.alpha = alpha } // draw 闃舵璇诲€硷紝閬垮厤姣忓抚閲嶇粍
            .background(color)
    )
}
```

import 鍙樻洿锛?`androidx.compose.ui.graphics.graphicsLayer`銆乣androidx.compose.ui.unit.Dp`锛沗color.copy(alpha=...)` 鏀逛负绾?`color`銆侰odeBlock 璋冪敤澶勪紶 `cursorHeight = codeLineHeightDp * 0.8f`銆?
- [ ] **Step 3: InlineCodeSize 鍗曚綅鏄庣‘锛堢牬鍧忔€ч噸鍛藉悕锛?*

```kotlin
data class InlineCodeSize(
    /** 瀹藉害锛堝儚绱狅級 */
    val widthPx: Float,
    /** 楂樺害锛堝儚绱狅級 */
    val heightPx: Float,
) {
    fun width(density: Density): Dp = with(density) { widthPx.toDp() }
    fun height(density: Density): Dp = with(density) { heightPx.toDp() }
}
```

`measureInlineCodeSize` 鐨?`maxWidth` 鍙傛暟閲嶅懡鍚?`maxWidthPx: Float = Float.POSITIVE_INFINITY`锛孠Doc 娉ㄦ槑鍍忕礌鍗曚綅銆俫rep 鍏ㄤ粨搴?`widthDp(|heightDp(|InlineCodeSize(|measureInlineCodeSize(` 鏇存柊璋冪敤鐐癸紙preview 妯″潡鍐咃級锛宍x.widthDp(density)` 鈫?`x.width(density)`銆?
- [ ] **Step 4: InlineCodeStyleTest 鏀圭浉瀵规柇瑷€**

鍒犻櫎甯搁噺鍥炲０鏂█锛屾敼鍐欎负锛?
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

锛堜繚鐣欏師绗?1銆? 鐢ㄤ緥涓?theme 娲剧敓"鏂█锛歵extStyle.color == theme.colorFor(PLAIN)锛沠ontSize/lineHeight 閿氱偣 13.sp/20.sp 淇濈暀銆傦級import assertNotEquals/assertTrue銆?
- [ ] **Step 5: REGRESS + preview 缂栬瘧閫氳繃**

- [ ] **Step 6: Commit** `feat: 鏂囨鍙敞鍏ヤ笌骞冲彴璇█鏍囩绠€鍖栵紱鍏夋爣鍔ㄧ敾涓嬫矇 draw 灞傦紱娴嬮噺 API 鍗曚綅鏄惧紡鍖朻

---

### Task 12: 鏋勫缓宸ョ▼鏀舵暃

**Files:**
- Modify: 鏍?`build.gradle.kts`銆乣codehighlight-parser/build.gradle.kts`銆乣codehighlight-render/build.gradle.kts`銆乣gradle/libs.versions.toml`銆乣gradle.properties`

- [ ] **Step 1: 鐗堟湰鍏?version catalog锛?.0.0锛?*

`libs.versions.toml` `[versions]` 杩藉姞 `codehigh = "2.0.0"`锛沗gradle.properties` 鍒犻櫎 `VERSION=1.1.2`锛涗袱涓簱妯″潡 `rootProject.property("VERSION").toString()` 鈫?`libs.versions.codehigh.get()`銆?
- [ ] **Step 2: POM 鍏叡鍧楁敹鏁涘埌鏍规瀯寤?*

鏍?`build.gradle.kts` 杩藉姞锛?
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

涓や釜搴撴ā鍧楃殑 mavenPublishing 鍧楀垹鑷充粎鍓?`coordinates(...)` + `pom { name.set / description.set }`锛坈oordinates 鐢?Step 1 鐨?toml 寮曠敤锛夈€?
- [ ] **Step 3: 搴撴ā鍧楀幓闄ゅ簲鐢ㄧ骇浜х墿**

parser/render 涓ゆā鍧楀垹闄わ細iOS `binaries.framework { ... }` 鏁村潡锛?6-44 / 36-44 鍖哄煙锛変笌 js/wasmJs 鐨?`binaries.executable()`锛堝叡 4 澶勶級銆俻review/composeApp 涓嶅姩锛堝彲杩愯 demo 闇€瑕侊級銆?
- [ ] **Step 4: explicitApiWarning**

涓や釜搴撴ā鍧?`kotlin {` 鍧楅琛屽姞 `explicitApiWarning()`锛堟笎杩涜縼绉伙細鏂版敼鍔ㄥ嵆鏃舵姤閿欐彁閱掞紝瀛橀噺涓嶇牬鍧忔瀯寤猴級銆?
- [ ] **Step 5: 楠岃瘉**

`.\gradlew.bat :codehighlight-parser:assemble :codehighlight-render:assemble --console=plain`锛堢‘璁?POM/鍧愭爣閰嶇疆鍚堟硶銆乧onfiguration-cache 涓嶆姤閿欙級銆?
- [ ] **Step 6: Commit** `chore: 鐗堟湰鏀舵暃 version catalog锛?.0.0锛夈€丳OM 鍏叡鍧椾笂绉汇€佸簱妯″潡鍘诲簲鐢ㄤ骇鐗┿€佸紑鍚?explicitApiWarning`

---

### Task 13: CI + 鏂囨。淇 + 鐑熷洷娴嬭瘯

**Files:**
- Create: `.github/workflows/ci.yml`
- Modify: `HIGHLIGHTER_COVERAGE_ANALYSIS.md`銆乣README.md`
- Create: `codehighlight-preview/src/commonTest/kotlin/com/hrm/codehigh/preview/SampleCodeSmokeTest.kt`锛堢洰褰曚笉瀛樺湪鍒欏垱寤猴紝preview build.gradle.kts 闇€纭 commonTest 渚濊禆 kotlin-test锛岃嫢鏃犲垯琛?`sourceSets { commonTest.dependencies { implementation(libs.kotlin.test) } }`锛?- Delete: `composeApp/src/commonTest/kotlin/com/hrm/codehigh/ComposeAppCommonTest.kt`

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

鏈湴楠岃瘉浠诲姟鍚嶅瓨鍦細`.\gradlew.bat :codehighlight-parser:tasks --all | Select-String "iosSimulatorArm64Test|jsBrowserTest|wasmJsBrowserTest"`锛坕OS 浠诲姟鏈湴 Windows 涓嶅彲鎵ц锛屼粎纭浠诲姟鍚嶅瓨鍦ㄤ簬浠诲姟鍒楄〃鈥斺€斾笉瀛樺湪鍒欐敼鐢?`:codehighlight-parser:compileKotlinIosSimulatorArm64` 绛夌紪璇戜换鍔★級銆?
- [ ] **Step 2: preview 鐑熷洷娴嬭瘯**

```kotlin
package com.hrm.codehigh.preview

import com.hrm.codehigh.lexer.LanguageRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 鍏ㄩ儴棰勮鏍蜂緥鍙瀵瑰簲璇嶆硶鍣ㄥ畬鏁磋В鏋愶紙瀛楃鍏ㄨ鐩栦笉鍙樺紡锛?*/
class SampleCodeSmokeTest {
    @Test
    fun should_tokenizeAllSamples_withFullCoverage() {
        val samples: List<Pair<String, String>> = listOf(
            "kotlin" to SampleCode.kotlin,
            "python" to SampleCode.python,
            // 鍏朵綑 26 涓瑷€甯搁噺閫愪竴鍔犲叆锛堟墦寮€ SampleCode.kt 鎸夊父閲忓悕琛ュ叏锛?        )
        assertTrue(samples.size >= 28, "搴旇鐩栧叏閮ㄩ瑙堟牱渚?)
        for ((lang, code) in samples) {
            val tokens = LanguageRegistry.getOrPlain(lang).tokenize(code)
            assertEquals(code, tokens.joinToString("") { it.text }, "lang=$lang 瀛楃鍏ㄨ鐩栫牬鍧?)
        }
    }
}
```

锛堟墽琛屾椂鎵撳紑 `codehighlight-preview/src/commonMain/kotlin/com/hrm/codehigh/preview/data/SampleCode.kt`锛屾寜瀹為檯甯搁噺鍚嶈ˉ鍏?28 椤规竻鍗曪紱鑻ヤ釜鍒父閲忓悕闈炶瑷€鍚嶏紝鐢ㄦ敞閲婃爣娉ㄦ槧灏勩€傦級鍒犻櫎 composeApp 鍗犱綅娴嬭瘯鏂囦欢銆?
- [ ] **Step 3: 鏂囨。淇**

HIGHLIGHTER_COVERAGE_ANALYSIS.md锛堟寜琛屽彿瀹氫綅锛岄€愰」鏇存锛夛細
- :366 `IncrementalHighlighter` 鎻忚堪 internal 鈫?`public`锛?parser 灞傛祦寮忚В鏋愭帴鍙ｏ紝鍏紑"锛夛紱
- :373 鍒犻櫎 `AstDiffEngine锛坕nternal锛塦 鏉＄洰锛堝叏浠撳簱涓嶅瓨鍦級锛?- :35 `CodeAst` internal 鈫?public锛?- :461-466 鍓创鏉挎弿杩版敼涓?鍩轰簬 Compose `LocalClipboard`/`ClipEntry`锛圱10 杩佺Щ鍚庣幇鐘讹級"锛?- :486-489 娴嬭瘯娓呭崟鏀逛负鐪熷疄鏂囦欢锛坧arser锛欿otlinLexerTest銆丒scapeBoundsTest銆丩exerContractTest銆丩anguageCoverageTest銆丒xtendedLanguageSupportTest銆両ncrementalHighlighterTest锛況ender锛欳odeLineRenderTest銆丠ighlightedStringTest銆両nlineCodeStyleTest銆丆odeThemeTest锛沺review锛歋ampleCodeSmokeTest锛夛紝鍒犻櫎"Java/Python 绛夊悇璇█璇嶆硶鍒嗘瀽鍣ㄥ崟鍏冩祴璇?鐨勮櫄鏋勮〃杩帮紱
- :489/:523 `:code-high:jvmTest` 鈫?`:codehighlight-parser:jvmTest`锛?- :538 渚濊禆鍥句负 parser 鈫?render 鈫?preview 鈫?composeApp/androidApp 涓夊眰缁撴瀯锛?- :423/:443 "18 绉嶈瑷€" 鈫?"28 绉嶈瑷€"銆?
README.md锛?- 寰界珷锛欿otlin `2.3.20`銆丆MP `1.10.3`銆乵inSdk 寰界珷閾炬帴 `api?level=23`锛?- grep `CodeToken(|InlineCodeSize(|maxVisibleLines` 鍚屾 2.0.0 API 鍙樻洿绀轰緥锛圕odeToken 鏋勯€犵涓夊弬涓?source銆乵axVisibleLines 榛樿 500銆両nlineCodeSize 瀛楁 widthPx/heightPx锛夛紱
- 娴嬭瘯鍛戒护绔犺妭纭 `./gradlew test` 琛ㄨ堪涓庢ā鍧楀悕涓€鑷淬€?
- [ ] **Step 4: 楠岃瘉** `.\gradlew.bat :codehighlight-preview:jvmTest --console=plain`

- [ ] **Step 5: Commit** `ci: 澧炲姞 PR/push 娴嬭瘯娴佹按绾匡紱淇鏂囨。涓庝唬鐮佷笉绗︼紱preview 鏍蜂緥鐑熷洷娴嬭瘯`

---

### Task 14: 缁堥獙

- [ ] **Step 1: 鍏ㄩ噺 JVM 娴嬭瘯** `.\gradlew.bat jvmTest --console=plain`锛堝叏妯″潡锛? 澶辫触锛?- [ ] **Step 2: 鍏?target 缂栬瘧** `.\gradlew.bat :codehighlight-parser:assemble :codehighlight-render:assemble :composeApp:assemble :androidApp:assembleDebug --console=plain`
- [ ] **Step 3: 鏄庣‘宸茬煡闄愬埗骞跺啓鍏?README锛堢畝鐭竴鑺傦級**锛氭墜鍐?澹版槑寮忚瘝娉曞櫒鍙岃建骞跺瓨锛堝悗缁敹鏁涙柟鍚戯級锛汣odeBlock 鍐呴儴浠嶄负闈炴噿鍔犺浇 Column锛堝凡閫氳繃榛樿 500 琛屼笂闄?+ 鎶樺彔鎺у埗鎴愭湰锛孡azyColumn 鐣欏緟涓撻」锛夈€?- [ ] **Step 4: 缁堥獙 Commit锛堝鏈夐仐婕忓井璋冿級** + 姹囨€绘姤鍛婏紙淇娓呭崟 vs 璇勫鏉＄洰瀵圭収锛?
---

## 鑷煡璁板綍

- 瑕嗙洊瀵圭収锛氳瘎瀹?#1鈫扵2锛?2鈫扵3/T4锛?3鈫扵6锛?4鈫扵3/T4/T5锛?5鈫扵3锛?6鈫扵1锛?7鈫扵4锛?8/#9鈫扵3锛?10鈫扵1锛?11锛堝弻杞ㄦ敹鏁涳級鈫掔敤鎴峰喅绛栬烦杩囷紱#12鈫扵4锛?13鈫扵3锛?14鈫扵3锛?15鈫扵5锛?16鈫扵3/T4锛?17鈫扵2锛?18鈫扵3锛?19鈫扵4锛坈ountLinesBefore 淇濈暀 O(n)锛屾潈琛¤褰曪級锛?20鈫扵10銆俽ender锛歋1鈫扵10锛汼2鈫扵10锛汼3鈫扵10锛堝紓姝?榛樿涓婇檺锛汱azyColumn 璺宠繃锛夛紱S4鈫扵8锛汼5鈫扵10锛汳1鈫扵10锛汳2鈫扵9锛汳3鈫扵8锛汳4鈫扵11锛汳5鈫扵11锛汳6鈫扵8锛汳7鈫扵10锛汳8鈫扵10锛汱1鈫扵11锛汱2鈫扵11锛汱3鈫扵11锛汱4鈫扵11锛汱5鈫扵11锛汱6鈫扵11锛汱7鈫扵9/T10銆傛瀯寤?娴嬭瘯锛?鈫扵13锛?鈫扵13锛?鈫扵7锛?鈫扵7锛?鈫扵4/T7锛?鈫扵12锛?/8鈫扵12锛?鈫扵12锛?0鈫扵1锛堟浠ｇ爜鍒犻櫎锛?T7锛?1/12鈫扵8锛?3鈫扵7锛?4鈫扵13锛?5/16鈫扵13銆?- 绫诲瀷涓€鑷存€э細CodeToken(type, range, source) 鍏ㄨ鍒掔粺涓€锛沀pdateResult -1 璇箟鍦?T4 瀹氫箟銆乀10 娑堣垂锛汣odeLineKind 鍦?T8 杩佺Щ鑷?theme 鍖咃紝T9/T10 寮曠敤涓€鑷淬€?