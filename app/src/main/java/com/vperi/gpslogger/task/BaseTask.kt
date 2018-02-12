package com.vperi.gpslogger.task

import android.content.Context
import com.anadeainc.rxbus.BusProvider
import nl.komponents.kovenant.deferred

abstract class BaseTask(val context: Context, autoStart: Boolean = false) {
  private val bus = BusProvider.getInstance()
  val deferred = deferred<Any?, Any?>()
  val promise get() = deferred.promise

  init {
    bus.register(this)
    promise.always { bus.unregister(this) }
    if (autoStart) this.start()
  }

  abstract fun start()
}