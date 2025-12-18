package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StatsCommand implements CommandExecutor {
    private final PlayerData playerData;
    private final Main plugin;

    public StatsCommand(Main plugin, PlayerData playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /show <kills|kd|wins> <玩家名>");
            return true;
        }

        String type = args[0].toLowerCase();
        String playerName = args[1];

        switch (type) {
            case "kills":
                showKills(sender, playerName);
                break;
            case "kd":
                showKD(sender, playerName);
                break;
            case "wins":
                showWins(sender, playerName);
                break;
            default:
                sender.sendMessage("§c无效的统计类型！可用类型: kills, kd, wins");
                break;
        }

        return true;
    }

    private void showKills(CommandSender sender, String playerName) {
        try {
            String sql = "SELECT kills FROM player_stats WHERE player_name = ?";
            try (PreparedStatement pstmt = playerData.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int kills = rs.getInt("kills");
                    sender.sendMessage("§a玩家 " + playerName + " 的击杀数: §e" + kills);
                } else {
                    sender.sendMessage("§c未找到玩家 " + playerName + " 的统计数据");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage("§c获取击杀数据时发生错误");
            e.printStackTrace();
        }
    }

    private void showKD(CommandSender sender, String playerName) {
        try {
            String sql = "SELECT kills, deaths FROM player_stats WHERE player_name = ?";
            try (PreparedStatement pstmt = playerData.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int kills = rs.getInt("kills");
                    int deaths = rs.getInt("deaths");
                    int a = 999;
                    double kd = deaths == 0 ? a : (double) kills / deaths;
                    sender.sendMessage("§a玩家 " + playerName + " 的KD值: §e" + String.format("%.2f", kd));
                } else {
                    sender.sendMessage("§c未找到玩家 " + playerName + " 的统计数据");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage("§c获取KD数据时发生错误");
            e.printStackTrace();
        }
    }

    private void showWins(CommandSender sender, String playerName) {
        try {
            String sql = "SELECT wins FROM player_stats WHERE player_name = ?";
            try (PreparedStatement pstmt = playerData.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    int wins = rs.getInt("wins");
                    sender.sendMessage("§a玩家 " + playerName + " 的屠龙次数: §e" + wins);
                } else {
                    sender.sendMessage("§c未找到玩家 " + playerName + " 的统计数据");
                }
            }
        } catch (SQLException e) {
            sender.sendMessage("§c获取胜利数据时发生错误");
            e.printStackTrace();
        }
    }
} 