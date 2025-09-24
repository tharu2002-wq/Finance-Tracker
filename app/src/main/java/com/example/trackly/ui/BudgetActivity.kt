package com.example.trackly.ui


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.trackly.R
import com.example.trackly.data.PreferencesManager
import com.example.trackly.data.TransactionRepository
import com.example.trackly.databinding.ActivityBudgetBinding
import com.example.trackly.model.Budget
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BudgetActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBudgetBinding
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var preferencesManager: PreferencesManager

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBudgetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.budget)

        transactionRepository = TransactionRepository(this)
        preferencesManager = PreferencesManager(this)

        setupBottomNavigation()
        setupMonthSelector()
        setupBudgetDisplay()
        setupBudgetChart()
        setupTabLayout()

        binding.btnSetBudget.setOnClickListener {
            showSetBudgetDialog()
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
        // Select the Budget tab (index 2)
        binding.tabLayout.getTabAt(2)?.select()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        startActivity(Intent(this@BudgetActivity, MainActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    1 -> {
                        startActivity(Intent(this@BudgetActivity, TransactionsActivity::class.java))
                        overridePendingTransition(0, 0)
                        finish()
                    }
                    2 -> {
                        // Already in BudgetActivity
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    private fun setupBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
        binding.bottomNavigation.selectedItemId = R.id.nav_budget
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_budget -> true
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
            updateBudgetDisplay()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }

        binding.btnNextMonth.setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            updateBudgetDisplay()
            binding.tvCurrentMonth.text = dateFormat.format(calendar.time)
        }
    }

    private fun setupBudgetDisplay() {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val budget = preferencesManager.getBudget()
        val currency = preferencesManager.getCurrency()

        if (budget.month == month && budget.year == year && budget.amount > 0) {
            binding.tvNoBudget.visibility = View.GONE
            binding.cardBudgetInfo.visibility = View.VISIBLE

            val totalExpenses = transactionRepository.getTotalExpensesForMonth(month, year)
            val remaining = budget.amount - totalExpenses
            val percentage = (totalExpenses / budget.amount) * 100

            binding.tvBudgetAmount.text = String.format("%s %.2f", currency, budget.amount)
            binding.tvExpensesAmount.text = String.format("%s %.2f", currency, totalExpenses)
            binding.tvRemainingAmount.text = String.format("%s %.2f", currency, remaining)

            binding.progressBudget.progress = percentage.toInt().coerceAtMost(100)
            binding.tvBudgetPercentage.text = String.format("%.1f%%", percentage)

            if (percentage >= 100) {
                binding.tvBudgetStatus.text = getString(R.string.budget_exceeded)
                binding.tvBudgetStatus.setTextColor(Color.RED)
                binding.tvRemainingAmount.setTextColor(Color.RED)
            } else if (percentage >= 80) {
                binding.tvBudgetStatus.text = getString(R.string.budget_warning)
                binding.tvBudgetStatus.setTextColor(Color.parseColor("#FFA500")) // Orange
                binding.tvRemainingAmount.setTextColor(Color.parseColor("#FFA500"))
            } else {
                binding.tvBudgetStatus.text = getString(R.string.budget_good)
                binding.tvBudgetStatus.setTextColor(Color.GREEN)
                binding.tvRemainingAmount.setTextColor(Color.GREEN)
            }
        } else {
            binding.tvNoBudget.visibility = View.VISIBLE
            binding.cardBudgetInfo.visibility = View.GONE
        }
    }

    private fun setupBudgetChart() {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val expensesByCategory = transactionRepository.getExpensesByCategory(month, year)

        if (expensesByCategory.isEmpty()) {
            binding.barChart.setNoDataText(getString(R.string.no_expenses_this_month))
            binding.barChart.invalidate()
            return
        }

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        expensesByCategory.entries.forEachIndexed { index, entry ->
            entries.add(BarEntry(index.toFloat(), entry.value.toFloat()))
            labels.add(entry.key)
        }

        val dataSet = BarDataSet(entries, "Expenses by Category")
        dataSet.colors = listOf(
            Color.parseColor("#4CAF50"),
            Color.parseColor("#2196F3"),
            Color.parseColor("#FFC107"),
            Color.parseColor("#E91E63"),
            Color.parseColor("#9C27B0"),
            Color.parseColor("#FF5722"),
            Color.parseColor("#795548"),
            Color.parseColor("#607D8B")
        )
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        binding.barChart.data = barData
        binding.barChart.description.isEnabled = false
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        binding.barChart.xAxis.granularity = 1f
        binding.barChart.xAxis.labelRotationAngle = 45f
        binding.barChart.animateY(1000)
        binding.barChart.invalidate()
    }

    private fun showSetBudgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_budget, null)
        val etBudgetAmount = dialogView.findViewById<EditText>(R.id.et_budget_amount)
        val tvMonth = dialogView.findViewById<TextView>(R.id.tv_month)

        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        tvMonth.text = dateFormat.format(calendar.time)

        val budget = preferencesManager.getBudget()
        if (budget.month == calendar.get(Calendar.MONTH) && budget.year == calendar.get(Calendar.YEAR)) {
            etBudgetAmount.setText(budget.amount.toString())
        }

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                try {
                    val amount = etBudgetAmount.text.toString().toDouble()
                    val newBudget = Budget(
                        amount = amount,
                        month = calendar.get(Calendar.MONTH),
                        year = calendar.get(Calendar.YEAR)
                    )
                    preferencesManager.setBudget(newBudget)
                    updateBudgetDisplay()

                    Toast.makeText(
                        this,
                        getString(R.string.budget_saved),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Toast.makeText(
                        this,
                        getString(R.string.error_saving_budget),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateBudgetDisplay() {
        setupBudgetDisplay()
        setupBudgetChart()
    }

    override fun onResume() {
        super.onResume()
        updateBudgetDisplay()
    }
}