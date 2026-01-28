package tz.yx.gml.note

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.tabs.TabLayout
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import tz.yx.gml.R
import tz.yx.gml.databinding.ActivityNoteBinding
import tz.yx.gml.utils.FingerprintManager
import java.io.*

/**
 * 笔记应用主界面
 * 提供文本编辑、文件打开、保存和列表查看功能
 */
class NoteActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNoteBinding
    private var currentUri: Uri? = null
    private var currentFileName: String = ""
    private lateinit var markwon: Markwon
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fingerprintManager: FingerprintManager
    private var isFingerprintAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        markwon = Markwon.builder(this)
            .usePlugin(TablePlugin.create(this))
            .usePlugin(TaskListPlugin.create(this))
            .usePlugin(HtmlPlugin.create())
            .build()

        ViewCompat.setOnApplyWindowInsetsListener(binding.note) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top, bottom = systemBars.bottom)
            insets
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
        val isFingerprintEnabled = sharedPreferences.getBoolean("note_fingerprint_enabled", false)
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
        intent?.data?.let { uri ->
            currentUri = uri
            openFile(uri)
        }

        setupClickListeners()
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



    private fun setupClickListeners() {
        // Tab切换监听
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> binding.viewFlipper.displayedChild = 0 // 编辑视图
                    1 -> {
                        binding.viewFlipper.displayedChild = 1 // 预览视图
                        renderMarkdown()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 保存按钮点击事件
        binding.save.setOnClickListener {
            saveFile()
        }

        // 列表按钮点击事件
        binding.list.setOnClickListener {
            val intent = Intent(this, NotelistActivity::class.java)
            startActivity(intent)
        }
    }

    private fun renderMarkdown() {
        val markdownText = binding.noteEditText.text.toString()
        markwon.setMarkdown(binding.previewTextView, markdownText)
    }

    private fun saveFile() {
        val title = binding.notetitle.text.toString().trim()
        val content = binding.noteEditText.text.toString()
        if (title.isEmpty()) {
            Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = if (title.endsWith(".md")) title else "$title.md"
        try {
            var filePath = ""
            if (currentUri != null) {
                updateFile(currentUri!!, fileName, content)
                filePath = currentUri!!.path ?: ""
            } else {
                createNewFile(fileName, content)
                filePath = currentUri?.path ?: ""
            }
            Toast.makeText(this, "文件保存成功: $filePath", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("NoteActivity", "保存文件失败", e)
            Toast.makeText(this, "保存文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun createNewFile(fileName: String, content: String) {
        val notesDir = File(filesDir, "Note")
        if (!notesDir.exists()) {
            notesDir.mkdirs()
        }

        val file = File(notesDir, fileName)
        FileWriter(file).use { writer ->
            writer.write(content)
        }

        // 更新当前文件信息
        currentFileName = fileName

        // 创建新的URI
        currentUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
    }

    private fun updateFile(uri: Uri, newFileName: String, content: String) {
        // 写入内容到文件
        contentResolver.openOutputStream(uri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(content)
            }
        }

        // 如果文件名改变，需要重命名文件
        if (newFileName != currentFileName) {
            val noteDir = File(filesDir, "Note")
            val oldFile = File(noteDir, currentFileName)
            val newFile = File(noteDir, newFileName)

            if (oldFile.exists()) {
                if (oldFile.renameTo(newFile)) {
                    // 更新当前文件信息
                    currentFileName = newFileName

                    // 更新URI
                    currentUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        newFile
                    )
                } else {
                    Toast.makeText(this, "重命名文件失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openFile(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val content = StringBuilder()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        content.append(line).append("\n")
                        line = reader.readLine()
                    }

                    // 设置内容到编辑器
                    binding.noteEditText.setText(content.toString().trimEnd())

                    // 从URI获取文件名并设置为标题
                    val fileName = getFileName(uri)
                    currentFileName = fileName
                    binding.notetitle.setText(
                        if (fileName.endsWith(".md")) {
                            fileName.substring(0, fileName.length - 3)
                        } else {
                            fileName
                        }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("NoteActivity", "打开文件失败", e)
            Toast.makeText(this, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = ""
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex("_display_name")
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
            }
        }

        if (fileName.isEmpty()) {
            fileName = uri.lastPathSegment ?: "未命名.md"
        }

        return fileName
    }
}