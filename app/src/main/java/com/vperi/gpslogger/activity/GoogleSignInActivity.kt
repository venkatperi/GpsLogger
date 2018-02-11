package com.vperi.gpslogger.activity

/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.TextView
import com.anadeainc.rxbus.BusProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.vperi.gpslogger.R

/**
 * Demonstrate Firebase Authentication using a Google ID Token.
 */
class GoogleSignInActivity : BaseActivity(), View.OnClickListener {
  private val bus = BusProvider.getInstance()
  private var mAuth: FirebaseAuth? = null

  private var mGoogleSignInClient: GoogleSignInClient? = null
  private var mStatusTextView: TextView? = null
  private var mDetailTextView: TextView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_google)

    // Views
    mStatusTextView = findViewById(R.id.status)
    mDetailTextView = findViewById(R.id.detail)

    // Button listeners
    findViewById<View>(R.id.sign_in_button).setOnClickListener(this)
    findViewById<View>(R.id.sign_out_button).setOnClickListener(this)
    findViewById<View>(R.id.disconnect_button).setOnClickListener(this)

    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken(getString(R.string.default_web_client_id))
        .requestEmail()
        .build()

    mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

    mAuth = FirebaseAuth.getInstance()
  }

  // [START on_start_check_user]
  override fun onStart() {
    super.onStart()
    // Check if user is signed in (non-null) and update UI accordingly.
    val currentUser = mAuth!!.currentUser
    updateUI(currentUser)
  }
  // [END on_start_check_user]

  // [START onactivityresult]
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)

    // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
    if (requestCode == RC_SIGN_IN) {
      val task = GoogleSignIn.getSignedInAccountFromIntent(data)
      try {
        // Google Sign In was successful, authenticate with Firebase
        val account = task.getResult(ApiException::class.java)
        firebaseAuthWithGoogle(account)
      } catch (e: ApiException) {
        // Google Sign In failed, update UI appropriately
        Log.w(TAG, "Google sign in failed", e)
        // [START_EXCLUDE]
        updateUI(null)
        // [END_EXCLUDE]
      }

    }
  }
  // [END onactivityresult]

  // [START auth_with_google]
  private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
    Log.d(TAG, "firebaseAuthWithGoogle:" + acct.id!!)
    // [START_EXCLUDE silent]
    showProgressDialog()
    // [END_EXCLUDE]

    val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
    mAuth!!.signInWithCredential(credential)
        .addOnCompleteListener(this, { task ->
          if (task.isSuccessful) {
            // Sign in success, update UI with the signed-in user's information
            Log.d(TAG, "signInWithCredential:success")
            val user = mAuth!!.currentUser
            updateUI(user)
          } else {
            // If sign in fails, display a message to the user.
            Log.w(TAG, "signInWithCredential:failure", task.exception)
            Snackbar.make(findViewById(R.id.main_layout), "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
            updateUI(null)
          }

          // [START_EXCLUDE]
          hideProgressDialog()
          // [END_EXCLUDE]
        })
  }
  // [END auth_with_google]

  // [START signin]
  private fun signIn() {
    val signInIntent = mGoogleSignInClient!!.signInIntent
    startActivityForResult(signInIntent, RC_SIGN_IN)
  }
  // [END signin]

  private fun signOut() {
    // Firebase sign out
    mAuth!!.signOut()

    // Google sign out
    mGoogleSignInClient!!.signOut().addOnCompleteListener(this, { updateUI(null) })
  }

  private fun revokeAccess() {
    // Firebase sign out
    mAuth!!.signOut()

    // Google revoke access
    mGoogleSignInClient!!.revokeAccess().addOnCompleteListener(this,
        { updateUI(null) })
  }

  private fun updateUI(user: FirebaseUser?) {
    hideProgressDialog()
    if (user != null) {
      mStatusTextView!!.text = getString(R.string.google_status_fmt, user.email)
      mDetailTextView!!.text = getString(R.string.firebase_status_fmt, user.uid)

      findViewById<View>(R.id.sign_in_button).visibility = View.GONE
      findViewById<View>(R.id.sign_out_and_disconnect).visibility = View.VISIBLE
    } else {
      mStatusTextView!!.setText(R.string.signed_out)
      mDetailTextView!!.text = null

      findViewById<View>(R.id.sign_in_button).visibility = View.VISIBLE
      findViewById<View>(R.id.sign_out_and_disconnect).visibility = View.GONE
    }
    bus.post(MainActivity.AuthenticatedUser())
    super.finish()
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.sign_in_button -> signIn()
      R.id.sign_out_button -> signOut()
      R.id.disconnect_button -> revokeAccess()
    }
  }

  companion object {
    private const val TAG = "GoogleActivity"
    private const val RC_SIGN_IN = 9001
  }
}

