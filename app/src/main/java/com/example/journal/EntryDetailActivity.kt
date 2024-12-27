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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class EntryDetailActivity : AppCompatActivity() {
    private val conn = FirebaseFirestore.getInstance()
    private var initialTitle: String? = null
    private var initialContent: String? = null
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
        val saveBtn: Button = findViewById(R.id.save_button) // Referencing the button by ID
        val titleEditText: EditText = findViewById(R.id.detail_title)
        val contentEditText: EditText = findViewById(R.id.detail_content)

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
                    initialTitle = title // Save initial values
                    initialContent = content // Save initial values

                    titleEditText.setText(title)
                    contentEditText.setText(content)

                    val createdAt = document.getString("created_at")
                    val lblDate: TextView = findViewById(R.id.detail_date)

                    if (!createdAt.isNullOrEmpty()) {
                        try {
                            // Parse the date string and format it
                            val createdDate = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            lblDate.text = createdDate.format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
                        } catch (e: DateTimeParseException) {
                            lblDate.text = "Invalid Date"
                            Log.e("DEBUG", "Date parsing error: ${e.message}")
                        }
                    } else {
                        lblDate.text = "No Date Provided"
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

        // Add TextWatcher to detect changes in title EditText
        titleEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                checkForChanges(saveBtn, titleEditText, contentEditText)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        // Add TextWatcher to detect changes in content EditText
        contentEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {
                checkForChanges(saveBtn, titleEditText, contentEditText)
            }
            override fun afterTextChanged(editable: Editable?) {}
        })

        // Save button functionality
        saveBtn.setOnClickListener {
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()

            // Save the changes to Firestore
            conn.collection("journal_entries").document(recordId)
                .update("title", title, "journal_entry", content)
                .addOnSuccessListener {
                    Log.d("EntryDetailActivity", "Document successfully updated!")
                    isSaved = true // Mark as saved
                }
                .addOnFailureListener { e ->
                    Log.e("EntryDetailActivity", "Error updating document", e)
                }

            // Hide the save button after saving
            saveBtn.visibility = View.GONE
        }

        // Function to show a discard confirmation dialog
        fun showDiscardDialog() {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("You have unsaved changes. Are you sure you want to discard them?")
                .setCancelable(false)
                .setPositiveButton("Discard") { _, _ ->
                    // Navigate back to the previous activity or screen without saving
                    val intent = Intent(this, display_scroll::class.java)
                    startActivity(intent)
                }
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            builder.create().show()
        }

        // Back button functionality
        backBtn.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val entry = contentEditText.text.toString().trim()

            // Check if there are unsaved changes
            if (isSaved || (title == initialTitle && entry == initialContent)) {
                // If there are no unsaved changes, navigate back without confirmation
                val intent = Intent(this, display_scroll::class.java)
                startActivity(intent)
            } else {
                // If there are unsaved changes, show the discard confirmation dialog
                showDiscardDialog()
            }
        }
    }

    // Function to check if there are changes and show the save button
    private fun checkForChanges(saveBtn: Button, titleEditText: EditText, contentEditText: EditText) {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()

        // If either title or content is different from initial values, show the save button
        if (title != initialTitle || content != initialContent) {
            saveBtn.visibility = View.VISIBLE
        } else {
            saveBtn.visibility = View.GONE
        }
    }
}
