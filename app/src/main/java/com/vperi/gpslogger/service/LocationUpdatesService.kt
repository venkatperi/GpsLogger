/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vperi.gpslogger.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.anadeainc.rxbus.BusProvider
import com.anadeainc.rxbus.Subscribe
import com.vperi.gpslogger.R
import com.vperi.gpslogger.Utils
import com.vperi.gpslogger.activity.MainActivity
import com.vperi.util.FcmHelper
import com.vperi.util.LocationHelper
import com.vperi.util.PrefHelper
import com.vperi.util.toMap

/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 *
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 *
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification associated with that service is removed.
 */
class LocationUpdatesService : Service() {
  private val bus = BusProvider.getInstance()

  private val prefs by lazy { PrefHelper(this) }

  private val fcmHelper by lazy { FcmHelper(this) }

  private val mBinder = LocalBinder()

  private val mNotificationManager: NotificationManager? by lazy {
    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  }

  private val locationHelper = LocationHelper(this)

  private val mServiceHandler: Handler? by lazy {
    val handlerThread = HandlerThread(TAG)
    handlerThread.start()
    Handler(handlerThread.looper)
  }

  /**
   * Returns the [NotificationCompat] used as part of the foreground service.
   */
  // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
  // The PendingIntent that leads to a call to onStartCommand() in this service.
  // The PendingIntent to launch activity.
  // Set the Channel ID for Android O.
  // Channel ID
  private val notification: Notification
    @RequiresApi(Build.VERSION_CODES.N)
    get() {
      val intent = Intent(this, LocationUpdatesService::class.java)

      val text = Utils.getLocationText(locationHelper.location)
      intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)
      val servicePendingIntent = PendingIntent.getService(this, 0, intent,
          PendingIntent.FLAG_UPDATE_CURRENT)
      val activityPendingIntent = PendingIntent.getActivity(this, 0,
          Intent(this, MainActivity::class.java), 0)

      val builder = NotificationCompat.Builder(this)
          .addAction(R.drawable.ic_launch, getString(R.string.launch_activity), activityPendingIntent)
          .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates), servicePendingIntent)
          .setContentText(text)
          .setContentTitle(Utils.getLocationTitle(this))
          .setOngoing(true)
          .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
          .setSmallIcon(R.mipmap.ic_launcher)
          .setTicker(text)
          .setWhen(System.currentTimeMillis())
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        builder.setChannelId(CHANNEL_ID)
      }

      return builder.build()
    }

  @RequiresApi(Build.VERSION_CODES.O)
  override fun onCreate() {
    bus.register(this)

    // Android O requires a Notification Channel.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = getString(R.string.app_name)
      val mChannel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
      mNotificationManager!!.createNotificationChannel(mChannel)
    }
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    Log.i(TAG, "Service started")
    locationHelper.start()

    // Tells the system to not try to recreate the service after it has been killed.
    return Service.START_NOT_STICKY
  }

  override fun onBind(intent: Intent): IBinder? {
    // Called when a client (MainActivity in case of this sample) comes to the foreground
    // and binds with this service. The service should cease to be a foreground service
    // when that happens.
    Log.i(TAG, "in onBind()")
    stopForeground(true)
    return mBinder
  }

  override fun onRebind(intent: Intent) {
    // Called when a client (MainActivity in case of this sample) returns to the foreground
    // and binds once again with this service. The service should cease to be a foreground
    // service when that happens.
    Log.i(TAG, "in onRebind()")
    stopForeground(true)
    super.onRebind(intent)
  }

  override fun onUnbind(intent: Intent): Boolean {
    Log.i(TAG, "Last client unbound from service")

    // Called when the last client (MainActivity in case of this sample) unbinds from this
    // service. If this method is called due to a configuration change in MainActivity, we
    // do nothing. Otherwise, we make this service a foreground service.
    Log.i(TAG, "Starting foreground service")
    startForeground(NOTIFICATION_ID, notification)
    return true // Ensures onRebind() is called when a client re-binds.
  }

  override fun onDestroy() {
    locationHelper.stop()
    mServiceHandler!!.removeCallbacksAndMessages(null)
    bus.unregister(this)
  }

  @Subscribe
  fun onNewLocation(location: Location) {
    fcmHelper.sendUpstreamMessage(location.toMap())

    // Notify anyone listening for broadcasts about the new location.
    val intent = Intent(ACTION_BROADCAST)
    intent.putExtra(EXTRA_LOCATION, location)
    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
  }

  /**
   * Class used for the client Binder.  Since this service runs in the same process as its
   * clients, we don't need to deal with IPC.
   */
  inner class LocalBinder : Binder() {
    internal val service: LocationUpdatesService
      get() = this@LocationUpdatesService
  }

  companion object {
    private val TAG = LocationUpdatesService::class.java.simpleName
    private val PACKAGE_NAME = "com.vperi.gpslogger"
    private val CHANNEL_ID = "channel_01"
    internal val ACTION_BROADCAST = "$PACKAGE_NAME.broadcast"
    internal val EXTRA_LOCATION = "$PACKAGE_NAME.location"
    private val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private val NOTIFICATION_ID = 12345678

  }
}
