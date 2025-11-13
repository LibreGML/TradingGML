package tz.yx.gml.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import tz.yx.gml.R
import androidx.core.content.edit

class FingerprintManager(private val context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    companion object {
        const val PASSWORD_FINGERPRINT_KEY = "password_fingerprint_enabled"
        const val NOTE_FINGERPRINT_KEY = "note_fingerprint_enabled"
    }

    fun isPasswordFingerprintEnabled(): Boolean {
        return sharedPreferences.getBoolean(PASSWORD_FINGERPRINT_KEY, false)
    }

    fun isNoteFingerprintEnabled(): Boolean {
        return sharedPreferences.getBoolean(NOTE_FINGERPRINT_KEY, false)
    }

    fun setPasswordFingerprintEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(PASSWORD_FINGERPRINT_KEY, enabled) }
    }

    fun setNoteFingerprintEnabled(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(NOTE_FINGERPRINT_KEY, enabled)}
    }

    /**
     * 检查设备是否支持指纹识别
     */
    fun isFingerprintAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == 
                BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * 显示指纹认证对话框
     */
    fun showFingerprintPrompt(
        activity: FragmentActivity,
        title: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (!isFingerprintAvailable()) {
            Toast.makeText(context, "您的设备不支持指纹哦~", Toast.LENGTH_SHORT).show()
            onFailure()
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(context, "指纹认证失败: $errString", Toast.LENGTH_SHORT).show()
                    onFailure()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(context, "指纹认证失败，请重试", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("请验证指纹以继续")
            .setNegativeButtonText("取消")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}