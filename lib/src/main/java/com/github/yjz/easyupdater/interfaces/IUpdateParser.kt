package com.github.yjz.easyupdater.interfaces

import com.github.yjz.easyupdater.model.UpdateEntity

/**
 * 作者:yjz
 * 创建日期：2025/11/29
 * 描述: 解析器接口：由使用者实现，将后端返回的任意 JSON 转为 UpdateEntity
 */
interface IUpdateParser {
    fun parse(json: String): UpdateEntity
}