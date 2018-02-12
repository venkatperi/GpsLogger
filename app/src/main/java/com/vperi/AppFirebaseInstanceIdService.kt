package com.vperi

import android.util.Log
import com.anadeainc.rxbus.BusProvider
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.vperi.gpslogger.R
import com.vperi.util.PrefHelper

class AppFirebaseInstanceIdService : FirebaseInstanceIdService() {
  private val prefs by lazy { PrefHelper(this) }
  private val bus = BusProvider.getInstance()

  override fun onTokenRefresh() {
    val refreshedToken = FirebaseInstanceId.getInstance().token
    val appInstanceId = FirebaseInstanceId.getInstance().id
    Log.d(TAG, "Instance Id: $appInstanceId, token: ${refreshedToken!!}")
    prefs[getString(R.string.firebase_app_instance_id)] = appInstanceId
    prefs[R.string.firebase_instance_id_token] = refreshedToken
    bus.post(AppInstanceId(appInstanceId))
  }

  companion object {
    private val TAG = AppFirebaseInstanceIdService::class.java.simpleName
  }
}

class AppInstanceId(id: String)