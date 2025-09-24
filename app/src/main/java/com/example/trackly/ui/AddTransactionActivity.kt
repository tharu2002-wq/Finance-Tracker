package com.example.trackly.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.trackly.R
import com.example.trackly.data.TransactionRepository
import com.example.trackly.databinding.ActivityAddTransactionBinding
import com.example.trackly.model.Category
import com.example.trackly.model.Transaction
import com.example.trackly.notification.NotificationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationManager: NotificationManager
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        transactionRepository = TransactionRepository(this)
        notificationManager = NotificationManager(this)

        setupCategorySpinner()
        setupDatePicker()
        setupButtons()
    }

    private fun setupCategorySpinner() {
        val categories = Category.DEFAULT_CATEGORIES.toTypedArray()
        val adapter = ArrayAdapter(
            this, android.R.layout.simple_dropdown_item_1line, categories
        )

        // For AutoCompleteTextView in Material Design
        (binding.spinnerCategory as? android.widget.AutoCompleteTextView)?.setAdapter(adapter)

        // For traditional Spinner
        (binding.spinnerCategory as? android.widget.Spinner)?.adapter = adapter
    }

    private fun setupDatePicker() {
        binding.etDate.setText(dateFormatter.format(calendar.time))

        binding.etDate.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    binding.etDate.setText(dateFormatter.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                saveTransaction()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (binding.etTitle.text.toString().trim().isEmpty()) {
            binding.etTitle.error = getString(R.string.field_required)
            isValid = false
        }

        if (binding.etAmount.text.toString().trim().isEmpty()) {
            binding.etAmount.error = getString(R.string.field_required)
            isValid = false
        }

        // Get category based on the view type
        var category: String? = null

        // For AutoCompleteTextView
        if (binding.spinnerCategory is android.widget.AutoCompleteTextView) {
            category = binding.spinnerCategory.text.toString()
        }
        // For traditional Spinner
        else if (binding.spinnerCategory is android.widget.Spinner) {
            val spinner = binding.spinnerCategory as android.widget.Spinner
            if (spinner.selectedItemPosition >= 0) {
                category = spinner.selectedItem.toString()
            }
        }

        if (category.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.field_required), Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun saveTransaction() {
        try {
            val title = binding.etTitle.text.toString().trim()
            val amount = binding.etAmount.text.toString().toDouble()

            // Get category based on the view type
            var category = "Other" // Default value

            // For AutoCompleteTextView
            if (binding.spinnerCategory is android.widget.AutoCompleteTextView) {
                category = binding.spinnerCategory.text.toString()
            }
            // For traditional Spinner
            else if (binding.spinnerCategory is android.widget.Spinner) {
                val spinner = binding.spinnerCategory as android.widget.Spinner
                if (spinner.selectedItemPosition >= 0) {
                    category = spinner.selectedItem.toString()
                }
            }

            val date = calendar.time
            val isExpense = binding.radioExpense.isChecked

            val transaction = Transaction(
                title = title,
                amount = amount,
                category = category,
                date = date,
                isExpense = isExpense
            )

            transactionRepository.saveTransaction(transaction)
            notificationManager.checkBudgetAndNotify()

            Toast.makeText(
                this,
                getString(R.string.transaction_added),
                Toast.LENGTH_SHORT
            ).show()

            finish()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                getString(R.string.error_adding_transaction),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}