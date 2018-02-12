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

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.anadeainc.rxbus.BusProvider
import com.vperi.gpslogger.R
import com.vperi.gpslogger.Utils
import com.vperi.gpslogger.service.LocationUpdatesService
import com.vperi.gpslogger.task.CheckAuthTask
import com.vperi.gpslogger.task.CheckPermissionsTask

class MainActivity : AppCompatActivity() {
  private val bus = BusProvider.getInstance()

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
      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bus.register(this)
    CheckPermissionsTask(this, true).promise.success {
      CheckAuthTask(this, true).promise.success {
        startService()
      }
    }.fail { finish() }
    setContentView(R.layout.activity_main)
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
      unbindService(mServiceConnection)
    }
    super.onStop()
  }

  override fun onDestroy() {
    super.onDestroy()
    bus.unregister(this)
  }

  private fun startService() {
    Log.d(TAG, "starting service")
    startService(Intent(applicationContext, LocationUpdatesService::class.java))
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

  companion object {
    private val TAG = MainActivity::class.java.simpleName
  }
}
