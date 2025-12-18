// Main.java
package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public class Main extends JavaPlugin {

    private FileConfiguration itemsConfig;
    private File itemsFile;

    private GameManager gameManager;
    private comPass compass;
    private GameListener gameListener;
    private RecoveryCompass recoveryCompass;
    private SpecialItem specialItem;
    private GoatHorn goatHorn;
    private FireBall fireBall;
    private BeeGun beeGun;
    private MagicBlock magicBlock;
    private Chain chain;
    private IronMan ironMan;
    private Twist twist;
    private Skull skull;
    private DragonSlayerRanking dragonSlayerRanking;
    private IncraseProbability incraseProbability;
    private Sensor sensor;
    private Brush brush;
    private ServerInfoDisplay serverInfoDisplay;
    private String serverId;
    private int gameMode;
    private PiglinBarterListener piglinBarterListener;
    private GameCommandExecutor gameCommandExecutor;
    private Vote vote;
    private PlayerData playerData;


    //getServerId() 方法会直接从配置文件中读取 "server-id" 的值
    // 如果配置文件中没有设置这个值，它会返回 "默认服务器" 作为默认值
    public String getServerId() {
        return getConfig().getString("server-id", "mc.mooncookie.cn");
    }

    public int getGameMode() {
        return getConfig().getInt("gameMode", 0);
    }

    public GameManager getGameManager() {
        return gameManager;
    }



    public RecoveryCompass getRecoveryCompass() {
        return recoveryCompass;
    }

    public PlayerData getPlayerData() {
        return playerData;
    }

    @Override
    public void onEnable() {

        loadItemsConfig();

        //如果没有插件文件夹，创建插件文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        //如果没有config.yml文件，用config.yml的文件来创建
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }
        //如果存在config.yml文件
        else{
            serverId = getServerId();
            gameMode = getGameMode();
        }

        // 加载配置
        reloadConfig();

        //复制数据包
        //copyDatapacksToAllWorlds();
        //enableDatapack2();

        // 创建实例
        recoveryCompass = new RecoveryCompass(this);
        this.getCommand("runner").setExecutor(recoveryCompass);
        this.getCommand("unrunner").setExecutor(recoveryCompass);
       // this.getCommand("hushenfu").setExecutor(recoveryCompass);
        gameManager = new GameManager(this);
        compass = new comPass(this);
        //将 "compass" 命令的执行器设置为 compass 实例。确保在 plugin.yml 中已定义此命令。
        this.getCommand("compass").setExecutor(compass);
        this.getCommand("hushenfu").setExecutor(compass);
        playerData = new PlayerData(this);
        gameListener = new GameListener(gameManager,this, playerData);
        specialItem = new SpecialItem(this);
        goatHorn = new GoatHorn(this);
        fireBall = new FireBall(this);
        beeGun = new BeeGun(this);
        magicBlock = new MagicBlock(this);
        chain = new Chain(this);
        ironMan = new IronMan(this);
        twist = new Twist(this);
        skull = new Skull(this);
        dragonSlayerRanking = new DragonSlayerRanking(this);
        incraseProbability = new IncraseProbability(this);
        sensor = new Sensor(this);
        brush = new Brush(this);
        piglinBarterListener = new PiglinBarterListener(this);
        gameCommandExecutor = new GameCommandExecutor(gameListener);
        this.getCommand("gotoend").setExecutor(new GameCommandExecutor(gameListener));
        vote = new Vote(this);

        // 设置相互引用
        gameManager.setCompass(compass);
        compass.setGameManager(gameManager);

        getServer().getPluginManager().registerEvents(gameListener, this);
        getServer().getPluginManager().registerEvents(compass, this);
        getServer().getPluginManager().registerEvents(recoveryCompass, this);
        getServer().getPluginManager().registerEvents(specialItem, this);
        getServer().getPluginManager().registerEvents(goatHorn, this);
        getServer().getPluginManager().registerEvents(fireBall, this);
        getServer().getPluginManager().registerEvents(beeGun, this);
        getServer().getPluginManager().registerEvents(magicBlock, this);
        getServer().getPluginManager().registerEvents(chain, this);
        getServer().getPluginManager().registerEvents(ironMan, this);
        getServer().getPluginManager().registerEvents(twist, this);
        getServer().getPluginManager().registerEvents(skull, this);
        getServer().getPluginManager().registerEvents(piglinBarterListener, this);

        // 注册命令
        this.getCommand("show").setExecutor(new StatsCommand(this, playerData));


        getLogger().info("EndHunter插件已启用");
    }


    @Override
    public void onDisable() {
        if (playerData != null) {
            playerData.closeConnection();
        }
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File world = new File(getDataFolder(), "..\\..\\world");
            deleteFile(world);
            world = new File(getDataFolder(), "..\\..\\world_nether");
            deleteFile(world);
            world = new File(getDataFolder(), "..\\..\\world_the_end");
            deleteFile(world);
            System.out.println("地图清理完成");
        }));

        getLogger().info("EndHunter plugin被禁用了!");
    }

    public void loadItemsConfig() {
        itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            // 如果文件不存在，从jar包中复制默认的items.yml
            saveResource("items.yml", false);
            getLogger().info("items.yml not found, created a default one.");
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        getLogger().info("items.yml loaded successfully.");
    }

    public FileConfiguration getItemsConfig() {
        if (itemsConfig == null) {
            loadItemsConfig(); // 以防万一
        }
        return itemsConfig;
    }

    public void saveItemsConfig() {
        if (itemsConfig == null || itemsFile == null) {
            return;
        }
        try {
            getItemsConfig().save(itemsFile);
        } catch (IOException ex) {
            getLogger().severe("Could not save config to " + itemsFile + ": " + ex.getMessage());
        }
    }

    public void reloadItemsConfig() {
        if (itemsFile == null) {
            itemsFile = new File(getDataFolder(), "items.yml");
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        getLogger().info("items.yml reloaded.");
    }

    public static void DeleteFile(File file) {
        if (file.isDirectory()) {
            //是目录
            if (file.delete()) {
                return;
            }
            File[] files = file.listFiles();
            for (File value : files) {
                DeleteFile(value);
            }
        } else {
            //是文件
            file.delete();
        }
    }

    public static boolean deleteFile(File file) {
        if (file.isDirectory()) {
            // 是目录
            File[] contents = file.listFiles();
            if (contents != null) {
                for (File f : contents) {
                    if (!deleteFile(f)) {
                        return false;
                    }
                }
            }
        }
        // 删除文件或空目录
        return file.delete();
    }

    public void copyDatapacksToAllWorlds(){
        getLogger().info("开始复制数据包到主世界");
        World world = getServer().getWorld("world");
        copyDatapacksToWorld(world);
        getLogger().info("复制数据包到主世界已完成");
    }

    private void copyDatapacksToWorld(World world) {
        getLogger().info("尝试把数据包复制到世界: " + world.getName());

        //worldFolder为world这个文件夹(路径)
        File worldFolder = world.getWorldFolder();
        //datapacksFolder为world文件夹下的datapacks文件夹(路径)
        File datapacksFolder = new File(worldFolder, "datapacks");
        getLogger().info("目标文件夹为: " + datapacksFolder.getAbsolutePath());

        if (!datapacksFolder.exists()) {
            //是否新建了这个文件夹
            boolean created = datapacksFolder.mkdirs();
            if (created) {
                getLogger().info("新建了datapacks文件夹" + world.getName());
            } else {
                getLogger().severe("没有新建datapacks文件夹" + world.getName());
                return;
            }
        }

        //sourceDatapackDir为插件文件夹下的datapacks文件夹(路径)
        File sourceDatapackDir = new File(getDataFolder().getParentFile(), "datapacks");
       getLogger().info("源文件夹为: " + sourceDatapackDir.getAbsolutePath());

        if (sourceDatapackDir.exists() && sourceDatapackDir.isDirectory()) {
            try {
                int copiedCount = copyDatapacks(sourceDatapackDir, datapacksFolder);
                getLogger().info("Copied " + copiedCount + " datapacks to world: " + world.getName());

                // 启用复制的数据包
                enableDatapacks(datapacksFolder);
            } catch (IOException e) {
                getLogger().severe("Error occurred while copying datapacks to " + world.getName() +
                        ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            getLogger().warning("Source datapacks folder does not exist or is not a directory: " + sourceDatapackDir.getAbsolutePath());
        }
    }

    private void enableDatapacks(File datapacksFolder) {
        File[] datapacks = datapacksFolder.listFiles((dir, name) -> name.endsWith(".zip") || new File(dir, name).isDirectory());
        if (datapacks != null) {
            for (File datapack : datapacks) {
                String datapackName = datapack.getName();
                if (datapack.isDirectory()) {
                    datapackName = "file/" + datapackName;
                } else if (datapackName.endsWith(".zip")) {
                    datapackName = "file/" + datapackName.substring(0, datapackName.length() - 4);
                }

                String command = "datapack enable \"" + datapackName + "\"";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                getLogger().info("Enabled datapack: " + datapackName);
            }
        }
    }

    public int copyDatapacks(File source, File target) throws IOException {
        int copiedCount = 0;
        File[] files = source.listFiles();
        if (files == null) {
            getLogger().warning("源文件夹没东西:" + source.getAbsolutePath());
            return 0;
        }

        for (File file : files) {
            File destFile = new File(target, file.getName());
            if (file.isDirectory()) {
                if (!destFile.exists() && !destFile.mkdirs()) {
                    getLogger().warning("没能创造目录: " + destFile.getAbsolutePath());
                    continue;
                }
                copiedCount += copyDatapacks(file, destFile);
            } else {
                try {
                    Files.copy(file.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    copiedCount++;
                    getLogger().info("Copied file: " + file.getName() + " to " + destFile.getAbsolutePath());
                } catch (IOException e) {
                    getLogger().warning("Failed to copy file: " + file.getName() + ". Error: " + e.getMessage());
                    throw e; // 重新抛出异常，让调用者知道发生了错误
                }
            }
        }
        return copiedCount;
    }


    private void enableDatapack(String datapackName) {
        String command = "datapack enable \"file/" + datapackName + "\"";
        getLogger().info("尝试启用数据包: " + command);
        boolean success = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (success) {
            getLogger().info("成功启用数据包: " + datapackName);
        } else {
            getLogger().warning("无法启用数据包: " + datapackName);
        }
    }

    private void enableDatapack2() {
        String command = "datapack enable \"file/123.zip\"";
        final String datapackName = "123.zip";
        final int maxAttempts = 2; // 最大尝试次数
        final long delayBetweenAttempts = 20L; // 每次尝试之间的延迟（ticks）

        getLogger().info("尝试启用数据包: " + command);

        new BukkitRunnable() {
            int attempts = 0;

            @Override
            public void run() {
                attempts++;
                getLogger().info("尝试启用数据包 (尝试 " + attempts + "/" + maxAttempts + ")");

                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "datapack list");

                // 检查数据包是否已启用
//                if (isDatapackEnabled(datapackName)) {
//                    getLogger().info("数据包 " + datapackName + " 已成功启用！");
//                    this.cancel(); // 停止重复尝试
//                }
                 if (attempts >= maxAttempts) {
                    getLogger().warning("已尝试2次");
                    this.cancel(); // 停止重复尝试
                }
            }
        }.runTaskTimer(this, 0L, delayBetweenAttempts);

    }











}

