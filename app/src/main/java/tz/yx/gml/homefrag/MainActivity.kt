package tz.yx.gml.homefrag

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textview.MaterialTextView
import tz.yx.gml.plugins.BirdActivity
import tz.yx.gml.note.NoteActivity
import tz.yx.gml.R
import tz.yx.gml.databinding.ActivityMainBinding
import tz.yx.gml.passwd.PasswdActivity
import tz.yx.gml.plugins.FakeActivity
import tz.yx.gml.utils.FingerprintManager
import java.lang.reflect.Field

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var fingerprintManager: FingerprintManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        fingerprintManager = FingerprintManager(this)

        setupViewPager()
        setupMenu()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is android.widget.EditText) {
                val outRect = android.graphics.Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }


private fun setupViewPager() {
    val adapter = MainPagerAdapter(this)
    binding.viewPager.adapter = adapter
    binding.viewPager.setPageTransformer { page, position ->
        page.apply {
            translationX = -position * width
            alpha = 1 - kotlin.math.abs(position)
            scaleX = 0.7f + 0.3f * (1 - kotlin.math.abs(position))
            scaleY = 0.7f + 0.3f * (1 - kotlin.math.abs(position))
        }
    }
    try {
        val recyclerView = binding.viewPager.getChildAt(0)
        val recyclerViewClass = recyclerView.javaClass
        val layoutManager = recyclerViewClass.getDeclaredField("mLayoutManager")
        layoutManager.isAccessible = true
        val viewPagerClass = binding.viewPager.javaClass.superclass
        val smoothScrollField = viewPagerClass.getDeclaredField("mSmoothScrollDuration")
        smoothScrollField.isAccessible = true
        smoothScrollField.set(binding.viewPager, 4000)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
        when (position) {
            0 -> tab.text = "合约收益"
            1 -> tab.text = "风险控制"
            2 -> tab.text = "USDT交易"
            3 -> tab.text = "股票补仓"
        }
    }.attach()
}



    // MARK: - 菜单设置
    // ==================================================================================================

    private fun setupMenu() {
        binding.homeMenu.setOnClickListener { showMenu(it) }
        binding.yx.setOnClickListener { showAboutBottomSheet() }
        binding.fake.setOnClickListener { toFakePage() }
    }


    private fun showMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menuhome, menu)
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_rule -> {
                        toRulePage()
                        true
                    }
                    R.id.action_note -> {
                        toNotePage()
                         true
                    }
                    R.id.bird -> {
                        toBirdPage()
                        true
                    }
                    R.id.password ->{
                        toPasswdPage()
                        true
                    }
                    R.id.setting -> {
                        showSetting()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }



    private fun toNotePage() { startActivity(Intent(this, NoteActivity::class.java)) }

    private fun toRulePage() {
        val intent = Intent(this, RuleActivity::class.java)
        val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(this)
        startActivity(intent, options.toBundle())
    }

    private fun toBirdPage() { startActivity(Intent(this, BirdActivity::class.java)) }

    private fun toPasswdPage() { startActivity(Intent(this, PasswdActivity::class.java)) }


    private fun showAboutBottomSheet() {
        BottomSheetDialog(this).apply {
            setContentView(layoutInflater.inflate(R.layout.about, null))
            show()
        }
    }

    private fun toFakePage() { startActivity(Intent(this, FakeActivity::class.java)) }



    private fun showSetting() {
        val view = layoutInflater.inflate(R.layout.setting, null)
        MaterialAlertDialogBuilder(this)
            .setView(view)
            .show()

        val passwordFingerprintSwitch = view.findViewById<SwitchMaterial>(R.id.password_fingerprint_switch)
        val noteFingerprintSwitch = view.findViewById<SwitchMaterial>(R.id.note_fingerprint_switch)

        passwordFingerprintSwitch.isChecked = sharedPreferences.getBoolean("password_fingerprint_enabled", false)
        noteFingerprintSwitch.isChecked = sharedPreferences.getBoolean("note_fingerprint_enabled", false)

        if (!fingerprintManager.isFingerprintAvailable()) {
            passwordFingerprintSwitch.isEnabled = false
            noteFingerprintSwitch.isEnabled = false
            // 显示提示信息
            Toast.makeText(this, "您的设备不支持指纹哦~", Toast.LENGTH_SHORT).show()
        }

        passwordFingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!fingerprintManager.isFingerprintAvailable() && isChecked) {
                passwordFingerprintSwitch.isChecked = false
                Toast.makeText(this, "您的设备不支持指纹哦~", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            
            with(sharedPreferences.edit()) {
                putBoolean("password_fingerprint_enabled", isChecked)
                apply()
            }
        }

        noteFingerprintSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!fingerprintManager.isFingerprintAvailable() && isChecked) {
                noteFingerprintSwitch.isChecked = false
                Toast.makeText(this, "您的设备不支持指纹哦~", Toast.LENGTH_SHORT).show()
                return@setOnCheckedChangeListener
            }
            
            with(sharedPreferences.edit()) {
                putBoolean("note_fingerprint_enabled", isChecked)
                apply()
            }
        }

    }


}

