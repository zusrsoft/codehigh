package com.hrm.codehigh.theme

/** 代码行渲染种类：普通/高亮/diff 各形态 */
enum class CodeLineKind {
    NORMAL, HIGHLIGHTED, DIFF_ADDED, DIFF_REMOVED, DIFF_META_HEADER, DIFF_META_HUNK
}
