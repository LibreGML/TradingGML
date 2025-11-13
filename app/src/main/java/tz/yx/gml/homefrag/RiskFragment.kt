package tz.yx.gml.homefrag

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tz.yx.gml.R
import tz.yx.gml.databinding.FragmentRiskBinding
import java.math.BigDecimal
import java.math.RoundingMode

class RiskFragment : Fragment() {
    
    private var _binding: FragmentRiskBinding? = null
    private val binding get() = _binding!!
    
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
        setupInputListeners()
    }
    
    private fun setupInputListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = calculateRisk()
        }
        
        with(binding) {
            mymoney.addTextChangedListener(watcher)
            acceptlose.addTextChangedListener(watcher)
            contractSize.addTextChangedListener(watcher)
            lever.addTextChangedListener(watcher)
            entryPrice.addTextChangedListener(watcher)
            stopprice.addTextChangedListener(watcher)
            positionType.addOnButtonCheckedListener { _, _, _ -> calculateRisk() }
        }
    }
    
    private fun calculateRisk() {
        try {
            val mymoney = parseInput(binding.mymoney.text.toString())
            val acceptlose = parseInput(binding.acceptlose.text.toString())
            val contractSize = parseInput(binding.contractSize.text.toString())
            val lever = parseInput(binding.lever.text.toString())
            val entryPrice = parseInput(binding.entryPrice.text.toString())
            val stopprice = parseInput(binding.stopprice.text.toString())
            
            val isBuy = binding.positionType.checkedButtonId != R.id.sellButton
            
            // 计算价格风险
            val priceRisk = if (isBuy) {
                entryPrice.subtract(stopprice)
            } else {
                stopprice.subtract(entryPrice)
            }.abs()
            
            // 计算单手损失
            val oneHandLose = contractSize.multiply(priceRisk)
            
            // 如果价格风险为0，无法计算
            if (priceRisk.compareTo(BigDecimal.ZERO) == 0) {
                binding.shouldpos.text = "无法计算"
                return
            }
            
            // 推荐手数 = 可承受损失 / 单手损失
            val shouldPos = acceptlose.divide(oneHandLose, 4, RoundingMode.HALF_UP)
            
            // 保证金限制手数 = 账户余额 / ((开仓价 * 合约规模) / 杠杆)
            val marginLimit = if (lever.compareTo(BigDecimal.ZERO) > 0) {
                mymoney.multiply(lever).divide(entryPrice.multiply(contractSize), 4, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }
            
            // 取较小值作为最终推荐手数
            val finalPos = shouldPos.min(marginLimit)
            
            binding.shouldpos.text = "${finalPos.setScale(2, RoundingMode.HALF_UP)} 手"
        } catch (e: Exception) {
            binding.shouldpos.text = "计算错误"
        }
    }
    
    private fun parseInput(value: String): BigDecimal = try {
        BigDecimal(value).setScale(4, RoundingMode.HALF_UP)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}