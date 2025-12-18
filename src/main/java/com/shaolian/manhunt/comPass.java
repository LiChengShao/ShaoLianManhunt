package com.shaolian.manhunt;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class comPass implements Listener , CommandExecutor {
    private final Map<UUID, BukkitTask> trackingTasks = new HashMap<>();
    private Map<UUID, Inventory> playerInventories = new HashMap<>();
    //创建共享背包
    public Inventory sharedBag;
    public Inventory sharedBag2;
    private static final double ALERT_DISTANCE = 50.0;
    private static final long CHECK_INTERVAL = 20L; // 检查间隔（ticks）
    public static boolean isSharedBagEnabled = false; // 共享背包是否启用

    private int updateTaskId = -1; // 更新护身符lore的任务


    private final Main plugin;
    private GameManager gameManager;

    public comPass(Main plugin) {
        this.plugin = plugin;
        // 构造函数中初始化共享背包
        sharedBag = Bukkit.createInventory(null, 27, "§c猎人共享背包");
        sharedBag2 = Bukkit.createInventory(null, 27, "§c逃亡者共享背包");
        //启动雷达
        startProximityCheck();
    }

    public void setIsSharedBagEnabled(){
        if(RecoveryCompass.decideOpenORCloseBag() == "close" || RecoveryCompass.finalMode == "主播模式"){return;}

        // 5分钟后启用共享背包
        new BukkitRunnable() {
            @Override
            public void run() {
                isSharedBagEnabled = true;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (GameManager.hunters.contains(player.getUniqueId())) {
                        player.sendMessage("§a共享背包功能已启用！");
                    }
                }
            }
        }.runTaskLater(plugin, 5 * 60 * 20); // 5分钟
    }

    public void setGameManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }


    //创建指南针
    public ItemStack createTrackingCompass() {
        ItemStack trackCompass = new ItemStack(Material.COMPASS);
        ItemMeta meta = trackCompass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c追杀指南针");
            meta.setLore(Arrays.asList("§7左击指南针可选择要追踪的玩家", "§8ManhuntCompass"));
            meta.setUnbreakable(true);
            meta.setCustomModelData(999);
            trackCompass.setItemMeta(meta);
        }
        return trackCompass;
    }


    // 修改createAmulet方法
    public ItemStack createAmulet(Player runner) {
        ItemStack amulet = new ItemStack(Material.CLOCK);
        // 🌟 推荐：直接设置初始Lore，而不是扫描空背包
        updateSpecificAmuletLore(runner, amulet);
        // 启动定时任务，让它去扫描背包并更新
        startLoreUpdateTask(runner);
        return amulet;
    }

    private void updateSpecificAmuletLore(Player runner, ItemStack amulet) {
        if (amulet == null || amulet.getType() != Material.CLOCK) {
            return;
        }

        ItemMeta meta = amulet.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c护身符");

            List<String> lore = new ArrayList<>();
            lore.add("§7这是一个护身符");

            // 🌟 关键修复 1：创建列表的副本进行遍历
            // 这样即使 GameManager.hunters 在遍历过程中发生了变化（如玩家加入），也不会报错
            List<UUID> huntersSnapshot;
            try {
                huntersSnapshot = new ArrayList<>(GameManager.hunters);
            } catch (Exception e) {
                // 如果读取列表本身出错，防止崩溃
                huntersSnapshot = new ArrayList<>();
            }

            for (UUID hunterId : huntersSnapshot) {
                try {
                    Player hunter = Bukkit.getPlayer(hunterId);

                    // 🌟 关键修复 2：逻辑优化
                    // 只要 getPlayer 能获取到且不为 null，通常就意味着玩家在线
                    if (hunter != null && hunter.isOnline()) {
                        // 维度检查
                        if (runner.getWorld().equals(hunter.getWorld())) {
                            double distance = runner.getLocation().distance(hunter.getLocation());
                            lore.add(String.format("§6%s: §c%.1f米", hunter.getName(), distance));
                        } else {
                            lore.add(String.format("§6%s: §e不在同一个维度", hunter.getName()));
                        }
                    } else {
                        // 离线处理
                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(hunterId);
                        String name = offlinePlayer.getName();
                        if (name == null) {
                            name = "未知猎人";
                        }
                        lore.add(String.format("§6%s: §8已离线", name));
                    }
                } catch (Exception e) {
                    // 单个猎人数据出错不影响整体
                    e.printStackTrace();
                }
            }

            // 应用更改
            try {
                meta.setLore(lore);
                meta.setUnbreakable(true);
                meta.setCustomModelData(419);
                amulet.setItemMeta(meta);
            } catch (Exception e) {
                System.err.println("应用护身符Meta时出错: " + e.getMessage());
            }
        }
    }

    private void updateAmuletLore(Player runner) {
        PlayerInventory inventory = runner.getInventory();

        // 遍历背包，找到所有的 CLOCK 物品并更新
        for(int i = 0; i < 45; i++) {
            ItemStack item = inventory.getItem(i);
            if(item != null && item.getType() == Material.CLOCK) {
                // 调用新的方法来更新 Lore，实现代码复用和维度检查
                updateSpecificAmuletLore(runner, item);
            }
        }
    }

    // 添加启动定时任务的方法
    public void startLoreUpdateTask(Player runner) {
        if(updateTaskId != -1) {
            Bukkit.getScheduler().cancelTask(updateTaskId);
            updateTaskId = -1;
        }

        // 如果runner无效则不创建新任务
        if(runner == null || !runner.isOnline()) {
            return;
        }

        updateTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // 检查runner是否仍然有效
            if(runner == null || !runner.isOnline()) {
                Bukkit.getScheduler().cancelTask(updateTaskId);
                updateTaskId = -1;
                return;
            }
            try {
                updateAmuletLore(runner);
            } catch (Exception e) {
                // 捕获异常避免任务终止
                e.printStackTrace();
            }
        }, 0L, 20L);
    }



    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event){
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack item = event.getItem();

        //如果这个人是猎人
        if(GameManager.hunters.contains(playerId)){
            //如果物品是指南针
            if (item != null && item.getType() == Material.COMPASS ) {
                ItemMeta meta = item.getItemMeta();
                if(meta.hasCustomModelData() && meta.getCustomModelData() == 999){
                    event.setCancelled(true); // 取消原版的指南针的 行为
                    if(event.getAction() == Action.LEFT_CLICK_AIR ||
                            event.getAction() == Action.LEFT_CLICK_BLOCK){
                        //打开追踪GUI
                        openTrackingGUI(playerId);
                    }
                    else if(event.getAction() == Action.RIGHT_CLICK_AIR ||
                            event.getAction() == Action.RIGHT_CLICK_BLOCK){
                        //打开共同背包的方法
                        if(RecoveryCompass.decideOpenORCloseBag() == "close" || RecoveryCompass.finalMode == "主播模式"){
                            player.sendMessage("§c本局游戏中共享背包已被禁用！");
                            return;
                        }
                        openCommonBagGUI(playerId);
                    }
                }
            }
        }
        //如果这个人是逃生者
        if(GameManager.runners.contains(playerId)){
            //如果物品是指南针
            if (item != null && item.getType() == Material.COMPASS ) {
                ItemMeta meta = item.getItemMeta();
                if(meta.hasCustomModelData() && meta.getCustomModelData() == 999){
                    event.setCancelled(true); // 取消原版的指南针的 行为
                    if(event.getAction() == Action.LEFT_CLICK_AIR ||
                            event.getAction() == Action.LEFT_CLICK_BLOCK){
                        //打开追踪GUI
                        openTrackingGUI2(playerId);
                    }
                    else if(event.getAction() == Action.RIGHT_CLICK_AIR ||
                            event.getAction() == Action.RIGHT_CLICK_BLOCK){
                        //打开共同背包的方法
                        if(RecoveryCompass.decideOpenORCloseBag() == "close" || RecoveryCompass.finalMode == "主播模式"){
                            player.sendMessage("§c本局游戏中共享背包已被禁用！");
                            return;
                        }
                        openCommonBagGUI(playerId);
                    }
                }
            }
        }

        if (item != null && item.getType() == Material.FEATHER) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore()) {
                List<String> lore = meta.getLore();
                if (lore.contains("§8ManhuntFeatherSpeed")) {
                    // 取消原版的羽毛放置行为
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        // 获得一分钟的速度2
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 60, 1));
                        // 摆动主手或副手
                        if (player.getInventory().getItemInMainHand().equals(item)) {
                            player.swingMainHand();
                        } else if (player.getInventory().getItemInOffHand().equals(item)) {
                            player.swingOffHand();
                            player.getInventory().setItemInOffHand(null);
                        }
                        // 从背包中移除这个物品
                        player.getInventory().remove(item);
                        player.sendMessage(ChatColor.GREEN + "你获得了1分钟的速度II效果！");
                    }
                } else if (lore.contains("§8ManhuntFeatherFire")) {
                    // 取消原版的羽毛放置行为
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        // 获得一分钟的抗火效果
                        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60, 0));
                        // 摆动主手或副手
                        if (player.getInventory().getItemInMainHand().equals(item)) {
                            player.swingMainHand();
                        } else if (player.getInventory().getItemInOffHand().equals(item)) {
                            player.swingOffHand();
                            player.getInventory().setItemInOffHand(null);
                        }
                        // 从背包中移除这个物品
                        player.getInventory().remove(item);
                        player.sendMessage(ChatColor.RED + "你获得了1分钟的抗火效果！");
                    }
                } else if (lore.contains("§8ManhuntFeatherSwim")) {
                    // 取消原版的羽毛放置行为
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        // 获得一分钟的海豚的恩惠效果
                        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 60, 0));
                        // 摆动主手或副手
                        if (player.getInventory().getItemInMainHand().equals(item)) {
                            player.swingMainHand();
                            removeItemInMainHand(player);
                        } else if (player.getInventory().getItemInOffHand().equals(item)) {
                            player.swingOffHand();
                            player.getInventory().setItemInOffHand(null);
                        }
                        // 从背包中移除这个物品
                        player.getInventory().remove(item);
                        player.sendMessage(ChatColor.BLUE + "你获得了1分钟的海之眷顾效果！");
                    }
                } else if (lore.contains("§8ManhuntFeatherJump")) {
                    // 取消原版的羽毛放置行为
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        // 获得一分钟的跳跃提升3效果
                        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 60, 2));
                        // 摆动主手或副手
                        if (player.getInventory().getItemInMainHand().equals(item)) {
                            player.swingMainHand();
                        } else if (player.getInventory().getItemInOffHand().equals(item)) {
                            player.swingOffHand();
                            player.getInventory().setItemInOffHand(null);
                        }
                        // 从背包中移除这个物品
                        player.getInventory().remove(item);
                        player.sendMessage(ChatColor.YELLOW + "你获得了1分钟的跳跃提升III效果！");
                    }
                } else if (lore.contains("§8ManhuntFeatherRegen")) {
                    // 取消原版的羽毛放置行为
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        // 获得一分钟的生命恢复1效果
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 60, 0));
                        // 摆动主手或副手
                        if (player.getInventory().getItemInMainHand().equals(item)) {
                            player.swingMainHand();
                        } else if (player.getInventory().getItemInOffHand().equals(item)) {
                            player.swingOffHand();
                            player.getInventory().setItemInOffHand(null);
                        }
                        // 从背包中移除这个物品
                        player.getInventory().remove(item);
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "你获得了1分钟的生命恢复I效果！");
                    }
                } else if (lore.contains("§8ManhuntFeatherInvis")) {
                    // 取消原版的羽毛放置行为
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) {
                        // 获得一分钟的隐身效果
                        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 60, 0));
                        // 摆动主手或副手
                        if (player.getInventory().getItemInMainHand().equals(item)) {
                            player.swingMainHand();
                        } else if (player.getInventory().getItemInOffHand().equals(item)) {
                            player.swingOffHand();
                            player.getInventory().setItemInOffHand(null);
                        }
                        // 从背包中移除这个物品
                        player.getInventory().remove(item);
                        player.sendMessage(ChatColor.GRAY + "你获得了1分钟的隐身效果！");
                    }
                }
            }
        }
    }

    public void openTrackingGUI(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (GameManager.hunters.contains(playerId)) {  //如果这个playerId是猎人
            //创建GUI
            Inventory gui = Bukkit.createInventory(null, 27, "§c选择追踪目标");

            // 先显示逃亡者
            for (UUID targetId : GameManager.runners) {
                if (!targetId.equals(playerId)) { // 跳过自己
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null && target.isOnline()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(target);
                            meta.setDisplayName("§a逃亡者: " + target.getName());

                            List<String> lore = new ArrayList<>();
                            lore.add("§7点击开始追踪此玩家");
                            lore.add("§a身份: §a逃亡者");
                            meta.setLore(lore);

                            skull.setItemMeta(meta);
                            gui.addItem(skull);
                        }
                    }
                }
            }
            // 再显示猎人
            for (UUID targetId : GameManager.hunters) {
                if (!targetId.equals(playerId)) { // 跳过自己
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null && target.isOnline()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(target);
                            meta.setDisplayName("§c猎人: " + target.getName());

                            List<String> lore = new ArrayList<>();
                            lore.add("§7点击开始追踪此玩家");
                            lore.add("§c身份: §c猎人");
                            meta.setLore(lore);

                            skull.setItemMeta(meta);
                            gui.addItem(skull);
                        }
                    }
                }
            }
            player.openInventory(gui);
        }
    }

    private void openTrackingGUI2(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (GameManager.runners.contains(playerId)) {  //如果这个playerId是逃亡者
            //创建GUI的意思吧
            Inventory gui = Bukkit.createInventory(null, 27, "§a选择追踪同伴");
            // 显示逃亡者
            for (UUID targetId : GameManager.runners) {
                if (!targetId.equals(playerId)) { // 跳过自己
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null && target.isOnline()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(target);
                            meta.setDisplayName("§a逃亡者: " + target.getName());

                            List<String> lore = new ArrayList<>();
                            lore.add("§7点击开始追踪此玩家");
                            lore.add("§a身份: §a逃亡者");
                            meta.setLore(lore);

                            skull.setItemMeta(meta);
                            gui.addItem(skull);
                        }
                    }
                }
            }
            player.openInventory(gui);
        }
    }

    //打开共同背包
    public void openCommonBagGUI(UUID playerId){
        Player player = Bukkit.getPlayer(playerId);
        if(GameManager.hunters.contains(playerId)){//如果这个人是猎人
            if (isSharedBagEnabled) {
                player.openInventory(sharedBag);
            } else {
                player.sendMessage("§c共享背包功能将在游戏开始5分钟后启用！");
            }
        }else{
            // 如果这个人不是猎人
            player.openInventory(sharedBag2);
        }
    }


    //玩家点击GUI的头颅
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        //if (!(event.getWhoClicked() instanceof Player)) return;

        if (event.getView().getTitle().equals("§c选择追踪目标")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                Player hunter = (Player) event.getWhoClicked();
                ItemMeta itemMeta = clickedItem.getItemMeta();

                if (itemMeta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) itemMeta;
                    if (skullMeta.getOwningPlayer() != null) {
                        Player target = Bukkit.getPlayer(skullMeta.getOwningPlayer().getUniqueId());
                        if (target != null && target.isOnline()) {
                            startTracking(hunter, target);
                            hunter.closeInventory();
                            hunter.sendMessage("§a开始追踪: " + target.getName());
                        }
                    }
                }
            }
        }

        if (event.getView().getTitle().equals("§a选择追踪同伴")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                Player runner = (Player) event.getWhoClicked();
                ItemMeta itemMeta = clickedItem.getItemMeta();
                if (itemMeta instanceof SkullMeta) {
                    SkullMeta skullMeta = (SkullMeta) itemMeta;
                    if (skullMeta.getOwningPlayer() != null) {
                        Player target = Bukkit.getPlayer(skullMeta.getOwningPlayer().getUniqueId());
                        if (target != null && target.isOnline()) {
                            startTracking(runner, target);
                            runner.closeInventory();
                            runner.sendMessage("§a开始追踪: " + target.getName());
                        }
                    }
                }
            }
        }

        // 检查是否是共享背包
        if (event.getInventory().equals(sharedBag)) {
            Player player = (Player) event.getWhoClicked();
            // 检查是否是放置物品的操作
            if (event.getAction() == InventoryAction.PLACE_ALL ||
                    event.getAction() == InventoryAction.PLACE_ONE ||
                    event.getAction() == InventoryAction.PLACE_SOME) {

                ItemStack clickedItem = event.getCursor(); // 获取光标上的物品
                if (isSpecialItem(clickedItem)) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + "你不能将这个特殊物品放入共享背包！");
                }
            }
        }

    }


    private void startTracking(Player player1, Player target) {
        plugin.getLogger().info("开始追踪: 玩家 " + player1.getName() + " 追踪目标 " + target.getName());
        // 取消之前的追踪任务（如果存在）
        stopTracking(player1);

        // 立即更新指南针
        updateCompass(player1, target);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                // 检查游戏状态和玩家状态
                if (!player1.isOnline() || !target.isOnline() ||
                        (!gameManager.isHunter(target.getUniqueId()) &&
                                !gameManager.isRunner(target.getUniqueId()))) {
                    plugin.getLogger().info("追踪取消: 目标离线. 追踪者: " +
                            player1.getName() + ", 目标: " + target.getName());
                    this.cancel();
                    return;
                }

                updateCompass(player1, target);
            }
        }.runTaskTimer(plugin, 0L, 20L);
        // 保存新的追踪任务
        trackingTasks.put(player1.getUniqueId(), task);
    }


    private void updateCompass(Player player1, Player target) {
        ItemStack compass = null;
        for (ItemStack item : player1.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS &&
                    item.getItemMeta() != null &&
                    item.getItemMeta().getLore() != null &&
                    //!!!
                    item.getItemMeta().getLore().contains("§8ManhuntCompass")) {
                compass = item;
                break;
            }
        }

        if (compass != null) {
            CompassMeta meta = (CompassMeta) compass.getItemMeta();
            if (meta != null) {
                // 设置指南针指向目标位置
                Location targetLoc = target.getLocation();
                Location hunterLoc = player1.getLocation();

                if (hunterLoc.getWorld().equals(targetLoc.getWorld())) {
                    meta.setLodestone(targetLoc);
                    meta.setLodestoneTracked(false);
                    // 计算并更新距离
                    int distance = (int) player1.getLocation().distance(targetLoc);
                    meta.setDisplayName("§c追踪: " + target.getName() + " §7(距离: " +
                            distance + "米)");

                }else {
                    // 不同维度时显示提示
                    meta.setDisplayName("§c无法追踪: " + target.getName() + " §7(不同维度)");
                }


                // 确保更新Lore
                List<String> lore = new ArrayList<>();
                lore.add("§7左击指南针可选择要追踪的玩家");
                lore.add("§8ManhuntCompass");
                meta.setLore(lore);

                compass.setItemMeta(meta);
            }
        }

        // 更新Action Bar显示距离
        Location hunterLoc = player1.getLocation();
        Location targetLoc = target.getLocation();
    }


    private void stopTracking(Player player1) {
        BukkitTask task = trackingTasks.remove(player1.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    public boolean isSpecialItem(ItemStack item) {
        if (item == null) return false;

        // 检查是否是指南针
        if (item.getType() == Material.COMPASS) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§8ManhuntCompass")) {
                return meta.getLore().contains("§8ManhuntCompass");
            }
        }
        return false;
    }

    //
    public void startProximityCheck() {
        new BukkitRunnable() {
            @Override
            public void run() {
                //每 CHECK_INTERVAL(1s) 检查一次
                checkProximity();
            }
        }.runTaskTimer(plugin, 0L, CHECK_INTERVAL);
    }

    private void checkProximity() {
        for (UUID runnerId : GameManager.runners) {
            Player runner = Bukkit.getPlayer(runnerId);
//            if (runner == null || !runner.isOnline()) {
//                continue; // 跳过离线或无效的逃跑者
//            }
            for (UUID hunterId : GameManager.hunters) {
                Player hunter = Bukkit.getPlayer(hunterId);
                if (hunter == null || !hunter.isOnline()) {
                    continue; // 跳过离线或无效的猎人
                }

                if (runner.getWorld() == hunter.getWorld() &&
                        runner.getLocation().distance(hunter.getLocation()) <= ALERT_DISTANCE) {
                    alertRunner(runner);
                    break; // 一旦发现一个接近的猎人就跳出内循环
                }
            }
        }
    }

    private void alertRunner(Player runner) {
        //runner.sendMessage("§c警告：有猎人在你50格以内！");
        runner.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                TextComponent.fromLegacyText("§c警告：有猎人在你50格以内！"));
    }

    //创建救命毫毛一
    public ItemStack createFeatherSpeed() {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§2救命毫毛一");
            meta.setLore(Arrays.asList("§2右击可获得一分钟速度2", "§8ManhuntFeatherSpeed"));
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            feather.setItemMeta(meta);
        }
        return feather;
    }

    //创建救命毫毛二
    public ItemStack createFeatherFire() {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c救命毫毛二");
            meta.setLore(Arrays.asList("§c右击可获得一分钟抗火", "§8ManhuntFeatherFire"));
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            meta.setUnbreakable(true);
            feather.setItemMeta(meta);
        }
        return feather;
    }

    //创建救命毫毛三
    public ItemStack createFeatherSwim() {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§1救命毫毛三");
            meta.setLore(Arrays.asList("§1右击可获得一分钟海豚的恩惠", "§8ManhuntFeatherSwim"));
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            feather.setItemMeta(meta);
        }
        return feather;
    }

    //创建救命毫毛四
    public ItemStack createFeatherJump() {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e救命毫毛四");
            meta.setLore(Arrays.asList("§e右击可获得一分钟跳跃提升3", "§8ManhuntFeatherJump"));
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            feather.setItemMeta(meta);
        }
        return feather;
    }

    //创建救命毫毛五
    public ItemStack createFeatherRegen() {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d救命毫毛五");
            meta.setLore(Arrays.asList("§d右击可获得一分钟生命恢复1", "§8ManhuntFeatherRegen"));
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            feather.setItemMeta(meta);
        }
        return feather;
    }

    //创建救命毫毛六
    public ItemStack createFeatherInvis() {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f救命毫毛六");
            meta.setLore(Arrays.asList("§f右击可获得一分钟隐身", "§8ManhuntFeatherInvis"));
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.setUnbreakable(true);
            feather.setItemMeta(meta);
        }
        return feather;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§c只有玩家可以使用这个命令！");
            return true;
        }

        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("hushenfu")) {

            if (GameManager.runners.contains(player.getUniqueId())) {
                Inventory inv = player.getInventory();
                    ItemStack amulet = createAmulet(player);
                    HashMap<Integer, ItemStack> leftover = inv.addItem(amulet);
                    if (!leftover.isEmpty()) {
                        for (ItemStack item : leftover.values()) {
                            player.getWorld().dropItem(player.getLocation(), item);
                        }
                    }
                } else {
                    player.sendMessage("§c你不是逃亡者！");
                }
            return true;
            }


        // 检查玩家是否已经有追杀指南针
        if (!hasTrackingCompass(player)) {
            ItemStack compass = createTrackingCompass();
            player.getInventory().addItem(compass);
            player.sendMessage("§a你获得了一个追杀指南针！");
        } else {
            player.sendMessage("§c你已经有一个追杀指南针了！");
        }

        return true;
    }

    private boolean hasTrackingCompass(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == Material.COMPASS && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasCustomModelData() && meta.getCustomModelData() == 999) {
                    return true;
                }
            }
        }
        return false;
    }

    public void removeItemInMainHand(Player player) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand != null && !itemInHand.getType().isAir()) {
            if (itemInHand.getAmount() > 1) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            player.updateInventory();
        }
    }




}
