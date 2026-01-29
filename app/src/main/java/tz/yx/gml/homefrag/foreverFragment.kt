package tz.yx.gml.homefrag

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tz.yx.gml.databinding.FragmentForeverBinding
import java.math.BigDecimal
import java.math.RoundingMode

class foreverFragment : Fragment() {

    private var _binding: FragmentForeverBinding? = null
    private val binding get() = _binding!!

    companion object {
        // 目标配置比例
        private val TARGET_RATIOS = mapOf(
            "reverseRepo" to BigDecimal("0.25"),      // 现金类（国债逆回购）25%
            "sp500Etf" to BigDecimal("0.15"),       // 股票类（标普500ETF）占股票类60%（60% of 25% = 15%）
            "cashFlowEtf" to BigDecimal("0.10"),    // 股票类（现金流ETF）占股票类40%（40% of 25% = 10%）
            "treasury10y" to BigDecimal("0.25"),    // 债券类（10年国债）25%
            "goldEtf" to BigDecimal("0.25")         // 商品类（黄金ETF）25%
        )

        // 大类资产目标比例
        private val MAJOR_ASSET_TARGETS = mapOf(
            "cash" to BigDecimal("0.25"),      // 现金类 25%
            "stock" to BigDecimal("0.25"),     // 股票类 25%
            "bond" to BigDecimal("0.25"),      // 债券类 25%
            "commodity" to BigDecimal("0.25")  // 商品类 25%
        )

        // 股票类内部目标比例
        private val STOCK_INTERNAL_TARGETS = mapOf(
            "sp500Etf" to BigDecimal("0.60"),     // 标普500ETF占股票类60%
            "cashFlowEtf" to BigDecimal("0.40")  // 现金流ETF占股票类40%
        )

        // BigDecimal计算精度
        private const val CALCULATION_SCALE = 8  // 使用更高精度进行中间计算
        private const val DISPLAY_SCALE = 2    // 显示精度
        
        private const val MAJOR_REBALANCING_THRESHOLD = 0.10  // 大类资产偏离阈值 10% (即<15%或>35%)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForeverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
        initializeDefaultValues()
    }

