package com.example.journal

import android.Manifest
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Intent
import android.content.pm.PackageManager
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.widget.Toast
import androidx.core.app.ActivityCompat

class entry_form : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    val conn = FirebaseFirestore.getInstance()

    // Request code for permissions
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_form)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val entry: EditText = findViewById(R.id.content_field)
        val submitEntry: Button = findViewById(R.id.submit_entry)

        submitEntry.setOnClickListener {
            val diaryEntry = entry.text.toString()

            // Get the current location
            getCurrentLocation { location ->
                if (location != null) {
                    // Use the location and other data to store in Firestore
                    val newEntry = hashMapOf(
                        "journal_entry" to diaryEntry,
                        "created_at" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        "location" to "Lat: ${location.latitude}, Long: ${location.longitude}"  // Store location
                    )

                    conn.collection("journal_entries").add(newEntry)

                    val intent = Intent(this, display_scroll::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getCurrentLocation(callback: (android.location.Location?) -> Unit) {
        // Check if permissions are granted (you should handle runtime permissions)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request permissions if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                callback(location)  // Return the location
            }
            .addOnFailureListener {
                callback(null)  // Return null if failed
            }
    }

    // Handle the result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can now access the location
                getCurrentLocation { location ->
                    // You can use the location now
                }
            } else {
                // Permission denied, show a message
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
