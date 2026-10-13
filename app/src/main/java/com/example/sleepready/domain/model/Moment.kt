package com.example.sleepready.domain.model

/**
 * 时光记录领域模型。所有图片以应用私有目录的绝对路径形式保存，
 * 仓库层负责从用户选中的 Uri 复制到 filesDir/moments/<uuid>.jpg。
 */
data class Moment(
    val id: Long,
    val title: String,
    val body: String?,
    /** 用户填的事件时间，可为空。 */
    val eventTimeMillis: Long?,
    val imagePaths: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)
