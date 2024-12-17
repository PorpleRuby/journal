 package com.example.journal

 import android.graphics.Color
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
 import com.google.firebase.firestore.FieldValue
 import com.google.firebase.firestore.Query
 import java.time.LocalDateTime
 import java.time.format.DateTimeFormatter
 import java.time.format.DateTimeParseException

class display_scroll : AppCompatActivity() {

    private val conn = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_display_scroll)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val layout: LinearLayout = findViewById(R.id.linearLayout)
        var search: SearchView = findViewById(R.id.search)

        // Fetch data from Firestore
        conn.collection("journal_entries")
            .get()
            .addOnSuccessListener { records ->
                for (record in records) {
                    val template = LayoutInflater.from(this).inflate(R.layout.activity_entries_display, layout, false)

                    // Bind views
                    val lblContent: TextView = template.findViewById(R.id.display_entry)
                    val lblDate: TextView = template.findViewById(R.id.date_display)
                    val imgMood: ImageView = template.findViewById(R.id.mood)
                    val imgDelete: ImageView = template.findViewById(R.id.delete_icon)
                    val imgEdit: ImageView = template.findViewById(R.id.edit_icon)

                    // Set default values
                    imgDelete.setImageResource(R.drawable.trash)
                    imgEdit.setImageResource(R.drawable.pencil)
                    imgMood.setImageResource(R.drawable.emoji_love)
                    lblContent.text = record.getString("journal_entry") ?: "No Entry"

                    val recordId = record.id


                    // Handle delete button click
                    imgDelete.setOnClickListener {
                        AlertDialog.Builder(this)
                            .setTitle("Delete Notification")
                            .setMessage("Are you sure you want to delete this post?")
                            .setPositiveButton("Yes") { _, _ ->
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

                search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        return false
                    }

                    override fun onQueryTextChange(searchinput: String?): Boolean {
                        //search input by user in the field view
                        val searchInput = searchinput ?: ""
                        //filtered records from table
                        //if post has the fullnam searched
                        val filteredPost = records.filter {
                            it.getString("journal_entry")?.contains(searchInput, true) == true ||
                                    it.getString("post")?.contains(searchInput, true) == true
                        }

                        layout.removeAllViews()
                        //copy the filtered list
                        for (record in filteredPost) {
                            val template = LayoutInflater.from(this@display_scroll).inflate(R.layout.activity_entries_display, layout, false)

                            // Bind views
                            val lblContent: TextView = template.findViewById(R.id.display_entry)
                            val lblDate: TextView = template.findViewById(R.id.date_display)
                            val imgMood: ImageView = template.findViewById(R.id.mood)
                            val imgDelete: ImageView = template.findViewById(R.id.delete_icon)
                            val imgEdit: ImageView = template.findViewById(R.id.edit_icon)

                            // Set default values
                            imgDelete.setImageResource(R.drawable.trash)
                            imgEdit.setImageResource(R.drawable.pencil)
                            imgMood.setImageResource(R.drawable.emoji_love)
                            lblContent.text = record.getString("journal_entry") ?: "No Entry"

                            val recordId = record.id


                            // Handle delete button click
                            imgDelete.setOnClickListener {
                                AlertDialog.Builder(this@display_scroll)
                                    .setTitle("Delete Notification")
                                    .setMessage("Are you sure you want to delete this post?")
                                    .setPositiveButton("Yes") { _, _ ->
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

                        return true
                    }

                })


            }
            .addOnFailureListener {
                Log.e("DEBUG", "Failed to fetch records: ${it.message}")
            }
    }
}