package tz.yx.gml.homefrag

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tz.yx.gml.databinding.FragmentUsdtBinding
import java.math.BigDecimal
import java.math.RoundingMode

class UsdtFragment : Fragment() {

    private var _binding: FragmentUsdtBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUsdtBinding.inflate(inflater, container, false)
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
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                calculateProfit()
            }
        }

        with(binding) {
            mymoney.addTextChangedListener(textWatcher)
            contractSize.addTextChangedListener(textWatcher)
            lever.addTextChangedListener(textWatcher)
            stopprice.addTextChangedListener(textWatcher)
        }
    }

    private fun calculateProfit() {
        try {
            val buyPriceText = binding.mymoney.text.toString()
            val buyAmountText = binding.contractSize.text.toString()
            val sellPriceText = binding.lever.text.toString()
            val feeText = binding.stopprice.text.toString()

            if (buyPriceText.isEmpty() || buyAmountText.isEmpty() ||
                sellPriceText.isEmpty() || feeText.isEmpty()) {
                binding.shouldpos.text = "请输入所有数值"
                return
            }

            val buyPrice = BigDecimal(buyPriceText)
            val buyAmountCNY = BigDecimal(buyAmountText)
            val sellPrice = BigDecimal(sellPriceText)
            val networkFee = BigDecimal(feeText)

            if (buyPrice <= BigDecimal.ZERO || buyAmountCNY <= BigDecimal.ZERO ||
                sellPrice <= BigDecimal.ZERO || networkFee < BigDecimal.ZERO) {
                binding.shouldpos.text = "请输入有效的数值"
                return
            }

            val buyAmountUSD = buyAmountCNY.divide(buyPrice, 2, RoundingMode.DOWN)
            
            val sellAmountCNY = sellPrice.multiply(buyAmountUSD).setScale(2, RoundingMode.DOWN)
            
            val feeInCNY = networkFee.multiply(sellPrice).setScale(2, RoundingMode.DOWN)
            
            val profitOrLoss = sellAmountCNY.subtract(buyAmountCNY).subtract(feeInCNY)
            
            val resultText = when {
                profitOrLoss > BigDecimal.ZERO -> {
                    "盈利: ${profitOrLoss.abs()} CNY"
                }
                profitOrLoss < BigDecimal.ZERO -> {
                    "亏损: ${profitOrLoss.abs()} CNY"
                }
                else -> {
                    "盈亏: 0 CNY"
                }
            }
            
            binding.shouldpos.text = resultText

        } catch (e: Exception) {
            binding.shouldpos.text = "计算错误"
        }
    }
}