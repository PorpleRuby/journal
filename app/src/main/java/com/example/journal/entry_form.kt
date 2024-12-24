package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.content.Intent


class entry_form : AppCompatActivity() {

    val conn = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_form)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val entry: EditText = findViewById(R.id.content_field)
        val submitEntry: Button = findViewById(R.id.submit_entry)

        submitEntry.setOnClickListener {
            val diaryEntry = entry.text.toString()

            // Store the diary entry without location
            val newEntry = hashMapOf(
                "journal_entry" to diaryEntry,
                "created_at" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )

            // Save to Firestore
            conn.collection("journal_entries").add(newEntry)

            // Navigate to another activity
            val intent = Intent(this, display_scroll::class.java)
            startActivity(intent)
        }
    }
}