package tz.yx.gml.passwd

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import tz.yx.gml.R
import tz.yx.gml.databinding.ActivityPasswdBinding
import tz.yx.gml.databinding.DialogExportOptionsBinding
import tz.yx.gml.databinding.DialogPasswordDetailBinding
import tz.yx.gml.databinding.DialogPasswordEditBinding
import tz.yx.gml.databinding.PasswdItemBinding
import tz.yx.gml.utils.FingerprintManager
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.view.MotionEvent

class PasswdActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPasswdBinding
    private val gson = Gson()
    private val passwordList = mutableListOf<PasswordItem>()
    private val filteredPasswordList = mutableListOf<PasswordItem>()
    private lateinit var passwordAdapter: PasswordAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fingerprintManager: FingerprintManager
    private var isFingerprintAuthenticated = false

    private lateinit var selectFileLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPasswdBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 初始化文件选择器
        selectFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { importPasswordsFromFile(it) }
        }

        // 初始化指纹管理器和SharedPreferences
        fingerprintManager = FingerprintManager(this)
        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)

        // 检查是否需要指纹解锁
        checkFingerprintUnlock()
    }

    /**
     * 检查是否需要指纹解锁
     */
    private fun checkFingerprintUnlock() {
        // 使用与SettingActivity中相同的键值
        val isFingerprintEnabled = sharedPreferences.getBoolean("password_fingerprint_enabled", false)
        if (isFingerprintEnabled && fingerprintManager.isFingerprintAvailable() && !isFingerprintAuthenticated) {
            showFingerprintAuthentication()
        } else {
            initializeActivity()
        }
    }

    /**
     * 显示指纹认证对话框
     */
    private fun showFingerprintAuthentication() {
        fingerprintManager.showFingerprintPrompt(
            this,
            "芝麻开门哈哈～",
            onSuccess = {
                isFingerprintAuthenticated = true
                initializeActivity()
            },
            onFailure = {
                // 认证失败则返回MainActivity
                val intent = android.content.Intent(this, tz.yx.gml.homefrag.MainActivity::class.java)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        )
    }

    /**
     * 初始化Activity内容
     */
    private fun initializeActivity() {
        setupRecyclerView()
        loadPasswords()
        setupClickListeners()
        setupSearchListener()
        updateExportButtonState()
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun setupRecyclerView() {
        passwordAdapter = PasswordAdapter()
        binding.passwordRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.passwordRecyclerView.adapter = passwordAdapter
    }

    private fun setupClickListeners() {
        binding.addPasswordFab.setOnClickListener {
            showEditDialog(null)
        }

        binding.exportbtn.setOnClickListener {
            showExportOptionsDialog()
        }

        // 添加导入按钮点击监听器
        binding.importbtn.setOnClickListener {
            selectFileForImport()
        }
    }

    private fun setupSearchListener() {
        binding.searchPwd.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterPasswords(s?.toString() ?: "")
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterPasswords(query: String) {
        filteredPasswordList.clear()
        if (query.isEmpty()) {
            filteredPasswordList.addAll(passwordList)
        } else {
            filteredPasswordList.addAll(
                passwordList.filter {
                    it.platform.contains(query, ignoreCase = true) ||
                            it.account.contains(query, ignoreCase = true)
                }
            )
        }
        passwordAdapter.run { notifyDataSetChanged() }
    }

    private fun loadPasswords() {
        try {
            val sharedPref = getSharedPreferences("passwords", MODE_PRIVATE)
            val passwordJson = sharedPref.getString("password_list", "[]")
            val listType = com.google.gson.reflect.TypeToken.getParameterized(
                MutableList::class.java,
                PasswordItem::class.java
            ).type

            val loadedList: MutableList<PasswordItem> = gson.fromJson(passwordJson, listType) ?: mutableListOf()
            passwordList.clear()
            passwordList.addAll(loadedList)

            // 初始化过滤列表
            filteredPasswordList.clear()
            filteredPasswordList.addAll(passwordList)

            passwordAdapter.notifyDataSetChanged()
            updateExportButtonState()
        } catch (e: Exception) {
            Toast.makeText(this, "加载密码数据失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun savePasswords() {
        try {
            val sharedPref = getSharedPreferences("passwords", MODE_PRIVATE)
            with(sharedPref.edit()) {
                val passwordJson = gson.toJson(passwordList)
                putString("password_list", passwordJson)
                apply()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateExportButtonState() {
        binding.exportbtn.isEnabled = passwordList.isNotEmpty()
    }

    // 显示导出选项对话框
    private fun showExportOptionsDialog() {
        if (passwordList.isEmpty()) {
            Toast.makeText(this, "没有密码可导出", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogBinding = DialogExportOptionsBinding.inflate(LayoutInflater.from(this))
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnExportJson.setOnClickListener {
            dialog.dismiss()
            exportPasswordsToJson()
        }

        dialogBinding.btnExportCsv.setOnClickListener {
            dialog.dismiss()
            exportPasswordsToCsv()
        }

        dialog.show()
    }

    // 选择文件进行导入
    private fun selectFileForImport() {
        try {
            selectFileLauncher.launch("*/*")
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开文件选择器: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 从文件导入密码
    private fun importPasswordsFromFile(uri: Uri) {
        try {
            val fileName = getFileName(uri) ?: ""
            val mimeType = contentResolver.getType(uri)

            when {
                fileName.endsWith(".csv", ignoreCase = true) ||
                        mimeType?.startsWith("text/csv") == true ||
                        mimeType?.startsWith("text/comma-separated-values") == true -> {
                    importPasswordsFromCsv(uri)
                }
                fileName.endsWith(".json", ignoreCase = true) ||
                        mimeType?.startsWith("application/json") == true -> {
                    importPasswordsFromJson(uri)
                }
                else -> {
                    // 尝试根据内容判断文件类型
                    try {
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            val reader = BufferedReader(InputStreamReader(inputStream))
                            val firstLine = reader.readLine() ?: ""

                            if (firstLine.contains("{") && firstLine.contains("\"passwords\"")) {
                                importPasswordsFromJson(uri)
                            } else if (firstLine.contains(",") && firstLine.contains("平台")) {
                                importPasswordsFromCsv(uri)
                            } else {
                                // 默认尝试作为JSON处理
                                importPasswordsFromJson(uri)
                            }
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "无法识别文件格式，请确保是CSV或JSON格式", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 从JSON文件导入密码
    private fun importPasswordsFromJson(uri: Uri) {
        try {
            if (passwordList.isNotEmpty()) {
                // 显示确认对话框，提示用户导入将覆盖现有密码
                MaterialAlertDialogBuilder(this)
                    .setTitle("确认导入")
                    .setMessage("导入将会覆盖当前所有密码记录，确定要继续吗？")
                    .setPositiveButton("确认导入") { _, _ ->
                        performImportJson(uri)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                // 如果当前没有密码记录，直接导入
                performImportJson(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 执行JSON导入操作
    private fun performImportJson(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.use {
                val json = it.bufferedReader().use { reader -> reader.readText() }
                val exportData = gson.fromJson(json, PasswordExport::class.java)

                if (exportData != null) {
                    // 清空当前密码列表
                    passwordList.clear()

                    // 添加导入的密码
                    passwordList.addAll(exportData.passwords)

                    // 保存密码到SharedPreferences
                    savePasswords()

                    // 更新UI
                    filterPasswords(binding.searchPwd.text.toString())
                    updateExportButtonState()

                    Toast.makeText(this, "成功导入 ${exportData.passwords.size} 条密码记录", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "导入文件格式不正确", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Toast.makeText(this, "无法读取文件", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "导入失败：没有存储权限，请在设置中授予存储权限", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 从CSV文件导入密码
    private fun importPasswordsFromCsv(uri: Uri) {
        try {
            if (passwordList.isNotEmpty()) {
                // 显示确认对话框，提示用户导入将覆盖现有密码
                MaterialAlertDialogBuilder(this)
                    .setTitle("确认导入")
                    .setMessage("导入将会覆盖当前所有密码记录，确定要继续吗？")
                    .setPositiveButton("确认导入") { _, _ ->
                        performImportCsv(uri)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            } else {
                // 如果当前没有密码记录，直接导入
                performImportCsv(uri)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 执行CSV导入操作
    private fun performImportCsv(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            inputStream?.use {
                val reader = BufferedReader(InputStreamReader(it))
                val lines = reader.readLines()

                // 检查文件是否为空
                if (lines.isEmpty()) {
                    Toast.makeText(this, "CSV文件为空", Toast.LENGTH_LONG).show()
                    return
                }

                // 跳过标题行
                if (lines.size <= 1) {
                    Toast.makeText(this, "CSV文件格式不正确或为空", Toast.LENGTH_LONG).show()
                    return
                }

                // 检查标题行是否正确
                val headerLine = lines[0]
                val headers = parseCsvLine(headerLine)
                if (headers.size < 4 ||
                    !headers[0].unquote().trim().equals("平台", true) ||
                    !headers[1].unquote().trim().equals("账户", true) ||
                    !headers[2].unquote().trim().equals("密码", true) ||
                    !headers[3].unquote().trim().equals("备注", true)) {
                    Toast.makeText(this, "CSV文件标题行格式不正确，应为: 平台,账户,密码,备注", Toast.LENGTH_LONG).show()
                    return
                }

                // 清空当前密码列表
                passwordList.clear()

                var successCount = 0
                var errorCount = 0

                // 解析数据行
                for (i in 1 until lines.size) {
                    try {
                        val line = lines[i]
                        if (line.isBlank()) continue // 跳过空行

                        val values = parseCsvLine(line)

                        if (values.size >= 4) {
                            // 验证必要字段不为空
                            val platform = values[0].unquote().trim()
                            val account = values[1].unquote().trim()
                            val password = values[2].unquote().trim()

                            if (platform.isNotEmpty() && account.isNotEmpty() && password.isNotEmpty()) {
                                val passwordItem = PasswordItem(
                                    platform = platform,
                                    account = account,
                                    password = password,
                                    note = values[3].unquote().trim()
                                )
                                passwordList.add(passwordItem)
                                successCount++
                            } else {
                                errorCount++
                            }
                        } else {
                            errorCount++
                        }
                    } catch (e: Exception) {
                        errorCount++
                    }
                }

                // 保存密码到SharedPreferences
                savePasswords()

                // 更新UI
                filterPasswords(binding.searchPwd.text.toString())
                updateExportButtonState()

                val message = if (errorCount == 0) {
                    "成功导入 $successCount 条密码记录"
                } else {
                    "导入完成，成功 $successCount 条，失败 $errorCount 条"
                }

                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "无法读取文件", Toast.LENGTH_LONG).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "导入失败：没有存储权限，请在设置中授予存储权限", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "导入失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 解析CSV行 - 修复解析逻辑错误
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    // 处理引号
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        // 双引号表示一个引号字符
                        current.append('"')
                        i += 2 // 跳过下一个引号
                        continue
                    } else {
                        // 切换引号状态
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    // 逗号分隔符（不在引号内）
                    result.add(current.toString())
                    current.clear()
                }
                else -> {
                    current.append(char)
                }
            }
            i++
        }

        result.add(current.toString())
        return result
    }

    // 为字符串添加引号的方法
    private fun String.unquote(): String {
        return if (this.startsWith("\"") && this.endsWith("\"") && this.length >= 2) {
            this.substring(1, this.length - 1).replace("\"\"", "\"")
        } else {
            this
        }
    }

    private fun exportPasswordsToJson() {
        // 检查是否有密码条目可以导出
        if (passwordList.isEmpty()) {
            Toast.makeText(this, "没有密码可导出", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 获取用户默认下载目录
            val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // 使用带时间戳的文件名避免覆盖
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "我的密码_$timestamp.json"
            val file = File(exportDir, fileName)

            val exportData = PasswordExport(passwordList)
            file.writeText(gson.toJson(exportData))

            Toast.makeText(
                this,
                "导出至: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "导出失败：没有存储权限，请在设置中授予存储权限",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "导出失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun exportPasswordsToCsv() {
        // 检查是否有密码条目可以导出
        if (passwordList.isEmpty()) {
            Toast.makeText(this, "没有密码可导出", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // 获取用户默认下载目录
            val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // 使用带时间戳的文件名避免覆盖
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "我的密码_$timestamp.csv"
            val file = File(exportDir, fileName)

            // 构建CSV内容
            val csvContent = buildCsvContent()

            file.writeText(csvContent)

            Toast.makeText(
                this,
                "导出至: ${file.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "导出失败：没有存储权限，请在设置中授予存储权限",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "导出失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun buildCsvContent(): String {
        val csv = StringBuilder()

        // 添加标题行
        csv.append("平台,账户,密码,备注\n")

        // 添加数据行
        passwordList.forEach { item ->
            csv.append("\"${item.platform.replace("\"", "\"\"")}\",")
            csv.append("\"${item.account.replace("\"", "\"\"")}\",")
            csv.append("\"${item.password.replace("\"", "\"\"")}\",")
            csv.append("\"${item.note.replace("\"", "\"\"")}\"")
            csv.append("\n")
        }

        return csv.toString()
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        val projection = arrayOf(android.provider.OpenableColumns.DISPLAY_NAME)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }

    private fun showEditDialog(item: PasswordItem?) {
        val dialogBinding = DialogPasswordEditBinding.inflate(LayoutInflater.from(this))

        val isEditMode = item != null
        dialogBinding.dialogTitle.text = if (isEditMode) "编辑密码" else "添加密码"

        if (isEditMode) {
            item?.let {
                dialogBinding.editPlatform.setText(it.platform)
                dialogBinding.editAccount.setText(it.account)
                dialogBinding.editPassword.setText(it.password)
                dialogBinding.editNote.setText(it.note)
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.saveButton.setOnClickListener {
            val platform = dialogBinding.editPlatform.text?.toString()?.trim() ?: ""
            val account = dialogBinding.editAccount.text?.toString()?.trim() ?: ""
            val password = dialogBinding.editPassword.text?.toString()?.trim() ?: ""
            val note = dialogBinding.editNote.text?.toString()?.trim() ?: ""

            if (platform.isEmpty() || account.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "平台名称、账户和密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isEditMode) {
                item?.let { passwordItem ->
                    passwordItem.platform = platform
                    passwordItem.account = account
                    passwordItem.password = password
                    passwordItem.note = note

                    // 更新过滤列表中的项
                    val indexInFiltered = filteredPasswordList.indexOfFirst { it.id == passwordItem.id }
                    if (indexInFiltered != -1) {
                        filteredPasswordList[indexInFiltered] = passwordItem
                    }
                }
            } else {
                val newItem = PasswordItem(
                    platform = platform,
                    account = account,
                    password = password,
                    note = note
                )
                passwordList.add(newItem)

                // 只有在当前搜索条件下匹配才添加到过滤列表
                if (binding.searchPwd.text.isEmpty() ||
                    newItem.platform.contains(binding.searchPwd.text, true) ||
                    newItem.account.contains(binding.searchPwd.text, true)) {
                    filteredPasswordList.add(newItem)
                }
            }

            savePasswords()
            passwordAdapter.notifyDataSetChanged()
            updateExportButtonState()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDetailDialog(item: PasswordItem) {
        val dialogBinding = DialogPasswordDetailBinding.inflate(LayoutInflater.from(this))

        dialogBinding.detailPlatform.text = item.platform
        dialogBinding.detailAccount.text = item.account
        dialogBinding.detailPassword.text = item.password
        dialogBinding.detailNote.text = item.note

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun deletePassword(item: PasswordItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除 ${item.platform} 的密码记录吗？")
            .setPositiveButton("删除") { _, _ ->
                // 确保从两个列表中都删除项目，保持数据一致性
                passwordList.removeIf { it.id == item.id }
                filteredPasswordList.removeIf { it.id == item.id }
                savePasswords()
                passwordAdapter.notifyDataSetChanged()
                updateExportButtonState()
                Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    inner class PasswordAdapter : RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PasswordViewHolder {
            val binding = PasswdItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return PasswordViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PasswordViewHolder, position: Int) {
            if (position >= 0 && position < filteredPasswordList.size) {
                holder.bind(filteredPasswordList[position])
            }
        }

        override fun getItemCount(): Int = filteredPasswordList.size

        inner class PasswordViewHolder(
            private val binding: PasswdItemBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            fun bind(item: PasswordItem) {
                binding.platformText.text = item.platform
                binding.accountText.text = item.account
                binding.passwordText.text = item.password

                binding.editButton.setOnClickListener {
                    val popupMenu = PopupMenu(this@PasswdActivity, it)
                    popupMenu.menuInflater.inflate(R.menu.password_item_menu, popupMenu.menu)
                    popupMenu.setOnMenuItemClickListener { menuItem ->
                        when (menuItem.itemId) {
                            R.id.action_edit -> {
                                showEditDialog(item)
                                true
                            }
                            R.id.action_delete -> {
                                deletePassword(item)
                                true
                            }
                            else -> false
                        }
                    }
                    popupMenu.show()
                }

                binding.root.setOnClickListener {
                    showDetailDialog(item)
                }
            }
        }
    }
}