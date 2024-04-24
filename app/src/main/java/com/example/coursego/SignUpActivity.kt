package com.example.coursego
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coursego.databinding.ActivitySignUpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class SignUpActivity : BaseActivity() {
        private lateinit var binding: ActivitySignUpBinding
        private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            // Add this line to disable dark mode
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            binding = ActivitySignUpBinding.inflate(layoutInflater)
            enableEdgeToEdge()
            setContentView(binding.root)
            auth = Firebase.auth
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
            binding.tvLoginPage.setOnClickListener {
                startActivity(Intent(this@SignUpActivity,SignInActivity::class.java))
                finish()
            }
            binding.btnSignUp.setOnClickListener {
                registerUser()
            }
        }

    private fun registerUser()
    {
        val name = binding.etSinUpName.text.toString()
        val email = binding.etSinUpEmail.text.toString()
        val password = binding.etSinUpPassword.text.toString()
        val userData = User(name,email)
        if (validateForm(name, email, password))
        {
            showProgressBar()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener{task ->
                    if (task.isSuccessful)
                    {
                        val database = Firebase.database
                        val myRef = database.getReference("Users").child(auth.currentUser!!.uid)
                        myRef.setValue(userData).addOnSuccessListener {
                            Toast.makeText(this,"Successful.. Login Now..", Toast.LENGTH_SHORT).show()
                            dismissProgessBar()
                            startActivity(Intent(this,SignInActivity::class.java))
                            finish()
                        }

                    }
                    else{
                        Toast.makeText(this,"Oops! Something went wrong", Toast.LENGTH_SHORT).show()
                        dismissProgessBar()
                }
                }
        }
    }
    private fun validateForm(name:String, email:String,password:String):Boolean
    {
        return when {
            TextUtils.isEmpty(name)->{
                binding.tilName.error = "Enter name"
                false
            }
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
}