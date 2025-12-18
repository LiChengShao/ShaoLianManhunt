package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;


import java.util.HashMap;
import java.util.UUID;

public class TP implements CommandExecutor {
    private final Main plugin;

    public TP (Main plugin) {
        this.plugin = plugin;
        //monitorNearestRunnerDistance();
    }

    //传送相关代码
    private static final double OVERWORLD_TELEPORT_THRESHOLD = 1000.0;
    private static final double NETHER_TELEPORT_THRESHOLD = 400.0;
    private static final double OVERWORLD_TELEPORT_RANGE = 300.0;
    private static final double NETHER_TELEPORT_RANGE = 150.0;
    private static final long COOLDOWN_TIME = 600000; // 10分钟的冷却时间（毫秒）

    private long gameStartTime = 0; // 添加游戏开始时间字段

    // 用于跟踪玩家冷却时间的 HashMap
    private final HashMap<UUID, Long> cooldowns = new HashMap<>();

    double requiredDistance = 2500;
    double requiredDistance_nether = 1000;

    // 添加设置游戏开始时间,并且每秒增加一次
    public void setGameStartTime() {
        this.gameStartTime = 0;
        new BukkitRunnable() {
            @Override
            public void run() {
                gameStartTime++;
            }
        }.runTaskTimer(plugin,0, 20L);
    }
    private Player getNearestRunner(Player hunter) {
        Player nearestRunner = null;
        //用一个极大的数赋值给minDistance
        double minDistance = Double.MAX_VALUE;

        // 遍历所有逃亡者
        for (UUID runnerId : GameManager.runners) {
            Player runner = Bukkit.getPlayer(runnerId);
            //前提是两者在同一个世界
            if (runner != null && runner.isOnline() && runner.getWorld() == hunter.getWorld()) {
                // 计算逃亡者和猎人的距离
                double distance = hunter.getLocation().distance(runner.getLocation());
                // 如果距离小于当前最小距离(最开始这是一个极大的值，所以这个if必然为true)
                if (distance < minDistance) {
                    //最小距离会变成两者的距离
                    minDistance = distance;
                    nearestRunner = runner;
                }
            }
        }

        return nearestRunner;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用这个命令。");
            return true;
        }
        if (RecoveryCompass.finalMode.equals("主播模式")) {
            sender.sendMessage("该模式无法使用tpa");
            return true;
        }

