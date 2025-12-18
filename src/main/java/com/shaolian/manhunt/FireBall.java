package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.block.Block;
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
import org.bukkit.util.Vector;

import java.util.*;

public class FireBall implements Listener {
    private final Main plugin;

    public FireBall(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 60 * 1000; // 1分钟的冷却时间（毫秒）


    //创造特殊的旗帜
    public ItemStack createFireBall() {
        ItemStack fireBall = new ItemStack(Material.PURPLE_BANNER);
        ItemMeta meta = fireBall.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d飞毯");
            meta.setLore(Arrays.asList("§7左击可以飞行", "§dCD:60s"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            fireBall.setItemMeta(meta);
        }
        return fireBall;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.PURPLE_BANNER) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击可以飞行")) {
                event.setCancelled(true); // 取消原版的行为
                if(event.getAction() == Action.LEFT_CLICK_BLOCK
                ||event.getAction() == Action.LEFT_CLICK_AIR){
                    //event.setCancelled(true); // 取消原版的行为
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        fireBallJump(player);
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

    public void fireBallJump(Player player) {
        // 获取玩家当前的速度
        Vector currentVelocity = player.getVelocity();

        // 获取玩家的朝向
        Vector direction = player.getLocation().getDirection();

        // 计算弹飞的力度
        double baseForce = 8; // 基础弹飞力度
        double speedMultiplier = 4.0; // 速度影响因子

        // 计算当前速度的大小（不考虑Y轴）
        double currentSpeed = Math.sqrt(currentVelocity.getX() * currentVelocity.getX() + currentVelocity.getZ() * currentVelocity.getZ());

        // 根据当前速度调整弹飞力度
        double adjustedForce = baseForce + (currentSpeed * speedMultiplier);

        // 计算新的速度向量
        Vector newVelocity = direction.multiply(adjustedForce);

        // 添加向上的力，使玩家能够跳起来
        newVelocity.setY(1.6);

        // 应用新的速度到玩家
        player.setVelocity(newVelocity);

        // 播放音效和粒子效果
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);


    }












}
