package com.vperi.gpslogger.task

import android.content.Context

class DummyTask(context: Context) : BaseTask(context) {

  override fun start() {
    deferred.resolve(null)
  }
}
