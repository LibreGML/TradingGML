package tz.yx.gml.plugins

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import tz.yx.gml.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.ContentValues
import android.net.Uri
import android.provider.CallLog
import android.provider.Telephony
import android.Manifest
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class FakeActivity : AppCompatActivity() {
    
    private lateinit var etCallDate: EditText
    private lateinit var etSmsDate: EditText
    private lateinit var etCallNumber: EditText
    private lateinit var etCallDuration: EditText
    private lateinit var etSmsAddress: EditText
    private lateinit var etSmsBody: EditText
    private lateinit var rgCallType: RadioGroup
    private lateinit var rgSmsType: RadioGroup
    private lateinit var tvStatus: TextView
    
    private var selectedCallDate: Calendar = Calendar.getInstance()
    private var selectedSmsDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fake)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        initViews()
        setupDatePicker()
        setupButtons()
    }
    
    private fun initViews() {
        etCallDate = findViewById(R.id.et_call_date)
        etSmsDate = findViewById(R.id.et_sms_date)
        etCallNumber = findViewById(R.id.et_call_number)
        etCallDuration = findViewById(R.id.et_call_duration)
        etSmsAddress = findViewById(R.id.et_sms_address)
        etSmsBody = findViewById(R.id.et_sms_body)
        rgCallType = findViewById(R.id.rg_call_type)
        rgSmsType = findViewById(R.id.rg_sms_type)
        tvStatus = findViewById(R.id.tv_status)
    }
    
    private fun setupDatePicker() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        etCallDate.setText(dateFormat.format(selectedCallDate.time))
        etSmsDate.setText(dateFormat.format(selectedSmsDate.time))
        
        etCallDate.setOnClickListener {
            showDatePicker { year, month, dayOfMonth ->
                showTimePicker { hourOfDay, minute ->
                    selectedCallDate.set(year, month, dayOfMonth, hourOfDay, minute)
                    etCallDate.setText(dateFormat.format(selectedCallDate.time))
                }
            }
        }
        
        etSmsDate.setOnClickListener {
            showDatePicker { year, month, dayOfMonth ->
                showTimePicker { hourOfDay, minute ->
                    selectedSmsDate.set(year, month, dayOfMonth, hourOfDay, minute)
                    etSmsDate.setText(dateFormat.format(selectedSmsDate.time))
                }
            }
        }
    }
    
    private fun setupButtons() {
        val btnAddCallLog = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_call_log)
        val btnAddSms = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_add_sms)
        
        btnAddCallLog.setOnClickListener {
            addCallLog()
        }
        
        btnAddSms.setOnClickListener {
            addSms()
        }
    }
    
    private fun showDatePicker(onDateSet: (Int, Int, Int) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                onDateSet(year, month, dayOfMonth)
            },
            selectedCallDate.get(Calendar.YEAR),
            selectedCallDate.get(Calendar.MONTH),
            selectedCallDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }
    
    private fun showTimePicker(onTimeSet: (Int, Int) -> Unit) {
        val timePickerDialog = android.app.TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                onTimeSet(hourOfDay, minute)
            },
            selectedCallDate.get(Calendar.HOUR_OF_DAY),
            selectedCallDate.get(Calendar.MINUTE),
            true
        )
        timePickerDialog.show()
    }
    
    private fun addCallLog() {
        val number = etCallNumber.text.toString().trim()
        val durationStr = etCallDuration.text.toString().trim()
        
        if (number.isEmpty()) {
            showToast("请输入电话号码")
            return
        }
        
        if (durationStr.isEmpty()) {
            showToast("请输入通话时长")
            return
        }
        
        val duration = durationStr.toLongOrNull()
        if (duration == null) {
            showToast("通话时长必须是数字")
            return
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALL_LOG) 
            != PackageManager.PERMISSION_GRANTED) {
            showToast("没有通话记录写入权限")
            return
        }
        
        try {
            val values = ContentValues().apply {
                put(CallLog.Calls.NUMBER, number)
                put(CallLog.Calls.DATE, selectedCallDate.timeInMillis)
                put(CallLog.Calls.DURATION, duration)
                
                val callType = when (rgCallType.checkedRadioButtonId) {
                    R.id.rb_outgoing -> CallLog.Calls.OUTGOING_TYPE
                    R.id.rb_incoming -> CallLog.Calls.INCOMING_TYPE
                    R.id.rb_missed -> CallLog.Calls.MISSED_TYPE
                    else -> CallLog.Calls.OUTGOING_TYPE
                }
                put(CallLog.Calls.TYPE, callType)
                put(CallLog.Calls.NEW, 0) // 0表示已读
            }
            
            val uri = contentResolver.insert(CallLog.Calls.CONTENT_URI, values)
            if (uri != null) {
                showToast("通话记录添加成功")
            } else {
                showToast("通话记录添加失败")
            }
        } catch (e: Exception) {
            showToast("添加通话记录失败: ${e.message}")
            Log.e("FakeActivity", "添加通话记录失败", e)
        }
    }
    
    private fun addSms() {
        val address = etSmsAddress.text.toString().trim()
        val body = etSmsBody.text.toString().trim()
        
        if (address.isEmpty()) {
            showToast("请输入发送方号码")
            return
        }
        
        if (body.isEmpty()) {
            showToast("请输入短信内容")
            return
        }
        
        try {
            val values = ContentValues().apply {
                put(Telephony.Sms.ADDRESS, address)
                put(Telephony.Sms.BODY, body)
                put(Telephony.Sms.DATE, selectedSmsDate.timeInMillis)
                put(Telephony.Sms.DATE_SENT, selectedSmsDate.timeInMillis)
                put("seen", 1) // 1表示已读
                put("read", 1) // 1表示已读
                put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX) // 接收的短信
                put("status", Telephony.Sms.STATUS_NONE)
                put("locked", 0) // 0表示未锁定
                put("person", 0L)
                put("protocol", 0)
                put("service_center", "")
                put("reply_path_present", 0)
                put("subject", "")
                put("thread_id", getOrCreateThreadId(address))
            }
            
            val uri = contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
            if (uri != null) {
                showToast("短信记录添加成功")
                Log.d("FakeActivity", "短信插入成功，URI: $uri")
            } else {
                showToast("短信记录添加失败")
                Log.e("FakeActivity", "短信插入返回null")
            }
        } catch (e: SecurityException) {
            showToast("权限不足，请确认已设置为默认短信应用")
            Log.e("FakeActivity", "安全异常，权限不足", e)
        } catch (e: Exception) {
            showToast("添加短信记录失败: ${e.message}")
            Log.e("FakeActivity", "添加短信记录失败", e)
        }
    }
    
    private fun getOrCreateThreadId(address: String): Long {
        try {
            return Telephony.Threads.getOrCreateThreadId(this, address)
        } catch (e: Exception) {
            Log.e("FakeActivity", "获取thread_id失败", e)
            return 0L
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        tvStatus.text = message
        Log.d("FakeActivity", message)
    }
}