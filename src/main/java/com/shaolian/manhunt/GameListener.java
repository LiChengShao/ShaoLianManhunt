// GameListener.java
package com.shaolian.manhunt;

import com.shaolian.manhunt.GameManager;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Item;
import org.bukkit.block.Block;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.advancement.Advancement;
import org.checkerframework.checker.units.qual.K;

public class GameListener implements Listener {
    private final double damageReductionFactor = 0.2; // 将伤害减少到原来的20%
    private Set<UUID> recentBedExplosions = new HashSet<>();

    private final GameManager gameManager;
    private final Main plugin;
    private RecoveryCompass recoveryCompass;
    private PlayerData playerData;

    private static final long STARTUP_SEED = System.currentTimeMillis(); // 插件启动时生成的固定种子

    public GameListener(GameManager gameManager, Main plugin, PlayerData playerData) {
        this.gameManager = gameManager;
        this.plugin = plugin;
        this.recoveryCompass = plugin.getRecoveryCompass();
        this.playerData  = playerData;
        //监听y坐标变化
        monitorPlayerPosition();

    }

    private final Map<UUID, ItemStack[]> hiddenArmor = new HashMap<>();
    private final double SCALE_FACTOR = 8.0; // 新的缩放因子
    private boolean netherAdvancementTriggered = false;
    private boolean strongHoldAdvancementTriggered = false;
    private boolean endUnlocked = false; // 标记是否已经解锁指令
    private final Set<UUID> usedGotoStrongHoldCommand = new HashSet<>();
    private Location strongHoldLocation  = null;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        gameManager.playerJoin(playerId);
    }

    //玩家退出(离线)事件
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        gameManager.playerQuit(playerId);
    }

    //监听玩家死亡事件
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        //如果游戏还没有开始,玩家立即重生，在lobby,0,117,0复活
        if (GameManager.allowback) {
            return;
        }

        Player player = event.getEntity();
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();

        Player killer = player.getKiller();
        String killerName = player.getKiller() != null ? player.getKiller().getName() : null;
        UUID killerId = player.getKiller() != null ? player.getKiller().getUniqueId() : null;

        createDeathMarker(player);

        // 更新死亡玩家的死亡数
        playerData.updatePlayerStats(playerName, 0, 1);

        // 更新击杀者的击杀数
        if (killerName != null) {
            playerData.updatePlayerStats(killerName, 1, 0);
        }

        if(RecoveryCompass.finalMode.equals("要塞战争")
                || RecoveryCompass.finalMode.equals("一追多模式")){
            //如果玩家是逃亡者，设置为旁观者，从逃亡者阵营移除,并检查游戏是否结束
            if (gameManager.isRunner(playerId)) {
                player.setGameMode(GameMode.SPECTATOR);
                gameManager.removeRunner(playerId);
                GameManager.deathRunners.add(playerId);
                gameManager.checkGameEnd();
                gameManager.checkIfOnlyOneLeft();
            }
            //如果玩家是猎人
            if (gameManager.isHunter(playerId)) {
                // 设置保留物品栏
                event.setKeepInventory(true);
                // 清空掉落物列表，确保不会有任何物品掉落
                event.getDrops().clear();
                // 保留玩家的经验
                event.setKeepLevel(true);
                event.setDroppedExp(0);
        }

            //增加猎人生命上限
            if (RecoveryCompass.finalMode.equals("一追多模式")) {
                //如果死亡的是逃亡者，杀手是猎人
                if (!gameManager.isHunter(playerId) && gameManager.isHunter(killerId)) {
                    //杀手猎人加一颗星的生命上限
                    killer.getAttribute(Attribute.MAX_HEALTH).setBaseValue(player.getAttribute(Attribute.MAX_HEALTH).getValue() + 1);
                }
            }
    }

        if(RecoveryCompass.finalMode.equals("原版猎人") ||
                RecoveryCompass.finalMode.equals("随机掉落原版猎人")
        || RecoveryCompass.finalMode.equals("内鬼模式")
        || RecoveryCompass.finalMode.equals("主播模式")
        ){
            //如果玩家是逃亡者，设置为旁观者，从逃亡者阵营移除,并检查游戏是否结束
            if (gameManager.isRunner(playerId)) {
                player.setGameMode(GameMode.SPECTATOR);
                gameManager.removeRunner(playerId);
                GameManager.deathRunners.add(playerId);
                gameManager.checkGameEnd();
                gameManager.checkIfOnlyOneLeft();
            }
            //如果玩家是猎人
            if (gameManager.isHunter(playerId)) {
                // 获取玩家的物品栏
                PlayerInventory inventory = player.getInventory();
                // 创建一个列表来存储需要保留的物品
                List<ItemStack> itemsToKeep = new ArrayList<>();
                // 遍历玩家物品栏
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && item.getType() == Material.COMPASS) {
                        ItemMeta meta = item.getItemMeta();
                        if (meta != null && meta.hasCustomModelData() && meta.getCustomModelData() == 999) {
                            // 如果是追杀指南针，添加到保留列表
                            itemsToKeep.add(item);
                        }
                    }
                }
                // 清空掉落物列表
                event.getDrops().clear();
                // 将所有物品（除了追杀指南针）添加到掉落物列表
                for (ItemStack item : inventory.getContents()) {
                    if (item != null && !itemsToKeep.contains(item)) {
                        event.getDrops().add(item);
                    }
                }

                // 清空玩家物品栏
                inventory.clear();

                // 将保留的物品（追杀指南针）放回玩家物品栏
                for (ItemStack item : itemsToKeep) {
                    inventory.addItem(item);
                }
            }
        }
    }


    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        //如果游戏还没有开始,玩家立即重生，在lobby,0,117,0复活
        if (GameManager.allowback) {
            event.setRespawnLocation(new Location(GameManager.lobbyWorld, 0, 117, 0));
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        //如果玩家是猎人，给予最大生命值
        if(RecoveryCompass.finalMode.equals("要塞战争")){
            if (gameManager.isHunter(playerId)) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40);
                player.setHealth(40);
            }
        }

        if(RecoveryCompass.finalMode.equals("原版猎人") ||
                RecoveryCompass.finalMode.equals("随机掉落原版猎人")
                || RecoveryCompass.finalMode.equals("内鬼模式")
        || RecoveryCompass.finalMode.equals("一追多模式")){
            if (gameManager.isHunter(playerId)) {
                player.getAttribute(Attribute.MAX_HEALTH).setBaseValue(30);
                player.setHealth(30);
                Inventory inv = player.getInventory();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        gameManager.giveItems(inv);
                    }
                }.runTaskLater(plugin, 2L);
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // 检查死亡的实体是否是末影龙
        if (event.getEntity() instanceof EnderDragon) {
            gameManager.dragonDefeated();
            //计入分数
            for (UUID playerId : GameManager.runners) {
                String playerName = Bukkit.getPlayer(playerId).getName();
                plugin.getLogger().info("尝试增加该玩家的逃亡者的屠龙数:" + playerName);
                playerData.incrementPlayerWins(playerName);
            }
        }
        // 检查死亡的实体是否是末影人
        if (event.getEntityType() == EntityType.ENDERMAN) {
            // 确保掉落末影珍珠
            event.getDrops().clear(); // 清除原有掉落物
            event.getDrops().add(new ItemStack(Material.ENDER_PEARL)); // 添加末影珍珠
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.getEntityType() == EntityType.TNT) {
            Location center = event.getLocation();
            List<Block> destroyedBlocks = event.blockList();

            // 移除末地传送门框架
            Iterator<Block> iterator = destroyedBlocks.iterator();
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getType() == Material.END_PORTAL_FRAME) {
                    iterator.remove();
                }
            }

            // 添加周围的黑曜石到爆炸列表
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue; // 跳过中心方块

                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = blockLoc.getBlock();

                        // 如果是黑曜石，添加到破坏列表
                        if (block.getType() == Material.OBSIDIAN && !destroyedBlocks.contains(block)) {
                            destroyedBlocks.add(block);
                        }
                    }
                }
            }

            // 不需要调用 setBlockList，因为我们直接修改了 event.blockList()
        }
    }


    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        //如果无法移动
        if (gameManager.movementRestricted) {
            // 获取玩家的起始位置和目标位置
            Location from = event.getFrom();
            Location to = event.getTo();

            // 检查玩家是否移动了超过允许的距离
            if (from.getWorld().equals(to.getWorld()) && from.distance(to) > 0.1) {
                // 首先确保玩家在同一个世界中
                // 然后检查移动距离是否超过 0.1 个方块（可以根据需要调整这个值）

                // 允许玩家转动视角，但限制位置移动
                to.setX(from.getX());
                to.setY(from.getY());
                to.setZ(from.getZ());
                // 将目标位置的 X, Y, Z 坐标设置为起始位置的坐标
                // 这样保留了玩家的视角变化，但阻止了位置移动

                event.setTo(to);
                // 使用修改后的位置更新事件
            }
        }
    }

    // 防止玩家受到伤害
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // 检查是否是玩家受到伤害
        if (event.getEntity() instanceof Player) {
            // 如果伤害原因是摔落，取消伤害
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                //如果游戏没开始
                if (!GameManager.isGameRunning) {
                    event.setCancelled(true);
                }
            }

            // 如果允许伤害
            if(gameManager.isDamageAllowed()){
            }
            else{
                event.setCancelled(true);
            }
        }
    }

    //当玩家坐标小于-60的时候
    // 创建监听玩家位置的方法
    public void monitorPlayerPosition() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            //如果允许回溯,意思也就是游戏开始前的一段时间
            if (GameManager.allowback) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getLocation().getY() < 35) {
                        Location loc0 = new Location(GameManager.lobbyWorld, 0, 117, 0);
                        player.teleport(loc0);
                    }
                    if (player.getLocation().getY() < 93) {
                        //允许受到伤害
                        gameManager.allowDamage();
                    }
                    else{
                        gameManager.disableDamage();
                    }
                }
            }

        }, 0L, 2L); // 每0.1秒检查一次
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {

    }

    private void applySpecialInvisibility(Player player) {
        // 应用普通隐身效果
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 180, 0)); // 3分钟

        // 隐藏玩家的盔甲
        hidePlayerArmor(player);

        // 设置定时器来移除效果
        new BukkitRunnable() {
            @Override
            public void run() {
                removeSpecialInvisibility(player);
            }
        }.runTaskLater(plugin, 20 * 180); // 3分钟后移除效果
    }

    private void removeSpecialInvisibility(Player player) {

        showPlayerArmor(player);
    }

