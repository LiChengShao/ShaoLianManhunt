package com.shaolian.manhunt;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import me.clip.placeholderapi.PlaceholderAPI;


public class PlayerVisibilityManager {

    private final JavaPlugin plugin;

    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private final Map<UUID, BukkitTask> updateTasks = new HashMap<>();

    public PlayerVisibilityManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }


//        public void setupCompass(Player player) {
//            BossBar bossBar = Bukkit.createBossBar("方向", BarColor.BLUE, BarStyle.SOLID);
//            bossBar.setVisible(true);
//            bossBar.addPlayer(player);
//            playerBossBars.put(player.getUniqueId(), bossBar);
//            updateCompass(player);
//        }
//
//        public void updateCompass(Player player) {
//        new BukkitRunnable() {
//            public void run() {
//                BossBar bossBar = playerBossBars.get(player.getUniqueId());
//                if (bossBar != null) {
//                    float yaw = player.getLocation().getYaw();
//                    String direction = getCardinalDirection(yaw);
//                    bossBar.setTitle("当前方向: " + direction);
//                }
//            }
//        }.runTaskTimer(plugin, 0L, 20L);
//        }
//
//        private String getCardinalDirection(float yaw) {
//            yaw = (yaw % 360 + 360) % 360;
//            if (yaw < 22.5 || yaw >= 337.5) return "南";
//            if (yaw >= 22.5 && yaw < 67.5) return "西南";
//            if (yaw >= 67.5 && yaw < 112.5) return "西";
//            if (yaw >= 112.5 && yaw < 157.5) return "西北";
//            if (yaw >= 157.5 && yaw < 202.5) return "北";
//            if (yaw >= 202.5 && yaw < 247.5) return "东北";
//            if (yaw >= 247.5 && yaw < 292.5) return "东";
//            return "东南";
//        }


    public void setupCompass(Player player) {
        // 启动定时更新任务
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                updateCompass(player);
            }
        }.runTaskTimer(plugin, 0L, 5L); // 每0.25秒更新一次
        updateTasks.put(player.getUniqueId(), task);
    }

    public void updateCompass(Player player) {
        BossBar bossBar = playerBossBars.computeIfAbsent(player.getUniqueId(),
                id -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_10)); // 修改为 NOTCHED_20


//        BossBar bossBar = playerBossBars.computeIfAbsent(player.getUniqueId(),
//                id -> Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID));

        // 获取玩家当前朝向
        float yaw = player.getLocation().getYaw();
        String direction = getCardinalDirection(yaw);

        // 构建罗盘显示
        // 获取玩家当前面朝方向的左侧方向（西），并添加到罗盘中
        // getRelativeDirection方法会根据当前方向返回正确的相对方向
        StringBuilder compass = new StringBuilder("|||");
        // 添加玩家当前面朝的方向到罗盘中间
        compass.append(getRelativeDirection(direction, "西")).append("|");
        compass.append(direction).append("|");
        // 获取玩家当前面朝方向的右侧方向（东），并添加到罗盘中
        compass.append(getRelativeDirection(direction, "东")).append("|");
        compass.append("|||");


        // 添加玩家位置标记
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.equals(player)) {
                int pos = getRelativePosition(player, other);
                if (pos >= 0 && pos < compass.length()) {
                    compass.replace(pos, pos + 1, ChatColor.RED + "P" + ChatColor.RESET);
                }
            }
        }

        // 设置BossBar的标题为罗盘内容
        bossBar.setTitle(compass.toString());
        // 设置进度为1.0，隐藏血量条
        bossBar.setProgress(1.0);
        bossBar.addPlayer(player);

    }


    private int getRelativePosition(Player player, Player other) {
        // 计算其他玩家相对于当前玩家的位置
        Location playerLoc = player.getLocation();
        Location otherLoc = other.getLocation();

        double dx = otherLoc.getX() - playerLoc.getX();
        double dz = otherLoc.getZ() - playerLoc.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - player.getLocation().getYaw();
        angle = (angle + 360) % 360;

        // 将角度映射到罗盘位置
        return (int) (angle / 22.5) + 3;
    }

    private String getRelativeDirection(String current, String target) {
        // 根据当前方向获取相对方向
        switch (current) {
            case "北":
                return target.equals("西") ? "西" : "东";
            case "南":
                return target.equals("西") ? "东" : "西";
            case "东":
                return target.equals("西") ? "北" : "南";
            case "西":
                return target.equals("西") ? "南" : "北";
            default:
                return "";
        }
    }

    private String getCardinalDirection(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw < 45 || yaw >= 315) return "南";
        if (yaw >= 45 && yaw < 135) return "西";
        if (yaw >= 135 && yaw < 225) return "北";
        return "东";
    }





}