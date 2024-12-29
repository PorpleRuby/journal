package com.example.journal

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class EntryDetailActivity : AppCompatActivity() {
    private val conn = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // Firebase Auth instance
    private var initialTitle: String? = null
    private var initialContent: String? = null
    private var initialMood: String? = null
    private var isSaved: Boolean = false // Track if changes were saved

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entry_detail)

        // Adjust system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val backBtn: ImageView = findViewById(R.id.back_detail_btn)
        val saveBtn: Button = findViewById(R.id.save_button)
        val titleEditText: EditText = findViewById(R.id.detail_title)
        val contentEditText: EditText = findViewById(R.id.detail_content)
        val moodEditText: EditText = findViewById(R.id.mood_entry_details) // New field

        // Initially hide the save button
        saveBtn.visibility = View.INVISIBLE

        // Get the record ID from the Intent
        val recordId = intent.getStringExtra("recordId")
        if (recordId.isNullOrEmpty()) {
            Log.e("EntryDetailActivity", "Record ID is null or empty")
            return
        }

        // Fetch the specific record from Firestore
        conn.collection("journal_entries").document(recordId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val title = document.getString("title") ?: "Untitled"
                    val content = document.getString("journal_entry") ?: "No Content"
                    val mood = document.getString("mood") ?: "No Mood" // Fetch mood
                    val userId = document.getString("user_id") ?: "Unknown User" // Fetch userId

                    initialTitle = title
                    initialContent = content
                    initialMood = mood

                    titleEditText.setText(title)
                    contentEditText.setText(content)
                    moodEditText.setText(mood)

                    val createdAt = document.getString("created_at")
                    val lblDate: TextView = findViewById(R.id.detail_date)

                    if (!createdAt.isNullOrEmpty()) {
                        try {
                            val createdDate = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            lblDate.text = createdDate.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
                        } catch (e: DateTimeParseException) {
                            lblDate.text = "Invalid Date"
                            Log.e("DEBUG", "Date parsing error: ${e.message}")
                        }
                    } else {
                        lblDate.text = "No Date Provided"
                    }

                    // Pass the userId to back button functionality
                    backBtn.setOnClickListener {
                        val title = titleEditText.text.toString().trim()
                        val content = contentEditText.text.toString().trim()
                        val mood = moodEditText.text.toString().trim()

                        if (isSaved || (title == initialTitle && content == initialContent && mood == initialMood)) {
                            // Pass userId to the next activity
                            val intent = Intent(this, display_scroll::class.java)
                            intent.putExtra("user_id", userId) // Pass the user_id here
                            startActivity(intent)
                        } else {
                            showDiscardDialog(userId)
                        }
                    }

                } else {
                    Log.e("EntryDetailActivity", "Document does not exist")
                    findViewById<TextView>(R.id.detail_title).text = "Record Not Found"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("EntryDetailActivity", "Error fetching record", exception)
                findViewById<TextView>(R.id.detail_title).text = "Error Fetching Record"
            }

        // Add TextWatchers to detect changes
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkForChanges(saveBtn, titleEditText, contentEditText, moodEditText)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        contentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkForChanges(saveBtn, titleEditText, contentEditText, moodEditText)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        moodEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkForChanges(saveBtn, titleEditText, contentEditText, moodEditText)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        saveBtn.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val mood = moodEditText.text.toString()

            conn.collection("journal_entries").document(recordId)
                .update("title", title, "journal_entry", content, "mood", mood)
                .addOnSuccessListener {
                    Log.d("EntryDetailActivity", "Document successfully updated!")
                    isSaved = true
                }
                .addOnFailureListener { e ->
                    Log.e("EntryDetailActivity", "Error updating document", e)
                }
            saveBtn.visibility = View.GONE
        }
    }

    private fun showDiscardDialog(userId: String?) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("You have unsaved changes. Are you sure you want to discard them?")
            .setCancelable(false)
            .setPositiveButton("Discard") { _, _ ->
                // Navigate back to the previous activity or screen without saving
                val intent = Intent(this, display_scroll::class.java)
                intent.putExtra("user_id", userId) // Pass the user_id here
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    private fun checkForChanges(saveBtn: Button, titleEditText: EditText, contentEditText: EditText, moodEditText: EditText) {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()
        val mood = moodEditText.text.toString().trim()

        if (title != initialTitle || content != initialContent || mood != initialMood) {
            saveBtn.visibility = View.VISIBLE
        } else {
            saveBtn.visibility = View.GONE
        }
    }
}
