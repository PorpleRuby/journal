package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.Intent
import android.widget.ImageView
import com.google.firebase.auth.FirebaseAuth

class entry_form : AppCompatActivity() {

    private val conn = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Firebase Auth instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_form)
        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val title: EditText = findViewById(R.id.title_field)
        val entry: EditText = findViewById(R.id.content_field)
        val submitEntry: Button = findViewById(R.id.submit_entry)
        val backBtn: ImageView = findViewById(R.id.back_entry_btn)

        submitEntry.setOnClickListener {
            val diaryTitle = title.text.toString().ifEmpty { "Untitled" } // Use "Untitled" if no title is provided
            val diaryEntry = entry.text.toString()

            // Get the current user ID
            val currentUser = auth.currentUser
            val userId = currentUser?.uid

            if (userId != null) {
                // Store the diary entry with title, date, and user ID
                val newEntry = hashMapOf(
                    "title" to diaryTitle,
                    "journal_entry" to diaryEntry,
                    "created_at" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "user_id" to userId // Associate the entry with the user
                )

                // Save to Firestore
                conn.collection("journal_entries").add(newEntry)

                // Navigate to another activity
                val intent = Intent(this, display_scroll::class.java)
                startActivity(intent)
            } else {
                // Handle the case where the user is not authenticated
                showErrorDialog("You must be logged in to add an entry.")
            }
        }

        backBtn.setOnClickListener {
            if (entry.text.isNotEmpty() || title.text.isNotEmpty()) {
                // Show discard confirmation dialog
                showDiscardDialog()
            } else {
                // Navigate back without confirmation if there's no text
                val intent = Intent(this, display_scroll::class.java)
                startActivity(intent)
            }
        }
    }

    private fun showDiscardDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Discard writing?")
            .setPositiveButton("Yes") { _, _ ->
                // Navigate back to the previous activity
                val intent = Intent(this, display_scroll::class.java)
                startActivity(intent)
            }
            .setNegativeButton("No") { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun showErrorDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }
}