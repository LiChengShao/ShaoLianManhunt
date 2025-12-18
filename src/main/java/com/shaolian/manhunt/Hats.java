package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.Listener;


import java.util.ArrayList;
import java.util.List;

public class Hats implements Listener {

    private Main plugin;

    public Hats(Main plugin)  {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public ItemStack createCustomHats() {
        ItemStack banner = new ItemStack(Material.WHITE_BANNER);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();

        // 设置物品的名称
        meta.setDisplayName("§6自定义头盔");
        // 设置物品的Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值相当于钻石头盔");
        meta.setLore(lore);
        // 设置物品的模型数据
        NamespacedKey modelKey = new NamespacedKey("minecraft", "wizard_hat");  // 创建NamespacedKey
        meta.setItemModel(modelKey);  // 使用NamespacedKey设置模型
        banner.setItemMeta(meta);

        return banner;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;
        if (item.isSimilar(createCustomHats())) {
            // 创建GUI
            Inventory gui = Bukkit.createInventory(null, 36, "选择头饰");

            // 添加巫师帽
            ItemStack wizardHat = createWizardHat(player);
            gui.setItem(10, wizardHat);

            //添加小鸡帽
            ItemStack chickenHat = createChickenHat(player);
            gui.setItem(11, chickenHat);

            ItemStack hat1 = createHat1(player);
            gui.setItem(12, hat1);
            ItemStack hat2 = createHat2(player);
            gui.setItem(13, hat2);
            ItemStack hat3 = createHat3(player);
            gui.setItem(14, hat3);
            ItemStack hat4 = createHat4(player);
            gui.setItem(15, hat4);
            ItemStack hat5 = createHat5(player);
            gui.setItem(16, hat5);
            ItemStack hat6 = createHat6(player);
            gui.setItem(19, hat6);

            // 添加屏障
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta barrierMeta = barrier.getItemMeta();
            barrierMeta.setDisplayName(ChatColor.RED + "不佩戴头饰");
            barrier.setItemMeta(barrierMeta);
            gui.setItem(35, barrier);

            // 打开GUI
            event.getPlayer().openInventory(gui);
            event.setCancelled(true);
        }
    }

    // 添加处理GUI点击事件的方法
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();

        if (clicked == null) return; // 添加 null 检查

        //如果游戏没开始，点击帽子无效
        if(!GameManager.isGameRunning) {
            if (clicked.getType() == Material.STICK || clicked.getType() == Material.CHICKEN_SPAWN_EGG
                    || clicked.getType() == Material.GOLD_INGOT || clicked.getType() == Material.FEATHER
                    || clicked.getType() == Material.TURTLE_SCUTE || clicked.getType() == Material.BLACK_STAINED_GLASS
                    || clicked.getType() == Material.CAULDRON) {
                event.setCancelled(true);
                event.setCancelled(true);
            }
        }

        if (event.getView().getTitle().equals("选择头饰")) {
            event.setCancelled(true);

            //巫师帽
            if (clicked.getType() == Material.STICK) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:wizard_hat\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",
                        player.getName()
                );

                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }
            //小鸡帽
            else if (clicked.getType() == Material.CHICKEN_SPAWN_EGG) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:chicken-hat\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",

