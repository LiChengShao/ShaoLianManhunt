package com.shaolian.manhunt;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.bukkit.event.Listener;

public class PlayerData implements Listener {

    private Main plugin;
    private Connection connection;

    public PlayerData(Main plugin) {
        this.plugin = plugin;
        // 移除这行，因为 PlayerData 本身在这里不需要监听任何 Bukkit 事件（除非你有其他用途）
        // plugin.getServer().getPluginManager().registerEvents(this, plugin);

        connectDatabase();
        createTable(); // 确保在尝试任何操作前表已创建/更新
        plugin.getLogger().info("PlayerData初始化完成");
    }

    private void connectDatabase() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File dbFile = new File(dataFolder, "uhc.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("成功连接数据库");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("连接数据库失败: " + e.getMessage());
        }
    }

    private void createTable() {
        // 添加 wins 列，并设置默认值为 0
        String sql = "CREATE TABLE IF NOT EXISTS player_stats ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "player_name TEXT NOT NULL UNIQUE,"
                + "kills INTEGER NOT NULL DEFAULT 0," // 添加 DEFAULT 0
                + "deaths INTEGER NOT NULL DEFAULT 0," // 添加 DEFAULT 0
                + "wins INTEGER NOT NULL DEFAULT 0"   // 新增 wins 列
                + ");";

        // 处理可能存在的表结构变更（如果表已存在但没有wins列）
        // 这是一个简化的处理，更健壮的方案会检查列是否存在
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            plugin.getLogger().info("数据表player_stats检查/创建成功");

            // 尝试添加 wins 列，如果它不存在的话
            // 注意：这种方式在某些数据库系统上可能因表已存在该列而报错，但SQLite通常会忽略
            // 更安全的做法是查询表结构 (PRAGMA table_info(player_stats)) 来决定是否需要ALTER
            try {
                stmt.execute("ALTER TABLE player_stats ADD COLUMN wins INTEGER NOT NULL DEFAULT 0;");
                plugin.getLogger().info("尝试为 player_stats 添加 wins 列 (如果不存在)");
            } catch (SQLException e) {
                // 如果列已存在，SQLite会抛出 "duplicate column name: wins" 错误，这是正常的，可以忽略
                if (e.getMessage().contains("duplicate column name") || e.getMessage().contains("column wins already exists")) {
                    plugin.getLogger().info("wins 列已存在于 player_stats。");
                } else {
                    plugin.getLogger().warning("为 player_stats 添加 wins 列失败 (可能是其他原因): " + e.getMessage());
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("创建/更新数据表失败: " + e.getMessage());
        }
    }

    /**
     * 更新玩家的击杀和死亡次数。
     * 如果玩家不存在，则会创建新记录。
     * @param playerName 玩家名称
     * @param killsToAdd 要增加的击杀数
     * @param deathsToAdd 要增加的死亡数
     */
    public void updatePlayerStats(String playerName, int killsToAdd, int deathsToAdd) {
        // 保持 wins 不变，如果插入新行，wins会是默认的0
        String sql = "INSERT INTO player_stats(player_name, kills, deaths, wins) VALUES(?, ?, ?, 0) "
                + "ON CONFLICT(player_name) DO UPDATE SET "
                + "kills = kills + excluded.kills, "
                + "deaths = deaths + excluded.deaths;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.setInt(2, killsToAdd);
            pstmt.setInt(3, deathsToAdd);
            pstmt.executeUpdate();
            plugin.getLogger().info("成功更新玩家 " + playerName + " 的击杀/死亡统计数据");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("更新玩家 " + playerName + " 击杀/死亡统计数据失败: " + e.getMessage());
        }
    }

    /**
     * 为指定玩家增加一次胜利。
     * 如果玩家不存在，则会创建新记录，并将胜利次数设为1，其他统计数据设为0。
     * @param playerName 玩家名称
     */
    public void incrementPlayerWins(String playerName) {
        // 如果玩家不存在，插入新记录，wins为1，kills和deaths为0
        // 如果玩家存在，仅更新wins
        String sql = "INSERT INTO player_stats(player_name, kills, deaths, wins) VALUES(?, 0, 0, 1) "
                + "ON CONFLICT(player_name) DO UPDATE SET "
                + "wins = wins + 1;";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
            plugin.getLogger().info("玩家 " + playerName + " 的胜利次数已增加");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("增加玩家 " + playerName + " 胜利次数失败: " + e.getMessage());
        }
    }

    // 最好在插件卸载时关闭数据库连接
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("数据库连接已关闭");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("关闭数据库连接失败: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}