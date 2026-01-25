package tz.yx.gml.homefrag

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import tz.yx.gml.R
import tz.yx.gml.databinding.ActivityRuleBinding
import tz.yx.gml.utils.DataExportImportUtils
import kotlin.random.Random


class YxActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRuleBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var dataExportImportUtils: DataExportImportUtils
    private lateinit var importFileLauncher: ActivityResultLauncher<String>

    // 添加货币类型枚举
    companion object {
        const val CURRENCY_YUAN_PANG = "yuan_pang"
        const val CURRENCY_CNY = "cny"
    }

    // 判断是否为双倍莹钞日（每月1日）
    private fun isDoubleYuanPangDay(): Boolean {
        return try {
            val dayOfMonth =
                android.text.format.DateFormat.format("dd", java.util.Date()).toString().toInt()
            dayOfMonth == 1
        } catch (e: Exception) {
            false // 如果日期解析失败，默认不是双倍日
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityRuleBinding.inflate(layoutInflater)
            setContentView(binding.root)

            enableEdgeToEdge()
            ViewCompat.setOnApplyWindowInsetsListener(binding.rule) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }


            // 初始化文件选择器
            importFileLauncher =
                registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                    uri?.let { selectedUri ->
                        handleImportFile(selectedUri)
                    }
                }

            // 初始化SharedPreferences
            sharedPreferences = getSharedPreferences("YxData", MODE_PRIVATE)
            dataExportImportUtils = DataExportImportUtils(this)


            // 加载数据
            loadData()

            // 设置监听器
            setupListeners()

            // 设置法令条目点击事件
            setupOrdinanceClickEvents()

            // 设置奖赏按钮点击事件
            setupRewardClickEvents()

            // 初始化法令状态
            updateOrdinanceStatus()

            // 更新莹历显示
            updateYingYearDisplay()
        } catch (e: Exception) {
            // 如果初始化失败，显示错误信息
            MaterialAlertDialogBuilder(this)
                .setTitle("初始化错误")
                .setMessage("应用初始化失败: ${e.message}")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun loadData() {
        val balance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)
        val penalties = sharedPreferences.getInt("penalty_count", 0)

        // 获取当前选择的货币类型，默认为莹钞
        val selectedCurrency = sharedPreferences.getString("selected_currency", CURRENCY_YUAN_PANG)
            ?: CURRENCY_YUAN_PANG

        // 根据选择的货币类型显示不同的数值和单位
        val displayBalance = when (selectedCurrency) {
            CURRENCY_CNY -> {
                val convertedBalance = (balance * 0.47).toInt()
                "$convertedBalance ¥"
            }

            else -> "$balance Ƶ"
        }

        val displayTotalEarned = when (selectedCurrency) {
            CURRENCY_CNY -> {
                val convertedTotal = (totalEarned * 0.47).toInt()
                "总计获得: $convertedTotal ¥"
            }

            else -> "总计获得: $totalEarned Ƶ"
        }

        binding.yuanPangBalance.typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
        binding.yuanPangBalance.text = displayBalance
        binding.totalYuanPang.text = displayTotalEarned
        binding.penaltyCount.text = "违法次数: $penalties 次"

        updateYingYearDisplay()
    }

    private fun setupListeners() {
        binding.walletname.setOnClickListener {
            showMyPicture()
        }

        binding.spendButton.setOnClickListener {
            showSpendDialog()
        }
        binding.addButton.setOnClickListener {
            showAddDialog()
        }
        binding.saveButton.setOnClickListener {
            showSavingsDialog()
        }
        // 添加查看储蓄记录的按钮监听器（长按原子图标查看储蓄记录）
        binding.saveButton.setOnLongClickListener {
            showSavingsRecords()
            true
        }
        // 为新添加的储蓄记录按钮设置点击事件
        binding.viewSavingsRecordsButton.setOnClickListener {
            showSavingsRecords()
        }

        // 添加货币选择按钮的点击事件
        binding.currencySelectorButton.setOnClickListener {
            showCurrencySelectorPopup(it)
        }

        // 添加清空记录按钮的点击事件
        binding.tozero.setOnClickListener {
            showClearAllRecordsDialog()
        }

        // 更新莹历显示
        updateYingYearDisplay()

        // 添加违法历史按钮的点击事件
        binding.banhistory.setOnClickListener {
            showBanHistoryDialog()
        }

        // 添加导出导入按钮的点击事件
        binding.exorimport.setOnClickListener {
            showExportImportBottomSheet()
        }


    }

    private fun showCurrencySelectorPopup(anchorView: android.view.View) {
        val popupMenu = android.widget.PopupMenu(this, anchorView)

        // 添加菜单项
        val yuanPangItem = popupMenu.menu.add("莹钞 (Ƶ)")
        val cnyItem = popupMenu.menu.add("人民币 (¥)")

        // 设置点击监听器
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.title.toString()) {
                "莹钞 (Ƶ)" -> {
                    updateCurrencyDisplay(CURRENCY_YUAN_PANG)
                    true
                }

                "人民币 (¥)" -> {
                    updateCurrencyDisplay(CURRENCY_CNY)
                    true
                }

                else -> false
            }
        }

        popupMenu.show()
    }

    private fun updateCurrencyDisplay(currencyType: String) {
        // 保存当前选择的货币类型
        val editor = sharedPreferences.edit()
        editor.putString("selected_currency", currencyType)
        editor.apply()

        // 重新加载数据显示
        loadData()
    }

    private fun setupOrdinanceClickEvents() {
        // 创建法令条目
        createOrdinanceItems()
    }

    private fun updateOrdinanceStatus() {
        // 重新创建法令条目以反映最新状态
        createOrdinanceItems()
    }

    private fun toggleOrdinanceViolation(
        ordinanceNumber: Int,
        textView: android.widget.TextView,
        statusIcon: android.widget.ImageView
    ) {
        // 检查当前状态
        val currentViolatedOrdinances =
            sharedPreferences.getStringSet("violated_ordinances", mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        val ordinanceKey = "ordinance_$ordinanceNumber"

        if (currentViolatedOrdinances.contains(ordinanceKey)) {
            // 如果已违法，询问是否取消违法
            showCancelViolationConfirmation(ordinanceNumber, textView, statusIcon)
        } else {
            // 如果未违法，标记为违法
            showViolationConfirmation(ordinanceNumber, textView, statusIcon)
        }
    }

    private fun showCancelViolationConfirmation(
        ordinanceNumber: Int,
        textView: android.widget.TextView,
        statusIcon: android.widget.ImageView
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle("取消违法标记")
            .setMessage("您已完成惩罚，是否取消此法令的违法标记？")
            .setPositiveButton("确认取消") { _, _ ->
                cancelViolation(ordinanceNumber, textView, statusIcon)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun cancelViolation(
        ordinanceNumber: Int,
        textView: android.widget.TextView,
        statusIcon: android.widget.ImageView
    ) {
        // 恢复UI - 将文本颜色改回正常，状态图标改为绿色
        statusIcon.setImageResource(R.drawable.ic_check_circle_outline) // 使用对勾图标
        statusIcon.setColorFilter(Color.parseColor("#388E3C"))
        textView.setTextColor(getColor(android.R.color.secondary_text_light))

        // 更新数据 - 仅移除违法记录，不改变违法次数
        val violatedOrdinances =
            sharedPreferences.getStringSet("violated_ordinances", mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        violatedOrdinances.remove("ordinance_$ordinanceNumber")

        val editor = sharedPreferences.edit()
        editor.putStringSet("violated_ordinances", violatedOrdinances)

        // 更新详细违法记录的状态为已完成
        val detailedViolationKey = "violation_detail_$ordinanceNumber"
        val violationDetail = sharedPreferences.getString(detailedViolationKey, null)
        if (violationDetail != null) {
            val parts = violationDetail.split(":")
            if (parts.size >= 2) {
                val timestamp = parts[0]
                val updatedDetail = "$timestamp:completed" // completed表示已完成处罚
                editor.putString(detailedViolationKey, updatedDetail)
            }
        }

        editor.apply()

        loadData()
        updateOrdinanceStatus() // 更新所有状态
    }

    private fun showViolationConfirmation(
        ordinanceNumber: Int,
        textView: android.widget.TextView,
        statusIcon: android.widget.ImageView
    ) {
        MaterialAlertDialogBuilder(this)
            .setTitle("确认违法")
            .setMessage("您确定要标记此法令违法吗？法令编号：$ordinanceNumber")
            .setPositiveButton("确认") { _, _ ->
                recordViolation(ordinanceNumber, textView, statusIcon)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun recordViolation(
        ordinanceNumber: Int,
        textView: android.widget.TextView,
        statusIcon: android.widget.ImageView
    ) {
        // 更新UI - 将状态图标改为红色，文本颜色改为红色
        statusIcon.setImageResource(R.drawable.ic_cancel) // 使用取消图标
        statusIcon.setColorFilter(Color.parseColor("#D32F2F"))
        textView.setTextColor(Color.parseColor("#D32F2F"))

        // 更新数据
        val currentPenalties = sharedPreferences.getInt("penalty_count", 0)

        val editor = sharedPreferences.edit()
        editor.putInt("penalty_count", currentPenalties + 1)

        // 保存违法记录，包含违法时间戳
        val violatedOrdinances =
            sharedPreferences.getStringSet("violated_ordinances", mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        violatedOrdinances.add("ordinance_$ordinanceNumber")
        editor.putStringSet("violated_ordinances", violatedOrdinances)

        // 保存详细的违法记录，包括时间戳和处罚状态
        val detailedViolationKey = "violation_detail_$ordinanceNumber"
        val violationDetail = "${System.currentTimeMillis()}:pending" // pending表示待完成处罚
        editor.putString(detailedViolationKey, violationDetail)

        // 记录违法时间
        editor.putLong("last_violation_time", System.currentTimeMillis())

        editor.apply()

        loadData()
        updateOrdinanceStatus() // 更新所有状态
    }

    private fun setupRewardClickEvents() {
        // 固定奖励
        binding.claimFixedReward.setOnClickListener {
            claimFixedReward()
        }

        // 无违法奖励
        binding.claimNoViolationReward.setOnClickListener {
            claimNoViolationReward()
        }

        // 全年无违法奖励
        binding.claimAnnualNoViolationReward.setOnClickListener {
            claimAnnualNoViolationReward()
        }

        // 储蓄率奖励
        binding.claimSavingReward.setOnClickListener {
            claimSavingReward()
        }

        // 禁欲奖励
        binding.claimAbstinenceReward.setOnClickListener {
            claimAbstinenceReward()
        }

        // 健身奖励
        binding.claimFitnessReward.setOnClickListener {
            claimFitnessReward()
        }

        // 读书奖励
        binding.claimReadingReward.setOnClickListener {
            claimReadingReward()
        }

        // 学习奖励
        binding.claimLearningReward.setOnClickListener {
            claimLearningReward()
        }

        // 项目奖励
        binding.claimProjectReward.setOnClickListener {
            claimProjectReward()
        }

        // 认证奖励
        binding.claimCertificationReward.setOnClickListener {
            claimCertificationReward()
        }

        // 数字产品奖励
        binding.claimDigitalProductReward.setOnClickListener {
            claimDigitalProductReward()
        }

        // 博弈奖励
        binding.claimGamblingReward.setOnClickListener {
            claimGamblingReward()
        }

        // 双倍奖励确认
        binding.claimDoubleReward.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("提示")
                .setMessage(if (isDoubleYuanPangDay()) "（今日为双倍日）" else "今天不是双倍日")
                .setPositiveButton("确定", null)
                .show()
        }

        // 彩蛋任务奖励
        binding.claimEggReward.setOnClickListener {
            claimEggReward()
        }

        // 抚慰金奖励
        binding.claimComfortReward.setOnClickListener {
            claimComfortReward()
        }

        // 增加收入来源奖励
        binding.claimIncomeSourceReward.setOnClickListener {
            claimIncomeSourceReward()
        }

        // 小G财指突破奖励
        binding.claimWealthIndexReward.setOnClickListener {
            claimWealthIndexReward()
        }
    }

    // 添加缺失的储蓄率奖励方法
    private fun claimSavingReward() {
        val input = TextInputEditText(this)
        input.hint = "请输入当月储蓄率 (%)"

        val inputLayout = TextInputLayout(this).apply {
            addView(input)
            hint = "储蓄率"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("申报储蓄率奖励")
            .setView(layout)
            .setPositiveButton("确认") { _, _ ->
                val rateStr = input.text.toString()
                if (rateStr.isNotEmpty()) {
                    val rate = try {
                        rateStr.toDouble()
                    } catch (e: NumberFormatException) {
                        0.0
                    }

                    if (rate >= 71.0) { // 达到71%以上
                        val rewardAmount = if (isDoubleYuanPangDay()) 64 * 2 else 64 // 双倍莹钞日翻倍

                        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
                        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

                        val editor = sharedPreferences.edit()
                        editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
                        editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
                        editor.apply()

                        loadData()

                        MaterialAlertDialogBuilder(this)
                            .setTitle("奖励申领成功")
                            .setMessage("储蓄率奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                            .setPositiveButton("确定", null)
                            .show()
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("无法申领")
                            .setMessage("当月储蓄率未达到71%，无法申领奖励")
                            .setPositiveButton("确定", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 添加缺失的读书奖励方法
    private fun claimReadingReward() {
        val input = TextInputEditText(this)
        input.hint = "请输入书籍价格或阅读奖励基础值"

        val inputLayout = TextInputLayout(this).apply {
            addView(input)
            hint = "书籍价格(莹钞)"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("申报读书奖励")
            .setView(layout)
            .setPositiveButton("确认") { _, _ ->
                val priceStr = input.text.toString()
                if (priceStr.isNotEmpty()) {
                    val price = try {
                        priceStr.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }

                    var rewardAmount = if (isDoubleYuanPangDay()) price * 2 else price // 奖励等同书价莹钞

                    // 询问是否属于反迷信、哲学等类别，额外+30莹钞
                    MaterialAlertDialogBuilder(this)
                        .setTitle("奖励确认")
                        .setMessage("是否为反迷信、哲学等类别？如果是，将额外奖励30莹钞")
                        .setPositiveButton("是，额外+30") { _, _ ->
                            rewardAmount += if (isDoubleYuanPangDay()) 30 * 2 else 30

                            val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
                            val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

                            val editor = sharedPreferences.edit()
                            editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
                            editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
                            editor.apply()

                            loadData()

                            MaterialAlertDialogBuilder(this)
                                .setTitle("奖励申领成功")
                                .setMessage("读书奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                                .setPositiveButton("确定", null)
                                .show()
                        }
                        .setNegativeButton("否") { _, _ ->
                            val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
                            val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

                            val editor = sharedPreferences.edit()
                            editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
                            editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
                            editor.apply()

                            loadData()

                            MaterialAlertDialogBuilder(this)
                                .setTitle("奖励申领成功")
                                .setMessage("读书奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                                .setPositiveButton("确定", null)
                                .show()
                        }
                        .show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 添加缺失的项目奖励方法
    private fun claimProjectReward() {
        val input = TextInputEditText(this)
        input.hint = "请输入项目净利润"

        val inputLayout = TextInputLayout(this).apply {
            addView(input)
            hint = "项目净利润(莹钞)"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("申报项目奖励")
            .setView(layout)
            .setPositiveButton("确认") { _, _ ->
                val profitStr = input.text.toString()
                if (profitStr.isNotEmpty()) {
                    val profit = try {
                        profitStr.toInt()
                    } catch (e: NumberFormatException) {
                        0
                    }

                    val rewardAmountBase = try {
                        (profit * 0.05).toInt() // 净利润的5%
                    } catch (e: ArithmeticException) {
                        0
                    }
                    val rewardAmount =
                        if (isDoubleYuanPangDay()) rewardAmountBase * 2 else rewardAmountBase

                    val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
                    val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

                    val editor = sharedPreferences.edit()
                    editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
                    editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
                    editor.apply()

                    loadData()

                    MaterialAlertDialogBuilder(this)
                        .setTitle("奖励申领成功")
                        .setMessage("项目奖励 $rewardAmount 莹钞已发放！（基于净利润 $profit 的5%）" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createOrdinanceItems() {
        // 清空现有条目
        binding.ordinanceContainer.removeAllViews()

        // 法令内容数组
        val ordinances = listOf(
            "• 誓死捍卫自己的基本人权与自由！自己拥有对自我命运的绝对主权！",
            "• 本人对自身婚姻事务拥有绝对且排他的最终决定权。是否结婚、与谁结婚、何时结婚均由本人独立判断。",
            "• 严禁一切形式的占卜、算命、宗教及封建迷信活动与信仰。违者罚跑800米一圈，20个引体向上，罚款1000元。",
            "• 坚持丁克主义，永久性禁止生育后代，违者罚款200000元，罚跑2次1200米。",
            "• 永久性彻底禁止一切投机与赌博行为，严禁交易个股、期货、期权、外汇、加密货币等。违者罚跑2次1200米，2组20个引体向上。",
            "• 严禁购买任何加密货币、稳定币、NFT等链上资产。违者罚跑2次1200米，3组20个引体向上。",
            "• 严禁以任何形式为任何第三方的债务提供担保、增信或承诺承担连带责任。违者罚跑1次1200米，2组20个引体向上。",
            "• 原则上严禁借入资金，借入金额不得超过上月基本生活费的两倍。违者罚跑1次1200米。",
            "• 原则上严禁借出资金，单笔出借金额超过自身月收入的2倍，必须严格遵循《GML借款合同框架》。违者罚跑800米一圈，10个引体向上。",
            "• 严禁将生活费及预留的应急资金用于任何风险投资、交易、出借。违者罚跑2次1200米，2组20个引体向上。",
            "• 坚决维护个人财产所有权及绝对处分权，禁止让渡于他人。违者罚跑1200米一圈，20个引体向上。",
            "• 严禁吸烟、吸毒。违者罚款10000元，罚跑2次1200米，3组20个引体向上。",
            "• 严禁在国内平台发表任何可能被认定为煽动分裂、颠覆或传播严重谣言的内容。违者罚款500元，强制断网5小时。",
            "• 严禁在婚姻关系存续期间出轨、嫖娼。违者罚跑2次1200米，2组20个引体向上，20个仰卧起坐，罚款10000元。",
            "• 禁止购买任何游戏内虚拟物品、代币或服务，即禁止游戏充值，违者罚跑800米，20个仰卧起坐。",
            "• 严禁在任何网络平台，进行打赏、刷礼物、充电等付费活动，违者罚跑1000米，40个仰卧起坐。"
        )

        val violatedOrdinances =
            sharedPreferences.getStringSet("violated_ordinances", mutableSetOf()) ?: mutableSetOf()

        // 为每个法令创建卡片视图
        for ((index, ordinanceText) in ordinances.withIndex()) {
            val cardView = createOrdinanceCard(
                ordinanceText,
                index + 1,
                violatedOrdinances.contains("ordinance_${index + 1}")
            )
            binding.ordinanceContainer.addView(cardView)
        }
    }

    private fun createOrdinanceCard(
        ordinanceText: String,
        ordinanceNumber: Int,
        isViolated: Boolean
    ): com.google.android.material.card.MaterialCardView {
        val cardView = com.google.android.material.card.MaterialCardView(this)
        val layoutParams = android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, dpToPx(12)) // 底部间距
        }
        cardView.layoutParams = layoutParams

        cardView.apply {
            radius = dpToPx(12).toFloat()  // 修复：转换为Float
            cardElevation = dpToPx(2).toFloat()  // 修复：转换为Float
            setCardBackgroundColor(
                if (isViolated) Color.parseColor("#FFCDD2") else Color.parseColor(
                    "#FFEBEE"
                )
            )
            strokeWidth = 1
            strokeColor = getColor(android.R.color.darker_gray)
            isClickable = true
            isFocusable = true
        }

        // 创建卡片内部的线性布局
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // 创建文本视图
        val textView = android.widget.TextView(this).apply {
            text = ordinanceText
            setTextColor(
                if (isViolated) Color.parseColor("#D32F2F") else getColor(android.R.color.secondary_text_light)
            )
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
            val textViewLayoutParams = android.widget.LinearLayout.LayoutParams(
                0,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
                setMargins(0, 0, dpToPx(16), 0)
            }
            this.layoutParams = textViewLayoutParams  // 修复：使用this.layoutParams而不是直接赋值给val变量
        }

        // 创建状态图标
        val statusIcon = android.widget.ImageView(this).apply {
            setImageResource(
                if (isViolated) R.drawable.ic_cancel else R.drawable.ic_check_circle_outline
            )
            setColorFilter(
                if (isViolated) Color.parseColor("#D32F2F") else Color.parseColor("#388E3C")
            )
            val iconLayoutParams = android.widget.LinearLayout.LayoutParams(dpToPx(28), dpToPx(28))
            this.layoutParams = iconLayoutParams  // 修复：使用this.layoutParams而不是直接赋值给val变量
        }

        // 添加点击事件
        val clickListener = android.view.View.OnClickListener {
            toggleOrdinanceViolation(ordinanceNumber, textView, statusIcon)
        }

        textView.setOnClickListener(clickListener)
        statusIcon.setOnClickListener(clickListener)
        cardView.setOnClickListener(clickListener)

        linearLayout.addView(textView)
        linearLayout.addView(statusIcon)
        cardView.addView(linearLayout)

        return cardView
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    private fun showAddDialog() {
        val input = TextInputEditText(this)
        input.hint = "输入收入金额"
        val inputLayout = TextInputLayout(this).apply {
            addView(input)
            setPadding(50, 50, 50, 0)
            hint = "收入金额"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("收入莹钞")
            .setView(inputLayout)
            .setPositiveButton("确认") { _, _ ->
                val amountStr = input.text.toString()
                if (amountStr.isNotEmpty()) {
                    val amount = try {
                        val value = amountStr.toInt()
                        // 确保金额为正数且在合理范围内
                        if (value <= 0) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("错误")
                                .setMessage("储蓄金额必须大于0")
                                .setPositiveButton("确定", null)
                                .show()
                            return@setPositiveButton
                        }
                        if (value > 1000000) { // 设置最大金额限制
                            MaterialAlertDialogBuilder(this)
                                .setTitle("错误")
                                .setMessage("单次储蓄金额不能超过1,000,000")
                                .setPositiveButton("确定", null)
                                .show()
                            return@setPositiveButton
                        }
                        value
                    } catch (e: NumberFormatException) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("错误")
                            .setMessage("请输入有效的储蓄金额")
                            .setPositiveButton("确定", null)
                            .show()
                        return@setPositiveButton
                    }

                    if (amount > 0) {
                        addYingPang(amount)
                    }
                }
            }
            .show()
    }


    private fun showMyPicture() {
        val gmlBottomSheet = layoutInflater.inflate(R.layout.gml_picture, null)
        val gmlbottomSheetDialog = BottomSheetDialog(this)
        gmlbottomSheetDialog.setContentView(gmlBottomSheet)
        gmlbottomSheetDialog.show()
    }

    private fun showSpendDialog() {
        val input = TextInputEditText(this)
        input.hint = "输入支出金额"

        val inputLayout = TextInputLayout(this).apply {
            addView(input)
            hint = "支出金额"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("支出莹钞")
            .setView(layout)
            .setPositiveButton("确认") { _, _ ->
                val amountStr = input.text.toString()
                if (amountStr.isNotEmpty()) {
                    val amount = try {
                        val value = amountStr.toInt()
                        // 确保金额为正数且在合理范围内
                        if (value <= 0) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("错误")
                                .setMessage("储蓄金额必须大于0")
                                .setPositiveButton("确定", null)
                                .show()
                            return@setPositiveButton
                        }
                        if (value > 1000000) { // 设置最大金额限制
                            MaterialAlertDialogBuilder(this)
                                .setTitle("错误")
                                .setMessage("单次储蓄金额不能超过1,000,000")
                                .setPositiveButton("确定", null)
                                .show()
                            return@setPositiveButton
                        }
                        value
                    } catch (e: NumberFormatException) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("错误")
                            .setMessage("请输入有效的储蓄金额")
                            .setPositiveButton("确定", null)
                            .show()
                        return@setPositiveButton
                    }

                    if (amount > 0) {
                        spendYuanPang(amount)
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 新增：储蓄对话框
    private fun showSavingsDialog() {
        val options =
            arrayOf("活期储蓄", "3个月定期", "6个月定期", "1年期定期", "3年期定期", "5年期定期")

        MaterialAlertDialogBuilder(this)
            .setTitle("选择储蓄方式")
            .setItems(options) { _, which ->
                val savingType = when (which) {
                    0 -> "current" // 活期
                    1 -> "three_months" // 3个月
                    2 -> "six_months" // 6个月
                    3 -> "one_year" // 1年
                    4 -> "three_years" // 3年
                    5 -> "five_years" // 5年
                    else -> "current"
                }

                showSavingsInputDialog(savingType)
            }
            .show()
    }

    private fun showSavingsInputDialog(savingType: String) {
        val input = TextInputEditText(this)
        input.hint = "输入储蓄金额"
        val cpiInput = TextInputEditText(this)
        cpiInput.hint = "输入月度CPI (%)"

        val inputLayout = TextInputLayout(this).apply {
            addView(input)
            hint = "储蓄金额"
        }

        val cpiLayout = TextInputLayout(this).apply {
            addView(cpiInput)
            hint = "月度CPI (%)"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 0)
            addView(inputLayout)
            addView(cpiLayout)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("储蓄详情")
            .setView(layout)
            .setPositiveButton("确认储蓄") { _, _ ->
                val amountStr = input.text.toString()
                val cpiStr = cpiInput.text.toString()

                if (amountStr.isNotEmpty() && cpiStr.isNotEmpty()) {
                    val amount = try {
                        val value = amountStr.toInt()
                        // 确保金额为正数且在合理范围内
                        if (value <= 0) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("错误")
                                .setMessage("储蓄金额必须大于0")
                                .setPositiveButton("确定", null)
                                .show()
                            return@setPositiveButton
                        }
                        if (value > 1000000) { // 设置最大金额限制
                            MaterialAlertDialogBuilder(this)
                                .setTitle("错误")
                                .setMessage("单次储蓄金额不能超过1,000,000")
                                .setPositiveButton("确定", null)
                                .show()
                            return@setPositiveButton
                        }
                        value
                    } catch (e: NumberFormatException) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("错误")
                            .setMessage("请输入有效的储蓄金额")
                            .setPositiveButton("确定", null)
                            .show()
                        return@setPositiveButton
                    }

                    val cpi = try {
                        val value = cpiStr.toDouble()
                        // 确保CPI值在合理范围内
                        if (value < -100.0 || value > 100.0) {
                            MaterialAlertDialogBuilder(this)
                                .setTitle("错误")
                                .setMessage("CPI值应在-100到100之间")
                                .setPositiveButton("确定", null)
                                .show()
                            return@setPositiveButton
                        }
                        value
                    } catch (e: NumberFormatException) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("错误")
                            .setMessage("请输入有效的CPI数值")
                            .setPositiveButton("确定", null)
                            .show()
                        return@setPositiveButton
                    }

                    if (amount > 0 && cpi >= 0) {
                        processSavings(amount, cpi, savingType)
                    } else {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("错误")
                            .setMessage("请输入有效的金额和CPI数值")
                            .setPositiveButton("确定", null)
                            .show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun processSavings(amount: Int, cpi: Double, savingType: String) {
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)

        if (currentBalance < amount) {
            MaterialAlertDialogBuilder(this)
                .setTitle("余额不足")
                .setMessage("当前莹钞余额不足，无法储蓄 $amount 莹钞")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        // 计算利率
        val baseRate = cpi + getSavingCoefficient(savingType)
        val interestRate = when (savingType) {
            "current" -> baseRate // 活期
            "three_months" -> baseRate + 0.95 // 3个月定期
            "six_months" -> baseRate + 1.15 // 6个月定期
            "one_year" -> baseRate + 1.25 // 1年期定期
            "three_years" -> baseRate + 1.75 // 3年期定期
            "five_years" -> baseRate + 1.80 // 5年期定期
            else -> baseRate
        }

        // 计算本息
        val months = when (savingType) {
            "current" -> 1 // 活期按月计算
            "three_months" -> 3
            "six_months" -> 6
            "one_year" -> 12
            "three_years" -> 36
            "five_years" -> 60
            else -> 1
        }

        val totalAmount = try {
            (amount * (1 + interestRate / 100 * months / 12)).toInt()
        } catch (e: Exception) {
            // 防止计算溢出或其他计算错误
            amount
        }

        // 从余额中扣除储蓄金额
        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance - amount)

        // 保存储蓄记录
        val savingsRecords =
            sharedPreferences.getStringSet("savings_records", mutableSetOf())?.toMutableSet()
                ?: mutableSetOf()
        val maturityTime = System.currentTimeMillis() + (months * 30L * 24 * 60 * 60 * 1000)
        // 确保时间不会溢出
        val record = if (maturityTime < System.currentTimeMillis()) {
            // 如果计算出的时间小于当前时间（说明溢出了），使用一个合理的远期时间
            "${System.currentTimeMillis()}:${amount}:${savingType}:${cpi}:${totalAmount}:${System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000}:${interestRate}"
        } else {
            "${System.currentTimeMillis()}:${amount}:${savingType}:${cpi}:${totalAmount}:${maturityTime}:${interestRate}"
        }
        savingsRecords.add(record)
        editor.putStringSet("savings_records", savingsRecords)

        editor.apply()

        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle("储蓄成功")
            .setMessage(
                "已储蓄 $amount 莹钞，类型: ${getSavingTypeName(savingType)}，预计到期本息: $totalAmount 莹钞，利率: ${
                    String.format(
                        "%.2f",
                        interestRate
                    )
                }%"
            )
            .setPositiveButton("确定", null)
            .show()
    }

    private fun getSavingCoefficient(savingType: String): Double {
        // 根据储蓄类型返回储蓄系数，范围 [-1%, 1%]
        // 简化逻辑，可以根据实际情况调整
        return when (savingType.lowercase()) { // 使用lowercase()提高容错性
            "current" -> 0.0 // 活期
            "three_months" -> 0.2 // 3个月
            "six_months" -> 0.4 // 6个月
            "one_year" -> 0.6 // 1年
            "three_years" -> 0.8 // 3年
            "five_years" -> 1.0 // 5年
            else -> 0.0
        }
    }

    private fun getSavingTypeName(savingType: String): String {
        return when (savingType.lowercase()) {
            "current" -> "活期储蓄"
            "three_months" -> "3个月定期"
            "six_months" -> "6个月定期"
            "one_year" -> "1年期定期"
            "three_years" -> "3年期定期"
            "five_years" -> "5年期定期"
            else -> "活期储蓄"
        }
    }

    private fun spendYuanPang(amount: Int) {
        // 验证金额有效性
        if (amount <= 0) {
            MaterialAlertDialogBuilder(this)
                .setTitle("错误")
                .setMessage("支出金额必须大于0")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        if (amount > 1000000) { // 设置最大支出限制
            MaterialAlertDialogBuilder(this)
                .setTitle("错误")
                .setMessage("单次支出金额不能超过1,000,000")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        if (currentBalance >= amount) {
            val editor = sharedPreferences.edit()
            editor.putInt("yuan_pang_balance", currentBalance - amount)
            editor.apply()

            loadData()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("余额不足")
                .setMessage("当前莹钞余额不足，无法支出 $amount 莹钞")
                .setPositiveButton("确定", null)
                .show()
        }
    }


    private fun addYingPang(amount: Int) {
        // 验证金额有效性
        if (amount <= 0) {
            MaterialAlertDialogBuilder(this)
                .setTitle("错误")
                .setMessage("收入金额必须大于0")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        if (amount > 1000000) { // 设置最大收入限制
            MaterialAlertDialogBuilder(this)
                .setTitle("错误")
                .setMessage("单次收入金额不能超过1,000,000")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val editor = sharedPreferences.edit()
        // 防止整数溢出
        val newBalance = try {
            Math.addExact(currentBalance, amount)
        } catch (e: ArithmeticException) {
            Int.MAX_VALUE // 如果相加会溢出，则设置为最大值
        }
        editor.putInt("yuan_pang_balance", newBalance)
        editor.apply()
        loadData()
    }

    private fun claimFixedReward() {
        // 检查是否在2月或10月
        val currentMonth = try {
            android.text.format.DateFormat.format("MM", java.util.Date()).toString().toInt()
        } catch (e: Exception) {
            0 // 如果解析失败，返回默认值
        }
        val currentYear = try {
            android.text.format.DateFormat.format("yyyy", java.util.Date()).toString().toInt()
        } catch (e: Exception) {
            0 // 如果解析失败，返回默认值
        }

        // 检查是否是2月或10月，并且当年还没有领取过
        val lastClaimYear = sharedPreferences.getInt("last_fixed_reward_year", 0)
        val lastClaimMonth = sharedPreferences.getInt("last_fixed_reward_month", 0)

        if ((currentMonth == 2 || currentMonth == 10) && (lastClaimYear != currentYear || lastClaimMonth != currentMonth)) {
            val rewardAmount = if (isDoubleYuanPangDay()) 106 * 2 else 106 // 双倍莹钞日翻倍
            val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
            val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

            val editor = sharedPreferences.edit()
            editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
            editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
            editor.putInt("last_fixed_reward_year", currentYear)
            editor.putInt("last_fixed_reward_month", currentMonth)
            editor.apply()

            loadData()

            MaterialAlertDialogBuilder(this)
                .setTitle("奖励申领成功")
                .setMessage("已获得 $rewardAmount 莹钞奖励！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                .setPositiveButton("确定", null)
                .show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("提示")
                .setMessage("只有在2月和10月才能领取此奖励" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun claimNoViolationReward() {
        // 检查最近一个月是否有违法记录
        // 获取最后违法时间，兼容Integer和Long类型
        val lastViolationTimeValue = sharedPreferences.getAll()["last_violation_time"]
        val lastViolationTime = when (lastViolationTimeValue) {
            is Long -> lastViolationTimeValue
            is Int -> lastViolationTimeValue.toLong()
            else -> 0L
        }
        val currentTime = System.currentTimeMillis()
        val monthInMillis = 30L * 24 * 60 * 60 * 1000 // 粗略一个月的毫秒数

        if (currentTime - lastViolationTime >= monthInMillis) {
            val rewardAmount = if (isDoubleYuanPangDay()) 32 * 2 else 32 // 双倍莹钞日翻倍
            val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
            val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

            val editor = sharedPreferences.edit()
            editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
            editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
            // 更新最后违法时间为当前时间，以便后续计算
            editor.putLong("last_violation_time", currentTime)
            editor.apply()

            loadData()

            MaterialAlertDialogBuilder(this)
                .setTitle("奖励申领成功")
                .setMessage("无违法奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                .setPositiveButton("确定", null)
                .show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("无法申领")
                .setMessage("近一个月内有违法记录，暂无法申领无违法奖励")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun claimAnnualNoViolationReward() {
        // 检查去年是否有违法记录
        // 获取最后违法时间，兼容Integer和Long类型
        val lastYearViolationTimeValue = sharedPreferences.getAll()["last_violation_time"]
        val lastYearViolationTime = when (lastYearViolationTimeValue) {
            is Long -> lastYearViolationTimeValue
            is Int -> lastYearViolationTimeValue.toLong()
            else -> 0L
        }
        val currentTime = System.currentTimeMillis()
        val yearInMillis = 365L * 24 * 60 * 60 * 1000 // 粗略一年的毫秒数

        // 简化逻辑：检查过去一年是否有违法
        if (currentTime - lastYearViolationTime >= yearInMillis) {
            val rewardAmount = if (isDoubleYuanPangDay()) 213 * 2 else 213 // 双倍莹钞日翻倍
            val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
            val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

            val editor = sharedPreferences.edit()
            editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
            editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
            editor.apply()

            loadData()

            MaterialAlertDialogBuilder(this)
                .setTitle("奖励申领成功")
                .setMessage("年度无违法奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                .setPositiveButton("确定", null)
                .show()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("无法申领")
                .setMessage("过去一年内有违法记录，暂无法申领年度奖励")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    private fun claimAbstinenceReward() {
        // 获取当前周期进度和上次打卡时间
        val currentCycleProgress = sharedPreferences.getInt("abstinence_cycle_progress", 0)
        // 获取上次禁欲打卡日期，兼容Integer和Long类型
        val lastAbstinenceDateValue = sharedPreferences.getAll()["last_abstinence_date"]
        val lastAbstinenceDate = when (lastAbstinenceDateValue) {
            is Long -> lastAbstinenceDateValue
            is Int -> lastAbstinenceDateValue.toLong()
            else -> 0L
        }
        
        // 获取当前日期
        val currentTime = System.currentTimeMillis()
        val currentDay = try {
            android.text.format.DateFormat.format("yyyyMMdd", java.util.Date(currentTime)).toString().toInt()
        } catch (e: Exception) {
            0 // 如果解析失败，返回默认值
        }
        
        // 获取上次打卡日期
        val lastDay = try {
            if (lastAbstinenceDate != 0L) {
                android.text.format.DateFormat.format("yyyyMMdd", java.util.Date(lastAbstinenceDate)).toString().toInt()
            } else {
                0  // 如果上次打卡时间是0，则认为没有打过卡
            }
        } catch (e: Exception) {
            0
        }
        
        // 检查是否今天已经打卡
        if (hasCheckedInToday()) {
            // 今天已经打卡，显示当前进度并提示已打卡
            val currentCycleProgressMod = currentCycleProgress % 35
            val actualDayInCycle = if (currentCycleProgress == 0) 0 else if (currentCycleProgressMod == 0 && currentCycleProgress > 0) 35 else currentCycleProgressMod
            val completedCycles = currentCycleProgress / 35
            
            val message = "今天已经打卡过了！\n\n" +
                    "当前进度：$actualDayInCycle/35天\n" +
                    "当前是第 ${completedCycles + 1} 个周期\n" +
                    "已完成周期：$completedCycles 个\n" +
                    if (actualDayInCycle > 0 && actualDayInCycle < 35) {
                        val daysLeft = 35 - actualDayInCycle
                        "距离完成还有：$daysLeft 天\n"
                    } else if (actualDayInCycle == 35) {
                        "距离完成还有：0 天\n"
                    } else {
                        "开始新的周期后，每天打卡积累进度\n"
                    } +
                    "继续保持，坚持就是胜利！"
            
            MaterialAlertDialogBuilder(this)
                .setTitle("今日已打卡")
                .setMessage(message)
                .setPositiveButton("重置周期") { _, _ ->
                    // 添加重置周期的确认对话框
                    MaterialAlertDialogBuilder(this)
                        .setTitle("确认重置周期")
                        .setMessage("您确定要重置禁欲周期吗？当前进度将会丢失，从第1天重新开始。")
                        .setPositiveButton("确认重置") { _, _ ->
                            resetAbstinenceCycle()
                        }
                        .setNegativeButton("取消", null)
                        .show()
                }
                .setNegativeButton("确定", null)
                .show()
            return
        }
        
        // 检查是否是第一次打卡
        val isFirstTime = (currentCycleProgress == 0 && lastAbstinenceDate == 0L)
        
        val dialogMessage = if (isFirstTime) {
            "您确定要开始禁欲周期吗？这是一个重要的承诺！\n\n开始后，您将努力完成35天的禁欲挑战。\n\n注意：每天只能成功打卡一次。"
        } else {
            val currentDayInCycle = if (currentCycleProgress % 35 == 0 && currentCycleProgress > 0) 35 else currentCycleProgress % 35
            val actualDayInCycle = if (currentDayInCycle == 0 && currentCycleProgress > 0) 35 else currentDayInCycle
            val daysRemaining = 35 - (currentCycleProgress % 35)
            "您确定要继续禁欲周期吗？\n\n当前进度：$actualDayInCycle/35天\n剩余：$daysRemaining 天\n\n注意：每天只能成功打卡一次。"
        }
        
        // 弹出确认对话框，让用户确认是否真正开始或继续禁欲打卡
        MaterialAlertDialogBuilder(this)
            .setTitle("确认禁欲打卡")
            .setMessage(dialogMessage)
            .setPositiveButton("确认打卡") { _, _ ->
                // 检查是否连续打卡
                val expectedNextDay = lastDay + 1
                val isConsecutive = (currentDay == expectedNextDay) || (lastDay == 0) // 如果是第一次打卡，则视为连续
                
                val newProgress = if (isConsecutive) {
                    currentCycleProgress + 1
                } else {
                    // 中断了，重置计数并结算之前已完成的完整周期奖励
                    val completedCycles = currentCycleProgress / 35
                    if (completedCycles > 0) {
                        // 计算当前奖励基数，然后反推出每个已完成周期的奖励
                        var accumulatedReward = 0
                        var cycleReward = 148 // 初始奖励
                        for (i in 1..completedCycles) {
                            if (i > 1) {
                                // 从第二个周期开始，每个周期奖励是前一个周期的1.1倍
                                cycleReward = try {
                                    (cycleReward * 1.1).toInt()
                                } catch (e: ArithmeticException) {
                                    Int.MAX_VALUE
                                    break
                                }
                            }
                            accumulatedReward += if (isDoubleYuanPangDay()) cycleReward * 2 else cycleReward
                        }
                                
                        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
                        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)
                                
                        val editor = sharedPreferences.edit()
                        editor.putInt("yuan_pang_balance", currentBalance + accumulatedReward)
                        editor.putInt("total_yuan_pang_earned", totalEarned + accumulatedReward)
                        editor.apply()
                                
                        loadData()
                    }
                    1 // 重新开始计数
                }
                
                // 检查是否完成了一个或多个完整的35天周期
                if (newProgress % 35 == 0) {
                    // 完成了一个或多个35天周期，给予奖励并开始下一个周期
                    completeAbstinenceCycle(newProgress)
                } else {
                    // 更新进度但不完成周期
                    val editor = sharedPreferences.edit()
                    editor.putInt("abstinence_cycle_progress", newProgress)
                    editor.putLong("last_abstinence_date", currentTime)
                    editor.apply()
                    
                    // 显示打卡成功信息
                    val currentCycleProgressMod = newProgress % 35
                    val actualDayInCycle = if (newProgress == 0) 0 else if (currentCycleProgressMod == 0 && newProgress > 0) 35 else currentCycleProgressMod
                    val completedCycles = newProgress / 35
                    
                    val message = "禁欲打卡成功！\n\n" +
                            "当前进度：$actualDayInCycle/35天\n" +
                            "当前是第 ${completedCycles + 1} 个周期\n" +
                            "已完成周期：$completedCycles 个\n" +
                            if (actualDayInCycle > 0 && actualDayInCycle < 35) {
                                val daysLeft = 35 - actualDayInCycle
                                "距离完成还有：$daysLeft 天\n"
                            } else if (actualDayInCycle == 35) {
                                "距离完成还有：0 天\n"
                            } else {
                                "开始新的周期后，每天打卡积累进度\n"
                            } +
                            "继续保持，坚持就是胜利！"
                    
                    MaterialAlertDialogBuilder(this)
                        .setTitle("打卡成功")
                        .setMessage(message)
                        .setPositiveButton("重置周期") { _, _ ->
                            // 添加重置周期的确认对话框
                            MaterialAlertDialogBuilder(this)
                                .setTitle("确认重置周期")
                                .setMessage("您确定要重置禁欲周期吗？当前进度将会丢失，从第1天重新开始。")
                                .setPositiveButton("确认重置") { _, _ ->
                                    resetAbstinenceCycle()
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                        .setNegativeButton("确定", null)
                        .show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun completeAbstinenceCycle(progress: Int) {
        // 计算完成的完整周期数量
        val completedCycles = progress / 35
        
        // 计算当前周期的奖励（基于当前的奖励基数）
        val currentCycleReward = calculateCurrentCycleReward()
        val totalReward = if (isDoubleYuanPangDay()) currentCycleReward * 2 else currentCycleReward
        
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)
        
        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + totalReward)
        editor.putInt("total_yuan_pang_earned", totalEarned + totalReward)
        
        // 更新下一周期的奖励基数（几何累进），将当前奖励基数乘以1.1
        val nextCycleReward = try {
            (currentCycleReward * 1.1).toInt()
        } catch (e: ArithmeticException) {
            Int.MAX_VALUE // 如果计算溢出，设置为最大值
        }
        editor.putInt("abstinence_base_reward", nextCycleReward)
        
        // 重置周期进度为剩余天数，如果恰好完成完整周期则重置为0
        val remainingDays = progress % 35
        editor.putInt("abstinence_cycle_progress", remainingDays)
        editor.putLong("last_abstinence_date", System.currentTimeMillis())
        
        editor.apply()
        
        loadData()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("周期完成奖励")
            .setMessage("恭喜完成禁欲周期！获得 $totalReward 莹钞奖励！（当前周期奖励基数：$currentCycleReward）" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun calculateCurrentCycleReward(): Int {
        // 获取当前周期的基础奖励，如果没有则使用初始值148
        return sharedPreferences.getInt("abstinence_base_reward", 148)
    }
    
    private fun settlePreviousCycleReward(previousProgress: Int) {
        // 计算之前完成的完整周期数量
        val completedCycles = previousProgress / 35
        if (completedCycles > 0) {
            // 计算每个周期的奖励，第一个周期是148，后续周期是前一个周期的1.1倍
            var accumulatedReward = 0
            var cycleReward = 148 // 初始奖励
            for (i in 1..completedCycles) {
                if (i > 1) {
                    // 从第二个周期开始，每个周期奖励是前一个周期的1.1倍
                    cycleReward = try {
                        (cycleReward * 1.1).toInt()
                    } catch (e: ArithmeticException) {
                        Int.MAX_VALUE
                        break
                    }
                }
                accumulatedReward += if (isDoubleYuanPangDay()) cycleReward * 2 else cycleReward
            }
            
            val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
            val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)
            
            val editor = sharedPreferences.edit()
            editor.putInt("yuan_pang_balance", currentBalance + accumulatedReward)
            editor.putInt("total_yuan_pang_earned", totalEarned + accumulatedReward)
            editor.apply()
            
            loadData()
        }
    }
    
    // 检查今天是否已经完成禁欲打卡
    private fun hasCheckedInToday(): Boolean {
        val lastAbstinenceDateValue = sharedPreferences.getAll()["last_abstinence_date"]
        val lastAbstinenceDate = when (lastAbstinenceDateValue) {
            is Long -> lastAbstinenceDateValue
            is Int -> lastAbstinenceDateValue.toLong()
            else -> 0L
        }
        
        val currentTime = System.currentTimeMillis()
        val currentDay = try {
            android.text.format.DateFormat.format("yyyyMMdd", java.util.Date(currentTime)).toString().toInt()
        } catch (e: Exception) {
            0 // 如果解析失败，返回默认值
        }
        
        val lastDay = try {
            if (lastAbstinenceDate != 0L) {
                android.text.format.DateFormat.format("yyyyMMdd", java.util.Date(lastAbstinenceDate)).toString().toInt()
            } else {
                0  // 如果上次打卡时间是0，则认为没有打过卡
            }
        } catch (e: Exception) {
            0
        }
        
        return currentDay == lastDay && currentDay != 0
    }
    
    private fun showAbstinenceProgressInfo(progress: Int) {
        val currentReward = calculateCurrentCycleReward()
        val doubleReward = if (isDoubleYuanPangDay()) currentReward * 2 else currentReward
        val nextCycleReward = try {
            (currentReward * 1.1).toInt()
        } catch (e: ArithmeticException) {
            Int.MAX_VALUE
        }
        
        val currentCycleProgress = progress % 35
        val completedCycles = progress / 35 // 已完成的完整周期数
        val daysLeft = 35 - currentCycleProgress // 当前周期剩余天数
        
        // 计算当前周期内的具体天数
        val currentCycleDay = if (currentCycleProgress == 0 && progress > 0) 35 else currentCycleProgress
        
        // 修正显示逻辑，确保正确显示当前进度
        val displayDay = if (progress == 0) 0 else if (currentCycleProgress == 0 && progress > 0) 35 else currentCycleProgress
        
        val message = "禁欲周期进度：$displayDay/35天\n" +
                "当前是第 ${completedCycles + 1} 个周期\n" +
                "已完成周期：$completedCycles 个\n" +
                if (displayDay > 0 && displayDay < 35) "距离完成还有：$daysLeft 天\n" else if (displayDay == 35) "距离完成还有：0 天\n" else "开始新的周期后，每天打卡积累进度\n" +
                "当前周期奖励：$currentReward 莹钞" + if (isDoubleYuanPangDay()) "（双倍莹钞日：$doubleReward 莹钞）" else "" +
                "\n下一周期奖励：$nextCycleReward 莹钞\n" +
                "成功完成一个周期后，奖励将几何累进至下一周期"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("禁欲周期进度")
            .setMessage(message)
            .setPositiveButton("重置周期") { _, _ ->
                // 添加重置周期的确认对话框
                MaterialAlertDialogBuilder(this)
                    .setTitle("确认重置周期")
                    .setMessage("您确定要重置禁欲周期吗？当前进度将会丢失，从第1天重新开始。")
                    .setPositiveButton("确认重置") { _, _ ->
                        resetAbstinenceCycle()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
            .setNegativeButton("确定", null)
            .show()
    }
    
    // 新增：重置禁欲周期
    private fun resetAbstinenceCycle() {
        val editor = sharedPreferences.edit()
        editor.putInt("abstinence_cycle_progress", 0)
        editor.putLong("last_abstinence_date", 0L) // 重置最后打卡日期
        editor.apply()
        
        MaterialAlertDialogBuilder(this)
            .setTitle("周期已重置")
            .setMessage("禁欲周期已重置，您可以随时重新开始新的周期。")
            .setPositiveButton("确定", null)
            .show()
    }
    
    // 新增：获取禁欲打卡状态信息
    private fun getAbstinenceStatusInfo(): String {
        val currentCycleProgress = sharedPreferences.getInt("abstinence_cycle_progress", 0)
        val completedCycles = currentCycleProgress / 35
        val currentCycleDay = if (currentCycleProgress % 35 == 0 && currentCycleProgress > 0) 35 else currentCycleProgress % 35
        val actualDayInCycle = if (currentCycleProgress == 0) 0 else if (currentCycleProgress % 35 == 0 && currentCycleProgress > 0) 35 else currentCycleProgress % 35
        
        return if (currentCycleProgress == 0) {
            "尚未开始禁欲周期"
        } else {
            "当前进度：$actualDayInCycle/35天，已完成 $completedCycles 个完整周期"
        }
    }

    private fun claimFitnessReward() {
        // 检查今天是否已经打卡
        val today = try {
            android.text.format.DateFormat.format("yyyyMMdd", java.util.Date()).toString().toLong()
        } catch (e: Exception) {
            0 // 如果解析失败，返回默认值
        }
        
        // 获取上次健身打卡日期，兼容Integer和Long类型
        val lastFitnessDateValue = sharedPreferences.getAll()["last_fitness_date"]
        val lastFitnessDate = when (lastFitnessDateValue) {
            is Long -> lastFitnessDateValue
            is Int -> lastFitnessDateValue.toLong()
            else -> 0L
        }
        
        // 获取上次打卡日期
        val lastDay = try {
            android.text.format.DateFormat.format("yyyyMMdd", java.util.Date(lastFitnessDate)).toString().toLong()
        } catch (e: Exception) {
            0L
        }
        
        if (today == lastDay) {
            // 今天已经打卡，显示当前连续打卡进度
            val currentStreak = sharedPreferences.getInt("fitness_streak", 0)
            MaterialAlertDialogBuilder(this)
                .setTitle("健身打卡")
                .setMessage("今天已经打卡过了，当前连续打卡：$currentStreak 天！")
                .setPositiveButton("确定", null)
                .show()
            return
        }
        
        // 弹出对话框让用户确认是否完成了完整训练计划
        val trainingPlan = "今日训练计划：\n" +
                "• 高抬腿 2分钟\n" +
                "• 25个俯卧撑\n" +
                "• 30次深蹲\n" +
                "• 40个臀桥\n" +
                "• 2分钟平板支撑\n" +
                "• 1分钟坐位体前屈\n" +
                "• 深呼吸 2分钟"
        
        MaterialAlertDialogBuilder(this)
            .setTitle("确认完成训练")
            .setMessage(trainingPlan + "\n\n请确认您已完成以上全部训练项目")
            .setPositiveButton("已完成") { _, _ ->
                // 执行打卡逻辑
                completeFitnessCheckIn(today)
            }
            .setNegativeButton("未完成", null)
            .show()
    }
    
    private fun completeFitnessCheckIn(today: Long) {
        val currentStreak = sharedPreferences.getInt("fitness_streak", 0)
        
        // 检查是否连续打卡
        // 获取上次健身打卡日期，兼容Integer和Long类型
        val lastFitnessDateValue = sharedPreferences.getAll()["last_fitness_date"]
        val lastFitnessDate = when (lastFitnessDateValue) {
            is Long -> lastFitnessDateValue
            is Int -> lastFitnessDateValue.toLong()
            else -> 0L
        }
        val lastDayInt = try {
            android.text.format.DateFormat.format("yyyyMMdd", java.util.Date(lastFitnessDate)).toString().toInt()
        } catch (e: Exception) {
            0
        }
        val expectedNextDay = lastDayInt + 1
        val isConsecutive = (today.toInt() == expectedNextDay) || (lastDayInt == 0) // 如果是第一次打卡，则视为连续
        
        val newStreak = if (isConsecutive) {
            currentStreak + 1
        } else {
            // 如果中断了，从1开始重新计数
            1
        }
        
        val baseReward = if (isDoubleYuanPangDay()) 6 * 2 else 6 // 双倍莹钞日翻倍
        var additionalReward = 0
        var rewardDetails = "基础奖励：$baseReward 莹钞"
        
        // 检查是否连续7天全勤
        if (newStreak % 7 == 0) {
            val weeklyReward = if (isDoubleYuanPangDay()) 42 * 2 else 42 // 连续7天奖励
            additionalReward += weeklyReward
            rewardDetails += "\n连续7天奖励：$weeklyReward 莹钞"
        }
        
        // 检查是否月完成24天
        if (newStreak % 24 == 0) {
            val monthlyReward = if (isDoubleYuanPangDay()) 100 * 2 else 100 // 月完成24天奖励
            additionalReward += monthlyReward
            rewardDetails += "\n月完成24天奖励：$monthlyReward 莹钞"
        }
        
        val totalReward = baseReward + additionalReward
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)
        
        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + totalReward)
        editor.putInt("total_yuan_pang_earned", totalEarned + totalReward)
        editor.putInt("fitness_streak", newStreak)
        editor.putLong("last_fitness_date", System.currentTimeMillis()) // 记录打卡时间
        editor.apply()
        
        loadData()
        
        val streakMessage = if (isConsecutive) {
            "连续打卡 $newStreak 天！"
        } else {
            "重新开始打卡，当前连续 $newStreak 天。继续加油！"
        }
        
        val message = "健身打卡成功！$streakMessage\n\n" +
                "奖励详情：\n" +
                "$rewardDetails\n" +
                "总计奖励：$totalReward 莹钞" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else ""
        
        MaterialAlertDialogBuilder(this)
            .setTitle("打卡成功")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun claimLearningReward() {
        val rewardAmount = if (isDoubleYuanPangDay()) 1 * 2 else 1 // 双倍莹钞日翻倍
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
        editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
        editor.apply()

        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle("奖励申领成功")
            .setMessage("学习奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun claimCertificationReward() {
        val rewardAmount = if (isDoubleYuanPangDay()) 350 * 2 else 350 // 双倍莹钞日翻倍
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
        editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
        editor.apply()

        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle("奖励申领成功")
            .setMessage("认证奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun claimDigitalProductReward() {
        val rewardAmount = if (isDoubleYuanPangDay()) 200 * 2 else 200 // 双倍莹钞日翻倍
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
        editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
        editor.apply()

        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle("奖励申领成功")
            .setMessage("数字产品奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun claimGamblingReward() {
        // 检查是否本月已经进行过博弈
        val lastGamblingMonth = sharedPreferences.getInt("last_gambling_month", 0)
        val currentMonth =
            android.text.format.DateFormat.format("MM", java.util.Date()).toString().toInt()
        val currentDay =
            android.text.format.DateFormat.format("dd", java.util.Date()).toString().toInt()

        if (lastGamblingMonth == currentMonth) {
            MaterialAlertDialogBuilder(this)
                .setTitle("提示")
                .setMessage("本月博弈机会已用完，请下月再试")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        // 模拟抛硬币：随机生成0或1
        val coinResult = Random.nextInt(0, 2) // 0为反面，1为正面

        // 不再需要单独的奖励金额，直接应用到本次操作
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

        val baseReward = 20 // 博弈基础奖励
        val totalReward = if (coinResult == 1) baseReward * 2 else baseReward // 正面翻倍，反面正常

        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + totalReward)
        editor.putInt("total_yuan_pang_earned", totalEarned + totalReward)
        editor.putInt("last_gambling_month", currentMonth)

        val resultMessage = if (coinResult == 1) {
            "博弈结果：正面！奖励翻倍！"
        } else {
            "博弈结果：反面！奖励正常发放。"
        }

        val detailMessage =
            "抛硬币结果：${if (coinResult == 1) "正面" else "反面"}，获得 $totalReward 莹钞奖励！"

        editor.apply()
        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle(resultMessage)
            .setMessage(detailMessage)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun claimEggReward() {
        val rewardAmount = if (isDoubleYuanPangDay()) 30 * 2 else 30 // 双倍莹钞日翻倍
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
        editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
        editor.apply()

        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle("奖励申领成功")
            .setMessage("彩蛋任务奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun claimComfortReward() {
        val rewardAmount = if (isDoubleYuanPangDay()) 96 * 2 else 96 // 双倍莹钞日翻倍
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
        editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
        editor.apply()

        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle("抚慰金申领成功")
            .setMessage("抚慰金 $rewardAmount 已发放，一定不要放弃！莹联万岁！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
            .setPositiveButton("我一定可以！", null)
            .show()
    }

    // 新增：增加收入来源奖励
    private fun claimIncomeSourceReward() {
        val rewardAmount = if (isDoubleYuanPangDay()) 319 * 2 else 319 // 双倍莹钞日翻倍
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

        val editor = sharedPreferences.edit()
        editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
        editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
        editor.apply()

        loadData()

        MaterialAlertDialogBuilder(this)
            .setTitle("奖励申领成功")
            .setMessage("增加收入来源奖励 $rewardAmount 莹钞已发放！" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
            .setPositiveButton("确定", null)
            .show()
    }

    // 新增：小G财指突破奖励
    private fun claimWealthIndexReward() {
        val input = TextInputEditText(this)
        input.hint = "请输入小G财指数值"

        val inputLayout = TextInputLayout(this).apply {
            addView(input)
            hint = "小G财指"
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("申报小G财指突破奖励")
            .setView(layout)
            .setPositiveButton("确认") { _, _ ->
                val indexStr = input.text.toString()
                if (indexStr.isNotEmpty()) {
                    // 验证输入是否为有效数字
                    if (!indexStr.matches(Regex("-?\\d+(\\.\\d+)?$"))) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("输入错误")
                            .setMessage("请输入有效的数字")
                            .setPositiveButton("确定", null)
                            .show()
                        return@setPositiveButton
                    }

                    val index = try {
                        indexStr.toDouble()
                    } catch (e: NumberFormatException) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("输入错误")
                            .setMessage("请输入有效的数字")
                            .setPositiveButton("确定", null)
                            .show()
                        return@setPositiveButton
                    }

                    // 检查指数值是否为100的整数倍
                    val intValue = index.toInt()
                    if (intValue % 100 != 0) {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("无法申领")
                            .setMessage("小G财指必须达到100的整数倍（如100、200、300等）才能获得奖励")
                            .setPositiveButton("确定", null)
                            .show()
                        return@setPositiveButton
                    }

                    // 计算奖励：指数值*0.005
                    val rewardAmountBase = try {
                        (index * 0.005).toInt()
                    } catch (e: ArithmeticException) {
                        0
                    }
                    val rewardAmount =
                        if (isDoubleYuanPangDay()) rewardAmountBase * 2 else rewardAmountBase

                    val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
                    val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)

                    val editor = sharedPreferences.edit()
                    editor.putInt("yuan_pang_balance", currentBalance + rewardAmount)
                    editor.putInt("total_yuan_pang_earned", totalEarned + rewardAmount)
                    editor.apply()

                    loadData()

                    MaterialAlertDialogBuilder(this)
                        .setTitle("奖励申领成功")
                        .setMessage("小G财指突破奖励 $rewardAmount 莹钞已发放！（基于财指 $index）" + if (isDoubleYuanPangDay()) "（双倍莹钞日）" else "")
                        .setPositiveButton("确定", null)
                        .show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 新增：查看储蓄记录功能
    private fun showSavingsRecords() {
        val savingsRecords =
            sharedPreferences.getStringSet("savings_records", mutableSetOf()) ?: mutableSetOf()

        if (savingsRecords.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("储蓄记录")
                .setMessage("暂无储蓄记录")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        val recordsList = mutableListOf<String>()
        val expiredRecords = mutableListOf<String>() // 存放已到期的记录

        val currentTime = System.currentTimeMillis()

        for (record in savingsRecords) {
            val parts = record.split(":")
            if (parts.size >= 6) {
                try {
                    val depositTime = parts[0].toLong()
                    val amount = parts[1].toIntOrNull() ?: 0
                    val savingType = parts[2]
                    val cpi = parts[3].toDoubleOrNull() ?: 0.0
                    val totalAmount = parts[4].toIntOrNull() ?: 0
                    val maturityTime = parts[5].toLongOrNull() ?: 0
                    val interestRate = parts[6].toDoubleOrNull() ?: 0.0

                    val depositDate = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date(depositTime))
                    } catch (e: Exception) {
                        "未知日期"
                    }
                    val maturityDate = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            .format(java.util.Date(maturityTime))
                    } catch (e: Exception) {
                        "未知日期"
                    }

                    val recordInfo =
                        "金额: $amount Ƶ\n类型: ${getSavingTypeName(savingType)}\n存款日期: $depositDate\n到期日期: $maturityDate\n利率: ${
                            String.format(
                                "%.2f",
                                interestRate
                            )
                        }%\n预期本息: $totalAmount Ƶ"

                    if (currentTime >= maturityTime) {
                        expiredRecords.add("$recordInfo\n状态: 已到期\n")
                    } else {
                        recordsList.add("$recordInfo\n状态: 未到期\n")
                    }
                } catch (e: Exception) {
                    // 跳过格式错误的记录
                    continue
                }
            }
        }

        // 检查是否有到期的储蓄并自动处理
        if (expiredRecords.isNotEmpty()) {
            processExpiredSavings(savingsRecords)
        }

        // 显示未到期的储蓄记录
        val allRecords = expiredRecords + recordsList
        val recordsArray = allRecords.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("储蓄记录")
            .setItems(recordsArray) { _, _ ->
                // 点击项目不做任何操作
            }
            .setPositiveButton("确定", null)
            .show()
    }

    // 新增：处理已到期的储蓄
    private fun processExpiredSavings(allRecords: Set<String>) {
        val currentTime = System.currentTimeMillis()
        val currentBalance = sharedPreferences.getInt("yuan_pang_balance", 0)
        var totalAddedAmount = 0
        val updatedRecords = mutableSetOf<String>()
        var hasExpiredRecord = false

        for (record in allRecords) {
            val parts = record.split(":")
            if (parts.size >= 6) {
                try {
                    val maturityTime = parts[5].toLongOrNull() ?: 0
                    val totalAmount = parts[4].toIntOrNull() ?: 0

                    if (currentTime >= maturityTime) {
                        // 这笔储蓄已到期，将本息加入账户
                        totalAddedAmount += totalAmount
                        hasExpiredRecord = true
                    } else {
                        // 未到期的储蓄继续保留在记录中
                        updatedRecords.add(record)
                    }
                } catch (e: Exception) {
                    // 跳过格式错误的记录
                    continue
                }
            }
        }

        if (hasExpiredRecord) {
            val newBalance = try {
                currentBalance + totalAddedAmount
            } catch (e: ArithmeticException) {
                // 防止整数溢出
                Int.MAX_VALUE
            }

            val editor = sharedPreferences.edit()
            editor.putInt("yuan_pang_balance", newBalance)
            editor.putStringSet("savings_records", updatedRecords)
            editor.apply()

            loadData() // 更新显示的余额

            MaterialAlertDialogBuilder(this)
                .setTitle("储蓄到期提醒")
                .setMessage("您有储蓄已到期，共获得本息 $totalAddedAmount Ƶ，已自动转入您的账户！")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 每次返回时更新法令状态
        updateOrdinanceStatus()
        // 检查是否有到期的储蓄并处理
        checkAndProcessExpiredSavings()
        // 更新莹历显示
        updateYingYearDisplay()
    }

    // 新增：检查并处理到期储蓄
    private fun checkAndProcessExpiredSavings() {
        val savingsRecords =
            sharedPreferences.getStringSet("savings_records", mutableSetOf()) ?: mutableSetOf()
        if (savingsRecords.isNotEmpty()) {
            processExpiredSavings(savingsRecords)
        }
    }

    // 新增：显示清空所有记录的对话框
    private fun showClearAllRecordsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清空所有记录")
            .setMessage("您确定要清空所有莹钞余额、违法记录和储蓄记录吗？此操作不可恢复！")
            .setPositiveButton("确认清空") { _, _ ->
                clearAllRecords()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 新增：清空所有记录
    private fun clearAllRecords() {
        val editor = sharedPreferences.edit()

        // 清空莹钞余额相关
        editor.putInt("yuan_pang_balance", 0)
        editor.putInt("total_yuan_pang_earned", 0)

        // 清空违法记录相关
        editor.putInt("penalty_count", 0)
        editor.putLong("last_violation_time", 0)
        editor.putStringSet("violated_ordinances", mutableSetOf())

        // 清空所有详细的违法记录
        for (i in 1..16) { // 假设法令总数为16
            val detailedViolationKey = "violation_detail_$i"
            editor.remove(detailedViolationKey)
        }

        // 清空储蓄记录相关
        editor.putStringSet("savings_records", mutableSetOf())

        // 清空其他可能的相关记录
        editor.putInt("abstinence_cycle_progress", 0)
        editor.putInt("abstinence_accumulated_reward", 0)
        editor.putInt("fitness_streak", 0)
        editor.putLong("last_fitness_date", 0L)

        // 重置固定奖励领取记录
        editor.putInt("last_fixed_reward_year", 0)
        editor.putInt("last_fixed_reward_month", 0)

        editor.apply()

        loadData() // 更新UI显示
        updateOrdinanceStatus() // 更新法令状态

        MaterialAlertDialogBuilder(this)
            .setTitle("清空完成")
            .setMessage("所有记录已清空，账户已归零")
            .setPositiveButton("确定", null)
            .show()
    }

    // 新增：显示违法历史记录对话框
    private fun showBanHistoryDialog() {
        // 获取所有曾经违法的记录（包括已完成的）
        val allOrdinanceNumbers = (1..16) // 假设法令总数为16
        val banHistoryList = mutableListOf<String>()

        for (i in allOrdinanceNumbers) {
            val detailedViolationKey = "violation_detail_$i"
            val violationDetail = sharedPreferences.getString(detailedViolationKey, null)

            if (violationDetail != null) {
                val parts = violationDetail.split(":")
                if (parts.size >= 2) {
                    val timestamp = parts[0].toLong()
                    val status = parts[1]
                    val ordinanceText = getOrdinanceTextByNumber(i)

                    val violationDate = try {
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                        )
                            .format(java.util.Date(timestamp))
                    } catch (e: Exception) {
                        "未知时间"
                    }

                    val statusText = if (status == "completed") "已完成处罚" else "待完成处罚"

                    banHistoryList.add(
                        "法令编号: $i\n" +
                                "违法时间: $violationDate\n" +
                                "处罚状态: $statusText\n" +
                                "法令内容: $ordinanceText"
                    )
                }
            }
        }

        if (banHistoryList.isEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setTitle("违法历史记录")
                .setMessage("暂无违法记录")
                .setPositiveButton("确定", null)
                .show()
            return
        }

        val banHistoryArray = banHistoryList.toTypedArray()

        MaterialAlertDialogBuilder(this)
            .setTitle("违法历史记录")
            .setItems(banHistoryArray) { _, _ ->
                // 点击项目不做任何操作
            }
            .setPositiveButton("确定", null)
            .show()
    }

    // 新增：根据法令编号获取法令文本
    private fun getOrdinanceTextByNumber(number: Int): String {
        val ordinances = listOf(
            "誓死捍卫自己的基本人权与自由！自己拥有对自我命运的绝对主权！",
            "本人对自身婚姻事务拥有绝对且排他的最终决定权。是否结婚、与谁结婚、何时结婚均由本人独立判断。",
            "严禁一切形式的占卜、算命、宗教及封建迷信活动与信仰。违者罚跑800米一圈，20个引体向上，罚款1000元。",
            "坚持丁克主义，永久性禁止生育后代，违者罚款200000元，罚跑2次1200米。",
            "永久性彻底禁止一切投机与赌博行为，严禁交易个股、期货、期权、外汇、加密货币等。违者罚跑2次1200米，2组20个引体向上。",
            "严禁购买任何加密货币、稳定币、NFT等链上资产。违者罚跑2次1200米，3组20个引体向上。",
            "严禁以任何形式为任何第三方的债务提供担保、增信或承诺承担连带责任。违者罚跑1次1200米，2组20个引体向上。",
            "原则上严禁借入资金，借入金额不得超过上月基本生活费的两倍。违者罚跑1次1200米。",
            "原则上严禁借出资金，单笔出借金额超过自身月收入的2倍，必须严格遵循《GML借款合同框架》。违者罚跑800米一圈，10个引体向上。",
            "严禁将生活费及预留的应急资金用于任何风险投资、交易、出借。违者罚跑2次1200米，2组20个引体向上。",
            "坚决维护个人财产所有权及绝对处分权，禁止让渡于他人。违者罚跑1200米一圈，20个引体向上。",
            "严禁吸烟、吸毒。违者罚款10000元，罚跑2次1200米，3组20个引体向上。",
            "严禁在国内平台发表任何可能被认定为煽动分裂、颠覆或传播严重谣言的内容。违者罚款500元，强制断网5小时。",
            "严禁在婚姻关系存续期间出轨、嫖娼。违者罚跑2次1200米，2组20个引体向上，20个仰卧起坐，罚款10000元。",
            "禁止购买任何游戏内虚拟物品、代币或服务，即禁止游戏充值，违者罚跑800米，20个仰卧起坐。",
            "严禁在任何网络平台，进行打赏、刷礼物、充电等付费活动，违者罚跑1000米，40个仰卧起坐。"
        )

        return if (number > 0 && number <= ordinances.size) {
            ordinances[number - 1]
        } else {
            "未知法令"
        }
    }

    // 计算莹历的函数
    private fun calculateYingYear(publicYear: Int, publicMonth: Int): Int {
        val yingYear = 1895 + 18 * (publicYear - 2020) + 1.5 * (publicMonth - 1)
        return yingYear.toInt() // 去掉小数点部分
    }

    // 计算莹轩年数的函数（从2021年开始计算）
    private fun calculateYingXuanYears(publicYear: Int): Int {
        // 莹轩年从2021年开始计算，包含当前年份
        return publicYear - 2021 + 1
    }

    // 更新莹历显示
    private fun updateYingYearDisplay() {
        try {
            val currentDate = java.util.Date()
            val publicYear =
                android.text.format.DateFormat.format("yyyy", currentDate).toString().toInt()
            val publicMonth =
                android.text.format.DateFormat.format("MM", currentDate).toString().toInt()

            val yingYear = calculateYingYear(publicYear, publicMonth)
            val yingXuanYears = calculateYingXuanYears(publicYear)

            val yingYearText = "莹历${yingYear}年  莹轩${yingXuanYears}年"
            binding.yingyear.text = yingYearText
        } catch (e: Exception) {
            // 如果计算失败，显示默认值
            binding.yingyear.text = "莹历2000年 | 莹轩5年"
        }
    }

    // 新增：显示导出导入底部工作表
    private fun showExportImportBottomSheet() {
        val bottomSheetView = layoutInflater.inflate(R.layout.bottom_sheet_export_import, null)
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        val btnExport =
            bottomSheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnExport)
        val btnImport =
            bottomSheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnImport)

        btnExport.setOnClickListener {
            exportData()
            bottomSheetDialog.dismiss()
        }

        btnImport.setOnClickListener {
            importData()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    // 新增：导出数据
    private fun exportData() {
        // 获取当前所有数据
        val balance = sharedPreferences.getInt("yuan_pang_balance", 0)
        val totalEarned = sharedPreferences.getInt("total_yuan_pang_earned", 0)
        val penaltyCount = sharedPreferences.getInt("penalty_count", 0)
        val violatedOrdinances =
            sharedPreferences.getStringSet("violated_ordinances", mutableSetOf()) ?: mutableSetOf()
        val savingsRecords =
            sharedPreferences.getStringSet("savings_records", mutableSetOf()) ?: mutableSetOf()

        // 获取所有详细违法记录
        val violationDetails = mutableMapOf<String, String>()
        for (i in 1..16) { // 假设法令总数为16
            val detailedViolationKey = "violation_detail_$i"
            val violationDetail = sharedPreferences.getString(detailedViolationKey, null)
            if (violationDetail != null) {
                violationDetails[detailedViolationKey] = violationDetail
            }
        }

        val abstinenceCycleProgress = sharedPreferences.getInt("abstinence_cycle_progress", 0)
        val abstinenceAccumulatedReward =
            sharedPreferences.getInt("abstinence_accumulated_reward", 0)
        val fitnessStreak = sharedPreferences.getInt("fitness_streak", 0)
        // 获取上次健身打卡日期，兼容Integer和Long类型
        val lastFitnessDateValue = sharedPreferences.getAll()["last_fitness_date"]
        val lastFitnessDate = when (lastFitnessDateValue) {
            is Long -> lastFitnessDateValue
            is Int -> lastFitnessDateValue.toLong()
            else -> 0L
        }
        val lastFixedRewardYear = sharedPreferences.getInt("last_fixed_reward_year", 0)
        val lastFixedRewardMonth = sharedPreferences.getInt("last_fixed_reward_month", 0)
        // 获取最后违法时间，兼容Integer和Long类型
        val lastViolationTimeValue = sharedPreferences.getAll()["last_violation_time"]
        val lastViolationTime = when (lastViolationTimeValue) {
            is Long -> lastViolationTimeValue
            is Int -> lastViolationTimeValue.toLong()
            else -> 0L
        }
        val selectedCurrency = sharedPreferences.getString("selected_currency", CURRENCY_YUAN_PANG)
            ?: CURRENCY_YUAN_PANG

        // 调用工具类导出数据
        dataExportImportUtils.exportData(
            balance = balance,
            totalEarned = totalEarned,
            penaltyCount = penaltyCount,
            violatedOrdinances = violatedOrdinances,
            savingsRecords = savingsRecords,
            violationDetails = violationDetails,
            abstinenceCycleProgress = abstinenceCycleProgress,
            abstinenceAccumulatedReward = abstinenceAccumulatedReward,
            fitnessStreak = fitnessStreak,
            lastFitnessDate = lastFitnessDate,
            lastFixedRewardYear = lastFixedRewardYear,
            lastFixedRewardMonth = lastFixedRewardMonth,
            lastViolationTime = lastViolationTime,
            selectedCurrency = selectedCurrency
        )
    }

    // 新增：导入数据
    private fun importData() {
        try {
            // 启动文件选择器，只允许选择JSON文件
            importFileLauncher.launch("application/json")
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this)
                .setTitle("错误")
                .setMessage("无法打开文件选择器: ${e.message}")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    // 处理导入的文件
    private fun handleImportFile(uri: Uri) {
        try {
            // 使用流的方式读取文件内容
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val importedData = dataExportImportUtils.importDataFromJsonString(jsonString)
                importedData?.let { data ->
                    restoreData(data)
                }
            }
        } catch (e: Exception) {
            MaterialAlertDialogBuilder(this)
                .setTitle("错误")
                .setMessage("读取文件失败: ${e.message}")
                .setPositiveButton("确定", null)
                .show()
        }
    }

    // 新增：恢复导入的数据
    private fun restoreData(importedData: tz.yx.gml.utils.ExportData) {
        val editor = sharedPreferences.edit()

        // 恢复基础数据
        editor.putInt("yuan_pang_balance", importedData.balance)
        editor.putInt("total_yuan_pang_earned", importedData.totalEarned)
        editor.putInt("penalty_count", importedData.penaltyCount)
        editor.putStringSet("violated_ordinances", importedData.violatedOrdinances)
        editor.putStringSet("savings_records", importedData.savingsRecords)

        // 恢复详细违法记录
        for ((key, value) in importedData.violationDetails) {
            editor.putString(key, value)
        }

        // 恢复其他数据
        editor.putInt("abstinence_cycle_progress", importedData.abstinenceCycleProgress)
        editor.putInt("abstinence_accumulated_reward", importedData.abstinenceAccumulatedReward)
        editor.putInt("fitness_streak", importedData.fitnessStreak)
        editor.putLong("last_fitness_date", importedData.lastFitnessDate)
        editor.putInt("last_fixed_reward_year", importedData.lastFixedRewardYear)
        editor.putInt("last_fixed_reward_month", importedData.lastFixedRewardMonth)
        editor.putLong("last_violation_time", importedData.lastViolationTime)
        editor.putString("selected_currency", importedData.selectedCurrency)

        editor.apply()

        // 更新UI
        loadData()
        updateOrdinanceStatus()

        MaterialAlertDialogBuilder(this)
            .setTitle("导入完成")
            .setMessage("数据已成功恢复")
            .setPositiveButton("确定", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 确保SharedPreferences被正确关闭
        if (::sharedPreferences.isInitialized) {
        }
    }
}