package com.vperi.gpslogger.task

import android.content.Context
import android.content.Intent
import android.util.Log
import com.anadeainc.rxbus.Subscribe
import com.vperi.gpslogger.activity.AuthResult
import com.vperi.gpslogger.activity.GoogleAuthActivity

class CheckAuthTask(context: Context, autoStart: Boolean) : BaseTask(context, autoStart) {
  override fun start() {
    Log.d(TAG, "Checking authentication")

    val intent = Intent(context, GoogleAuthActivity::class.java)
    context.startActivity(intent)
  }

  @Subscribe
  fun onAuthResult(result: AuthResult) {
    when (result.succeeded) {
      true -> deferred.resolve(null)
      false -> deferred.reject(result.exception)
    }
  }

  companion object {
    private val TAG = CheckAuthTask::class.java.simpleName
  }
}
