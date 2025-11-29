package com.github.yjz.easyupdater


import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.widget.Toast
import com.github.yjz.easyupdater.interfaces.IUpdateUI
import com.github.yjz.easyupdater.model.UpdateEntity


/**
 * 作者:yjz
 * 创建日期：2025/11/29
 * 描述: 默认 UI 实现：使用系统 AlertDialog 和 ProgressDialog
 */
class DefaultUpdateUI(private val context: Context) : IUpdateUI {

    private var progressDialog: ProgressDialog? = null

    override fun showUpdateDialog(info: UpdateEntity, onUpdate: () -> Unit, onCancel: () -> Unit) {
        val title = context.getString(R.string.easy_upd_new_version, info.versionName)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(info.content)
            .setCancelable(!info.isForce)
            .setPositiveButton(R.string.easy_upd_update_now) { _, _ -> onUpdate() }
            .apply {
                if (!info.isForce) {
                    setNegativeButton(R.string.easy_upd_later) { _, _ -> onCancel() }
                }
            }
            .show()
    }

    override fun showPermissionDialog(onGoToSetting: () -> Unit, onCancel: () -> Unit) {
        AlertDialog.Builder(context)
            .setTitle(R.string.easy_upd_perm_title)
            .setMessage(R.string.easy_upd_perm_msg)
            .setPositiveButton(R.string.easy_upd_go_settings) { _, _ -> onGoToSetting() }
            .setNegativeButton(R.string.easy_upd_cancel) { _, _ -> onCancel() }
            .show()
    }

    override fun showDownloadProgress(progress: Int) {
        if (progressDialog == null) {
            @Suppress("DEPRECATION")
            progressDialog = ProgressDialog(context).apply {
                setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
                setTitle(context.getString(R.string.easy_upd_downloading))
                setCancelable(false)
                show()
            }
        }
        progressDialog?.progress = progress
    }

    override fun dismissDownloadProgress() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    override fun showError(e: Exception, msg: String) {
        // 默认实现简单吐个司或者弹个窗，也可以留空
    }

    override fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}