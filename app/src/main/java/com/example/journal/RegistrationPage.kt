package com.example.journal

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth


class RegistrationPage : AppCompatActivity() {
    val conn = FirebaseFirestore.getInstance()
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

        var loginBtn2: TextView = findViewById(R.id.loginBtn2)
        var txtemail: EditText = findViewById(R.id.regEmail)
        var txtfname: EditText = findViewById(R.id.regFullname)
        var txtpass: EditText = findViewById(R.id.regPass)
        var txtcpass: EditText = findViewById(R.id.regCPass)
        var lblErEmail: TextView = findViewById(R.id.regERREmail)
        var lblErPass: TextView = findViewById(R.id.regERRPass)
        var lblErCPass: TextView = findViewById(R.id.regERRCPass)
        var btnSignup: Button = findViewById(R.id.btnSignup)

        loginBtn2.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
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
                            // Proceed to register the user
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
                                                "password" to pass
                                            )
                                            conn.collection("users").document(uid)
                                                .set(newUser)
                                                .addOnSuccessListener {
                                                    // Redirect to ProfilePage
                                                    val intent = Intent(this, ProfilePage::class.java).apply {
                                                        putExtra("user_id", uid)
                                                        putExtra("fullname", fname)
                                                        putExtra("email", email)
                                                    }
                                                    startActivity(intent)
                                                    finish()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(
                                                        this,
                                                        "Failed to save user data: ${e.message}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Registration failed: ${task.exception?.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error checking email: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}