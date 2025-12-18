package com.shaolian.manhunt;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;




import java.lang.reflect.Method;
import java.util.*;

public class IronMan implements Listener {
    private final Main plugin;

    public IronMan(Main plugin) {
        this.plugin = plugin;
    }

    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final long COOLDOWN_TIME = 5 * 60 * 1000; // 5分钟的冷却时间（毫秒）
    private final Map<UUID, IronGolem> playerIronGolems = new HashMap<>();



    //创造特殊的南瓜
    public ItemStack createPumpkin() {
        ItemStack pumpkin = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = pumpkin.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d召唤铁傀儡同伴");
            meta.setLore(Arrays.asList("§7放置可以召唤铁傀儡同伴", "§dCD:5min"));
            meta.setUnbreakable(true);
            // 添加附魔效果（这里使用耐久性附魔，但不会实际影响物品）
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            // 隐藏附魔信息和其他可能的标志
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            pumpkin.setItemMeta(meta);
        }
        return pumpkin;
    }

    //与物品交互
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.CARVED_PUMPKIN) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("§7放置可以召唤铁傀儡同伴")) {
                event.setCancelled(true); // 取消原版的行为
                if(event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    event.setCancelled(true); // 取消原版的行为
                    // 检查冷却时间
                    if (checkCooldown(player)) {
                        creatIronMan(player);
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

    public void creatIronMan(Player player) {
        Location spawnLocation = player.getTargetBlock(null, 5).getLocation().add(0, 1, 0);

        // 创建烟花特效
        spawnFireworks(spawnLocation);

        // 延迟0.5秒后生成铁傀儡
        new BukkitRunnable() {
            @Override
            public void run() {
                IronGolem ironGolem = (IronGolem) player.getWorld().spawnEntity(spawnLocation, EntityType.IRON_GOLEM);
                ironGolem.setCustomName(player.getName() + "的铁傀儡");
                ironGolem.setCustomNameVisible(true);

                // 设置元数据，用于识别铁傀儡的主人
                ironGolem.setMetadata("owner", new FixedMetadataValue(plugin, player.getUniqueId()));

                playerIronGolems.put(player.getUniqueId(), ironGolem);

                // 削弱铁傀儡的伤害
                AttributeInstance attackDamage = ironGolem.getAttribute(Attribute.ATTACK_DAMAGE);
                if (attackDamage != null) {
                    // 将伤害降低到原来的60%
                    double newDamage = attackDamage.getBaseValue() * 0.6;
                    attackDamage.setBaseValue(newDamage);
                }



                for(Player player1 : Bukkit.getOnlinePlayers()) {
                    player1.sendMessage("§a" + player.getName() + "召唤了铁傀儡");
                }


                //移除铁傀儡
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (ironGolem.isValid()) {
                            ironGolem.remove();
                            removeIronGolem(player.getUniqueId());
                            player.sendMessage("§c你的铁傀儡已消失。");
                        }
                    }
                }.runTaskLater(plugin, 60 * 20L); // 1分钟后


            }
        }.runTaskLater(plugin, 10L);
    }


    private void removeIronGolem(UUID playerUUID) {
        //左边定义了该玩家的铁傀儡。右边在这个Map中移除了这个玩家
        IronGolem ironGolem = playerIronGolems.remove(playerUUID);
        if (ironGolem != null && ironGolem.isValid()) {
            // 移除Map中的铁傀儡
            ironGolem.remove();
        }

    }


    private void spawnFireworks(Location location) {
        Firework fw = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();

        FireworkEffect effect = FireworkEffect.builder().withColor(Color.SILVER).with(FireworkEffect.Type.BALL).build();
        fwm.addEffect(effect);
        fwm.setPower(0);

        fw.setFireworkMeta(fwm);
        fw.detonate();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        // 检查伤害来源是否为玩家，如果不是则直接返回
        if (!(event.getDamager() instanceof Player)) return;

        // 将伤害来源转换为玩家对象
        Player attacker = (Player) event.getDamager();

        // 检查攻击者是否召唤了铁傀儡
        IronGolem ironGolem = playerIronGolems.get(attacker.getUniqueId());
        // 如果玩家没有铁傀儡，或者铁傀儡不再有效（可能已经死亡或被移除），则直接返回
        if (ironGolem == null || !ironGolem.isValid()) return;

        // 设置铁傀儡的目标
        //event.getEntity() 返回事件中涉及的实体。
        // 在 EntityDamageByEntityEvent 中，这是被攻击的实体。
        //检查事件中被攻击的实体是否是 LivingEntity 类型或其子类型
        if (event.getEntity() instanceof LivingEntity) {
            //用target接收被攻击的实体
            LivingEntity target = (LivingEntity) event.getEntity();

            // 检查目标是否是铁傀儡
            if (target instanceof IronGolem) {
                // 如果目标是铁傀儡，检查它是否属于任何玩家
                for (IronGolem playerGolem : playerIronGolems.values()) {
                    if (playerGolem.getUniqueId().equals(target.getUniqueId())) {
                        // 如果目标是玩家的铁傀儡，不要将其设为攻击目标
                        return;
                    }
                }
             }
        ironGolem.setTarget((LivingEntity) event.getEntity());
    }
    }

    @EventHandler
    public void onIronGolemDamage(EntityDamageByEntityEvent event) {
        // 检查被攻击的实体是否为铁傀儡
        if (event.getEntity() instanceof IronGolem ironGolem) {
            // 检查是否是玩家的铁傀儡
            if (!ironGolem.hasMetadata("owner")) return;

            UUID ownerUUID = (UUID) ironGolem.getMetadata("owner").get(0).value();

            // 检查攻击者是否为玩家
            if (event.getDamager() instanceof Player damager) {
                // 如果攻击者不是铁傀儡的主人，允许伤害
                if (!damager.getUniqueId().equals(ownerUUID)) {
                    return;
                }
                // 如果攻击者是铁傀儡的主人，取消伤害
                event.setCancelled(true);
                damager.sendMessage("§c你不能伤害自己的铁傀儡！");
            }
        }

        // 检查攻击者是否为铁傀儡，被攻击者是否为玩家
        if (event.getDamager() instanceof IronGolem ironGolem && event.getEntity() instanceof Player victim) {
            // 检查是否是玩家的铁傀儡
            if (!ironGolem.hasMetadata("owner")) return;

            UUID ownerUUID = (UUID) ironGolem.getMetadata("owner").get(0).value();

            // 如果被攻击的玩家是铁傀儡的主人，取消伤害
            if (victim.getUniqueId().equals(ownerUUID)) {
                event.setCancelled(true);
            }
        }
    }







}














