package tz.yx.gml.homefrag

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableLayout
import android.widget.TableRow
import androidx.fragment.app.Fragment
import tz.yx.gml.R
import tz.yx.gml.databinding.FragmentVixBinding
import java.lang.Math.*
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ln

class vixFragment : Fragment() {

    private var _binding: FragmentVixBinding? = null
    private val binding get() = _binding!!
    
    // 标记哪些输入框已被用户主动修改，避免联动更新
    private val incomeModified = BooleanArray(12) { false }
    private val expenseModified = BooleanArray(12) { false }
    private val savingsModified = BooleanArray(12) { false }
    
    // 存储TextWatcher实例以便后续移除
    private val savingsWatchers: Array<TextWatcher> = Array(12) { index ->
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 标记该字段已被修改
                if (s?.isNotEmpty() == true) {
                    savingsModified[index] = true
                }
                
                calculateFinancialVolatility()
            }
        }
    }
    
    // 存储收入TextWatcher实例
    private val incomeWatchers: Array<TextWatcher> = Array(12) { index ->
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 如果是第一个月，更新所有未被修改的月份
                if (index == 0) {
                    val value = s?.toString() ?: ""
                    for (j in 1..11) {
                        if (!incomeModified[j]) {  // 只有未被用户修改过的字段才自动填充
                            // 直接设置值而不移除监听器，通过标志位避免无限循环
                            val incomeFields = arrayOf(
                                binding.income1, binding.income2, binding.income3, binding.income4,
                                binding.income5, binding.income6, binding.income7, binding.income8,
                                binding.income9, binding.income10, binding.income11, binding.income12
                            )
                            
                            incomeFields[j].removeTextChangedListener(incomeWatchers[j]) // 临时移除监听器避免递归调用
                            incomeFields[j].setText(value)
                            incomeFields[j].setSelection(value.length) // 设置光标位置到末尾
                            incomeFields[j].addTextChangedListener(incomeWatchers[j]) // 重新添加监听器
                        }
                    }
                }
                
                // 标记该字段已被修改（如果是第一个字段）
                if (index == 0 && s?.isNotEmpty() == true) {
                    incomeModified[0] = true
                }
                
                // 自动计算储蓄值
                autoCalculateSavings(index)
                
                calculateFinancialVolatility()
            }
        }
    }
    
    // 存储支出TextWatcher实例
    private val expenseWatchers: Array<TextWatcher> = Array(12) { index ->
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 标记该字段已被修改
                if (s?.isNotEmpty() == true) {
                    expenseModified[index] = true
                }
                
                // 自动计算储蓄值
                autoCalculateSavings(index)
                
                calculateFinancialVolatility()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVixBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupInputListeners() {
        // 为每个输入框设置单独的监听器以实现联动功能
        setupIncomeListeners()
        setupExpenseListeners()
        setupSavingsListeners()
    }
    
    private fun setupIncomeListeners() {
        val incomeFields = arrayOf(
            binding.income1, binding.income2, binding.income3, binding.income4,
            binding.income5, binding.income6, binding.income7, binding.income8,
            binding.income9, binding.income10, binding.income11, binding.income12
        )
        
        // 为每个字段添加对应的监听器
        incomeFields.forEachIndexed { index, field ->
            field.addTextChangedListener(incomeWatchers[index])
        }
    }
    
    private fun setupExpenseListeners() {
        val expenseFields = arrayOf(
            binding.expense1, binding.expense2, binding.expense3, binding.expense4,
            binding.expense5, binding.expense6, binding.expense7, binding.expense8,
            binding.expense9, binding.expense10, binding.expense11, binding.expense12
        )
        
        expenseFields.forEachIndexed { index, field ->
            field.addTextChangedListener(expenseWatchers[index])
        }
    }
    
    private fun setupSavingsListeners() {
        val savingsFields = arrayOf(
            binding.savings1, binding.savings2, binding.savings3, binding.savings4,
            binding.savings5, binding.savings6, binding.savings7, binding.savings8,
            binding.savings9, binding.savings10, binding.savings11, binding.savings12
        )
        
        savingsFields.forEachIndexed { index, field ->
            field.addTextChangedListener(savingsWatchers[index])
        }
    }
    
    private fun autoCalculateSavings(index: Int) {
        try {
            val incomeText = when(index) {
                0 -> binding.income1.text.toString()
                1 -> binding.income2.text.toString()
                2 -> binding.income3.text.toString()
                3 -> binding.income4.text.toString()
                4 -> binding.income5.text.toString()
                5 -> binding.income6.text.toString()
                6 -> binding.income7.text.toString()
                7 -> binding.income8.text.toString()
                8 -> binding.income9.text.toString()
                9 -> binding.income10.text.toString()
                10 -> binding.income11.text.toString()
                11 -> binding.income12.text.toString()
                else -> ""
            }
            
            val expenseText = when(index) {
                0 -> binding.expense1.text.toString()
                1 -> binding.expense2.text.toString()
                2 -> binding.expense3.text.toString()
                3 -> binding.expense4.text.toString()
                4 -> binding.expense5.text.toString()
                5 -> binding.expense6.text.toString()
                6 -> binding.expense7.text.toString()
                7 -> binding.expense8.text.toString()
                8 -> binding.expense9.text.toString()
                9 -> binding.expense10.text.toString()
                10 -> binding.expense11.text.toString()
                11 -> binding.expense12.text.toString()
                else -> ""
            }
            
            val savingsField = when(index) {
                0 -> binding.savings1
                1 -> binding.savings2
                2 -> binding.savings3
                3 -> binding.savings4
                4 -> binding.savings5
                5 -> binding.savings6
                6 -> binding.savings7
                7 -> binding.savings8
                8 -> binding.savings9
                9 -> binding.savings10
                10 -> binding.savings11
                11 -> binding.savings12
                else -> binding.savings1
            }
            
            if (incomeText.isNotEmpty() && expenseText.isNotEmpty()) {
                val income = incomeText.toDoubleOrNull() ?: 0.0
                val expense = expenseText.toDoubleOrNull() ?: 0.0
                val calculatedSavings = income - expense  // 储蓄 = 收入 - 支出
                
                // 只有当储蓄字段未被用户修改时才自动填充
                if (!savingsModified[index]) {
                    val formattedValue = String.format("%.2f", calculatedSavings)
                    // 临时移除监听器避免递归调用
                    savingsField.removeTextChangedListener(savingsWatchers[index])
                    savingsField.setText(formattedValue)
                    savingsField.setSelection(formattedValue.length)
                    // 重新添加监听器
                    savingsField.addTextChangedListener(savingsWatchers[index])
                }
            }
        } catch (e: NumberFormatException) {
            // 输入不是有效数字时忽略
        }
    }
    
    private fun calculateFinancialVolatility() {
        try {
            // 获取用户输入的12个月的收入、支出和储蓄数据
            val incomeInputs = listOf(
                binding.income1.text.toString(),
                binding.income2.text.toString(),
                binding.income3.text.toString(),
                binding.income4.text.toString(),
                binding.income5.text.toString(),
                binding.income6.text.toString(),
                binding.income7.text.toString(),
                binding.income8.text.toString(),
                binding.income9.text.toString(),
                binding.income10.text.toString(),
                binding.income11.text.toString(),
                binding.income12.text.toString()
            )
            
            val expenseInputs = listOf(
                binding.expense1.text.toString(),
                binding.expense2.text.toString(),
                binding.expense3.text.toString(),
                binding.expense4.text.toString(),
                binding.expense5.text.toString(),
                binding.expense6.text.toString(),
                binding.expense7.text.toString(),
                binding.expense8.text.toString(),
                binding.expense9.text.toString(),
                binding.expense10.text.toString(),
                binding.expense11.text.toString(),
                binding.expense12.text.toString()
            )
            
            val savingsInputs = listOf(
                binding.savings1.text.toString(),
                binding.savings2.text.toString(),
                binding.savings3.text.toString(),
                binding.savings4.text.toString(),
                binding.savings5.text.toString(),
                binding.savings6.text.toString(),
                binding.savings7.text.toString(),
                binding.savings8.text.toString(),
                binding.savings9.text.toString(),
                binding.savings10.text.toString(),
                binding.savings11.text.toString(),
                binding.savings12.text.toString()
            )

            // 检查是否有空值
            if (incomeInputs.any { it.isEmpty() } || 
                expenseInputs.any { it.isEmpty() } || 
                savingsInputs.any { it.isEmpty() }) {
                binding.resultValue.text = "请填写所有数据"
                return
            }

            // 将输入转换为Double数组
            val monthlyIncome = incomeInputs.map { it.toDouble() }
            val monthlyExpense = expenseInputs.map { it.toDouble() }
            val monthlySavings = savingsInputs.map { it.toDouble() }

            // 计算收入波动系数 Iv = 标准差 / 平均值
            val avgIncome = monthlyIncome.average()
            val incomeStdDev = if (avgIncome > 0) {
                sqrt(monthlyIncome.sumOf { pow(it - avgIncome, 2.0) } / 12)
            } else 0.0
            val incomeVolatility = if (avgIncome > 0) incomeStdDev / avgIncome else 0.0

            // 计算支出波动系数 Cv = 标准差 / 平均值
            val avgExpense = monthlyExpense.average()
            val expenseStdDev = if (avgExpense > 0) {
                sqrt(monthlyExpense.sumOf { pow(it - avgExpense, 2.0) } / 12)
            } else 0.0
            val expenseVolatility = if (avgExpense > 0) expenseStdDev / avgExpense else 0.0

            // 计算储蓄波动系数 Sb = 1 + ln(1 + 平均储蓄 / 平均支出)
            val avgSavings = monthlySavings.average()
            val savingsCoefficientOriginal = if (avgExpense > 0) {
                1 + ln(1 + (abs(avgSavings) / avgExpense))  // 使用绝对值确保正数
            } else 1.0

            // 计算最终财务波动值
            val denominator = if (savingsCoefficientOriginal != 0.0) {
                1 + (incomeVolatility + expenseVolatility) / abs(savingsCoefficientOriginal)  // 使用绝对值
            } else Double.MAX_VALUE
            
            val financialVolatility = if (denominator != 0.0) {
                0.47 / denominator
            } else 0.0

            // 显示结果，保留4位小数
            binding.resultValue.text = String.format("%.4f", financialVolatility)

            // 计算储蓄系数 S = 2 * ((financialVolatility - 0.425) / (0.47 - 0.425)) - 1
            // 其中financialVolatility就是E（财务波动值）
            val savingsCoefficientRaw = 2 * ((financialVolatility - 0.425) / (0.47 - 0.425)) - 1
            
            // 根据用户要求：正值鼓励储蓄，负值鼓励消费，所以反转符号
            val adjustedSavingsCoefficient = -savingsCoefficientRaw
            
            // 显示储蓄系数，保留4位小数并加上全角百分号
            binding.savingsCoefficientValue.text = String.format("%.4f％", adjustedSavingsCoefficient)

            // 高亮显示表格中对应的行
            highlightTableRows(financialVolatility)
            
        } catch (e: NumberFormatException) {
            binding.resultValue.text = "请输入有效数字"
            binding.savingsCoefficientValue.text = "0.0000"
        } catch (e: Exception) {
            binding.resultValue.text = "计算错误"
            binding.savingsCoefficientValue.text = "0.0000"
        }
    }
    
    private fun highlightTableRows(financialVolatility: Double) {
        // 清除之前的所有高亮
        clearTableHighlight()
        
        // 根据财务波动率确定要高亮的行
        val table = binding.root.findViewById<TableLayout>(R.id.dataTable)
        
        // 查找对应的行
        when {
            financialVolatility >= 0.465 -> { // 极强
                // 高亮第一行数据 (TableRow index 1, 因为标题行是index 0)
                if(table != null) highlightRow(table, 1)
            }
            financialVolatility >= 0.455 -> { // 强势
                if(table != null) highlightRow(table, 2)
            }
            financialVolatility >= 0.445 -> { // 正常
                if(table != null) highlightRow(table, 3)
            }
            financialVolatility >= 0.435 -> { // 偏弱
                if(table != null) highlightRow(table, 4)
            }
            financialVolatility >= 0.425 -> { // 弱势
                if(table != null) highlightRow(table, 5)
            }
            else -> { // 危机
                if(table != null) highlightRow(table, 6)
            }
        }
    }
    
    private fun highlightRow(table: TableLayout, rowNumber: Int) {
        if(rowNumber < table.childCount) {
            val row = table.getChildAt(rowNumber) as? TableRow
            row?.setBackgroundColor(android.graphics.Color.parseColor("#E3F2FD")) // 浅蓝色背景
        }
    }
    
    private fun clearTableHighlight() {
        val table = binding.root.findViewById<TableLayout>(R.id.dataTable)
        if(table != null) {
            for(i in 1 until table.childCount) { // 从1开始，跳过标题行
                val row = table.getChildAt(i) as? TableRow
                row?.setBackgroundColor(android.graphics.Color.TRANSPARENT) // 恢复透明背景
            }
        }
    }
}