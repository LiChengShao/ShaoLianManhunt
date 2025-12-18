package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MagicBlock implements Listener {
    private final Main plugin;

    public MagicBlock(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5 * 1000; //


    //创造特殊的方块
    public ItemStack createMagicBlock() {
        ItemStack specialCherryLeaves = new ItemStack(Material.PINK_WOOL);
        ItemMeta meta = specialCherryLeaves.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d魔法方块");
            meta.setLore(Arrays.asList("§7右击可以生成多个方块", "§dCD:5s","用这个找鞘翅或许不错"));
            meta.setUnbreakable(true);

            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);

            specialCherryLeaves.setItemMeta(meta);
        }
        return specialCherryLeaves;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.PINK_WOOL) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7右击可以生成多个方块")) {
                if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    event.setCancelled(true); // 取消的放置行为
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        // 创建一个虚拟的 BlockPlaceEvent
                        Block clickedBlock = event.getClickedBlock();
                        Block placedBlock = clickedBlock.getRelative(event.getBlockFace());
                        BlockPlaceEvent blockPlaceEvent = new BlockPlaceEvent(placedBlock, placedBlock.getState(), clickedBlock, item, player, true, event.getHand());

                        generateBridge(player, blockPlaceEvent);
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


    public void generateBridge(Player player, BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        BlockFace face = event.getBlockAgainst().getFace(placedBlock);

        if (face == BlockFace.UP) {
            // 如果是放在地面上，向上生长
            //growthDirection = BlockFace.UP;

            // 如果是放在地面上，生成防御塔
            generateDefenseTower(player, placedBlock);
        }
        else {
            // 如果是放在墙上或天花板上，生成桥
            BlockFace growthDirection = (face == BlockFace.DOWN) ? BlockFace.DOWN : face;
            generateBridgeStructure(player, placedBlock, growthDirection);
        }

        // 播放音效和粒子效果
        player.getWorld().playSound(placedBlock.getLocation(), org.bukkit.Sound.BLOCK_CHERRY_LEAVES_PLACE, 1.0f, 1.0f);
        player.getWorld().spawnParticle(org.bukkit.Particle.CHERRY_LEAVES, placedBlock.getLocation(), 50, 2, 2, 2, 0.1);
    }

    private Material getRandomWools() {
        Material[] woolTypes = {
                Material.WHITE_WOOL,
                Material.ORANGE_WOOL,
                Material.MAGENTA_WOOL,
                Material.LIGHT_BLUE_WOOL,
                Material.YELLOW_WOOL,
                Material.LIME_WOOL,
                Material.PINK_WOOL,
                Material.GRAY_WOOL,
                Material.LIGHT_GRAY_WOOL,
                Material.CYAN_WOOL,
                Material.PURPLE_WOOL,
                Material.BLUE_WOOL,
                Material.BROWN_WOOL,
                Material.GREEN_WOOL,
                Material.RED_WOOL,
                Material.BLACK_WOOL
        };
        return woolTypes[new Random().nextInt(woolTypes.length)];
    }

    private void generateBridgeStructure(Player player, Block startBlock, BlockFace direction) {
        new BukkitRunnable() {
            int length = 0;
            final int maxLength = 80;

            @Override
            public void run() {
                if (length >= maxLength) {
                    this.cancel();
                    return;
                }

                for (int i = 0; i < 5; i++) {
                    if (length >= maxLength) break;
                    Block targetBlock = startBlock.getRelative(direction, length);
                    if (targetBlock.getType() == Material.AIR) {
                        Material randomWool = getRandomWools();
                        targetBlock.setType(randomWool);
                        player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, targetBlock.getLocation(), 5, 0.5, 0.5, 0.5, 0);
                    }
                    length++;
                }

                player.getWorld().playSound(startBlock.getLocation(), org.bukkit.Sound.BLOCK_WOOL_PLACE, 1.0f, 1.0f);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }


    private void generateDefenseTower(Player player, Block baseBlock) {
        new BukkitRunnable() {
            int height = 0;
            final int maxHeight = 12;
            final int radius = 3;

            @Override
            public void run() {
                if (height >= maxHeight) {
                    this.cancel();
                    return;
                }

                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if ((Math.abs(x) == radius || Math.abs(z) == radius) || height == maxHeight - 1 || height == 6) {
                            Block block = baseBlock.getRelative(x, height, z);
                            if (block.getType() == Material.AIR) {
                                block.setType(getRandomWools());
                                player.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, block.getLocation(), 5, 0.5, 0.5, 0.5, 0);
                            }
                        }
                    }
                }

                // 在中心生成羊毛柱子
                Block centerBlock = baseBlock.getRelative(0, height, 0);
                centerBlock.setType(getRandomWools());

                // 确定玩家面向的方向
                BlockFace playerFace = getPlayerFacing(player);
                // 获取玩家面向的反方向
                BlockFace oppositePlayerFace = playerFace.getOppositeFace();

                // 在面向玩家的一侧添加梯子
                //Block ladderBlock = centerBlock.getRelative(playerFace);
                // 在玩家面向的反方向添加梯子
                Block ladderBlock = centerBlock.getRelative(oppositePlayerFace);
                if (ladderBlock.getType() == Material.AIR || ladderBlock.getType() == Material.LADDER
                || ladderBlock.getType().name().endsWith("_WOOL")) {
                    ladderBlock.setType(Material.LADDER);
                    org.bukkit.block.data.type.Ladder ladder = (org.bukkit.block.data.type.Ladder) ladderBlock.getBlockData();
                    ladder.setFacing(playerFace.getOppositeFace());
                    ladderBlock.setBlockData(ladder);
                }

                player.getWorld().playSound(baseBlock.getLocation(), org.bukkit.Sound.BLOCK_WOOL_PLACE, 1.0f, 1.0f);
                height++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // 辅助方法：获取玩家面向的方向
    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) {
            yaw += 360;
        }
        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }















}
