package com.vperi.util

import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.vperi.gpslogger.R

/**
 * Created by venkat on 2/8/18.
 */

class FcmHelper(var context: Context) {
  private val remoteConfig by lazy { FirebaseRemoteConfigHelper(context) }

  private var msgId = 1

  private val senderId: String
    get() = remoteConfig[R.string.fcm_sender_id]!!

  fun sendUpstreamMessage(key: String, value: String) {
    sendUpstreamMessage(mapOf(key to value))
  }

  fun sendUpstreamMessage(data: Map<String, String>) {
    sendUpstreamMessage(RemoteMessage.Builder("$senderId@gcm.googleapis.com")
        .setMessageId(Integer.toString(msgId++))
        .setData(data))
  }

  fun sendUpstreamMessage(builder: RemoteMessage.Builder) {
    val fm = FirebaseMessaging.getInstance()
    fm.send(builder.build())
  }

  companion object {
    private val TAG = FcmHelper::class.java.simpleName
  }
}