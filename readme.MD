# ![EngineIcon](./src/main/resources/top/kkoishi/stg/logo.ico) KKoishi STG Engine

这款STG引擎为一个轻量的STG引擎，其使用Java2D和Java2D自带的OpenGL硬件加速进行渲染。 它可以让你较轻松的实现一个stg的基本功能。

This STG engine is a lightweight STG engine, which uses Java2D and the hardware acceleration implemented by OpenGL in
Java2D for rendering, and it allows you to implement the basic functions of a STG easily.

开启硬件加速的JVM参数:

JVM parameters for enabling hardware acceleration:

```vmoptions 
// https://docs.oracle.com/javase/8/docs/technotes/guides/2d/flags.html
-Dsun.java2d.ddscale=true
-Dsun.java2d.opengl=true
-Dswing.aatext=true
-Dawt.nativeDoubleBuffering=true
```

或者在main方法最开始调用```Bootstrapper::enableHardwareAccelerationProperties```方法。

Or call the ```Bootstrapper::enableHardwareAccelerationProperties``` method at the beginning of the main method.

## ⚠ 注意事项

> 本项目仅为个人兴趣而制作，开发目的在于学习和探索，一切开发皆在学习，请勿用于非法用途。因使用本项目产生的一切问题与后果由使用者自行承担，项目开发者不承担任何责任。
>
> 测试用例的素材仅为提供效果参考。

> This project is only made for personal interest. The purpose of development is to learn and explore. All development
> is for learning. Please do not use it for illegal purposes.
> All problems and consequences arising from the use of this project shall be borne by the user, and the project
> developer shall not bear any responsibility.
>
> The material of the test case is only for reference.

## 🕑 启动方法

本引擎提供了一个类```top.kkoishi.stg.boot.Bootstrapper```来快速启动，只需要调用方法去设置里面的各项参数即可。

This engine provides a class ```top.kkoishi.stg.boot.Bootstrapper``` to start quickly, so you can just call the method
to set the various parameters inside.

- 请保证您具有一定的jvm语言基础。
- 在编译c++实现的启动程序前，请修改jar包路径等部分代码为您需要的内容。
- 下面的仅为说明示例，具体实现请参考```top.kkoishi.stg.test.Test```
- 正常构建jar包后(直接放class文件也行，你随意:D)，可以自己实现一套启动流程/启动脚本，也可以修改并编译在cpp_bootstrapper目录中提供的启动程序。

- Please ensure that you have a certain jvm language foundation.
- Before compiling the startup program implemented by c++, please modify some codes such as the jar path to the content
  you need.
- The following codes is just an example, please refer to the detailed implementation```top.kkoishi.stg.test.Test```
- After building and packaging the jar file normally (you can directly use the class files:D), you can implement the
  startup processes or startup scripts, or just simply modify and compile the startup program provided in the
  cpp_bootstrapper directory.

```kotlin
//kotlin
Bootstrapper.readEngineSettings()
Bootstrapper().size(WIDTH, HEIGHT).autoSync().containerTitle("KKoishi_ Stg Engine Test")
    .fullscreen(fullScreen).scale(scale).useEngineDefaultIcon().useVRAM(useVRAM).uiInsets(
        UI_INSETS.top, UI_INSETS.left, UI_INSETS.bottom, UI_INSETS.right
    ).append(GFXLoader("${Threads.workdir()}/test/gfx"))
    .append(AudioLoader("${Threads.workdir()}/test/audio")).initMethod {
        // input your init method.
        // this method will be invoked after the container is built.
    }.start()
```

### 💫 Bootstrapper中的各项参数说明

