package com.shaolian.manhunt;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
// 如果需要 PersistentDataContainer 来更可靠地识别 opener item
// import org.bukkit.persistence.PersistentDataType;
// import org.bukkit.NamespacedKey;

public class Hats2 implements Listener {

    private Main plugin; // 假设 Main 继承自 JavaPlugin
    private final String GUI_TITLE = "选择头饰";
    private ItemStack guiOpenerItem; // 缓存GUI打开器物品

    // 这些字段似乎是为另一种功能（例如特定头盔的持续效果）准备的
    // 在当前的GUI选择逻辑中没有直接使用
    // private final Map<UUID, Boolean> wearingCustomHelmet = new HashMap<>();
    // private final String CUSTOM_HELMET_ID = "somehats:cap_tint";

    public Hats2(Main plugin) {
        this.plugin = plugin;
        this.guiOpenerItem = createGuiOpenerItem(); // 创建并缓存打开器物品
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    // 创建用于打开头饰选择GUI的物品
    public ItemStack createGuiOpenerItem() {
        ItemStack opener = new ItemStack(Material.NETHER_STAR); // 使用下界之星或其他独特物品
        ItemMeta meta = opener.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "头饰选择器");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "右键点击打开头饰选择界面");
            // 更可靠的方式是使用 PersistentDataContainer 标记此物品:
            // NamespacedKey key = new NamespacedKey(plugin, "custom_hat_opener");
            // meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            meta.setLore(lore);
            opener.setItemMeta(meta);
        }
        return opener;
    }

    // 如果你想给玩家这个打开器物品，可以调用此方法
    public ItemStack getGuiOpenerItem() {
        return this.guiOpenerItem.clone(); // 返回一个克隆
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = event.getItem(); // 获取玩家主手上的物品

        // 检查是否是GUI打开器物品
        if (itemInHand == null || !itemInHand.isSimilar(this.guiOpenerItem)) {
            // 如果使用PersistentDataContainer:
            // if (itemInHand == null || itemInHand.getItemMeta() == null ||
            //     !itemInHand.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "custom_hat_opener"), PersistentDataType.BYTE)) {
            //    return;
            // }
            return;
        }

        // 你可能只想处理右键点击
        // if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
        //    return;
        // }

        // 创建并打开GUI
        Inventory gui = Bukkit.createInventory(null, 54, GUI_TITLE); // 54个槽位 = 6行，标准尺寸

        // 填充GUI的ItemAdder头饰
        int slotIndex = 10; // 从第10个槽位开始放置 (可以按需调整)
        addHatToGui(gui, slotIndex++, "somehats:axolotl_pink_hat");
        addHatToGui(gui, slotIndex++, "somehats:axolotl_tinted");
        addHatToGui(gui, slotIndex++, "somehats:cap");
        addHatToGui(gui, slotIndex++, "somehats:cap_tint");
        addHatToGui(gui, slotIndex++, "somehats:carrot_hat");
        addHatToGui(gui, slotIndex++, "somehats:ghost_hat");
        addHatToGui(gui, slotIndex+3, "somehats:ghost_hat_tinted");
        addHatToGui(gui, slotIndex++, "somehats:panda_hat");
        addHatToGui(gui, slotIndex++, "somehats:pirate_hat");
        addHatToGui(gui, slotIndex++, "somehats:pirate_hat_tint");
        addHatToGui(gui, slotIndex++, "somehats:santa_hat");
        addHatToGui(gui, slotIndex++, "somehats:santa_hat_tint");
        addHatToGui(gui, slotIndex++, "somehats:wool_cap_hat");
        addHatToGui(gui, slotIndex++, "somehats:wool_cap_hat_tint");
        addHatToGui(gui, slotIndex++, "villagerhats:armorermask");
        addHatToGui(gui, slotIndex++, "villagerhats:butcherheadband");
        addHatToGui(gui, slotIndex++, "villagerhats:cartographermonocle");
        addHatToGui(gui, slotIndex++, "villagerhats:fisherhat");
        addHatToGui(gui, slotIndex++, "villagerhats:fletcherhat");
        addHatToGui(gui, slotIndex++, "villagerhats:librarianhat");
        addHatToGui(gui, slotIndex++, "villagerhats:shepherdhat");
        addHatToGui(gui, slotIndex++, "villagerhats:weaponsmitheyepatch");

        // 根据需要添加更多帽子，slotIndex会自动递增

        // 添加“不佩戴头饰”的屏障
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta barrierMeta = barrier.getItemMeta();
        if (barrierMeta != null) {
            barrierMeta.setDisplayName(ChatColor.RED + "不佩戴头饰");
            List<String> barrierLore = new ArrayList<>();
            barrierLore.add(ChatColor.GRAY + "点击移除当前头饰");
            barrierMeta.setLore(barrierLore);
            barrier.setItemMeta(barrierMeta);
        }
        gui.setItem(53, barrier); // 放置在54槽GUI的最后一个槽位 (索引53)

        player.openInventory(gui);
        event.setCancelled(true); // 阻止与打开器物品的默认交互
    }

    private void addHatToGui(Inventory gui, int slot, String namespacedId) {
        // 检查槽位是否有效，并且不是为屏障等控制项保留的最后一个槽位
        if (slot >= gui.getSize() -1 && gui.getItem(gui.getSize() -1) != null && gui.getItem(gui.getSize() -1).getType() == Material.BARRIER) {
            plugin.getLogger().warning("GUI 槽位 " + slot + " 对于帽子 " + namespacedId + " 超出范围或过于接近控制物品。");
            return;
        }
        if (slot >= gui.getSize()) {
            plugin.getLogger().warning("GUI 槽位 " + slot + " 对于帽子 " + namespacedId + " 超出范围。");
            return;
        }

        CustomStack customStack = CustomStack.getInstance(namespacedId);
        if (customStack != null) {
            ItemStack hatItem = customStack.getItemStack();
            if (hatItem != null) {
                gui.setItem(slot, hatItem);
            } else {
                plugin.getLogger().warning("无法获取 " + namespacedId + " 的 ItemStack。");
            }
        } else {
            plugin.getLogger().warning("未找到 ItemAdder 物品: " + namespacedId);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // 检查是否是我们自定义的GUI
        if (!event.getView().getTitle().equals(GUI_TITLE)) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem(); // 获取被点击的物品
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return; // 点击了空格子
        }

        // 检查是否点击了屏障
        if (clickedItem.getType() == Material.BARRIER) {
            ItemMeta meta = clickedItem.getItemMeta();
            // 通过显示名称确保是我们放置的屏障
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals(ChatColor.RED + "不佩戴头饰")) {
                player.getInventory().setHelmet(null); // 移除头盔
                player.closeInventory();
                player.playSound(player.getLocation(), "entity.villager.no", 1, 1);
                return;
            }
        }

        // 检查被点击的物品是否是 ItemAdder 的自定义物品
        CustomStack customStack = CustomStack.byItemStack(clickedItem);
        if (customStack != null) {
            // 是 ItemAdder 的头饰，装备它
            // clone() 是个好习惯，以防万一ItemAdder对实例有特殊处理
            player.getInventory().setHelmet(clickedItem.clone());
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND,1,1);
            player.closeInventory();

            // 尝试获取 ItemAdder 定义的显示名称，如果获取不到则使用其 ID
            String itemName = customStack.getDisplayName() != null && !customStack.getDisplayName().isEmpty()
                    ? customStack.getDisplayName()
                    : customStack.getNamespacedID();

        }
        // 如果既不是屏障，也不是可识别的 ItemAdder 物品，由于事件已被取消，将不会发生任何事。
    }
}