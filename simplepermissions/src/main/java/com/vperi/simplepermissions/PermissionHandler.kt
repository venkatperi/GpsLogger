package com.vperi.simplepermissions

import android.content.Context
import android.util.Log
import java.util.ArrayList

/**
 * The class for handling permission callbacks.
 *
 *
 * Created on 7/16/2017 on 3:42 PM
 *
 * @author Nabin Bhandari
 */
abstract class PermissionHandler {

  /**
   * This method will be called if all of the requested permissions are granted.
   */
  abstract fun onGranted()

  private fun logResult(msg: String, permissions: ArrayList<String>) {
    val builder = StringBuilder()
    with(builder) {
      append(msg)
      permissions.forEach {
        append(" ")
        append(it)
      }
    }
    Log.d(TAG, builder.toString())
  }

  /**
   * This method will be called if some of the requested permissions have been denied.
   *
   * @param context           The android context.
   * @param deniedPermissions The list of permissions which have been denied.
   */
  open fun onDenied(context: Context, deniedPermissions: ArrayList<String>) {
    logResult("Denied", deniedPermissions)
//    Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show()
  }

  /**
   * This method will be called if some permissions have previously been set not to ask again.
   *
   * @param context     the android context.
   * @param blockedList the list of permissions which have been set not to ask again.
   * @return The overrider of this method should return true if no further action is needed,
   * and should return false if the default action is to be taken, i.e. send user to settings.
   * <br></br><br></br>
   * Note: If the option [Permissions.Options.sendDontAskAgainToSettings] has been
   * set to false, the user won't be sent to settings by default.
   */
  open fun onBlocked(context: Context, blockedList: ArrayList<String>): Boolean {
    logResult("Don't ask again.", blockedList)
    return false
  }

  /**
   * This method will be called if some permissions have just been set not to ask again.
   *
   * @param context           The android context.
   * @param justBlockedList   The list of permissions which have just been set not to ask again.
   * @param deniedPermissions The list of currently unavailable permissions.
   */
  open fun onJustBlocked(context: Context, justBlockedList: ArrayList<String>,
      deniedPermissions: ArrayList<String>) {
    logResult("Just set to not ask again:", justBlockedList)
    onDenied(context, deniedPermissions)
  }

  companion object {
    private val TAG = PermissionHandler::class.java.simpleName
  }
}
