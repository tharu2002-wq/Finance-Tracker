package com.example.trackly.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.example.trackly.R
import com.example.trackly.data.PreferencesManager
import com.example.trackly.data.TransactionRepository
import com.example.trackly.databinding.ActivityTransactionsBinding
import com.example.trackly.model.Transaction
import com.example.trackly.ui.adapters.TransactionsAdapter
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransactionsActivity : AppCompatActivity(), TransactionsAdapter.OnTransactionClickListener {
    private lateinit var binding: ActivityTransactionsBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var adapter: TransactionsAdapter

    private val calendar = Calendar.getInstance()
    private var currentFilter = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransactionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.transactions)

        transactionRepository = TransactionRepository(this)
        preferencesManager = PreferencesManager(this)

        setupTabLayout()
        setupBottomNavigation()
        setupMonthSelector()
        setupFilterSpinner()
        setupTransactionsList()

        binding.fabAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                overridePendingTransition(0, 0)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupTabLayout() {
        // Select the Transactions tab (index 1)
        binding.tabLayout.getTabAt(1)?.select()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        startActivity(Intent(this@TransactionsActivity, MainActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    1 -> {
                        // Already in TransactionsActivity
                    }
                    2 -> {
                        startActivity(Intent(this@TransactionsActivity, BudgetActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_transactions
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_transactions -> true
                R.id.nav_budget -> {
                    startActivity(Intent(this, BudgetActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMonthSelector() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = dateFormat.format(calendar.time)

        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateTransactionsList()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateTransactionsList()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }
    }

    private fun setupFilterSpinner() {
        val filters = arrayOf("All", "Expenses", "Income")
        val spinnerAdapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, filters
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFilter.adapter = spinnerAdapter

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFilter = filters[position].lowercase()
                updateTransactionsList()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupTransactionsList() {
        adapter = TransactionsAdapter(getFilteredTransactions(), preferencesManager.getCurrency(), this)
        binding.recyclerTransactions.adapter = adapter
    }

    private fun getFilteredTransactions(): List<Transaction> {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        return when (currentFilter) {
            "expenses" -> transactionRepository.getExpensesForMonth(month, year)
            "income" -> transactionRepository.getIncomeForMonth(month, year)
            else -> transactionRepository.getTransactionsForMonth(month, year)
        }.sortedByDescending { it.date }
    }

    private fun updateTransactionsList() {
        adapter.updateTransactions(getFilteredTransactions())
    }

    override fun onTransactionClick(transaction: Transaction) {
        val intent = Intent(this, EditTransactionActivity::class.java).apply {
            putExtra("transaction_id", transaction.id)
            putExtra("transaction_title", transaction.title)
            putExtra("transaction_amount", transaction.amount)
            putExtra("transaction_category", transaction.category)
            putExtra("transaction_date", transaction.date.time)
            putExtra("transaction_is_expense", transaction.isExpense)
        }
        startActivity(intent)
    }

    override fun onTransactionLongClick(transaction: Transaction) {
        val intent = Intent(this, DeleteTransactionActivity::class.java).apply {
            putExtra("transaction_id", transaction.id)
            putExtra("transaction_title", transaction.title)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateTransactionsList()
    }
}