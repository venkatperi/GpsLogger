package com.vperi.gpslogger.task

import android.content.Context
import android.util.Log
import com.vperi.simplepermissions.PermissionHandler
import com.vperi.simplepermissions.Permissions

class CheckPermissionsTask(context: Context) : BaseTask(context) {

  override fun start() {
    val permissions = Permissions.getRequestedPermissions(context)!!
    Log.d(TAG, "requesting: ${permissions.joinToString()}")
    Permissions.check(context,
        Permissions.getRequestedPermissions(context)!!, null, null,
        object : PermissionHandler() {
          override fun onGranted() = deferred.resolve(null)
          override fun onDenied(context: Context, deniedPermissions: ArrayList<String>) {
            deferred.reject(deniedPermissions)
          }
        })
  }

  companion object {
    private val TAG = CheckPermissionsTask::class.java.simpleName
  }
}
