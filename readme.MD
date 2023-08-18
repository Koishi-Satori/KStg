# KKoishi STG Engine

这款STG引擎为一个轻量的STG引擎，其使用Java2D和Java2D自带的OpenGL硬件加速进行渲染。 它可以让你较轻松的实现一个stg的基本功能。

This STG engine is a lightweight STG engine, which uses Java2D and the hardware acceleration implemented by OpenGL in Java2D for rendering, and it allows you to implement the basic functions of a STG easily.

开启硬件加速的JVM参数:

JVM parameters for enabling hardware acceleration:
```vmoptions 
-Dsun.java2d.ddscale=true
-Dsun.java2d.opengl=true
-Dswing.aatext=true
-Dawt.nativeDoubleBuffering=true
```
或者使用下面的main方法模板:

Or use the following main method template:
```kotlin
//kotlin
object Test {
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("sun.java2d.ddscale", "true")
        System.setProperty("sun.java2d.opengl", "true")
        System.setProperty("swing.aatext", "true")
        System.setProperty("awt.nativeDoubleBuffering", "true")
        // your codes
    }
}
```
```java
//java
public class Test {
    public static void main (String[] args) {
        System.setProperty("sun.java2d.ddscale", "true");
        System.setProperty("sun.java2d.opengl", "true");
        System.setProperty("swing.aatext", "true");
        System.setProperty("awt.nativeDoubleBuffering", "true");
        // your codes
    }
}
```

## ⚠ 注意事项
> 本项目仅为个人兴趣而制作，开发目的在于学习和探索，一切开发皆在学习，请勿用于非法用途。因使用本项目产生的一切问题与后果由使用者自行承担，项目开发者不承担任何责任。
> 
> 测试用例的素材仅为提供效果参考。

> This project is only made for personal interest. The purpose of development is to learn and explore. All development is for learning. Please do not use it for illegal purposes. 
> All problems and consequences arising from the use of this project shall be borne by the user, and the project developer shall not bear any responsibility. 
>
> The material of the test case is only for reference.

## 🕑 启动方法
- 请保证您具有一定的jvm语言基础。
- 在编译c++实现的启动程序前，请修改jar包路径等部分代码为您需要的内容。
- 下面的仅为说明示例，具体实现请参考```top.kkoishi.stg.test.Test```
- 正常构建jar包后(直接放class文件也行，你随意:D)，可以自己实现一套启动流程/启动脚本，也可以修改并编译在cpp_bootstrapper目录中提供的启动程序。

- Please ensure that you have a certain jvm language foundation.
- Before compiling the startup program implemented by c++, please modify some codes such as the jar path to the content you need.
- The following codes is just an example, please refer to the detailed implementation```top.kkoishi.stg.test.Test```
- After building and packaging the jar file normally (you can directly use the class files:D), you can implement the startup processes or startup scripts, or just simply modify and compile the startup program provided in the cpp_bootstrapper directory.
```kotlin
//kotlin
val f = JFrame("KKoishi_ STG Engine test")
// custom the JFrame
f.isVisible = true
// KeyBinds, you can use your key binds, and this is just an example
PlayerManager.keyBinds(
    f,
    Player.VK_C,
    Player.VK_DOWN,
    Player.VK_ESCAPE,
    Player.VK_LEFT,
    Player.VK_RIGHT,
    Player.VK_SHIFT,
    Player.VK_UP,
    Player.VK_X,
    Player.VK_Z,
    KeyEvent.VK_F11
)
Graphics.refresh(f)

// init menu, or stages and so on.
// next step is set the game state.
GenericFlags.gameState.set(GenericFlags.STATE_MENU)

val inst = Threads.getInstance()
InfoSystem.start(inst)
Renderer.start(inst)
GameLoop.start(inst)
AudioPlayer.start(inst)
```
- ### 注意事项
> - 自定义JFrame时，若您需要全屏显示，应该先使用```setUndecorated(true)```，
> 然后用Graphics中的setScreenSize setBufferSize去正确设置大小，
> 最后让JFrame全屏并且调用Render.fullScreen()。
> - 如果您需要添加SideBar，还需要使用Graphics.setUIInsets去设置UI边框大小。

(README未完成)