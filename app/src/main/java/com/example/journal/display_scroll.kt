package com.example.journal

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.ImageView
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

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
        val homeBtn: ImageView = findViewById(R.id.home_button_scroll)
        val addEntry: ImageView = findViewById(R.id.add_entry_scroll)
        val profileBtn: ImageView = findViewById(R.id.profileButtonScroll)
        val scrollView: ScrollView = findViewById(R.id.scrollView2)

        // Scroll to top when Home button is clicked
        homeBtn.setOnClickListener {
            scrollView.smoothScrollTo(0, 0) // Scrolls to the top of the scrollView
        }

        // Navigate to add entry page
        addEntry.setOnClickListener {
            val intent = Intent(this, entry_form::class.java)
            startActivity(intent)
        }

        // Navigate to profile page
        profileBtn.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        // Fetch data from Firestore
        conn.collection("journal_entries")
            .get()
            .addOnSuccessListener { records ->
                // Map records with date parsing
                val entries = records.mapNotNull { record ->
                    val createdAt = record.getString("created_at")
                    val createdDate = try {
                        LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    } catch (e: DateTimeParseException) {
                        null // Skip entries with invalid dates
                    }

                    createdDate?.let {
                        Triple(record, createdDate, createdAt)
                    }
                }

                // Sort entries by date in reverse chronological order (newest first)
                val sortedEntries = entries.sortedByDescending { it.second }

                layout.removeAllViews() // Clear previous views

                for ((record, createdDate, createdAt) in sortedEntries) {
                    val template = LayoutInflater.from(this).inflate(R.layout.activity_entries_display, layout, false)

                    // Bind views
                    val lblContent: TextView = template.findViewById(R.id.display_entry)
                    val lblDate: TextView = template.findViewById(R.id.date_display)
                    val imgDelete: ImageView = template.findViewById(R.id.delete_icon)
                    val lblTitle: TextView = template.findViewById(R.id.entry_title)
                    val cardView: CardView = template.findViewById(R.id.card_view)

                    imgDelete.setImageResource(R.drawable.trash)

                    // Set Title and Preview
                    val title = record.getString("title") ?: "Untitled"
                    val content = record.getString("journal_entry") ?: "No Content"
                    val preview = if (content.length > 50) content.substring(0, 50) + "..." else content

                    lblContent.text = preview
                    lblTitle.text = title

                    // Format and display the date
                    lblDate.text = createdDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))

                    // Set OnClickListener for CardView
                    cardView.setOnClickListener {
                        val recordId = record.id
                        val intent = Intent(this, EntryDetailActivity::class.java)
                        intent.putExtra("recordId", recordId)
                        startActivity(intent)
                    }

                    // Handle delete button
                    imgDelete.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Delete Notification")
                            .setMessage("Are you sure you want to delete this post?")
                            .setPositiveButton("Yes") { _, _ ->
                                conn.collection("journal_entries").document(record.id).delete()
                                    .addOnSuccessListener {
                                        layout.removeView(template)
                                    }.addOnFailureListener {
                                        Log.e("DEBUG", "Failed to delete record: ${it.message}")
                                    }
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }

                    // Add the template to the layout
                    layout.addView(template)
                }

                // Search functionality
                search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(searchInput: String?): Boolean {
                        val filteredPosts = sortedEntries.filter {
                            val entryMatches = it.first.getString("journal_entry")?.contains(searchInput ?: "", true) == true
                            val titleMatches = it.first.getString("title")?.contains(searchInput ?: "", true) == true
                            entryMatches || titleMatches
                        }

                        layout.removeAllViews()
                        for ((record, createdDate, _) in filteredPosts) {
                            val template = LayoutInflater.from(this@display_scroll).inflate(R.layout.activity_entries_display, layout, false)

                            // Bind views
                            val lblContent: TextView = template.findViewById(R.id.display_entry)
                            val lblDate: TextView = template.findViewById(R.id.date_display)
                            val lblTitle: TextView = template.findViewById(R.id.entry_title)

                            // Set Title and Preview
                            val title = record.getString("title") ?: "Untitled"
                            val content = record.getString("journal_entry") ?: "No Content"
                            val preview = if (content.length > 50) content.substring(0, 50) + "..." else content

                            lblContent.text = preview
                            lblTitle.text = title
                            lblDate.text = createdDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a"))

                            // Add OnClickListener
                            template.setOnClickListener {
                                val intent = Intent(this@display_scroll, EntryDetailActivity::class.java)
                                intent.putExtra("title", title)
                                intent.putExtra("journal_entry", content)
                                intent.putExtra("date", lblDate.text.toString())
                                startActivity(intent)
                            }

                            // Add the template to the layout
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
