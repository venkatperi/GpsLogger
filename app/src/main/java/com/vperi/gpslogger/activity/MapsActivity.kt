package com.vperi.gpslogger.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.vperi.gpslogger.R
import com.vperi.gpslogger.service.LocationUpdatesService

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

  private lateinit var mMap: GoogleMap

  private val myReceiver: MyReceiver? by lazy { MyReceiver() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_maps)
    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager
        .findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)
  }

  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  override fun onMapReady(googleMap: GoogleMap) {
    mMap = googleMap

    // Add a marker in Sydney and move the camera
    val sydney = LatLng(-34.0, 151.0)
    mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
    mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
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

  /**
   * Receiver for broadcasts sent by [LocationUpdatesService].
   */
  private inner class MyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val location: Location? = intent.getParcelableExtra(LocationUpdatesService.EXTRA_LOCATION)
      if (location != null) {
        val pos = LatLng(location.latitude, location.longitude)
        val update = CameraUpdateFactory.newLatLngZoom(pos, 10f)
        mMap.animateCamera(update)
      }
    }
  }
}
