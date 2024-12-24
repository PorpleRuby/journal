package com.example.journal

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import org.checkerframework.checker.units.qual.min
import kotlin.random.Random


class RegistrationPage : AppCompatActivity() {
    val conn = FirebaseFirestore.getInstance()
    fun generateUniqueUID(onUIDGenerated: (Int) -> Unit) {
        val uid = Random.nextInt(0, 1000000)
        conn.collection("users")
            .whereEqualTo("user_id", uid)
            .get()
            .addOnSuccessListener { record ->
                if (record.isEmpty) {
                    // UID is unique
                    onUIDGenerated(uid)
                } else {
                    // UID exists, try again
                    generateUniqueUID(onUIDGenerated)
                }
            }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            var email = txtemail.text.toString()
            var fname = txtfname.text.toString()
            var pass = txtpass.text.toString()
            var cpass = txtcpass.text.toString()
            var valid = true

            if (email.isEmpty() || fname.isEmpty() || pass.isEmpty()) {
                lblErCPass.text = "Please fill up all fields."
                valid = false
            }

            if (pass.length < 8) {
                lblErPass.text =
                    "The password is too short. Please make it a minimum of 8 characters."
                valid = false
            }
            if (pass != cpass && pass.length >= 8) {
                lblErCPass.text = "The passwords do not match. Please try again."
                txtpass.text = null
                txtcpass.text = null
                valid = false
            }

            conn.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { users ->
                    if (!users.isEmpty) {
                        // Username already exists
                        lblErEmail.text = "An account with this email already exists."
                        valid = false
                    }

                    if (valid) {
                        // Generate unique UID and register user
                        generateUniqueUID { uid ->
                            val newUser = hashMapOf(
                                "user_id" to uid,
                                "email" to email,
                                "fullname" to fname,
                                "password" to pass
                            )
                            conn.collection("users").add(newUser)
                                .addOnSuccessListener {
                                    val intent = Intent(this, ProfilePage::class.java)
                                    startActivity(intent)
                                }
                        }
                    }
                }
        }
    }
}