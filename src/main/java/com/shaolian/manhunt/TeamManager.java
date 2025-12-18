package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TeamManager {
    private Main plugin;

    private final Scoreboard scoreboard;
    private final List<Team> teams;

    public TeamManager(Main plugin) {
        this.plugin = plugin;

        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.teams = new ArrayList<>();
        initializeTeams();
    }

    private void initializeTeams() {
        createTeam("Blue", ChatColor.BLUE);
        createTeam("Green", ChatColor.GREEN);
        createTeam("Gold", ChatColor.GOLD);
        createTeam("Purple", ChatColor.LIGHT_PURPLE);
    }

    private void createTeam(String name, ChatColor color) {
        Team team = scoreboard.registerNewTeam(name);
        team.setColor(color);
        team.setAllowFriendlyFire(false);
        team.setCanSeeFriendlyInvisibles(true);
        team.setPrefix(color + "[" + name + "] ");
        teams.add(team);
    }

    public void assignPlayersToTeams(Set<UUID> readyPlayers) {
        List<UUID> shuffledPlayers = new ArrayList<>(readyPlayers);
        Collections.shuffle(shuffledPlayers);

        int teamSize = (int) Math.ceil((double) shuffledPlayers.size() / teams.size());

        int playerIndex = 0;
        for (Team team : teams) {
            for (int i = 0; i < teamSize && playerIndex < shuffledPlayers.size(); i++) {
                UUID playerId = shuffledPlayers.get(playerIndex);
                Player player = Bukkit.getPlayer(playerId);
                team.addEntry(player.getName());
                player.setScoreboard(scoreboard);
                player.sendMessage(ChatColor.GREEN + "你被分配到了 " + team.getColor() + team.getName() + ChatColor.GREEN + " 队!");
                playerIndex++;
            }
        }
    }

    // 可以添加一个方法来获取玩家的队伍
    public Team getPlayerTeam(Player player) {
        return scoreboard.getEntryTeam(player.getName());
    }
}
