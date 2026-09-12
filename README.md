# RedMagic Helper

基于 libxposed 的努比亚 / 红魔系统增强模块。

## 目录结构

```
├── app/                                # 主模块（Xposed 模块应用本体）
│   ├── src/main/
│   │   ├── kotlin/dev/lackluster/redmagichelper/
│   │   │   ├── HelperApplication.kt    # Application 入口
│   │   │   ├── data/                   # 数据定义层
│   │   │   │   ├── Pref.kt             #   所有功能开关的 SharedPreferences 键
│   │   │   │   ├── Pages.kt            #   页面路由常量
│   │   │   │   ├── Scope.kt            #   Hook 目标应用包名（作用域）
│   │   │   │   ├── SearchIndex.kt      #   设置项搜索索引
│   │   │   │   └── Constants.kt        #   全局常量
│   │   │   ├── hook/                   # Hook 层
│   │   │   │   ├── HookEntry.kt        #   模块入口，按包名分发 Hook
│   │   │   │   ├── apps/               #   按目标应用组织的 Hook 加载器
│   │   │   │   │   └── nubia/          #     努比亚 / 红魔系统应用
│   │   │   │   ├── compat/             #   Hook 兼容层（YukiHookAPI 封装）
│   │   │   │   ├── rules/              #   具体功能 Hook 实现，按目标应用分类
│   │   │   │   └── view/               #   界面相关 Hook
│   │   │   ├── service/                # 系统服务（如快捷设置磁贴）
│   │   │   ├── ui/                     # Compose 界面层
│   │   │   │   ├── MainActivity.kt     #   主 Activity 与导航
│   │   │   │   ├── page/               #   各功能设置页面
│   │   │   │   ├── component/          #   通用 UI 组件
│   │   │   │   └── dialog/             #   对话框
│   │   │   └── utils/                  # 工具类（Prefs、DexKit 等）
│   │   ├── res/                        # 资源：多语言字符串、图标、模块作用域数组
│   │   └── resources/META-INF/xposed/  # Xposed 元数据（scope.list 作用域列表）
│   └── build.gradle.kts                # 主模块构建脚本
│
├── hyperx-compose/                     # Compose UI 组件库（Git 子模块）
│   └── src/main/kotlin/dev/lackluster/hyperx/compose/
│       ├── activity/                   #   Activity 基类与安全 SharedPreferences
│       ├── base/                       #   页面基类 BasePage
│       ├── preference/                 #   开关、下拉、滑杆等设置项组件
│       ├── component/                  #   通用组件
│       ├── navigation/                 #   导航工具
│       └── theme/                      #   主题与配色
│
├── gradle/                             # Gradle Wrapper 与版本目录（libs.versions.toml）
├── img/                                # 文档用图
├── build.gradle.kts                    # 根项目构建脚本
├── settings.gradle.kts                 # 模块声明
└── gradle.properties                   # 构建属性
```

### 新增一个功能的典型改动位置

1. `data/Pref.kt` — 定义开关键
2. `hook/rules/<目标应用>/` — 编写 Hook 实现
3. `hook/apps/` — 在对应应用加载器中注册 `loadHooker(...)`
4. `data/Scope.kt`、`resources/META-INF/xposed/scope.list`、`res/values/array.xml` — 若 Hook 新应用，登记其作用域
5. `ui/page/` — 添加设置界面项
6. `data/SearchIndex.kt` — 添加搜索条目
7. `res/values*/strings.xml` — 添加多语言字符串
