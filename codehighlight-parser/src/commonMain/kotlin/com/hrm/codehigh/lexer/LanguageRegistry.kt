package com.hrm.codehigh.lexer

/**
 * 语言词法分析器注册表，管理语言标识符到 Lexer 的映射。
 * 对外公开 get() 和 register() 方法，其余实现细节标记为 internal。
 *
 * 默认语言在类加载时注册一次（饿汉式，JVM 类初始化机制保证线程安全；
 * JS 平台单线程无影响）。
 * [register] 与 [registerAlias] 供外部扩展注入自定义语言，
 * 但并发调用非线程安全，建议在应用初始化阶段（单线程）完成。
 */
object LanguageRegistry {

    /** 语言标识符到 Lexer 的映射表，internal 不对外暴露 */
    internal val registry: MutableMap<String, Lexer> = mutableMapOf()

    /** 别名映射表，internal 不对外暴露 */
    internal val aliases: MutableMap<String, String> = mutableMapOf()

    init {
        registerDefaults()
    }

    /**
     * 注册语言词法分析器。
     *
     * @param lang 语言标识符（小写）
     * @param lexer 对应的词法分析器
     */
    fun register(lang: String, lexer: Lexer) {
        registry[lang.lowercase()] = lexer
    }

    /**
     * 注册语言别名。
     *
     * @param alias 别名（如 "js"）
     * @param canonical 规范名称（如 "javascript"）
     */
    fun registerAlias(alias: String, canonical: String) {
        aliases[alias.lowercase()] = canonical.lowercase()
    }

    /**
     * 按语言标识符获取词法分析器。
     * 支持别名查找，未知语言返回 null。
     *
     * @param lang 语言标识符
     * @return 对应的词法分析器，未找到时返回 null
     */
    fun get(lang: String): Lexer? {
        val normalized = lang.lowercase().trim()
        val canonical = aliases[normalized] ?: normalized
        return registry[canonical]
    }

    /**
     * 获取词法分析器，未知语言降级为 PlainTextLexer。
     * 对外公开，供渲染层使用。
     */
    fun getOrPlain(lang: String): Lexer {
        return get(lang) ?: PlainTextLexer
    }

    /**
     * 注册所有内置语言词法分析器。
     * 由 object 初始化块在类加载时调用一次，标记为 internal。
     */
    internal fun registerDefaults() {
        // 系统语言
        register("kotlin", KotlinLexer)
        registerAlias("kt", "kotlin")
        registerAlias("kts", "kotlin")

        register("java", JavaLexer)

        register("swift", SwiftLexer)

        // 脚本语言
        register("python", PythonLexer)
        registerAlias("py", "python")

        register("javascript", JavaScriptLexer)
        registerAlias("js", "javascript")
        registerAlias("jsx", "javascript")

        register("typescript", TypeScriptLexer)
        registerAlias("ts", "typescript")
        registerAlias("tsx", "typescript")

        register("ruby", RubyLexer)
        registerAlias("rb", "ruby")

        register("php", PhpLexer)
        registerAlias("phtml", "php")
        registerAlias("php3", "php")
        registerAlias("php4", "php")
        registerAlias("php5", "php")

        register("dart", DartLexer)

        register("scala", ScalaLexer)
        registerAlias("sc", "scala")

        register("lua", LuaLexer)
        registerAlias("luau", "lua")

        register("haskell", HaskellLexer)
        registerAlias("hs", "haskell")

        register("elixir", ElixirLexer)
        registerAlias("ex", "elixir")
        registerAlias("exs", "elixir")

        // 系统/底层语言
        register("go", GoLexer)
        register("rust", RustLexer)
        registerAlias("rs", "rust")
        register("c", CLexer)
        register("cpp", CppLexer)
        registerAlias("c++", "cpp")
        registerAlias("cxx", "cpp")
        registerAlias("cc", "cpp")

        // 数据/配置语言
        register("sql", SqlLexer)
        register("json", JsonLexer)
        register("yaml", YamlLexer)
        registerAlias("yml", "yaml")
        register("r", RLangLexer)
        registerAlias("rscript", "r")
        register("toml", TomlLexer)
        register("dockerfile", DockerfileLexer)
        registerAlias("docker", "dockerfile")

        // 标记/样式语言
        register("bash", BashLexer)
        registerAlias("sh", "bash")
        registerAlias("shell", "bash")
        register("diff", DiffLexer)
        registerAlias("patch", "diff")
        register("xml", XmlLexer)
        register("html", HtmlLexer)
        registerAlias("htm", "html")
        register("css", CssLexer)
    }
}
