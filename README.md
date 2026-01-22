# TradingGML - 综合性交易与自律管理系统

<div align="center">

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![Language](https://img.shields.io/badge/language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

一款专为交易者和自律人士设计的多功能Android应用，集交易计算、纪律管理、笔记记录、密码管理、娱乐游戏等功能于一体。

</div>

## 📱 应用简介

TradingGML是一款面向金融交易者和自律追求者的综合性工具应用，致力于帮助用户建立良好的交易纪律、管理财务状况、记录重要信息，并提供其他实用功能。该应用采用现代化的Material Design设计风格，界面美观且易于使用，结合了交易计算、财务管理、自律系统等多种功能。

## 🌟 核心功能

### 1. 交易计算器套件
- **利润计算器**: 实时计算交易盈亏、保证金和ROI
- **风险计算器**: 计算建议仓位大小和风险控制参数
- **USDT买卖计算器**: 计算USDT交易的建议手数和盈亏情况
- **股票补仓计算器**: 计算股票补仓后的平均成本和盈亏情况
- 支持多空双向计算，满足不同交易策略需求

![num](https://github.com/user-attachments/assets/9a500d55-1b8c-4834-be9c-e3c6b13db4cb)

### 2. 自律管理系统 (莹钞系统)
- **个人宪法管理**: 16条个人法则，涵盖人权自由、婚姻自主、禁赌禁烟等重要领域
- **违规记录追踪**: 记录违反法则的行为，自动统计违规次数
- **奖励机制**: 提供多种奖励类型，包括：
  - 固定奖励（2月和10月发放）
  - 无违法奖励（连续一个月无违规）
  - 年度无违法奖励
  - 储蓄率奖励（达到71%以上）
  - 禁欲奖励（完成35天周期）
  - 健身打卡奖励
  - 学习奖励
  - 认证奖励
  - 数字产品奖励
  - 博弈奖励
  - 彩蛋任务奖励
  - 抚慰金奖励
  - 收入来源奖励
  - 小G财指突破奖励
- **储蓄系统**: 支持活期和定期储蓄，不同期限享受不同利率
- **货币兑换**: 支持莹钞(Ƶ)与人民币(¥)之间的兑换
![yx](https://github.com/user-attachments/assets/6c919748-2ac4-4c0d-a8b2-ac1814460a1b)

### 3. 临时笔记系统
- 支持Markdown格式的笔记编辑和预览
- 内置文件管理功能，方便查看和管理笔记文件
- 支持指纹解锁保护隐私安全
- 提供编辑和预览双模式切换

### 4. 密码管理器
- 安全存储各类账户密码信息
- 支持指纹解锁验证
- 提供导入导出功能，方便数据备份和迁移
- 搜索功能快速定位账户信息
- 支持增删改查操作

![password](https://github.com/user-attachments/assets/c5e43027-7035-4515-861a-0be09c8d6627)

### 5. 飞行小鸟游戏
- 内置经典Flappy Bird游戏
- 为用户提供休闲娱乐功能
- 帮助用户在紧张的交易间隙放松心情

![bird](https://github.com/user-attachments/assets/d5ddde07-a17d-4e9c-92a4-3460689e1ce0)

### 6. 短信模拟器
- 支持添加虚假通话记录和短信
- 可自定义时间、号码、内容等参数
- 适用于测试或其他特殊用途
- 支持多种短信和通话类型

![fake](https://github.com/user-attachments/assets/44e87ea1-7c7f-4201-9bab-9e57bd85883f)

### 7. 桌面小部件
- 提供便捷的桌面快捷方式
- 快速访问常用功能

![widget](https://github.com/user-attachments/assets/bcf56f15-99e1-4cf8-ac0c-1ef09abc1b75)

## 🔧 技术特性

### 开发技术栈
- **语言**: Kotlin
- **架构**: Android原生开发 + MVVM架构
- **UI框架**: Material Design Components
- **数据存储**: SharedPreferences, 文件存储
- **安全**: 指纹认证API
- **第三方库**: 
  - Markwon (Markdown渲染)
  - Gson (数据序列化)

### 核心组件
- **ViewPager2**: 主页滑动切换不同功能模块
- **TabLayout**: 顶部标签导航
- **RecyclerView**: 密码列表展示
- **DataBinding**: 视图绑定技术
- **ActivityResultContracts**: 现代化Activity结果处理

## 🎨 界面设计

### 主页设计
- 采用ViewPager2实现四个核心功能模块的滑动切换:
  - 合约收益计算器
  - 风险控制计算器  
  - USDT交易计算器
  - 股票补仓计算器

### 功能模块
- **交易纪律**: 卡片式设计展示各项交易规则
- **笔记系统**: Tab切换编辑和预览模式
- **密码管理**: 列表展示密码信息，支持添加、编辑、删除
- **短信模拟**: 表单式界面，操作直观简单

## 🔐 安全特性

### 指纹保护
- 笔记模块支持指纹解锁
- 密码管理器支持指纹解锁
- 可在设置中灵活配置各项指纹保护功能

### 数据安全
- 敏感数据本地存储
- 无网络传输，保障用户隐私
- 文件权限严格控制
- 支持数据导出/导入功能，便于备份

## ⚙️ 权限说明

应用需要以下权限以确保功能正常运行:
- `POST_NOTIFICATIONS`: 发送通知
- `READ_EXTERNAL_STORAGE`: 读取外部存储
- `WRITE_EXTERNAL_STORAGE`: 写入外部存储
- `USE_FINGERPRINT`: 指纹验证
- `WRITE_CALL_LOG`: 写入通话记录
- `WRITE_SMS`: 写入短信
- `READ_SMS`: 读取短信
- `RECEIVE_SMS`: 接收短信
- `SEND_SMS`: 发送短信

## 🚀 安装与使用

### 系统要求
- Android 6.0 (API level 23) 或更高版本
- 支持指纹识别的设备(可选，用于安全功能)

### 安装方式
1. 下载APK文件
2. 允许安装未知来源应用
3. 运行安装包完成安装

### 快捷方式
应用支持创建桌面快捷方式:
- 交易纪律
- 飞行小鸟
- 临时笔记
- 密码管理器

## 📁 项目结构

```
tz.yx.gml/
├── homefrag/           # 主页相关功能模块
│   ├── MainActivity.kt # 主界面
│   ├── MainPagerAdapter.kt # ViewPager适配器
│   ├── ProfitFragment.kt # 利润计算器
│   ├── RiskFragment.kt # 风险计算器
│   ├── UsdtFragment.kt # USDT交易计算器
│   ├── stockFragment.kt # 股票补仓计算器
│   └── YxActivity.kt # 自律管理系统主界面
├── note/               # 笔记管理模块
│   ├── NoteActivity.kt # 笔记编辑界面
│   └── NotelistActivity.kt # 笔记列表界面
├── passwd/             # 密码管理模块
│   ├── PasswdActivity.kt # 密码管理界面
│   ├── PasswordExport.kt # 密码导出功能
│   └── PasswordItem.kt # 密码数据模型
├── plugins/            # 插件功能
│   ├── BirdActivity.kt # 飞行小鸟游戏
│   └── FakeActivity.kt # 短信模拟器
├── sms/                # 短信处理相关
├── utils/              # 工具类
│   ├── DataExportImportUtils.kt # 数据导出导入工具
│   ├── FingerprintManager.kt # 指纹管理
│   └── SettingActivity.kt # 设置界面
└── MyNightWidgetProvider.kt # 桌面小部件
```

## 🛠️ 开发环境

- **IDE**: Android Studio
- **构建工具**: Gradle (Kotlin DSL)
- **最低SDK版本**: 23
- **目标SDK版本**: 36

## 📈 特色功能详解

### 自律系统 - 莹钞体系
TradingGML的核心特色之一是其独特的"莹钞"自律系统，这是一种基于虚拟货币的自我约束和激励机制：

- **莹钞符号**: Ƶ (Unicode字符)
- **违规惩罚**: 每次违反个人法则会被记录并影响奖励资格
- **奖励机制**: 多样化的奖励途径，鼓励积极行为
- **储蓄系统**: 支持不同期限的储蓄计划，享受相应利息
- **双倍日**: 每月1日为双倍奖励日，激励用户在月初设定良好开端

### 交易计算工具
针对交易者的需求，应用提供了四套完整的计算工具：

1. **利润计算器**: 计算开仓所需的保证金、预期盈亏和收益率
2. **风险计算器**: 根据账户资金和可承受损失，计算建议的开仓手数
3. **USDT计算器**: 分析USDT交易的盈亏情况
4. **股票补仓计算器**: 计算多次买入后的平均成本和盈亏

## 📄 许可证

本项目采用MIT许可证，详情请查看[LICENSE](LICENSE)文件。

## 📞 联系方式

如有任何问题或建议，请通过以下方式联系我:
- QQ: 1778607946@qq.com