    private fun setupInputListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = calculateAllocation()
        }

        with(binding) {
            totalInvestment.addTextChangedListener(watcher)
            reverseRepoAmount.addTextChangedListener(watcher)
            sp500EtfAmount.addTextChangedListener(watcher)
            cashFlowEtfAmount.addTextChangedListener(watcher)
            treasury10yAmount.addTextChangedListener(watcher)
            goldEtfAmount.addTextChangedListener(watcher)
            additionalInvestment.addTextChangedListener(watcher)
        }
    }

    private fun initializeDefaultValues() {
        with(binding) {
            totalInvestment.setText("100000")
            reverseRepoAmount.setText("0")
            sp500EtfAmount.setText("0")
            cashFlowEtfAmount.setText("0")
            treasury10yAmount.setText("0")
            goldEtfAmount.setText("0")
            additionalInvestment.setText("0")
        }
    }

    private fun calculateAllocation() {
        try {
            val totalInvestment = parseInput(binding.totalInvestment.text.toString())
            val additionalInvestment = parseInput(binding.additionalInvestment.text.toString())

            // 获取各资产当前持有金额
            val currentAmounts = mapOf(
                "reverseRepo" to parseInput(binding.reverseRepoAmount.text.toString()),
                "sp500Etf" to parseInput(binding.sp500EtfAmount.text.toString()),
                "cashFlowEtf" to parseInput(binding.cashFlowEtfAmount.text.toString()),
                "treasury10y" to parseInput(binding.treasury10yAmount.text.toString()),
                "goldEtf" to parseInput(binding.goldEtfAmount.text.toString())
            )

            // 计算实际总持有金额（从持仓金额计算得出）
            val actualTotal = currentAmounts.values.sumOf { it }

            // 如果持仓金额总和为0，则使用输入的总金额作为参考
            val effectiveTotal = if (actualTotal.compareTo(BigDecimal.ZERO) > 0) {
                actualTotal
            } else {
                totalInvestment
            }

            // 计算当前占比
            val currentRatios = if (effectiveTotal.compareTo(BigDecimal.ZERO) > 0) {
                currentAmounts.mapValues { (_, amount) ->
                    amount.divide(effectiveTotal, CALCULATION_SCALE, RoundingMode.HALF_UP)
                }
            } else {
                // 如果没有持仓也没有输入总金额，则使用目标比例
                TARGET_RATIOS
            }

            // 更新UI显示
            updateDisplay(currentAmounts, currentRatios, effectiveTotal)

            // 如果有持仓，检查是否需要调仓
            if (actualTotal.compareTo(BigDecimal.ZERO) > 0) {
                checkRebalancingNeeds(currentAmounts, effectiveTotal)
            } else {
                // 没有持仓时，显示初始分配建议
                showInitialAllocationAdvice(totalInvestment)
            }

            // 计算追加投资分配建议（按目标比例分配）
            calculateAdditionalInvestmentAllocation(additionalInvestment)

        } catch (e: Exception) {
            resetResults()
        }
    }

    private fun updateDisplay(amounts: Map<String, BigDecimal>, ratios: Map<String, BigDecimal>, totalValue: BigDecimal) {
        with(binding) {
            // 更新各资产金额和占比显示，统一使用DISPLAY_SCALE精度
            reverseRepoValue.text = "${amounts["reverseRepo"]?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元"
            sp500EtfValue.text = "${amounts["sp500Etf"]?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元"
            cashFlowEtfValue.text = "${amounts["cashFlowEtf"]?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元"
            treasury10yValue.text = "${amounts["treasury10y"]?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元"
            goldEtfValue.text = "${amounts["goldEtf"]?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元"

            reverseRepoRatio.text = "${ratios["reverseRepo"]?.multiply(BigDecimal("100"))?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%"
            sp500EtfRatio.text = "${ratios["sp500Etf"]?.multiply(BigDecimal("100"))?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%"
            cashFlowEtfRatio.text = "${ratios["cashFlowEtf"]?.multiply(BigDecimal("100"))?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%"
            treasury10yRatio.text = "${ratios["treasury10y"]?.multiply(BigDecimal("100"))?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%"
            goldEtfRatio.text = "${ratios["goldEtf"]?.multiply(BigDecimal("100"))?.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%"

            totalCurrentValue.text = "${totalValue.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元"
        }
    }

    private fun checkRebalancingNeeds(currentAmounts: Map<String, BigDecimal>, actualTotal: BigDecimal) {
        val adviceText = StringBuilder()
        
        // 计算各大类资产金额
        val cashAmount = currentAmounts["reverseRepo"] ?: BigDecimal.ZERO
        val stockAmount = (currentAmounts["sp500Etf"] ?: BigDecimal.ZERO) + (currentAmounts["cashFlowEtf"] ?: BigDecimal.ZERO)
        val bondAmount = currentAmounts["treasury10y"] ?: BigDecimal.ZERO
        val commodityAmount = currentAmounts["goldEtf"] ?: BigDecimal.ZERO
        
        val majorAssetAmounts = mapOf(
            "cash" to cashAmount,
            "stock" to stockAmount,
            "bond" to bondAmount,
            "commodity" to commodityAmount
        )
        
        // 计算各大类资产占比，使用统一精度
        val majorAssetRatios = mutableMapOf<String, BigDecimal>()
        for ((assetType, amount) in majorAssetAmounts) {
            majorAssetRatios[assetType] = if (actualTotal.compareTo(BigDecimal.ZERO) > 0) {
                amount.divide(actualTotal, CALCULATION_SCALE, RoundingMode.HALF_UP)
            } else {
                MAJOR_ASSET_TARGETS[assetType] ?: BigDecimal.ZERO
            }
        }
        
        // 首先检查大类资产调仓
        // 找出超配和低配的资产
        val overallocatedAssets = mutableListOf<Pair<String, BigDecimal>>() // (assetType, excessAmount)
        val underallocatedAssets = mutableListOf<Pair<String, BigDecimal>>() // (assetType, shortageAmount)
        
        for ((assetType, currentRatio) in majorAssetRatios) {
            val targetRatio = MAJOR_ASSET_TARGETS[assetType] ?: BigDecimal.ZERO
            val lowerThreshold = targetRatio.subtract(BigDecimal.valueOf(MAJOR_REBALANCING_THRESHOLD)).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
            val upperThreshold = targetRatio.add(BigDecimal.valueOf(MAJOR_REBALANCING_THRESHOLD)).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
            
            val idealAmount = actualTotal.multiply(targetRatio).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
            val actualAmount = majorAssetAmounts[assetType] ?: BigDecimal.ZERO
            
            if (currentRatio >= upperThreshold) { // 超配 - 包含边界值
                val excess = actualAmount.subtract(idealAmount).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                if (excess.compareTo(BigDecimal.ZERO) > 0) {
                    overallocatedAssets.add(Pair(assetType, excess))
                }
            } else if (currentRatio <= lowerThreshold) { // 低配 - 包含边界值
                val shortage = idealAmount.subtract(actualAmount).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                if (shortage.compareTo(BigDecimal.ZERO) > 0) {
                    underallocatedAssets.add(Pair(assetType, shortage))
                }
            }
        }
        
        // 检查股票类内部调仓（无论股票类整体占比是否在合理范围内）
        val stockRatio = majorAssetRatios["stock"] ?: BigDecimal.ZERO
        val stockLowerBound = MAJOR_ASSET_TARGETS["stock"]!!.subtract(BigDecimal.valueOf(MAJOR_REBALANCING_THRESHOLD)).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)  // 0.15
        val stockUpperBound = MAJOR_ASSET_TARGETS["stock"]!!.add(BigDecimal.valueOf(MAJOR_REBALANCING_THRESHOLD)).setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)      // 0.35
        
        var internalRebalancingNeeded = false
        var internalAdviceText = ""
        if (stockAmount.compareTo(BigDecimal.ZERO) > 0) {  // 只有当股票类有持仓时才检查内部调仓
            internalRebalancingNeeded = checkStockInternalRebalancing(currentAmounts, stockAmount, actualTotal)
            if (internalRebalancingNeeded) {
                internalAdviceText = getStockInternalRebalancingAdvice(currentAmounts, stockAmount, actualTotal)
            }
        }
        
        // 如果存在超配和低配的资产，需要调仓
        if (overallocatedAssets.isNotEmpty() || underallocatedAssets.isNotEmpty()) {
            val majorAdvice = StringBuilder()
            
            // 显示超配资产
            if (overallocatedAssets.isNotEmpty()) {
                majorAdvice.append("📊【超配资产】:\n")
                for ((assetType, excess) in overallocatedAssets) {
                    val currentRatio = majorAssetRatios[assetType]!!
                    val targetRatio = MAJOR_ASSET_TARGETS[assetType]!!
                    majorAdvice.append("• ${getMajorAssetDisplayName(assetType)}: 超配 ${excess.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元 (当前占比 ${currentRatio.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%, 目标配比 ${targetRatio.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%)\n")
                    
                    // 对于股票类，进一步分解到具体ETF
                    if (assetType == "stock") {
                        val sp500EtfAmount = currentAmounts["sp500Etf"] ?: BigDecimal.ZERO
                        val cashFlowEtfAmount = currentAmounts["cashFlowEtf"] ?: BigDecimal.ZERO
                        val totalStockAmount = sp500EtfAmount.add(cashFlowEtfAmount)
                        
                        if (totalStockAmount.compareTo(BigDecimal.ZERO) > 0) {
                            val sp500Reduction = if (totalStockAmount.compareTo(BigDecimal.ZERO) > 0) {
                                excess.multiply(sp500EtfAmount).divide(totalStockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
                                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            } else {
                                BigDecimal.ZERO
                            }
                            val cashFlowReduction = if (totalStockAmount.compareTo(BigDecimal.ZERO) > 0) {
                                excess.multiply(cashFlowEtfAmount).divide(totalStockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
                                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            } else {
                                BigDecimal.ZERO
                            }
                            
                            majorAdvice.append("  └─ 建议减持: 标普500ETF约 ${sp500Reduction} 元，现金流ETF约 ${cashFlowReduction} 元\n")
                        }
                    }
                }
                majorAdvice.append("\n")
            }
            
            // 显示低配资产
            if (underallocatedAssets.isNotEmpty()) {
                majorAdvice.append("📉【低配资产】:\n")
                for ((assetType, shortage) in underallocatedAssets) {
                    val currentRatio = majorAssetRatios[assetType]!!
                    val targetRatio = MAJOR_ASSET_TARGETS[assetType]!!
                    majorAdvice.append("• ${getMajorAssetDisplayName(assetType)}: 低配 ${shortage.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元 (当前占比 ${currentRatio.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%, 目标配比 ${targetRatio.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%)\n")
                    
                    // 对于股票类，进一步分解到具体ETF
                    if (assetType == "stock") {
                        val sp500EtfAmount = currentAmounts["sp500Etf"] ?: BigDecimal.ZERO
                        val cashFlowEtfAmount = currentAmounts["cashFlowEtf"] ?: BigDecimal.ZERO
                        val totalStockAmount = sp500EtfAmount.add(cashFlowEtfAmount)
                        
                        if (totalStockAmount.compareTo(BigDecimal.ZERO) > 0) {
                            val sp500Addition = if (totalStockAmount.compareTo(BigDecimal.ZERO) > 0) {
                                shortage.multiply(sp500EtfAmount).divide(totalStockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
                                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            } else {
                                BigDecimal.ZERO
                            }
                            val cashFlowAddition = if (totalStockAmount.compareTo(BigDecimal.ZERO) > 0) {
                                shortage.multiply(cashFlowEtfAmount).divide(totalStockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
                                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            } else {
                                BigDecimal.ZERO
                            }
                            
                            majorAdvice.append("  └─ 建议增持: 标普500ETF约 ${sp500Addition} 元，现金流ETF约 ${cashFlowAddition} 元\n")
                        } else {
                            // 如果当前股票类没有持仓，按目标比例分配
                            val sp500Addition = shortage.multiply(STOCK_INTERNAL_TARGETS["sp500Etf"]!!)
                                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            val cashFlowAddition = shortage.multiply(STOCK_INTERNAL_TARGETS["cashFlowEtf"]!!)
                                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            
                            majorAdvice.append("  └─ 建议增持: 标普500ETF约 ${sp500Addition} 元，现金流ETF约 ${cashFlowAddition} 元\n")
                        }
                    }
                }
                majorAdvice.append("\n")
            }
            
            // 计算资产转移计划 - 内部调仓方案
            if (overallocatedAssets.isNotEmpty() && underallocatedAssets.isNotEmpty()) {
                majorAdvice.append("🔄【资产转移方案】:\n")
                majorAdvice.append("• 根据永久投资组合理论，建议通过内部资产调配实现再平衡:\n\n")
                
                // 精确计算每个超配资产向低配资产的转移金额
                for ((underAssetType, totalShortage) in underallocatedAssets) {
                    var stillNeed = totalShortage.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                    
                    if (stillNeed.compareTo(BigDecimal.ZERO) <= 0) continue
                    
                    // 按比例从各超配资产转移资金到此低配资产
                    val totalExcess = overallocatedAssets.sumOf { it.second }
                    
                    if (totalExcess.compareTo(BigDecimal.ZERO) > 0) {
                        for ((overAssetType, excessAmount) in overallocatedAssets) {
                            if (stillNeed.compareTo(BigDecimal.ZERO) <= 0) break
                            
                            // 按超配资产的相对大小分配转移金额
                            val proportion = excessAmount.divide(totalExcess, CALCULATION_SCALE, RoundingMode.HALF_UP)
                            val transferAmount = (proportion.multiply(stillNeed))
                                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                                
                            // 实际转移金额不能超过可用超配金额
                            val actualTransfer = minOf(transferAmount, excessAmount)
                                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            
                            if (actualTransfer.compareTo(BigDecimal.ZERO) > 0) {
                                majorAdvice.append("  ○ 从 ${getMajorAssetDisplayName(overAssetType)} 减持 ${actualTransfer} 元，买入 ${getMajorAssetDisplayName(underAssetType)}\n")
                                stillNeed = stillNeed.subtract(actualTransfer).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                            }
                        }
                    }
                }
                
                majorAdvice.append("\n📋【调仓要点】:\n")
                majorAdvice.append("• 本方案为内部资产调配建议，不涉及外部资金流入\n")
                majorAdvice.append("• 通过买卖操作实现资产再平衡，总投资额保持不变\n")
                majorAdvice.append("• 旨在恢复四大类资产各占25%的理想配置\n")
            } else if (overallocatedAssets.isNotEmpty()) {
                // 只有超配没有低配的情况
                majorAdvice.append("\n📋【调仓建议】:\n")
                majorAdvice.append("• 当前存在超配资产，建议减持超配资产并按目标比例买入其他资产\n")
                majorAdvice.append("• 通过内部资金调配实现资产再平衡\n")
            } else if (underallocatedAssets.isNotEmpty()) {
                // 只有低配没有超配的情况
                majorAdvice.append("\n📋【调仓建议】:\n")
                majorAdvice.append("• 当前存在低配资产，建议减持其他资产并买入低配资产\n")
                majorAdvice.append("• 如无其他资产可减持，需考虑追加投资以达到目标配置\n")
            }
            
            // 添加股票类内部调仓建议（如果存在）
            if (internalRebalancingNeeded) {
                majorAdvice.append("\n$internalAdviceText")
            }
            
            adviceText.append("⚠️【大类资产调仓】需要调仓\n\n$majorAdvice")
            binding.rebalancingAdvice.text = adviceText.toString()
            binding.rebalancingAdvice.setTextColor(resources.getColor(android.R.color.holo_orange_light, null))
            return
        } else if (internalRebalancingNeeded) {
            // 如果大类资产不需要调仓但股票类内部需要调仓
            adviceText.append(internalAdviceText)
            binding.rebalancingAdvice.text = adviceText.toString()
            binding.rebalancingAdvice.setTextColor(resources.getColor(android.R.color.holo_blue_dark, null))
            return
        }
        
        // 如果都没有需要调仓的，显示配置合理
        binding.rebalancingAdvice.text = "✅【当前配置合理】:\n\n当前各大类资产占比均在合理范围内，符合永久投资组合理论，无需调仓\n\n💡【温馨提示】:\n• 建议定期检查投资组合，一般每季度或半年复盘一次\n• 当市场波动导致资产配置偏离目标比例超过10%时，考虑执行调仓\n• 永久投资组合旨在通过均衡配置降低长期风险"
        binding.rebalancingAdvice.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
    }
    
    private fun checkStockInternalRebalancing(currentAmounts: Map<String, BigDecimal>, stockAmount: BigDecimal, actualTotal: BigDecimal): Boolean {
        if (stockAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false  // 股票类没有持仓，无需内部调仓
        }
        
        // 计算股票类内部占比
        val sp500EtfAmount = currentAmounts["sp500Etf"] ?: BigDecimal.ZERO
        val cashFlowEtfAmount = currentAmounts["cashFlowEtf"] ?: BigDecimal.ZERO
        
        val sp500RatioInStock = if (stockAmount.compareTo(BigDecimal.ZERO) > 0) {
            sp500EtfAmount.divide(stockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
        } else {
            STOCK_INTERNAL_TARGETS["sp500Etf"] ?: BigDecimal.ZERO
        }
        
        val cashFlowRatioInStock = if (stockAmount.compareTo(BigDecimal.ZERO) > 0) {
            cashFlowEtfAmount.divide(stockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
        } else {
            STOCK_INTERNAL_TARGETS["cashFlowEtf"] ?: BigDecimal.ZERO
        }
        
        // 检查内部比例是否失衡
        val sp500TargetInStock = STOCK_INTERNAL_TARGETS["sp500Etf"] ?: BigDecimal.ZERO
        val cashFlowTargetInStock = STOCK_INTERNAL_TARGETS["cashFlowEtf"] ?: BigDecimal.ZERO
        
        // 根据规范，股票类内部调仓阈值为：
        // 标普500ETF占比 > 75% 或 < 45%
        // 现金流ETF占比 > 55% 或 < 25%
        val sp500LowerThreshold = BigDecimal("0.45").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        val sp500UpperThreshold = BigDecimal("0.75").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        val cashFlowLowerThreshold = BigDecimal("0.25").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        val cashFlowUpperThreshold = BigDecimal("0.55").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        
        // 检查是否需要内部调仓
        val sp500OutOfThreshold = sp500RatioInStock < sp500LowerThreshold || sp500RatioInStock > sp500UpperThreshold
        val cashFlowOutOfThreshold = cashFlowRatioInStock < cashFlowLowerThreshold || cashFlowRatioInStock > cashFlowUpperThreshold
        
        return sp500OutOfThreshold || cashFlowOutOfThreshold
    }
    
    private fun getStockInternalRebalancingAdvice(currentAmounts: Map<String, BigDecimal>, stockAmount: BigDecimal, actualTotal: BigDecimal): String {
        if (stockAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return ""  // 股票类没有持仓，无需内部调仓
        }
        
        // 计算股票类内部占比
        val sp500EtfAmount = currentAmounts["sp500Etf"] ?: BigDecimal.ZERO
        val cashFlowEtfAmount = currentAmounts["cashFlowEtf"] ?: BigDecimal.ZERO
        
        val sp500RatioInStock = if (stockAmount.compareTo(BigDecimal.ZERO) > 0) {
            sp500EtfAmount.divide(stockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
        } else {
            STOCK_INTERNAL_TARGETS["sp500Etf"] ?: BigDecimal.ZERO
        }
        
        val cashFlowRatioInStock = if (stockAmount.compareTo(BigDecimal.ZERO) > 0) {
            cashFlowEtfAmount.divide(stockAmount, CALCULATION_SCALE, RoundingMode.HALF_UP)
        } else {
            STOCK_INTERNAL_TARGETS["cashFlowEtf"] ?: BigDecimal.ZERO
        }
        
        // 检查内部比例是否失衡
        val sp500TargetInStock = STOCK_INTERNAL_TARGETS["sp500Etf"] ?: BigDecimal.ZERO
        val cashFlowTargetInStock = STOCK_INTERNAL_TARGETS["cashFlowEtf"] ?: BigDecimal.ZERO
        
        // 根据规范，股票类内部调仓阈值为：
        // 标普500ETF占比 > 75% 或 < 45%
        // 现金流ETF占比 > 55% 或 < 25%
        val sp500LowerThreshold = BigDecimal("0.45").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        val sp500UpperThreshold = BigDecimal("0.75").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        val cashFlowLowerThreshold = BigDecimal("0.25").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        val cashFlowUpperThreshold = BigDecimal("0.55").setScale(CALCULATION_SCALE, RoundingMode.HALF_UP)
        
        val internalAdvice = StringBuilder()
        
        if (sp500RatioInStock < sp500LowerThreshold || sp500RatioInStock > sp500UpperThreshold) {
            val sp500IdealInStock = stockAmount.multiply(sp500TargetInStock)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
            
            val direction = if (sp500RatioInStock > sp500TargetInStock) "超配" else "低配"
            
            internalAdvice.append("📊【标普500ETF】:\n")
            internalAdvice.append("• 在股票类中占比 ${sp500RatioInStock.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%，目标占比 ${sp500TargetInStock.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%，属于$direction\n")
            internalAdvice.append("• 股票类总额 ${stockAmount.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元，标普500理想金额应为 $sp500IdealInStock 元，当前金额为 ${sp500EtfAmount.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元\n")
            
            if (sp500RatioInStock > sp500TargetInStock) {
                val excess = sp500EtfAmount.subtract(sp500IdealInStock)
                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                internalAdvice.append("• 超配 $excess 元，建议减持标普500ETF，将资金转移至现金流ETF\n\n")
            } else {
                val shortage = sp500IdealInStock.subtract(sp500EtfAmount)
                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                internalAdvice.append("• 低配 $shortage 元，建议增持标普500ETF，可从现金流ETF转移资金\n\n")
            }
        }
        
        if (cashFlowRatioInStock < cashFlowLowerThreshold || cashFlowRatioInStock > cashFlowUpperThreshold) {
            val cashFlowIdealInStock = stockAmount.multiply(cashFlowTargetInStock)
                .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
            
            val direction = if (cashFlowRatioInStock > cashFlowTargetInStock) "超配" else "低配"
            
            internalAdvice.append("📊【现金流ETF】:\n")
            internalAdvice.append("• 在股票类中占比 ${cashFlowRatioInStock.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%，目标占比 ${cashFlowTargetInStock.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)}%，属于$direction\n")
            internalAdvice.append("• 股票类总额 ${stockAmount.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元，现金流ETF理想金额应为 $cashFlowIdealInStock 元，当前金额为 ${cashFlowEtfAmount.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元\n")
            
            if (cashFlowRatioInStock > cashFlowTargetInStock) {
                val excess = cashFlowEtfAmount.subtract(cashFlowIdealInStock)
                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                internalAdvice.append("• 超配 $excess 元，建议减持现金流ETF，将资金转移至标普500ETF\n\n")
            } else {
                val shortage = cashFlowIdealInStock.subtract(cashFlowEtfAmount)
                    .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                internalAdvice.append("• 低配 $shortage 元，建议增持现金流ETF，可从标普500ETF转移资金\n\n")
            }
        }
        
        val fullAdvice = StringBuilder()
        fullAdvice.append("ℹ️【股票类内部调仓】:\n")
        fullAdvice.append("• 本调仓建议针对股票类内部的两种ETF进行优化配置\n")
        fullAdvice.append("• 股票类内部目标：标普500ETF占60%，现金流ETF占40%\n")
        fullAdvice.append("• 调仓原则：通过内部资产转移实现再平衡，总投资额不变\n\n")
        fullAdvice.append(internalAdvice.toString())
        
        return fullAdvice.toString()
    }
    
    private fun getMajorAssetDisplayName(assetType: String): String {
        return when(assetType) {
            "cash" -> "现金类(国债逆回购)"
            "stock" -> "股票类(标普500ETF+现金流ETF)"
            "bond" -> "债券类(10年国债)"
            "commodity" -> "商品类(黄金ETF)"
            else -> assetType
        }
    }
    
    // 显示初始分配建议
    private fun showInitialAllocationAdvice(totalInvestment: BigDecimal) {
        val adviceText = StringBuilder()
        adviceText.append("💡【初始分配建议】:\n\n")
        adviceText.append("根据永久投资组合理论，建议将资金按以下比例分配到四大类资产:\n\n")
        TARGET_RATIOS.forEach { (asset, ratio) ->
            val idealAmount = totalInvestment.multiply(ratio).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
            val percentage = ratio.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
            adviceText.append("• ${getAssetDisplayName(asset)}: ${percentage}% (${idealAmount} 元)\n")
        }
        adviceText.append("\n💡【永久投资组合核心理念】:\n")
        adviceText.append("• 现金类、股票类、债券类、商品类各占25%，实现风险分散\n")
        adviceText.append("• 通过定期调仓维持均衡配置，适应经济周期变化\n")
        
        binding.rebalancingAdvice.text = adviceText.toString()
        binding.rebalancingAdvice.setTextColor(resources.getColor(android.R.color.holo_blue_light, null))
    }
    
    // 计算追加投资的分配建议（按目标比例分配）
    private fun calculateAdditionalInvestmentAllocation(additionalInvestment: BigDecimal) {
        if (additionalInvestment.compareTo(BigDecimal.ZERO) <= 0) {
            binding.additionalInvestmentAdvice.text = "追加投资分配建议：暂无追加投资"
            return
        }

        val adviceText = StringBuilder()
        adviceText.append("💡【追加投资分配建议】:\n\n")
        adviceText.append("基于永久投资组合理论，建议将追加投资按目标比例分配:\n\n")
        adviceText.append("追加投资总额: ${additionalInvestment.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)} 元\n\n")

        // 按四大类资产分配追加投资
        for ((assetType, targetRatio) in MAJOR_ASSET_TARGETS) {
            val allocation = additionalInvestment.multiply(targetRatio).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
            val percentage = targetRatio.multiply(BigDecimal("100")).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
            
            when (assetType) {
                "cash" -> adviceText.append("${getMajorAssetDisplayName(assetType)}: ${percentage}% (${allocation} 元)\n")
                "stock" -> {
                    // 股票类内部按目标比例分配
                    val sp500Allocation = allocation.multiply(STOCK_INTERNAL_TARGETS["sp500Etf"]!!)
                        .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                    val cashFlowAllocation = allocation.multiply(STOCK_INTERNAL_TARGETS["cashFlowEtf"]!!)
                        .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                    val sp500Percentage = STOCK_INTERNAL_TARGETS["sp500Etf"]!!.multiply(BigDecimal("100"))
                        .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                    val cashFlowPercentage = STOCK_INTERNAL_TARGETS["cashFlowEtf"]!!.multiply(BigDecimal("100"))
                        .setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
                    
                    adviceText.append("${getMajorAssetDisplayName(assetType)}: ${percentage}% (${allocation} 元)\n")
                    adviceText.append("  - 标普500ETF: ${sp500Percentage}% (${sp500Allocation} 元)\n")
                    adviceText.append("  - 现金流ETF: ${cashFlowPercentage}% (${cashFlowAllocation} 元)\n")
                }
                "bond" -> adviceText.append("${getMajorAssetDisplayName(assetType)}: ${percentage}% (${allocation} 元)\n")
                "commodity" -> adviceText.append("${getMajorAssetDisplayName(assetType)}: ${percentage}% (${allocation} 元)\n")
            }
        }

        binding.additionalInvestmentAdvice.text = adviceText.toString()
    }

    private fun getAssetDisplayName(asset: String): String {
        return when(asset) {
            "reverseRepo" -> "现金(国债逆回购)"
            "sp500Etf" -> "股票(标普500ETF)"
            "cashFlowEtf" -> "股票(现金流ETF)"
            "treasury10y" -> "债券(10年国债)"
            "goldEtf" -> "商品(黄金ETF)"
            else -> asset
        }
    }

    private fun resetResults() {
        with(binding) {
            reverseRepoValue.text = "0.00 元"
            sp500EtfValue.text = "0.00 元"
            cashFlowEtfValue.text = "0.00 元"
            treasury10yValue.text = "0.00 元"
            goldEtfValue.text = "0.00 元"

            reverseRepoRatio.text = "0.00%"
            sp500EtfRatio.text = "0.00%"
            cashFlowEtfRatio.text = "0.00%"
            treasury10yRatio.text = "0.00%"
            goldEtfRatio.text = "0.00%"

            totalCurrentValue.text = "0.00 元"
            rebalancingAdvice.text = "📋【使用说明】:\n• 本工具用于永久投资组合配置分析\n• 输入总投资金额及各类资产持仓金额\n• 系统将自动分析当前配置并提供调仓建议\n• 调仓建议均为内部资产调配，不涉及外部资金\n• 目标：四大类资产各占25%的理想配置"
            additionalInvestmentAdvice.text = "💡【追加投资建议】:\n• 如需追加投资，请在下方输入金额\n• 系统将提供按目标比例分配的建议"
        }
    }

    private fun parseInput(value: String): BigDecimal = try {
        if (value.isEmpty()) BigDecimal.ZERO else BigDecimal(value).setScale(DISPLAY_SCALE, RoundingMode.HALF_UP)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}