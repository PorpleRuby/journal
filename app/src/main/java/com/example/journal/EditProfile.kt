package com.example.journal

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore


class EditProfile : AppCompatActivity() {
    private val conn = FirebaseFirestore.getInstance()
    private lateinit var lblUID: EditText
    private lateinit var lblFname: EditText
    private lateinit var lblEmail: EditText
    private lateinit var lblPass: EditText

    private lateinit var errorEmail :TextView
    private lateinit var errorPass :TextView

    private lateinit var btnCopy: ImageView
    private lateinit var btnToggle: ImageView
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var pfp: ImageView
    private lateinit var changePfp: ImageView

    private lateinit var database: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var imagePickLauncher: ActivityResultLauncher<Intent>
    private var selectedImageUri: Uri? = null

    private var originalFullName: String? = null
    private var originalEmail: String? = null
    private var originalPassword: String? = null
    private var originalProfilePicUrl: String? = null

    private var userUID: String? = null // Declare userUID at the class level

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        imagePickLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                selectedImageUri = data?.data // Update the class-level variable
                if (selectedImageUri != null) {
                    userUID?.let {
                        setProfilePic(it, selectedImageUri!!)
                    }
                }
            }
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lblUID = findViewById(R.id.displayEditUID)
        lblFname = findViewById(R.id.displayEditFname)
        lblEmail = findViewById(R.id.displayEditEmail)
        lblPass = findViewById(R.id.displayEditPass)
        pfp = findViewById(R.id.displayEditPfp)
        changePfp = findViewById(R.id.displayEditChangePfp)
        errorEmail = findViewById(R.id.errEditEmail)
        errorPass = findViewById(R.id.errEditPass)

        btnCopy = findViewById(R.id.btnCopy)
        btnToggle = findViewById(R.id.btnToggle)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        database = FirebaseDatabase.getInstance().reference
        mAuth = FirebaseAuth.getInstance()

        showUserData()

        changePfp.setOnClickListener {
            ImagePicker.with(this)
                .cropSquare()
                .compress(512)
                .maxResultSize(512, 512)
                .createIntent { intent ->
                    imagePickLauncher.launch(intent)
                }
        }

        btnCopy.setOnClickListener {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val userId = lblUID.text.toString() // Get the user ID text from lblUID
            val clip = ClipData.newPlainText("User ID", userId) // Label and text to copy
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "User ID copied to clipboard.", Toast.LENGTH_SHORT).show()
        }

        var isPasswordVisible = false

        btnToggle.setOnClickListener {
            if (isPasswordVisible) {
                // Hide password
                lblPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                btnToggle.setImageResource(R.drawable.baseline_toggle_on_24) // Change image when hiding password
            } else {
                // Show password
                lblPass.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                btnToggle.setImageResource(R.drawable.baseline_toggle_off_24) // Change image when showing password
            }
            isPasswordVisible = !isPasswordVisible // Toggle the state
            lblPass.setSelection(lblPass.text.length) // Move the cursor to the end
        }

        btnCancel.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit? Your changes will not be saved.")
                .setPositiveButton("Yes") { _, _ ->
                    val intent = Intent(this, ProfilePage::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("No", null)
                .show()
        }

        btnSave.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Save Changes")
                .setMessage("Save your changes?")
                .setPositiveButton("Yes") { _, _ ->
                    val email = lblEmail.text.toString()
                    val fname = lblFname.text.toString()
                    val pass = lblPass.text.toString()

                    var valid = true
                    errorEmail.text = ""
                    errorPass.text = ""

                    if (email.isEmpty() || fname.isEmpty() || pass.isEmpty()) {
                        errorEmail.text = "Please fill up all fields."
                        valid = false
                    }

                    if (pass.length < 8 || !pass.contains("[A-Za-z0-9!\"#$%&'()*+,-./:;\\\\<=>?@\\[\\]^_`{|}~]".toRegex())) {
                        errorPass.text =
                            "The password does not follow the policy. It must have a minimum of 8 characters, have an uppercase, lowercase, special character, and a number."
                        valid = false
                    }

                    if (valid) {
                        conn.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { users ->
                                if (!users.isEmpty) {
                                    lblEmail.text = Editable.Factory.getInstance()
                                        .newEditable(originalEmail ?: "")
                                    errorEmail.text = "An account with this email already exists."
                                } else {
                                    val userUID = mAuth.currentUser?.uid
                                    if (userUID != null) {
                                        val updatedUser = hashMapOf(
                                            "fullname" to fname,
                                            "email" to email,
                                            "password" to pass,
                                            "profile_picture_url" to (selectedImageUri?.toString()
                                                ?: originalProfilePicUrl ?: "default")
                                        )
                                        conn.collection("users").document(userUID)
                                            .update(updatedUser as Map<String, Any>)
                                            .addOnSuccessListener {
                                                Toast.makeText(
                                                    this,
                                                    "Profile updated successfully!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                finish() // Return to profile page
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(
                                                    this,
                                                    "Failed to update profile: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                // Revert changes
                                                lblFname.text = Editable.Factory.getInstance()
                                                    .newEditable(originalFullName ?: "")
                                                lblEmail.text = Editable.Factory.getInstance()
                                                    .newEditable(originalEmail ?: "")
                                                lblPass.text = Editable.Factory.getInstance()
                                                    .newEditable(originalPassword ?: "")
                                            }
                                    }
                                }
                            }
                    }
                }.setNegativeButton("No", null)
                .show()
        }
    }

    private fun showUserData() {
            val userUID = intent.getStringExtra("user_id")

            if (userUID != null) {
                conn.collection("users").document(userUID).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            originalFullName = document.getString("fullname")
                            originalEmail = document.getString("email")
                            originalPassword = document.getString("password")
                            originalProfilePicUrl = document.getString("profile_picture_url")

                            lblFname.text = Editable.Factory.getInstance().newEditable(originalFullName ?: "")
                            lblEmail.text = Editable.Factory.getInstance().newEditable(originalEmail ?: "")
                            lblPass.text = Editable.Factory.getInstance().newEditable(originalPassword ?: "")
                            lblUID.text = Editable.Factory.getInstance().newEditable(userUID ?: "")

                            if (originalProfilePicUrl == "default") {
                                pfp.setImageResource(R.drawable.cute_pfp_default)
                            } else if (!originalProfilePicUrl.isNullOrEmpty()) {
                                Glide.with(this).load(originalProfilePicUrl).circleCrop().skipMemoryCache(true).diskCacheStrategy(
                                    DiskCacheStrategy.NONE).into(pfp)

                            } else {
                                pfp.setImageResource(R.drawable.cute_pfp_default)
                            }
                        } else {
                            Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to fetch user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

    private fun setProfilePic(userUID: String, newImageUri: Uri) {
        val newImageUrl = newImageUri.toString()
        val userRef = conn.collection("users").document(userUID)

        userRef.update("profile_picture_url", newImageUrl)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile picture updated successfully!", Toast.LENGTH_SHORT).show()

                // Refresh the activity
                finish() // Close the current instance
                startActivity(intent) // Restart the activity
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update profile picture: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}