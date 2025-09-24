package com.example.trackly.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.trackly.R
import com.example.trackly.data.PreferencesManager
import com.example.trackly.data.TransactionRepository
import com.example.trackly.databinding.ActivityMainBinding
import com.example.trackly.notification.NotificationManager
import com.example.trackly.ui.adapters.RecentTransactionsAdapter
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var adapter: RecentTransactionsAdapter

    private val calendar = Calendar.getInstance()

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        transactionRepository = TransactionRepository(this)
        preferencesManager = PreferencesManager(this)
        notificationManager = NotificationManager(this)

        // Request notification permission
        requestNotificationPermission()

        notificationManager.scheduleDailyReminder()

        setupBottomNavigation()
        setupMonthDisplay()
        setupSummaryCards()
        setupCategoryChart()
        setupRecentTransactions()
        setupTabLayout()

        binding.btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, AddTransactionActivity::class.java))
        }

        binding.btnPreviousMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            updateDashboard()
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateDashboard()
        }
    }
    private fun setupTabLayout() {
        // Select the Dashboard tab (index 0)
        binding.tabLayout.getTabAt(0)?.select()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        // Already in MainActivity
                    }
                    1 -> {
                        startActivity(Intent(this@MainActivity, TransactionsActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    2 -> {
                        startActivity(Intent(this@MainActivity, BudgetActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can show notifications
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "Notification permission denied. You won't receive budget alerts.", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Rest of your methods...
    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_dashboard
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
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

    private fun setupMonthDisplay() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
    }

    private fun setupSummaryCards() {
        val currency = preferencesManager.getCurrency()
        val totalIncome = transactionRepository.getTotalIncomeForMonth(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        )
        val totalExpenses = transactionRepository.getTotalExpensesForMonth(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        )
        val balance = totalIncome - totalExpenses

        binding.tvIncomeAmount.text = String.format("%s %.2f", currency, totalIncome)
        binding.tvExpenseAmount.text = String.format("%s %.2f", currency, totalExpenses)
        binding.tvBalanceAmount.text = String.format("%s %.2f", currency, balance)

        // Budget progress
        val budget = preferencesManager.getBudget()
        if (budget.month == calendar.get(Calendar.MONTH) &&
            budget.year == calendar.get(Calendar.YEAR) &&
            budget.amount > 0) {

            val percentage = (totalExpenses / budget.amount) * 100
            binding.progressBudget.progress = percentage.toInt().coerceAtMost(100)
            binding.tvBudgetStatus.text = String.format(
                "%.1f%% of %s %.2f", percentage, currency, budget.amount
            )

            if (percentage >= 100) {
                binding.tvBudgetStatus.setTextColor(Color.RED)
            } else if (percentage >= 80) {
                binding.tvBudgetStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
            } else {
                binding.tvBudgetStatus.setTextColor(Color.GREEN)
            }
        } else {
            binding.progressBudget.progress = 0
            binding.tvBudgetStatus.text = getString(R.string.no_budget_set)
            binding.tvBudgetStatus.setTextColor(Color.GRAY)
        }
    }

    private fun setupCategoryChart() {
        val expensesByCategory = transactionRepository.getExpensesByCategory(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        )

        if (expensesByCategory.isEmpty()) {
            binding.pieChart.setNoDataText(getString(R.string.no_expenses_this_month))
            binding.pieChart.invalidate()
            return
        }

        val entries = ArrayList<PieEntry>()
        val colors = ArrayList<Int>()

        expensesByCategory.forEach { (category, amount) ->
            entries.add(PieEntry(amount.toFloat(), category))
            colors.add(ColorTemplate.MATERIAL_COLORS[entries.size % ColorTemplate.MATERIAL_COLORS.size])
        }

        val dataSet = PieDataSet(entries, "Categories")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val pieData = PieData(dataSet)
        binding.pieChart.data = pieData
        binding.pieChart.description.isEnabled = false
        binding.pieChart.centerText = getString(R.string.expenses_by_category)
        binding.pieChart.setCenterTextSize(14f)
        binding.pieChart.legend.textSize = 12f
        binding.pieChart.animateY(1000)
        binding.pieChart.invalidate()
    }

    private fun setupRecentTransactions() {
        val transactions = transactionRepository.getTransactionsForMonth(
            calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR)
        ).sortedByDescending { it.date }.take(5)

        adapter = RecentTransactionsAdapter(transactions, preferencesManager.getCurrency())
        binding.recyclerRecentTransactions.adapter = adapter

        binding.tvViewAllTransactions.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
            overridePendingTransition(0, 0)
            finish()
        }
    }

    private fun updateDashboard() {
        setupMonthDisplay()
        setupSummaryCards()
        setupCategoryChart()
        setupRecentTransactions()
    }

    override fun onResume() {
        super.onResume()

        // Debug logging
        val budget = preferencesManager.getBudget()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        if (budget.month == currentMonth && budget.year == currentYear && budget.amount > 0) {
            val totalExpenses = transactionRepository.getTotalExpensesForMonth(currentMonth, currentYear)
            val budgetPercentage = (totalExpenses / budget.amount) * 100

            Log.d("MainActivity", "Budget: $totalExpenses / ${budget.amount} = $budgetPercentage%")
            Log.d("MainActivity", "Notifications enabled: ${preferencesManager.isNotificationEnabled()}")
        }

        updateDashboard()
        // Check budget and notify
        notificationManager.checkBudgetAndNotify()
    }
}