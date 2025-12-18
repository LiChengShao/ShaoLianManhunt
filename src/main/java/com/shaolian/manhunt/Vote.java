package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Vote implements CommandExecutor, Listener {
    private final Main plugin;
    private final Map<UUID, UUID> votes = new HashMap<>(); // 记录投票信息 <投票者, 被投票者>
    public static  UUID outPlayerId = null;
    private final Map<UUID, Boolean> hasVoted = new HashMap<>(); // 记录玩家是否已投票

    public Vote(Main plugin) {
        this.plugin = plugin;
        plugin.getCommand("vote").setExecutor(this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令！");
            return true;
        }

        if (!GameManager.isGameRunning) {
            player.sendMessage(ChatColor.RED + "游戏尚未开始！");
            return true;
        }

        if (GameManager.ghast.equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "你的身份是内鬼并且只有你的身份内鬼！你的任务是帮助逃亡者获得游戏胜利！");
        }

        if (GameManager.runners.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "内鬼是" + RecoveryCompass.ghastName);
        }

        openVoteGUI(player);
        return true;
    }

    private void openVoteGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.RED + "投票界面");
        for (UUID hunterId : GameManager.hunters) {
            Player hunter = Bukkit.getPlayer(hunterId);
            if (hunter != null) {
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(hunter);
                meta.setDisplayName(ChatColor.RED + hunter.getName());

                // 检查是否已投票
                String votedStatus = votes.containsKey(hunterId) ? ChatColor.GREEN +
                        "已投票" : ChatColor.RED + "未投票";
                meta.setLore(Collections.singletonList(votedStatus));

                skull.setItemMeta(meta);
                gui.addItem(skull);
            }
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(ChatColor.RED + "投票界面")){
            Player voter = (Player) event.getWhoClicked();
            if (!GameManager.hunters.contains(voter.getUniqueId())) {
                voter.sendMessage(ChatColor.RED + "只有猎人可以投票！");
                voter.playSound(voter.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f); // 播放村民拒绝的音效
                event.setCancelled(true);
                return;
            }

            if (hasVoted.containsKey(voter.getUniqueId())) {
                voter.sendMessage(ChatColor.RED + "你已经投过票了！");
                event.setCancelled(true);
                return;
            }

            ItemStack clickedItem = event.getCurrentItem();
//            if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD)
//                return;

            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            Player votedPlayer = Bukkit.getPlayer(meta.getOwningPlayer().getUniqueId());

            if (votedPlayer != null) {
                votes.put(voter.getUniqueId(), votedPlayer.getUniqueId());
                hasVoted.put(voter.getUniqueId(), true);
                voter.sendMessage(ChatColor.GREEN + "你已投票给 " + ChatColor.RED + votedPlayer.getName());
                openVoteGUI(voter); // 刷新GUI

                // 检查是否满足淘汰条件
                checkVoteResult(votedPlayer);
            }

            event.setCancelled(true);
        }



    }

    public Map<UUID, UUID> getVotes() {
        return votes;
    }

    // 新增方法：检查投票结果并执行淘汰
    private void checkVoteResult(Player votedPlayer) {
        // 统计被投票数
        int voteCount = Collections.frequency(votes.values(), votedPlayer.getUniqueId());
        int totalHunters = GameManager.hunters.size();

        // 如果所有猎人都已投票或者某个玩家获得超过一半的票数
        if (votes.size() == totalHunters || voteCount > totalHunters / 2) {
            // 公告淘汰信息
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.sendTitle(
                        ChatColor.RED + "投票结果",
                        ChatColor.GOLD + votedPlayer.getName() + " 被投 " + voteCount + " 票后被淘汰",
                        10, 70, 20
                );
            });

            // 执行淘汰
            GameManager.hunters.remove(votedPlayer.getUniqueId()); // 移除猎人身份
            outPlayerId = votedPlayer.getUniqueId();
            votedPlayer.setHealth(0); // 杀死玩家
            votedPlayer.spigot().respawn(); // 立即重生
            votedPlayer.setGameMode(GameMode.SPECTATOR);


            // 从猎人队伍中移除玩家
            Team hunterTeam = Bukkit.getScoreboardManager().getMainScoreboard().getTeam("Hunters");
            if (hunterTeam != null && hunterTeam.hasEntry(votedPlayer.getName())) {
                hunterTeam.removeEntry(votedPlayer.getName());
            }
            // 将玩家加入内鬼队伍
            GameManager.ghastTeam.addEntry(votedPlayer.getName());

//            // 更新玩家前缀
//            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(votedPlayer.getName());
//            if (team != null) {
//                team.setPrefix(ChatColor.DARK_PURPLE + "[群众选出的内鬼] ");
//            } else {
//                // 如果没有队伍，创建一个新队伍并设置前缀
//                Team newTeam = Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam("Ghast");
//                newTeam.setPrefix(ChatColor.DARK_PURPLE + "[群众选出的内鬼] ");
//                newTeam.addEntry(votedPlayer.getName());
//            }
//
//            // 更新玩家显示名称
//            votedPlayer.setDisplayName(ChatColor.DARK_PURPLE + "[群众选出的内鬼] " + votedPlayer.getName());
//            votedPlayer.setPlayerListName(ChatColor.DARK_PURPLE + "[群众选出的内鬼] " + votedPlayer.getName());

        }
    }
}
