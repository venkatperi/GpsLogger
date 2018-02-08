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


import android.content.Context
import android.location.Location
import android.preference.PreferenceManager
import java.text.DateFormat
import java.util.*

internal object Utils {

  internal val KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates"

  /**
   * Returns true if requesting location updates, otherwise returns false.
   *
   * @param context The [Context].
   */
  fun requestingLocationUpdates(context: Context): Boolean {
    return PreferenceManager.getDefaultSharedPreferences(context)
        .getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)
  }

  fun fcmSenderId(context: Context): String {
    return PreferenceManager.getDefaultSharedPreferences(context).getString("fcm_sender_id", "")
  }

  fun owntracksTid(context: Context): String {
    return PreferenceManager.getDefaultSharedPreferences(context).getString("owntracks_tid", "")
  }

  /**
   * Stores the location updates state in SharedPreferences.
   * @param requestingLocationUpdates The location updates state.
   */
  fun setRequestingLocationUpdates(context: Context, requestingLocationUpdates: Boolean) {
    PreferenceManager.getDefaultSharedPreferences(context)
        .edit()
        .putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates)
        .apply()
  }

  /**
   * Returns the `location` object as a human readable string.
   * @param location  The [Location].
   */
  fun getLocationText(location: Location?): String {
    return if (location == null)
      "Unknown location"
    else
      "(" + location.latitude + ", " + location.longitude + ")"
  }

  fun getLocationTitle(context: Context): String {
    return context.getString(R.string.location_updated,
        DateFormat.getDateTimeInstance().format(Date()))
  }

  // {"_type": "location","acc": %LOCACC,"batt": %BATT,"lat": %LOC1,"lon": %LOC2,"t": "a","tid": "me","tst": %LOCTMS}
  fun locationToOwnTracks(location: Location, tid: String): Map<String, String> {
    return mapOf(
        "_type" to "location",
        "lat" to location.latitude.toString(),
        "lon" to location.longitude.toString(),
        "acc" to location.accuracy.toString(),
        "tid" to tid,
        "tst" to location.time.toString()
    )
  }
}
