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

        // Get the user ID from the intent
        val userId = intent.getStringExtra("user_id")
        if (userId == null) {
            showErrorAndRedirect()
            return
        }

        // Fetch data from Firestore for the logged-in user
        conn.collection("journal_entries")
            .whereEqualTo("user_id", userId) // Filter entries by user_id
            .get()
            .addOnSuccessListener { records ->
                val entries = records.filter { record ->
                    val createdAt = record.getString("created_at")
                    try {
                        val createdDate = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        true
                    } catch (e: DateTimeParseException) {
                        false
                    }
                }.map { record ->
                    val createdAt = record.getString("created_at")
                    val createdDate = LocalDateTime.parse(createdAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    Triple(record as QueryDocumentSnapshot, createdDate, createdAt ?: "")
                }

                // Sort entries by date in reverse chronological order (newest first)
                val sortedEntries = entries.sortedByDescending { it.second }

                layout.removeAllViews() // Clear previous views

                for ((record, createdDate, _) in sortedEntries) {
                    val template = LayoutInflater.from(this).inflate(R.layout.activity_entries_display, layout, false)

                    bindEntryView(template, record, createdDate)

                    // Set OnClickListener for CardView
                    val cardView: CardView = template.findViewById(R.id.card_view)
                    cardView.setOnClickListener {
                        val recordId = record.id
                        val intent = Intent(this, EntryDetailActivity::class.java)
                        intent.putExtra("recordId", recordId)
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

                    // Add the template to the layout
                    layout.addView(template)
                }

                // Search functionality
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
        val intent = Intent(this, LoginPage::class.java) // Adjust to your login activity
        startActivity(intent)
    }

    private fun bindEntryView(template: View, record: QueryDocumentSnapshot, createdDate: LocalDateTime) {
        val lblContent: TextView = template.findViewById(R.id.display_entry)
        val lblDate: TextView = template.findViewById(R.id.date_display)
        val lblTitle: TextView = template.findViewById(R.id.entry_title)

        val title = record.getString("title") ?: "Untitled"
        val content = record.getString("journal_entry") ?: "No Content"
        val preview = if (content.length > 50) content.substring(0, 50) + "..." else content

        lblContent.text = preview
        lblTitle.text = title
        lblDate.text = createdDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))
    }

    private fun performSearch(searchInput: String?, sortedEntries: List<Triple<QueryDocumentSnapshot, LocalDateTime, String>>) {
        val filteredPosts = sortedEntries.filter {
            val entryMatches = it.first.getString("journal_entry")?.contains(searchInput ?: "", true) == true
            val titleMatches = it.first.getString("title")?.contains(searchInput ?: "", true) == true
            entryMatches || titleMatches
        }

        val layout: LinearLayout = findViewById(R.id.linearLayout)
        layout.removeAllViews()
        for ((record, createdDate, _) in filteredPosts) {
            val template = LayoutInflater.from(this).inflate(R.layout.activity_entries_display, layout, false)

            bindEntryView(template, record, createdDate)

            // Add OnClickListener
            template.setOnClickListener {
                val intent = Intent(this, EntryDetailActivity::class.java)
                intent.putExtra("title", record.getString("title"))
                intent.putExtra("journal_entry", record.getString("journal_entry"))
                intent.putExtra("date", createdDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a")))
                startActivity(intent)
            }

            // Add the template to the layout
            layout.addView(template)
        }
    }
}
