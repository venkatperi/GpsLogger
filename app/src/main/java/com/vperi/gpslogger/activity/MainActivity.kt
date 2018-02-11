package com.vperi.gpslogger.activity

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
import android.provider.Settings
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.anadeainc.rxbus.BusProvider
import com.anadeainc.rxbus.Subscribe
import com.vperi.gpslogger.BuildConfig
import com.vperi.gpslogger.R
import com.vperi.gpslogger.Utils
import com.vperi.gpslogger.service.LocationUpdatesService
import com.vperi.util.FirebaseAuthHelper

class MainActivity : AppCompatActivity() {
  private val bus = BusProvider.getInstance()

  private val authHelper by lazy { FirebaseAuthHelper() }

  // The BroadcastReceiver used to listen from broadcasts from the service.
  private val myReceiver: MyReceiver? by lazy { MyReceiver() }

  // A reference to the service used to get location updates.
  private var mService: LocationUpdatesService? = null

  // Tracks the bound state of the service.
  private var mBound = false

  // Monitors the state of the connection to the service.
  private val mServiceConnection = object : ServiceConnection {

    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      Log.i(TAG, "service connected: $name")
      val binder = service as LocationUpdatesService.LocalBinder
      mService = binder.service
      mBound = true
      checkOrRequestPermissions()
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
      R.id.map -> {
        startActivity(Intent(this, MapsActivity::class.java))
        true
      }
      R.id.login -> {
        startActivity(Intent(this, GoogleSignInActivity::class.java))
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun checkOrRequestPermissions() {
    if (checkPermissions()) {
      bus.post(PermissionGrant(Manifest.permission.ACCESS_FINE_LOCATION))
    } else {
      requestPermissions()
    }
  }

  private fun checkOrAuthenticateUser() {
    if (authHelper.isAuthenticated)
      bus.post(AuthenticatedUser())
    else {
      authenticateUser()
    }
  }

  private fun authenticateUser() {
    startActivity(Intent(this, GoogleSignInActivity::class.java))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bus.register(this)
    setContentView(R.layout.activity_main)
  }

  override fun onStart() {
    super.onStart()
    checkOrRequestPermissions()
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
    }
    super.onStop()
  }

  override fun onDestroy() {
    super.onDestroy()
    bus.unregister(this)
  }

  @Subscribe
  fun onPermissionGrant(grant: PermissionGrant) {
    when (grant.permission) {
      Manifest.permission.ACCESS_FINE_LOCATION -> startService()
    }
  }

  private fun startService() {
    Log.d(TAG, "starting service")
    startService(Intent(applicationContext, LocationUpdatesService::class.java))
  }

  /**
   * Returns the current state of the permissions needed.
   */
  private fun checkPermissions(): Boolean {
    Log.d(TAG, "checkPermissions")
    return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(this,
        Manifest.permission.ACCESS_FINE_LOCATION)
  }

  private fun requestPermissions() {
    Log.d(TAG, "requestPermissions")
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
        grantResults.isEmpty() ->
          // If user interaction was interrupted, the permission request is cancelled and you
          // receive empty arrays.
          Log.i(TAG, "User interaction was cancelled.")

        grantResults[0] == PackageManager.PERMISSION_GRANTED ->
          bus.post(PermissionGrant(Manifest.permission.ACCESS_FINE_LOCATION))

        else -> Snackbar.make(
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

  /**
   * Receiver for broadcasts sent by [LocationUpdatesService].
   */
  private inner class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val location: Location? = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION)
      if (location != null) {
        Toast.makeText(this@MainActivity, Utils.getLocationText(location), Toast.LENGTH_SHORT).show()
      }
    }
  }

  inner class PermissionGrant(var permission: String)
  class AuthenticatedUser

  companion object {
    private val TAG = MainActivity::class.java.simpleName

    // Used in checking for runtime permissions.
    private const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
  }
}
