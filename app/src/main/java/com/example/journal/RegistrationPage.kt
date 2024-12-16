package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import android.content.Intent

class RegistrationPage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_register)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        var loginBtn2:TextView = findViewById(R.id.loginBtn2)
        var txtemail:EditText = findViewById(R.id.regEmail)
        var txtusername:EditText = findViewById(R.id.regUsername)
        var txtpass:EditText = findViewById(R.id.regPass)
        var txtcpass:EditText = findViewById(R.id.regCPass)
        var lblErEmail:TextView = findViewById(R.id.regERREmail)
        var lblErUser:TextView = findViewById(R.id.regERRUser)
        var lblErPass:TextView = findViewById(R.id.regERRPass)
        var lblErCPass:TextView = findViewById(R.id.regERRCPass)
        var btnSignup:Button = findViewById(R.id.btnSignup)

        loginBtn2.setOnClickListener {
            var email = txtemail.text.toString()
            var username = txtusername.text.toString()
            var pass = txtpass.text.toString()
            var cpass = txtcpass.text.toString()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}