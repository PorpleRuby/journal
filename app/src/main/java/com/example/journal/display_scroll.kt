package com.example.journal


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import android.content.Intent
import androidx.cardview.widget.CardView

class display_scroll : AppCompatActivity() {

    private val conn = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_display_scroll)

        // Setup for insets handling
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val layout: LinearLayout = findViewById(R.id.linearLayout)
        val search: SearchView = findViewById(R.id.search)

        // Fetch data from Firestore
        conn.collection("journal_entries")
            .get()
            .addOnSuccessListener { records ->
                for (record in records) {
                    val template = LayoutInflater.from(this).inflate(R.layout.activity_entries_display, layout, false)

                    // Bind views
                    val lblContent: TextView = template.findViewById(R.id.display_entry)
                    val lblDate: TextView = template.findViewById(R.id.date_display)
                    val imgDelete: ImageView = template.findViewById(R.id.delete_icon)
                    val lblTitle: TextView = template.findViewById(R.id.entry_title)
                    val cardView: CardView = template.findViewById(R.id.card_view)

                    // Set default values for image resources
                    imgDelete.setImageResource(R.drawable.trash)

                    // Assuming your Firestore has a "title" and "journal_entry" field
                    val title = record.getString("title") ?: "Untitled"
                    val content = record.getString("journal_entry") ?: "No Content"
                    val preview = if (content.length > 50) content.substring(0, 50) + "..." else content

                    // Set Title and Preview for display
                    lblContent.text = "$preview"
                    lblTitle.text = "$title"

                    // Set the CardView OnClickListener
                    cardView.setOnClickListener {
                        // Create an Intent to navigate to EntryDetailActivity
                        val intent = Intent(this, EntryDetailActivity::class.java)

                        // Pass the necessary data (title, content, date) using intent
                        intent.putExtra("title", title)
                        intent.putExtra("journal_entry", content)
                        intent.putExtra("date", date)

                        // Start the activity
                        startActivity(intent)
                    }

                    // Handle delete button click
                    imgDelete.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Delete Notification")
                            .setMessage("Are you sure you want to delete this post?")
                            .setPositiveButton("Yes") { _, _ ->
                                val recordId = record.id
                                conn.collection("journal_entries").document(recordId).delete()
                                    .addOnSuccessListener {
                                        layout.removeView(template)
                                    }.addOnFailureListener {
                                        Log.e("DEBUG", "Failed to delete record: ${it.message}")
                                    }
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }

                    // Handle date formatting
                    val createdAt = record.getString("created_at")
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

                    // Add the inflated view to the layout
                    layout.addView(template)
                }

                // Search functionality
                search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(searchinput: String?): Boolean {
                        val searchInput = searchinput ?: ""
                        val filteredPost = records.filter {
                            val entryMatches = it.getString("journal_entry")?.contains(searchInput, true) == true
                            val titleMatches = it.getString("title")?.contains(searchInput, true) == true
                            entryMatches || titleMatches
                        }

                        layout.removeAllViews()
                        for (record in filteredPost) {
                            val template = LayoutInflater.from(this@display_scroll).inflate(R.layout.activity_entries_display, layout, false)

                            // Bind views
                            val lblContent: TextView = template.findViewById(R.id.display_entry)
                            val lblDate: TextView = template.findViewById(R.id.date_display)
                            val lblTitle: TextView = template.findViewById(R.id.entry_title)

                            // Set preview text for entry display
                            val title = record.getString("title") ?: "Untitled"
                            val content = record.getString("journal_entry") ?: "No Content"
                            val preview = if (content.length > 50) content.substring(0, 50) + "..." else content
                            lblContent.text = "$preview"
                            lblTitle.text = "$title"

                            // Handle date formatting
                            val createdAt = record.getString("created_at")
                            if (!createdAt.isNullOrEmpty()) {
                                try {
                                    val createdDate = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    lblDate.text = createdDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a"))
                                } catch (e: DateTimeParseException) {
                                    lblDate.text = "Invalid Date"
                                    Log.e("DEBUG", "Date parsing error: ${e.message}")
                                }
                            } else {
                                lblDate.text = "No Date Provided"
                            }

                            template.setOnClickListener {
                                val intent = Intent(this@display_scroll, EntryDetailActivity::class.java)
                                intent.putExtra("title", title)
                                intent.putExtra("journal_entry", content)
                                intent.putExtra("date", lblDate.text.toString())
                                startActivity(intent)
                            }

                            // Add the inflated view to the layout
                            layout.addView(template)
                        }

                        return true
                    }
                })
            }
            .addOnFailureListener {
                Log.e("DEBUG", "Failed to fetch records: ${it.message}")
            }
    }
}
