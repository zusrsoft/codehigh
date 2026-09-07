package com.hrm.codehigh.theme

/** 代码行渲染种类：普通/高亮/diff 各形态 */
enum class CodeLineKind {
    /** 普通行 */
    NORMAL,

    /** 高亮行 */
    HIGHLIGHTED,

    /** diff 新增行 */
    DIFF_ADDED,

    /** diff 删除行 */
    DIFF_REMOVED,

    /** diff 文件头行 */
    DIFF_META_HEADER,

    /** diff hunk 头行 */
    DIFF_META_HUNK
}
