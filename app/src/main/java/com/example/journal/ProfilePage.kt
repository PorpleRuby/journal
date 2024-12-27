package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.DatabaseReference
import com.google.firebase.firestore.FirebaseFirestore

class ProfilePage : AppCompatActivity() {

    private val conn = FirebaseFirestore.getInstance()
    private lateinit var lblUID: TextView
    private lateinit var lblFname: TextView
    private lateinit var lblEmail: TextView
    private lateinit var btnEdit: TextView
    private lateinit var btnLogout: TextView
    private lateinit var pfp: ImageView
    private lateinit var changePfp: ImageView
    private lateinit var database: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lblUID = findViewById(R.id.displayUID)
        lblFname = findViewById(R.id.displayFname)
        lblEmail = findViewById(R.id.displayEmail)
        pfp = findViewById(R.id.pfp)
        changePfp = findViewById(R.id.changePfp)
        btnEdit = findViewById(R.id.btnEdit)
        btnLogout = findViewById(R.id.btnLogout)

        database = FirebaseDatabase.getInstance().reference
        mAuth = FirebaseAuth.getInstance()

        showUserData();

        btnEdit.setOnClickListener {

        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") {
                        _,_->
                    // Sign out from Firebase
                    mAuth.signOut()

                    // Redirect to Login Page
                    val intent = Intent(this, LoginPage::class.java)
                    startActivity(intent)

                    // Show a toast message for confirmation
                    Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun showUserData() {
            val userUID = intent.getStringExtra("user_id")

            if (userUID != null) {
                val userRef = FirebaseFirestore.getInstance().collection("users").document(userUID)
                userRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullname = document.getString("fullname")
                        val email = document.getString("email")

                        lblFname.text = fullname ?: "No name found"
                        lblEmail.text = email ?: "No email found"
                        lblUID.text = "User ID:" + userUID
                    } else {
                        Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }
    }