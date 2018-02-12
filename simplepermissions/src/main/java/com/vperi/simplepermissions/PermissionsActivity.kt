package com.vperi.simplepermissions

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils

import java.util.ArrayList

/**
 * Created by Nabin Bhandari on 7/21/2017 on 11:19 PM
 */

@TargetApi(Build.VERSION_CODES.M)
class PermissionsActivity : Activity() {
  private var cleanHandlerOnDestroy = true
  private var allPermissions: ArrayList<String>? = null
  private var deniedPermissions: ArrayList<String>? = null
  private var noRationaleList: ArrayList<String>? = null
  private var options: Permissions.Options? = null

  @TargetApi(Build.VERSION_CODES.M)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (!intent.hasExtra(EXTRA_PERMISSIONS)) {
      finish()
      return
    }

    window.statusBarColor = 0
    allPermissions = intent.getSerializableExtra(EXTRA_PERMISSIONS) as ArrayList<String>
    options = intent.getSerializableExtra(EXTRA_OPTIONS) as Permissions.Options?
    if (options == null) {
      options = Permissions.Options()
    }
    deniedPermissions = ArrayList()
    noRationaleList = ArrayList()

    allPermissions!!.forEach {
      if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED) {
        deniedPermissions!!.add(it)
        if (!shouldShowRequestPermissionRationale(it)) {
          noRationaleList!!.add(it)
        }
      }
    }

    val rationale = intent.getStringExtra(EXTRA_RATIONALE)
    if (noRationaleList!!.isEmpty() || TextUtils.isEmpty(rationale)) {
      Permissions.log("No rationale.")
      requestPermissions(deniedPermissions!!.toTypedArray(), RC_PERMISSION)
    } else {
      Permissions.log("Show rationale.")
      showRationale(rationale)
    }
  }

  private fun showRationale(rationale: String) {
    val listener = DialogInterface.OnClickListener { _, which ->
      if (which == DialogInterface.BUTTON_POSITIVE) {
        requestPermissions(deniedPermissions!!.toTypedArray(), RC_PERMISSION)
      } else {
        deny()
      }
    }
    AlertDialog.Builder(this)
        .setTitle(options!!.rationaleDialogTitle)
        .setMessage(rationale)
        .setPositiveButton(android.R.string.ok, listener)
        .setNegativeButton(android.R.string.cancel, listener)
        .setOnCancelListener({ deny() }).create().show()
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
      grantResults: IntArray) {
    if (grantResults.size == 0) {
      deny()
    } else {
      deniedPermissions!!.clear()
      for (i in grantResults.indices) {
        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
          deniedPermissions!!.add(permissions[i])
        }
      }
      if (deniedPermissions!!.size == 0) {
        Permissions.log("Just allowed.")
        grant()
      } else {
        val blockedList = ArrayList<String>() //set not to ask again.
        val justBlockedList = ArrayList<String>() //just set not to ask again.
        val justDeniedList = ArrayList<String>()
        for (permission in deniedPermissions!!) {
          if (shouldShowRequestPermissionRationale(permission)) {
            justDeniedList.add(permission)
          } else {
            blockedList.add(permission)
            if (!noRationaleList!!.contains(permission)) {
              justBlockedList.add(permission)
            }
          }
        }

        if (justBlockedList.size > 0) { //checked don't ask again for at least one.
          if (permissionHandler != null) {
            permissionHandler!!.onJustBlocked(this, justBlockedList, deniedPermissions!!)
          }
          finish()
        } else if (justDeniedList.size > 0) { //clicked deny for at least one.
          deny()
        } else { //unavailable permissions were already set not to ask again.
          if (permissionHandler != null && !permissionHandler!!.onBlocked(this,
                  blockedList)) {
            sendToSettings()
          } else
            finish()
        }
      }
    }
  }

  private fun sendToSettings() {
    if (!options!!.sendBlockedToSettings) {
      deny()
      return
    }
    Permissions.log("Ask to go to settings.")
    AlertDialog.Builder(this).setTitle(options!!.settingsDialogTitle)
        .setMessage(options!!.settingsDialogMessage)
        .setPositiveButton(options!!.settingsText, { _, _ ->
          val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
              Uri.fromParts("package", packageName, null))
          startActivityForResult(intent, RC_SETTINGS)
        })
        .setNegativeButton(android.R.string.cancel, { _, _ -> deny() })
        .setOnCancelListener({ deny() }).create().show()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    if (requestCode == RC_SETTINGS && permissionHandler != null) {
      Permissions.check(this, allPermissions!!.toTypedArray(), null, options,
          permissionHandler!!)
      cleanHandlerOnDestroy = false
    }
    finish()
  }

  override fun onDestroy() {
    if (cleanHandlerOnDestroy) {
      permissionHandler = null
    }
    super.onDestroy()
  }

  private fun deny() {
    if (permissionHandler != null) {
      permissionHandler!!.onDenied(this, deniedPermissions!!)
    }
    finish()
  }

  private fun grant() {
    if (permissionHandler != null) {
      permissionHandler!!.onGranted()
    }
    finish()
  }

  companion object {

    private const val RC_SETTINGS = 6739
    private const val RC_PERMISSION = 6937

    internal const val EXTRA_PERMISSIONS = "permissions"
    internal const val EXTRA_RATIONALE = "rationale"
    internal const val EXTRA_OPTIONS = "options"

    internal var permissionHandler: PermissionHandler? = null
  }
}
