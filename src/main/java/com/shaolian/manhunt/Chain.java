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

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.libraryaddict.disguise.disguisetypes.*;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.PlayerDisguise;


import java.util.*;

public class Chain implements Listener {
    private final Main plugin;

    public Chain(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5 * 60 * 1000; // 1分钟的冷却时间（毫秒）
    private final Map<UUID, BukkitRunnable> batTasks = new HashMap<>();
    private final int BAT_DURATION = 10 * 20; // 10 秒

    //创造特殊的蝙蝠锁链
    public ItemStack createChain() {
        ItemStack chain = new ItemStack(Material.CHAIN);
        ItemMeta meta = chain.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d变身蝙蝠");
            meta.setLore(Arrays.asList("§7左击可以让自己变成蝙蝠", "§dCD:5min","§7变身时间10s"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            chain.setItemMeta(meta);
        }
        return chain;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.CHAIN) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击可以让自己变成蝙蝠")) {
                event.setCancelled(true); // 取消原版的行为
                if(event.getAction() == Action.LEFT_CLICK_AIR ||
                        event.getAction() == Action.LEFT_CLICK_BLOCK){
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        shrinkPlayer(player);
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


    public void shrinkPlayer(Player player) {
        // 创建蝙蝠伪装
        MobDisguise batDisguise = new MobDisguise(DisguiseType.BAT);

        // 设置伪装选项
        batDisguise.setViewSelfDisguise(true); // 允许玩家看到自己的伪装
        batDisguise.setModifyBoundingBox(true); // 修改碰撞箱以匹配蝙蝠大小

        // 应用伪装
        DisguiseAPI.disguiseEntity(player, batDisguise);

        // 添加飞行能力
        player.setAllowFlight(true);
        player.setFlying(true);

        // 可选：添加夜视效果，因为蝙蝠在黑暗中视力较好
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 20 * 10, 0)); // 10s的夜视效果

        // 播放声音和粒子效果
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);

        // 通知玩家
        for(Player player2 : Bukkit.getOnlinePlayers()){
            player2.sendMessage(player.getName() + "§a变成了蝙蝠！可以飞行！！！");
        }
        player.sendMessage("§a如果你有的时候无法放置或者摧毁方块，那就是被蝙蝠的碰撞箱挡住了，试着跳一下");


        // 设置定时器
        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                restorePlayer(player);
            }
        };
        task.runTaskLater(plugin, BAT_DURATION);
        batTasks.put(player.getUniqueId(), task);
    }

    public void restorePlayer(Player player) {
        // 移除伪装
        if (DisguiseAPI.isDisguised(player)) {
            DisguiseAPI.undisguiseToAll(player);
        }

        // 如果需要，可以重新应用玩家的原始外观
        //PlayerDisguise playerDisguise = new PlayerDisguise(player.getName());
        //DisguiseAPI.disguiseToAll(player, playerDisguise);

        //playerDisguise.setModifyBoundingBox(true); // 添加这行来确保碰撞箱被正确设置


        // 移除飞行能力
        player.setAllowFlight(false);
        player.setFlying(false);

        // 移除夜视效果
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);

        // 播放声音和粒子效果
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 0.5f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 50, 0.5, 0.5, 0.5, 0.1);

        // 移除定时器
        BukkitRunnable task = batTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }

        player.sendMessage("§a你已恢复原状！");
    }








}














