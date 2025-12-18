package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import org.bukkit.util.Vector;

public class BeeGun implements Listener {
    private final Main plugin;

    public BeeGun(Main plugin) {
        this.plugin = plugin;
        // 每分钟清理一次无效的蜜蜂记录
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupBees, 1200L, 1200L);
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 60 * 1000; // 1分钟的冷却时间（毫秒）

    private Map<UUID, UUID> beeOwners = new HashMap<>();
    private static final long BEE_LIFESPAN = 30 * 20; // 30 秒，以游戏刻为单位


    //创造特殊的枪
    public ItemStack createDragonHead() {
        ItemStack specialDragonHead = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta meta = specialDragonHead.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d蜜蜂枪");
            meta.setLore(Arrays.asList("§7左击可以发射蜜蜂", "§dCD:1min"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            specialDragonHead.setItemMeta(meta);
        }
        return specialDragonHead;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.DRAGON_HEAD) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7左击可以发射蜜蜂")) {
                event.setCancelled(true); // 取消原版的行为
                if(event.getAction() == Action.LEFT_CLICK_AIR ||
                        event.getAction() == Action.LEFT_CLICK_BLOCK){
                     event.setCancelled(true); // 取消原版的行为
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        launchBees(player);
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

    public void launchBees(Player player) {
        // 获取玩家的朝向
        Vector direction = player.getLocation().getDirection();

        // 发射5只蜜蜂
        for (int i = 0; i < 5; i++) {
            // 在玩家位置前方稍微偏移的地方生成蜜蜂
            Bee bee = (Bee) player.getWorld().spawnEntity(
                    player.getLocation().add(direction.multiply(2)).add(Math.random() - 0.5, 1, Math.random() - 0.5),
                    EntityType.BEE
            );

            // 设置蜜蜂的速度，使其向玩家朝向飞行
            bee.setVelocity(direction.multiply(1.5));

            // 设置蜜蜂为愤怒状态
            bee.setAnger(Integer.MAX_VALUE);

            // 设置蜜蜂不会消失
            bee.setRemoveWhenFarAway(false);

            // 记录这只蜜蜂的所有者
            beeOwners.put(bee.getUniqueId(), player.getUniqueId());

            // 设置蜜蜂的目标为最近的其他玩家
            player.getWorld().getPlayers().stream()
                    .filter(p -> !p.equals(player))
                    .min((p1, p2) -> Double.compare(p1.getLocation().distanceSquared(bee.getLocation()),
                            p2.getLocation().distanceSquared(bee.getLocation())))
                    .ifPresent(bee::setTarget);

            // 设置蜜蜂的生命周期
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (bee.isValid()) {
                    bee.remove();
                    beeOwners.remove(bee.getUniqueId());
                }
            }, BEE_LIFESPAN);

        // 播放音效和粒子效果
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BEE_LOOP_AGGRESSIVE, 0.5f, 0.2f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation(), 30, 0.5, 0.5, 0.5, 0.1);

      }
        for (Player player2 : Bukkit.getOnlinePlayers()){
            player2.sendMessage("§d" + player.getName() + " 发射了5只有毒的蜜蜂！");
        }
    }

    @EventHandler
    public void onBeeTargetPlayer(EntityTargetEvent event) {
        if (event.getEntity() instanceof Bee && event.getTarget() instanceof Player) {
            Bee bee = (Bee) event.getEntity();
            Player targetPlayer = (Player) event.getTarget();

            UUID ownerUUID = beeOwners.get(bee.getUniqueId());
            if (ownerUUID != null && ownerUUID.equals(targetPlayer.getUniqueId())) {
                event.setCancelled(true);

                // 尝试重新设置目标为其他玩家
                targetPlayer.getWorld().getPlayers().stream()
                        .filter(p -> !p.getUniqueId().equals(ownerUUID))
                        .min((p1, p2) -> Double.compare(p1.getLocation().distanceSquared(bee.getLocation()),
                                p2.getLocation().distanceSquared(bee.getLocation())))
                        .ifPresent(bee::setTarget);
            }
        }
    }

    @EventHandler
    public void onBeeDamagePlayer(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Bee && event.getEntity() instanceof Player) {
            Bee bee = (Bee) event.getDamager();
            Player player = (Player) event.getEntity();

            // 检查这只蜜蜂是否是由插件发射的
            if (beeOwners.containsKey(bee.getUniqueId())) {
                // 给予玩家5秒的中毒和虚弱效果
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 10 * 20, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 10 * 20, 0));


            }
        }
    }



    private void cleanupBees() {
        beeOwners.entrySet().removeIf(entry ->
                Bukkit.getEntity(entry.getKey()) == null || !(Bukkit.getEntity(entry.getKey()) instanceof Bee));
    }





    }
