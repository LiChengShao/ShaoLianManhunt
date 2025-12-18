// GameManager.java
package com.shaolian.manhunt;


import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.GameMode;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class GameManager  {
    public final Main plugin;
    private comPass compass;
    private RecoveryCompass recoveryCompass;
    private SpecialItem specialItem;
    private GoatHorn goatHorn;
    private FireBall fireBall;
    private BeeGun beeGun;
    private MagicBlock magicBlock;
    private TeamManager teamManager;
    private Chain chain;
    private IronMan ironMan;
    private Twist twist;
    private Skull skull;
    private Sensor sensor;
    private Brush brush;
    private TP tp;
    private PlayerVisibilityManager playerVisibilityManager;
    private Hats hats;
    private Hats2 hats2;
    private Feather featherInstance;


    public static final Set<UUID>  readyPlayers = new HashSet<>();
    public static final Set<UUID> hunters = new HashSet<>();
    public static final Set<UUID> runners = new HashSet<>();
    public static final Set<UUID> deathRunners = new HashSet<>();
    public static UUID ghast = null;

    public static boolean isGameRunning;
    private boolean isCountdownRunning = false;
    private int countdownTask = -1;
    private Location spawnLocation;
    public Location gameStartLocation = new Location(Bukkit.getWorld("world_the_end"),
            8, 61, -14);
    private final Map<UUID, BukkitTask> offlineTimers = new HashMap<>();
    public boolean movementRestricted = false;
    public boolean damage = false;
    public static boolean allowback = true;
    private final boolean AutoRestart = true;
    private Scoreboard scoreboard;
    private Team hunterTeam;
    private Team runnerTeam;
    public static Team ghastTeam;

    public final Location loc1;
    public Location netherCenter;
    public Location loc9;
    public Location loc6;
    public static World lobbyWorld;

    private List<ItemStack> items = new ArrayList<>();
    private Random random = new Random();
    // 为逃亡者生成随机位置
    // TODO
    //Location runnerLocation = getRandomLocationInRange(-10, 10, -10, 10);
    Location runnerLocation = getRandomLocationInRange(-1500, 1500, -1500, 1500);
    Location runnerLocation_ZhuboMode = getRandomLocationInRange(250, 260, 120, 130);

    // 在游戏开始时初始化
    private final List<Location> teleportLocations = new ArrayList<>();


    //被使用的时候:
    //当你的 Minecraft 插件（在这个例子中是 Main 类）被服务器加载和启用时，
    // 通常会创建 GameManager 的实例。这通常发生在插件的 onEnable() 方法中。
    public GameManager(Main plugin) {
        this.plugin = plugin;
        this.recoveryCompass = plugin.getRecoveryCompass();
        this.specialItem = new SpecialItem(plugin);
        this.goatHorn = new GoatHorn(plugin);
        this.fireBall = new FireBall(plugin);
        this.beeGun = new BeeGun(plugin);
        this.magicBlock = new MagicBlock(plugin);
        this.teamManager = new TeamManager(plugin);
        this.chain = new Chain(plugin);
        this.ironMan = new IronMan(plugin);
        this.twist = new Twist(plugin);
        this.skull = new Skull(plugin);
        this.sensor = new Sensor(plugin);
        this.brush = new Brush(plugin);
        this.tp = new TP(plugin);
        plugin.getCommand("tpa").setExecutor(tp);
        this.hats = new Hats(plugin);
        this.hats2 = new Hats2(plugin);
        this.featherInstance = new Feather(plugin);
       // playerVisibilityManager = new PlayerVisibilityManager(plugin);

        //生成或者初始化lobby
        createOrLoadLobbyWorld();

        //清空世界的掉落物
        clearDrops();

        isGameRunning = false;

        // 设置loc1位置
        World overworld = Bukkit.getWorld("world"); // 获取主世界
        if (overworld != null) {
            // 寻找要塞
            this.loc1 = findStrongholdLocation(overworld);
        }
        else {
            // 如果无法获取主世界，使用默认位置或抛出异常
            this.loc1 = null;
            plugin.getLogger().warning("无法获取主世界，使用默认世界的坐标初始化 loc1");
        }
            plugin.getLogger().info("loc1的y坐标为" + loc1.getY());

        loc9 = new Location(Bukkit.getWorld("world"),loc1.getX(),
             201,loc1.getZ());

            //禁止pvp
        for(World world : Bukkit.getWorlds()){
            world.setPVP(false);
        }
        //初始化物品
        initializeItems();

        // 定时夜视效果
        startNightVisionForAllOnlinePlayers();

        //初始化传送点
        //initializeTeleportLocations();


//        if (loc1 != null) {
//            // 使用同步任务来预加载区块
//            Bukkit.getScheduler().runTask(plugin, () -> {
//                preloadChunks(loc1, 2); // 预加载 2 个区块半径，你可以根据需要调整这个值
//            });
//        } else {
//            plugin.getLogger().warning("无法预加载区块：loc1 是 null");
//        }

    }



    public void setCompass(comPass compass) {
        this.compass = compass;
    }

    public void restrictMovement() {
        this.movementRestricted = true;
    }
    public void allowMovement() {
        this.movementRestricted = false;
    }
    public boolean isMovementRestricted() {
        return movementRestricted;
    }


    // 添加设置allowback的方法
    public void setAllowback(boolean allowback) {
        this.allowback = allowback;
    }


    public void disableDamage(){
        this.damage = false;
    }
    public void allowDamage(){
        this.damage = true;
    }
    public boolean isDamageAllowed(){
        return damage;
    }

    private boolean pearlDisabled = false;

    public void disablePearl() {
        pearlDisabled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pearlDisabled = false;
        }, 20 * 15); // 15秒后恢复
    }

    public boolean isPearlDisabled() {
        return pearlDisabled;
    }

    public void playerJoin(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        //如果游戏没开始，将玩家加入到准备玩家列表中
        //playerVisibilityManager.setupCompass(player);

       player.sendMessage(ChatColor.GOLD + "§l如果没有自定义头盔效果，请使用1.21.4版本进入游戏");

        if(!isGameRunning){
            readyPlayers.add(playerId);
            player.setGameMode(GameMode.ADVENTURE);

            lobbyWorld = Bukkit.getWorld("lobby");
            if(lobbyWorld == null){
                plugin.getLogger().info("lobbyWorld为null");
            }

            //玩家传送到大厅
            teleportToLobby(player);

            //检查游戏是否需要开始
            checkGameStart();
            player.sendMessage(ChatColor.LIGHT_PURPLE + "欢迎游玩猎人追杀！");
            plugin.getLogger().info(playerId + "为uuid");

            //清空背包
            player.getInventory().clear();
            PlayerInventory inv = player.getInventory();

            //给予投票选择
            inv.setItem(4,recoveryCompass.createNetherStar());
            inv.setItem(0,hats.createCustomHats());
          //  inv.setItem(1,hats2.createGuiOpenerItem());
            inv.setItem(8,Feather.createFeatherChoose());

            //玩家传送到大厅
            teleportToLobby(player);

        }
        //如果游戏已经开始
        else{
            //如果有身份
            if(runners.contains(playerId) || hunters.contains(playerId)) {
                // 重新应用队伍设置
                addPlayerToTeam(playerId);
                if (RecoveryCompass.finalMode == "主播模式"){
                    compass.startLoreUpdateTask(player);
                }
            }
            //如果不是猎人也不是逃亡者
            else{
                //如果这个玩家是死亡的逃亡者,return
                if(deathRunners.contains(playerId)){
                    // 重新应用队伍设置
                    addPlayerToTeam(playerId);
                    //设置为旁观者
                    player.setGameMode(GameMode.SPECTATOR);
                    return;
                }
                //如果这个玩家是中途才进入的玩家
                else{
                    //如果不是一追多模式
                    if(!RecoveryCompass.finalMode.equals("一追多模式")){
                        hunters.add(playerId);
                        addPlayerToTeam(playerId);
                        player.setGameMode(GameMode.SURVIVAL);
                        //清空背包
                        player.getInventory().clear();
                    }
                    //如果是一追多模式
                    else{
                        runners.add(playerId);
                        addPlayerToTeam(playerId);
                        player.setGameMode(GameMode.SURVIVAL);
                        //清空背包
                        player.getInventory().clear();
                    }
                }

                //如果这个玩家是被投票出局的
                if(Vote.outPlayerId != null && Vote.outPlayerId.equals(playerId)){
                    // 重新应用队伍设置
                    addPlayerToTeam(playerId);
                    return;
                }


                //传送玩家
                if(RecoveryCompass.finalMode.equals("要塞战争")){
                    player.teleport(loc1);
                    //给最大生命值
                    player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40);
                    player.setHealth(40);
                    //设置这个玩家的出生点
                    World world = loc1.getWorld();
                    world.setSpawnLocation(loc1);
                    player.setBedSpawnLocation(loc1, true);
                }
                if(RecoveryCompass.finalMode.equals("原版猎人") ||
                        RecoveryCompass.finalMode.equals("随机掉落原版猎人")
                        || RecoveryCompass.finalMode.equals("内鬼模式")
                        || RecoveryCompass.finalMode.equals("一追多模式"))
                {
                    player.teleport(runnerLocation);
                    //给最大生命值
                    player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30);
                    player.setHealth(30);
                    player.setBedSpawnLocation(runnerLocation, true);
                }
                if(RecoveryCompass.finalMode.equals("主播模式"))
                {
                    player.teleport(runnerLocation_ZhuboMode);
                    //给最大生命值
                    player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30);
                    player.setHealth(30);
                    player.setBedSpawnLocation(runnerLocation, true);
                }


                // 重新应用队伍设置
                addPlayerToTeam(playerId);

                player.setFoodLevel(20);
                player.setSaturation(0);
                player.setFireTicks(0);
                player.setFallDistance(0);
                //给指南针
                PlayerInventory inv = player.getInventory();
                inv.addItem(compass.createTrackingCompass());
            }
        }


    }

    public void playerQuit(UUID playerId) {
        //当游戏没有开始的时候
        if (!isGameRunning) {
            readyPlayers.remove(playerId);
            //检查游戏是否可以开始
            checkGameStart();
        }

        //如果游戏已经开始
        else{
                //如果退出的玩家是猎人
                if (isHunter(playerId)) {
                    //scheduleRoleRemoval(playerId, 999999999); // 1分钟 = 60秒
                    Bukkit.broadcastMessage(ChatColor.RED + "猎人离线不会移除身份");
                }
                if (isRunner(playerId)) {
                    //如果退出的玩家是逃亡者
                    scheduleRoleRemoval(playerId, 60); // 3分钟 = 180秒
                    Bukkit.broadcastMessage(ChatColor.GREEN + "逃亡者离线60s后将会被立即移除身份");
                    //检查游戏是否结束
                    checkGameEnd();
                    checkIfOnlyOneLeft();
                }
        }
    }


    //离线身份移除
    private void scheduleRoleRemoval(UUID playerId, int seconds) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(playerId);
                //十分钟后玩家为空或者不在线
                if (player == null || !player.isOnline()) {
                    if (isRunner(playerId)) {
                        runners.remove(playerId);
                        deathRunners.add(playerId);
                        readyPlayers.remove(playerId);
                        Bukkit.broadcastMessage("§c一名逃亡者已离线超过60s，被移出游戏");
                    }

                    // 检查游戏是否应该结束
                    checkGameEnd();
                }
            }
        }.runTaskLater(plugin, seconds * 20L);
    }

    // 从队伍中移除玩家的辅助方法
    private void removePlayerFromTeam(UUID playerId) {
        String playerName = Bukkit.getOfflinePlayer(playerId).getName();
        if (hunterTeam.hasEntry(playerName)) {
            hunterTeam.removeEntry(playerName);
        }
        if (runnerTeam.hasEntry(playerName)) {
            runnerTeam.removeEntry(playerName);
        }
    }


    private void checkGameStart() {
        //如果游戏正在进行，则不执行任何操作
        if (isGameRunning) return;
        //如果玩家数量少于2，并且没有倒计时正在进行，则开始倒计时
        if (readyPlayers.size() >= 2 && !isCountdownRunning) {
            startCountdown();
        //如果玩家数量少于2，并且倒计时正在进行，则取消倒计时
        } else if (readyPlayers.size() < 2 && isCountdownRunning) {
            cancelCountdown();
        }
    }

    private void startCountdown() {
        //如果游戏开始了，则不执行任何操作
        if (isGameRunning) {
            return;
        }
        //那么以下就是游戏没开始的时候
        //如果有倒计时在运行，则不执行任何操作
        if (isCountdownRunning) {
            return; // 防止多次启动倒计时
        }

        //那么以下就是游戏没有开始并且没有倒计时在运行的时候
        //开始倒计时
        isCountdownRunning = true;

        countdownTask = new BukkitRunnable() {
            int countdown = 120;

            @Override
            public void run() {
                if (countdown <= 0) {
                    this.cancel(); // 取消当前任务

                    Bukkit.getScheduler().cancelTask(countdownTask);

                    isCountdownRunning = false;

                    recoveryCompass.decideFinalGameMode();

                    if(RecoveryCompass.finalMode.equals("要塞战争")){
                        startGame();
                        for(Player player : Bukkit.getOnlinePlayers()) {
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("游戏模式:要塞战争"));
                        }
                    }
                    else if (RecoveryCompass.finalMode.equals("主播模式")){
                        startGame3();
                    }
                    else if(RecoveryCompass.finalMode.equals("原版猎人")
                    || RecoveryCompass.finalMode.equals("随机掉落原版猎人")
                    || RecoveryCompass.finalMode.equals("内鬼模式")
                    || RecoveryCompass.finalMode.equals("一追多模式")){
                        startGame2();
//                        for(Player player : Bukkit.getOnlinePlayers()) {
//                            if(RecoveryCompass.finalMode.equals("原版猎人")){
//                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
//                                        TextComponent.fromLegacyText("游戏模式:原版猎人"));
//                            }
//                            else if(RecoveryCompass.finalMode.equals("随机掉落原版猎人")){
//                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
//                                        TextComponent.fromLegacyText("游戏模式:随机掉落原版猎人"));
//                            }
//                            else if(RecoveryCompass.finalMode.equals("内鬼模式")){
//                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
//                                        TextComponent.fromLegacyText("游戏模式:内鬼模式"));
//                            }
//                            else if(RecoveryCompass.finalMode.equals("一追多模式")){
//                                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
//                                        TextComponent.fromLegacyText("游戏模式:一追多模式"));}
//                        }

                    }

                    return;
                }
                else {
                    String message = ChatColor.AQUA + "游戏将在 " + ChatColor.GOLD + "§l" + countdown + ChatColor.AQUA + " 秒后开始！";
                    //Bukkit.broadcastMessage(ChatColor.YELLOW + "游戏将在 " + countdown + " 秒后开始！");
                    for(Player player : Bukkit.getOnlinePlayers()) {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                    }
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L).getTaskId();
    }

    //取消倒计时
    private void cancelCountdown() {
        if (isCountdownRunning) {
            Bukkit.getScheduler().cancelTask(countdownTask);
            isCountdownRunning = false;
            Bukkit.broadcastMessage(ChatColor.RED + "倒计时已取消！人数不足！");
        }
    }


    //开始游戏
    private void startGame() {
        if (isGameRunning) {
            return; // 如果游戏已经在运行，直接返回
        }
        isGameRunning = true;

        isCountdownRunning = false;

        setAllowback(false);

        //设置最初时间为0
        tp.setGameStartTime();
        compass.setIsSharedBagEnabled();

        //设置世界的出生点
        setPlayerSpawnToLoc1();

        //设置世界的边界
        setWorldBorderToLoc1(150);
        setWorldBorderToLoc1AndNether(100);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String message = ChatColor.GOLD + "TNT可以炸掉黑曜石";
            String message2 = ChatColor.GOLD + "床的伤害削弱了80％";
            for (Player player1 : Bukkit.getOnlinePlayers()) {
                player1.sendMessage(message);
                player1.sendMessage(message2);
            }
        }, 0L, 20L * 240); // 0L 表示立即开始，20L * 60 表示每60秒（1分钟）执行一次

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String message = ChatColor.GOLD + "偶尔会无法自动换种的情况，几局后会自动恢复";
            for (Player player1 : Bukkit.getOnlinePlayers()) {
                player1.sendMessage(message);
            }
        }, 0L, 20L * 480);

        //分配角色
        recoveryCompass.assignRoles();

        //公告身份,延迟一下执行
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.GREEN + "游戏开始！");
                }
                //Bukkit.broadcastMessage(ChatColor.GREEN + "游戏开始！");
                for(UUID playerId : runners) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "你是逃亡者！");
                    }
                }
                for(UUID playerId : hunters) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (playerId.equals(GameManager.ghast)) {
                        player.sendMessage(ChatColor.DARK_PURPLE + "你是内鬼！");
                    }

                    else   {
                        if(player != null){
                            player.sendMessage(ChatColor.RED + "你是猎人！");
                        }
                    }
                }
                for(Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.GOLD + "你将有30s时间准备背包！！！");
                }
                //Bukkit.broadcastMessage(ChatColor.GOLD + "你将有15s时间准备背包！！！");
                //公告身份
                announcePlayerRoles();
            }
        }.runTaskLater(plugin, 3L);

        //分配队伍
        initializeTeams();
        for (UUID playerId : readyPlayers) {
            addPlayerToTeam(playerId);
        }


        //设置为冒险模式，而且禁止PVP
        for (UUID playerId : readyPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            player.setGameMode(GameMode.ADVENTURE);
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40);
            player.setHealth(40);
            player.setFoodLevel(20);
            player.setSaturation(0);
            player.setFireTicks(0);
            player.setFallDistance(0);
            PlayerInventory inv = player.getInventory();
            inv.clear();
        }

        for(World world : Bukkit.getWorlds()) {
            world.setPVP(false);
        }

        //给予两种身份的玩家特殊物品
        giveSpecialItems();

        //传送玩家
        teleportPlayers();

        //玩家15s被固定在位置。并且15s免疫伤害。并且15s无法扔珍珠
        restrictMovement();
        disableDamage();
        disablePearl();
        //15s后解除无法移动以及设置为生存模式以及允许PVP
        new BukkitRunnable() {
            @Override
            public void run() {
                allowMovement();
                allowDamage();
                pearlDisabled = false;//启用珍珠
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player != null) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                }
                for(World world : Bukkit.getWorlds()) {
                    world.setPVP(true);
                }
            }
        }.runTaskLater(plugin, 30 * 20L);
    }

    //开始游戏2
    private void startGame2() {
        if (isGameRunning) {
            return; // 如果游戏已经在运行，直接返回
        }

        isGameRunning = true;
        isCountdownRunning = false;

        setAllowback(false);

        //设置最初时间为0
        tp.setGameStartTime();
        compass.setIsSharedBagEnabled();

        //设置世界时间为上午
        World world = Bukkit.getWorld("world");
        if (world != null) {
            world.setTime(1000);
        }

        //分配角色
        recoveryCompass.assignRoles();

        //公告身份,延迟一下执行
        new BukkitRunnable() {
            @Override
            public void run() {
                for(UUID playerId : runners) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "你是逃亡者！");
                    }
                }
                for(UUID playerId : hunters) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(ChatColor.RED + "你是猎人！");
                    }
                }
                announcePlayerRoles();
            }
        }.runTaskLater(plugin, 3L);

        //分配队伍
        initializeTeams();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            addPlayerToTeam(playerId);
        }

        //生成玻璃
        generateGlass();

        //传送玩家
        teleportPlayers2();

        //设置世界的出生点
        //PlayerSpawnToLoc1();

        //设置世界的边界
        setWorldBorderToLoc1(999999);
        setWorldBorderToLoc1AndNether(99999);

        //设置为冒险模式，而且禁止PVP
        for (UUID playerId : readyPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            player.setGameMode(GameMode.ADVENTURE);
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30);
            player.setHealth(30);
            player.setFoodLevel(20);
            player.setSaturation(0);
            player.setFireTicks(0);
            player.setFallDistance(0);

            //清空除了装备栏之外的物品
            PlayerInventory inv = player.getInventory();
            ItemStack[] armorContents = inv.getArmorContents(); // 保存装备槽位的物品
            inv.clear(); // 清除背包物品
            inv.setArmorContents(armorContents); // 恢复装备槽位的物品

            player.sendMessage("§d追杀将在15s后开始");
        }

        for(World world2 : Bukkit.getWorlds()) {
            world2.setPVP(false);
        }

        // 获取feather
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }
            Inventory inv = player.getInventory();
            ItemStack[] chosenFeathersArray; // 重命名以更清晰
            // 检查玩家是否已经有羽毛选择记录
            if (Feather.playerFeatherChoices.containsKey(playerId)) {
                chosenFeathersArray = Feather.playerFeatherChoices.get(playerId);
            } else {
                // 如果玩家没有记录，说明他们从未打开GUI选择，或者选择被清空
                // 为这些玩家设置默认的羽毛选择，并存入 playerFeatherChoices
                ItemStack[] defaultChoices = new ItemStack[3];
                // 使用 Feather 类中已有的 createFeather 方法来创建代表“选择”的物品
                // 这些物品的Lore将被用于决定给予哪种实际羽毛
                defaultChoices[0] = featherInstance.createFeather("§1海豚的眷顾", "§8ManhuntFeatherSwim", true);
                defaultChoices[1] = featherInstance.createFeather("§c抗火", "§8ManhuntFeatherFire", true);
                defaultChoices[2] = featherInstance.createFeather("§e跳跃提升3", "§8ManhuntFeatherJump", true);

                Feather.playerFeatherChoices.put(playerId, defaultChoices); // 将默认选择存入记录
                chosenFeathersArray = defaultChoices; // 使用这些默认选择进行后续分配
            }
            // 根据玩家已选择的羽毛kit（可能是自定义的或刚设置的默认值）分配实际羽毛物品
            for (ItemStack featherChoiceItem : chosenFeathersArray) {
                // 确保选择的羽毛物品不为null，且有正确的元数据和Lore
                if (featherChoiceItem != null &&
                        featherChoiceItem.hasItemMeta() &&
                        featherChoiceItem.getItemMeta().hasLore() &&
                        !featherChoiceItem.getItemMeta().getLore().isEmpty()) {

                    String lore = featherChoiceItem.getItemMeta().getLore().get(0); // 第一个Lore行是识别ID
                    if (lore.contains("§8ManhuntFeatherSpeed")) {
                        inv.addItem(compass.createFeatherSpeed());
                    } else if (lore.contains("§8ManhuntFeatherSwim")) {
                        inv.addItem(compass.createFeatherSwim());
                    } else if (lore.contains("§8ManhuntFeatherFire")) {
                        inv.addItem(compass.createFeatherFire());
                    } else if (lore.contains("§8ManhuntFeatherJump")) {
                        inv.addItem(compass.createFeatherJump());
                    } else if (lore.contains("§8ManhuntFeatherRegen")) {
                        inv.addItem(compass.createFeatherRegen());
                    } else if (lore.contains("§8ManhuntFeatherInvis")) {
                        inv.addItem(compass.createFeatherInvis());
                    }
                }
            }

            inv.addItem(new ItemStack(Material.BREAD, 8));
            //1分钟饱和
            player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 2400, 0));
        }

        if(RecoveryCompass.finalMode.equals("随机掉落原版猎人")){
            //给所有人一个石头稿
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getInventory().addItem(new ItemStack(Material.STONE_PICKAXE));
            }
        }

        if(RecoveryCompass.finalMode.equals("一追多模式")){
            new BukkitRunnable() {
                @Override
                //给猎人一个鞘翅
                public void run() {
                    for (UUID playerId : hunters) {
                        Player player = Bukkit.getPlayer(playerId);
                        PlayerInventory inventory = player.getInventory();
                        ItemStack elytra = new ItemStack(Material.ELYTRA);
                        // 添加附魔
                        elytra.addEnchantment(Enchantment.UNBREAKING, 3);
                        elytra.addEnchantment(Enchantment.MENDING, 1);
                        if (inventory.firstEmpty() == -1) { // 检查背包是否已满
                            Location playerLocation = player.getLocation();
                            playerLocation.getWorld().dropItemNaturally(playerLocation, elytra); // 在玩家脚下生成鞘翅
                        } else {
                            inventory.addItem(elytra); // 将鞘翅添加到背包
                        }
                    }
                    for(Player player : Bukkit.getOnlinePlayers()){
                        player.sendTitle("", "§d已给予所有猎人鞘翅", 20, 100, 20);
                    }
                }
            }.runTaskLater(plugin, 10 * 60 * 20L);
        }

        //给予两种身份的玩家特殊物品
        for (UUID playerId : hunters) {
            Player player = Bukkit.getPlayer(playerId);
            Inventory inv = player.getInventory();
            //给指南针
            inv.addItem( compass.createTrackingCompass());
            if(RecoveryCompass.finalMode.equals("一追多模式")){
                // 创建全套保护一的下界合金套装
                ItemStack[] netheriteArmor = {
                        createEnchantedItem(Material.NETHERITE_HELMET, Enchantment.PROTECTION, 1),
                        createEnchantedItem(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION, 1),
                        createEnchantedItem(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION, 1),
                        createEnchantedItem(Material.NETHERITE_BOOTS, Enchantment.PROTECTION, 1)
                };

                // 获取玩家的装备栏
                PlayerInventory playerInv = player.getInventory();

                // 将套装直接装备到玩家身上
                //playerInv.setHelmet(netheriteArmor[0]);
                playerInv.setChestplate(netheriteArmor[1]);
                playerInv.setLeggings(netheriteArmor[2]);
                playerInv.setBoots(netheriteArmor[3]);

            }
        }
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            Inventory inv = player.getInventory();
            //给指南针
            //inv.addItem(recoveryCompass.createTrackingCompass2());
            inv.addItem( compass.createTrackingCompass());
        }

        //玩家15s被固定在位置。并且15s免疫伤害。并且15s无法扔珍珠
        restrictMovement();
        disableDamage();
        disablePearl();

        // 添加25秒倒计时显示
        new BukkitRunnable() {
            int countdown = 25;
            @Override
            public void run() {
                if (countdown <= 0) {
                    this.cancel();
                } else {
                    for (UUID playerId : readyPlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                            if(RecoveryCompass.finalMode.equals("内鬼模式")){
                                if(ghast.equals(playerId)){
                                    player.sendTitle(
                                            ChatColor.GREEN + String.valueOf(countdown),
                                            ChatColor.GOLD + "你是内鬼,你的任务是帮助逃亡者获得有游戏胜利",
                                            0, 21, 0);

                                }
                                if(runners.contains(playerId)){
                                    player.sendTitle(
                                            ChatColor.GREEN + String.valueOf(countdown),
                                            ChatColor.GOLD + "内鬼是" + ChatColor.DARK_PURPLE + RecoveryCompass.ghastName,
                                            0, 21, 0);
                                }
                                else{
                                    player.sendTitle(
                                            ChatColor.GREEN + String.valueOf(countdown),
                                            ChatColor.GOLD + String.valueOf(countdown),
                                            0, 21, 0);
                                }
                            }else{
                                player.sendTitle(
                                        ChatColor.GREEN + String.valueOf(countdown),
                                        ChatColor.GOLD + String.valueOf(countdown),
                                        0, 21, 0);
                            }
                        }
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        //25s后解除无法移动以及设置为生存模式以及允许PVP
        new BukkitRunnable() {
            @Override
            public void run() {
                allowMovement();
                allowDamage();
                pearlDisabled = false;//启用珍珠
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player != null) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                }
                for(World world : Bukkit.getWorlds()) {
                    world.setPVP(true);
                }
            }
        }.runTaskLater(plugin, 25 * 20L);


        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String message1 = ChatColor.GOLD + "★" + ChatColor.LIGHT_PURPLE + "/compass可以获得指南针";
            String message2 = ChatColor.GOLD + "★" + ChatColor.LIGHT_PURPLE + "/tpa <玩家名> 可以传送到指定逃亡者";
            for (Player player1 : Bukkit.getOnlinePlayers()) {
                player1.sendMessage(message1);
                player1.sendMessage(message2);
            }
        }, 0L, 20L * 480);

    }


    //开始游戏2
    private void startGame3() {
        if (isGameRunning) {
            return; // 如果游戏已经在运行，直接返回
        }

        isGameRunning = true;
        isCountdownRunning = false;

        setAllowback(false);

        //设置最初时间为0
        tp.setGameStartTime();
        compass.setIsSharedBagEnabled();

        //设置世界时间为早晨
        World world = Bukkit.getWorld("world");
        if (world != null) {
            world.setTime(0);
        }

        //分配角色
        recoveryCompass.assignRoles();

        //公告身份,延迟一下执行
        new BukkitRunnable() {
            @Override
            public void run() {
                for(UUID playerId : runners) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(ChatColor.GREEN + "你是逃亡者！");
                    }
                }
                for(UUID playerId : hunters) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player != null) {
                        player.sendMessage(ChatColor.RED + "你是猎人！");
                    }
                }
                announcePlayerRoles();
            }
        }.runTaskLater(plugin, 3L);

        //分配队伍
        initializeTeams();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            addPlayerToTeam(playerId);
        }

        //生成玻璃
        generateGlass();

        //传送玩家
        teleportPlayers3();

        //设置世界的出生点
        //PlayerSpawnToLoc1();

        //设置世界的边界
        setWorldBorderToLoc1(999999);
        setWorldBorderToLoc1AndNether(99999);

        //设置为冒险模式，而且禁止PVP
        for (UUID playerId : readyPlayers) {
            Player player = Bukkit.getPlayer(playerId);
            player.setGameMode(GameMode.ADVENTURE);
            player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30);
            player.setHealth(30);
            player.setFoodLevel(20);
            player.setSaturation(0);
            player.setFireTicks(0);
            player.setFallDistance(0);

            //清空除了装备栏之外的物品
            PlayerInventory inv = player.getInventory();
            ItemStack[] armorContents = inv.getArmorContents(); // 保存装备槽位的物品
            inv.clear(); // 清除背包物品
            inv.setArmorContents(armorContents); // 恢复装备槽位的物品

            player.sendMessage("§d追杀将在15s后开始");
        }

        for(World world2 : Bukkit.getWorlds()) {
            world2.setPVP(false);
        }

        //给予两种身份的玩家特殊物品
        for (UUID playerId : hunters) {
            Player player = Bukkit.getPlayer(playerId);
            Inventory inv = player.getInventory();
            //给指南针
            inv.addItem( compass.createTrackingCompass());
        }
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            Inventory inv = player.getInventory();
            //给指南针
            //inv.addItem( compass.createTrackingCompass());
            // 给护身符
            inv.addItem(compass.createAmulet(player));
            // 逃生者满饱和
            player.setSaturation(20);
        }

        //玩家15s被固定在位置。并且15s免疫伤害。并且15s无法扔珍珠
        restrictMovement();
        disableDamage();
        disablePearl();

        // 添加25秒倒计时显示
        new BukkitRunnable() {
            int countdown = 25;
            @Override
            public void run() {
                if (countdown <= 0) {
                    this.cancel();
                } else {
                    for (UUID playerId : readyPlayers) {
                        Player player = Bukkit.getPlayer(playerId);
                        if (player != null) {
                                player.sendTitle(
                                        ChatColor.GREEN + String.valueOf(countdown),
                                        ChatColor.GOLD + String.valueOf(countdown),
                                        0, 21, 0);

                        }
                    }
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        //25s后解除无法移动以及设置为生存模式以及允许PVP
        new BukkitRunnable() {
            @Override
            public void run() {
                allowMovement();
                allowDamage();
                pearlDisabled = false;//启用珍珠
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player != null) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                }
                for(World world : Bukkit.getWorlds()) {
                    world.setPVP(true);
                }
            }
        }.runTaskLater(plugin, 25 * 20L);


        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            String message1 = ChatColor.GOLD + "★" + ChatColor.LIGHT_PURPLE + "/compass可以获得指南针";
            String message2 = ChatColor.GOLD + "★" + ChatColor.LIGHT_PURPLE + "/tpa <玩家名> 可以传送到指定逃亡者";
            for (Player player1 : Bukkit.getOnlinePlayers()) {
                player1.sendMessage(message1);
                player1.sendMessage(message2);
            }
        }, 0L, 20L * 480);

    }

    public void initializeTeams() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        scoreboard = manager.getNewScoreboard();

        hunterTeam = scoreboard.registerNewTeam("Hunters");
        hunterTeam.setColor(ChatColor.RED);
        hunterTeam.setPrefix(ChatColor.RED + "[猎人] ");

        runnerTeam = scoreboard.registerNewTeam("Runners");
        runnerTeam.setColor(ChatColor.AQUA);
        runnerTeam.setPrefix(ChatColor.AQUA + "[逃亡者] ");

        // 新增内鬼队伍
        ghastTeam = scoreboard.registerNewTeam("Ghast");
        ghastTeam.setColor(ChatColor.DARK_PURPLE);
        ghastTeam.setPrefix(ChatColor.DARK_PURPLE + "[群众选出的内鬼] ");
    }

    public void addPlayerToTeam(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (isHunter(player.getUniqueId())) {
            hunterTeam.addEntry(player.getName());
        } else if (isRunner(player.getUniqueId())) {
            runnerTeam.addEntry(player.getName());
        }else if (isGhast(playerId)) { // 新增内鬼队伍检查
            ghastTeam.addEntry(player.getName());
        }
        player.setScoreboard(scoreboard);
    }





    public void removeRunner(UUID playerId){
        runners.remove(playerId);
    }

    private void teleportPlayers() {
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // 获取目标位置
                Location loc11 = getLoc11();

                // 在目标位置下方生成玻璃
                Block blockUnderTarget = loc11.getBlock().getRelative(BlockFace.DOWN);
                Material material = blockUnderTarget.getType();
                if (material == Material.WATER || material == Material.LAVA) {
                    blockUnderTarget.setType(Material.GLASS);
                }
                player.teleport(loc11);
            }
        }


        for (UUID playerId : hunters) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // 获取目标位置
                Location loc22 = getRandomNearbyLocation();

                // 在目标位置下方生成玻璃
                Block blockUnderTarget = loc22.getBlock().getRelative(BlockFace.DOWN);
                Material material = blockUnderTarget.getType();
                if (material == Material.WATER || material == Material.LAVA) {
                    blockUnderTarget.setType(Material.GLASS);
                }
                player.teleport(loc22);
            }
        }
    }

    private void generateGlass() {}

    private void teleportPlayers2() {
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // 获取目标位置
                Location targetLocation = runnerLocation;

                // 在目标位置下方生成玻璃
                Block blockUnderTarget = targetLocation.getBlock().getRelative(BlockFace.DOWN);
                Material material = blockUnderTarget.getType();
                if (material == Material.WATER || material == Material.LAVA) {
                    blockUnderTarget.setType(Material.GLASS);
                }
                //传送逃亡者到随机位置
                player.teleport(targetLocation);
                // 设置该位置为玩家的出生点
                // true 表示即使在该位置没有床，也强制设置出生点
                player.setBedSpawnLocation(targetLocation, true);

            }
        }
        for (UUID playerId : hunters) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                // 获取目标位置
                Location targetLocation = getNearbyLocation(runnerLocation, 3, 5);

                // 在目标位置下方生成玻璃
                Block blockUnderTarget = targetLocation.getBlock().getRelative(BlockFace.DOWN);
                Material material = blockUnderTarget.getType();
                if (material == Material.WATER || material == Material.LAVA) {
                    blockUnderTarget.setType(Material.GLASS);
                }

                player.teleport(targetLocation);
                // 设置该位置为玩家的出生点
                // true 表示即使在该位置没有床，也强制设置出生点
                player.setBedSpawnLocation(runnerLocation, true);
            }
        }
    }

    private void teleportPlayers3() {
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                // 获取目标位置
                Location targetLocation = runnerLocation_ZhuboMode;

                // 在目标位置下方生成玻璃
                Block blockUnderTarget = targetLocation.getBlock().getRelative(BlockFace.DOWN);
                Material material = blockUnderTarget.getType();
                if (material == Material.WATER || material == Material.LAVA) {
                    blockUnderTarget.setType(Material.GLASS);
                }
                //传送逃亡者到随机位置
                player.teleport(targetLocation);
                // 设置该位置为玩家的出生点
                // true 表示即使在该位置没有床，也强制设置出生点
                player.setBedSpawnLocation(targetLocation, true);

            }
        }
        for (UUID playerId : hunters) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {

                // 获取目标位置
                Location targetLocation = getNearbyLocation(runnerLocation_ZhuboMode, 3, 5);

                // 在目标位置下方生成玻璃
                Block blockUnderTarget = targetLocation.getBlock().getRelative(BlockFace.DOWN);
                Material material = blockUnderTarget.getType();
                if (material == Material.WATER || material == Material.LAVA) {
                    blockUnderTarget.setType(Material.GLASS);
                }

                player.teleport(targetLocation);
                // 设置该位置为玩家的出生点
                // true 表示即使在该位置没有床，也强制设置出生点
                player.setBedSpawnLocation(targetLocation, true);
            }
        }
    }

    // 在指定范围内生成随机位置
    private Location getRandomLocationInRange(int minX, int maxX, int minZ, int maxZ) {
        World world = Bukkit.getWorlds().get(0); // 获取主世界
        Location location;
        Block block;
        int attempts = 0;

        do {
            int x = (int) (Math.random() * (maxX - minX + 1) + minX);
            int z = (int) (Math.random() * (maxZ - minZ + 1) + minZ);
            int y = world.getHighestBlockYAt(x, z); // 获取最高可站立方块的Y坐标
            location = new Location(world, x, y + 1, z);

            // 检查当前位置是否是水
            block = world.getBlockAt(x, y, z);
            attempts++;

            // 防止无限循环，设置最大尝试次数
            if (attempts > 100) {
                break;
            }
        } while (block.getType() == Material.WATER || block.getType() == Material.LAVA);

        return location;
    }

    // 在指定位置附近生成随机位置
    private Location getNearbyLocation(Location center, int minDistance, int maxDistance) {
        double angle = Math.random() * 2 * Math.PI;
        double distance = Math.random() * (maxDistance - minDistance) + minDistance;
        int x = (int) (center.getX() + distance * Math.cos(angle));
        int z = (int) (center.getZ() + distance * Math.sin(angle));
        int y = center.getWorld().getHighestBlockYAt(x, z) + 1;


        return new Location(center.getWorld(), x, y, z);

    }


    public void endGame(String reason) {
        //所有人变成旁观者,清空背包
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player != null) {
                player.setGameMode(GameMode.SPECTATOR);
                player.getInventory().clear();
            }

            switch (reason) {
                //猎人赢
                case "1":
                    player.sendTitle("§c猎人胜利", "§7游戏结束", 10, 70, 20);
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            if(RecoveryCompass.finalMode.equals("内鬼模式")){
                                player.sendTitle("§7卧底是" , "§c" + RecoveryCompass.ghastName, 10, 70, 20);
                            }
                        }
                    }.runTaskLater(plugin, 5 * 20L);
                    break;
                //逃亡者赢
                case "2":
                    player.sendTitle("§a逃亡者胜利", "§7游戏结束", 10, 70, 20);
                    new BukkitRunnable(){
                        @Override
                        public void run() {
                            if(RecoveryCompass.finalMode.equals("内鬼模式")){
                                player.sendTitle("§7卧底是" , "§c" + RecoveryCompass.ghastName, 10, 70, 20);
                            }
                        }
                    }.runTaskLater(plugin, 5 * 20L);
                    break;
                //平局
                case "3":
                    player.sendTitle("§e平局", "§7游戏结束", 10, 70, 20);
                    break;
            }
            if(RecoveryCompass.finalMode.equals("内鬼模式")){
                Bukkit.broadcastMessage("§c卧底是" + RecoveryCompass.ghastName);
            }
        }

        Bukkit.broadcastMessage("§a游戏结束，30s后重置世界");

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 播放烟花音效
            new BukkitRunnable() {
                int duration = 0;
                @Override
                public void run() {
                    if (duration >= 7) { // 播放7次
                        this.cancel();
                        return;
                    }
                    // 随机播放不同的烟花音效
                    float pitch = 0.8f + (duration * 0.05f); // 逐渐提高音高
                    player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, pitch);
                    // 添加爆炸音效
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, pitch);
                    }, 10L); // 延迟10 ticks播放爆炸音效
                    // 添加火花音效
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.8f, pitch + 0.2f);
                    }, 15L);
                    duration++;
                }
            }.runTaskTimer(plugin, 0L, 20L); // 每隔20 ticks（1秒）播放一次
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                //设置游戏结束
                clearAllRoleAndTeam();

                kickAllPlayers();
                deleteAllWorldFolders();
                deleteAllWorldFolders1();

                File serverDirectory = Bukkit.getWorldContainer();
                String[] worldNames = {"world", "world_nether", "world_the_end"};

                for (String worldName : worldNames) {
                    // 卸载世界
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        Bukkit.unloadWorld(world, false);
                    }}

                for (String worldName : worldNames) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        File worldFolder = new File(serverDirectory, worldName);
                        deleteWorldFolder(worldFolder);
                    }}
                Bukkit.shutdown();
            }
        }.runTaskLater(plugin,30 * 20L);



    }

    public void endGame2() {

    }

    public void clearAllRoleAndTeam(){
        isGameRunning = false;
        //清除猎人和逃亡者
        hunters.clear();
        runners.clear();
        //清空准备者身份
        readyPlayers.clear();
        //清除队伍
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }

        if (hunterTeam != null) {
            hunterTeam.unregister();
        }
        if (runnerTeam != null) {
            runnerTeam.unregister();
        }
    }


    public void kickAllPlayers() {
               for (Player player : Bukkit.getOnlinePlayers()) {
                   player.kickPlayer("§c服务器正在重置，请稍后重新加入。");
               }
                // 对所有在线玩家执行踢出操作
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.kickPlayer(ChatColor.GREEN + "游戏已结束！服务器已重启！");
                });
                // 广播服务器锁定消息
                plugin.getLogger().info("已踢出所有玩家。");

              }



    public void deleteAllWorldFolders1() {
        // 获取服务器根目录
        File serverDirectory = Bukkit.getWorldContainer();

        // 定义要删除的世界名称
        String[] worldNames = {"world", "world_nether", "world_the_end"};

        for (String worldName : worldNames) {
            // 卸载世界
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }

            // 删除世界文件夹
            File worldFolder = new File(serverDirectory, worldName);
            if (worldFolder.exists()) {
                try {
                    // 如果是主世界，保留datapacks文件夹
                    if (worldName.equals("world")) {
                        Files.walk(worldFolder.toPath())
                                .sorted(Comparator.reverseOrder())
                                //.filter(path -> !path.startsWith(worldFolder.toPath().resolve("datapacks")))
                                .map(Path::toFile)
                                .forEach(file -> {
                                    if (!file.delete()) {
                                        Bukkit.getLogger().warning("无法删除文件: " + file.getAbsolutePath());
                                    }
                                });
                    } else {
                        // 对于其他世界，删除所有内容
                        Files.walk(worldFolder.toPath())
                                .sorted(Comparator.reverseOrder())
                                .map(Path::toFile)
                                .forEach(file -> {
                                    if (!file.delete()) {
                                        Bukkit.getLogger().warning("无法删除文件: " + file.getAbsolutePath());
                                    }
                                });
                    }
                } catch (IOException e) {
                    Bukkit.getLogger().warning("删除世界文件夹时出错: " + worldName);
                    e.printStackTrace();
                }
            }
            // 确保删除 level.dat 和 level.dat_old 文件
            File levelDat = new File(serverDirectory, worldName + "/level.dat");
            File levelDatOld = new File(serverDirectory, worldName + "/level.dat_old");
            levelDat.delete();
            levelDatOld.delete();
        }}


    public void deleteAllWorldFolders() {
        // 获取服务器根目录
        File serverDirectory = Bukkit.getWorldContainer();

        // 定义要删除的世界名称
        String[] worldNames = {"world", "world_nether", "world_the_end"};

        for (String worldName : worldNames) {
            // 卸载世界
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }

            // 删除世界文件夹
            File worldFolder = new File(serverDirectory, worldName);
            if (worldFolder.exists()) {
                try {
                    Files.walk(worldFolder.toPath())
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                    Bukkit.getLogger().info("成功删除世界文件夹: " + worldName);
                } catch (IOException e) {
                    Bukkit.getLogger().warning("删除世界文件夹时出错: " + worldName);
                    e.printStackTrace();
                }}}}

    public void deleteWorldFolder(File worldFolder) {
        final int MAX_RETRIES = 3;
        final long RETRY_DELAY = 100; // 毫秒

        try {
            Files.walk(worldFolder.toPath())
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
                            try {
                                Files.deleteIfExists(path);
                                break; // 如果成功删除，跳出重试循环
                            } catch (IOException e) {
                                if (attempt == MAX_RETRIES - 1) {
                                    // 最后一次尝试失败
                                    Bukkit.getLogger().warning("无法删除文件（已重试" + MAX_RETRIES + "次）: "
                                            + path.toString());
                                    Bukkit.getLogger().warning("错误: " + e.getMessage());
                                } else {
                                    // 等待一段时间后重试
                                    try {
                                        Thread.sleep(RETRY_DELAY);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        Bukkit.getLogger().warning("删除操作被中断");
                                        return;
                                    }
                                }
                            }
                        }
                    });
        } catch (IOException e) {
            Bukkit.getLogger().severe("遍历文件夹时发生错误: " + e.getMessage());
        }

        // 检查文件夹是否完全删除
        if (worldFolder.exists()) {
            Bukkit.getLogger().warning("无法完全删除文件夹: " + worldFolder.getAbsolutePath());
            Bukkit.getLogger().warning("请手动检查并删除剩余文件。");
        } else {
            Bukkit.getLogger().info("成功删除文件夹: " + worldFolder.getAbsolutePath());
        }
    }


        //在此处新建一个world文件夹。并且将服务器根目录下plugins文件夹下的datapacks文件夹连同这个文件夹下的文件
        // 一起复制到新建的world文件夹下
        // 在此处新建一个world文件夹，并复制datapacks
