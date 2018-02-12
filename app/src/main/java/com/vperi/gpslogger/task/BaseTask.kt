package com.vperi.gpslogger.task

import android.content.Context
import nl.komponents.kovenant.deferred

abstract class BaseTask(val context: Context) {
  val deferred = deferred<Any?, Any?>()
  val promise get() = deferred.promise

  abstract fun start()
}