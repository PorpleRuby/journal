package com.example.journal

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.dhaval2404.imagepicker.ImagePicker
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
    private lateinit var imagePickLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imagePickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                data?.data?.let { uri ->
                    selectedImageUri = uri
                    setProfilePic(this, selectedImageUri, pfp)
                }
            }
        }

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

        showUserData()

        btnEdit.setOnClickListener {
            val intent = Intent(this, LoginPage::class.java)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    mAuth.signOut()
                    val intent = Intent(this, LoginPage::class.java)
                    startActivity(intent)
                    Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("No", null)
                .show()
        }

        changePfp.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent ->
                    imagePickLauncher.launch(intent)
                }
        }
    }

    private fun showUserData() {
        val userUID = intent.getStringExtra("user_id")
        val profilePicUrl = intent.getStringExtra("profile_picture_url")

        if (userUID != null) {
            conn.collection("users").document(userUID).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        lblFname.text = document.getString("fullname") ?: "No name found"
                        lblEmail.text = document.getString("email") ?: "No email found"
                        lblUID.text = "User ID: $userUID"
                        val imageUrl = document.getString("profile_picture_url") ?: profilePicUrl
                        Glide.with(this).load(imageUrl).circleCrop().into(pfp)
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

    private fun setProfilePic(context: Activity, imageUri: Uri?, imageView: ImageView) {
        Glide.with(context).load(imageUri)
            .apply(RequestOptions.circleCropTransform())
            .into(imageView)
    }
}