//    private void hidePlayerArmor(Player player) {
//        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
//
//                onlinePlayer.hidePlayer(plugin, player);
//
//        }
//    }

//    private void showPlayerArmor(Player player) {
//        for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
//
//                onlinePlayer.showPlayer(plugin, player);
//
//        }
//    }

    public void hidePlayerArmor(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] armorContents = inventory.getArmorContents();
        hiddenArmor.put(player.getUniqueId(), armorContents.clone());

        for (int i = 0; i < armorContents.length; i++) {
            if (armorContents[i] != null) {
                armorContents[i] = makeArmorInvisible(armorContents[i]);
            }
        }

        inventory.setArmorContents(armorContents);
        player.updateInventory();
    }

    public void showPlayerArmor(Player player) {
        UUID playerId = player.getUniqueId();
        if (hiddenArmor.containsKey(playerId)) {
            player.getInventory().setArmorContents(hiddenArmor.remove(playerId));
            player.updateInventory();
        }
    }

    private ItemStack makeArmorInvisible(ItemStack item) {
        ItemStack invisibleItem = item.clone();
        ItemMeta meta = invisibleItem.getItemMeta();
        if (meta != null) {
            // 设置自定义模型数据来使物品"不可见"
            meta.setCustomModelData(1);
            invisibleItem.setItemMeta(meta);
        }
        return invisibleItem;
    }

    public boolean hasHiddenArmor(Player player) {
        return hiddenArmor.containsKey(player.getUniqueId());
    }

    public void createDeathMarker(Player player) {
        Location deathLocation = player.getLocation();

        // 确保死亡位置是空气（防止覆盖其他方块）
        Block fenceBlock = deathLocation.getBlock();
        if (fenceBlock.getType() != Material.AIR) {
            deathLocation.setY(deathLocation.getWorld().getHighestBlockYAt(deathLocation) + 1);
            fenceBlock = deathLocation.getBlock();
        }

        // 放置木栅栏
        fenceBlock.setType(Material.OAK_FENCE);

        // 在木栅栏上方放置玩家头颅
        Block skullBlock = fenceBlock.getRelative(0, 1, 0);
        skullBlock.setType(Material.PLAYER_HEAD);

        // 设置头颅的朝向和玩家信息
        if (skullBlock.getState() instanceof Skull) {
            Skull skull = (Skull) skullBlock.getState();
            skull.setRotation(getClosestBlockFace(player.getLocation().getYaw()));
            skull.setOwningPlayer(player);
            skull.update();
        }

        // 记录日志
        plugin.getLogger().info(player.getName() + " 死亡，在 " +
                formatLocation(deathLocation) + " 创建了死亡标记。");
    }

    private BlockFace getClosestBlockFace(float yaw) {
        // 将yaw转换为0-360度范围
        yaw = (yaw % 360 + 360) % 360;
        // 根据yaw返回最接近的BlockFace
        if (yaw < 45 || yaw >= 315) return BlockFace.SOUTH;
        if (yaw < 135) return BlockFace.WEST;
        if (yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    private String formatLocation(Location loc) {
        return String.format("(世界: %s, X: %d, Y: %d, Z: %d)",
                loc.getWorld().getName(),
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ());
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();
        ItemStack item = event.getItem();

        if (action == Action.RIGHT_CLICK_BLOCK && clickedBlock != null &&
                isBed(clickedBlock.getType())) {
            World.Environment dimension = player.getWorld().getEnvironment();

            if (dimension == World.Environment.NETHER || dimension == World.Environment.THE_END) {
                // 在地狱或末地中右击床
                event.setCancelled(true); // 取消原始事件

                // 模拟床爆炸，但减少伤害
                simulateReducedBedExplosion(clickedBlock.getLocation());

                // 移除床
                removeBed(event);
            }
        }

        if (gameManager.isPearlDisabled() && event.getAction().name().contains("RIGHT_CLICK")) {
            if (item != null && item.getType() == Material.ENDER_PEARL) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.RED + "游戏开始后15秒内无法使用末影珍珠！");
            }
        }

    }

