package com.example.smartspend

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class EditCategory : AppCompatActivity() {

    private var categoryID: Int = -1
    private lateinit var etCategoryName: EditText
    private lateinit var etMaxBudget: EditText
    private lateinit var colorPickerButton: Button
    private var selectedColorHex: String = "#FFFFFF"  // Default color
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_category)

        // Retrieve passed data
        categoryID = intent.getIntExtra("categoryID", -1)
        val categoryName = intent.getStringExtra("categoryName")
        val colorCode = intent.getStringExtra("colorCode") ?: "#FFFFFF"
        val maxBudget = intent.getDoubleExtra("maxBudget", 0.0)

        etCategoryName = findViewById(R.id.edit_text_name)
        etMaxBudget = findViewById(R.id.edit_text_amount)
        colorPickerButton = findViewById(R.id.color_picker_button)

        // Set initial data
        etCategoryName.setText(categoryName)
        etMaxBudget.setText(maxBudget.toString())
        selectedColorHex = colorCode
        colorPickerButton.setBackgroundColor(Color.parseColor(colorCode))

        // Set up color picker
        colorPickerButton.setOnClickListener {
            ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .initialColor(Color.parseColor(selectedColorHex))
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(12)
                .setPositiveButton("OK") { _, selectedColor, _ ->
                    selectedColorHex = String.format("#%06X", 0xFFFFFF and selectedColor)
                    colorPickerButton.setBackgroundColor(selectedColor) // Update button color
                }
                .setNegativeButton("Cancel", null)
                .build()
                .show()
        }

        // Set up save button to update category
        findViewById<Button>(R.id.save_button).setOnClickListener {
            val updatedName = etCategoryName.text.toString().trim()
            val updatedBudget = etMaxBudget.text.toString().toDoubleOrNull()

            if (updatedName.isEmpty() || updatedBudget == null) {
                Toast.makeText(this, "Please enter valid details", Toast.LENGTH_SHORT).show()
            } else {
                updateCategory(categoryID, updatedName, selectedColorHex, updatedBudget)
            }
        }
    }

    // Method to update category details
    private fun updateCategory(categoryID: Int, categoryName: String, colorCode: String, maxBudget: Double) {
        val url = "https://smartspendapi.azurewebsites.net/api/Category/updateDetails/$categoryID"
        val json = JSONObject().apply {
            put("categoryName", categoryName)
            put("colorCode", colorCode)
            put("maxBudget", maxBudget)
        }
        val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())
        val request = Request.Builder()
            .url(url)
            .put(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EditCategory, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@EditCategory, "Category updated successfully", Toast.LENGTH_SHORT).show()
                        finish() // Close activity after update
                    } else {
                        Toast.makeText(this@EditCategory, "Update failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
