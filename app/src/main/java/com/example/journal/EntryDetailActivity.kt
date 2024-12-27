package com.example.journal

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore

class EntryDetailActivity : AppCompatActivity() {
    private val conn = FirebaseFirestore.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entry_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Get the record ID from the Intent
        // Get the record ID from the Intent
        val recordId = intent.getStringExtra("recordId")
        if (recordId == null || recordId.isEmpty()) {
            Log.e("EntryDetailActivity", "Record ID is null or empty")
            return
        }

// Fetch the specific record from Firestore
        val conn = FirebaseFirestore.getInstance()
        conn.collection("journal_entries").document(recordId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    findViewById<TextView>(R.id.detail_title).text = document.getString("title") ?: "Untitled"
                    findViewById<TextView>(R.id.detail_content).text = document.getString("journal_entry") ?: "No Content"
                    findViewById<TextView>(R.id.detail_date).text = document.getString("created_at") ?: "No Date Provided"
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