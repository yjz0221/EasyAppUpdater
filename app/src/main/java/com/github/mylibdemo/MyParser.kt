package com.github.mylibdemo

import android.content.Context
import android.os.Build
import android.text.TextUtils
import org.json.JSONObject
import com.github.yjz.easyupdater.interfaces.IUpdateParser
import com.github.yjz.easyupdater.model.UpdateEntity


class MyParser(private val context: Context) : IUpdateParser {

    override fun parse(json: String): UpdateEntity {
        // 1. 解析最外层
        val rootObj = JSONObject(json)
        // 你的数据都在 "data" 字段里，需要先判空
        val data = rootObj.optJSONObject("data")

        if (data == null) {
            // 如果没有 data，说明没有更新信息，返回默认空对象
            return UpdateEntity(hasUpdate = false, isForce = false, versionName = "", content = "", downloadUrl = "")
        }

        // 2. 提取关键字段
        val serverVersionCode = data.optInt("version", 0) // 服务器版本号: 12
        val versionName = data.optString("filever", "")   // 版本名: v25101115
        val downloadUrl = data.optString("ossPath", "")   // 下载地址
        // 处理更新日志：如果是空字符串，给个默认提示
        var content = data.optString("verDesc", "")
        if (TextUtils.isEmpty(content)) {
            content = "检测到新版本 $versionName，建议立即更新以获得更好体验。"
        }

        // 处理强制更新：0 -> false, 1 -> true
        val isForce = data.optInt("isForce", 0) == 1

        // 3. 核心逻辑：比对版本号
        val localVersionCode = getLocalVersionCode()
        val hasUpdate = serverVersionCode > localVersionCode

        // 4. 返回标准实体
        return UpdateEntity(
            hasUpdate = hasUpdate,
            isForce = isForce,
            versionName = versionName,
            content = content,
            downloadUrl = downloadUrl,
            // (可选) 如果你想把 "filesize": "154132711" 传给 UI 层显示，可以放在 extra 里
            // extra = data.optString("filesize")
        )
    }


    /**
     * 获取本地 App 版本号 (兼容处理)
     */
    private fun getLocalVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}