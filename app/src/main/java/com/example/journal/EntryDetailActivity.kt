package com.example.journal

import android.os.Bundle
import android.util.Log
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

        // Back button functionality (if needed)
        val backBtn: ImageView = findViewById(R.id.back_detail_btn)

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
                    findViewById<TextView>(R.id.detail_title).text = document.getString("title") ?: "Untitled"
                    findViewById<TextView>(R.id.detail_content).text = document.getString("journal_entry") ?: "No Content"

                    val createdAt = document.getString("created_at")
                    val lblDate: TextView = findViewById(R.id.detail_date) // Fix for TextView reference

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
    }
}
