package com.github.yjz.easyupdater.model

/**
 * 作者:yjz
 * 创建日期：2025/11/29
 * 描述:库内部使用的标准数据模型
 */
data class UpdateEntity(
    val hasUpdate: Boolean,      // 是否需要更新
    val isForce: Boolean,        // 是否强制更新
    val versionName: String,     // 版本名称 (如 "1.2.0")
    val content: String,         // 更新日志
    val downloadUrl: String,      // APK下载链接
    val extra: Any? = null       // 额外数据，用于透传给 UI 层
)