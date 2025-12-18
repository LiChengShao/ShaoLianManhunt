package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

import java.util.*;


public class Brush implements Listener {

    private Main plugin;


    public Brush(Main plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this,plugin);
    }


    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 60 * 2 * 1000; // 2分钟的冷却时间（毫秒）
    private final Random random = new Random();


    //创造特殊的蝙
    public ItemStack createBrush() {
        ItemStack brush = new ItemStack(Material.BRUSH);
        ItemMeta meta = brush.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d召唤陨石");
            meta.setLore(Arrays.asList("§7左击召唤陨石", "§dCD:2min"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            brush.setItemMeta(meta);
        }
        return brush;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.BRUSH) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击召唤陨石")) {
                event.setCancelled(true); // 取消原版的行为
                if(event.getAction() == Action.LEFT_CLICK_AIR ||
                        event.getAction() == Action.LEFT_CLICK_BLOCK){
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        summonsMeteorites(player);
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

    public void summonsMeteorites(Player player) {
        summonMeteorite(player);
        for(Player player1 : Bukkit.getOnlinePlayers()){
            player1.sendMessage(player.getName()+"§c召唤了陨石");
        }
    }

    public void summonMeteorite(Player player) {
        Location startLocation = player.getLocation().clone().add(0, 50, 0);
        createMeteorite(startLocation);
    }

    private void createMeteorite(Location center) {
        //用List来储存陨石方块，因为陨石由多种方块组成
        List<FallingBlock> meteoriteBlocks = new ArrayList<>();
        //获取玩家所在的世界
        World world = center.getWorld();
        int radius = 5;
        int maxBlocks = 150; // 限制最大方块数;

        for (int i = 0; i < maxBlocks; i++) {
            // 在球体内随机选择位置
            double x = (random.nextDouble() * 2 - 1) * radius;
            double y = (random.nextDouble() * 2 - 1) * radius;
            double z = (random.nextDouble() * 2 - 1) * radius;

            if (x * x + y * y + z * z <= radius * radius) {
                Location blockLoc = center.clone().add(x, y, z);
                Material material = getRandomMeteoriteMaterial();
                FallingBlock fallingBlock = world.spawnFallingBlock(blockLoc, material.createBlockData());
                fallingBlock.setDropItem(false);
                fallingBlock.setHurtEntities(true);
                meteoriteBlocks.add(fallingBlock);
            }
        }
//        for (int x = -radius; x <= radius; x++) {
//            for (int y = -radius; y <= radius; y++) {
//                for (int z = -radius; z <= radius; z++) {
//                    //遍及方块，并确保x,y,z坐标在球体内
//                    if (x * x + y * y + z * z <= radius * radius) {
//                        //blockLoc的位置为玩家的位置加偏移位置，也就是每个陨石方块的文职
//                        Location blockLoc = center.clone().add(x, y, z);
//                        //才来哦用这个方法获取
//                        Material material = getRandomMeteoriteMaterial();
//                        //FallingBlock是一个实体，它会自动下落，所以我们不需要自己写下落的代码
//                        FallingBlock fallingBlock = world.spawnFallingBlock(blockLoc, material.createBlockData());
//                        // 设置该下落方块在落地后不会变成掉落物品
//                        fallingBlock.setDropItem(false);
//                        // 设置该下落方块可以对实体造成伤害
//                        fallingBlock.setHurtEntities(true);
//                        // 将生成的下落方块添加到陨石方块列表中，以便后续跟踪和控制
//                        meteoriteBlocks.add(fallingBlock);
//                    }
//                }
//            }
//        }

        // 开始陨石下落的动画
//        new BukkitRunnable() {
//            @Override
//            public void run() {
//                //allLanded变量用于检查所有方块是否都已落地
//                boolean allLanded = true;
//                //对于所有的陨石方块
//                for (FallingBlock block : meteoriteBlocks) {
//                    if (block.isValid()) {
//                        allLanded = false;
//                        block.setVelocity(new Vector(0, -0.5, 0));
//                    } else {
//                        handleLandedBlock(block.getLocation());
//                    }
//                }
//                if (allLanded) {
//                    this.cancel();
//                }
//            }
//        }.runTaskTimer(plugin, 0L, 1L);
//    }

        // 使用更高效的方式来控制陨石下落
        new BukkitRunnable() {
            int ticksRun = 0;
            @Override
            public void run() {
                ticksRun++;
                boolean allLanded = true;
                for (FallingBlock block : meteoriteBlocks) {
                    if (block.isValid()) {
                        //没有全部着陆
                        allLanded = false;
                        // 每5tick才更新一次速度，减少计算量
                        if (ticksRun % 5 == 0) {
                            block.setVelocity(new Vector(0, -0.5, 0));
                        }
                    }
                    //这个方块已经着陆了
                    else {
                        handleLandedBlock(block.getLocation());
                        meteoriteBlocks.remove(block);
                    }
                }
                if (allLanded || ticksRun > 200) { // 添加最大运行时间限制
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }


    private Material getRandomMeteoriteMaterial() {
        Material[] materials = {
                Material.STONE, Material.COBBLESTONE, Material.DEEPSLATE,
                Material.BASALT, Material.END_STONE, Material.OBSIDIAN,
                Material.COBBLED_DEEPSLATE, Material.MAGMA_BLOCK,
        };
        return materials[random.nextInt(materials.length)];
    }

    private void handleLandedBlock(Location location) {
        Block landedOn = location.getBlock();
        landedOn.setType(Material.AIR);
        simulateBoom(location);
        location.getWorld().spawnParticle(Particle.EXPLOSION, location, 1);
        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

    }

    private void simulateBoom(Location location) {
        World world = location.getWorld();
        if (world == null) return;

        // 爆炸半径
        double radius = 3.0;
        // 爆炸威力（影响方块破坏范围）
        float power = 4.0f;
        // 是否破坏方块
        boolean breakBlocks = true;
        // 是否生成火焰
        boolean setFire = true;

        // 创建一个假的爆炸效果
        // 威力设置为0.3f，这个值很小，主要是为了产生视觉和音效，而不是实际的破坏
        // setFire 和 breakBlocks 参数在这里实际上不起作用，因为威力太小
        world.createExplosion(location, 0.3f, setFire, breakBlocks);

        // 手动破坏方块
        for (int x = -((int) radius); x <= (int) radius; x++) {
            for (int y = -((int) radius); y <= (int) radius; y++) {
                for (int z = -((int) radius); z <= (int) radius; z++) {
                    Location blockLoc = location.clone().add(x, y, z);
                    if (blockLoc.distance(location) <= radius) {
                        Block block = blockLoc.getBlock();
                            if (block.getType().isSolid() && Math.random() < 0.7) {
                                if (block.getType() != Material.END_PORTAL
                                        && block.getType() != Material.END_GATEWAY
                                        && block.getType() != Material.END_PORTAL_FRAME){
                                block.breakNaturally();
                            }
                    }
                }
            }
        }

        // 对范围内的实体施加很小的伤害和击退效果
        for (Entity entity : world.getNearbyEntities(location, radius, radius, radius)) {
            if (entity instanceof LivingEntity) {
                LivingEntity livingEntity = (LivingEntity) entity;
                // 造成很小的伤害（0.5颗心）
                livingEntity.damage(1.0);

                // 计算击退方向和强度
                Vector knockback = entity.getLocation().toVector().subtract(location.toVector()).normalize().multiply(0.5);
                entity.setVelocity(entity.getVelocity().add(knockback));
            }
        }
    }
    }




}