# 保持库的所有接口和类名不被混淆
# 因为使用者需要直接引用 EasyUpdater, UpdateEntity, IUpdateParser 等类
-keep class com.github.yjz.easyupdater.** { *; }
-keep interface com.github.yjz.easyupdater.** { *; }
# 如果你的库使用了反射或者需要在 XML 中引用 (如 UpdaterFileProvider)，也必须保持
-keep class com.github.yjz.easyupdater.UpdaterFileProvider