        Player hunter = (Player) sender;
        if (!GameManager.hunters.contains(hunter.getUniqueId())) {
            hunter.sendMessage("§c只有猎人可以使用这个命令。");
            return true;
        }
        // 检查游戏开始时间是否超过20分钟
        if ( gameStartTime < 20 * 60) { // 20分钟
            long timeLeft = 1200 - gameStartTime;
            hunter.sendMessage("§c游戏开始后20分钟内无法使用此命令。剩余时间: " + timeLeft + "秒");
            hunter.sendMessage("§c并且你需要和目标逃生者达到一定距离才可以传送: §e主世界§l"
                    + requiredDistance + "§r§e格 §b下界§l"
                    + requiredDistance_nether + "§r§b格");
            return true;
        }
        if (command.getName().equalsIgnoreCase("tpa")) {
            // 检查冷却时间
            if (isOnCooldown(hunter)) {
                long timeLeft = getRemainingCooldown(hunter);
                hunter.sendMessage("§c该命令还在冷却中。剩余时间: " + formatTime(timeLeft));
                return true;
            }
            // 新增：检查参数数量
            if (args.length < 1) {
                hunter.sendMessage("§c用法: /tpa <玩家名>");
                return true;
            }
            String targetPlayerName = args[0];
            Player targetRunner = Bukkit.getPlayerExact(targetPlayerName);
            // 新增：检查目标玩家是否存在且在线
            if (targetRunner == null || !targetRunner.isOnline()) {
                hunter.sendMessage("§c玩家 " + targetPlayerName + " 不在线或不存在。");
                return true;
            }
            // 新增：检查目标玩家是否为逃亡者
            if (!GameManager.runners.contains(targetRunner.getUniqueId())) {
                hunter.sendMessage("§c" + targetPlayerName + " 不是逃亡者。");
                return true;
            }
            // 新增：检查猎人和目标逃亡者是否在同一个世界
            if (hunter.getWorld() != targetRunner.getWorld()) {
                hunter.sendMessage("§c你和 " + targetRunner.getName() + " 必须在同一个维度才能传送。");
                return true;
            }

            // 修改：距离检查逻辑
            double distance = hunter.getLocation().distance(targetRunner.getLocation());
            World.Environment env = hunter.getWorld().getEnvironment();
            double reqDistance;
            if (env == World.Environment.NORMAL) {
                // TODO
                reqDistance = requiredDistance;
                //requiredDistance = 2500.0;
            } else if (env == World.Environment.NETHER) {
                reqDistance = requiredDistance_nether;
            } else {
                hunter.sendMessage("§c末地无法使用该指令。");
                return true;
            }
            if (distance < reqDistance) {
                hunter.sendMessage("§c你距离 " + targetRunner.getName() + " 不足 " +
                        (int)reqDistance + " 格，无法传送。当前距离: " + String.format("%.2f", distance) + "格。");
                return true;
            }
            // 如果距离满足要求，继续执行传送逻辑
            World world = hunter.getWorld(); // 或者 targetRunner.getWorld()，因为它们在同一世界
            double teleportRange = world.getEnvironment() == World.Environment.NORMAL ?
                    OVERWORLD_TELEPORT_RANGE : NETHER_TELEPORT_RANGE;
            // 传送地点teleportLocation接收findSafeLocation方法的返回值
            // 传递指定逃亡者的位置以及传送范围
            Location teleportLocation = findSafeLocation(targetRunner.getLocation(), teleportRange);
            if (teleportLocation == null) {
                hunter.sendMessage("§c无法找到安全的传送位置。");
                return true;
            }
            hunter.teleport(teleportLocation);
            Bukkit.broadcastMessage("§6猎人" + "§c" + hunter.getName() + "§6 传送到了 §a" + targetRunner.getName() + "§6 附近。");
            // 设置冷却时间
            setCooldown(hunter);
            return true;
        }
        return false;
    }


    //返回是否位置安全的方法
    private boolean isSafeLocation(Location location) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        // 检查脚下和头部的方块是否是空气或水
        boolean feetSafe = !feet.getType().isSolid() || feet.getType() == Material.WATER;
        boolean headSafe = !head.getType().isSolid() || head.getType() == Material.WATER;

        // 检查地面是否是实心方块或水
        boolean groundSafe = ground.getType().isSolid() || ground.getType() == Material.WATER;
        return feetSafe && headSafe && groundSafe;

    }


    private Location findSafeLocation(Location center, double range) {
        World world = center.getWorld();
        boolean isNether = world.getEnvironment() == World.Environment.NETHER;

        for (int i = 0; i < 50; i++) { // 尝试50次
            double x = center.getX() + (Math.random() - 0.5) * 2 * range;
            double z = center.getZ() + (Math.random() - 0.5) * 2 * range;

            int y;
            if (isNether) {
                y = findSafeYInNether(world, (int)x, (int)z);
            } else {
                y = world.getHighestBlockYAt((int)x, (int)z);

                // y = findSafeYInOverworld(world, (int)x, (int)z);
            }

            //绝大概率是true
            if (y != -1) {
                Location location = new Location(world, x, y, z);
                if (isSafeLocation(location)) {
                    return location;
                }
            }
        }
        return null;
    }

//    private int findSafeYInOverworld(World world, int x, int z) {
//        int y = world.getHighestBlockYAt(x, z);
//        Block highestBlock = world.getBlockAt(x, y, z);
//
//        // 检查最高方块是否为水
//        if (highestBlock.getType() == Material.WATER) {
//            // 如果是水，返回水面的Y坐标
//            return y + 1;
//        } else {
//            // 如果不是水，检查上方两个方块是否为空气或水
//            Block aboveBlock = world.getBlockAt(x, y + 1, z);
//            Block twoBlocksAbove = world.getBlockAt(x, y + 2, z);
//
//            if ((aboveBlock.getType() == Material.AIR || aboveBlock.getType() == Material.WATER) &&
//                    (twoBlocksAbove.getType() == Material.AIR || twoBlocksAbove.getType() == Material.WATER)) {
//                return y + 1;
//            }
//        }
//
//        // 如果没有找到安全的位置，返回-1
//        return -1;
//    }

    private int findSafeYInNether(World world, int x, int z) {
        // 从底部向上搜索安全的位置
        for (int y = 1; y < 127; y++) {
            Block block = world.getBlockAt(x, y, z);
            Block above = world.getBlockAt(x, y + 1, z);
            Block below = world.getBlockAt(x, y - 1, z);

            if (!block.getType().isSolid() && !above.getType().isSolid() && below.getType().isSolid()
                    && !block.isLiquid() && !above.isLiquid() && !below.isLiquid()) {
                return y;
            }
        }
        return -1; // 没有找到安全的位置
    }

    // 检查玩家是否在冷却中
    private boolean isOnCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        return System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) < COOLDOWN_TIME;
    }

    // 设置玩家的冷却时间
    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    // 获取剩余冷却时间（毫秒）
    private long getRemainingCooldown(Player player) {
        long lastUsage = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return Math.max(COOLDOWN_TIME - (System.currentTimeMillis() - lastUsage), 0);
    }

    // 格式化时间（将毫秒转换为分钟和秒）
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d分%d秒", minutes, seconds);
    }

}
