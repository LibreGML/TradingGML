package tz.yx.gml.homefrag

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tz.yx.gml.R
import tz.yx.gml.databinding.FragmentProfitBinding
import java.math.BigDecimal
import java.math.RoundingMode

class ProfitFragment : Fragment() {
    
    private var _binding: FragmentProfitBinding? = null
    private val binding get() = _binding!!
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfitBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
    }
    
    private fun setupInputListeners() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = calculateProfit()
        }
        
        with(binding) {
            contractSize.addTextChangedListener(watcher)
            quantity.addTextChangedListener(watcher)
            lever.addTextChangedListener(watcher)
            entryPrice.addTextChangedListener(watcher)
            exitPrice.addTextChangedListener(watcher)
            positionType.addOnButtonCheckedListener { _, _, _ -> calculateProfit() }
        }
    }
    
    private fun calculateProfit() {
        try {
            val contractSize = parseInput(binding.contractSize.text.toString())
            val quantity = parseInput(binding.quantity.text.toString())
            val lever = parseInput(binding.lever.text.toString()).takeIf { it != BigDecimal.ZERO } ?: BigDecimal.ONE
            val entryPrice = parseInput(binding.entryPrice.text.toString())
            val exitPrice = parseInput(binding.exitPrice.text.toString())
            
            val isBuy = binding.positionType.checkedButtonId != R.id.sellButton
            
            val deposit = contractSize.multiply(entryPrice).multiply(quantity).divide(lever, 2, RoundingMode.HALF_UP)
            val priceDiff = if (isBuy) exitPrice.subtract(entryPrice) else entryPrice.subtract(exitPrice)
            val profit = contractSize.multiply(quantity).multiply(priceDiff).setScale(2, RoundingMode.HALF_UP)
            val roi = if (deposit != BigDecimal.ZERO) {
                profit.divide(deposit, 4, RoundingMode.HALF_UP).multiply(BigDecimal("100")).setScale(2, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            
            with(binding) {
                "$deposit USD".also { marginValue.text = it }
                "$profit USD".also { profitValue.text = it }
                "$roi%".also { roiValue.text = it }
            }
        } catch (e: Exception) {
            with(binding) {
                "USD".also { marginValue.text = it }
                "USD".also { profitValue.text = it }
                "%".also { roiValue.text = it }
            }
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