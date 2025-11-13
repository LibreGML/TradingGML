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
            // 获取输入值
            val buyPriceText = binding.mymoney.text.toString()
            val buyAmountText = binding.contractSize.text.toString()
            val sellPriceText = binding.lever.text.toString()
            val feeText = binding.stopprice.text.toString()

            // 检查是否有空值
            if (buyPriceText.isEmpty() || buyAmountText.isEmpty() || 
                sellPriceText.isEmpty() || feeText.isEmpty()) {
                binding.shouldpos.text = "请输入所有数值"
                return
            }

            // 转换为BigDecimal进行精确计算
            val buyPrice = BigDecimal(buyPriceText)
            val buyAmountCNY = BigDecimal(buyAmountText)
            val sellPrice = BigDecimal(sellPriceText)
            val networkFee = BigDecimal(feeText)

            // 检查输入值是否为正数
            if (buyPrice <= BigDecimal.ZERO || buyAmountCNY <= BigDecimal.ZERO || 
                sellPrice <= BigDecimal.ZERO || networkFee < BigDecimal.ZERO) {
                binding.shouldpos.text = "请输入有效的数值"
                return
            }

            // 计算买入的USDT数量 (买入金额 / 买入价格) - 截断到小数点后2位
            val buyAmountUSD = buyAmountCNY.divide(buyPrice, 2, RoundingMode.DOWN)
            
            // 计算卖出获得的CNY金额 (卖出价格 * USDT数量) - 截断到小数点后2位
            val sellAmountCNY = sellPrice.multiply(buyAmountUSD).setScale(2, RoundingMode.DOWN)
            
            // 计算网络手续费对应的CNY价值 (手续费USDT * 卖出价格) - 截断到小数点后2位
            val feeInCNY = networkFee.multiply(sellPrice).setScale(2, RoundingMode.DOWN)
            
            // 计算盈亏 (卖出获得的CNY - 买入花费的CNY - 手续费)
            val profitOrLoss = sellAmountCNY.subtract(buyAmountCNY).subtract(feeInCNY)
            
            // 格式化显示结果
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