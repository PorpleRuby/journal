package com.example.journal

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.*
import java.util.*

class ProfilePage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_profile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var lblFname:TextView = findViewById(R.id.displayFname)
        var lblEmail:TextView = findViewById(R.id.displayEmail)
        var btnEdit:TextView = findViewById(R.id.btnEdit)
        var btnLogout:TextView = findViewById(R.id.btnLogout)
        //getUserData();
    }

    /*void getUserData(){
        FirebaseUtil.currentUserDetals().get.addonCompleteListener(task -> {
            currentUserModel = task.getResult().toObject(UserModel.class);
            lblFname.setText(currentUserModel.getUsername());
            lblEmail.setText(current)
        });
    }*/
}