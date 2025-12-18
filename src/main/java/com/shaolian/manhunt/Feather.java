package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Feather implements Listener {
    private Main plugin;
    public static Map<UUID, ItemStack[]> playerFeatherChoices = new HashMap<>();

    public Feather(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }

    public static ItemStack createFeatherChoose() {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c选择羽毛kit");
            meta.setLore(Arrays.asList("§7右击选择你的羽毛kit"));
            feather.setItemMeta(meta);
        }
        return feather;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.equals(createFeatherChoose())) {
            openFeatherGUI(player);
            event.setCancelled(true);
        }
    }

    private void openFeatherGUI(Player player) {
        UUID playerId = player.getUniqueId();
        if (!playerFeatherChoices.containsKey(playerId)) {
            // 默认分配前三个羽毛
            ItemStack[] defaultFeathers = new ItemStack[3];
            defaultFeathers[0] = createFeather("§1海豚的眷顾", "§8ManhuntFeatherSwim", true);
            defaultFeathers[1] = createFeather("§c抗火", "§8ManhuntFeatherFire", true);
            defaultFeathers[2] = createFeather("§e跳跃提升3", "§8ManhuntFeatherJump", true);
            playerFeatherChoices.put(playerId, defaultFeathers);
        }
        Inventory gui = Bukkit.createInventory(null, 36, "§a选择你的羽毛kit");
        ItemStack[] chosenFeathers = playerFeatherChoices.get(playerId);
        ItemStack dolphinGrace = createFeather("§1海豚的眷顾", "§8ManhuntFeatherSwim", isFeatherChosen(chosenFeathers, "§1海豚的眷顾"));
        ItemStack fireResistance = createFeather("§c抗火", "§8ManhuntFeatherFire", isFeatherChosen(chosenFeathers, "§c抗火"));
        ItemStack jumpBoost = createFeather("§e跳跃提升3", "§8ManhuntFeatherJump", isFeatherChosen(chosenFeathers, "§e跳跃提升3"));
        ItemStack speed = createFeather("§2速度2", "§8ManhuntFeatherSpeed", isFeatherChosen(chosenFeathers, "§2速度2"));
        ItemStack regeneration = createFeather("§d生命恢复1", "§8ManhuntFeatherRegen", isFeatherChosen(chosenFeathers, "§d生命恢复1"));
        ItemStack invisibility = createFeather("§f隐身", "§8ManhuntFeatherInvis", isFeatherChosen(chosenFeathers, "§f隐身"));
        gui.setItem(11, dolphinGrace);
        gui.setItem(13, fireResistance);
        gui.setItem(15, jumpBoost);
        gui.setItem(20, speed);
        gui.setItem(22, regeneration);
        gui.setItem(24, invisibility);
        player.openInventory(gui);
    }

    private boolean isFeatherChosen(ItemStack[] chosenFeathers, String featherName) {
        for (ItemStack feather : chosenFeathers) {
            if (feather != null && feather.getItemMeta() != null && feather.getItemMeta().getDisplayName().equals(featherName)) {
                return true;
            }
        }
        return false;
    }

    public static ItemStack createFeather(String name, String lore, boolean isChosen) {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta meta = feather.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList(lore, isChosen ? "§a已选择" : "§c未选择"));
            feather.setItemMeta(meta);
        }
        return feather;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§a选择你的羽毛kit")) {
            event.setCancelled(true);
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem != null && clickedItem.getType() == Material.FEATHER) {
                Player player = (Player) event.getWhoClicked();
                UUID playerId = player.getUniqueId();
                ItemStack[] chosenFeathers = playerFeatherChoices.getOrDefault(playerId, new ItemStack[3]);
                
                String clickedFeatherName = clickedItem.getItemMeta().getDisplayName();
                boolean isCurrentlyChosen = isFeatherChosen(chosenFeathers, clickedFeatherName);
                
                if (isCurrentlyChosen) {
                    // 如果已经选择了这个羽毛，取消选择
                    for (int i = 0; i < chosenFeathers.length; i++) {
                        if (chosenFeathers[i] != null && 
                            chosenFeathers[i].getItemMeta() != null && 
                            chosenFeathers[i].getItemMeta().getDisplayName().equals(clickedFeatherName)) {
                            chosenFeathers[i] = null;
                            player.sendMessage("§c已取消选择: " + clickedFeatherName);
                            break;
                        }
                    }
                } else {
                    // 如果还没有选择这个羽毛，检查是否可以添加
                    int chosenCount = 0;
                    for (ItemStack feather : chosenFeathers) {
                        if (feather != null) chosenCount++;
                    }
                    
                    if (chosenCount < 3) {
                        // 找到第一个空位
                        for (int i = 0; i < chosenFeathers.length; i++) {
                            if (chosenFeathers[i] == null) {
                                chosenFeathers[i] = clickedItem;
                                player.sendMessage("§a已选择: " + clickedFeatherName);
                                break;
                            }
                        }
                    } else {
                        player.sendMessage("§c你最多只能选择3个羽毛kit！");
                        return;
                    }
                }
                
                playerFeatherChoices.put(playerId, chosenFeathers);
                // 刷新GUI显示
                openFeatherGUI(player);
            }
        }
    }

    public static Map<UUID, ItemStack[]> getPlayerFeatherChoices() {
        return playerFeatherChoices;
    }
}

