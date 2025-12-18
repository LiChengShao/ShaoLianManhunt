package com.shaolian.manhunt;


import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PiglinBarterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import java.util.Random;

public class PiglinBarterListener implements Listener {

    private final Main plugin;

    public PiglinBarterListener(Main plugin) {
        this.plugin = plugin;
    }

    private final Random random = new Random();

    @EventHandler
    public void onPiglinBarter(PiglinBarterEvent event) {
        // 清除原版交易结果
        event.getOutcome().clear();

        // 自定义交易概率
        double chance = random.nextDouble();

        if (chance < 0.15) { // 16% 概率获得珍珠
            event.getOutcome().add(new ItemStack(Material.ENDER_PEARL));
        } else if (chance < 0.17) { // 1% 概率获得灵魂疾行附魔书
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
            meta.addStoredEnchant(Enchantment.SOUL_SPEED, random.nextInt(3) + 1, true);
            book.setItemMeta(meta);
            event.getOutcome().add(book);
        } else if (chance < 0.22) { // 5% 概率获得灵魂疾行铁靴
            ItemStack boots = new ItemStack(Material.IRON_BOOTS);
            boots.addEnchantment(Enchantment.SOUL_SPEED, random.nextInt(3) + 1);
            event.getOutcome().add(boots);
        } else if (chance < 0.23) { // 1% 概率获得喷溅型抗火药水
            ItemStack fireResistancePotion = new ItemStack(Material.SPLASH_POTION);
            PotionMeta meta = (PotionMeta) fireResistancePotion.getItemMeta();
            meta.setBasePotionData(new PotionData(PotionType.FIRE_RESISTANCE));
            fireResistancePotion.setItemMeta(meta);
            event.getOutcome().add(fireResistancePotion);

        } else if (chance < 0.24) { // 1% 概率获得普通抗火药水
            ItemStack fireResistancePotion2 = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) fireResistancePotion2.getItemMeta();
            meta.setBasePotionData(new PotionData(PotionType.FIRE_RESISTANCE));
            fireResistancePotion2.setItemMeta(meta);
            event.getOutcome().add(fireResistancePotion2);

        } else if (chance < 0.26) { // 2% 概率获得普通水瓶
            ItemStack waterBottle = new ItemStack(Material.POTION);
            PotionMeta meta = (PotionMeta) waterBottle.getItemMeta();
            meta.setBasePotionData(new PotionData(PotionType.WATER));
            waterBottle.setItemMeta(meta);
            event.getOutcome().add(waterBottle);
        } else if (chance < 0.32) { // 6% 概率获得5~7个铁粒
            event.getOutcome().add(new ItemStack(Material.IRON_NUGGET, random.nextInt(3) + 5));
        } else if (chance < 0.38) { // 6% 概率获得3~4根线
            event.getOutcome().add(new ItemStack(Material.STRING, random.nextInt(2) + 3));
        } else if (chance < 0.42) { // 6% 概率获得4个下界石英
            event.getOutcome().add(new ItemStack(Material.QUARTZ, random.nextInt(10) + 5));
        } else if (chance < 0.50) { // 8% 概率获得黑曜石
            event.getOutcome().add(new ItemStack(Material.OBSIDIAN));
        } else if (chance < 0.55) { // 5% 概率获得哭泣的黑曜石
            event.getOutcome().add(new ItemStack(Material.CRYING_OBSIDIAN));
        } else if (chance < 0.60) { // 5% 概率获得火焰弹
            event.getOutcome().add(new ItemStack(Material.FIRE_CHARGE));
        } else if (chance < 0.65) { // 5% 概率获得皮革
            event.getOutcome().add(new ItemStack(Material.LEATHER));
        } else if (chance < 0.70) { // 5% 概率获得灵魂沙
            event.getOutcome().add(new ItemStack(Material.SOUL_SAND));
        } else if (chance < 0.75) { // 5% 概率获得4个下界砖
            event.getOutcome().add(new ItemStack(Material.NETHER_BRICK, random.nextInt(10) + 5));
        } else if (chance < 0.80) { // 5% 概率获得光灵箭
            event.getOutcome().add(new ItemStack(Material.SPECTRAL_ARROW, random.nextInt(3) + 1));
        } else if (chance < 0.85) { // 5% 概率获得普通箭
            event.getOutcome().add(new ItemStack(Material.ARROW, random.nextInt(3) + 1));
        } else if (chance < 0.93) { // 8% 概率获得沙砾
            event.getOutcome().add(new ItemStack(Material.GRAVEL, random.nextInt(9) + 4));
        } else { // 7% 概率获得黑石
            event.getOutcome().add(new ItemStack(Material.BLACKSTONE, random.nextInt(9) + 4));
        }
    }
}
