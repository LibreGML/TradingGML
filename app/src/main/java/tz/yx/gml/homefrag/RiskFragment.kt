package tz.yx.gml.homefrag

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import tz.yx.gml.R
import tz.yx.gml.databinding.FragmentRiskBinding
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.pow

class RiskFragment : Fragment() {
    
    private var _binding: FragmentRiskBinding? = null
    private val binding get() = _binding!!
    
    // 利率类型选项
    private val rateTypes = mapOf(
        R.id.radioAnnualRate to "年利率(%)",
        R.id.radioMonthlyRate to "月利率(%)",
        R.id.radioDailyRate to "日利率(%)",
        R.id.radioSevenDayYield to "七日年化收益率(%)",
        R.id.radioWanfenIncome to "万份收益(元)",
        R.id.radioTreasuryRepo to "国债逆回购年化收益率(%)"
    )
    
    // 复利频率选项
    private val compoundFrequencies = mapOf(
        R.id.radioAnnualCompound to "年复利",
        R.id.radioMonthlyCompound to "月复利",
        R.id.radioDailyCompound to "日复利",
        R.id.radioQuarterlyCompound to "季度复利",
        R.id.radioSemiAnnualCompound to "半年复利"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiskBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRadioGroups()
        setupInputListeners()
        calculateInterestRates() // 初始化计算
        
        // 添加说明文本
        showHelpMessage()
    }
    
    private fun showHelpMessage() {
        // 在界面上显示简短说明
        // 由于布局中没有专门的说明区域，我们可以通过日志来提醒用户
        Log.i("RiskFragment", "使用说明: 选择利率类型 -> 输入数值 -> 查看转换结果。系统自动处理复利计算。")
    }
    
    private fun setupRadioGroups() {
        // 设置利率类型单选组
        binding.rateTypeRadioGroup.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            calculateInterestRates()
        }
        