|         参数         |                                                                 功能                                                                 |    修改方法    | 备注                                                                                                                                                                                                                                                                                                               |
|:------------------:|:----------------------------------------------------------------------------------------------------------------------------------:|:----------:|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|       height       |                                              设置窗口的高度<br/>Set the height of the window                                              |    size    | 需要同时提供高度和宽度<br/>Need to provide both height and width                                                                                                                                                                                                                                                            |
|       weight       |                                              设置窗口的宽度<br/>Set the width of the window                                               |    size    | 需要同时提供高度和宽度<br/>Need to provide both height and width                                                                                                                                                                                                                                                            |
|      useVRAM       |                                 是否使用GPU显存进行双缓冲<br/>Whether to use GPU memory for double buffering                                  |  useVRAM   | 传入一个bool，决定屏幕缓冲内容是否使用VRAM储存<br/>Pass in a bool to determine whether the screen buffer content should be stored in VRAM.                                                                                                                                                                                          |
|        icon        |                                                    设置窗口的图标<br/>Set window icon                                                     |    icon    | 传入图标图像文件所在的位置。可以使用useEngineDefaultIcon替代，这将会把图标换成[引擎的默认图标](./src/main/resources/top/kkoishi/stg/logo.ico)<br/>Pass in the location of the icon image file. You can use use Engine Default Icon instead, which will change the icon to [the engine's default icon](./src/main/resources/top/kkoishi/stg/logo.ico) |
|      autoSync      | 在窗口重新获得焦点后，是否同步逻辑帧率与渲染帧率<br/>Whether to synchronize the logical frame rate and rendering frame rate after the window regains focus |  autoSync  | 传入一个bool，开启后可以避免在失去焦点后在显示帧率时有误<br/>Pass in a bool. When turned on, it can avoid errors in displaying the frame rate after losing focus.                                                                                                                                                                          |
|       scale        |                                    设置窗口放大到的大小<br/>Set the size to which the window is enlarged                                     |   scale    | 传入放大后的窗口宽度和高度，此选项在全屏模式开启后会被屏蔽<br/>Pass in the enlarged window width and height, this option will be blocked after the full screen mode is enabled                                                                                                                                                                |
|     fullscreen     |                                                 开启全屏模式<br/>Enable full screen mode                                                 | fullscreen | 传入一个bool，决定是否开启全屏。全屏可能会提高渲染效率也可能不会，取决于平台是否支持full-screen exclusive mode<br/>Pass in a bool to decide whether to enable full screen. Full screen may or may not improve rendering efficiency, depending on whether the platform supports full-screen exclusive mode                                                |
|     initMethod     |                            窗口完成初始化后执行的方法<br/>Method executed after the window completes initialization                             | initMethod | 传入lambda表达式或者方法引用<br/>Pass in lambda expression or method reference                                                                                                                                                                                                                                              |
|       title        |                                                    设置窗口的标题<br/>Set window title                                                    |   title    | 传入字符串<br/>Pass in a string                                                                                                                                                                                                                                                                                       |
|     container      |                                                      设置窗口<br/>Set the window                                                       | container  | 传入JFrame或者其子类的实例。不建议修改，默认使用的是引擎自带的OptimizedContainer<br/>Pass in an instance of JFrame or its subclass, which is not recommended to modify it. What the engine uses by default is the OptimizedContainer.                                                                                                        |
|      uiInsets      |                                     设置侧边栏的间隔Insets<br/>Set the interval Insets of the sidebar                                      |  uiInsets  | 设置侧边栏的Insets。侧边栏就你打stg时旁边显示分数的玩意，这东西包裹了整个可活动区域<br/>Set the Insets for the sidebar. The sidebar is the thing that displays the score next to you when you play stg, and it wraps the entire movable area.                                                                                                         |
| definitionsLoaders |                                                  添加脚本加载器<br/>Add a script loader                                                   |   append   | 添加各种Loader，用于加载引擎的脚本。<br/>Add various Loaders for loading engine scripts.                                                                                                                                                                                                                                        |

### 快速上手
- ⚠ 此段中的Object均指[top.kkoishi.stg.common.entities.Object](./src/main/kotlin/top/kkoishi/stg/common/entities/Object.kt)。

本引擎OOP程度较高，基本只需要正确的设置对象和状态即可。 Bootstrapper::initMethod里提供的初始化方法即为您想在最开始时运行的逻辑。

例如：在最开始时显示主菜单，可实现Menu/BaseMenu类，再调用ObjectPool::addUIObject方法把类实例放入（请保证单例性），最后切换gameState为STATE_MENU即可。

不同的gameState需要放入不同的对象，下面是具体说明：

|   gameState   |                    对象类型                    | 调用方法                          |  说明  |
|:-------------:|:------------------------------------------:|:------------------------------|:----:|
| STATE_LOADING |                  任意Object                  | ObjectPool::addLoadingContent | 加载状态 |
|  STATE_MENU   |                 任意UIObject                 | ObjectPool::addUIObject       |  菜单  |
| STATE_PLAYING | 用于表示实体的Object，Bullet，Player，Stage，UIObject |                               |      |

- ### 注意事项

> - 如果您需要添加[SideBar](./src/main/kotlin/top/kkoishi/stg/common/ui/SideBar.kt)
    ，还需要使用Graphics.setUIInsets或者用快速启动类去设置UI边框大小。
> - 需要实现自己的启动流程可以参考```Bootstrapper::start```方法。
> - Bootstrapper的initMethod方法会在窗口创建完成后执行传入的lambda表达式或者function，请在里面设置您自己的初始化代码，并且最后切换```GenericSystem.gameState```
至您需要的状态。
> - 调用```Bootstrapper.readEngineSettings()```用于读取引擎有关工作目录等的ini文件，请将[engines.ini](./engine.ini)
    放在当前目录，然后正确设置工作目录。
> - 默认的engines.ini内容将工作目录定义为jar包所在目录的上一级。

> - If you need to add [SideBar](./src/main/kotlin/top/kkoishi/stg/common/ui/SideBar.kt), you also need to use
    Graphics.setUIInsets or use the Quick Start class to set the UI border size.
> - If you want to implement your own startup process, you can refer to the ```Bootstrapper::start``` method.
> - Bootstrapper's initMethod method will execute the passed lambda expression or function after the window is created.
    Please put your own initialization code in it, and finally switch ```GenericSystem.gameState``` to the state you
    need.
> - ```Bootstrapper.readEngineSettings()``` is used to read the ini file of the engine about the working directory, etc.
    Please put [engines.ini](./engine.ini) in the current directory, and then set the working directory correctly.
> - The default engines.ini content defines the working directory as the upper level of the directory where the jar
    package is located.

- ### 逻辑

> - [GenericFlags](./src/main/kotlin/top/kkoishi/stg/logic/GenericSystem.kt).gameState用于标识当前游戏状态。
> - 当其为GenericFlags.STATE_MENU时，只有[ObjectPool](./src/main/kotlin/top/kkoishi/stg/logic/ObjectPool.kt)
    .uiObject中的实例会被渲染。
> - 当其为GenericFlags.PLAYING时，ObjectPool中所有对象实例以及player均会被渲染。
> - 您需要做的就是在切换状态之前正确的把实例一股脑丢进去即可。
> - 本引擎提供了一套脚本来加载材质音频等。
> ---
>
> - 实现基本要求的类均位于[top.kkoishi.stg.common](./src/main/kotlin/top/kkoishi/stg/common/package-info.java)
    包内，（通常）只用继承实现他们的逻辑。
> - top.kkoishi.stg.common中所有可渲染对象都实现接口top.kkoishi.stg.common.entities.Object，包含更新逻辑、碰撞检测及渲染。
> - 所有实体（[Player](./src/main/kotlin/top/kkoishi/stg/common/entities/Player.kt), [Boss](./src/main/kotlin/top/kkoishi/stg/common/entities/Boss.kt), [Enemy](./src/main/kotlin/top/kkoishi/stg/common/entities/Enemy.kt), [Item](./src/main/kotlin/top/kkoishi/stg/common/entities/Item.kt), [Bullet](./src/main/kotlin/top/kkoishi/stg/common/bullets/Bullet.kt)
）都在top.kkoishi.stg.common.entities包内。
> - 上述包内有一些抽象类（以Base/Abstract开头）
中）来降低实现难度，而除了Bullet这些类均继承了Entity类。
> - top.kkoishi.stg.common.ui包负责ui渲染，提供了一系列类来渲染ui，只需要实现并丢进ObjectPool.uiObject。
> - 其余的类为游戏逻辑，包括boss逻辑BossAction、Stage及其逻辑StageAction和类ScreenRenderObject。
> ---
>
> - 引擎整体逻辑为先渲染、然后计算，同时并行播放audio，大体包含四个主要线程InfoSystem、Renderer、GameLoop、AudioPlayer和一个CrashHandle子进程（可选）

## 🔠 按键绑定

KeyBinds类用于管理全局的按键绑定，

## 🅰 处理程序参数

本引擎提供了[Options](./src/main/kotlin/top/kkoishi/stg/util/Options.kt)
类和[Option](./src/main/kotlin/top/kkoishi/stg/util/Option.kt)类来快速处理程序参数，实现方式参考了javap的参数处理部分。
您只需要将自己的Option用```Options::addOption```添加到Options中，再调用load方法即可处理程序参数。
默认有一个参数```-debug```用于打开详细日志输出。

(README未完成)

## 线程异常处理和单一实例

## [脚本说明](./test/scripts.md)

## 引擎结构
```text
top.kkoishi.stg
             ├─audio - 音频
             ├─boot - 快速启动相关
             │  ├─jvm - 废弃
             │  └─ui - 启动ui
             ├─common - 绝大部分需要继承的抽象类/接口
             │  ├─bullets - 子弹
             │  ├─entities - 实体
             │  └─ui - sidebar menu等ui
             ├─exceptions - 异常及其处理
             ├─gfx - 纹理等渲染逻辑
             ├─localization - 本地化
             ├─logic - 引擎计算逻辑
             │  ├─coordinatespace - 用于优化碰撞
             │  └─keys - 控制全局按键检测
             ├─replay - replay记录播放
             ├─script - 引擎脚本支持
             │  ├─execution - 脚本执行相关
             │  └─reflect - jvm反射
             └─util - 工具类
```

**最后 uuz真的很棒 给大家看看**
![uuz](./test/load.jpg)