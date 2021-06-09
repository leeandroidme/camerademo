package com.newland.tiktok.utils

import android.content.Context
import android.os.Environment
import com.newland.tiktok.BuildConfig
import java.io.*

/**
 * @author: leellun
 * @data: 2021/6/8.
 *
 */
class FileUtils {
    companion object {
        /**
         * 写文件
         *
         * @param path
         * @param content
         * @param append  是否追加
         * @return
         */
        fun writeStr(path: String?, content: String?, append: Boolean): Boolean {
            return try {
                // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
                val writer = FileWriter(path, append)
                writer.write(content)
                writer.close()
                true
            } catch (e: IOException) {
                e.printStackTrace()
                false
            }
        }

        /**
         * 读取指定路径文本内容
         *
         * @param path
         * @return 文件内容
         * @author LiuLun
         * @Time 2015年11月9日下午5:58:05
         */
        fun readStr(path: String?): String? {
            return try {
                val file = File(path)
                if (!file.exists()) return null
                val fis = FileInputStream(file)
                val br = BufferedReader(InputStreamReader(fis, "gbk"))
                val sb = StringBuilder()
                var s: String? = null
                while (br.readLine().also { s = it } != null) {
                    sb.append(s)
                }
                br.close()
                fis.close()
                sb.toString()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        fun readStr(inputStream: InputStream): String? {
            return try {
                val br = BufferedReader(InputStreamReader(inputStream, "utf-8"))
                val sb = StringBuilder()
                var s: String? = null
                while (br.readLine().also { s = it } != null) {
                    sb.append(s)
                }
                br.close()
                inputStream.close()
                sb.toString()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }

        /**
         * 删除文件
         */
        fun deleteFile(file: File) {
            if (!file.exists()) return
            if (file.isDirectory) {
                for (f in file.listFiles()) {
                    deleteFile(f)
                }
            } else {
                file.delete()
            }
        }

        fun write(ex: Throwable) {
            val sb = StringBuffer()
            val writer: Writer = StringWriter()
            val printWriter = PrintWriter(writer)
            ex.printStackTrace(printWriter)
            var cause = ex.cause
            while (cause != null) {
                cause.printStackTrace(printWriter)
                cause = cause.cause
            }
            printWriter.close()
            val result = writer.toString()
            sb.append(result)
            val exStr = sb.toString()
            val timestamp = System.currentTimeMillis()
            val time = System.currentTimeMillis().toString() + ""
            val fileName = "crash-$time-$timestamp.log"
            writeStr(fileName, exStr, true);
        }
        public fun getExterPath(context:Context,file: String): String {
            return "${context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)?.path}/${file}"
        }
    }
}