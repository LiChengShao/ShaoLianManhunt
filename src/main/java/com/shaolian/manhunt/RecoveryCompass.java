package com.shaolian.manhunt;

import org.bukkit.*;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import org.bukkit.event.Listener;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class RecoveryCompass implements  Listener, CommandExecutor {
    private final Main plugin;
    private GameManager gameManager;

    public RecoveryCompass(Main plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getGameManager(); // 添加这行初始化

    }

    public static int fortressVotes = 0;
    public static int hunterVotes = 0;
    public static int randomDropHunterVotes = 0;
    public static int OnevsMany = 0; //1vN模式
    public static int ZhuBo = 0; //1vN模式
    public static int ghastVotes = 0;

    public static int openBagVotes = 0; //启用共享背包
    public static int closeBagVotes = 0; //禁用用共享背包


    public static String ghastName;
    public static Player ghastPlayer;


    public static String  finalMode;

    public static Set<UUID> votedPlayers = new HashSet<>();
    public static Set<UUID> votedCommonBagPlayers = new HashSet<>();

    List<UUID> preferredHuntersCandidates = new ArrayList<>();
    List<UUID> otherCandidates = new ArrayList<>(); // 中立或明确不想成为猎人的玩家


    private List<UUID> allReadyPlayers = new ArrayList<>();

    private static Player zhuBoPlayer = null;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("runner")) {
            if (zhuBoPlayer != null) {
                sender.sendMessage("§c已经有一个主播阵营玩家：" + zhuBoPlayer.getName());
                return true;
            }
            zhuBoPlayer = player;
            sender.sendMessage("§a你已加入主播阵营！");
            sender.sendMessage("§c主播阵营玩家：" + zhuBoPlayer.getName());
            return true;
        }

        if (command.getName().equalsIgnoreCase("unrunner")) {
            // 主播阵营为null 主播阵营有一个其他玩家 直播阵营里的玩家是你
            if (zhuBoPlayer == null) {
                sender.sendMessage("§c当前没有主播阵营玩家！");
                return true;
            }
            else if (zhuBoPlayer.getUniqueId().equals(player.getUniqueId())) {
                zhuBoPlayer = null;
                sender.sendMessage("§c你已退出主播阵营");
            }
            else {
                sender.sendMessage("§c主播阵营玩家：" + zhuBoPlayer.getName());
                return true;
            }
        }

