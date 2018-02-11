package com.vperi.util

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Created by venkat on 2/10/18.
 */

class FirestoreHelper {
  val db by lazy { FirebaseFirestore.getInstance() }
}