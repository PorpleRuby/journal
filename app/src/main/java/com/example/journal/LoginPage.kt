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

class LoginPage : AppCompatActivity() {
    val conn = FirebaseFirestore.getInstance()
    private lateinit var mAuth: FirebaseAuth

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

        // things in the layout
        var logEmail: EditText = findViewById(R.id.loginEmail)
        var logPass: EditText = findViewById(R.id.loginPass)
        var lblLoginErr: TextView = findViewById(R.id.loginERR)
        var lblReg: TextView = findViewById(R.id.reg2)
        var btnLogin: Button = findViewById(R.id.btnLogin)
        // var counter = 3 // login attempts before disabling login button

        // clicked register button
        lblReg.setOnClickListener {
            val intent = Intent(this, RegistrationPage::class.java)
            startActivity(intent)
        }

        btnLogin.setOnClickListener {
            var email = logEmail.text.toString()
            var pass = logPass.text.toString()
            conn.collection("users")
                .whereEqualTo("email", email).whereEqualTo("password", pass)
                .get()
                .addOnSuccessListener { users ->
                    // Account is there, I'm supposed to make this more complicated probably but I don't know what to use
                    if (!users.isEmpty) {
                        mAuth.signInWithEmailAndPassword(email, pass)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // Sign in success, update UI with the signed-in user's information
                                    val user = mAuth.currentUser
                                }
                            }
                        val intent = Intent(this, ProfilePage::class.java)
                        startActivity(intent)
                    }
                    // Vague on what is incorrect for security measures
                    else {
                        lblLoginErr.text = "Email or password is incorrect."
                        /* counter -= 1
                        if (counter == 0) {
                            Handler().postDelayed(
                                { btnLogin.setEnabled(false) }, 5000 //Login button disabled for 5 sec
                            )
                        } */
                    }
                }
        }
    }
}
