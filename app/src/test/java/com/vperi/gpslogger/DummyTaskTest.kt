package com.vperi.gpslogger

import android.content.Context
import com.nhaarman.mockito_kotlin.mock
import com.vperi.gpslogger.task.BaseTask
import com.vperi.gpslogger.task.DummyTask
import net.jodah.concurrentunit.Waiter
import org.junit.Before
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class
DummyTaskTest {
  private val context: Context = mock()
  private var task: BaseTask<Any?, Any?>? = null
  private var waiter: Waiter? = null

  @Before
  fun before() {
    task = DummyTask(context)
    waiter = Waiter()
  }

  @Test
  fun start() {
    task!!.start()
    task!!.promise.success { waiter!!.resume() }
    waiter!!.await(10000)
  }
}
