package com.example.coursego

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coursego.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MainActivity : BaseActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var auth : FirebaseAuth

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Add this line to disable dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        showProgressBar()
        // Fetch the username from Firebase
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val database = Firebase.database
            val myRef = database.getReference("Users").child(userId)
            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {

                    val userData = dataSnapshot.getValue(User::class.java)
                    val userName = userData?.name
                    if (userName != null) {
                        binding.welcome.text = "Hi, $userName"
                        dismissProgessBar()

                    } else {
                        binding.welcome.text = "Hi, Anonymous"
                        dismissProgessBar()
                        Log.e("MainActivity", "User name is null")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("MainActivity", "Failed to get user name", databaseError.toException())
                }
            })
        } else {
            Log.e("MainActivity", "User ID is null")
        }
        binding.btnSignOut.setOnClickListener {
            if(auth.currentUser != null){
                auth.signOut()
                startActivity(Intent(this,SignInActivity::class.java))
                finish()
            }
        }
    }
}
