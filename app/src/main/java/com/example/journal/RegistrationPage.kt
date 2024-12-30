package com.example.journal

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegistrationPage : AppCompatActivity() {
    private val conn = FirebaseFirestore.getInstance()
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_registration_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_register)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val loginBtn2: TextView = findViewById(R.id.loginBtn2)
        val txtemail: EditText = findViewById(R.id.regEmail)
        val txtfname: EditText = findViewById(R.id.regFullname)
        val txtpass: EditText = findViewById(R.id.regPass)
        val txtcpass: EditText = findViewById(R.id.regCPass)
        val lblErEmail: TextView = findViewById(R.id.regERREmail)
        val lblErPass: TextView = findViewById(R.id.regERRPass)
        val lblErCPass: TextView = findViewById(R.id.regERRCPass)
        val btnSignup: Button = findViewById(R.id.btnSignup)

        loginBtn2.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }

        btnSignup.setOnClickListener {
            val email = txtemail.text.toString()
            val fname = txtfname.text.toString()
            val pass = txtpass.text.toString()
            val cpass = txtcpass.text.toString()
            var valid = true

            if (email.isEmpty() || fname.isEmpty() || pass.isEmpty()) {
                lblErCPass.text = "Please fill up all fields."
                valid = false
            }

            if (pass.length < 8 || !pass.contains("[A-Za-z0-9!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]".toRegex())) {
                lblErPass.text =
                    "The password does not follow the policy. It must have a minimum of 8 characters, have an uppercase, lowercase, special character, and a number."
                valid = false
            }
            if (pass != cpass) {
                lblErCPass.text = "The passwords do not match. Please try again."
                txtpass.text = null
                txtcpass.text = null
                valid = false
            }

            if (valid) {
                conn.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { users ->
                        if (!users.isEmpty) {
                            lblErEmail.text = "An account with this email already exists."
                            txtemail.text = null
                        } else {
                            mAuth.createUserWithEmailAndPassword(email, pass)
                                .addOnCompleteListener(this) { task ->
                                    if (task.isSuccessful) {
                                        val user = mAuth.currentUser
                                        val uid = user?.uid
                                        if (uid != null) {
                                            val newUser = hashMapOf(
                                                "user_id" to uid,
                                                "email" to email,
                                                "fullname" to fname,
                                                "password" to pass,
                                                "profile_picture_url" to "default"
                                            )
                                            conn.collection("users").document(uid)
                                                .set(newUser)
                                                .addOnSuccessListener {
                                                    // Redirect to ProfilePage
                                                    Toast.makeText(this, "Registered successfully.", Toast.LENGTH_SHORT).show()
                                                    val intent = Intent(this, ProfilePage::class.java).apply {
                                                        putExtra("user_id", uid)
                                                        putExtra("fullname", fname)
                                                        putExtra("email", email)
                                                    }
                                                    startActivity(intent)
                                                    finish()
                                                }
                                        }
                                    }
                                }
                        }
                    }
            }
        }
    }
}
