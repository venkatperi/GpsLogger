package com.vperi.gpslogger

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage

/**
 * Created by venkat on 2/8/18.
 */

class Fcm(private var senderId: String) {
  private var msgId = 1

  fun sendUpstreamMessage(key: String, value: String) {
    sendUpstreamMessage(mapOf(key to value))
  }

  fun sendUpstreamMessage(data: Map<String, String>) {
    sendUpstreamMessage(RemoteMessage.Builder("$senderId@gcm.googleapis.com")
        .setMessageId(Integer.toString(msgId++))
        .setData(data))
  }

  fun sendUpstreamMessage(builder: RemoteMessage.Builder) {
    Log.d(TAG, builder.toString())
    val fm = FirebaseMessaging.getInstance()
    fm.send(builder.build())
  }

  companion object {
    private val TAG = Fcm::class.java.simpleName
  }
}