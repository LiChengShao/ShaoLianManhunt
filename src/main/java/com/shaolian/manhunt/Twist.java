package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

public class Twist implements Listener {
    private final Main plugin;

    public Twist(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5 * 60 * 1000; // 5分钟的冷却时间（毫秒）


    //创造
    public ItemStack createTwistingVines() {
        ItemStack twistingVines = new ItemStack(Material.TWISTING_VINES);
        ItemMeta meta = twistingVines.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d天气预报");
            meta.setLore(Arrays.asList("§7左击可以改变天气", "§dCD:5min"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            twistingVines.setItemMeta(meta);
        }
        return twistingVines;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.TWISTING_VINES) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击可以改变天气")) {
                event.setCancelled(true); // 取消原版的行为
                if (event.getAction() == Action.LEFT_CLICK_AIR ||
                        event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        changeWeather(player);
                    } else {
                        player.sendMessage("§c冷却中！");
                    }
                }

            }
        }
    }

    private boolean checkCooldown(Player player) {
        long currentTime = System.currentTimeMillis();
        UUID playerUUID = player.getUniqueId();

        if (cooldowns.containsKey(playerUUID)) {
            long lastUsage = cooldowns.get(playerUUID);
            if (currentTime - lastUsage < COOLDOWN_TIME) {
                long remainingTime = (COOLDOWN_TIME - (currentTime - lastUsage)) / 1000;
                player.sendMessage("§c你还需要等待 " + remainingTime + " 秒才能再次使用此物品。");
                return false;
            }
        }

        cooldowns.put(playerUUID, currentTime);
        return true;
    }

    public void changeWeather(Player player) {
        // 产生粒子特效
        player.getWorld().spawnParticle(Particle.SNOWFLAKE, player.getLocation(), 100, 3, 3, 3, 0.1);

        // 改变玩家附近的天气为雪天
        player.getWorld().setStorm(true);
        player.getWorld().setThundering(true);

        // 获取玩家所在的世界和位置
        World world = player.getWorld();
        Location playerLoc = player.getLocation();

        // 定义冰冻范围
        int radius = 20;
        int height = 4;

        new BukkitRunnable() {
            @Override
            public void run() {
                // 遍历指定范围内的方块
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (int y = -2; y <= 1; y++) {
                            Location loc = playerLoc.clone().add(x, y, z);
                            Block block = loc.getBlock();

                            // 检查是否在圆形范围内
                            if (loc.distance(playerLoc) <= radius) {
                                // 如果是液体方块，将其变成冰
                                if (block.isLiquid()) {
                                    block.setType(Material.ICE);
                                }
                            }
                        }
                    }
                }

                //添加方法，让距离该玩家15格以内的玩家获得缓慢和冻伤的debuff
                // 获取范围内的所有实体
                // 定义效果的范围
                double range = 15.0;
                for (Entity entity : world.getNearbyEntities(playerLoc, range, range, range)) {
                    // 检查实体是否为玩家，且不是源玩家自己
                    if (entity instanceof Player && !entity.equals(player)) {
                        Player targetPlayer = (Player) entity;

                        // 应用缓慢效果
                        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 15 * 20, 3));
                        targetPlayer.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 15 * 20, 1));

                        // 可选：发送消息给受影响的玩家
                        targetPlayer.sendMessage("§c你被附近的寒冰影响了！");
                    }
                }

                // 发送消息给玩家
                for (Player player2 : Bukkit.getOnlinePlayers()) {
                    player2.sendMessage("§b" + player.getName() + "§a改变了周围的天气，并冻结了附近的液体！");
                }
                recover(player);
            }
        }.runTaskLater(plugin, 10L);
    }

    public void recover(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                player.getWorld().setStorm(false);
                player.getWorld().setThundering(false);

            }
        }.runTaskLater(plugin, 60 * 20L);
    }
}