//        if (command.getName().equalsIgnoreCase("hushenfu")) {
//
//            if (GameManager.runners.contains(player.getUniqueId())) {
//                Inventory inv = player.getInventory();
//                // 获取 comPass 实例
//                comPass compass = gameManager.getCompass();
//                if (compass != null) {
//                    ItemStack amulet = compass.createAmulet(player);
//                    HashMap<Integer, ItemStack> leftover = inv.addItem(amulet);
//                    if (!leftover.isEmpty()) {
//                        for (ItemStack item : leftover.values()) {
//                            player.getWorld().dropItem(player.getLocation(), item);
//                        }
//                    }
//                } else {
//                    player.sendMessage("§c护身符功能异常，请联系管理员！");
//                }
//            }
//        }

        return false;
    }



    //选择游戏模式的物品(下界之星)
    public ItemStack createNetherStar() {
        ItemStack star = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = star.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§d投票选择游戏模式");
            List<String> lore = new ArrayList<>();
            lore.add("§7右击可以投票游戏模式");
            meta.setUnbreakable(true);
            // 添加自定义标签
            NamespacedKey key = new NamespacedKey(plugin, "undroppable");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BOOLEAN, true);
            star.setItemMeta(meta);
        }
        return star;
    }


    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ItemStack item = event.getItem();

        //如果这个人是逃亡者
        if(GameManager.runners.contains(playerId)){
            //如果物品是溯源指南针
            if (item != null && item.getType() == Material.RECOVERY_COMPASS ) {
                ItemMeta meta = item.getItemMeta();
                if(meta.hasCustomModelData() && meta.getCustomModelData() == 999){
                    event.setCancelled(true); // 取消原版的指南针的 行为
                    if(event.getAction() == Action.LEFT_CLICK_AIR ||
                            event.getAction() == Action.LEFT_CLICK_BLOCK){
                        //打开追踪GUI
                        openTrackingGUI2(playerId);
                    }
                    else if(event.getAction() == Action.RIGHT_CLICK_AIR ||
                            event.getAction() == Action.RIGHT_CLICK_BLOCK){
                    }
                }
            }
        }

        if (item != null && item.getType() == Material.NETHER_STAR) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName() && meta.getDisplayName().equals("§d投票选择游戏模式")) {
                event.setCancelled(true); // 取消原版的指南针放置行为
                //打开选择游戏模式GUI
                openChooseModeGUI(player);
            }
        }
    }

    private void openTrackingGUI2(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (GameManager.runners.contains(playerId)) {  //如果这个playerId是逃亡者
            //创建GUI的意思吧
            Inventory gui = Bukkit.createInventory(null, 27, "§a选择追踪同伴");

            // 显示逃亡者
            for (UUID targetId : GameManager.runners) {
                if (!targetId.equals(playerId)) { // 跳过自己
                    Player target = Bukkit.getPlayer(targetId);
                    if (target != null && target.isOnline()) {
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) skull.getItemMeta();
                        if (meta != null) {
                            meta.setOwningPlayer(target);
                            meta.setDisplayName("§a逃亡者: " + target.getName());

                            List<String> lore = new ArrayList<>();
                            lore.add("§7点击开始追踪此玩家");
                            lore.add("§a身份: §a逃亡者");
                            meta.setLore(lore);

                            skull.setItemMeta(meta);
                            gui.addItem(skull);
                        }
                    }
                }
            }
            player.openInventory(gui);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("§c投票选择游戏模式")) {
            event.setCancelled(true); // 防止玩家拿走物品
            if (event.getCurrentItem() != null) {
                Player player = (Player) event.getWhoClicked();
                UUID playerId = player.getUniqueId();
                // 检查玩家是否已经投票
                if(event.getSlot() != 47 && event.getSlot() != 49 && event.getSlot() != 51){
                    if (votedPlayers.contains(playerId)) {
                        player.sendMessage("§c你已经投过票了！");
                        return;
                    }
                    if (event.getSlot() == 11) {
                        player.sendMessage("§a你投票了要塞战争模式！");
                        fortressVotes++;
                        votedPlayers.add(playerId);
                    }

                    if (event.getSlot() == 13) {
                        player.sendMessage("§a你投票了原版猎人但是随机掉落模式！");
                        randomDropHunterVotes++;
                        votedPlayers.add(playerId);
                    }


                    if (event.getSlot() == 15) {
                        player.sendMessage("§a你投票了原版猎人模式！");
                        hunterVotes++;
                        votedPlayers.add(playerId);
                    }

                    if (event.getSlot() == 29) {
                        player.sendMessage("§a你投票了原版模式但是一个内鬼模式！");
                        ghastVotes++;
                        votedPlayers.add(playerId);
                    }

                    if (event.getSlot() == 31) {
                        player.sendMessage("§a你投票了一追多模式");
                        OnevsMany++;
                        votedPlayers.add(playerId);
                    }
                    if (event.getSlot() == 33) {
                        player.sendMessage("§a你投票了主播模式");
                        ZhuBo++;
                        votedPlayers.add(playerId);
                    }
                } else{
                    //偏好猎人
                    if(event.getSlot() == 49){
                        if(preferredHuntersCandidates.contains(playerId)){
                            return;
                        }
                        player.sendMessage("§a你将优先被分配为猎人");
                        preferredHuntersCandidates.add(playerId);
                    }
                    if (votedCommonBagPlayers.contains(playerId)) {
                        player.sendMessage("§c你已经投票过 启用/禁用 共享背包了！");
                        return;
                    }
                    //启用共享背包
                    if(event.getSlot() == 47){
                        player.sendMessage("§a你投票了启用共享背包！");
                        openBagVotes++;
                        votedCommonBagPlayers.add(playerId);
                    }
                    //禁用共享背包
                    if(event.getSlot() == 51){
                        player.sendMessage("§a你投票了禁用共享背包！");
                        closeBagVotes ++;
                        votedCommonBagPlayers.add(playerId);
                    }
                }
                player.closeInventory();
                updateAllPlayersGUI();
            }
        }
    }


    private void updateAllPlayersGUI() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTitle().equals("§c投票选择游戏模式")) {
                player.updateInventory();
            }
        }
    }

    // 分配角色方法
    public void assignRoles() {
        if(finalMode.equals("内鬼模式")){
            plugin.getLogger().info("开始分配角色...");

            // 分配角色前先清空之前的角色分配
            GameManager.runners.clear();
            GameManager.hunters.clear();
            GameManager.ghast = null;
            allReadyPlayers.clear();

            plugin.getLogger().info("已清空之前的角色分配。");

            // 添加所有准备玩家
            allReadyPlayers.addAll(GameManager.readyPlayers);

            // 打乱每个列表
            Collections.shuffle(allReadyPlayers);
            plugin.getLogger().info("已打乱玩家列表。");

            int totalPlayers = GameManager.readyPlayers.size();
            int targetRunnerCount = Math.max(1, (int)Math.round(totalPlayers / 4.0));
            plugin.getLogger().info("总玩家数：" + totalPlayers + "，目标逃亡者数：" + targetRunnerCount);

            // 随机选择逃亡者
            List<UUID> runners = new ArrayList<>();
            Random random = new Random();
            while (runners.size() < targetRunnerCount) {
                int randomIndex = random.nextInt(allReadyPlayers.size());
                UUID randomPlayer = allReadyPlayers.get(randomIndex);
                if (!runners.contains(randomPlayer)) {
                    runners.add(randomPlayer);
                }
            }
            GameManager.runners.addAll(runners);

            // 分配玩家
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (runners.contains(playerId)) {
                    plugin.getLogger().info(player.getName() + " 被随机分配为逃亡者。");
                } else {
                    GameManager.hunters.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被随机分配为猎人。");
                }
            }

            // 在猎人中随机选择一个卧底
            if (!GameManager.hunters.isEmpty()) {
                // 将Set转换为List
                List<UUID> huntersList = new ArrayList<>(GameManager.hunters);
                int ghastIndex = random.nextInt(huntersList.size());
                UUID ghastPlayerId = huntersList.get(ghastIndex);
                GameManager.ghast = ghastPlayerId;
                ghastName = Bukkit.getPlayer(ghastPlayerId).getName();
                ghastPlayer = Bukkit.getPlayer(ghastPlayerId);
            }
        }