//        File newWorldFolder = new File(serverDirectory, "world");
//        if (!newWorldFolder.exists()) {
//            newWorldFolder.mkdir();
//        }
//
//// 复制plugins文件夹下的datapacks到新的world文件夹
//        File pluginsFolder = new File(serverDirectory, "plugins");
//        File sourceDatapacks = new File(pluginsFolder, "datapacks");
//        File destDatapacks = new File(newWorldFolder, "datapacks");
//
//        if (sourceDatapacks.exists()) {
//            try {
//                Files.walkFileTree(sourceDatapacks.toPath(), new SimpleFileVisitor<Path>() {
//                    @Override
//                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
//                        Path targetDir = destDatapacks.toPath().resolve(sourceDatapacks.toPath().relativize(dir));
//                        Files.createDirectories(targetDir);
//                        return FileVisitResult.CONTINUE;
//                    }
//
//                    @Override
//                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                        Files.copy(file, destDatapacks.toPath().resolve(sourceDatapacks.toPath().relativize(file)), StandardCopyOption.REPLACE_EXISTING);
//                        return FileVisitResult.CONTINUE;
//                    }
//                });
//                Bukkit.getLogger().info("成功复制datapacks文件夹到新的world文件夹");
//            } catch (IOException e) {
//                Bukkit.getLogger().warning("复制datapacks文件夹时出错");
//                e.printStackTrace();
//            }
//        } else {
//            Bukkit.getLogger().warning("在plugins文件夹中未找到datapacks文件夹");}



    public void deleteAndRecreateWorld(String worldName) {
        // 获取服务器根目录
        File serverDirectory = Bukkit.getWorldContainer();
        File worldFolder = new File(serverDirectory, worldName);

        // 1. 卸载并删除旧世界
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            Bukkit.unloadWorld(world, false);
        }

        if (worldFolder.exists()) {
            try {
                Files.walk(worldFolder.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                Bukkit.getLogger().warning("第二个方法删除世界文件夹时出错: " + worldName);
                e.printStackTrace();
                return;
            }
        }

        // 2. 创建新的 level.dat 文件
        try {
            worldFolder.mkdirs(); // 确保世界文件夹存在
            File levelDat = new File(worldFolder, "level.dat");
            createNewLevelDat(levelDat);
            Bukkit.getLogger().info("成功创建新的 level.dat 文件，包含新的随机种子");
        } catch (IOException e) {
            Bukkit.getLogger().warning("创建新的 level.dat 文件时出错");
            e.printStackTrace();
        }
    }

    private void createNewLevelDat(File levelDat) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(levelDat))) {
            // 写入 NBT 格式的 level.dat 文件
            // 这里只包含最基本的信息，包括一个新的随机种子
            out.writeByte(10); // Compound 开始
            out.writeUTF(""); // Root compound 名称（空）

            // Data compound
            out.writeByte(10);
            out.writeUTF("Data");

            // 随机种子
            out.writeByte(4); // Long 类型
            out.writeUTF("RandomSeed");
            out.writeLong(new Random().nextLong());

            // 结束 Data compound
            out.writeByte(0);

            // 结束 root compound
            out.writeByte(0);
        }
    }


    public boolean isRunner(UUID playerId) {
        return runners.contains(playerId);
    }

    public boolean isHunter(UUID playerId) {
        return hunters.contains(playerId);
    }

    public boolean isGhast(UUID playerId) {
        return Vote.outPlayerId == playerId;
    }

    //检查游戏是否结束
    public void checkGameEnd() {
        //如果游戏没有开始，返回，不执行任何操作
        if (!isGameRunning) return;

        //打印一下还活着的逃亡者的名字
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                Bukkit.getLogger().info("逃亡者: " + player.getName());
            }
        }


        //如果逃亡者无人
        if (runners.isEmpty()) {
            if(!RecoveryCompass.finalMode.equals("内鬼模式")){
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player != null) {
                        player.sendMessage(ChatColor.RED + "所有逃亡者已被击杀！猎人胜利！");
                        endGame("1");
                    }
                }
            }
            else {
                Player ghastplayer = RecoveryCompass.ghastPlayer;
                if(ghastplayer == null){
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player != null) {
                            player.sendMessage(ChatColor.RED + "所有逃亡者和内鬼已被击杀！猎人胜利！");
                            endGame("1");
                        }
                    }
                }
                else{
                    hunters.remove(ghast);
                    runners.add(ghast);

                    // 从猎人队伍中移除玩家
                    Team hunterTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("Hunters");
                    if (hunterTeam != null && hunterTeam.hasEntry(Bukkit.getPlayer(ghast).getName())) {
                        hunterTeam.removeEntry(Bukkit.getPlayer(ghast).getName());
                    }
                    // 将玩家加入逃亡者队伍
                    runnerTeam.addEntry(Bukkit.getPlayer(ghast).getName());
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player != null) {
                            player.sendMessage(ChatColor.RED + "所有逃亡者已被击杀！内鬼复活成逃亡者！游戏继续！");
                        }
                    }
                }
            }

        }
    }



    public void checkIfOnlyOneLeft() {
        //如果游戏没有开始，返回，不执行任何操作
        if (!isGameRunning) {
            return;
        }
        //如果逃亡者只有一个人
        if (runners.size() == 1) {
            UUID lastRunnerUUID = runners.iterator().next(); // 获取最后一个逃亡者的 UUID
            Player lastRunner = Bukkit.getPlayer(lastRunnerUUID);
            //Bukkit.broadcastMessage(ChatColor.AQUA + "就剩一个了，活下去！");
            loc6 = lastRunner.getLocation();

        }
    }

    public void dragonDefeated() {
            if (isGameRunning) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.AQUA + "末影龙已被击杀！逃亡者胜利！");
                }

                endGame("2");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {

        }
    }



    private void giveSpecialItems(){
        for (UUID playerId : hunters) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                PlayerInventory inv = player.getInventory();
                //
                inv.addItem(getRandomItem());

                //给指南针
                inv.addItem(compass.createTrackingCompass());
                // 工具
                inv.addItem(new ItemStack(Material.DIAMOND_AXE));
                inv.addItem(new ItemStack(Material.DIAMOND_PICKAXE));
                inv.addItem(new ItemStack(Material.DIAMOND_SHOVEL));
                inv.addItem(new ItemStack(Material.DIAMOND_SWORD));
                inv.addItem(new ItemStack(Material.SHEARS));
                inv.addItem(new ItemStack(Material.SHIELD));

                // 建筑材料
                inv.addItem(new ItemStack(Material.COBBLESTONE, 128));
                inv.addItem(new ItemStack(Material.OAK_PLANKS, 128));
                inv.addItem(new ItemStack(Material.DIRT, 128));
                inv.addItem(new ItemStack(Material.POINTED_DRIPSTONE, 32));
                inv.addItem(new ItemStack(Material.COBWEB, 48));

                // 其他工具和物品
                inv.addItem(new ItemStack(Material.FISHING_ROD));
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addEnchantment(Enchantment.POWER, 1);
                inv.addItem(bow);
                inv.addItem(new ItemStack(Material.WATER_BUCKET, 1));
                inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
                inv.addItem(new ItemStack(Material.WATER_BUCKET, 1));
                inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
                inv.addItem(new ItemStack(Material.ENDER_PEARL, 16));
                inv.addItem(new ItemStack(Material.SPECTRAL_ARROW, 128));
                inv.addItem(new ItemStack(Material.GOLDEN_CARROT, 64));
                inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
                inv.addItem(new ItemStack(Material.TOTEM_OF_UNDYING));
                //给铁砧和32经验瓶和十个黑曜石和打火石
                inv.addItem(new ItemStack(Material.ANVIL));
                inv.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 32));
                inv.addItem(new ItemStack(Material.OBSIDIAN, 10));
                inv.addItem(new ItemStack(Material.FLINT_AND_STEEL, 1));

                // 钻石盔甲
                player.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                player.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                player.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
                player.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));



            }
        }
        for (UUID playerId : runners) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                PlayerInventory inv = player.getInventory();

                ItemStack[] randomItems = getTwoRandomDifferentItems();
                inv.addItem(randomItems);

 //               inv.addItem(sensor.createSensor());
   //             inv.addItem(brush.createBrush());

                //给特殊物品
                 // inv.addItem(getRandomItem());
       //          inv.addItem(specialItem.createSoulLantern());
  //              inv.addItem(goatHorn.createGoatHorn());
