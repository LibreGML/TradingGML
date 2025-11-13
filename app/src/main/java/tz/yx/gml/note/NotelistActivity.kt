package tz.yx.gml.note

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import tz.yx.gml.R
import tz.yx.gml.databinding.ActivityListBinding
import tz.yx.gml.databinding.DialogRenameBinding
import tz.yx.gml.databinding.ItemFileBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class NotelistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityListBinding
    private lateinit var fileList: List<File>
    private lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.notelist) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        loadFileList()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadFileList()
        fileAdapter.notifyDataSetChanged()
    }

    private fun loadFileList() {
        val notesDir = File(filesDir, "Note")
        if (!notesDir.exists()) {
            notesDir.mkdirs()
        }
        
        fileList = notesDir.listFiles { file ->
            file.isFile && file.name.endsWith(".md")
        }?.toList() ?: emptyList()
        
        // 更新空状态视图
        binding.emptyView.visibility = if (fileList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(fileList) { file ->
            // 点击文件项打开编辑器
            openFileInEditor(file)
        }
        
        binding.fileRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotelistActivity)
            adapter = fileAdapter
        }
    }

    private fun openFileInEditor(file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val intent = Intent(this, NoteActivity::class.java).apply {
                data = fileUri
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFileMenu(view: View, file: File) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.file_item_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_share -> {
                    shareFile(file)
                    true
                }
                R.id.action_rename -> {
                    showRenameDialog(file)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmation(file)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun shareFile(file: File) {
        try {
            val fileUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/markdown"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "分享文件"))
        } catch (e: Exception) {
            Toast.makeText(this, "分享失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameDialog(file: File) {
        val dialogBinding = DialogRenameBinding.inflate(layoutInflater)
        val editText = dialogBinding.renameEditText
        
        // 设置当前文件名（去掉.md扩展名）
        val currentName = if (file.name.endsWith(".md")) {
            file.name.substring(0, file.name.length - 3)
        } else {
            file.name
        }
        editText.setText(currentName)
        editText.selectAll()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("重命名文件")
            .setView(dialogBinding.root)
            .setPositiveButton("确定") { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty()) {
                    renameFile(file, newName)
                }
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun renameFile(file: File, newName: String) {
        val newNameWithExtension = if (newName.endsWith(".md")) newName else "$newName.md"
        val newFile = File(file.parentFile, newNameWithExtension)
        
        if (file.name != newNameWithExtension) {
            if (newFile.exists()) {
                Toast.makeText(this, "文件名已存在", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (file.renameTo(newFile)) {
                loadFileList()
                fileAdapter.updateFileList(fileList)
                Toast.makeText(this, "重命名成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "重命名失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmation(file: File) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除文件")
            .setMessage("确定要删除文件 \"${file.name}\" 吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                deleteFile(file)
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteFile(file: File) {
        if (file.delete()) {
            loadFileList()
            fileAdapter.updateFileList(fileList)
            Toast.makeText(this, "文件已删除", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "删除失败", Toast.LENGTH_SHORT).show()
        }
    }

    inner class FileAdapter(
        private var files: List<File>,
        private val onItemClick: (File) -> Unit
    ) : RecyclerView.Adapter<FileAdapter.FileViewHolder>() {

        fun updateFileList(newFiles: List<File>) {
            files = newFiles
            binding.emptyView.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
            val itemBinding = ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return FileViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
            holder.bind(files[position])
        }

        override fun getItemCount(): Int = files.size

        inner class FileViewHolder(private val itemBinding: ItemFileBinding) : 
            RecyclerView.ViewHolder(itemBinding.root) {
            
            fun bind(file: File) {
                itemBinding.fileNameText.text = file.name
                itemBinding.fileInfoText.text = formatFileDetails(file)
                
                // 点击整个项
                itemBinding.root.setOnClickListener {
                    onItemClick(file)
                }
                
                // 长按显示菜单
                itemBinding.root.setOnLongClickListener {
                    showFileMenu(itemBinding.moreButton, file)
                    true
                }
                
                // 点击更多按钮显示菜单
                itemBinding.moreButton.setOnClickListener {
                    showFileMenu(it, file)
                }
                
                // 点击文件图标显示菜单
                itemBinding.fileIcon.setOnClickListener {
                    showFileMenu(it, file)
                }
            }
            
            private fun formatFileDetails(file: File): String {
                val size = formatFileSize(file.length())
                val date = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                return "$size · $date"
            }
            
            private fun formatFileSize(size: Long): String {
                return when {
                    size < 1024 -> "$size B"
                    size < 1024 * 1024 -> "${String.format("%.1f", size / 1024.0)} KB"
                    else -> "${String.format("%.1f", size / (1024.0 * 1024.0))} MB"
                }
            }
        }
    }
}