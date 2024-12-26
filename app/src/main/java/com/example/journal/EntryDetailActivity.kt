package com.example.journal

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EntryDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_entry_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val title = intent.getStringExtra("title") ?: "Untitled"
        val content = intent.getStringExtra("journal_entry") ?: "No Content"
        val date = intent.getStringExtra("date") ?: "No Date Provided"

        // Bind data to the views
        findViewById<TextView>(R.id.detail_title).text = title
        findViewById<TextView>(R.id.detail_content).text = content
        findViewById<TextView>(R.id.detail_date).text = date
    }
}