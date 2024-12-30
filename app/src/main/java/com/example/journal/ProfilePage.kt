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
    private var userUID: String? = null // Declare userUID at the class level

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imagePickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val selectedImageUri = data?.data
                if (selectedImageUri != null) {
                    userUID?.let {
                        setProfilePic(it, selectedImageUri)
                    }
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
        val homeBtn: ImageView = findViewById(R.id.home_button_scroll)
        val addEntry: ImageView = findViewById(R.id.add_entry_scroll)
        val profileBtn: ImageView = findViewById(R.id.profileButtonScroll)
        lblFname = findViewById(R.id.displayFname)
        lblEmail = findViewById(R.id.displayEmail)
        pfp = findViewById(R.id.pfp)
        changePfp = findViewById(R.id.changePfp)
        btnEdit = findViewById(R.id.btnEdit)
        btnLogout = findViewById(R.id.btnLogout)

        database = FirebaseDatabase.getInstance().reference
        mAuth = FirebaseAuth.getInstance()

        showUserData()

        // redirect to entries display
        homeBtn.setOnClickListener {
            val intent = Intent(this, display_scroll::class.java).apply {
                putExtra("user_id", userUID)
            }
            startActivity(intent)
        }

        // add entry
        addEntry.setOnClickListener {
            val intent = Intent(this, entry_form::class.java)
            startActivity(intent)
        }

        // redirect to profile page
        profileBtn.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        btnEdit.setOnClickListener {
            userUID?.let { uid -> // Use a safe call to ensure `userUID` is not null
                val intent = Intent(this, EditProfile::class.java)
                intent.putExtra("user_id", uid) // Pass the user ID
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "User ID not available.", Toast.LENGTH_SHORT).show()
            }
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
        userUID = mAuth.currentUser?.uid // Initialize userUID from FirebaseAuth

        if (userUID != null) {
            conn.collection("users").document(userUID!!).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullname = document.getString("fullname")
                        val email = document.getString("email")
                        val profilePicUrl = document.getString("profile_picture_url")

                        lblFname.text = fullname ?: "No name found" //if credentials aren't found
                        lblEmail.text = email ?: "No email found"

                        if (profilePicUrl == "default") {
                            // Load the default drawable / pfp
                            pfp.setImageResource(R.drawable.cute_pfp_default)
                        } else if (!profilePicUrl.isNullOrEmpty()) {
                            // Load the URL image
                            Glide.with(this).load(profilePicUrl).circleCrop().into(pfp)
                        } else {
                            // Fallback to the default drawable if above fails
                            pfp.setImageResource(R.drawable.cute_pfp_default)
                        }
                    }
                }
        }
    }

    private fun setProfilePic(userUID: String, newImageUri: Uri) {
        val newImageUrl = newImageUri.toString() // Get the URI of the uploaded image
        val userRef = conn.collection("users").document(userUID)

        userRef.update("profile_picture_url", newImageUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()
                Glide.with(this).load(newImageUrl).circleCrop().into(pfp) // Update the UI
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}