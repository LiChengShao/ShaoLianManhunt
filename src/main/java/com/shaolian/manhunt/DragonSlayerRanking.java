package com.shaolian.manhunt;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;


//hologram全息字体排行榜
public class DragonSlayerRanking {
    private final Main plugin;
    private Hologram hologram;
    private Hologram gameInfoHologram; // 新增：游戏玩法全息字体
    private final PlayerData playerData;

    public DragonSlayerRanking(Main plugin) {
        this.plugin = plugin;
        this.playerData = plugin.getPlayerData();
        createHologram();
        createGameInfoHologram(); // 新增：创建游戏玩法全息字体
    }

    //这个方法被初始化了
    private void createHologram() {
        Location location = new Location(plugin.getServer().getWorld("lobby"), 0, 121, 0); // 设置全息图位置
        hologram = DHAPI.createHologram("dragon_slayer_ranking", location);
        updateHologram();
    }

    public void updateHologram() {
        new BukkitRunnable() {
            int animationFrame = 0;

            @Override
            public void run() {
                List<String> lines = new ArrayList<>();
                String[] colors = {"§c", "§6", "§e", "§a", "§b", "§d"};
                String titleColor = colors[animationFrame % colors.length];

                lines.add(titleColor + "✦ " + ChatColor.BOLD + "屠龙榜" + titleColor + " ✦");
                lines.add(titleColor + "✦ " + ChatColor.BOLD + "/show kills <玩家名> 显示玩家击杀数" + titleColor + " ✦");
                lines.add(titleColor + "✦ " + ChatColor.BOLD + "/show kd <玩家名> 显示玩家KD值" + titleColor + " ✦");
                lines.add(titleColor + "✦ " + ChatColor.BOLD + "/show wins <玩家名> 显示玩家屠龙数" + titleColor + " ✦");
                lines.add(ChatColor.GRAY + "➖➖➖➖➖➖➖➖➖➖➖");

                List<PlayerStats> topPlayers = getTopPlayersFromDatabase();
                for (int i = 0; i < topPlayers.size() && i < 5; i++) {
                    PlayerStats player = topPlayers.get(i);
                    String rankIcon = getRankIcon(i);
                    String playerName = formatPlayerName(player.getName(), i);
                    String wins = formatWins(player.getWins());

                    lines.add(rankIcon + " " + playerName + " " + wins);
                }

                lines.add(ChatColor.GRAY + "➖➖➖➖➖➖➖➖➖➖➖");
                lines.add(ChatColor.YELLOW + "每60秒更新一次");

                DHAPI.setHologramLines(hologram, lines);

                animationFrame++;
                if (animationFrame >= 60) {
                    this.cancel();
                    updateHologram(); // 重新开始动画
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
    }

    private String getRankIcon(int rank) {
        switch (rank) {
            case 0: return "§6§l👑";
            case 1: return "§f§l🥈";
            case 2: return "§c§l🥉";
            default: return "§7§l" + (rank + 1);
        }
    }

    private String formatPlayerName(String name, int rank) {
        switch (rank) {
            case 0: return "§6§l" + name;
            case 1: return "§f§l" + name;
            case 2: return "§c§l" + name;
            default: return "§7" + name;
        }
    }

    private String formatWins(int wins) {
        return "§e🏆 " + ChatColor.BOLD + wins;
    }

    private List<PlayerStats> getTopPlayersFromDatabase() {
        List<PlayerStats> topPlayers = new ArrayList<>();
        String query = "SELECT player_name, wins FROM player_stats ORDER BY wins DESC LIMIT 5";

        try {
            // 获取数据库文件路径
            File dbFile = new File(plugin.getDataFolder(), "uhc.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            
            // 每次查询时创建新的连接
            try (Connection conn = DriverManager.getConnection(url);
                 PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                while (rs.next()) {
                    String playerName = rs.getString("player_name");
                    int wins = rs.getInt("wins");
                    topPlayers.add(new PlayerStats(playerName, wins));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error fetching top players from database: " + e.getMessage());
            e.printStackTrace();
        }

        return topPlayers;
    }

    // 内部类改名为PlayerStats
    class PlayerStats {
        private final String name;
        private final int wins;

        public PlayerStats(String name, int wins) {
            this.name = name;
            this.wins = wins;
        }

        public String getName() {
            return name;
        }

        public int getWins() {
            return wins;
        }
    }

    // 新增方法：创建游戏玩法全息字体
    private void createGameInfoHologram() {
        Location location = new Location(plugin.getServer().getWorld("lobby"), 3, 122, -5); // 设置在全息图旁边
        gameInfoHologram = DHAPI.createHologram("game_info", location);
        updateGameInfoHologram();
    }

    // 新增方法：更新游戏玩法全息字体
    private void updateGameInfoHologram() {
        List<String> lines = new ArrayList<>();
        lines.add("§6§l游戏玩法说明");
        lines.add(ChatColor.GRAY + "➖➖➖➖➖➖➖➖➖➖➖");
        lines.add("§a猎人和逃生者比例最大程度接近2:1");
        lines.add("§a逃生者需要击杀末影龙获得游戏胜利");
        lines.add("§a猎人需要击杀所有逃生者获得游戏胜利");
        lines.add("§b逃生者前期有三根救命毫毛");
        lines.add("§b少量食物以及短暂的饱和效果");
        lines.add("§c猎人使用/tpa <玩家名> 可以传送到指定逃亡者附近");
        lines.add("§c(前20min被禁用,CD为10min)");
        lines.add("§f使用/compass获得一个指南针");
        lines.add("§f猎人在5min后开启共享背包功能");
        lines.add("§f逃亡者有追踪队友的指南针");
        lines.add("§e交易出珍珠的概率修改为10％左右");
        lines.add("§e击杀末影人100％掉落珍珠");
        lines.add("§e第一个逃亡者进入下界后会让所有玩家获得10个黑曜石");
        lines.add("§e第一个逃亡者进入要塞后,所有玩家将解锁传送到要塞的指令/gotoend");
        lines.add(ChatColor.GRAY + "➖➖➖➖➖➖➖➖➖➖➖");

        DHAPI.setHologramLines(gameInfoHologram, lines);
    }
}
