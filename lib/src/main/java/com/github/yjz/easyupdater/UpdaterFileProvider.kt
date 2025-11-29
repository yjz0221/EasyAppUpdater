package com.github.yjz.easyupdater


import androidx.core.content.FileProvider


/**
 * 作者:yjz
 * 创建日期：2025/11/29
 * 描述: 这是一个空类，唯一的目的就是为了在 Manifest 中注册，防止与宿主 App 的 Provider 冲突。
 */
class UpdaterFileProvider : FileProvider()