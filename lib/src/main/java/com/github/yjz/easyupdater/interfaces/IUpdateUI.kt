package com.github.yjz.easyupdater.interfaces

import com.github.yjz.easyupdater.model.UpdateEntity

/**
 * 作者:yjz
 * 创建日期：2025/11/29
 * 描述: UI 策略接口：实现此接口以定制自己的弹窗样式
 */
interface IUpdateUI {

    /**
     * 显示发现新版本弹窗
     * @param info 更新信息
     * @param onUpdate 用户点击"立即更新"时，你必须调用此回调
     * @param onCancel 用户点击"取消"时，你必须调用此回调
     */
    fun showUpdateDialog(info: UpdateEntity, onUpdate: () -> Unit, onCancel: () -> Unit)

    /**
     * 显示"安装权限"提示弹窗 (Android 8.0+)
     * 当应用没有安装权限时调用
     * @param onGoToSetting 用户点击"去开启"时的回调（库会自动跳转设置页）
     * @param onCancel 用户点击"取消"时的回调
     */
    fun showPermissionDialog(onGoToSetting: () -> Unit, onCancel: () -> Unit)


    /**
     * 显示下载进度
     * @param progress 当前进度 (0-100)
     */
    fun showDownloadProgress(progress: Int)

    /**
     * 下载完成或失败，关闭进度弹窗
     */
    fun dismissDownloadProgress()

    /**
     * 显示错误提示
     */
    fun showError(e: Exception,msg: String) {}


    /**
     * 显示轻量级提示 (用于显示"已是最新版"或"检查失败")
     */
    fun showToast(msg: String)
}