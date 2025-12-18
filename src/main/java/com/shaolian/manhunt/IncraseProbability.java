package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class IncraseProbability implements Listener {

    private final Main plugin;
    private final HashMap<UUID, Integer> playerBoosts = new HashMap<>();

    public IncraseProbability(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createSpecialTotem() {
        ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = totem.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6选择优先阵营");
            List<String> lore = new ArrayList<>();
            lore.add("§7右键点击打开阵营选择界面");
            meta.setLore(lore);
            totem.setItemMeta(meta);
        }
        return totem;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.getType() == Material.TOTEM_OF_UNDYING && item.hasItemMeta()
                    && item.getItemMeta().getDisplayName().equals("§6选择优先阵营")) {
                openBoostGUI(event.getPlayer());
                event.setCancelled(true);
            }
        }
    }

    private void openBoostGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "§a提升逃生者概率");

        ItemStack crystal = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = crystal.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§d提升逃生者概率");
            List<String> lore = new ArrayList<>();
            lore.add("§7点击提升10%被分配为逃生者的概率");
            lore.add("§7当前提升: " + getPlayerBoost(player) + "%");
            meta.setLore(lore);
            crystal.setItemMeta(meta);
        }

        gui.setItem(13, crystal);
        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§a提升逃生者概率")) {
            event.setCancelled(true);
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.END_CRYSTAL) {
                Player player = (Player) event.getWhoClicked();
                increasePlayerBoost(player);
                player.closeInventory();
                player.sendMessage("§a你的逃生者概率提升了10%！");
            }
        }
    }

    private void increasePlayerBoost(Player player) {
        UUID playerId = player.getUniqueId();
        int currentBoost = playerBoosts.getOrDefault(playerId, 0);
        playerBoosts.put(playerId, currentBoost + 10);
    }

    private int getPlayerBoost(Player player) {
        return playerBoosts.getOrDefault(player.getUniqueId(), 0);
    }

    public void assignRoles(List<Player> players) {
        int totalPlayers = players.size();
        int targetRunnerCount = Math.max(1, (int) Math.round(totalPlayers / 3.0));
        List<Player> runners = new ArrayList<>();
        List<Player> hunters = new ArrayList<>();

        for (Player player : players) {
            int boost = getPlayerBoost(player);
            double chance = (double) boost / 100;
            if (Math.random() < chance && runners.size() < targetRunnerCount) {
                runners.add(player);
            }
        }

        for (Player player : players) {
            if (!runners.contains(player)) {
                if (runners.size() < targetRunnerCount) {
                    runners.add(player);
                } else {
                    hunters.add(player);
                }
            }
        }

        // 分配角色
        for (Player runner : runners) {
            // 设置逃生者角色
            runner.sendMessage("§a你被分配为逃生者！");
        }
        for (Player hunter : hunters) {
            // 设置猎人角色
            hunter.sendMessage("§c你被分配为猎人！");
        }
    }
}
