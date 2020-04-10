@file:Suppress("DEPRECATION")

package com.example.socialapplication.Activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.*
import com.example.socialapplication.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase

private const val RC_SIGN_IN = 100

@Suppress("DEPRECATION")
class LoginActivity : BaseActivity() {

    private lateinit var mEmailEt : EditText
    private lateinit var mPasswordEt : EditText
    private lateinit var notHaveAccnt : TextView
    private lateinit var mRecoverPassTv : TextView
    private lateinit var mLoginBtn : Button
    private lateinit var mLoginGoogleBtn : SignInButton

    private lateinit var mobileSignInClient : GoogleSignInClient


    private lateinit var progressDialog : ProgressDialog

    private lateinit var mAuth : FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findView()
        initView()
        initListeners()

    }

    override fun findView() {

        mEmailEt = findViewById(R.id.emailEt)
        mPasswordEt = findViewById(R.id.passwordEt)
        notHaveAccnt = findViewById(R.id.not_accountTv)
        mRecoverPassTv = findViewById(R.id.recoverPasswordTv)
        mLoginBtn = findViewById(R.id.loginBtn)
        mLoginGoogleBtn = findViewById(R.id.googleLoginBtn)

    }

    override fun initView() {

        progressDialog = ProgressDialog(this)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mobileSignInClient = GoogleSignIn.getClient(this, gso)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

    }

    override fun initListeners() {

        mLoginBtn.setOnClickListener {

            prosesValidasiDataLogin()

        }

        mRecoverPassTv.setOnClickListener {

            passwordRecoverDialog()

        }

        mLoginGoogleBtn.setOnClickListener {

            val signInIntent = mobileSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)

        }

        notHaveAccnt.setOnClickListener {

            startActivity(Intent(this, RegisterActivity::class.java))

        }

    }

    private fun passwordRecoverDialog() {

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Recover Password")

        val linearLayout = LinearLayout(this)

        val emailEt = EditText(this)
        emailEt.hint = "Email"
        emailEt.inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        emailEt.minEms = 16

        linearLayout.addView(emailEt)
        linearLayout.setPadding(10,10,10,10)

        builder.setView(linearLayout)

        builder.setPositiveButton("Recover") { _, _ ->

            val email = emailEt.text.toString().trim()
            beginRecovery(email)

        }

        builder.setNegativeButton("Cancel") { dialog, _ ->

            dialog.dismiss()

        }

        //show dialog
        builder.create().show()

    }

    private fun beginRecovery(email: String) {

        progressDialog.setMessage("Sending email ...")
        progressDialog.show()

        mAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->

            progressDialog.dismiss()
            if (task.isSuccessful){

                Toast.makeText(this, "Email Sent", Toast.LENGTH_SHORT).show()

            }else{

                Toast.makeText(this, "Failed ...", Toast.LENGTH_SHORT).show()

            }

        }.addOnFailureListener { exception ->

            progressDialog.dismiss()
            Toast.makeText(this, "${exception.message}", Toast.LENGTH_SHORT).show()

        }

    }

    private fun prosesValidasiDataLogin() {

        val email = mEmailEt.text.toString().trim()
        val password = mPasswordEt.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            mEmailEt.error = "Invalid Email"
            mEmailEt.isFocusable = true

        } else {

            loginUser(email, password)

        }

    }

    private fun loginUser(email: String, password: String) {

        progressDialog.setMessage("Login User ...")
        progressDialog.show()

        mAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener { task ->

            if (task.isSuccessful){

                progressDialog.dismiss()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()

            }else{

                progressDialog.dismiss()
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()

            }

        }.addOnFailureListener { exception ->

            progressDialog.dismiss()
            Toast.makeText(this, "${exception.message}", Toast.LENGTH_SHORT).show()

        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {

                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)

            } catch (e: ApiException) {

                Toast.makeText(this, "${e.message}", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {

                    // Sign in success, update UI with the signed-in user's information
                    val user = mAuth.currentUser

                    if (task.result!!.additionalUserInfo!!.isNewUser){

                        val email = user!!.email
                        val uid = user.uid

                        val dbProfile = mDatabase.getReference("Users").child(uid)

                        val hashMap : HashMap<String, Any?>? = HashMap()
                        hashMap?.set("email", email!!)
                        hashMap?.set("uid", uid)
                        hashMap?.set("name", "UserName")
                        hashMap?.set("onlineStatus", "online")
                        hashMap?.set("typingTo", "noOne")
                        hashMap?.set("phone", "")
                        hashMap?.set("image",
                            "https://firebasestorage.googleapis.com/v0/b/socialapplication-79355.appspot.com/o/Users_Profile_Cover_Imgs%2Fuser_profil_default.png?alt=media&token=43b9ace0-5037-4c14-a5da-5722b0c2949a"
                        )
                        hashMap?.set("cover", "")

                        dbProfile.setValue(hashMap)

                    }

                    Toast.makeText(this, "${user!!.email}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    //updateUI(user)

                } else {

                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show()
                    //updateUI(null)
                }

            }.addOnFailureListener { exception ->

                Toast.makeText(this, "${exception.message}", Toast.LENGTH_SHORT).show()

            }

    }

}
