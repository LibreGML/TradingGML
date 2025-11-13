package tz.yx.gml.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class SmsReceiverService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("SmsReceiverService", "SMS received, but we're not processing it")
        return START_NOT_STICKY
    }
}