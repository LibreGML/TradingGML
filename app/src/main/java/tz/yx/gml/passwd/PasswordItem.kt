package tz.yx.gml.passwd

import java.util.UUID

data class PasswordItem(
    var id: String = UUID.randomUUID().toString(),
    var platform: String = "",
    var account: String = "",
    var password: String = "",
    var note: String = ""
)