package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

class ProfilePage : AppCompatActivity() {
    private lateinit var imageView: ImageView
    private lateinit var updateButton: Button
    private var imageUri: Uri? = null
    private lateinit var storageReference: StorageReference

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Initialize Firebase Storage
        storageReference = FirebaseStorage.getInstance().reference

        // Handle button click
        updateButton.setOnClickListener {
            openFileChooser()
        }
    }

        private fun openFileChooser() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
                imageUri = data.data
                imageView.setImageURI(imageUri) // Preview image locally
                uploadImageToFirebase()
            }
        }

        private fun uploadImageToFirebase() {
            if (imageUri != null) {
                val fileReference = storageReference.child("profile_pictures/${UUID.randomUUID()}.jpg")
                val uploadTask = fileReference.putFile(imageUri!!)

                uploadTask.addOnSuccessListener {
                    fileReference.downloadUrl.addOnSuccessListener { uri ->
                        // Use the download URL to save in Firestore or elsewhere
                        println("Image uploaded successfully: $uri")
                    }
                }.addOnFailureListener {
                    println("Upload failed: ${it.message}")
                }
            }
        }
    }