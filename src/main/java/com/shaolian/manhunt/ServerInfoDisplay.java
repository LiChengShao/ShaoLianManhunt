// FILEPATH: C:/Users/86178/Desktop/manhunt末地版/src/main/java/com/shaolian/manhunt/ServerInfoDisplay.java

package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.util.ArrayList;
import java.util.List;

public class ServerInfoDisplay {

    private Main plugin;
    private final String serverId;
    private final Scoreboard scoreboard;
    private final Objective objective;

    public ServerInfoDisplay(Main plugin,String serverId) {
        this.plugin = plugin;
        this.serverId = serverId;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = scoreboard.registerNewObjective("serverInfo", "dummy", ChatColor.GOLD + "服务器信息");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        //创建一个定时任务
        new BukkitRunnable() {
            @Override
            public void run() {
                updateScoreboard();
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void updateScoreboard() {
        // 清除旧的队伍
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }

        // 显示服务器ID
        addEntry(ChatColor.YELLOW + "IP: " + ChatColor.WHITE + serverId, 0);

        // 显示每个玩家的坐标
        int index = 1;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Location loc = player.getLocation();
            String coords = String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ());
            addEntry(ChatColor.AQUA + player.getName() + ": " + ChatColor.WHITE + coords, index++);
            if (index > 14) break; // 防止超出记分板限制
        }

        // 显示获得"结束了？"成就的玩家
        List<String> endAchievers = getEndAchievers();
        addEntry(ChatColor.YELLOW + "获得终末成就的玩家:", index++);
        if (!endAchievers.isEmpty()) {
            String achievers = String.join(", ", endAchievers);
            addEntry(ChatColor.WHITE + achievers, index);
        } else {
            addEntry(ChatColor.WHITE + "无", index);
        }

        // 为所有在线玩家设置记分板
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setScoreboard(scoreboard);
        }
    }

    private void addEntry(String text, int index) {
        Team team = scoreboard.registerNewTeam("line" + index);
        team.addEntry(ChatColor.values()[index].toString());
        team.setPrefix(text);
        objective.getScore(ChatColor.values()[index].toString()).setScore(15 - index);
    }

    private List<String> getEndAchievers() {
        List<String> achievers = new ArrayList<>();
        Advancement endAdvancement = Bukkit.getAdvancement(new NamespacedKey("minecraft", "story/enter_the_end"));

        if (endAdvancement != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                AdvancementProgress progress = player.getAdvancementProgress(endAdvancement);
                if (progress.isDone()) {
                    achievers.add(player.getName());
                }
            }
        }
        return achievers;
    }
}
