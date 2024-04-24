package com.example.coursego

import android.os.Bundle
import android.text.TextUtils
import android.util.Patterns
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coursego.databinding.ActivityForgetPasswordBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class ForgetPasswordActivity : BaseActivity() {
    private lateinit var binding: ActivityForgetPasswordBinding
    private lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Add this line to disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityForgetPasswordBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        auth = Firebase.auth
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnForgotPasswordSubmit.setOnClickListener {
            resetPassword()
        }

        // Handle back press using OnBackPressedCallback
//        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                // Add any custom actions here (like before in onBackPressed())
//                finish()
//            }
//        })
    }

    private fun resetPassword(){
        val email = binding.etForgotPasswordEmail.text.toString()
        if(validateForm(email)){
            showProgressBar()
            auth.sendPasswordResetEmail(email).addOnCompleteListener{ task ->
                if(task.isSuccessful){
                    dismissProgessBar()
                    binding.etForgotPasswordEmail.visibility = View.GONE
                    binding.tvSubmitMsg.visibility = View.VISIBLE
                    binding.tilEmailForgetPassword.visibility = View.GONE
                    binding.btnForgotPasswordSubmit.visibility = View.GONE
                }
                else{
                    dismissProgessBar()
                    showToast(this,"Cannot Reset your Password.. Try again later..")
                }
            }
        }
    }
    private fun validateForm(email:String):Boolean
    {
        return when {
            TextUtils.isEmpty(email) && !Patterns.EMAIL_ADDRESS.matcher(email).matches()->{
                binding.etForgotPasswordEmail.error = "Enter valid email address"
                false
            }
            else -> { true }
        }
    }
}