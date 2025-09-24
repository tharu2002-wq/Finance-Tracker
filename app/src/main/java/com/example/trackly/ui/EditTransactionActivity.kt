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

class EditTransactionActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddTransactionBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var notificationManager: NotificationManager
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private lateinit var transaction: Transaction

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Get transaction data from intent (use correct keys)
        val id = intent.getStringExtra("transaction_id") ?: ""
        val title = intent.getStringExtra("transaction_title") ?: ""
        val amount = intent.getDoubleExtra("transaction_amount", 0.0)
        val category = intent.getStringExtra("transaction_category") ?: ""
        val date = java.util.Date(intent.getLongExtra("transaction_date", System.currentTimeMillis()))
        val isExpense = intent.getBooleanExtra("transaction_is_expense", true)

        // Log the received data for debugging
        android.util.Log.d("EditTransactionActivity", "Received: id=$id, title=$title, amount=$amount, category=$category, date=$date, isExpense=$isExpense")

        transaction = Transaction(
            id = id,
            title = title,
            amount = amount,
            category = category,
            date = date,
            isExpense = isExpense
        )

        transactionRepository = TransactionRepository(this)
        notificationManager = NotificationManager(this)

        // Set the calendar to the transaction date
        calendar.time = transaction.date

        setupCategorySpinner()
        setupDatePicker()
        setupButtons()
        populateFields()
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

    private fun populateFields() {
        binding.dialogTitle.text = getString(R.string.edit_transaction)
        binding.etTitle.setText(transaction.title)
        binding.etAmount.setText(transaction.amount.toString())

        // Set category based on the view type
        if (binding.spinnerCategory is android.widget.AutoCompleteTextView) {
            (binding.spinnerCategory as android.widget.AutoCompleteTextView).setText(transaction.category, false)
            android.util.Log.d("EditTransactionActivity", "Set AutoCompleteTextView category: ${transaction.category}")
        } else if (binding.spinnerCategory is android.widget.Spinner) {
            val spinner = binding.spinnerCategory as android.widget.Spinner
            val categoryPosition = Category.DEFAULT_CATEGORIES.indexOf(transaction.category)
            if (categoryPosition >= 0) {
                spinner.setSelection(categoryPosition)
                android.util.Log.d("EditTransactionActivity", "Set Spinner category: ${transaction.category}, position: $categoryPosition")
            } else {
                android.util.Log.w("EditTransactionActivity", "Category not found in DEFAULT_CATEGORIES: ${transaction.category}")
            }
        }

        if (transaction.isExpense) {
            binding.radioExpense.isChecked = true
            binding.radioIncome.isChecked = false
        } else {
            binding.radioIncome.isChecked = true
            binding.radioExpense.isChecked = false
        }
        android.util.Log.d("EditTransactionActivity", "Set radio: isExpense=${transaction.isExpense}")

        // Set date
        binding.etDate.setText(dateFormatter.format(transaction.date))
        android.util.Log.d("EditTransactionActivity", "Set date: ${transaction.date}")
    }

    private fun setupButtons() {
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                updateTransaction()
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

    private fun updateTransaction() {
        try {
            transaction.title = binding.etTitle.text.toString().trim()
            transaction.amount = binding.etAmount.text.toString().toDouble()

            // Get category based on the view type
            if (binding.spinnerCategory is android.widget.AutoCompleteTextView) {
                transaction.category = binding.spinnerCategory.text.toString()
            } else if (binding.spinnerCategory is android.widget.Spinner) {
                val spinner = binding.spinnerCategory as android.widget.Spinner
                if (spinner.selectedItemPosition >= 0) {
                    transaction.category = spinner.selectedItem.toString()
                }
            }

            transaction.date = calendar.time
            transaction.isExpense = binding.radioExpense.isChecked

            transactionRepository.saveTransaction(transaction)
            notificationManager.checkBudgetAndNotify()

            Toast.makeText(
                this,
                getString(R.string.transaction_updated),
                Toast.LENGTH_SHORT
            ).show()

            finish()
        } catch (e: Exception) {
            android.util.Log.e("EditTransactionActivity", "Error updating transaction: ${e.message}", e)
            Toast.makeText(
                this,
                getString(R.string.error_updating_transaction),
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