package com.example.journal

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginPage : AppCompatActivity() {
    private val conn = FirebaseFirestore.getInstance()
    private lateinit var mAuth: FirebaseAuth
    private var loginAttempts = 3 // max amount of login attempts
    private val timeout = 30000L // 30 sec timeout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mAuth = FirebaseAuth.getInstance()
        setContentView(R.layout.activity_login_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logEmail: EditText = findViewById(R.id.loginEmail)
        val logPass: EditText = findViewById(R.id.loginPass)
        val lblLoginErr: TextView = findViewById(R.id.loginERR)
        val lblReg: TextView = findViewById(R.id.reg2)
        val btnLogin: Button = findViewById(R.id.btnLogin)

        lblReg.setOnClickListener {
            val intent = Intent(this, RegistrationPage::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            val email = logEmail.text.toString()
            val pass = logPass.text.toString()

            conn.collection("users")
                .whereEqualTo("email", email)
                .whereEqualTo("password", pass)
                .get()
                .addOnSuccessListener { users ->
                    if (!users.isEmpty) {
                        val userDoc = users.documents[0]
                        val userUID = userDoc.id
                        val fullname = userDoc.getString("fullname") ?: "No name found"
                        val userEmail = userDoc.getString("email") ?: "No email found"

                        // Proceed with Firebase Authentication
                        mAuth.signInWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Pass data to ProfilePage
                                    val intent = Intent(this, display_scroll::class.java).apply {
                                        putExtra("user_id", userUID)
                                        putExtra("fullname", fullname)
                                        putExtra("email", userEmail)
                                    }
                                    Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show()
                                    startActivity(intent)
                                    finish()
                                } else {
                                    lblLoginErr.text = "Authentication failed. Try again."
                                }
                            }
                    } else {
                        // Handle incorrect credentials
                        loginAttempts--
                        lblLoginErr.text = "Incorrect email or password." + loginAttempts + " attempts left."

                        if (loginAttempts <= 0) {
                            btnLogin.isEnabled = false
                            lblLoginErr.text = "Too many failed attempts. Please wait 30 seconds."
                            Handler().postDelayed({
                                loginAttempts = 3
                                btnLogin.isEnabled = true
                                lblLoginErr.text = ""
                            }, timeout)
                        }
                    }
                }
                .addOnFailureListener {
                    lblLoginErr.text = "An error occurred. Please try again later."
                }
        }
    }
}
