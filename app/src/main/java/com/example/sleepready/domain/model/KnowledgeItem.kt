package com.example.sleepready.domain.model

/**
 * 心动板块条目领域模型（v2 之后支持多分类，v5 之后支持图片附件）。
 *
 * 数据库中 `category` 列以「逗号分隔的 storageValue」形式存储多个分类，
 * 例如 `"want_to_learn,want_to_do"`。Repository 在 ⇄ 之间负责转换。
 *
 * [imagePaths] 最多 3 张，绝对路径（filesDir/knowledge/<uuid>.jpg）。
 */
data class KnowledgeItem(
    val id: Long,
    val title: String,
    val categories: Set<KnowledgeCategory>,
    val link: String?,
    val note: String?,
    val status: KnowledgeStatus,
    val imagePaths: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        const val MAX_IMAGES = 3
    }
}
