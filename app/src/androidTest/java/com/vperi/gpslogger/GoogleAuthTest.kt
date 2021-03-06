package com.vperi.gpslogger

import android.support.test.InstrumentationRegistry
import android.support.test.filters.MediumTest
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import com.vperi.gpslogger.task.BaseTask
import com.vperi.gpslogger.task.CheckAuthTask
import net.jodah.concurrentunit.Waiter
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
class GoogleAuthTest {
  private var task: BaseTask? = null
  private var waiter: Waiter? = null
  @Rule
  @JvmField
  var permissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.READ_CONTACTS)

  @Before
  fun before() {
    task = CheckAuthTask(InstrumentationRegistry.getTargetContext())
    waiter = Waiter()
  }

  @Test
  fun verifyPermissions() {
    task!!.start()
    task!!.promise
        .success { waiter!!.resume() }
        .fail { waiter!!.fail() }
    waiter!!.await(20000)
  }
}