//                inv.addItem(beeGun.createDragonHead());
//                inv.addItem(fireBall.createFireBall());
//                inv.addItem(magicBlock.createMagicBlock());
//                  inv.addItem(chain.createChain());
//                  inv.addItem(ironMan.createPumpkin());
//                  inv.addItem(twist.createTwistingVines());
//                  inv.addItem(skull.createSkull());


                //给指南针
               // inv.addItem(compass.createPotionCompass());

                // 工具
                inv.addItem(new ItemStack(Material.DIAMOND_AXE));

                ItemStack diamondPickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
                diamondPickaxe.addEnchantment(Enchantment.EFFICIENCY, 1);
                inv.addItem(diamondPickaxe);

                ItemStack diamondShovel = new ItemStack(Material.DIAMOND_SHOVEL);
                diamondShovel.addEnchantment(Enchantment.EFFICIENCY, 1);
                inv.addItem(diamondShovel);

                inv.addItem(new ItemStack(Material.DIAMOND_SWORD));
                inv.addItem(new ItemStack(Material.SHEARS));
                inv.addItem(new ItemStack(Material.SHIELD));

                // 建筑材料
                inv.addItem(new ItemStack(Material.COBBLESTONE, 128));
                inv.addItem(new ItemStack(Material.OAK_PLANKS, 128));
                inv.addItem(new ItemStack(Material.DIRT, 128));
                inv.addItem(new ItemStack(Material.POINTED_DRIPSTONE, 32));
                inv.addItem(new ItemStack(Material.COBWEB, 48));

                // 其他工具和物品
                inv.addItem(new ItemStack(Material.FISHING_ROD));
                ItemStack bow = new ItemStack(Material.BOW);
                bow.addEnchantment(Enchantment.POWER, 1);
                inv.addItem(bow);
                inv.addItem(new ItemStack(Material.WATER_BUCKET, 1));
                inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
                inv.addItem(new ItemStack(Material.WATER_BUCKET, 1));
                inv.addItem(new ItemStack(Material.LAVA_BUCKET, 1));
                inv.addItem(new ItemStack(Material.ENDER_PEARL, 16));
                inv.addItem(new ItemStack(Material.SPECTRAL_ARROW, 128));
                inv.addItem(new ItemStack(Material.GOLDEN_CARROT, 64));
                inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 16));
                inv.addItem(new ItemStack(Material.TOTEM_OF_UNDYING));
                inv.addItem(new ItemStack(Material.ENDER_EYE,20));
                //给铁砧和32经验瓶和二十个黑曜石和一个打火石
                inv.addItem(new ItemStack(Material.ANVIL));
                inv.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 32));
                inv.addItem(new ItemStack(Material.OBSIDIAN, 20));
                inv.addItem(new ItemStack(Material.FLINT_AND_STEEL, 1));



                //给TNT
                inv.addItem(new ItemStack(Material.TNT, 10));

                //给铁砧和经验瓶

                // 盔甲
                player.getEquipment().setHelmet(new ItemStack(Material.NETHERITE_HELMET));
                player.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
                player.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
                player.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));

                //给一个重锤
                inv.addItem(new ItemStack(Material.MACE, 1));


                //给一组风弹
                inv.addItem(new ItemStack(Material.WIND_CHARGE, 128));


            }
    }
}

