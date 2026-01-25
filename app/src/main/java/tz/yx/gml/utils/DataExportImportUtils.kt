package tz.yx.gml.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class ExportData(
    @SerializedName("balance") val balance: Int,
    @SerializedName("totalEarned") val totalEarned: Int,
    @SerializedName("penaltyCount") val penaltyCount: Int,
    @SerializedName("violatedOrdinances") val violatedOrdinances: Set<String>,
    @SerializedName("savingsRecords") val savingsRecords: Set<String>,
    @SerializedName("violationDetails") val violationDetails: Map<String, String>,
    @SerializedName("abstinenceCycleProgress") val abstinenceCycleProgress: Int,
    @SerializedName("abstinenceAccumulatedReward") val abstinenceAccumulatedReward: Int,
    @SerializedName("fitnessStreak") val fitnessStreak: Int,
    @SerializedName("lastFitnessDate") val lastFitnessDate: Long,
    @SerializedName("lastFixedRewardYear") val lastFixedRewardYear: Int,
    @SerializedName("lastFixedRewardMonth") val lastFixedRewardMonth: Int,
    @SerializedName("lastViolationTime") val lastViolationTime: Long,
    @SerializedName("selectedCurrency") val selectedCurrency: String,
    @SerializedName("timestamp") val timestamp: Long
)

class DataExportImportUtils(private val context: Context) {
    
    private val gson = Gson()
    
    /**
     * 导出数据到JSON文件
     */
    fun exportData(
        balance: Int,
        totalEarned: Int,
        penaltyCount: Int,
        violatedOrdinances: Set<String>,
        savingsRecords: Set<String>,
        violationDetails: Map<String, String>,
        abstinenceCycleProgress: Int,
        abstinenceAccumulatedReward: Int,
        fitnessStreak: Int,
        lastFitnessDate: Long,
        lastFixedRewardYear: Int,
        lastFixedRewardMonth: Int,
        lastViolationTime: Long,
        selectedCurrency: String
    ): Boolean {
        return try {
            // 获取用户默认下载目录
            val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // 使用带时间戳的文件名避免覆盖
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "莹钞数据_$timestamp.json"
            val file = File(exportDir, fileName)

            val exportData = ExportData(
                balance = balance,
                totalEarned = totalEarned,
                penaltyCount = penaltyCount,
                violatedOrdinances = violatedOrdinances,
                savingsRecords = savingsRecords,
                violationDetails = violationDetails,
                abstinenceCycleProgress = abstinenceCycleProgress,
                abstinenceAccumulatedReward = abstinenceAccumulatedReward,
                fitnessStreak = fitnessStreak,
                lastFitnessDate = lastFitnessDate,
                lastFixedRewardYear = lastFixedRewardYear,
                lastFixedRewardMonth = lastFixedRewardMonth,
                lastViolationTime = lastViolationTime,
                selectedCurrency = selectedCurrency,
                timestamp = System.currentTimeMillis()
            )
            
            file.writeText(gson.toJson(exportData))

            Toast.makeText(
                context,
                "导出至: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            
            true
        } catch (e: SecurityException) {
            Toast.makeText(
                context,
                "导出失败：没有存储权限，请在设置中授予存储权限",
                Toast.LENGTH_LONG
            ).show()
            false
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "导出失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
    
    /**
     * 从JSON文件导入数据
     */
    fun importData(filePath: String): ExportData? {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                Toast.makeText(
                    context,
                    "导入文件不存在",
                    Toast.LENGTH_SHORT
                ).show()
                return null
            }
            
            val jsonString = file.readText()
            importDataFromJsonString(jsonString)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "导入失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            null
        }
    }
    
    /**
     * 从JSON字符串导入数据
     */
    fun importDataFromJsonString(jsonString: String): ExportData? {
        return try {
            val exportData = gson.fromJson(jsonString, ExportData::class.java)
            
            Toast.makeText(
                context,
                "数据导入成功",
                Toast.LENGTH_SHORT
            ).show()
            
            exportData
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "导入失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            null
        }
    }
}