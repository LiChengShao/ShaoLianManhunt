package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.block.data.MultipleFacing;

import java.util.*;

public class SpecialItem implements Listener {
    private final Main plugin;

    public SpecialItem(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5 * 60 * 1000; // 5分钟的冷却时间（毫秒）


    //创造特殊的灯笼
    public ItemStack createSoulLantern() {
        ItemStack specialSoulLantern = new ItemStack(Material.SOUL_LANTERN);
        ItemMeta meta = specialSoulLantern.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d幽匿room");
            meta.setLore(Arrays.asList("§7左击可以创造幽匿room", "§dCD:5min"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            specialSoulLantern.setItemMeta(meta);
        }
        return specialSoulLantern;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.SOUL_LANTERN) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击可以创造幽匿room")) {
                event.setCancelled(true); // 取消原版的行为
                if(event.getAction() == Action.LEFT_CLICK_AIR ||
                        event.getAction() == Action.LEFT_CLICK_BLOCK){
                    event.setCancelled(true); // 取消原版的行为
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        createSoulLanternRoom(player);
                      //  createSculkVeins(player);
                        //createSculkVein(player);
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

//    public void createSoulLanternRoom(Player player) {
//        Location center = player.getLocation();
//        World world = center.getWorld();
//        int radius = 15;
//        //主要的变化是将 Set<Block> placedBlocks = new HashSet<>();
//        // 改为 final Set<Block> placedBlocks = new HashSet<>();。
//        //这样做可以确保 placedBlocks 可以在匿名内部类（BukkitRunnable）中被访问。
//        final Set<Block> placedBlocks = new HashSet<>(); // 使用 final 声明
//
//        for (int x = -radius; x <= radius; x++) {
//            for (int y = -radius; y <= radius; y++) {
//                for (int z = -radius; z <= radius; z++) {
//                    double distance = Math.sqrt(x*x + y*y + z*z);
//
//                    if (distance > radius - 0.5 && distance < radius + 0.5) {
//                        Location blockLoc = center.clone().add(x, y, z);
//                        Block block = world.getBlockAt(blockLoc);
//
//                        if (block.getType() != Material.END_PORTAL) {
//                            block.setType(Material.CRYING_OBSIDIAN);
//                            placedBlocks.add(block);
//                        }
//                    }
//                }
//            }
//        }
//        // 给玩家添加力量I和抗火效果，持续30秒
//        player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 20 * 30, 1));
//        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 30, 0));
//
//        Bukkit.broadcastMessage(player.getName() + "§d的幽匿room已创建！");
//
//        // 30秒后移除方块
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                for (Block block : placedBlocks) {
//                    if (block.getType() == Material.CRYING_OBSIDIAN) {
//                        block.setType(Material.AIR);
//                    }
//                }
//                // 移除玩家的力量和抗火效果
//                player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
//                player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
//                Bukkit.broadcastMessage(player.getName() + "§d幽匿room已消失，增益效果已结束。");
//            }
//        }.runTaskLater(plugin, 20 * 30); // 20 ticks * 30 seconds
//    }


    public void createSoulLanternRoom(Player player) {
        Location center = player.getLocation();
        World world = center.getWorld();
        int radius = 15;
        final Set<Block> placedBlocks = new HashSet<>();

        // 分阶段生成球体
        new BukkitRunnable() {
            double currentY = -radius;
            double step = 0.5; // 每次增加的高度

            @Override
            public void run() {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        for (double y = currentY; y < currentY + step; y += 0.5) {
                            double distance = Math.sqrt(x*x + y*y + z*z);
                            if (distance > radius - 0.5 && distance < radius + 0.5) {
                                Location blockLoc = center.clone().add(x, y, z);
                                Block block = world.getBlockAt(blockLoc);
                                if (block.getType() != Material.END_PORTAL
                                        && block.getType() != Material.END_GATEWAY
                                        && block.getType() != Material.END_PORTAL_FRAME) {
                                    block.setType(Material.CRYING_OBSIDIAN);
                                    placedBlocks.add(block);
                                }
                            }
                        }
                    }
                }
                currentY += step;
                if (currentY > radius) {
                    this.cancel();
                    // 球体生成完毕，给予玩家效果
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 9, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 9, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 9, 1));
                    for(Player player1 : Bukkit.getOnlinePlayers()){
                        player1.sendMessage(player.getName() + "§d的幽匿房间已创建！");
                    }

                    //playCreepySound(player);
                    // 设置15秒后开始消失
                    startDisappearingProcess(player, placedBlocks);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // 每tick执行一次，约3秒完成
    }

    private void startDisappearingProcess(Player player, Set<Block> placedBlocks) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Location center = player.getLocation();
                double radius = 15.0;
                double step = 0.1; // 每次减少的半径

                new BukkitRunnable() {
                    double currentRadius = radius;

                    @Override
                    public void run() {
                        // 创建一个迭代器来遍历所有放置的方块
                        Iterator<Block> iterator = placedBlocks.iterator();

                        // 开始遍历所有放置的方块
                        while (iterator.hasNext()) {
                            Block block = iterator.next();
                            Location blockLoc = block.getLocation();
                            double distance = blockLoc.distance(center);

                            // 检查方块是否在当前清除半径之外
                            if (distance >= currentRadius) {
                                if (block.getType() == Material.CRYING_OBSIDIAN) {
                                    block.setType(Material.AIR);
                                    iterator.remove();
                                }
                            }
                        }

                        currentRadius -= step;

                        // 播放消失音效
                        if (Math.random() < 0.1) { // 10% 的概率在每次执行时播放音效
                            playDisappearingSound(player);
                        }

                        if (currentRadius < 0 || placedBlocks.isEmpty()) {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 1L); // 每tick执行一次，实现平滑消失
            }
        }.runTaskLater(plugin, 15 * 20); // 15秒后开始消失过程
    }

    private void createSculkVeins(Player player) {
        new BukkitRunnable() {
            Location center = player.getLocation();
            int currentRadius = 0;
            int radius = 15;
            final Set<Block> placedBlocks = new HashSet<>();

            @Override
            public void run() {
                for (int x = -currentRadius; x <= currentRadius; x++) {
                    for (int y = -currentRadius; y <= currentRadius; y++) {
                        for (int z = -currentRadius; z <= currentRadius; z++) {
                            if (x*x + y*y + z*z <= currentRadius*currentRadius) {
                                Location blockLoc = center.clone().add(x, y, z);
                                Block block = blockLoc.getBlock();
                                Block blockAbove = block.getRelative(BlockFace.UP);

                                if (block.getType().isSolid() && blockAbove.getType().isAir()) {
                                    if (Math.random() < 0.8) { // 80% 的概率生成幽匿脉络
                                        blockAbove.setType(Material.SCULK_VEIN);
                                        BlockData blockData = blockAbove.getBlockData();
                                        if (blockData instanceof MultipleFacing) {
                                            MultipleFacing sculkVein = (MultipleFacing) blockData;
                                            sculkVein.setFace(BlockFace.DOWN, true);
                                            blockAbove.setBlockData(sculkVein);
                                        }
                                        placedBlocks.add(blockAbove);
                                    }
                                }
                            }
                        }
                    }
                }

                currentRadius++;
                if (currentRadius > radius) {
                    this.cancel();
                    player.sendMessage("§d幽匿脉络蔓延完成！");
                }
            }
        }.runTaskTimer(plugin, 0L, 2L); // 每0.1秒执行一次，总共执行30次，约3秒完成蔓延
    }




    private void playDisappearingSound(Player player) {
        Sound[] disappearingSounds = {
                Sound.BLOCK_ANCIENT_DEBRIS_BREAK,
                Sound.BLOCK_BASALT_BREAK,
                Sound.BLOCK_STONE_BREAK,
                Sound.BLOCK_NETHERRACK_BREAK,
                Sound.ENTITY_ENDERMAN_TELEPORT
        };
        Sound selectedSound = disappearingSounds[new Random().nextInt(disappearingSounds.length)];
        player.playSound(player.getLocation(), selectedSound, 0.5f, 0.5f);

        // 为附近的玩家也播放音效
        for (Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (nearbyPlayer != player && nearbyPlayer.getLocation().distance(player.getLocation()) <= 30) {
                nearbyPlayer.playSound(player.getLocation(), selectedSound, 0.25f, 0.5f);
            }
        }
    }

}













