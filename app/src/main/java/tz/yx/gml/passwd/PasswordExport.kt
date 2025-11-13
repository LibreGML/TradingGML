package tz.yx.gml.passwd

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PasswordExport(
    val passwords: List<PasswordItem>,
    val exportTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val version: Int = 1
)