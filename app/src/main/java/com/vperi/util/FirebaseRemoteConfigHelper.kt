package com.vperi.util

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.vperi.gpslogger.R

class FirebaseRemoteConfigHelper(var context: Context) {
  var remoteConfig = FirebaseRemoteConfig.getInstance()!!

  var developerMode = true

  var cacheExpiration: Long = 3600

  var fetched: Boolean = false

  init {
    val remoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
        .setDeveloperModeEnabled(developerMode)
        .build()
    with(remoteConfig) {
      setConfigSettings(remoteConfigSettings)
      setDefaults(R.xml.remote_config_defaults)
      if (info.configSettings.isDeveloperModeEnabled) {
        cacheExpiration = 0
      }

      fetch(cacheExpiration)
          .addOnCompleteListener({ task ->
            if (task.isSuccessful) {
              remoteConfig.activateFetched()
              fetched = true
            } else {
              //task failed
            }
          })
    }
  }

  inline operator fun <reified T : Any> get(key: Int): T? =
      get(context.getString(key))

  inline operator fun <reified T : Any> get(key: String): T? =
      remoteConfig[key]
}

inline operator fun <reified T : Any> FirebaseRemoteConfig.get(key: String): T? {
  return when (T::class) {
    String::class -> getString(key) as T?
    Boolean::class -> getBoolean(key) as T?
    Double::class -> getDouble(key) as T?
    Long::class -> getLong(key) as T?
    ByteArray::class -> getByteArray(key) as T?
    else -> throw UnsupportedOperationException("Not yet implemented")
  }
}