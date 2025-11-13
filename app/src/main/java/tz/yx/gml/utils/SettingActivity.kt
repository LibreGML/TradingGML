package tz.yx.gml.utils

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import tz.yx.gml.databinding.SettingBinding

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: SettingBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fingerprintManager: FingerprintManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化组件
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        fingerprintManager = FingerprintManager(this)

        // 设置标题
        title = "应用设置"

        // 初始化开关状态
        initSwitches()

        // 设置开关监听器
        setupSwitchListeners()
    }

    private fun initSwitches() {
        // 从SharedPreferences中读取保存的设置
        val passwordFingerprintEnabled = sharedPreferences.getBoolean("password_fingerprint_enabled", false)
        val noteFingerprintEnabled = sharedPreferences.getBoolean("note_fingerprint_enabled", false)

        // 设置开关状态
        binding.passwordFingerprintSwitch.isChecked = passwordFingerprintEnabled
        binding.noteFingerprintSwitch.isChecked = noteFingerprintEnabled

        // 检查设备是否支持指纹识别，如果不支持则禁用开关
        if (!fingerprintManager.isFingerprintAvailable()) {
            binding.passwordFingerprintSwitch.isEnabled = false
            binding.noteFingerprintSwitch.isEnabled = false
            Toast.makeText(this, "您的设备不支持指纹哦~", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSwitchListeners() {
        // 密码管理器指纹开关监听
        binding.passwordFingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!fingerprintManager.isFingerprintAvailable() && isChecked) {
                binding.passwordFingerprintSwitch.isChecked = false
                Toast.makeText(this, "您的设备不支持指纹哦~", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            // 保存设置到SharedPreferences
            with(sharedPreferences.edit()) {
                putBoolean("password_fingerprint_enabled", isChecked)
                apply()
            }
        }

        // 临时笔记本指纹开关监听
        binding.noteFingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!fingerprintManager.isFingerprintAvailable() && isChecked) {
                binding.noteFingerprintSwitch.isChecked = false
                Toast.makeText(this, "您的设备不支持指纹哦~", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }

            // 保存设置到SharedPreferences
            with(sharedPreferences.edit()) {
                putBoolean("note_fingerprint_enabled", isChecked)
                apply()
            }
        }
    }
}