//    private void removeBed(Block bedBlock) {
//        // 获取床的数据
//        Bed bed = (Bed) bedBlock.getBlockData();
//
//        // 移除床的这一部分
//        bedBlock.setType(Material.AIR);
//
//        // 如果这是床的头部，也移除床的脚部
//        if (bed.getPart() == Bed.Part.HEAD) {
//            Block footBlock = bedBlock.getRelative(bed.getFacing().getOppositeFace());
//            if (footBlock.getType() == bedBlock.getType()) {
//                footBlock.setType(Material.AIR);
//            }
//        }
//        // 如果这是床的脚部，也移除床的头部
//        else if (bed.getPart() == Bed.Part.FOOT) {
//            Block headBlock = bedBlock.getRelative(bed.getFacing());
//            if (headBlock.getType() == bedBlock.getType()) {
//                headBlock.setType(Material.AIR);
//            }
//        }
//    }

    private void removeBed(PlayerInteractEvent event) {
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        // 修改为更安全的类型检查
        BlockData blockData = clickedBlock.getBlockData();
        if (blockData instanceof Bed) {
            Bed bed = (Bed) blockData;
            // 你的床处理逻辑
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            // 检查是否是由我们的模拟爆炸造成的伤害
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                World.Environment dimension = player.getWorld().getEnvironment();

                if ((dimension == World.Environment.NETHER ||
                        dimension == World.Environment.THE_END)
                        && !recentBedExplosions.isEmpty()) {
                    // 减少伤害
                    double newDamage = event.getDamage() * damageReductionFactor;
                    event.setDamage(newDamage);
                }
            }
        }
    }

    private boolean isBed(Material material) {
        return material.name().endsWith("_BED");
    }

    private void simulateReducedBedExplosion(Location location) {
        World world = location.getWorld();
        if (world != null) {
            // 创建一个唯一标识符
            UUID explosionId = UUID.randomUUID();
            recentBedExplosions.add(explosionId);

            // 创建爆炸
            world.createExplosion(location, 5.0f, true, true);

            // 在短暂延迟后移除标识符
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                recentBedExplosions.remove(explosionId);
            }, 1L); // 1 tick 后移除
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        if (event.getCause() == TeleportCause.NETHER_PORTAL) {
            Location from = event.getFrom();
            World fromWorld = from.getWorld();
            World toWorld;

            if (fromWorld.getEnvironment() == World.Environment.NORMAL) {
                // 从主世界到下界
                toWorld = event.getTo().getWorld();
                if (toWorld.getEnvironment() != World.Environment.NETHER) return;

                double newX = from.getX() / SCALE_FACTOR;
                double newZ = from.getZ() / SCALE_FACTOR;

                Location newLocation = new Location(toWorld, newX, from.getY(), newZ, from.getYaw(), from.getPitch());
                event.setTo(newLocation);
            }
            if (fromWorld.getEnvironment() == World.Environment.NETHER) {
                // 从下界到主世界
                toWorld = event.getTo().getWorld();
                if (toWorld.getEnvironment() != World.Environment.NORMAL) return;

                double newX = from.getX() * SCALE_FACTOR;
                double newZ = from.getZ() * SCALE_FACTOR;

                Location newLocation = new Location(toWorld, newX, from.getY(), newZ, from.getYaw(), from.getPitch());
                event.setTo(newLocation);
            }
        }
    }


    //防止旁观者跑图
    @EventHandler
    public void onPlayerMove2(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            World world = player.getWorld();
            WorldBorder border = world.getWorldBorder();
            Location playerLoc = player.getLocation();

            if (!isWithinBorder(playerLoc, border)) {
                Location safeLocation = getSafeLocation(playerLoc, border);
                player.teleport(safeLocation);
                player.sendMessage("§c你不能超出世界边界！");
            }
        }
    }

    private boolean isWithinBorder(Location location, WorldBorder border) {
        double size = border.getSize() / 2.0;
        double x = location.getX() - border.getCenter().getX();
        double z = location.getZ() - border.getCenter().getZ();
        return Math.abs(x) <= size && Math.abs(z) <= size;
    }

    private Location getSafeLocation(Location playerLoc, WorldBorder border) {
        double x = playerLoc.getX();
        double z = playerLoc.getZ();
        double y = playerLoc.getY();

        double borderSize = border.getSize() / 2.0;
        double centerX = border.getCenter().getX();
        double centerZ = border.getCenter().getZ();

        x = Math.max(centerX - borderSize + 1, Math.min(centerX + borderSize - 1, x));
        z = Math.max(centerZ - borderSize + 1, Math.min(centerZ + borderSize - 1, z));

        return new Location(playerLoc.getWorld(), x, y, z, playerLoc.getYaw(), playerLoc.getPitch());
    }

    @EventHandler
    public void onPlayerAdvancementDone(PlayerAdvancementDoneEvent event) {
        Player player = event.getPlayer();
        Advancement advancement = event.getAdvancement();
        UUID playerId = player.getUniqueId();

        //如果进入下界的成就没有解锁
        if (!netherAdvancementTriggered) {
            if(GameManager.runners.contains(playerId)) {
                if (advancement.getKey().getKey().equals("nether/root")) {
                    // 给所有在线玩家10个黑曜石
                    for (Player player2 : Bukkit.getOnlinePlayers()) {
                        //  player2.getInventory().addItem(new ItemStack(Material.OBSIDIAN, 10));
                        ItemStack obsidian = new ItemStack(Material.OBSIDIAN, 10);
                        // 尝试添加到背包，如果失败则掉落物品
                        if (player2.getInventory().addItem(obsidian).size() > 0) {
                            player2.getWorld().dropItemNaturally(player2.getLocation(), obsidian);
                        }
                        player2.sendMessage(ChatColor.GREEN + "第一个逃亡者" + player.getName() + "进入下界，所有人获得了10个黑曜石！");
                    }
                    netherAdvancementTriggered = true; // 标记为已触发
                }
            }
        }

        if (advancement.getKey().getKey().equals("story/follow_ender_eye")) { // 隔墙有眼成就的Key
            if (player.getGameMode() == GameMode.SPECTATOR) return; // 忽略旁观者模式玩家
            if (!GameManager.runners.contains(playerId)) return; // 忽略非逃亡者玩家
            strongHoldLocation = player.getLocation();
            // 这里可以保存或处理该玩家坐标，比如打印日志
            Bukkit.getLogger().info("第一个获得隔墙有眼成就的非旁观者逃亡者玩家: " + player.getName() + "，坐标: " + strongHoldLocation.toVector());
            // 如果只监听第一个触发，可以加个标志位防止重复触发
            strongHoldAdvancementTriggered = true; // 标记为已触发


            // 如果传送要塞指令尚未解锁
            if ( !endUnlocked) {
                endUnlocked = true; // 标记为已解锁
                // 广播消息
                for(Player p : Bukkit.getOnlinePlayers()){
                    p.sendTitle(ChatColor.GREEN + "",  "逃亡者已进入要塞", 10, 70, 20);
                }
                Bukkit.broadcastMessage(ChatColor.GOLD + "★★★" + ChatColor.LIGHT_PURPLE +
                        "逃亡者已进入要塞！所有玩家已解锁传送到要塞的指令/gotoend！");
            }
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (GameManager.runners.contains(playerId)) {
           event.setFormat(ChatColor.AQUA + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
        }
        else if(GameManager.hunters.contains(playerId)){
            event.setFormat(ChatColor.RED + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
        }
        else if(Vote.outPlayerId != null && Vote.outPlayerId.equals(playerId)){
            event.setFormat(ChatColor.DARK_PURPLE + player.getName() + ChatColor.WHITE + ": " + event.getMessage());
        }
    }


   public boolean handleGotoEndCommand(Player player) {
        if (!endUnlocked) {
            player.sendMessage(ChatColor.RED + "传送要塞指令未解锁！");
            return true;
        }

       if (usedGotoStrongHoldCommand.contains(player.getUniqueId())) {
           player.sendMessage(ChatColor.RED + "你已经使用过要塞传送指令！");
           return true;
       }


       Location safeLocation = findSafeLocationInEnd();
       player.teleport(safeLocation);
       for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
           onlinePlayer.sendMessage(ChatColor.GREEN + player.getName() + "已传送至要塞附近位置！");
       }
       usedGotoStrongHoldCommand.add(player.getUniqueId());
       return true;
   }


    // 查找末地中的安全位置
    private Location findSafeLocationInEnd() {
        Random random = new Random();
        Location location;
        World world = Bukkit.getWorld("world");

        // 尝试最多 N 次找到安全位置
        for (int i = 0; i < 10; i++) {
            // 在玩家获得进入中心 50 格范围内随机生成位置
            int x = (int) strongHoldLocation.getX() + random.nextInt(50) - 25; // -50 到 +50 范围内随机
            int z = (int) strongHoldLocation.getZ() + random.nextInt(50) - 25; // -50 到 +50 范围内随机
            int y = world.getHighestBlockYAt(x, z) + 1;

            location = new Location(world, x, y, z);

            // 检查位置是否安全
            if (isLocationSafe(location)) {
                return location;
            }
        }

        int defaultx = (int)strongHoldLocation.getX();
        int defaultz = (int)strongHoldLocation.getZ();
        int defaulty = world.getHighestBlockYAt(defaultx, defaultz) + 1;

        // 如果找不到安全位置，返回默认位置
        return new Location(world, defaultx, defaulty, defaultz);
    }

    // 检查位置是否安全
    private boolean isLocationSafe(Location location) {
        Block block = location.getBlock();
        Block below = block.getRelative(BlockFace.DOWN);

        // 检查脚下方块是否安全
        if (below.getType() == Material.AIR || below.getType() == Material.LAVA) {
            return false;
        }

        // 检查周围是否有危险方块
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block adjacent = block.getRelative(x, 0, z);
                if (adjacent.getType() == Material.LAVA) {
                    return false;
                }
            }
        }

        return true;
    }

    //随机掉落
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // 检查游戏模式是否为"随机掉落原版猎人"
        if(RecoveryCompass.finalMode == null){
            return;
        }

        if (!RecoveryCompass.finalMode.equals("随机掉落原版猎人")) {
            return;
        }

        Block block = event.getBlock();
        Material blockType = block.getType();
        Player player = event.getPlayer();

        // 取消原版掉落
        event.setDropItems(false);

        // 定义随机掉落物
        ItemStack randomDrop = getRandomDropForBlock(blockType);

        // 如果随机掉落物不为空，则掉落
        if (randomDrop != null) {
            block.getWorld().dropItemNaturally(block.getLocation(), randomDrop);
          //  player.sendMessage("§a你获得了随机掉落物: " + randomDrop.getType().name());
        }
    }

    private ItemStack getRandomDropForBlock(Material blockType) {
        // 获取所有Material值
        Material[] allMaterials = Material.values();

        // 过滤掉不可获取的物品（如空气、命令方块等）
        List<Material> possibleDrops = Arrays.stream(allMaterials)
                .filter(m -> m.isItem() && !m.isAir() && !m.isLegacy())
                // 过滤掉战利品（唱片等）
                .filter(m -> !m.name().endsWith("_RECORD"))
                // 过滤掉合成物品（盔甲、工具、工作台等）
                .filter(m -> !m.name().endsWith("_HELMET") && !m.name().endsWith("_CHESTPLATE") &&
                        !m.name().endsWith("_LEGGINGS") && !m.name().endsWith("_BOOTS") &&
                        !m.name().endsWith("_SWORD") && !m.name().endsWith("_PICKAXE") &&
                        !m.name().endsWith("_AXE") && !m.name().endsWith("_SHOVEL") &&
                        !m.name().endsWith("_HOE") && !m.name().equals("CRAFTING_TABLE"))
                // 过滤掉生存模式无法获得的物品（刷怪蛋、命令方块等）
                .filter(m -> !m.name().endsWith("_SPAWN_EGG") && !m.name().startsWith("COMMAND_"))
                // 过滤掉命令方块、基岩、屏障以及调试棒
                .filter(m -> !m.equals(Material.BEDROCK) && !m.equals(Material.BARRIER) &&
                        !m.equals(Material.DEBUG_STICK) && !m.equals(Material.COMMAND_BLOCK))
                // 过滤掉SHERD结尾的物品
                .filter(m -> !m.name().endsWith("_SHERD"))
                .collect(Collectors.toList());

        // 使用插件启动时生成的固定种子和方块类型生成种子
        long seed = STARTUP_SEED + blockType.name().hashCode();
        Random random = new Random(seed);
        // 从过滤后的列表中选择物品
        Material randomMaterial = possibleDrops.get(random.nextInt(possibleDrops.size()));

//        // 随机选择一个物品
//        Random random = new Random();
//        Material randomMaterial = possibleDrops.get(random.nextInt(possibleDrops.size()));

        // 返回1-2个随机数量的掉落物
        return new ItemStack(randomMaterial, random.nextInt(2) + 1);
    }





    // 监听玩家点击背包事件
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        checkHelmet(player);
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null) return; // 添加 null 检查
        //如果游戏没开始，点击帽子无效
        if(!GameManager.isGameRunning) {
            if (event.getSlotType() == InventoryType.SlotType.ARMOR && event.getSlot() == 39) {
                event.setCancelled(true);
            }
        }

    }

    // 监听玩家拖动背包事件
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        checkHelmet(player);
    }

    @EventHandler
    public void onPlayerInteract2(PlayerInteractEvent event) {
       if(GameManager.isGameRunning){
           return;
       }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        checkHelmet(player);
    }

    private final NamespacedKey armorKey = new NamespacedKey("your_plugin_name", "extra_armor");
    // 检查玩家头盔槽位的物品
    private void checkHelmet(Player player) {
        // 获取玩家头盔槽位的物品
        ItemStack helmet = player.getInventory().getHelmet();
        // 获取当前头盔带来的护甲值
        double currentHelmetArmor = player.getPersistentDataContainer().getOrDefault
                (armorKey, PersistentDataType.DOUBLE, 0.0);

        // 重置头盔带来的护甲值
        player.getAttribute(Attribute.ARMOR).setBaseValue(player.getAttribute(Attribute.ARMOR)
                .getBaseValue() - currentHelmetArmor);
        player.getPersistentDataContainer().set(armorKey, PersistentDataType.DOUBLE, 0.0);


        if (helmet == null || helmet.getType() == Material.AIR) {
            // 移除护甲值效果
            double currentArmor = player.getPersistentDataContainer().getOrDefault(armorKey, PersistentDataType.DOUBLE, 0.0);
            player.getPersistentDataContainer().set(armorKey, PersistentDataType.DOUBLE, Math.max(0.0, currentArmor - 3.0));
            player.getAttribute(Attribute.ARMOR).setBaseValue(Math.max(0.0, player.getAttribute(Attribute.ARMOR).getBaseValue() - 3.0));
        }

        else  {
            // 给玩家增加护甲值
            double currentArmor = player.getPersistentDataContainer().getOrDefault(armorKey, PersistentDataType.DOUBLE, 0.0);
            player.getPersistentDataContainer().set(armorKey, PersistentDataType.DOUBLE, currentArmor + 3.0);
            player.getAttribute(Attribute.ARMOR).setBaseValue(player.getAttribute(Attribute.ARMOR).getBaseValue() + 3.0);
        }
    }








}
