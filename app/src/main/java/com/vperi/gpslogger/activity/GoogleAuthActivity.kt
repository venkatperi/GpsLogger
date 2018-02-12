package com.vperi.gpslogger.activity

import android.app.Activity
import android.content.Intent
import android.support.design.widget.Snackbar
import android.util.Log
import com.anadeainc.rxbus.BusProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.vperi.gpslogger.R

class GoogleAuthActivity : Activity() {
  private val bus = BusProvider.getInstance()

  private val mAuth: FirebaseAuth? by lazy { FirebaseAuth.getInstance() }

  private val mGoogleSignInClient: GoogleSignInClient? by lazy {
    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    GoogleSignIn.getClient(this, gso)
  }

  override fun onStart() {
    super.onStart()
    val action: String = if (intent.hasExtra(getString(R.string.auth_action))) intent.getStringExtra(getString(R.string.auth_action)) else getString(R.string.sign_in)
    when (action) {
      getString(R.string.sign_in) -> signIn()
      getString(R.string.sign_out) -> signOut()
      getString(R.string.revoke_access) -> revokeAccess()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == RC_SIGN_IN) {
      val task = GoogleSignIn.getSignedInAccountFromIntent(data)
      try {
        val account = task.getResult(ApiException::class.java)
        firebaseAuthWithGoogle(account)
        postResultAndFinish(true)
      } catch (e: ApiException) {
        Log.w(TAG, "Google sign in failed", e)
        postResultAndFinish(false, e)
      }
    }
  }

  private fun postResultAndFinish(succeeded: Boolean, e: Exception? = null) {
    bus.post(AuthResult(succeeded, e))
    finish()
  }

  private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
    Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)

    val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
    mAuth!!.signInWithCredential(credential)
        .addOnCompleteListener(this, { task ->
          if (task.isSuccessful) {
            Log.d(TAG, "signInWithCredential:success")
            postResultAndFinish(true)
          } else {
            Log.w(TAG, "signInWithCredential:failure", task.exception)
            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
            postResultAndFinish(false, task.exception)
          }
        })
  }

  private fun signIn() {
    val signInIntent = mGoogleSignInClient!!.signInIntent
    startActivityForResult(signInIntent, RC_SIGN_IN)
  }

  private fun signOut() {
    mAuth!!.signOut()
    mGoogleSignInClient!!.signOut().addOnCompleteListener(this, {
      postResultAndFinish(true)
    })
  }

  private fun revokeAccess() {
    mAuth!!.signOut()
    mGoogleSignInClient!!.revokeAccess().addOnCompleteListener(this, {
      postResultAndFinish(true)
    })
  }

  companion object {

    private val TAG = GoogleAuthActivity::class.java.simpleName
    private const val RC_SIGN_IN = 9001

    const val RESULT_SUCCESS = 1001
    const val RESULT_FAILED = 1002
  }
}

class AuthResult(val succeeded: Boolean, val exception: Exception? = null)
