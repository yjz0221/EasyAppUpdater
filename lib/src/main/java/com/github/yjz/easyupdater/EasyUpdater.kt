package com.github.yjz.easyupdater

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.yjz.easyupdater.interfaces.IUpdateParser
import com.github.yjz.easyupdater.interfaces.IUpdateUI
import com.github.yjz.easyupdater.model.UpdateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpRetryException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets


/**
 * 作者:yjz
 * 创建日期：2025/11/29
 * 描述: 核心实现类
 */
class EasyUpdater private constructor(
    private val activity: FragmentActivity,
    private val checkUrl: String,
    private val parser: IUpdateParser,
    private val headers: Map<String, String>?,
    private val uiStrategy: IUpdateUI,
    private val httpMethod: String,   // 请求方法
    private val requestBody: String?, // 请求体
    private val formParams: Map<String, String>? // 表单类型的参数
) {

    class Builder(private val activity: FragmentActivity) {
        private var checkUrl: String = ""
        private var parser: IUpdateParser? = null
        private var headers: Map<String, String>? = null
        private var uiStrategy: IUpdateUI? = null
        private var httpMethod: String = "GET" // 默认为 GET
        private var requestBody: String? = null
        private var formParams: Map<String, String>? = null


        fun checkUrl(url: String) = apply {
            this.checkUrl = url
        }

        fun jsonParser(parser: IUpdateParser) = apply {
            this.parser = parser
        }

        fun headers(headers: Map<String, String>) = apply {
            this.headers = headers
        }

        // 允许设置自定义 UI
        fun uiStrategy(strategy: IUpdateUI) = apply {
            this.uiStrategy = strategy
        }

        // 设置请求方法
        fun method(method: String) = apply {
            this.httpMethod = method.uppercase()
        }


        /**
         * POST JSON
         */
        fun postJson(jsonBody: String) = apply {
            this.httpMethod = "POST"
            this.requestBody = jsonBody
            this.formParams = null // 互斥清除
        }

        /**
         * POST 表单
         */
        fun postForm(params: Map<String, String>) = apply {
            this.httpMethod = "POST"
            this.formParams = params
            this.requestBody = null // 互斥清除
        }


        fun build(): EasyUpdater {
            require(checkUrl.isNotEmpty()) { "Check URL required" }
            require(parser != null) { "Parser required" }

            // 如果用户没传 UI 策略，就使用默认的
            val finalUI = uiStrategy ?: DefaultUpdateUI(activity)

            return EasyUpdater(
                activity,
                checkUrl,
                parser!!,
                headers,
                finalUI,
                httpMethod,
                requestBody,
                formParams
            )
        }
    }


    /**
     * 开始检查更新
     *
     * @param isManual 是否是手动检查 (默认 false)
     * - true: 无更新或出错时，会执行回调或显示默认提示
     * - false: 静默模式，仅在有更新时弹窗
     * @param onNoUpdate (可选) 当检测到没有更新时的回调。
     * - 传入此回调后，库将不再弹出默认的"当前已是最新版本"Toast
     * @param onError (可选) 当检查失败时的回调。
     * - 传入此回调后，库将不再弹出默认错误提示
     */
    fun check(
        isManual: Boolean = false,
        onNoUpdate: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        // 如果是手动检查，立即显示加载框
        if (isManual) {
            uiStrategy.showCheckLoading()
        }

        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val conn = URL(checkUrl).openConnection() as HttpURLConnection
                conn.requestMethod = httpMethod
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                // 1. 设置通用 Header
                headers?.forEach { (k, v) ->
                    conn.setRequestProperty(k, v)
                }

                // 2. 处理发送数据 (POST Body)
                val bodyBytes: ByteArray? = when {
                    // 场景 A: JSON Body
                    !requestBody.isNullOrEmpty() -> {
                        if (headers?.keys?.none { it.equals("Content-Type", true) } == true) {
                            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                        }

                        requestBody.toByteArray(StandardCharsets.UTF_8)
                    }

                    // 场景 B: 表单参数 (Form)
                    !formParams.isNullOrEmpty() -> {
                        if (headers?.keys?.none { it.equals("Content-Type", true) } == true) {
                            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        }
                        val sb = StringBuilder()
                        var i = 0
                        for ((k, v) in formParams) {
                            if (i > 0) sb.append("&")
                            sb.append(URLEncoder.encode(k, "UTF-8"))
                            sb.append("=")
                            sb.append(URLEncoder.encode(v, "UTF-8"))
                            i++
                        }
                        sb.toString().toByteArray(StandardCharsets.UTF_8)
                    }
                    else -> null
                }

                // 3. 写入数据
                if (bodyBytes != null) {
                    conn.doOutput = true
                    conn.outputStream.use { it.write(bodyBytes) }
                }

                // 4. 获取响应
                val responseCode = conn.responseCode
                if (responseCode == 200) {
                    val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                    try {
                        val info = parser.parse(jsonStr)
                        withContext(Dispatchers.Main) {
                            // 请求成功，在处理结果前，先关闭加载框
                            if (isManual) {
                                uiStrategy.dismissCheckLoading()
                            }

                            if (info.hasUpdate) {
                                // === 有更新 ===
                                uiStrategy.showUpdateDialog(
                                    info = info,
                                    onUpdate = { checkPermissionAndDownload(info) },
                                    onCancel = {}
                                )
                            } else {
                                // === 无更新 ===
                                if (onNoUpdate != null) {
                                    onNoUpdate() // 用户自定义处理
                                } else if (isManual) {
                                    uiStrategy.showToast(activity.getString(R.string.easy_upd_already_latest)) // 默认提示
                                }
                            }
                        }
                    } catch (e: Exception) {
                        throw e
                    }
                } else {
                    // HTTP 错误 (非 200)
                    throw IOException("服务器返回错误: $responseCode")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    // 发生异常，先关闭加载框，再报错误
                    if (isManual) {
                        uiStrategy.dismissCheckLoading()
                    }

                    // === 发生错误 ===
                    if (onError != null) {
                        onError(e) // 用户自定义处理
                    } else if (isManual) {
                        val errorMsg = activity.getString(R.string.easy_upd_check_error) + ": ${e.message}"
                        uiStrategy.showError(e, errorMsg)
                    }
                }
            }
        }
    }

    private fun checkPermissionAndDownload(info: UpdateEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!activity.packageManager.canRequestPackageInstalls()) {
                // 委托给 UI 策略
                uiStrategy.showPermissionDialog(
                    onGoToSetting = {
                        // 库负责具体的跳转逻辑
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:${activity.packageName}"))
                        activity.startActivity(intent)
                    },
                    onCancel = {
                        // 用户取消了权限申请，流程结束
                    }
                )
                return
            }
        }
        downloadApk(info)
    }

    private fun downloadApk(info: UpdateEntity) {
        uiStrategy.showDownloadProgress(0)

        activity.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. 确定目标文件路径
                val file = File(activity.externalCacheDir, "update_v${info.versionName}.apk")

                // 2. 检查文件是否已存在且有效
                if (file.exists()) {
                    if (isApkValid(file)) {
                        // 文件存在且完整，直接跳转安装，无需下载
                        withContext(Dispatchers.Main) {
                            uiStrategy.dismissDownloadProgress()
                            installApk(file)
                        }
                        return@launch
                    } else {
                        // 文件存在但损坏（可能是上次没下完），删除旧文件
                        file.delete()
                    }
                }

                // 3. 开始下载流程
                val conn = URL(info.downloadUrl).openConnection() as HttpURLConnection
                conn.requestMethod = "GET" // 下载通常用GET
                conn.connectTimeout = 10000
                // 如果下载需要 Header，加上
                headers?.forEach { (k, v) -> conn.setRequestProperty(k, v) }

                val totalLen = conn.contentLength
                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { len = it } != -1) {
                            output.write(buffer, 0, len)
                            downloaded += len
                            if (totalLen > 0) {
                                val progress = (downloaded * 100 / totalLen).toInt()
                                withContext(Dispatchers.Main) {
                                    uiStrategy.showDownloadProgress(progress)
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    uiStrategy.dismissDownloadProgress()
                    // 下载完成后再次校验一下（防止网络劫持导致下载了错误的文件）
                    if (isApkValid(file)) {
                        installApk(file)
                    } else {
                        val exception = IOException("Check Valid Failed")
                        uiStrategy.showError(exception, activity.getString(R.string.easy_upd_parse_error))
                        file.delete()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    uiStrategy.dismissDownloadProgress()
                    uiStrategy.showError(e, activity.getString(R.string.easy_upd_download_error))
                }
            }
        }
    }

    /**
     * 检查 APK 文件是否完整有效
     * 原理：尝试解析 APK 的包信息，如果解析失败返回 null，说明文件损坏或未下载完
     */
    private fun isApkValid(file: File): Boolean {
        return try {
            val pm = activity.packageManager
            val info = pm.getPackageArchiveInfo(file.absolutePath, 0)
            info != null
        } catch (e: Exception) {
            false
        }
    }
    private fun installApk(file: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        val uri: Uri
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            uri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.easyupdater.provider",
                file
            )
        } else {
            uri = Uri.fromFile(file)
        }

        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        activity.startActivity(intent)
    }
}