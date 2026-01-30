# TradingGML - 金融交易计算器

<div align="center">

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![Language](https://img.shields.io/badge/language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

一款专注于金融交易计算的Android应用，包含多种交易工具和投资分析功能。

</div>

## 📋 目录

- [应用简介](#-应用简介)
- [✨ 核心功能](#-核心功能)
- [🔧 技术特性](#-技术特性)
- [🎨 界面设计](#-界面设计)
- [🔐 安全特性](#-安全特性)
- [⚙️ 权限说明](#️-权限说明)
- [🚀 安装与使用](#-安装与使用)
- [🏗️ 项目构建与开发](#️-项目构建与开发)
- [📁 项目结构](#-项目结构)
- [📝 代码结构](#-代码结构)
- [📈 功能详解](#-功能详解)
- [📄 许可证](#-许可证)
- [📞 联系方式](#-联系方式)

## 📱 应用简介

TradingGML是一款专注于金融交易计算的Android应用，主要提供多种交易工具和投资分析功能。应用包含CFD收益计算器、利率转换工具、财务波动率分析、ETF补仓计算以及永久投资组合分析等功能。此外还包含一些个人使用的自律管理功能（仅供开发者本人使用）。

## ✨ 核心功能

### 1. 交易计算器套件

#### 主页交易工具集
- **CFD收益计算器**: 计算交易盈亏、保证金和ROI
- **利率转换计算器**: 利率类型转换与复利计算
- **财务波动率计算器**: 分析月度收支波动，计算储蓄系数
- **ETF补仓计算器**: 计算ETF补仓后的平均成本和盈亏
- **永久投资组合**: 资产配置分析与调仓建议

![num](https://github.com/user-attachments/assets/9a500d55-1b8c-4834-be9c-e3c6b13db4cb)

### 2. 附加功能

#### 个人自律管理（仅供我自己使用）
- **自律管理系统**: 个人行为规范与奖励机制（仅供我自己使用）
- **财务管理**: 个人财务记录与分析（仅供我自己使用）

![wallet](https://github.com/user-attachments/assets/c6ca00d9-8211-4fc6-ad01-186f7b0fd9d5)
![wallet2](https://github.com/user-attachments/assets/9c3af1c3-770e-4420-978f-4403b23cb18e)

### 3. 工具功能

#### 笔记与密码管理
- **Markdown笔记**: 支持Markdown格式的笔记编辑和预览
- **密码管理器**: 安全存储各类账户密码信息
- **数据备份**: 提供导入导出功能

![password](https://github.com/user-attachments/assets/c5e43027-7035-4515-861a-0be09c8d6627)

### 4. 娱乐与模拟功能

#### 游戏与模拟器
- **飞行小鸟游戏**: 内置经典Flappy Bird游戏
- **短信模拟器**: 支持添加虚假通话记录和短信

![bird](https://github.com/user-attachments/assets/d5ddde07-a17d-4e9c-92a4-3460689e1ce0)
![fake](https://github.com/user-attachments/assets/44e87ea1-7c7f-4201-9bab-9e57bd85883f)

### 5. 桌面小部件

#### 便捷访问工具
- **桌面小部件**: 提供便捷的桌面快捷方式

![widget](https://github.com/user-attachments/assets/bcf56f15-99e1-4cf8-ac0c-1ef09abc1b75)

## 🔧 技术特性

### 开发技术栈
- **编程语言**: Kotlin
- **架构模式**: Android原生开发 + MVVM架构
- **UI框架**: Material Design 3 Components
- **数据存储**: SharedPreferences, 文件存储
- **安全机制**: 指纹认证API
- **第三方库**: 
  - Markwon (Markdown渲染)
  - Gson (数据序列化)
  - Biometric (生物识别认证)

### 核心组件
- **ViewPager2**: 主页滑动切换不同功能模块
- **TabLayout**: 顶部标签导航
- **RecyclerView**: 密码列表展示
- **DataBinding**: 视图绑定技术
- **ActivityResultContracts**: 现代化Activity结果处理
- **MaterialAlertDialog**: 现代化对话框组件

## 🎨 界面设计

### 设计语言
- 采用Material Design 3设计规范，使用CardView、ElevatedButton等组件

### 主页设计
- 采用ViewPager2实现多个核心功能模块的滑动切换:
  - CFD收益计算器
  - 利率转换计算器  
  - 财务波动率计算器
  - ETF补仓计算器
  - 永久投资组合

## 🔐 安全特性

### 生物识别保护
- **笔记模块**: 支持指纹解锁
- **密码管理器**: 支持指纹解锁
- **自律系统**: 支持指纹解锁
- **灵活配置**: 可在设置中灵活配置各项指纹保护功能

### 数据安全
- **本地存储**: 敏感数据本地存储，无云端同步
- **隐私保护**: 无网络传输，保障用户隐私
- **权限控制**: 文件权限严格控制
- **数据备份**: 支持加密数据导出/导入功能

## ⚙️ 权限说明

应用需要以下权限以确保功能正常运行:

| 权限 | 用途 | 说明 |
|------|------|------|
| `POST_NOTIFICATIONS` | 发送通知 | 显示桌面小部件提醒 |
| `READ_EXTERNAL_STORAGE` | 读取外部存储 | 访问笔记和配置文件 |
| `WRITE_EXTERNAL_STORAGE` | 写入外部存储 | 保存笔记和导出数据 |
| `USE_BIOMETRIC` | 生物识别 | 指纹验证功能 |
| `WRITE_CALL_LOG` | 写入通话记录 | 短信模拟器功能 |
| `WRITE_SMS` | 写入短信 | 短信模拟器功能 |
| `READ_SMS` | 读取短信 | 短信模拟器功能 |
| `RECEIVE_SMS` | 接收短信 | 短信模拟器功能 |
| `SEND_SMS` | 发发短信 | 短信模拟器功能 |

## 🚀 安装与使用

### 系统要求
- Android 6.0 (API level 23) 或更高版本
- 支持指纹识别的设备(可选，用于安全功能)

### 安装方式
1. 下载APK文件
2. 允许安装未知来源应用
3. 运行安装包完成安装

## 🏗️ 项目构建与开发

### 开发环境配置
- **IDE**: Android Studio (推荐 Otter 3 Feature Drop | 2025.2.3 或更高版本)
- **JDK**: JDK 17 或更高版本
- **Gradle**: 8.10 或更高版本

### 项目依赖
```kotlin
// 核心依赖
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.10.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

// 生物识别认证
implementation 'androidx.biometric:biometric:1.1.0'

// Markdown渲染
implementation 'io.noties.markwon:core:4.6.2'
implementation 'io.noties.markwon:ext-tables:4.6.2'
implementation 'io.noties.markwon:ext-tasklist:4.6.2'
implementation 'io.noties.markwon:html:4.6.2'

// JSON序列化
implementation 'com.google.code.gson:gson:2.10.1'

// 测试依赖
testImplementation 'junit:junit:4.13.2'
androidTestImplementation 'androidx.test.ext:junit:1.1.5'
androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
```

### 构建配置
```kotlin

        compileSdk 34
        minSdk 23
        targetSdk 34
        JavaVersion.VERSION_1_8
        jvmTarget = '1.8'

```

## 📁 项目结构

```
tz.yx.gml/
├── app/
│   ├── build.gradle.kts              # 应用模块构建配置
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # 应用清单文件
│       │   ├── java/tz/yx/gml/       # 源代码目录
│       │   │   ├── homefrag/         # 主页功能模块
│       │   │   ├── note/             # 笔记管理模块
│       │   │   ├── passwd/           # 密码管理模块
│       │   │   ├── plugins/          # 插件功能
│       │   │   ├── sms/              # 短信处理相关
│       │   │   ├── utils/            # 工具类
│       │   │   └── MyNightWidgetProvider.kt  # 桌面小部件
│       │   └── res/                  # 资源文件目录
│       │       ├── drawable/         # 图片资源
│       │       ├── layout/           # 布局文件
│       │       ├── mipmap/           # 应用图标
│       │       ├── values/           # 字符串、颜色等资源
│       │       └── xml/              # XML资源文件
│       ├── androidTest/              # Android测试
│       └── test/                     # 单元测试
├── build.gradle.kts                  # 项目级构建配置
├── gradle.properties                 # Gradle配置属性
├── gradlew                           # Linux/Mac Gradle包装器
├── gradlew.bat                       # Windows Gradle包装器
└── settings.gradle.kts               # 项目设置
```

## 📝 代码结构

### 主要包结构
- `homefrag/`: 主页相关的交易计算功能
  - `MainActivity.kt`: 应用主界面，包含ViewPager2导航
  - `MainPagerAdapter.kt`: ViewPager2适配器
  - `ProfitFragment.kt`: CFD收益计算器
  - `RiskFragment.kt`: 利率转换计算器
  - `vixFragment.kt`: 财务波动率计算器
  - `stockFragment.kt`: ETF补仓计算器
  - `foreverFragment.kt`: 永久投资组合分析
  - `YxActivity.kt`: 个人自律管理系统（仅供开发者使用）

- `note/`: 笔记管理模块
  - `NoteActivity.kt`: Markdown笔记编辑器
  - `NotelistActivity.kt`: 笔记列表管理

- `passwd/`: 密码管理模块
  - `PasswdActivity.kt`: 密码管理主界面
  - `PasswordItem.kt`: 密码数据模型
  - `PasswordExport.kt`: 密码导出功能

- `plugins/`: 插件功能
  - `BirdActivity.kt`: 飞行小鸟游戏
  - `FakeActivity.kt`: 短信模拟器

- `sms/`: 短信处理相关
  - `SmsHandlingActivity.kt`: 短信处理
  - `SmsReceiver.kt`: 短信接收器
  - `SmsReceiverService.kt`: 短信接收服务
  - `MmsReceiver.kt`: 彩信接收器
  - `HeadlessSmsSendService.kt`: 无声短信发送服务

- `utils/`: 工具类
  - `DataExportImportUtils.kt`: 数据导出导入工具
  - `FingerprintManager.kt`: 指纹管理工具
  - `SettingActivity.kt`: 设置界面

### 关键类功能说明
- `MainActivity`: 应用入口，提供交易计算器导航
- `ProfitFragment`: CFD收益计算，支持多空双向计算
- `RiskFragment`: 利率转换与复利计算
- `vixFragment`: 财务波动率分析，计算储蓄系数
- `stockFragment`: ETF补仓计算，平均成本分析
- `foreverFragment`: 永久投资组合分析，资产配置建议

## 📈 功能详解

### 交易计算工具
应用提供了五套完整的计算工具，专注于金融交易分析：

1. **CFD收益计算器**: 计算开仓所需的保证金、预期盈亏和收益率
2. **利率转换计算器**: 处理不同利率类型的转换与复利计算
3. **财务波动率计算器**: 分析收支波动，计算储蓄系数
4. **ETF补仓计算器**: 计算多次买入后的平均成本和盈亏
5. **永久投资组合**: 资产配置分析与调仓建议

## 📄 许可证

本项目采用MIT许可证，详情请查看[LICENSE](LICENSE)文件。

## 📞 联系方式

如有任何问题或建议，请通过以下方式联系我:
- QQ: 1778607946@qq.com
