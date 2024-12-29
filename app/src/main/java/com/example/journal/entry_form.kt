package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.Intent

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
        val moodUser: EditText = findViewById(R.id.mood_entry) // Mood field
        val submitEntry: Button = findViewById(R.id.submit_entry)
        val backBtn: ImageView = findViewById(R.id.back_entry_btn)

        submitEntry.setOnClickListener {
            val diaryTitle = title.text.toString().ifEmpty { "Untitled" }
            val diaryEntry = entry.text.toString()
            val currentUser = auth.currentUser
            val userId = currentUser?.uid
            val mood = moodUser.text.toString()

            if (userId != null) {
                val newEntry = hashMapOf(
                    "title" to diaryTitle,
                    "journal_entry" to diaryEntry,
                    "mood" to mood,
                    "created_at" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    "user_id" to userId
                )

                conn.collection("journal_entries").add(newEntry).addOnSuccessListener {
                    val intent = Intent(this, display_scroll::class.java)
                    intent.putExtra("user_id", userId) // Pass the user_id to the next activity
                    startActivity(intent)
                }.addOnFailureListener {
                    showErrorDialog("Failed to save entry. Please try again.")
                }
            } else {
                showErrorDialog("You must be logged in to add an entry.")
            }
        }

        backBtn.setOnClickListener {
            val currentUser = auth.currentUser
            val userId = currentUser?.uid

            if (entry.text.isNotEmpty() || title.text.isNotEmpty() || moodUser.text.isNotEmpty()) {
                // Show discard confirmation dialog
                showDiscardDialog(userId)
            } else {
                // Navigate back without confirmation if there's no text
                if (userId != null) {
                    val intent = Intent(this, display_scroll::class.java)
                    intent.putExtra("user_id", userId) // Pass the user_id to the next activity
                    startActivity(intent)
                } else {
                    showErrorDialog("User is not logged in.")
                }
            }
        }
    }

    private fun showDiscardDialog(userId: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Discard writing?")
            .setPositiveButton("Yes") { _, _ ->
                // Navigate back to the previous activity with userId
                if (userId != null) {
                    val intent = Intent(this, display_scroll::class.java)
                    intent.putExtra("user_id", userId) // Pass the user_id to the next activity
                    startActivity(intent)
                } else {
                    showErrorDialog("User is not logged in.")
                }
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
