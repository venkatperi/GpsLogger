package com.vperi.util

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.vperi.gpslogger.R

class FcmHelper(var context: Context) {
  private val remoteConfig by lazy { FirebaseRemoteConfigHelper(context) }

  private var msgId = 1

  private val senderId: String
    get() = remoteConfig[R.string.fcm_sender_id]!!

  @Suppress("unused")
  fun sendUpstreamMessage(key: String, value: String) {
    sendUpstreamMessage(mapOf(key to value))
  }

  fun sendUpstreamMessage(data: Map<String, String>) {
    sendUpstreamMessage(RemoteMessage.Builder("$senderId@gcm.googleapis.com")
        .setMessageId(Integer.toString(msgId++))
        .setData(data))
  }

  private fun sendUpstreamMessage(builder: RemoteMessage.Builder) {
    Log.d(TAG, "sendUpstreamMessage")
    val fm = FirebaseMessaging.getInstance()
    fm.send(builder.build())
  }

  companion object {
    private val TAG = FcmHelper::class.java.simpleName
  }
}