package com.vperi.gpslogger.test

import android.os.Build
import android.support.test.InstrumentationRegistry
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiObjectNotFoundException
import android.support.test.uiautomator.UiSelector

/**
 * Created by venkat on 2/12/18.
 */
object Utils {
  fun clickPermissionsDialog(allow: Boolean) {
    if (Build.VERSION.SDK_INT >= 23) {
      val button = UiDevice
          .getInstance(InstrumentationRegistry.getInstrumentation())
          .findObject(UiSelector().text(if (allow) "ALLOW" else "DENY"))
      if (button.exists()) {
        try {
          button.click()
        } catch (e: UiObjectNotFoundException) {
        }
      }
    }
  }
}