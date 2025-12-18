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
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class Skull implements Listener {
    private final Main plugin;

    public Skull(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME =  40 * 1000; // 2分钟的冷却时间（毫秒）
    private BukkitTask task = null;


    //创造
    public ItemStack createSkull() {
        ItemStack skull = new ItemStack(Material.SKULL_POTTERY_SHERD);
        ItemMeta meta = skull.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d蜘蛛侠");
            meta.setLore(Arrays.asList("§7左击对着地面发射蜘蛛网陷阱", "§dCD:40s"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.SKULL_POTTERY_SHERD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击对着地面发射蜘蛛网陷阱")) {
                event.setCancelled(true); // 取消原版的行为
                if (event.getAction() == Action.LEFT_CLICK_AIR ||
                        event.getAction() == Action.LEFT_CLICK_BLOCK) {
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        launchCobweb(player);
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

    public void launchCobweb(Player player) {
        World world = player.getWorld();
        Location playerLoc = player.getEyeLocation();
        Vector direction = playerLoc.getDirection();

        // 播放发射音效
        world.playSound(playerLoc, Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.0f);

        // 创建一个新的BukkitRunnable来处理粒子效果和蜘蛛网生成
        task = new BukkitRunnable() {
            double t = 0;
            Location loc = playerLoc.clone();

            @Override
            public void run() {
                t += 0.5;
                Vector vec = direction.clone().multiply(t);
                loc = playerLoc.clone().add(vec);

                // 生成粒子效果
                world.spawnParticle(Particle.DUST, loc, 5, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(Color.WHITE, 1));

                // 检查是否击中方块
                if (loc.getBlock().getType().isSolid() || t > 50) {
                    this.cancel();
                    task = null;
                    generateCobweb(loc);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void generateCobweb(Location center) {
        World world = center.getWorld();
        if (world == null) return;

        // 播放蜘蛛网生成音效
        world.playSound(center, Sound.BLOCK_WOOL_PLACE, 1.0f, 1.0f);

        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    if (Math.sqrt(x*x + y*y + z*z) <= 4) {
                        Location loc = center.clone().add(x, y, z);
                        Block block = loc.getBlock();
                        Block below = block.getRelative(BlockFace.DOWN);

                        // 如果当前方块是空气且下方有实体方块，则生成蜘蛛网
                        if (block.getType() == Material.AIR && below.getType().isSolid()) {
                            block.setType(Material.COBWEB);
                        }
                    }
                }
            }
        }
        if (task != null) {
            task.cancel();
            task = null;
        }
    }




}