                        player.getName()
                );

                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }
            //潜海金盔
            else if (clicked.getType() == Material.GOLD_INGOT) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:diving-helmet\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",

                        player.getName()
                );
                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }
            //飞行员头盔
            else if (clicked.getType() == Material.FEATHER) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:flight-hat\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",

                        player.getName()
                );
                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }
            //军用头盔
            else if (clicked.getType() == Material.TURTLE_SCUTE) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:military-helmet\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",

                        player.getName()
                );
                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }
            //摩托头盔
            else if (clicked.getType() == Material.BLACK_STAINED_GLASS) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:motorcycle-hat\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",
                        player.getName()
                );
                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }
            //煎蛋头盔
            else if (clicked.getType() == Material.CAULDRON) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:pot-hat\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",
                        player.getName()
                );
                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }
            //善魂头盔
            else if (clicked.getType() == Material.GHAST_TEAR) {
                // 构建替换指令
                String command = String.format(
                        "item replace entity %s armor.head with stick[item_model=\"minecraft:happy_ghast_hat\"," +
                                "equippable={slot:\"head\"," +
                                "equip_sound:\"item.armor.equip_chain\"," + // 修改为盔甲装备音效
                                "dispensable:true," + //允许通过发射器装备
                                "swappable:true," + //允许与其他装备交换
                                "damage_on_hurt:true}]",
                        player.getName()
                );
                // 通过控制台执行指令
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                player.closeInventory();
            }

            else if (clicked.getType() == Material.BARRIER) {
                // 移除帽子
                player.getInventory().setHelmet(null);
                // 播放音效，仅对当前玩家
                player.playSound(player.getLocation(), "entity.villager.yes", 1, 1);
            }
        }
    }

    // 创建巫师帽
    private ItemStack createWizardHat(Player player) {
        ItemStack wizardHat = new ItemStack(Material.STICK);
        ItemMeta meta = wizardHat.getItemMeta();

        // 设置物品的名称
        meta.setDisplayName(ChatColor.GOLD + "巫师法帽");
        // 设置物品的Lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        // 设置物品的模型数据
        NamespacedKey modelKey = new NamespacedKey("minecraft", "wizard_hat");  // 创建NamespacedKey
        meta.setItemModel(modelKey);  // 使用NamespacedKey设置模型

        // 将meta应用到物品上
        wizardHat.setItemMeta(meta);

        // 设置物品的装备属性


        // 设置物品的装备音效
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        wizardHat.setItemMeta(meta);

        return wizardHat;
    }

    private ItemStack createChickenHat(Player player) {
        ItemStack chickenHat = new ItemStack(Material.CHICKEN_SPAWN_EGG);
        ItemMeta meta = chickenHat.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "小鸡帽");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        meta.setItemModel(new NamespacedKey("minecraft", "chicken-hat"));
        chickenHat.setItemMeta(meta);
        return chickenHat;
    }

    private ItemStack createHat1(Player player) {
        ItemStack Hat1 = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = Hat1.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "潜海金盔");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        meta.setItemModel(new NamespacedKey("minecraft", "diving-helmet"));
        Hat1.setItemMeta(meta);
        return Hat1;
    }

    private ItemStack createHat2(Player player) {
        ItemStack Hat2 = new ItemStack(Material.FEATHER);
        ItemMeta meta = Hat2.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "飞行员头盔");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        meta.setItemModel(new NamespacedKey("minecraft", "flight-hat"));
        Hat2.setItemMeta(meta);
        return Hat2;
    }

    private ItemStack createHat3(Player player) {
        ItemStack Hat3 = new ItemStack(Material.TURTLE_SCUTE);
        ItemMeta meta = Hat3.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "军用头盔");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        meta.setItemModel(new NamespacedKey("minecraft", "military-helmet"));
        Hat3.setItemMeta(meta);
        return Hat3;
    }

    private ItemStack createHat4(Player player) {
        ItemStack Hat4 = new ItemStack(Material.BLACK_STAINED_GLASS);
        ItemMeta meta = Hat4.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "摩托头盔");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        meta.setItemModel(new NamespacedKey("minecraft", "motorcycle-hat"));
        Hat4.setItemMeta(meta);
        return Hat4;
    }

    private ItemStack createHat5(Player player) {
        ItemStack Hat5 = new ItemStack(Material.CAULDRON);
        ItemMeta meta = Hat5.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "煎蛋头盔");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        meta.setItemModel(new NamespacedKey("minecraft", "pot-hat"));
        Hat5.setItemMeta(meta);
        return Hat5;
    }

    private ItemStack createHat6(Player player) {
        ItemStack Hat6 = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = Hat6.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "善魂头盔");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "护甲值等于无附魔的钻石头盔");
        meta.setLore(lore);
        meta.setItemModel(new NamespacedKey("minecraft", "happy_ghast_hat"));
        Hat6.setItemMeta(meta);
        return Hat6;
    }









}
