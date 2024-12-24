package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import android.content.Intent
import com.google.firebase.firestore.FirebaseFirestore

class LoginPage : AppCompatActivity() {
    val conn = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_login)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var logEmail: EditText = findViewById(R.id.loginEmail)
        var logPass: EditText = findViewById(R.id.loginPass)
        var lblLoginErr: TextView = findViewById(R.id.loginERR)
        var btnLogin: Button = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            var email = logEmail.text.toString()
            var pass = logPass.text.toString()
            conn.collection("users")
                .whereEqualTo("email", email).whereEqualTo("password", pass)
                .get()
                .addOnSuccessListener { users ->
                    if (!users.isEmpty) {
                        val intent = Intent(this, ProfilePage::class.java)
                        startActivity(intent)
                    }
                    else {
                        lblLoginErr.text = "Email or password is incorrect."
                    }
                }
        }
    }
}