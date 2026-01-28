package tz.yx.gml.homefrag

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tz.yx.gml.databinding.FragmentStockBinding
import java.math.BigDecimal
import java.math.RoundingMode

class StockFragment : Fragment() {

    private var _binding: FragmentStockBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
    }

    private fun setupInputListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = calculateStockRebuy()
        }

        with(binding) {
            firstBuyPrice.addTextChangedListener(watcher)
            firstBuyQuantity.addTextChangedListener(watcher)
            rebuyPrice.addTextChangedListener(watcher)
            rebuyQuantity.addTextChangedListener(watcher)
            commissionRate.addTextChangedListener(watcher)
            stampDutyRate.addTextChangedListener(watcher)
            transferFeeRate.addTextChangedListener(watcher)
        }
    }

    private fun calculateStockRebuy() {
        try {
            val firstBuyPrice = parseInput(binding.firstBuyPrice.text.toString())
            val firstBuyQuantity = parseInput(binding.firstBuyQuantity.text.toString())
            val rebuyPrice = parseInput(binding.rebuyPrice.text.toString())
            val rebuyQuantity = parseInput(binding.rebuyQuantity.text.toString())
            val commissionRate = parseInput(binding.commissionRate.text.toString()).divide(BigDecimal("10000"), 6, RoundingMode.DOWN) // 万分之转为小数
            val stampDutyRate = parseInput(binding.stampDutyRate.text.toString()).divide(BigDecimal("100"), 6, RoundingMode.DOWN) // 百分比转为小数
            val transferFeeRate = parseInput(binding.transferFeeRate.text.toString()).divide(BigDecimal("100"), 6, RoundingMode.DOWN) // 百分比转为小数

            if (firstBuyPrice <= BigDecimal.ZERO || firstBuyQuantity <= BigDecimal.ZERO ||
                rebuyPrice <= BigDecimal.ZERO || rebuyQuantity <= BigDecimal.ZERO) {
                resetResultFields()
                return
            }

            val totalQuantity = firstBuyQuantity.add(rebuyQuantity)

            val firstBuyAmount = firstBuyPrice.multiply(firstBuyQuantity)
            val firstBuyCommission = firstBuyAmount.multiply(commissionRate)
            val firstBuyTransferFee = firstBuyAmount.multiply(transferFeeRate)
            val firstBuyTotalCost = firstBuyAmount.add(firstBuyCommission).add(firstBuyTransferFee)
            
            val rebuyAmount = rebuyPrice.multiply(rebuyQuantity)
            val rebuyCommission = rebuyAmount.multiply(commissionRate)
            val rebuyTransferFee = rebuyAmount.multiply(transferFeeRate)
            val rebuyTotalCost = rebuyAmount.add(rebuyCommission).add(rebuyTransferFee)
            
            val totalCost = firstBuyTotalCost.add(rebuyTotalCost)
            
            val finalCost = if (totalQuantity > BigDecimal.ZERO) {
                totalCost.divide(totalQuantity, 4, RoundingMode.DOWN)
            } else {
                BigDecimal.ZERO
            }
            
            val currentPrice = rebuyPrice
            val currentValue = currentPrice.multiply(totalQuantity)
            
            val sellCommission = currentValue.multiply(commissionRate)
            val sellStampDuty = currentValue.multiply(stampDutyRate)
            val sellTransferFee = currentValue.multiply(transferFeeRate)
            val totalSellCost = sellCommission.add(sellStampDuty).add(sellTransferFee)
            
            val currentProfit = currentValue.subtract(totalCost).subtract(totalSellCost)

            with(binding) {
                "${finalCost.setScale(4, RoundingMode.DOWN)} CNY/股".also { finalCostValue.text = it }
                "${currentProfit.setScale(2, RoundingMode.DOWN)} CNY".also { currentProfitValue.text = it }
                "${totalQuantity.setScale(0, RoundingMode.DOWN)} 股".also { totalQuantityValue.text = it }
            }
        } catch (e: Exception) {
            resetResultFields()
        }
    }

    private fun resetResultFields() {
        with(binding) {
            "CNY/股".also { finalCostValue.text = it }
            "CNY".also { currentProfitValue.text = it }
            "股".also { totalQuantityValue.text = it }
        }
    }

    private fun parseInput(value: String): BigDecimal = try {
        BigDecimal(value).setScale(6, RoundingMode.HALF_UP)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}