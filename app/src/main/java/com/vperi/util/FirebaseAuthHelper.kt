package com.vperi.util

import com.google.firebase.auth.FirebaseAuth

/**
 * Created by venkat on 2/10/18.
 */

class FirebaseAuthHelper {
  val currentUser by lazy { FirebaseAuth.getInstance().currentUser }

  val isAuthenticated: Boolean get() = currentUser != null
}