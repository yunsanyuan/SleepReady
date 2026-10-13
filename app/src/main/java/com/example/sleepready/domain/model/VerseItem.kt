package com.example.sleepready.domain.model

/**
 * 拾句板块条目领域模型。
 *
 *  - [title]   诗题，必填
 *  - [author]  作者
 *  - [dynasty] 朝代（先秦 / 汉 / 唐 / 宋 / 元 / 明 / 清 / 近现代 / 其他）
 *  - [content] 全文（多行）
 *  - [notes]   赏析 / 注解（可选）
 *  - [createdAt] 收录时间
 */
data class VerseItem(
    val id: Long,
    val title: String,
    val author: String,
    val dynasty: String,
    val content: String,
    val notes: String?,
    val createdAt: Long
) {
    companion object {
        /** 朝代筛选下拉的标准选项（顺序 = UI 显示顺序）。 */
        val DYNASTIES: List<String> = listOf(
            "全部", "先秦", "汉", "魏晋", "南北朝", "唐", "宋", "元", "明", "清", "近现代", "其他"
        )
    }
}
