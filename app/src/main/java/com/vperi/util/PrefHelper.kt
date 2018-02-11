package com.vperi.util

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.vperi.util.PreferenceHelper.get
import com.vperi.util.PreferenceHelper.set

class PrefHelper(val context: Context) {

  var defaultPrefs = PreferenceHelper.defaultPrefs(context)

  inline operator fun <reified T : Any> get(key: Int, defaultValue: T? = null): T? =
      get(context.getString(key), defaultValue)

  inline operator fun <reified T : Any> get(key: String, defaultValue: T? = null): T? =
      defaultPrefs[key, defaultValue]

  operator fun set(key: Int, value: Any?) {
    set(context.getString(key), value)
  }

  operator fun set(key: String, value: Any?) {
    defaultPrefs[key] = value
  }
}