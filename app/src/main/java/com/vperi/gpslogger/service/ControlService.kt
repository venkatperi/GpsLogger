package com.vperi.gpslogger.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.anadeainc.rxbus.BusProvider

/**
 * Created by venkat on 2/11/18.
 */
class ControlService : Service() {
  private val mBinder = LocalBinder()

  private val bus = BusProvider.getInstance()

  override fun onBind(intent: Intent?): IBinder {
    return mBinder
  }

  /**
   * Class used for the client Binder.  Since this service runs in the same process as its
   * clients, we don't need to deal with IPC.
   */
  inner class LocalBinder : Binder() {
    internal val service get() = this@ControlService
  }

  companion object {
    private val TAG = ControlService::class.java.simpleName

  }
}