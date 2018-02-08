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
package com.vperi.gpslogger

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.android.gms.location.*

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

  private var fcm: Fcm? = null

  private val mBinder = LocalBinder()

  /**
   * Used to check whether the bound activity has really gone away and not unbound as part of an
   * orientation change. We create a foreground service notification only if the former takes
   * place.
   */
  private var mChangingConfiguration = false

  private var mNotificationManager: NotificationManager? = null

  /**
   * Contains parameters used by [com.google.android.gms.location.FusedLocationProviderApi].
   */
  private var mLocationRequest: LocationRequest? = null

  /**
   * Provides access to the Fused Location Provider API.
   */
  private var mFusedLocationClient: FusedLocationProviderClient? = null

  /**
   * Callback for changes in location.
   */
  private var mLocationCallback: LocationCallback? = null

  private var mServiceHandler: Handler? = null

  /**
   * The current location.
   */
  private var mLocation: Location? = null

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

      val text = Utils.getLocationText(mLocation)
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
          .setPriority(NotificationManager.IMPORTANCE_HIGH)
          .setSmallIcon(R.mipmap.ic_launcher)
          .setTicker(text)
          .setWhen(System.currentTimeMillis())
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        builder.setChannelId(CHANNEL_ID)
      }

      return builder.build()
    }

  private var owntracksTid: String = ""

  override fun onCreate() {
    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    mLocationCallback = object : LocationCallback() {
      override fun onLocationResult(locationResult: LocationResult) {
        super.onLocationResult(locationResult)
        onNewLocation(locationResult.lastLocation)
      }
    }

    fcm = Fcm(Utils.fcmSenderId(this))
    owntracksTid = Utils.owntracksTid(this)

    val handlerThread = HandlerThread(TAG)
    handlerThread.start()
    mServiceHandler = Handler(handlerThread.looper)
    mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Android O requires a Notification Channel.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = getString(R.string.app_name)
      // Create the channel for the notification
      val mChannel = NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

      // Set the Notification Channel for the Notification Manager.
      mNotificationManager!!.createNotificationChannel(mChannel)
    }
  }

  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
    Log.i(TAG, "Service started")
    val startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
        false)

    // We got here because the user decided to remove location updates from the notification.
    if (startedFromNotification) {
      removeLocationUpdates()
      stopSelf()
    } else {
    }

    // Tells the system to not try to recreate the service after it has been killed.
    return Service.START_NOT_STICKY
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    mChangingConfiguration = true
  }

  override fun onBind(intent: Intent): IBinder? {
    // Called when a client (MainActivity in case of this sample) comes to the foreground
    // and binds with this service. The service should cease to be a foreground service
    // when that happens.
    Log.i(TAG, "in onBind()")
    stopForeground(true)
    mChangingConfiguration = false
    return mBinder
  }

  override fun onRebind(intent: Intent) {
    // Called when a client (MainActivity in case of this sample) returns to the foreground
    // and binds once again with this service. The service should cease to be a foreground
    // service when that happens.
    Log.i(TAG, "in onRebind()")
    stopForeground(true)
    mChangingConfiguration = false
    super.onRebind(intent)
  }

  override fun onUnbind(intent: Intent): Boolean {
    Log.i(TAG, "Last client unbound from service")

    // Called when the last client (MainActivity in case of this sample) unbinds from this
    // service. If this method is called due to a configuration change in MainActivity, we
    // do nothing. Otherwise, we make this service a foreground service.
    if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
      Log.i(TAG, "Starting foreground service")
      startForeground(NOTIFICATION_ID, notification)
    }
    return true // Ensures onRebind() is called when a client re-binds.
  }

  override fun onDestroy() {
    mServiceHandler!!.removeCallbacksAndMessages(null)
  }

  @Suppress("unused")
      /**
       * Makes a request for location updates. Note that in this sample we merely log the
       * [SecurityException].
       */
  fun requestLocationUpdates() {
    Log.i(TAG, "Requesting location updates")
    createLocationRequest()

    Utils.setRequestingLocationUpdates(this, true)
    startService(Intent(applicationContext, LocationUpdatesService::class.java))
    try {
      mFusedLocationClient!!.requestLocationUpdates(mLocationRequest,
          mLocationCallback, Looper.myLooper())
      getLastLocation()
    } catch (unlikely: SecurityException) {
      Utils.setRequestingLocationUpdates(this, false)
      Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
    }
  }

  /**
   * Removes location updates. Note that in this sample we merely log the
   * [SecurityException].
   */
  internal fun removeLocationUpdates() {
    Log.i(TAG, "Removing location updates")
    try {
      mFusedLocationClient!!.removeLocationUpdates(mLocationCallback)
      Utils.setRequestingLocationUpdates(this, false)
      stopSelf()
    } catch (unlikely: SecurityException) {
      Utils.setRequestingLocationUpdates(this, true)
      Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
    }
  }

  private fun getLastLocation() {
    try {
      mFusedLocationClient!!.lastLocation.addOnCompleteListener { task ->
        if (task.isSuccessful && task.result != null) {
          mLocation = task.result
        } else {
          Log.w(TAG, "Failed to get location.")
        }
      }
    } catch (unlikely: SecurityException) {
      Log.e(TAG, "Lost location permission." + unlikely)
    }

  }

  private fun onNewLocation(location: Location) {
    Log.i(TAG, "New location: " + location)

    mLocation = location

    fcm!!.sendUpstreamMessage(Utils.locationToOwnTracks(location, owntracksTid))

    // Notify anyone listening for broadcasts about the new location.
    val intent = Intent(ACTION_BROADCAST)
    intent.putExtra(EXTRA_LOCATION, location)
    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

    // Update notification content if running as a foreground service.
    if (serviceIsRunningInForeground(this)) {
//      mNotificationManager!!.notify(NOTIFICATION_ID, notification)
    }
  }

  /**
   * Sets the location request parameters.
   */
  private fun createLocationRequest() {
    Log.d(TAG, "createLocationRequest")
    mLocationRequest = LocationRequest()
    mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
    mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
    mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
  }

  /**
   * Class used for the client Binder.  Since this service runs in the same process as its
   * clients, we don't need to deal with IPC.
   */
  inner class LocalBinder : Binder() {
    internal val service: LocationUpdatesService
      get() = this@LocationUpdatesService
  }

  /**
   * Returns true if this is a foreground service.
   *
   * @param context The [Context].
   */
  private fun serviceIsRunningInForeground(context: Context): Boolean {
    val manager = context.getSystemService(
        Context.ACTIVITY_SERVICE) as ActivityManager
    return manager.getRunningServices(
        Integer.MAX_VALUE).any { javaClass.name == it.service.className && it.foreground }
  }

  companion object {

    private val PACKAGE_NAME = "com.google.android.gms.location.sample.locationupdatesforegroundservice"

    private val TAG = LocationUpdatesService::class.java.simpleName

    /**
     * The name of the channel for notifications.
     */
    private val CHANNEL_ID = "channel_01"

    internal val ACTION_BROADCAST = PACKAGE_NAME + ".broadcast"

    internal val EXTRA_LOCATION = PACKAGE_NAME + ".location"
    private val EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME + ".started_from_notification"

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private val NOTIFICATION_ID = 12345678
  }
}
