package com.example.smartspend

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Settings : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        val spinner: Spinner = findViewById(R.id.spinner_language)

        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.language_array,
            R.layout.spinner_item
        )


        adapter.setDropDownViewResource(R.layout.spinner_item)


        spinner.adapter = adapter
    }
}