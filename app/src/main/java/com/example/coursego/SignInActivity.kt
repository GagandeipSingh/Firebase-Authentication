//By One Tap Method

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

//By Credential Manager
//Add these Dependencies in app level gradle
    // implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    // implementation("androidx.credentials:credentials:1.3.0-alpha03")
    // implementation("androidx.credentials:credentials-play-services-auth:1.3.0-alpha03")
    // implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.0")
package com.example.coursego

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.credentials.GetCredentialException
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.nextcourse.databinding.ActivitySignInBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID

class SignInActivity : BaseActivity() {
    private lateinit var binding:ActivitySignInBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseCredential:AuthCredential
    private lateinit var name:String
    private lateinit var email:String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        firebaseAuth = FirebaseAuth.getInstance()
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
            signIn(this)
        }
    }
    private fun signInUser(){
        val email = binding.etSinInEmail.text.toString()
        val password = binding.etSinInPassword.text.toString()
        if(validateForm(email,password)){
            showProgressBar()
            firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        startActivity(Intent(this,MainActivity::class.java))
                        finish()
                        dismissProgessBar()
                    }
                    else{
                        Toast.makeText(this,"Can't Login..",Toast.LENGTH_SHORT).show()
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
    private fun signIn(context: SignInActivity) {
        val credentialManager = CredentialManager.create(this)
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .setNonce(hashedNonce)
            .build()
        val request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )
                val credential = result.credential
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(credential.data)
                val googleIdToken = googleIdTokenCredential.idToken

                // Use the ID token to authenticate with Firebase
                firebaseCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                firebaseAuth.signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(this@SignInActivity) { task ->
                        if (task.isSuccessful) {
                            // Sign in success
                            getDetails()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.exception)
                            Toast.makeText(context, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (@SuppressLint("NewApi", "LocalSuppress") e: GetCredentialException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                dismissProgessBar()
            } catch (e: GoogleIdTokenParsingException) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                dismissProgessBar()
            } catch (e: CancellationException) {
                // Handle cancellation
                Toast.makeText(context, "Account selection was cancelled", Toast.LENGTH_SHORT).show()
                dismissProgessBar()
            }catch (e: GetCredentialCancellationException) {
                // Handle the exception here
                Toast.makeText(context, "Account selection was cancelled", Toast.LENGTH_SHORT).show()
                dismissProgessBar()
            }catch (e: NoCredentialException) {
                Toast.makeText(context, "Too many sign-in attempts. Please try again later.", Toast.LENGTH_SHORT).show()
                dismissProgessBar()
            }
        }
    }

    private fun getDetails(){
        val user = firebaseAuth.currentUser
        user?.let {
            name = it.displayName.toString()
            email = it.email.toString()
//            Log.d("SignInActivity", "Name: $name, Email: $email")
            val database = Firebase.database
            val myRef = database.getReference("Users").child(user.uid)
            myRef.setValue(User(name,email)).addOnSuccessListener {
                startActivity(Intent(this,MainActivity::class.java))
                finish()
            }
        }
    }
}

