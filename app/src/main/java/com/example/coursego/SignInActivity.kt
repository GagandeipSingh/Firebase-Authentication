@file:Suppress("DEPRECATION")

package com.example.coursego
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coursego.databinding.ActivitySigninBinding
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignInActivity : BaseActivity() {
        private lateinit var binding: ActivitySigninBinding
        private lateinit var auth: FirebaseAuth
        private var oneTapClient: SignInClient?=null
        private lateinit var signInRequest: BeginSignInRequest
        private lateinit var name : String
        private lateinit var email: String

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Add this line to disable dark mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            binding = ActivitySigninBinding.inflate(layoutInflater)
            enableEdgeToEdge()
            setContentView(binding.root)
            auth = Firebase.auth
            oneTapClient = Identity.getSignInClient(this)
            signInRequest = BeginSignInRequest.builder()
                .setGoogleIdTokenRequestOptions(
                    BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                        .setSupported(true)
                        // Your server's client ID, not your Android client ID.
                        .setServerClientId(getString(R.string.default_web_client_id))
                        // Only show accounts previously used to sign in when true
                        .setFilterByAuthorizedAccounts(false)
                        .build())
                .build()
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            binding.tvRegister.setOnClickListener{
                startActivity(Intent(this@SignInActivity,SignUpActivity::class.java))
                finish()
            }
            binding.tvForgotPassword.setOnClickListener {
                startActivity(Intent(this@SignInActivity,ForgetPasswordActivity::class.java))
            }
            binding.btnSignIn.setOnClickListener {
                signInUser()
            }
            binding.btnSignInWithGoogle.setOnClickListener {
                showProgressBar()
                signingGoogle(this)
            }
        }
    private fun signInUser(){
        val email = binding.etSinInEmail.text.toString()
        val password = binding.etSinInPassword.text.toString()
        if(validateForm(email,password)){
            showProgressBar()
            auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                        dismissProgessBar()
                    }
                    else{
                        showToast(this,"Can't Login..")
                        dismissProgessBar()
                    }
                }
        }
    }
    private fun validateForm(email:String,password:String):Boolean
    {
        return when {
            TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()->{
                binding.tilEmail.error = "Enter valid email address"
                false
            }
            TextUtils.isEmpty(password)->{
                binding.tilPassword.error = "Enter password"
                false
            }
            else -> { true }
        }
    }

    private fun signingGoogle(view: SignInActivity){
        CoroutineScope(Dispatchers.Main).launch{
            signingGoogle()
        }
    }
    private suspend fun signingGoogle(){
        val result = oneTapClient?.beginSignIn(signInRequest)?.await()
        val intentSenderRequest = IntentSenderRequest.Builder(result!!.pendingIntent).build()
        activityResultLauncher.launch(intentSenderRequest) // Launch the sign-in intent
        dismissProgessBar()
    }


    private val activityResultLauncher : ActivityResultLauncher<IntentSenderRequest> = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            try {
                val googleCredential = oneTapClient!!.getSignInCredentialFromIntent(result.data)
                val idToken = googleCredential.googleIdToken
                when {
                    idToken != null -> {
                        // Got an ID token from Google. Use it to authenticate
                        // with Firebase.
                        showProgressBar()
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        auth.signInWithCredential(firebaseCredential)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    getDetails()
                                }
                            }
                    }
                }
            }catch (e:ApiException){
                e.printStackTrace()
            }
        }
    }
    private fun getDetails(){
        val user = Firebase.auth.currentUser
        user?.let {
            name = it.displayName.toString()
            email = it.email.toString()
//            Log.d("SignInActivity", "Name: $name, Email: $email")
            val database = Firebase.database
            val myRef = database.getReference("Users").child(auth.currentUser!!.uid)
            myRef.setValue(User(name,email)).addOnSuccessListener {
                startActivity(Intent(this,MainActivity::class.java))
                dismissProgessBar()
                finish()
            }
        }
    }
    }
