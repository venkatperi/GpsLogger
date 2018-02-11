package com.vperi

import android.util.Log
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.vperi.gpslogger.R
import com.vperi.util.PrefHelper

class AppFirebaseInstanceIdService : FirebaseInstanceIdService() {
  private val prefs by lazy { PrefHelper(this) }

  override fun onTokenRefresh() {
    val refreshedToken = FirebaseInstanceId.getInstance().token
    Log.d(TAG, "Refreshed token: " + refreshedToken!!)
    prefs[R.string.firebase_instance_id_token] = refreshedToken
  }

  companion object {
    private val TAG = AppFirebaseInstanceIdService::class.java.simpleName
  }
}