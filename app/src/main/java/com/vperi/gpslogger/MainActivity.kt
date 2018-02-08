package com.vperi.gpslogger

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

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button


/**
 * The only activity in this sample.
 *
 * Note: for apps running in the background on "O" devices (regardless of the targetSdkVersion),
 * location may be computed less frequently than requested when the app is not in the foreground.
 * Apps that use a foreground service -  which involves displaying a non-dismissable
 * notification -  can bypass the background location limits and request location updates as before.
 *
 * This sample uses a long-running bound and started service for location updates. The service is
 * aware of foreground status of this activity, which is the only bound client in
 * this sample. After requesting location updates, when the activity ceases to be in the foreground,
 * the service promotes itself to a foreground service and continues receiving location updates.
 * When the activity comes back to the foreground, the foreground service stops, and the
 * notification associated with that foreground service is removed.
 *
 * While the foreground service notification is displayed, the user has the option to launch the
 * activity from the notification. The user can also remove location updates directly from the
 * notification. This dismisses the notification and stops the service.
 */
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

  // The BroadcastReceiver used to listen from broadcasts from the service.
  private var myReceiver: MyReceiver? = null

  // A reference to the service used to get location updates.
  private var mService: LocationUpdatesService? = null

  // Tracks the bound state of the service.
  private var mBound = false

  private var fcm: Fcm? = null
  private var owntracksTid: String = ""

  // UI elements.
  private var mRequestLocationUpdatesButton: Button? = null
  private var mRemoveLocationUpdatesButton: Button? = null

  // Monitors the state of the connection to the service.
  private val mServiceConnection = object : ServiceConnection {

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      Log.i(TAG, "service connected: $name")
      val binder = service as LocationUpdatesService.LocalBinder
      mService = binder.service
      mBound = true
    }

    override fun onServiceDisconnected(name: ComponentName) {
      mService = null
      mBound = false
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.main_menu, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.settings -> {
        startActivity(Intent(this, SettingsActivity::class.java))
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (!checkPermissions()) {
      requestPermissions()
    }

    myReceiver = MyReceiver()
    setContentView(R.layout.activity_main)
  }


  override fun onStart() {
    super.onStart()
    PreferenceManager.getDefaultSharedPreferences(this)
        .registerOnSharedPreferenceChangeListener(this)

    mRequestLocationUpdatesButton = findViewById(R.id.request_location_updates_button)
    mRemoveLocationUpdatesButton = findViewById(R.id.remove_location_updates_button)

    mRequestLocationUpdatesButton!!.setOnClickListener {
      if (checkPermissions()) {
        mService!!.requestLocationUpdates()
      } else {
        requestPermissions()
      }
    }

    mRemoveLocationUpdatesButton!!.setOnClickListener { mService!!.removeLocationUpdates() }

    // Restore the state of the buttons when the activity (re)launches.
    setButtonsState(Utils.requestingLocationUpdates(this))

    // Bind to the service. If the service is in foreground mode, this signals to the service
    // that since this activity is in the foreground, the service can exit foreground mode.
    bindService(Intent(this, LocationUpdatesService::class.java), mServiceConnection,
        Context.BIND_AUTO_CREATE)

    fcm = Fcm(Utils.fcmSenderId(this))
    owntracksTid = Utils.owntracksTid(this)
    fcm!!.sendUpstreamMessage("hello", "world")
  }

  override fun onResume() {
    super.onResume()
    LocalBroadcastManager.getInstance(this).registerReceiver(myReceiver,
        IntentFilter(LocationUpdatesService.ACTION_BROADCAST))
  }

  override fun onPause() {
    LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver)
    super.onPause()
  }

  override fun onStop() {
    if (mBound) {
      // Unbind from the service. This signals to the service that this activity is no longer
      // in the foreground, and the service can respond by promoting itself to a foreground
      // service.
      unbindService(mServiceConnection)
      mBound = false
    }
    PreferenceManager.getDefaultSharedPreferences(this)
        .unregisterOnSharedPreferenceChangeListener(this)
    super.onStop()
  }

  /**
   * Returns the current state of the permissions needed.
   */
  private fun checkPermissions(): Boolean {
    return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION)
  }

  private fun requestPermissions() {
    val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
        Manifest.permission.ACCESS_FINE_LOCATION)

    // Provide an additional rationale to the user. This would happen if the user denied the
    // request previously, but didn't check the "Don't ask again" checkbox.
    if (shouldProvideRationale) {
      Log.i(TAG, "Displaying permission rationale to provide additional context.")
      Snackbar.make(
          findViewById(R.id.activity_main),
          R.string.permission_rationale,
          Snackbar.LENGTH_INDEFINITE)
          .setAction(R.string.ok, {
            // Request permission
            ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE)
          })
          .show()
    } else {
      Log.i(TAG, "Requesting permission")
      // Request permission. It's possible this can be auto answered if device policy
      // sets the permission in a given state or the user denied the permission
      // previously and checked "Never ask again".
      ActivityCompat.requestPermissions(this@MainActivity,
          arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
          REQUEST_PERMISSIONS_REQUEST_CODE)
    }
  }

  /**
   * Callback received when a permissions request has been completed.
   */
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                          grantResults: IntArray) {
    Log.i(TAG, "onRequestPermissionResult")
    if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
      when {
        grantResults.isEmpty() -> // If user interaction was interrupted, the permission request is cancelled and you
          // receive empty arrays.
          Log.i(TAG, "User interaction was cancelled.")
        grantResults[0] == PackageManager.PERMISSION_GRANTED -> // Permission was granted.
          mService!!.requestLocationUpdates()
        else -> {
          // Permission denied.
          setButtonsState(false)
          Snackbar.make(
              findViewById(R.id.activity_main),
              R.string.permission_denied_explanation,
              Snackbar.LENGTH_INDEFINITE)
              .setAction(R.string.settings, {
                // Build intent that displays the App settings screen.
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                val uri = Uri.fromParts("package",
                    BuildConfig.APPLICATION_ID, null)
                intent.data = uri
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
              })
              .show()
        }
      }
    }
  }

  /**
   * Receiver for broadcasts sent by [LocationUpdatesService].
   */
  private inner class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val location: Location? = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION)
      if (location != null) {
//        Toast.makeText(this@MainActivity, Utils.getLocationText(location), Toast.LENGTH_SHORT).show()
//        fcm!!.sendUpstreamMessage(Utils.locationToOwnTracks(location, owntracksTid))
      }
    }
  }

  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
    Log.d(TAG, s)
    // Update the buttons state depending on whether location updates are being requested.
    if (s == Utils.KEY_REQUESTING_LOCATION_UPDATES) {
      setButtonsState(sharedPreferences.getBoolean(Utils.KEY_REQUESTING_LOCATION_UPDATES,
          false))
    }
  }

  private fun setButtonsState(requestingLocationUpdates: Boolean) {
    if (requestingLocationUpdates) {
      mRequestLocationUpdatesButton!!.isEnabled = false
      mRemoveLocationUpdatesButton!!.isEnabled = true
    } else {
      mRequestLocationUpdatesButton!!.isEnabled = true
      mRemoveLocationUpdatesButton!!.isEnabled = false
    }
  }

  companion object {
    private val TAG = MainActivity::class.java.simpleName

    // Used in checking for runtime permissions.
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 34
  }
}
