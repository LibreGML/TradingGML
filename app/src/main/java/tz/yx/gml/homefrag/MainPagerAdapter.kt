package tz.yx.gml.homefrag

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 4
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProfitFragment()
            1 -> RiskFragment()
            2 -> UsdtFragment()
            3 -> StockFragment()
            else -> ProfitFragment()
        }
    }
}