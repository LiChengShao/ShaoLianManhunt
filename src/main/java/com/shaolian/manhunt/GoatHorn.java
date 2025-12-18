package com.shaolian.manhunt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Ravager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;



import java.util.*;

public class GoatHorn implements Listener {
    private final Main plugin;

    public GoatHorn(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5 * 60 * 1000; // 5分钟的冷却时间（毫秒）
    private final Map<UUID, UUID> playerRavagerMap = new HashMap<>();
    private final Map<UUID, Long> lastJumpMap = new HashMap<>();


    //创造特殊的号角
    public ItemStack createGoatHorn() {
        ItemStack specialGoatHorn = new ItemStack(Material.GOAT_HORN);
        ItemMeta meta = specialGoatHorn.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d掠夺兽");
            meta.setLore(Arrays.asList("§7右击可以召唤掠夺兽", "§dCD:5min"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            specialGoatHorn.setItemMeta(meta);
        }
        return specialGoatHorn;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.GOAT_HORN) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7右击可以召唤掠夺兽")) {
                if(event.getAction() == Action.RIGHT_CLICK_AIR ||
                        event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    // event.setCancelled(true); // 取消原版的行为
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        callMonster(player);
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

    public void callMonster(Player player) {
        World world = player.getWorld();
        Location spawnLocation = player.getLocation().add(player.getLocation().getDirection().multiply(2));

        // 生成掠夺兽
        Ravager ravager = (Ravager) world.spawnEntity(spawnLocation, EntityType.RAVAGER);

        // 设置掠夺兽的属性
        ravager.setCustomName(player.getName() + "的掠夺兽");
        ravager.setCustomNameVisible(true);

        // 防止掠夺兽攻击召唤者
        ravager.setTarget(null);

        // 允许玩家骑乘掠夺兽
        ravager.addPassenger(player);

        // 记录玩家和掠夺兽的关系
        playerRavagerMap.put(player.getUniqueId(), ravager.getUniqueId());

        // 设置掠夺兽的生命值
        ravager.getAttribute(Attribute.MAX_HEALTH).setBaseValue(100);
        ravager.setHealth(100);

        // 启动控制掠夺兽的任务
        //startControlTask(player, ravager);

        Bukkit.broadcastMessage(player.getName() + "§a召唤了一只掠夺兽！");

        // 设置掠夺兽的消失时间（例如2分钟后）
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ravager.isValid()) {
                    ravager.remove();
                    playerRavagerMap.remove(player.getUniqueId());
                    player.sendMessage("§c你的掠夺兽已消失。");
                }
            }
        }.runTaskLater(plugin, 20 * 60 * 2); // 2分钟后
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Ravager && event.getEntity() instanceof Player) {
            Ravager ravager = (Ravager) event.getDamager();
            Player damagedPlayer = (Player) event.getEntity();

            // 检查是否是召唤者的掠夺兽
            if (playerRavagerMap.containsValue(ravager.getUniqueId())) {
                // 如果是召唤者，取消伤害
                if (playerRavagerMap.get(damagedPlayer.getUniqueId()) == ravager.getUniqueId()) {
                    event.setCancelled(true);
                } else {
                    // 如果不是召唤者，造成伤害
                    event.setDamage(10); // 设置伤害值
                }
            }
        }

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Ravager) {
            Player player = (Player) event.getDamager();
            Ravager ravager = (Ravager) event.getEntity();
            if (playerRavagerMap.get(player.getUniqueId()) == ravager.getUniqueId()) {
                event.setCancelled(true);
                player.sendMessage("§c你不能攻击自己的掠夺兽！");
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Ravager) {
            Player player = event.getPlayer();
            Ravager ravager = (Ravager) event.getRightClicked();

            // 检查是否是玩家自己的掠夺兽
            if (playerRavagerMap.get(player.getUniqueId()) == ravager.getUniqueId()) {
                ravager.addPassenger(player);
                event.setCancelled(true);
            }
        }
    }

    private void startControlTask(Player player, Ravager ravager) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!ravager.isValid() || ravager.getPassengers().isEmpty() || !ravager.getPassengers().get(0).equals(player)) {
                    this.cancel();
                    return;
                }

                Vector direction = player.getLocation().getDirection();
                double speed = 0.5; // 可以调整这个值来改变移动速度
                Vector velocity = direction.multiply(speed);

                // 处理跳跃
                if (player.getLocation().getY() > ravager.getLocation().getY()) {
                    velocity.setY(0.5); // 可以调整这个值来改变跳跃高度
                }

                ravager.setVelocity(velocity);
            }
        }.runTaskTimer(plugin, 0, 1); // 每tick运行一次
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.getVehicle() instanceof Ravager) {
            Ravager ravager = (Ravager) player.getVehicle();
            if (playerRavagerMap.get(player.getUniqueId()) == ravager.getUniqueId()) {
                Vector direction = player.getLocation().getDirection();
                double speed = 0.4; // 可以调整这个值来改变移动速度
                // 获取玩家的朝向
                Location playerLoc = player.getLocation();

                // 设置掠夺兽的朝向与玩家一致
                ravager.setRotation(playerLoc.getYaw(), ravager.getLocation().getPitch());

                // 只考虑水平方向的移动
                direction.setY(0).normalize();

                Vector velocity = direction.multiply(speed);

                // 处理跳跃和下落
                handleVerticalMovement(ravager, player);

                // 应用水平速度
                ravager.setVelocity(velocity.add(new Vector(0, ravager.getVelocity().getY(), 0)));

                // 确保掠夺兽不会卡在方块中
                ensureNotStuck(ravager);
            }
        }
    }


    private void handleVerticalMovement(Ravager ravager, Player player) {
        Location loc = ravager.getLocation();
        Block blockBelow = loc.getBlock().getRelative(0, -1, 0);
        Block blockInFront = loc.getBlock().getRelative(player.getFacing());

        // 检查是否需要跳跃
        if (player.getLocation().getPitch() < -30 && canJump(ravager)) {
            ravager.setVelocity(ravager.getVelocity().setY(0.5));
            lastJumpMap.put(ravager.getUniqueId(), System.currentTimeMillis());
        }
        // 检查是否需要下落
        else if (!blockBelow.getType().isSolid()) {
            ravager.setVelocity(ravager.getVelocity().setY(-0.1));
        }
        // 检查是否需要上台阶
        else if (blockInFront.getType().isSolid() && !blockInFront.getRelative(0, 1, 0).getType().isSolid()) {
            ravager.setVelocity(ravager.getVelocity().setY(0.5));
        }
    }

    private boolean canJump(Ravager ravager) {
        long lastJumpTime = lastJumpMap.getOrDefault(ravager.getUniqueId(), 0L);
        return System.currentTimeMillis() - lastJumpTime > 1000; // 1秒冷却时间
    }

    private void ensureNotStuck(Ravager ravager) {
        Location loc = ravager.getLocation();
        if (loc.getBlock().getType().isSolid()) {
            // 如果掠夺兽卡在方块中，将其向上移动
            ravager.teleport(loc.add(0, 1, 0));
        }
    }

    @EventHandler
    public void onEntityTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Ravager) {
            Ravager ravager = (Ravager) event.getEntity();
            if (event.getTarget() instanceof Player) {
                Player targetPlayer = (Player) event.getTarget();
                if (playerRavagerMap.get(targetPlayer.getUniqueId()) == ravager.getUniqueId()) {
                    event.setCancelled(true);
                    ravager.setTarget(null);
                }
            }
        }
    }







}














