# Shizuku 客户端与 provider：release 混淆会破坏 Binder 和权限发现。
-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }

# AIDL 生成的接口与 Stub：跨进程调用依赖类名和签名。
-keep interface moe.xzr.fivegtile.IFivegController { *; }
-keep class moe.xzr.fivegtile.IFivegController$* { *; }

# RootService 与 UserService：Manifest 按类名绑定，Shizuku 通过反射实例化。
-keep class moe.xzr.fivegtile.FivegControllerService { *; }
-keep class moe.xzr.fivegtile.ShizukuControllerService { *; }
-keep class moe.xzr.fivegtile.FivegTileService { *; }