//        if(finalMode.equals("一追多模式")){
//            // 分配角色前先清空之前的角色分配
//            GameManager.runners.clear();
//            GameManager.hunters.clear();
//            allReadyPlayers.clear();
//
//            plugin.getLogger().info("已清空之前的角色分配。");
//
//            // 添加所有准备玩家
//            allReadyPlayers.addAll(GameManager.readyPlayers);
//
//            // 打乱每个列表
//            Collections.shuffle(allReadyPlayers);
//            plugin.getLogger().info("已打乱玩家列表。");
//
//            int totalPlayers = GameManager.readyPlayers.size();
//            int targetHunterCount = Math.max(1, (int)Math.round(totalPlayers / 7.0));
//            plugin.getLogger().info("总玩家数：" + totalPlayers + "，目标猎人者数：" + targetHunterCount);
//
//            // 随机选择猎人
//            List<UUID> hunters = new ArrayList<>();
//            Random random = new Random();
//
//            // 首先检查是否有名为ShaoLiCheng的玩家
//            for (UUID playerId : allReadyPlayers) {
//                Player player = Bukkit.getPlayer(playerId);
//                if (player != null && player.getName().equals("SCYMciyue")) {
//                    hunters.add(playerId);
//                    plugin.getLogger().info(player.getName() + " 被强制分配为猎人。");
//                    break;
//                }
//            }
//
//            // 如果已经添加了ShaoLiCheng，减少需要随机选择的猎人数
//            int remainingHunters = targetHunterCount - hunters.size();
//            while (hunters.size() < targetHunterCount) {
//                int randomIndex = random.nextInt(allReadyPlayers.size());
//                UUID randomPlayer = allReadyPlayers.get(randomIndex);
//                Player player = Bukkit.getPlayer(randomPlayer);
//                // 确保不是ShaoLiCheng(已经处理过)且不重复添加
//                if (!hunters.contains(randomPlayer) && (player == null || !player.getName().equals("ShaoLiCheng"))) {
//                    hunters.add(randomPlayer);
//                }
//            }
//
//
//            // 将剩余玩家分配为逃亡者
//            List<UUID> runners = new ArrayList<>(allReadyPlayers);
//            runners.removeAll(hunters);
//            GameManager.runners.addAll(runners);
//
//            // 分配玩家
//            for (UUID playerId : allReadyPlayers) {
//                Player player = Bukkit.getPlayer(playerId);
//                if (hunters.contains(playerId)) {
//                    GameManager.hunters.add(player.getUniqueId());
//                    plugin.getLogger().info(player.getName() + " 被随机分配为猎人。");
//                } else {
//                    plugin.getLogger().info(player.getName() + " 被随机分配为逃亡者。");
//                }
//            }
//        }
        else if(finalMode.equals("一追多模式")){
            // 分配角色前先清空之前的角色分配
            GameManager.runners.clear();
            GameManager.hunters.clear();
            allReadyPlayers.clear();

            plugin.getLogger().info("已清空之前的角色分配。");

            // 添加所有准备玩家
            allReadyPlayers.addAll(GameManager.readyPlayers);

            // 打乱每个列表
            Collections.shuffle(allReadyPlayers);
            plugin.getLogger().info("已打乱玩家列表。");

            int totalPlayers = GameManager.readyPlayers.size();
            int targetHunterCount = Math.max(1, (int)Math.round(totalPlayers / 7.0));
            plugin.getLogger().info("总玩家数：" + totalPlayers + "，目标猎人者数：" + targetHunterCount);

            // 随机选择猎人
            List<UUID> hunters = new ArrayList<>();
            Random random = new Random();

            // 首先检查是否有名为SCYMciyue的玩家
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.getName().equals("shaolianhenshuai")) {
                    hunters.add(playerId);
                    plugin.getLogger().info(player.getName() + " 被强制分配为猎人。");
                    break;
                }
            }

            // 处理猎人优先候选者
            if (!preferredHuntersCandidates.isEmpty()) {
                int remainingHuntersAfterForced = targetHunterCount - hunters.size();
                int candidatesToAdd = Math.min(remainingHuntersAfterForced, preferredHuntersCandidates.size());

                for (UUID candidateId : preferredHuntersCandidates) {
                    if (hunters.size() >= targetHunterCount) break; // 已达到目标数量

                    if (allReadyPlayers.contains(candidateId) && !hunters.contains(candidateId)) {
                        hunters.add(candidateId);
                        Player player = Bukkit.getPlayer(candidateId);
                        if (player != null) {
                            plugin.getLogger().info(player.getName() + " 作为优先候选者被分配为猎人。");
                        }
                        candidatesToAdd--;
                    }

                    if (candidatesToAdd <= 0) break; // 已添加足够的优先候选者
                }
            }

            // 如果还需要更多猎人，从剩余玩家中随机选择
            while (hunters.size() < targetHunterCount) {
                int randomIndex = random.nextInt(allReadyPlayers.size());
                UUID randomPlayer = allReadyPlayers.get(randomIndex);
                Player player = Bukkit.getPlayer(randomPlayer);
                // 确保不是SCYMciyue(已经处理过)且不重复添加，并且不在优先候选者中
                if (!hunters.contains(randomPlayer) &&
                        (player == null || !player.getName().equals("SCYMciyue")) &&
                        !preferredHuntersCandidates.contains(randomPlayer)) {
                    hunters.add(randomPlayer);
                }
            }

            // 将剩余玩家分配为逃亡者
            List<UUID> runners = new ArrayList<>(allReadyPlayers);
            runners.removeAll(hunters);
            GameManager.runners.addAll(runners);

            // 分配玩家
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (hunters.contains(playerId)) {
                    GameManager.hunters.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被分配为猎人。");
                } else {
                    plugin.getLogger().info(player.getName() + " 被分配为逃亡者。");
                }
            }
        }
        else if(finalMode.equals("主播模式")){
            // 分配角色前先清空之前的角色分配
            GameManager.runners.clear();
            GameManager.hunters.clear();
            allReadyPlayers.clear();

            plugin.getLogger().info("已清空之前的角色分配。");

            // 添加所有准备玩家
            allReadyPlayers.addAll(GameManager.readyPlayers);

            // 检查zhuBoPlayer是否在线
            Player runner;
            if (zhuBoPlayer == null) {
                Collections.shuffle(allReadyPlayers);
                runner = Bukkit.getPlayer(allReadyPlayers.get(0));
            }

            // 非空(在线或者不在线)
            else {
                if (zhuBoPlayer.isOnline()){
                    runner = zhuBoPlayer;
                }
                else {
                    Collections.shuffle(allReadyPlayers);
                    runner = Bukkit.getPlayer(allReadyPlayers.get(0));
                }
            }


            // 分配所有玩家为猎人，除了选定的runner
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player.equals(runner)) {
                    GameManager.runners.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被分配为逃亡者。");
                } else {
                    GameManager.hunters.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被分配为猎人。");
                }
            }
        }

        else{
        // 分配角色前先清空之前的角色分配
        GameManager.runners.clear();
        GameManager.hunters.clear();
        allReadyPlayers.clear();

        plugin.getLogger().info("已清空之前的角色分配。");

        // 添加所有准备玩家
        allReadyPlayers.addAll(GameManager.readyPlayers);

        // 打乱每个列表
        Collections.shuffle(allReadyPlayers);
        plugin.getLogger().info("已打乱玩家列表。");

        int totalPlayers = GameManager.readyPlayers.size();
        int targetRunnerCount = Math.max(1, (int)Math.round(totalPlayers / 3.0));
        int targetHunterCount = totalPlayers - targetRunnerCount;
            plugin.getLogger().info("总玩家数：" + totalPlayers +
                    "，目标逃亡者数：" + targetRunnerCount +
                    "，目标猎人数：" + targetHunterCount);



            for (UUID playerId : allReadyPlayers) {
                // 如果所有玩家都不想成为猎人或者这个玩家不想成为猎人
                if (preferredHuntersCandidates == null || !preferredHuntersCandidates.contains(playerId)) {
                    //那就把这个
                    otherCandidates.add(playerId);
                }
            }

            // 为了在同等偏好的玩家中随机选择，再次打乱这两个候选列表
            Collections.shuffle(preferredHuntersCandidates);
            Collections.shuffle(otherCandidates);
            plugin.getLogger().info("希望成为猎人的玩家数: " + preferredHuntersCandidates.size());
            plugin.getLogger().info("其他候选玩家数: " + otherCandidates.size());
            // 步骤1: 分配猎人 (Hunters)
            // 优先从明确希望成为猎人的玩家中选择
            for (UUID hunterCandidate : preferredHuntersCandidates) {
                if (GameManager.hunters.size() < targetHunterCount) {
                    GameManager.hunters.add(hunterCandidate);
                } else {
                    break; // 猎人名额已满
                }
            }
            plugin.getLogger().info("从偏好者中分配后，猎人数: " + GameManager.hunters.size() + "/" + targetHunterCount);
            // 如果猎人名额未满，则从其他玩家（中立或不想当猎人者）中选择来填补空缺
            // 这些玩家将被分配为猎人以满足队伍比例要求
            if (GameManager.hunters.size() < targetHunterCount) {
                for (UUID otherCandidate : otherCandidates) {
                    // 确保此玩家尚未被分配 (虽然从不同列表取，此检查主要用于逻辑清晰)
                    // 并且此玩家也不是之前优先选择的猎人候选人（尽管他们不在preferredHuntersCandidates里）
                    if (!GameManager.hunters.contains(otherCandidate)) { // 避免意外重复添加
                        if (GameManager.hunters.size() < targetHunterCount) {
                            GameManager.hunters.add(otherCandidate);
                        } else {
                            break; // 猎人名额已满
                        }
                    }
                }
            }
            plugin.getLogger().info("补充中立/非偏好者后，猎人数: " + GameManager.hunters.size() + "/" + targetHunterCount);
// 步骤2: 分配逃亡者 (Runners)
// 所有未被分配为猎人的玩家都成为逃亡者
            for (UUID playerId : allReadyPlayers) {
                if (!GameManager.hunters.contains(playerId)) {
                    // 确保不会重复添加 (虽然理论上不太可能发生)
                    if (!GameManager.runners.contains(playerId)){
                        GameManager.runners.add(playerId);
                    }
                }
            }
            plugin.getLogger().info("分配逃亡者后，逃亡者数: " + GameManager.runners.size());
// 最终日志记录分配结果
            plugin.getLogger().info("角色分配完成。最终结果：");
            for (UUID runnerId : GameManager.runners) {
                Player player = Bukkit.getPlayer(runnerId);
                String name = (player != null && player.isOnline()) ? player.getName() : runnerId.toString() + " (数据可能陈旧或玩家离线)";
                plugin.getLogger().info(name + " 被分配为逃亡者。");
            }
            for (UUID hunterId : GameManager.hunters) {
                Player player = Bukkit.getPlayer(hunterId);
                String name = (player != null && player.isOnline()) ? player.getName() : hunterId.toString() + " (数据可能陈旧或玩家离线)";
                plugin.getLogger().info(name + " 被分配为猎人。");
            }
// (可选但推荐) 添加验证逻辑，确保分配符合预期
            if (totalPlayers > 0) { // 仅当有玩家参与时进行验证
                if (GameManager.runners.size() + GameManager.hunters.size() != totalPlayers) {
                    plugin.getLogger().warning("警告：分配的总玩家数 (" + (GameManager.runners.size() + GameManager.hunters.size()) +
                            ") 与准备的玩家数 (" + totalPlayers + ") 不符！");
                }
                if (GameManager.runners.isEmpty()) { // Math.max(1,...) 应该避免此情况
                    plugin.getLogger().severe("严重错误：有玩家参与但没有分配任何逃亡者！");
                }
                if (GameManager.runners.size() != targetRunnerCount) {
                    plugin.getLogger().warning("警告：实际逃亡者数量 (" + GameManager.runners.size() +
                            ") 与目标数量 (" + targetRunnerCount + ") 不符。这可能是由于猎人分配的极端情况造成的。");
                }
                if (GameManager.hunters.size() != targetHunterCount) {
                    plugin.getLogger().warning("警告：实际猎人数量 (" + GameManager.hunters.size() +
                            ") 与目标数量 (" + targetHunterCount + ") 不符。这可能是由于猎人分配的极端情况造成的。");
                }
        }

        // 打印日志以便调试
        plugin.getLogger().info(ChatColor.DARK_AQUA + " 逃亡者: " + GameManager.runners.size() +
                ", 猎人: " + GameManager.hunters.size());
    }
    }

    public void assignRoles2() {
        if(finalMode.equals("内鬼模式")){
            plugin.getLogger().info("开始分配角色...");

            // 分配角色前先清空之前的角色分配
            GameManager.runners.clear();
            GameManager.hunters.clear();
            GameManager.ghast = null;
            allReadyPlayers.clear();

            plugin.getLogger().info("已清空之前的角色分配。");

            // 添加所有准备玩家
            allReadyPlayers.addAll(GameManager.readyPlayers);

            // 打乱每个列表
            Collections.shuffle(allReadyPlayers);
            plugin.getLogger().info("已打乱玩家列表。");

            int totalPlayers = GameManager.readyPlayers.size();
            int targetRunnerCount = Math.max(1, (int)Math.round(totalPlayers / 4.0));
            plugin.getLogger().info("总玩家数：" + totalPlayers + "，目标逃亡者数：" + targetRunnerCount);

            // 随机选择逃亡者
            List<UUID> runners = new ArrayList<>();
            Random random = new Random();
            while (runners.size() < targetRunnerCount) {
                int randomIndex = random.nextInt(allReadyPlayers.size());
                UUID randomPlayer = allReadyPlayers.get(randomIndex);
                if (!runners.contains(randomPlayer)) {
                    runners.add(randomPlayer);
                }
            }
            GameManager.runners.addAll(runners);

            // 分配玩家
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (runners.contains(playerId)) {
                    plugin.getLogger().info(player.getName() + " 被随机分配为逃亡者。");
                } else {
                    GameManager.hunters.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被随机分配为猎人。");
                }
            }

            // 在猎人中随机选择一个卧底
            if (!GameManager.hunters.isEmpty()) {
                // 将Set转换为List
                List<UUID> huntersList = new ArrayList<>(GameManager.hunters);
                int ghastIndex = random.nextInt(huntersList.size());
                UUID ghastPlayerId = huntersList.get(ghastIndex);
                GameManager.ghast = ghastPlayerId;
                ghastName = Bukkit.getPlayer(ghastPlayerId).getName();
                ghastPlayer = Bukkit.getPlayer(ghastPlayerId);
            }
        }
        // ... (省略一追多模式的代码，保持不变)
        if(finalMode.equals("一追多模式")){
            // 分配角色前先清空之前的角色分配
            GameManager.runners.clear();
            GameManager.hunters.clear();
            allReadyPlayers.clear();

            plugin.getLogger().info("已清空之前的角色分配。");

            // 添加所有准备玩家
            allReadyPlayers.addAll(GameManager.readyPlayers);

            // 打乱每个列表
            Collections.shuffle(allReadyPlayers);
            plugin.getLogger().info("已打乱玩家列表。");

            int totalPlayers = GameManager.readyPlayers.size();
            int targetHunterCount = Math.max(1, (int)Math.round(totalPlayers / 7.0));
            plugin.getLogger().info("总玩家数：" + totalPlayers + "，目标猎人者数：" + targetHunterCount);

            // 随机选择猎人
            List<UUID> hunters = new ArrayList<>();
            Random random = new Random();

            // 首先检查是否有名为SCYMciyue的玩家
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.getName().equals("shaolianhenshuai")) {
                    hunters.add(playerId);
                    plugin.getLogger().info(player.getName() + " 被强制分配为猎人。");
                    break;
                }
            }

            // 处理猎人优先候选者
            if (!preferredHuntersCandidates.isEmpty()) {
                int remainingHuntersAfterForced = targetHunterCount - hunters.size();
                int candidatesToAdd = Math.min(remainingHuntersAfterForced, preferredHuntersCandidates.size());

                for (UUID candidateId : preferredHuntersCandidates) {
                    if (hunters.size() >= targetHunterCount) break; // 已达到目标数量

                    if (allReadyPlayers.contains(candidateId) && !hunters.contains(candidateId)) {
                        hunters.add(candidateId);
                        Player player = Bukkit.getPlayer(candidateId);
                        if (player != null) {
                            plugin.getLogger().info(player.getName() + " 作为优先候选者被分配为猎人。");
                        }
                        candidatesToAdd--;
                    }

                    if (candidatesToAdd <= 0) break; // 已添加足够的优先候选者
                }
            }

            // 如果还需要更多猎人，从剩余玩家中随机选择
            while (hunters.size() < targetHunterCount) {
                int randomIndex = random.nextInt(allReadyPlayers.size());
                UUID randomPlayer = allReadyPlayers.get(randomIndex);
                Player player = Bukkit.getPlayer(randomPlayer);
                // 确保不是SCYMciyue(已经处理过)且不重复添加，并且不在优先候选者中
                if (!hunters.contains(randomPlayer) &&
                        (player == null || !player.getName().equals("SCYMciyue")) &&
                        !preferredHuntersCandidates.contains(randomPlayer)) {
                    hunters.add(randomPlayer);
                }
            }

            // 将剩余玩家分配为逃亡者
            List<UUID> runners = new ArrayList<>(allReadyPlayers);
            runners.removeAll(hunters);
            GameManager.runners.addAll(runners);

            // 分配玩家
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (hunters.contains(playerId)) {
                    GameManager.hunters.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被分配为猎人。");
                } else {
                    plugin.getLogger().info(player.getName() + " 被分配为逃亡者。");
                }
            }
        }
        // ... (省略主播模式的代码，保持不变)
        if(finalMode.equals("主播模式")){
            // 分配角色前先清空之前的角色分配
            GameManager.runners.clear();
            GameManager.hunters.clear();
            allReadyPlayers.clear();

            plugin.getLogger().info("已清空之前的角色分配。");

            // 添加所有准备玩家
            allReadyPlayers.addAll(GameManager.readyPlayers);

            // 检查zhuBoPlayer是否在线
            Player runner;
            if (zhuBoPlayer == null) {
                Collections.shuffle(allReadyPlayers);
                runner = Bukkit.getPlayer(allReadyPlayers.get(0));
            }

            // 非空(在线或者不在线)
            else {
                if (zhuBoPlayer.isOnline()){
                    runner = zhuBoPlayer;
                }
                else {
                    Collections.shuffle(allReadyPlayers);
                    runner = Bukkit.getPlayer(allReadyPlayers.get(0));
                }
            }


            // 分配所有玩家为猎人，除了选定的runner
            for (UUID playerId : allReadyPlayers) {
                Player player = Bukkit.getPlayer(playerId);
                if (player.equals(runner)) {
                    GameManager.runners.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被分配为逃亡者。");
                } else {
                    GameManager.hunters.add(player.getUniqueId());
                    plugin.getLogger().info(player.getName() + " 被分配为猎人。");
                }
            }
        }

        else{
            // =========================================================
            // 🌟 其他/默认模式角色分配 (已修改：强制 ShaoLiCheng 为逃亡者)
            // =========================================================

            // 分配角色前先清空之前的角色分配
            GameManager.runners.clear();
            GameManager.hunters.clear();
            allReadyPlayers.clear();
            otherCandidates.clear(); // 确保清理

            plugin.getLogger().info("已清空之前的角色分配。");

            // 添加所有准备玩家
            allReadyPlayers.addAll(GameManager.readyPlayers);

            // 打乱每个列表
            Collections.shuffle(allReadyPlayers);
            plugin.getLogger().info("已打乱玩家列表。");

            int totalPlayers = GameManager.readyPlayers.size();
            int targetRunnerCount = Math.max(1, (int)Math.round(totalPlayers / 3.0));
            int targetHunterCount = totalPlayers - targetRunnerCount;
            plugin.getLogger().info("总玩家数：" + totalPlayers +
                    "，目标逃亡者数：" + targetRunnerCount +
                    "，目标猎人数：" + targetHunterCount);


            // -----------------------------------------------------
            // 🌟 核心修改 1: 强制分配 ShaoLiCheng 为逃亡者
            // -----------------------------------------------------
            UUID forcedRunnerId = null;
            String forcedRunnerName = "ShaoLiCheng";

            for (UUID playerId : new ArrayList<>(allReadyPlayers)) { // 遍历副本，允许安全移除
                Player player = Bukkit.getPlayer(playerId);
                if (player != null && player.getName().equalsIgnoreCase(forcedRunnerName)) {
                    forcedRunnerId = playerId;
                    break;
                }
            }

            if (forcedRunnerId != null) {
                // 1. 将其加入逃亡者列表
                GameManager.runners.add(forcedRunnerId);
                plugin.getLogger().info("【强制分配】玩家 " + forcedRunnerName + " 被分配为逃亡者。");

                // 2. 将其从所有待分配的列表中移除，防止重复分配
                allReadyPlayers.remove(forcedRunnerId);
                preferredHuntersCandidates.remove(forcedRunnerId);
                // otherCandidates 稍后会重新计算

                // 3. 调整目标逃亡者数量 (因为已经分配了一个)
                // 目标猎人数等于总人数减去已分配逃亡者人数
                targetHunterCount = totalPlayers - GameManager.runners.size();
                targetRunnerCount = targetHunterCount > 0 ? totalPlayers - targetHunterCount : 0;

                plugin.getLogger().info("【调整后】目标猎人数: " + targetHunterCount);
                plugin.getLogger().info("【调整后】需要随机分配的逃亡者名额数: " + Math.max(0, targetRunnerCount - 1));
            }
            // -----------------------------------------------------
            // 🌟 核心修改 1 结束
            // -----------------------------------------------------


            // 步骤 0: 重新生成候选者列表
            otherCandidates.clear();
            for (UUID playerId : allReadyPlayers) {
                // 玩家如果不在优先猎人候选者列表中，且不是被强制分配的逃亡者 (已被移除)，则属于其他候选者
                if (!preferredHuntersCandidates.contains(playerId)) {
                    otherCandidates.add(playerId);
                }
            }


            // 为了在同等偏好的玩家中随机选择，再次打乱这两个候选列表
            Collections.shuffle(preferredHuntersCandidates);
            Collections.shuffle(otherCandidates);
            plugin.getLogger().info("希望成为猎人的玩家数: " + preferredHuntersCandidates.size());
            plugin.getLogger().info("其他候选玩家数: " + otherCandidates.size());

            // 步骤1: 分配猎人 (Hunters)
            // 优先从明确希望成为猎人的玩家中选择
            for (UUID hunterCandidate : preferredHuntersCandidates) {
                if (GameManager.hunters.size() < targetHunterCount) {
                    GameManager.hunters.add(hunterCandidate);
                } else {
                    break; // 猎人名额已满
                }
            }
            plugin.getLogger().info("从偏好者中分配后，猎人数: " + GameManager.hunters.size() + "/" + targetHunterCount);

            // 如果猎人名额未满，则从其他玩家中选择来填补空缺
            if (GameManager.hunters.size() < targetHunterCount) {
                for (UUID otherCandidate : otherCandidates) {
                    if (!GameManager.hunters.contains(otherCandidate)) {
                        if (GameManager.hunters.size() < targetHunterCount) {
                            GameManager.hunters.add(otherCandidate);
                        } else {
                            break; // 猎人名额已满
                        }
                    }
                }
            }
            plugin.getLogger().info("补充中立/非偏好者后，猎人数: " + GameManager.hunters.size() + "/" + targetHunterCount);

            // 步骤2: 分配逃亡者 (Runners)
            // 剩余所有未被分配为猎人的玩家都成为逃亡者
            for (UUID playerId : allReadyPlayers) {
                if (!GameManager.hunters.contains(playerId)) {
                    if (!GameManager.runners.contains(playerId)){
                        GameManager.runners.add(playerId);
                    }
                }
            }
            plugin.getLogger().info("分配逃亡者后，逃亡者数: " + GameManager.runners.size());


            // 最终日志记录分配结果
            plugin.getLogger().info("角色分配完成。最终结果：");
            for (UUID runnerId : GameManager.runners) {
                Player player = Bukkit.getPlayer(runnerId);
                String name = (player != null && player.isOnline()) ? player.getName() : runnerId.toString() + " (数据可能陈旧或玩家离线)";
                plugin.getLogger().info(name + " 被分配为逃亡者。");
            }
            for (UUID hunterId : GameManager.hunters) {
                Player player = Bukkit.getPlayer(hunterId);
                String name = (player != null && player.isOnline()) ? player.getName() : hunterId.toString() + " (数据可能陈旧或玩家离线)";
                plugin.getLogger().info(name + " 被分配为猎人。");
            }

            // (可选但推荐) 添加验证逻辑，确保分配符合预期
            if (totalPlayers > 0) { // 仅当有玩家参与时进行验证
                if (GameManager.runners.size() + GameManager.hunters.size() != totalPlayers) {
                    plugin.getLogger().warning("警告：分配的总玩家数 (" + (GameManager.runners.size() + GameManager.hunters.size()) +
                            ") 与准备的玩家数 (" + totalPlayers + ") 不符！");
                }
                if (GameManager.runners.isEmpty()) { // Math.max(1,...) 应该避免此情况
                    plugin.getLogger().severe("严重错误：有玩家参与但没有分配任何逃亡者！");
                }
                if (GameManager.runners.size() != totalPlayers - targetHunterCount) {
                    plugin.getLogger().warning("警告：实际逃亡者数量 (" + GameManager.runners.size() +
                            ") 与目标数量 (" + (totalPlayers - targetHunterCount) + ") 不符。这可能是由于猎人分配的极端情况造成的。");
                }
                if (GameManager.hunters.size() != targetHunterCount) {
                    plugin.getLogger().warning("警告：实际猎人数量 (" + GameManager.hunters.size() +
                            ") 与目标数量 (" + targetHunterCount + ") 不符。这可能是由于猎人分配的极端情况造成的。");
                }
            }

            // 打印日志以便调试
            plugin.getLogger().info(ChatColor.DARK_AQUA + " 逃亡者: " + GameManager.runners.size() +
                    ", 猎人: " + GameManager.hunters.size());
        }
    }







    public void openChooseModeGUI(Player player) {
            Inventory gui = Bukkit.createInventory(null, 54, "§c投票选择游戏模式");

            // 创建水桶
            ItemStack water = new ItemStack(Material.WATER_BUCKET);
            ItemMeta meta = water.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§e要塞战争");
               // meta.setLore(Arrays.asList("投票选择要塞战争模式"));
                List<String> waterLore = new ArrayList<>();
                waterLore.add("投票选择要塞战争模式");
                waterLore.add("§7当前票数: " + fortressVotes);
                meta.setLore(waterLore);
                meta.setUnbreakable(true);
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
                water.setItemMeta(meta);
            }

            // 创建岩浆桶
            ItemStack lava = new ItemStack(Material.LAVA_BUCKET);
            ItemMeta meta2 = lava.getItemMeta();
            if (meta2 != null) {
                meta2.setDisplayName("§c原版猎人");
               // meta2.setLore(Arrays.asList("投票选择原版猎人模式"));
                List<String> lavaLore = new ArrayList<>();
                lavaLore.add("投票选择原版猎人模式");
                lavaLore.add("§7当前票数: " + hunterVotes);
                meta2.setLore(lavaLore);
                meta2.setUnbreakable(true);
                meta2.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta2.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
                lava.setItemMeta(meta2);
            }

            //创建一朵花
            ItemStack flower = new ItemStack(Material.POPPY);
            ItemMeta meta3 = flower.getItemMeta();
            if (meta3 != null) {
                meta3.setDisplayName("§c原版猎人但是随机掉落");
                List<String> flowerLore = new ArrayList<>();
                flowerLore.add("投票选择原版猎人但是随机掉落模式");
                flowerLore.add("§7当前票数: " + randomDropHunterVotes);
                meta3.setLore(flowerLore);
                meta3.setUnbreakable(true);
                meta3.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta3.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
                flower.setItemMeta(meta3);
            }

            //创建避雷针
            ItemStack pufferfish = new ItemStack(Material.PUFFERFISH);
            ItemMeta meta4 = pufferfish.getItemMeta();
                if (meta4 != null) {
                    meta4.setDisplayName("§c原版猎人但是内鬼模式");
                    List<String> lightningLore = new ArrayList<>();
                    lightningLore.add("投票选择内鬼模式");
                    lightningLore.add("§7当前票数: " + ghastVotes);
                    lightningLore.add("玩法说明:逃亡者和猎人的比例改为接近1:3");
                    lightningLore.add("但是猎人中会有一个人是内鬼");
                    lightningLore.add("内鬼需要帮助逃亡者获得游戏的胜利");
                    lightningLore.add("逃亡者者全部阵亡后内鬼会编程逃亡者");
                    lightningLore.add("玩家通过/vote来进行投票淘汰内鬼！");
                    lightningLore.add("当有玩家投票后，如果某个猎人被投票的票数大于一半的猎人数量，这个猎人会被淘汰");
                    lightningLore.add("当所有猎人都投票后，票数最多的某个猎人会被淘汰");
                    meta4.setLore(lightningLore);
                    meta4.setUnbreakable(true);
                    meta4.addEnchant(Enchantment.UNBREAKING, 1, true);
                    meta4.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
                    pufferfish.setItemMeta(meta4);
            }

                //创建鞘翅
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta meta5 = elytra.getItemMeta();
        if (meta5 != null) {
            meta5.setDisplayName("§c猎人一追多模式");
            List<String> swordLore = new ArrayList<>();
            swordLore.add("投票选择猎人一追多模式");
            swordLore.add("§7当前票数: " + OnevsMany);
            swordLore.add("玩法说明:逃生者:猎人最大程度接近7:1");
            swordLore.add("猎人有除头盔外全套合金盔甲");
            swordLore.add("猎人将在游戏开始10min后获得鞘翅");
            swordLore.add("猎人死亡不掉落");
            swordLore.add("猎人每击杀一名逃亡者就会增加该猎人一颗心的生命上限");
            swordLore.add("逃生者需要击杀末影龙获得游戏胜利");
            meta5.setLore(swordLore);
            meta5.setUnbreakable(true);// 隐藏附魔、不可破坏、属性修饰等信息
            meta5.addItemFlags(
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ATTRIBUTES, // 这个 flag 负责隐藏伤害和攻速
                    ItemFlag.HIDE_DESTROYS,
                    ItemFlag.HIDE_PLACED_ON
            );

            meta5.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta5.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            elytra.setItemMeta(meta5);
        }

        //创建时钟
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta_clock = clock.getItemMeta();
        if (meta_clock != null) {
            meta_clock.setDisplayName("§c主播模式");
            List<String> swordLore = new ArrayList<>();
            swordLore.add("投票选择主播模式");
            swordLore.add("§7当前票数: " + ZhuBo);
            swordLore.add("玩法说明:只有一位逃生者");
            swordLore.add("玩法说明:输入/runner成为dream");
            swordLore.add("玩法说明:输入/unrunner退出dream");
            swordLore.add("玩法说明:输入/hushenfu获得新的护身符");
            swordLore.add("逃生者除了满饱和外无任何额外增强");
            swordLore.add("共享背包和tpa被关闭");
            swordLore.add("逃亡者有显示所有猎人跟自己距离的护身符");
            meta_clock.setLore(swordLore);
            meta_clock.setUnbreakable(true);// 隐藏附魔、不可破坏、属性修饰等信息
            meta_clock.addItemFlags(
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_UNBREAKABLE,
                    ItemFlag.HIDE_ATTRIBUTES, // 这个 flag 负责隐藏伤害和攻速
                    ItemFlag.HIDE_DESTROYS,
                    ItemFlag.HIDE_PLACED_ON
            );

            meta_clock.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta_clock.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            clock.setItemMeta(meta_clock);
        }

        //创建水晶
        ItemStack end_crystal = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta6 = end_crystal.getItemMeta();
        if (meta6 != null) {
            meta6.setDisplayName("§c点击优先分配为猎人");
            List<String> end_crystalLore = new ArrayList<>();
            end_crystalLore.add("你将会被优先被分配到猎人阵营");
            end_crystalLore.add("ps:只适用于原版猎人,一追多,要塞猎人和随机掉落");
            meta6.setLore(end_crystalLore);
            meta6.setUnbreakable(true);
            meta6.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta6.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            end_crystal.setItemMeta(meta6);
        }

        //创建木箱
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta7 = chest.getItemMeta();
        if (meta7 != null) {
            meta7.setDisplayName("§c启用共享背包");
            List<String> lore = new ArrayList<>();
            lore.add("投票启用共享背包");
            lore.add("§7当前票数: " + openBagVotes);
            meta7.setLore(lore);
            meta7.setUnbreakable(true);
            meta7.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta7.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            chest.setItemMeta(meta7);
        }

        //创建末影箱
        ItemStack end_chest = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta8 = end_chest.getItemMeta();
        if (meta8 != null) {
            meta8.setDisplayName("§c禁用共享背包");
            List<String> lore = new ArrayList<>();
            lore.add("投票禁用共享背包");
            lore.add("§7当前票数: " + closeBagVotes);
            meta8.setLore(lore);
            meta8.setUnbreakable(true);
            meta8.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta8.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
            end_chest.setItemMeta(meta8);
        }



            // 将物品放置在GUI中
            gui.setItem(11, water);
            gui.setItem(13, flower);
            gui.setItem(15, lava);
            gui.setItem(29, pufferfish);
            gui.setItem(31, elytra);
            gui.setItem(33, clock);
            gui.setItem(49, end_crystal);
            gui.setItem(47, chest);
            gui.setItem(51, end_chest);

            // 打开GUI给玩家
            player.openInventory(gui);
        }




    public String decideFinalGameMode() {
        Map<String, Integer> voteMap = new HashMap<>();
        voteMap.put("要塞战争", fortressVotes);
        voteMap.put("原版猎人", hunterVotes);
        voteMap.put("随机掉落原版猎人", randomDropHunterVotes);
        voteMap.put("内鬼模式", ghastVotes);
        voteMap.put("一追多模式", OnevsMany);
        voteMap.put("主播模式", ZhuBo);

        // 找到最高票数
        int maxVotes = Collections.max(voteMap.values());

        // 收集所有获得最高票数的模式
        List<String> topModes = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : voteMap.entrySet()) {
            if (entry.getValue() == maxVotes) {
                topModes.add(entry.getKey());
            }
        }

        // 如果只有一个最高票数模式，直接返回
        if (topModes.size() == 1) {
            finalMode = topModes.get(0);
            return finalMode;
        }

        // 如果有多个最高票数模式，随机选择一个
        Random random = new Random();
        finalMode = topModes.get(random.nextInt(topModes.size()));
        return finalMode;

    }

    public static String decideOpenORCloseBag() {
        if(openBagVotes > closeBagVotes) {
            return "open";
        }
        else {
            return "close";
        }
    }














}
