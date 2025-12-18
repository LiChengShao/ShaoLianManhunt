# ShaoLian猎人游戏插件
这是一个适用于 Minecraft Spigot/Paper 服务器（**Minecraft 版本：1.21.4**）的服务端猎人游戏插件，提供多种可自定义的游戏选项。

# 📋 必需依赖项
要正常运行此插件，必须在您的服务器上安装以下 JAR 文件（请将它们放置在 `plugins/` 目录中）：

- DecentHolograms-2.8.16.jar

- LibsDisguises-10.0.44-Free.jar

- LoneLibs_1.0.65.jar

- PlaceholderAPI-2.11.6.jar
  
- packetevents-spigot-2.11.0.jar([LibsDisguises插件会尝试自己下载，也可以自己下载)

- ProtocolLib.jar（注意：请确保版本与 Minecraft 1.21.4 兼容）

# 🚀 功能介绍
六种玩法，多个游戏自定义配置，玩家击杀/胜利/死亡的数据库(SQlite),详细介绍请见：
https://afdian.com/item/812f2ee82ef411f0b89b52540025c377

# 📝 其他说明
插件灵感来自油管的千万粉丝Dream开创的猎人追捕
这是我制作的第一款mc插件，代码可能存在不规范，但是此插件经过上百局和朋友的游玩后，可以保证游戏体验极好，感谢!

# 📢 一些注意事项
1.游戏中的自定义头盔需要下载材质包：https://wwauz.lanzn.com/i0TQV3dymywj 并且使用1.21.4才可以看到
2.游戏开始前玩家进入默认会进入大厅，可以注意到服务端根目录有lobby世界文件夹，可自行修改做出你想要的大厅
3.插件文件夹会生成玩家的击杀/胜利/死亡数据库
4.将服务端根目录的配置文件server.properties的spawn protect改成0，防止玩家无法在出生点附近破坏方块
5.批处理文件（启动脚本）可以修改为如下格式：
@echo off
:restart
echo 正在启动服务器...
"C:\Program Files\Java\jdk-21\bin\java.exe" -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=127.0.0.1 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 -Xmx4096M -Xms1024M -jar paper-1.21.4-225.jar nogui
echo 服务器已关闭，5 秒后重启...
timeout /t 5 /nobreak >nul
goto restart
这将在服务器被中断后5s重新自动运行。当游戏结束后，插件会删除世界存档并关闭服务器，这段启动脚本将使得服务器完全自动化


# ShaoLianManhunt
This is a server-side Manhunt game plugin for Minecraft Spigot/Paper servers (**Minecraft Version: 1.21.4**), featuring multiple customizable gameplay options.

# 📋 Required Dependencies
To run this plugin properly, the following JAR files must be installed on your server (place them in the `plugins/` directory):
- DecentHolograms-2.8.16.jar
- LibsDisguises-10.0.44-Free.jar
- LoneLibs_1.0.65.jar
- PlaceholderAPI-2.11.6.jar
- packetevents-spigot-2.11.0.jar(The LibsDisguises plugin will attempt to download itself, or it can download itself.)
- ProtocolLib.jar (note: ensure the version is compatible with Minecraft 1.21.4)

 
# 🚀 Feature Introduction
This plugin offers 6 gameplay modes and supports multiple custom game configurations. It uses SQLite to store player data such as kills, victories, and deaths. For detailed information, please visit:https://afdian.com/item/812f2ee82ef411f0b89b52540025c377
# 📝 Additional Notes
The plugin draws inspiration from the "Manhunt" gameplay created by Dream, a YouTuber with millions of subscribers.
This is my first Minecraft plugin. While the code may not be perfectly standardized, it has been tested through over 100 gameplay sessions with friends—ensuring a smooth and enjoyable gaming experience,Thanks!

# 📢 Precautions
1.Custom helmets in the game require downloading a resource pack: https://wwauz.lanzn.com/i0TQV3dymywj. They are only visible when using Minecraft 1.21.4.
2.Players will automatically enter the lobby before the game starts. You may notice a lobby world folder in the server root directory—feel free to modify it to create your desired lobby.
3.A database storing players' kills, victories, and deaths will be generated in the plugin folder.
4.Set spawn-protect to 0 in the server.properties configuration file (located in the server root directory) to prevent players from being unable to break blocks near the spawn point.
5.The batch file (startup script) can be modified to the following format:
@echo off
:restart
echo Starting the server...
"C:\Program Files\Java\jdk-21\bin\java.exe" -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyHost=127.0.0.1 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897 -Xmx4096M -Xms1024M -jar paper-1.21.4-225.jar nogui
echo Server closed. Restarting in 5 seconds...
timeout /t 5 /nobreak >nul
goto restart
This script will automatically restart the server 5 seconds after it is interrupted. When a game ends, the plugin will delete the world save files and shut down the server—this startup script enables full automation of the server.
