package com.vperi.util

import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.anadeainc.rxbus.BusProvider
import com.anadeainc.rxbus.Subscribe
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.vperi.gpslogger.R
import org.json.JSONObject

/**
 * Created by venkat on 2/10/18.
 */
class LocationHelper(var context: Context) {
  private val bus = BusProvider.getInstance()

  private val prefs by lazy { PrefHelper(context) }

  /**
   * Contains parameters used by [com.google.android.gms.location.FusedLocationProviderApi].
   */
  private val mLocationRequest: LocationRequest? by lazy {
    val req = LocationRequest()
    req.interval = prefs[R.string.pref_location_update_interval, 1000]!!
    req.fastestInterval = prefs[R.string.pref_location_fastest_update_interval, 500]!!
    req.priority = prefs[R.string.pref_location_priority, LocationRequest.PRIORITY_HIGH_ACCURACY]!!
    req
  }

  /**
   * Provides access to the Fused Location Provider API.
   */
  private val mFusedLocationClient: FusedLocationProviderClient? by lazy {
    LocationServices.getFusedLocationProviderClient(context)
  }

  /**
   * Callback for changes in location.
   */
  private val mLocationCallback: LocationCallback? = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
      super.onLocationResult(locationResult)
      bus.post(locationResult.lastLocation)
    }
  }

  var location: Location? = null

  private var running = false

  fun start() {
    if (running) return
    try {
      mFusedLocationClient!!.requestLocationUpdates(mLocationRequest,
          mLocationCallback, Looper.myLooper())
      getLastLocation()
      running = true
    } catch (unlikely: SecurityException) {
      Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
    }
  }

  fun stop() {
    if (!running) return
    Log.i(TAG, "Removing location updates")
    try {
      mFusedLocationClient!!.removeLocationUpdates(mLocationCallback)
      running = false
    } catch (unlikely: SecurityException) {
      Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
    }
  }

  private fun getLastLocation() {
    try {
      mFusedLocationClient!!.lastLocation.addOnCompleteListener { task ->
        if (task.isSuccessful && task.result != null) {
          bus.post(task.result)
        } else {
          Log.w(TAG, "Failed to get location.")
        }
      }
    } catch (unlikely: SecurityException) {
      Log.e(TAG, "Lost location permission. $unlikely")
    }
  }

  @Subscribe
  fun onNewLocation(location: Location) {
    Log.i(TAG, "New location: $location")
    this.location = location
  }

  companion object {
    private val TAG = LocationHelper::class.java.simpleName
  }
}

fun Location.toJSON(): JSONObject {
  val res = JSONObject()

  res.put("provider", provider)
  res.put("latitude", latitude)
  res.put("longitude", longitude)
  res.put("time", time)

  if (hasAccuracy()) res.put("accuracy", accuracy)
  if (hasAltitude()) res.put("altitude", altitude)
  if (hasSpeed()) res.put("speed", speed)
  if (hasBearing()) res.put("bearing", bearing)
  if (extras != null) res.put("extras", extras.toString())

  return res
}

fun Location.toMap(): Map<String, String> {
  val res = HashMap<String, String>()

  res["provider"] = provider
  res["latitude"] = latitude.toString()
  res["longitude"] = longitude.toString()
  res["time"] = time.toString()

  if (hasAccuracy()) res["accuracy"] = accuracy.toString()
  if (hasAltitude()) res["altitude"] = altitude.toString()
  if (hasSpeed()) res["speed"] = speed.toString()
  if (hasBearing()) res["bearing"] = bearing.toString()
//  if (extras != null) res["extras"] = extras.toString()

  return res
}