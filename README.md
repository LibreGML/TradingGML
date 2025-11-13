# TradingGML

<div align="center">

[![Platform](https://img.shields.io/badge/platform-Android-green.svg)](https://www.android.com)
[![Language](https://img.shields.io/badge/language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

一款专为交易者设计的多功能Android应用，集交易纪律、笔记管理、密码管理、短信模拟等功能于一体。

</div>

## 📱 应用简介

TradingGML是一款专为金融交易者打造的综合性工具应用，旨在帮助交易者更好地管理交易纪律、记录交易笔记、安全存储账户密码，并提供其他实用功能。该应用采用现代化的Material Design设计风格，界面美观且易于使用。

## 🌟 核心功能

### 1. 交易纪律管理
- 展示详细的交易守则和纪律要求
- 包含开仓、风控、频率、标的白名单和复盘等五大核心模块
- 提供专业的交易理念和实践指导

### 2. 临时笔记系统
- 支持Markdown格式的笔记编辑和预览
- 内置文件管理功能，方便查看和管理笔记文件
- 支持指纹解锁保护隐私安全

### 3. 密码管理器
- 安全存储各类账户密码信息
- 支持指纹解锁验证
- 提供导入导出功能，方便数据备份和迁移
- 搜索功能快速定位账户信息

### 4. 飞行小鸟游戏
- 内置经典Flappy Bird游戏
- 为用户提供休闲娱乐功能
- 帮助用户在紧张的交易间隙放松心情

### 5. 短信模拟器
- 支持添加虚假通话记录和短信
- 可自定义时间、号码、内容等参数
- 适用于测试或其他特殊用途

## 🔧 技术特性

### 开发技术栈
- **语言**: Kotlin
- **架构**: Android原生开发
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
  - 合约收益
  - 风险控制
  - USDT交易
  - 股票补仓

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
├── note/               # 笔记管理模块
├── passwd/             # 密码管理模块
├── plugins/            # 插件功能(游戏、短信模拟)
├── sms/                # 短信处理相关
└── utils/              # 工具类
```

## 🛠️ 开发环境

- **IDE**: Android Studio
- **构建工具**: Gradle (Kotlin DSL)
- **最低SDK版本**: 23
- **目标SDK版本**: 36

## 📄 许可证

本项目采用MIT许可证，详情请查看[LICENSE](LICENSE)文件。

## 📞 联系方式

如有任何问题或建议，请通过以下方式联系我:
- QQ: 1778607946@qq.com