public void giveUHCItems() {

}

    private void initializeItems() {
        // 在这里添加你制作的物品
        items.add(specialItem.createSoulLantern());
        items.add(goatHorn.createGoatHorn());
        items.add(fireBall.createFireBall());
        items.add(beeGun.createDragonHead());
        items.add(magicBlock.createMagicBlock());
        items.add(chain.createChain());
        items.add(ironMan.createPumpkin());
        items.add(twist.createTwistingVines());
        items.add(skull.createSkull());
        items.add(brush.createBrush());
        items.add(sensor.createSensor());

    }

    public ItemStack getRandomItem() {
        if (items.isEmpty()) {
            return null;
        }
        return items.get(random.nextInt(items.size()));
    }

    // 修改 getTwoRandomDifferentItems 方法返回类型
    public ItemStack[] getTwoRandomDifferentItems() {
        if (items.size() < 2) {
            // 如果物品列表中的物品少于两个，返回null或抛出异常
            return null; // 或者 throw new IllegalStateException("Not enough items to select two different ones.");
        }

        List<ItemStack> availableItems = new ArrayList<>(items);
        Collections.shuffle(availableItems);

        return new ItemStack[] {
                availableItems.remove(0),
                availableItems.remove(0)
        };
    }
    public ItemStack createSpecialTNT(int amount) {
        ItemStack specialTNT = new ItemStack(Material.TNT);
        ItemMeta meta = specialTNT.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "黑曜石破坏者TNT");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "可以炸毁黑曜石的特殊TNT"));
        specialTNT.setItemMeta(meta);
        return specialTNT;
    }


    private Location getLoc11() {
        World world = loc1.getWorld();
        if (world == null) return null;

        int x =loc1.getBlockX();
        int z = loc1.getBlockZ();

        // 找到地面位置
        int y = world.getHighestBlockYAt(x, z);
        return new Location(world, x, y + 1, z);
    }


    private Location getRandomNearbyLocation() {
        World world = loc1.getWorld();
        if (world == null) return null;

        Random random = new Random();
        int distance = random.nextInt(3) + 3; // 3到5之间的随机数
        double angle = random.nextDouble() * 2 * Math.PI; // 0到2π之间的随机角度

        int x =loc1.getBlockX() + (int) (distance * Math.cos(angle));
        int z = loc1.getBlockZ() + (int) (distance * Math.sin(angle));

        // 找到地面位置
        int y = world.getHighestBlockYAt(x, z);

        return new Location(world, x + 0.5, y + 1, z + 0.5);
    }


    public void announcePlayerRoles() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "=====================");
        }
        //Bukkit.broadcastMessage(ChatColor.GOLD + "=====================");

        // 公告猎人
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.RED + "猎人:");
        }
        //Bukkit.broadcastMessage(ChatColor.RED + "猎人:");
        for (UUID hunterId : hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.RED + "- " + hunter.getName());
                }
                //Bukkit.broadcastMessage(ChatColor.RED + "- " + hunter.getName());
            }
        }

        // 公告逃亡者
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.AQUA + "逃亡者:");
        }
        //Bukkit.broadcastMessage(ChatColor.AQUA + "逃亡者:");
        for (UUID runnerId : runners) {
            Player runner = Bukkit.getPlayer(runnerId);
            if (runner != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage(ChatColor.AQUA + "- " + runner.getName());
                }
                //Bukkit.broadcastMessage(ChatColor.AQUA + "- " + runner.getName());
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GOLD + "=====================");
        }
        //Bukkit.broadcastMessage(ChatColor.GOLD + "=====================");
    }

    private void clearDrops() {
        for (World world : Bukkit.getWorlds()) {
            world.getEntities().stream()
                    .filter(entity -> entity instanceof org.bukkit.entity.Item)
                    .forEach(Entity::remove);
        }
        plugin.getLogger().info(ChatColor.YELLOW + "所有世界的掉落物已被清除！");
    }

    public void deleteEndWorld() {
        World endWorld = Bukkit.getWorld("world_the_end");
        if (endWorld == null) {
            plugin.getLogger().warning("末地世界不存在，无法删除。");
            return;
        }

        // 卸载世界
        if (Bukkit.unloadWorld(endWorld, false)) {
            File worldFolder = endWorld.getWorldFolder();

            // 使用异步任务删除世界文件夹
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                if (deleteDirectory(worldFolder)) {
                    plugin.getLogger().info("末地世界已成功删除。");
                } else {
                    plugin.getLogger().warning("删除末地世界文件夹失败。");
                }
            });
        } else {
            plugin.getLogger().warning("卸载末地世界失败，无法删除。");
        }
    }

    // 递归删除目录及其内容
    private boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        if (!file.delete()) {
                            return false;
                        }
                    }
                }
            }
        }
        return directory.delete();
    }


    private Location findStrongholdLocation(World world) {
        Location strongholdLocation = null;

        try {
            // 使用 Bukkit API 查找最近的要塞
            strongholdLocation = world.locateNearestStructure(world.getSpawnLocation(), StructureType.STRONGHOLD,
                    100, false);

            if (strongholdLocation != null) {
                // 找到地表高度
                int y = world.getHighestBlockYAt(strongholdLocation);
                strongholdLocation.setY(y + 1);

                plugin.getLogger().info("找到要塞位置：" + strongholdLocation.getBlockX() + ", " + strongholdLocation.getBlockY() + ", " + strongholdLocation.getBlockZ());
            } else {
                plugin.getLogger().warning("无法找到要塞位置");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("查找要塞时出错: " + e.getMessage());
            e.printStackTrace();
        }

        // 如果找不到要塞，返回世界出生点
        return strongholdLocation != null ? strongholdLocation : world.getSpawnLocation();
    }

    public void setPlayerSpawnToLoc1() {
        if (loc1 != null) {
            World world = loc1.getWorld();
                world.setSpawnLocation(loc1);

                // 对于所有在线玩家，设置他们的出生点
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.setBedSpawnLocation(loc1, true);
                }

                plugin.getLogger().info("所有玩家的出生点已被设置到: " + loc1.getBlockX() +
                        ", " + loc1.getBlockY() + ", " + loc1.getBlockZ());
            }

        }


    public void setWorldBorderToLoc1(int size) {
        if (loc1 != null) {
            World world = loc1.getWorld();
            if (world != null) {
                WorldBorder border = world.getWorldBorder();
                border.setCenter(loc1);
                border.setSize(size);
                border.setWarningDistance(5);
                border.setWarningTime(15);

                plugin.getLogger().info("世界边界已设置，中心: " + loc1.getBlockX() + ", " + loc1.getBlockZ() + ", 大小: " + size);
            } else {
                plugin.getLogger().warning("无法设置世界边界：loc1 的世界是 null");
            }
        } else {
            plugin.getLogger().warning("无法设置世界边界：loc1 是 null");
        }
    }

    public void setWorldBorderToLoc1AndNether(int overworldSize) {
        if (loc1 != null) {
            World overworld = loc1.getWorld();
            World nether = Bukkit.getWorld("world_nether");

            if (overworld != null && nether != null) {
                // 设置主世界边界
                //setWorldBorder(overworld, loc1, overworldSize);

                // 计算地狱中对应的位置和大小
                Location netherLoc = new Location(nether, loc1.getX() / 2, loc1.getY(),
                        loc1.getZ() / 2);
                int netherSize = overworldSize;

                // 设置地狱边界
                setWorldBorder(nether, netherLoc, netherSize);

                plugin.getLogger().info("地狱的世界边界已设置");
            } else {
                plugin.getLogger().warning("无法设置世界边界：世界不存在");
            }
        } else {
            plugin.getLogger().warning("无法设置世界边界：loc1 是 null");
        }
    }

    private void setWorldBorder(World world, Location center, int size) {
        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(size);
        border.setWarningDistance(5);
        border.setWarningTime(15);

        plugin.getLogger().info(world.getName() + "世界边界已设置，中心: " +
                center.getBlockX() + ", " + center.getBlockZ() + ", 大小: " + size);
    }


    public void  generateArea(){
        World world = Bukkit.getWorld("world");
        if (world == null) return;

        // 生成区域
        int x = loc1.getBlockX();
        int y = 10;
        int z = loc1.getBlockZ();
        for(int x1 = x - 10; x1 <= x + 10; x1++){
            for(int z1 = z - 10; z1 <= z + 10; z1++){
                world.getBlockAt(x1, y, z1).setType(Material.GLASS);
            }
        }
        for(int x1 = x - 10; x1 <= x + 10; x1++){
            for(int y1 = y ; y1 <= 210 ;y1++){
                int z1 = z + 10;
                world.getBlockAt(x1, y1, z1).setType(Material.GLASS);
            }
        }
        for(int x1 = x - 10; x1 <= x + 10; x1++){
            for(int y1 = y ; y1 <= 210 ;y1++){
                int z1 = z - 10;
                world.getBlockAt(x1, y1, z1).setType(Material.GLASS);
            }
        }
        for(int z1 = z - 10; z1 <= z + 10; z1++){
            for(int y1 = y ; y1 <= 210 ;y1++){
                int x1 = x + 10;
                world.getBlockAt(x1, y1, z1).setType(Material.GLASS);
            }
        }
        for(int z1 = z - 10; z1 <= z + 10; z1++){
            for(int y1 = y ; y1 <= 210 ;y1++){
                int x1 = x - 10;
                world.getBlockAt(x1, y1, z1).setType(Material.GLASS);
            }
        }
    }

    public void clearArea(){
        World world = loc1.getWorld();
        if (world == null) return;

        // 生成区域
        int x = loc1.getBlockX();
        int y = 200;
        int z = loc1.getBlockZ();
        for(int x1 = x - 11; x1 <= x + 11; x1++){
            for(int z1 = z - 11; z1 <= z + 11; z1++){
                for(int y1 = y ; y1 <= 211 ;y1++){
                    world.getBlockAt(x1, y1, z1).setType(Material.AIR);
                }
            }
        }
    }


    public void setupAndShrinkWorldBorders() {
        //以loc1为中心,半径为500，最终为半径为15
        setupAndShrinkBorder(loc1.getWorld(), 1000, 30, "主世界");

        World netherWorld = Bukkit.getWorld("world_nether");
        if (netherWorld != null) {
            //netherCenter为地狱中心，坐标为Loc1的1/2
            netherCenter = new Location(netherWorld, loc1.getX() / 2, loc1.getY(), loc1.getZ() / 2);
            setupAndShrinkBorder(netherWorld, 500, 15, "地狱"); // 250/2=125, 7/2=3.5
        }
    }

    private void setupAndShrinkBorder(World world, double initialRadius, double finalRadius,
                                      String worldName) {
        WorldBorder border = world.getWorldBorder();

        // 如果world是地狱
        if (world.getEnvironment() == World.Environment.NETHER) {
            netherCenter = new Location(world, loc1.getX() / 2, loc1.getY(), loc1.getZ() / 2);
            border.setCenter(netherCenter);
            border.setWarningDistance(5);
            border.setWarningTime(15);
        }
        //如果世界是主世界
        if (world.getEnvironment() == World.Environment.NORMAL){
            border.setCenter(loc1);
            border.setWarningDistance(5);
            border.setWarningTime(15);
        }
        border.setSize(initialRadius);

        // 计算收缩参数
        //final double initialSize = initialRadius;
        //final double finalSize = finalRadius;
        final long shrinkDelay = 15 * 60 * 20; // 15分钟后开始收缩（转换为ticks）
        final long shrinkDuration = 60 * 60 * 20; // 收缩持续60分钟（转换为ticks）

        // 设置边界收缩
        new BukkitRunnable() {
            @Override
            public void run() {
                //60分钟后缩小到最终大小
                border.setSize(finalRadius, shrinkDuration / 20); // 将ticks转换为秒
            }
        }.runTaskLater(plugin, shrinkDelay);

        // 通知玩家
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.sendMessage("§c警告：" + worldName + "边界开始收缩！");
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, shrinkDelay);

        // 定期通知玩家边界状态
        new BukkitRunnable() {
            @Override
            public void run() {
                double currentSize = border.getSize();
                if (currentSize <= finalRadius) {
                    this.cancel(); // 如果边界已经达到最终大小，停止通知
                    return;
                }
                for (Player player : world.getPlayers()) {
                    player.sendMessage("§e" + worldName + "当前边界大小: " + String.format("%.1f", currentSize) + " x " + String.format("%.1f", currentSize));
                }
            }
        }.runTaskTimer(plugin, shrinkDelay, 5 * 60 * 20); // 每5分钟通知一次
    }

    //初始化传送位置
    private void initializeTeleportLocations() {
        World world = loc1.getWorld();
        if (world == null) return;
        // 东南西北四个方向，距离中心250格
        int[][] offsets = {{250, 0}, {0, 250}, {-250, 0}, {0, -250}};

        for (int[] offset : offsets) {
            double x = loc1.getX() + offset[0];
            double z = loc1.getZ() + offset[1];
            int y = world.getHighestBlockYAt((int)x, (int)z);

            //location为四个位置
            Location location = new Location(world, x, y + 1, z);
            teleportLocations.add(ensureLocationSafety(location));
        }
    }

    public Location getUHCTeleportLocation() {
        if (teleportLocations.isEmpty()) {
            // 如果没有预设的位置，返回中心位置
            return loc1;
        }
        // 随机选择一个预设的位置
        return teleportLocations.get(random.nextInt(teleportLocations.size()));
    }

    private Location ensureLocationSafety(Location location) {
        World world = location.getWorld();
        if (world == null) return location;
//        // 确保位置下方是实心方块
//        while (location.getBlock().getType().isAir() ) {
//            location.setY(location.getY() - 1);
//        }

        // 确保位置上方两格是空气
//        location.setY(location.getY() + 1);
//        while (!location.getBlock().getType().isAir() ||
//                !location.clone().add(0, 1, 0).getBlock().getType().isAir()) {
//            location.setY(location.getY() + 1);
//        }

        return location;
    }


    public void preloadChunks(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        int centerX = center.getBlockX() >> 4;
        int centerZ = center.getBlockZ() >> 4;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int chunkX = centerX + x;
                int chunkZ = centerZ + z;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ);
                }
            }
        }
        plugin.getLogger().info("已预加载 loc1 周围 " + radius + " 个区块半径的区域");
    }


    public void createOrLoadLobbyWorld() {
        lobbyWorld = Bukkit.getWorld("lobby");
        if (lobbyWorld == null) {
            plugin.getLogger().info("正在创建lobby世界......");
            WorldCreator creator = new WorldCreator("lobby");
            creator.environment(World.Environment.NORMAL);
            creator.type(WorldType.FLAT);
            lobbyWorld = creator.createWorld();
        } else {
            plugin.getLogger().info("Lobby已经存在，正在加载......");
        }

        if (lobbyWorld != null) {
            // 设置大厅世界的游戏规则
            lobbyWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            lobbyWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            lobbyWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            lobbyWorld.setTime(12200); // 设置时间为黄昏
            lobbyWorld.setDifficulty(Difficulty.PEACEFUL);
        } else {
            plugin.getLogger().severe("未能创建或加载lobby");
        }
    }


    public void teleportToLobby(Player player) {
        if (lobbyWorld != null) {
            Location loc0 = new Location(lobbyWorld, 0, 117, 0);
            player.teleport(loc0);
        } else {
            plugin.getLogger().warning("无法将玩家传送至大厅，因为大厅世界不存在。");
        }
    }

    public void giveItems(Inventory inv){
        inv.addItem(compass.createTrackingCompass());
    }

    // 辅助方法：创建附魔物品
    private ItemStack createEnchantedItem(Material material, Enchantment enchantment, int level) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
            item.setItemMeta(meta);
        }
        return item;
    }


    public void startNightVisionForAllOnlinePlayers() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
                }
            }
        }.runTaskTimer(plugin, 0L, 80L);
    }











}