        // 设置复利频率单选组
        binding.compoundFreqRadioGroup.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            calculateInterestRates()
        }
        
        // 默认选中第一个按钮（年利率 + 年复利），这对大多数用户来说最直观
        binding.rateTypeRadioGroup.check(R.id.radioAnnualRate)
        binding.compoundFreqRadioGroup.check(R.id.radioAnnualCompound)
    }
    
    private fun setupInputListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateInterestRates()
            }
        }
        
        with(binding) {
            rateValue.addTextChangedListener(watcher)
            cpiRate.addTextChangedListener(watcher)
        }
    }
    
    private fun calculateInterestRates() {
        try {
            val inputText = binding.rateValue.text.toString().trim()
            if (inputText.isEmpty()) {
                updateResults(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                return
            }
            
            val inputValue = parseInput(inputText)
            if (inputValue.compareTo(BigDecimal.ZERO) <= 0) {
                updateResults(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
                return
            }
            
            val selectedRateType = getSelectedRateType()
            val compoundFreq = getSelectedCompoundFrequency()
            val cpiRateText = binding.cpiRate.text.toString().trim()
            val cpiRate = if (cpiRateText.isEmpty()) BigDecimal.ZERO else parseInput(cpiRateText)
            
            Log.d("RiskFragment", "Input Value: $inputValue, Type: $selectedRateType, Compound Freq: $compoundFreq, CPI: $cpiRate")
            
            // 根据选择的利率类型和复利频率，统一转换为有效年利率
            val effectiveAnnualRate = convertToEffectiveAnnualRate(inputValue, selectedRateType, compoundFreq)
            
            Log.d("RiskFragment", "Effective Annual Rate: $effectiveAnnualRate")
            
            // 基于有效年利率计算其他利率类型
            val monthlyRate = calculateMonthlyRateFromEffective(effectiveAnnualRate, compoundFreq)
            val dailyRate = calculateDailyRateFromEffective(effectiveAnnualRate, compoundFreq)
            val sevenDayYield = effectiveAnnualRate // 七日年化收益率与年利率基本等效
            val wanfenIncome = calculateWanfenIncome(sevenDayYield)
            val realAnnualRate = calculateRealRate(effectiveAnnualRate, cpiRate)
            
            Log.d("RiskFragment", "Calculated rates - Monthly: $monthlyRate, Daily: $dailyRate, 7-day yield: $sevenDayYield, Wanfen: $wanfenIncome, Real: $realAnnualRate")
            
            // 更新结果显示
            updateResults(effectiveAnnualRate, monthlyRate, dailyRate, sevenDayYield, wanfenIncome, realAnnualRate)
            
        } catch (e: Exception) {
            Log.e("RiskFragment", "Error calculating interest rates", e)
            // 发生异常时清空结果
            updateResults(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        }
    }
    
    private fun getSelectedRateType(): String {
        val checkedId = binding.rateTypeRadioGroup.checkedRadioButtonId
        return rateTypes[checkedId] ?: "年利率(%)"
    }
    
    private fun getSelectedCompoundFrequency(): String {
        val checkedId = binding.compoundFreqRadioGroup.checkedRadioButtonId
        return compoundFrequencies[checkedId] ?: "年复利"
    }
    
    /**
     * 将输入的利率转换为有效年利率（考虑复利效应）
     */
    fun convertToEffectiveAnnualRatePublic(inputValue: BigDecimal, rateType: String, compoundFreq: String): BigDecimal {
        return convertToEffectiveAnnualRate(inputValue, rateType, compoundFreq)
    }
    
    /**
     * 将输入的利率转换为有效年利率（考虑复利效应）
     */
    private fun convertToEffectiveAnnualRate(inputValue: BigDecimal, rateType: String, compoundFreq: String): BigDecimal {
        return when (rateType) {
            "年利率(%)" -> {
                when (compoundFreq) {
                    "年复利" -> inputValue
                    "月复利" -> calculateEffectiveAnnualRateFromNominalMonthly(inputValue)
                    "日复利" -> calculateEffectiveAnnualRateFromNominalDaily(inputValue)
                    "季度复利" -> calculateEffectiveAnnualRateFromNominalQuarterly(inputValue)
                    "半年复利" -> calculateEffectiveAnnualRateFromNominalSemiAnnual(inputValue)
                    else -> inputValue
                }
            }
            "月利率(%)" -> {
                // 不管复利频率如何，都按月复利计算有效年利率
                calculateEffectiveAnnualRateFromMonthlyRate(inputValue)
            }
            "日利率(%)" -> {
                // 不管复利频率如何，都按日复利计算有效年利率
                calculateEffectiveAnnualRateFromDailyRate(inputValue)
            }
            "七日年化收益率(%)" -> inputValue // 直接作为年化利率
            "万份收益(元)" -> {
                // 从万份收益转换为年利率：(万份收益 / 10000) * 365 * 100
                inputValue.divide(BigDecimal.valueOf(10000), 10, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(365 * 100))
                    .setScale(4, RoundingMode.HALF_UP)
            }
            "国债逆回购年化收益率(%)" -> inputValue // 直接作为年化利率
            else -> inputValue
        }
    }
    
    // 从名义月利率计算有效年利率（按月复利）
    private fun calculateEffectiveAnnualRateFromNominalMonthly(monthlyRate: BigDecimal): BigDecimal {
        val monthlyDecimal = monthlyRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val one = BigDecimal.ONE
        val result = powBigDecimal(one.add(monthlyDecimal), 12).subtract(one)
        return result.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
    }
    
    // 从名义日利率计算有效年利率（按日复利）
    private fun calculateEffectiveAnnualRateFromNominalDaily(dailyRate: BigDecimal): BigDecimal {
        val dailyDecimal = dailyRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val one = BigDecimal.ONE
        val result = powBigDecimal(one.add(dailyDecimal), 365).subtract(one)
        return result.multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP)
    }
    
    // 从名义季度利率计算有效年利率（按季复利）
    private fun calculateEffectiveAnnualRateFromNominalQuarterly(quarterlyRate: BigDecimal): BigDecimal {
        val quarterlyDecimal = quarterlyRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val one = BigDecimal.ONE
        val result = powBigDecimal(one.add(quarterlyDecimal), 4).subtract(one)
        return result.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
    }
    
    // 从名义半年利率计算有效年利率（按半年复利）
    private fun calculateEffectiveAnnualRateFromNominalSemiAnnual(semiAnnualRate: BigDecimal): BigDecimal {
        val semiAnnualDecimal = semiAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val one = BigDecimal.ONE
        val result = powBigDecimal(one.add(semiAnnualDecimal), 2).subtract(one)
        return result.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
    }
    
    // 从月利率计算有效年利率（按月复利）
    private fun calculateEffectiveAnnualRateFromMonthlyRate(monthlyRate: BigDecimal): BigDecimal {
        return calculateEffectiveAnnualRateFromNominalMonthly(monthlyRate)
    }
    
    // 从日利率计算有效年利率（按日复利）
    private fun calculateEffectiveAnnualRateFromDailyRate(dailyRate: BigDecimal): BigDecimal {
        return calculateEffectiveAnnualRateFromNominalDaily(dailyRate)
    }
    
    // 从有效年利率计算月利率（考虑复利频率）
    private fun calculateMonthlyRateFromEffective(effectiveAnnualRate: BigDecimal, compoundFreq: String): BigDecimal {
        return when (compoundFreq) {
            "月复利" -> {
                // (1 + 年利率/100)^(1/12) - 1，然后乘以100得到百分比
                val annualDecimal = effectiveAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val result = powBigDecimal(one.add(annualDecimal), 1.0/12.0).subtract(one)
                result.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
            }
            "日复利" -> {
                // 先计算日利率，再转换为月利率
                val dailyRate = calculateDailyRateFromEffective(effectiveAnnualRate, "日复利")
                // 日利率转换为月利率：((1 + 日利率/100)^30 - 1) * 100
                val dailyDecimal = dailyRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val result = powBigDecimal(one.add(dailyDecimal), 30.0).subtract(one)
                result.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
            }
            "季度复利" -> {
                // 先计算季度利率，再转换为月利率
                val annualDecimal = effectiveAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val quarterlyResult = powBigDecimal(one.add(annualDecimal), 1.0/4.0).subtract(one)
                val quarterlyRate = quarterlyResult.multiply(BigDecimal.valueOf(100))
                // 季度利率转换为月利率：((1 + 季度利率/100)^(1/3) - 1) * 100
                val quarterlyDecimal = quarterlyRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val monthlyResult = powBigDecimal(one.add(quarterlyDecimal), 1.0/3.0).subtract(one)
                monthlyResult.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
            }
            "半年复利" -> {
                // 先计算半年利率，再转换为月利率
                val annualDecimal = effectiveAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val semiAnnualResult = powBigDecimal(one.add(annualDecimal), 1.0/2.0).subtract(one)
                val semiAnnualRate = semiAnnualResult.multiply(BigDecimal.valueOf(100))
                // 半年利率转换为月利率：((1 + 半年利率/100)^(1/6) - 1) * 100
                val semiAnnualDecimal = semiAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val monthlyResult = powBigDecimal(one.add(semiAnnualDecimal), 1.0/6.0).subtract(one)
                monthlyResult.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
            }
            else -> {
                // 年复利情况下，简单除以12
                effectiveAnnualRate.divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP)
            }
        }
    }
    
    // 从有效年利率计算日利率（考虑复利频率）
    private fun calculateDailyRateFromEffective(effectiveAnnualRate: BigDecimal, compoundFreq: String): BigDecimal {
        return when (compoundFreq) {
            "日复利" -> {
                // (1 + 年利率/100)^(1/365) - 1，然后乘以100得到百分比
                val annualDecimal = effectiveAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val result = powBigDecimal(one.add(annualDecimal), 1.0/365.0).subtract(one)
                result.multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP)
            }
            "月复利" -> {
                // 先计算月利率，再转换为日利率
                val monthlyRate = calculateMonthlyRateFromEffective(effectiveAnnualRate, "月复利")
                val monthlyDecimal = monthlyRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val result = powBigDecimal(one.add(monthlyDecimal), 1.0/30.0).subtract(one)
                result.multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP)
            }
            "季度复利" -> {
                // 先计算季度利率，再转换为日利率
                val annualDecimal = effectiveAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val quarterlyResult = powBigDecimal(one.add(annualDecimal), 1.0/4.0).subtract(one)
                val quarterlyRate = quarterlyResult.multiply(BigDecimal.valueOf(100))
                // 季度利率转换为日利率：((1 + 季度利率/100)^(1/91.25) - 1) * 100
                val quarterlyDecimal = quarterlyRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val dailyResult = powBigDecimal(one.add(quarterlyDecimal), 1.0/91.25).subtract(one)
                dailyResult.multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP)
            }
            "半年复利" -> {
                // 先计算半年利率，再转换为日利率
                val annualDecimal = effectiveAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val one = BigDecimal.ONE
                val semiAnnualResult = powBigDecimal(one.add(annualDecimal), 1.0/2.0).subtract(one)
                val semiAnnualRate = semiAnnualResult.multiply(BigDecimal.valueOf(100))
                // 半年利率转换为日利率：((1 + 半年利率/100)^(1/182.5) - 1) * 100
                val semiAnnualDecimal = semiAnnualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                val dailyResult = powBigDecimal(one.add(semiAnnualDecimal), 1.0/182.5).subtract(one)
                dailyResult.multiply(BigDecimal.valueOf(100)).setScale(6, RoundingMode.HALF_UP)
            }
            else -> {
                // 年复利情况下，简单除以365
                effectiveAnnualRate.divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP)
            }
        }
    }
    
    private fun calculateWanfenIncome(sevenDayYield: BigDecimal): BigDecimal {
        // 万份收益 = (七日年化收益率 / 100 / 365) * 10000
        val dailyRate = sevenDayYield.divide(BigDecimal.valueOf(100 * 365), 10, RoundingMode.HALF_UP)
        return dailyRate.multiply(BigDecimal.valueOf(10000)).setScale(4, RoundingMode.HALF_UP)
    }
    
    private fun calculateAnnualRateFromWanfen(wanfenIncome: BigDecimal): BigDecimal {
        // 从万份收益反推年利率: (万份收益 / 10000) * 365 * 100
        return wanfenIncome.divide(BigDecimal.valueOf(10000), 10, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(365 * 100))
            .setScale(4, RoundingMode.HALF_UP)
    }
    
    private fun calculateRealRate(nominalRate: BigDecimal, inflationRate: BigDecimal): BigDecimal {
        // 实际利率 = (1 + 名义利率/100) / (1 + 通胀率/100) - 1，然后乘以100得到百分比
        if (inflationRate.compareTo(BigDecimal.ZERO) == 0) {
            return nominalRate
        }
        
        val one = BigDecimal.ONE
        val nominalDecimal = nominalRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        val inflationDecimal = inflationRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
        
        val realRateDecimal = one.add(nominalDecimal).divide(one.add(inflationDecimal), 10, RoundingMode.HALF_UP).subtract(one)
        return realRateDecimal.multiply(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP)
    }
    
    private fun updateResults(
        annualRate: BigDecimal,
        monthlyRate: BigDecimal, 
        dailyRate: BigDecimal,
        sevenDayYield: BigDecimal,
        wanfenIncome: BigDecimal,
        realAnnualRate: BigDecimal
    ) {
        binding.annualRateResult.text = "${annualRate.setScale(4, RoundingMode.HALF_UP)}%"
        binding.monthlyRateResult.text = "${monthlyRate.setScale(4, RoundingMode.HALF_UP)}%"
        binding.dailyRateResult.text = "${dailyRate.setScale(6, RoundingMode.HALF_UP)}%"
        binding.sevenDayYieldResult.text = "${sevenDayYield.setScale(4, RoundingMode.HALF_UP)}%"
        binding.wanfenResult.text = "${wanfenIncome.setScale(4, RoundingMode.HALF_UP)}元"
        binding.realAnnualRateResult.text = "${realAnnualRate.setScale(4, RoundingMode.HALF_UP)}%"
    }
    
    private fun parseInput(value: String): BigDecimal = try {
        if (value.isEmpty()) BigDecimal.ZERO
        else BigDecimal(value).setScale(8, RoundingMode.HALF_UP)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
    
    // BigDecimal的幂运算辅助函数 - 修复精度问题
    private fun powBigDecimal(base: BigDecimal, exponent: Double): BigDecimal {
        val baseDouble = base.toDouble()
        val result = baseDouble.pow(exponent)
        // 避免NaN或无穷大结果
        return if (result.isNaN() || result.isInfinite()) {
            when {
                exponent == 0.0 -> BigDecimal.ONE
                exponent > 0 && base == BigDecimal.ONE -> BigDecimal.ONE
                else -> BigDecimal.ZERO
            }
        } else {
            BigDecimal(result.toString()).setScale(10, RoundingMode.HALF_UP)
        }
    }
    
    // 重载版本，用于整数指数
    private fun powBigDecimal(base: BigDecimal, exponent: Int): BigDecimal {
        return base.pow(exponent, MathContext(10, RoundingMode.HALF_UP))
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}