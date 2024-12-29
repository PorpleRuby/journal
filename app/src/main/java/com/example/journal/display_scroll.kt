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
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import android.widget.Toast
import android.view.View

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
        val noEntriesText: TextView = findViewById(R.id.no_entries) // Reference to "No Entries Yet" text
        val search: SearchView = findViewById(R.id.search)
        val homeBtn: ImageView = findViewById(R.id.home_button_scroll)
        val addEntry: ImageView = findViewById(R.id.add_entry_scroll)
        val profileBtn: ImageView = findViewById(R.id.profileButtonScroll)
        val scrollView: ScrollView = findViewById(R.id.scrollView2)

        // this will scroll to the top when the home button is clicked
        homeBtn.setOnClickListener {
            scrollView.smoothScrollTo(0, 0)
        }

        // add entry
        addEntry.setOnClickListener {
            val intent = Intent(this, entry_form::class.java)
            startActivity(intent)
        }

        // redirect to profile page
        profileBtn.setOnClickListener {
            val intent = Intent(this, ProfilePage::class.java)
            startActivity(intent)
        }

        val userId = intent.getStringExtra("user_id")
        if (userId == null) {
            showErrorAndRedirect()
            return
        }

        // Fetch data from Firestore
        conn.collection("journal_entries")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { records ->
                val entries = records.filter { record ->
                    val createdAt = record.getString("created_at")
                    try {
                        LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        true
                    } catch (e: DateTimeParseException) {
                        false
                    }
                }.map { record ->
                    val createdAt = record.getString("created_at")
                    val createdDate = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    Triple(record as QueryDocumentSnapshot, createdDate, createdAt ?: "")
                }

                val sortedEntries = entries.sortedByDescending { it.second }
                layout.removeAllViews()

                // Display message if no entries are found
                if (sortedEntries.isEmpty()) {
                    noEntriesText.visibility = View.VISIBLE
                } else {
                    noEntriesText.visibility = View.GONE
                }

                // Add entries to layout
                for ((record, createdDate, _) in sortedEntries) {
                    val template = LayoutInflater.from(this).inflate(R.layout.activity_entries_display, layout, false)
                    bindEntryView(template, record, createdDate)

                    // Set OnClickListener for CardView
                    val cardView: CardView = template.findViewById(R.id.card_view)
                    cardView.setOnClickListener {
                        val recordId = record.id
                        val intent = Intent(this, EntryDetailActivity::class.java)
                        intent.putExtra("recordId", recordId)
                        intent.putExtra("mood", record.getString("mood")) // Pass mood
                        startActivity(intent)
                    }

                    // Handle delete button
                    val imgDelete: ImageView = template.findViewById(R.id.delete_icon)
                    imgDelete.setImageResource(R.drawable.trash)
                    imgDelete.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Delete Notification")
                            .setMessage("Are you sure you want to delete this post?")
                            .setPositiveButton("Yes") { _, _ ->
                                conn.collection("journal_entries").document(record.id).delete()
                                    .addOnSuccessListener {
                                        layout.removeView(template)
                                        Toast.makeText(this, "Entry deleted successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Log.e("DEBUG", "Failed to delete record: ${it.message}")
                                        Toast.makeText(this, "Failed to delete entry", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .setNegativeButton("No", null)
                            .show()
                    }

                    layout.addView(template)
                }

                // Search button
                search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(searchInput: String?): Boolean {
                        performSearch(searchInput, sortedEntries)
                        return true
                    }
                })
            }
            .addOnFailureListener {
                Log.e("DEBUG", "Failed to fetch records: ${it.message}")
            }
    }

    private fun showErrorAndRedirect() {
        Toast.makeText(this, "User ID not found. Redirecting to login...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginPage::class.java)
        startActivity(intent)
    }

    private fun bindEntryView(template: View, record: QueryDocumentSnapshot, createdDate: LocalDateTime) {
        val lblContent: TextView = template.findViewById(R.id.display_entry)
        val lblDate: TextView = template.findViewById(R.id.date_display)
        val lblTitle: TextView = template.findViewById(R.id.entry_title)
        val lblMood: TextView = template.findViewById(R.id.mood_display)

        val title = record.getString("title") ?: "Untitled"
        val content = record.getString("journal_entry") ?: "No Content"
        val mood = record.getString("mood") ?: "Mood not specified"
        val preview = if (content.length > 50) content.substring(0, 50) + "..." else content

        lblContent.text = preview
        lblTitle.text = title
        lblMood.text = "Mood: $mood"
        lblDate.text = createdDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
    }

    private fun performSearch(searchInput: String?, sortedEntries: List<Triple<QueryDocumentSnapshot, LocalDateTime, String>>) {
        val filteredPosts = sortedEntries.filter {
            val entryMatches = it.first.getString("journal_entry")?.contains(searchInput ?: "", true) == true
            val titleMatches = it.first.getString("title")?.contains(searchInput ?: "", true) == true
            val moodMatches = it.first.getString("mood")?.contains(searchInput ?: "", true) == true
            entryMatches || titleMatches || moodMatches
        }

        val layout: LinearLayout = findViewById(R.id.linearLayout)
        layout.removeAllViews()
        for ((record, createdDate, _) in filteredPosts) {
            val template = LayoutInflater.from(this).inflate(R.layout.activity_entries_display, layout, false)
            bindEntryView(template, record, createdDate)
            layout.addView(template)
        }
    }
}
