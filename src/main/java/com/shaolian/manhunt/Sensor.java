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
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;


import java.util.*;

public class Sensor implements Listener {
    private final Main plugin;

    public Sensor(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5 * 10 * 1000; // 5分钟的冷却时间（毫秒）


    //创造特殊的
    public ItemStack createSensor() {
        ItemStack sensor = new ItemStack(Material.CALIBRATED_SCULK_SENSOR);
        ItemMeta meta = sensor.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d变身循声守卫");
            meta.setLore(Arrays.asList("§7左击可以变成循声守卫者", "§dCD:5min","§7变身时间10s"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            sensor.setItemMeta(meta);
        }
        return sensor;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.CALIBRATED_SCULK_SENSOR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击可以变成循声守卫者")) {
                event.setCancelled(true); // 取消原版的行为
                if(event.getAction() == Action.LEFT_CLICK_AIR ||
                        event.getAction() == Action.LEFT_CLICK_BLOCK){
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        guardian(player);
                    }
                    else {
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

    public void guardian(Player player) {
        // 检查玩家是否已经在变身状态
        if (DisguiseAPI.isDisguised(player)) {
            return;
        }

        // 创建远古守卫者的伪装
        MobDisguise disguise = new MobDisguise(DisguiseType.WARDEN);
        // 设置伪装选项
        disguise.setViewSelfDisguise(true); // 允许玩家看到自己的伪装
        DisguiseAPI.setViewDisguiseToggled(player, true);

        disguise.setModifyBoundingBox(true); // 修改碰撞箱以匹配

        // 应用伪装到玩家
        DisguiseAPI.disguiseEntity(player, disguise);

        // 给玩家发送消息
        for(Player player2 : Bukkit.getOnlinePlayers()){
            player2.sendMessage(player.getName() + "§a已变身为循声守卫，可以让玩家失明");
        }


        // 播放监守者出现的声音
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_EMERGE, 1.0f, 1.0f);


        // 创建一个定时任务，每秒检查一次是否可以发射声波
        BukkitRunnable attackTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (DisguiseAPI.isDisguised(player)) {
                    fireGuardianBeam(player);
                } else {
                    this.cancel(); // 如果玩家不再伪装，取消任务
                }
            }
        };
        attackTask.runTaskTimer(plugin, 20, 20); // 从1秒后开始，每秒执行一次

        // 10秒后取消伪装
        new BukkitRunnable() {
            @Override
            public void run() {
                if (DisguiseAPI.isDisguised(player)) {
                    DisguiseAPI.undisguiseToAll(player);
                    player.sendMessage("§c你的远古守卫者变身已结束！");
                    player.playSound(player.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_DEATH,
                            1.0f, 1.0f);
                    attackTask.cancel(); // 确保攻击任务被取消
                }
            }
        }.runTaskLater(plugin, 20 * 10); // 20 ticks * 10 = 10 seconds
    }

    private void fireGuardianBeam(Player player) {
        // 播放监守者的音爆声音
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 3.0f, 1.0f);

        // 获取玩家视线方向
        Vector direction = player.getLocation().getDirection();
        Location startLoc = player.getEyeLocation();

        // 创建音爆效果
        new BukkitRunnable() {
            double t = 0;
            public void run() {
                t += 0.5;
                Location loc = startLoc.clone().add(direction.clone().multiply(t));

                if (t > 20 || loc.getBlock().getType().isSolid()) {
                    this.cancel();
                    return;
                }

                // 音爆粒子效果
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1, 0, 0, 0, 0);

                // 检查是否有玩家在音爆路径上
                for (Entity entity : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                    if (entity instanceof Player && entity != player) {
                        Player target = (Player) entity;
                        // 造成伤害和击退
                        target.damage(10.0, player);
                        Vector knockback = target.getLocation().toVector().subtract(startLoc.toVector()).normalize().multiply(2);
                        target.setVelocity(knockback);
                        target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 100, 0));
                        target.sendMessage("§c你被监守者的音爆击中了！");
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

    }














}














