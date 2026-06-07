package quest.yuzhou.rcchest;

import net.Indyuce.mmoitems.MMOItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class LootManager {

    private record LootItem(String itemName, String itemType, double unidentifyChance, int rarity, int minAmount, int maxAmount) {}

    private final FileConfiguration config;
    private HashMap<String, List<LootItem>> lootTable = new HashMap<>();
    private final HashMap<String, String> information = new HashMap<>();
    private final Random random = new Random();

    public LootManager(FileConfiguration config) {
        this.config = config;
        loadLootTable();
        loadInformation();
    }

    // 加载配置中的物品和稀有度
    public void loadLootTable() {
        HashMap<String, List<LootItem>> result = new HashMap<>();
        for (String key : config.getConfigurationSection("chest-types").getKeys(false)) {
            List<Map<?, ?>> configItems = config.getConfigurationSection("chest-types." + key).getMapList("items");
            List<LootItem> items = new ArrayList<>();
            for (Map<?, ?> itemData : configItems) {
                String itemName = (String) itemData.get("name");
                String itemType = (String) itemData.get("type");
                double unidentifyChance = ((Number) itemData.get("unidentify-chance")).doubleValue();
                int rarity = (int) itemData.get("rarity");
                int minAmount = (int) itemData.get("min-amount");
                int maxAmount = (int) itemData.get("max-amount");

                items.add(new LootItem(itemName, itemType, unidentifyChance, rarity, minAmount, maxAmount));
            }
            result.put(key, items);
        }
        lootTable = result;
    }

    // 玩家打开箱子时获取随机物品
    public void giveRandomLoot(Player player, String type) {
        List<LootItem> rewards = getRandomItems(type);
        for (LootItem lootItem : rewards) {
            String commandLine = "mi give " + lootItem.itemType() + " " + lootItem.itemName() + " " + player.getName() + " " + lootItem.minAmount() + "-" + lootItem.maxAmount() + " " + lootItem.unidentifyChance() + " 100 0 silent";
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
        }
    }

    // 按权重随机选择物品
    private List<LootItem> getRandomItems(String type) {
        List<LootItem> rewards = new ArrayList<>();
        List<LootItem> table = new ArrayList<>(lootTable.get(type)); // 创建一个副本，避免修改原列表
        int itemMinCount = config.getInt("chest-types." + type + ".item-min-count");
        int itemMaxCount = config.getInt("chest-types." + type + ".item-max-count");
        int numberOfItems = random.nextInt(itemMaxCount - itemMinCount + 1) + itemMinCount;

        for (int i = 0; i < numberOfItems && !table.isEmpty(); i++) {
            int totalWeight = table.stream().mapToInt(LootItem::rarity).sum();
            int randomValue = random.nextInt(totalWeight);

            int currentWeight = 0;
            for (Iterator<LootItem> iterator = table.iterator(); iterator.hasNext(); ) {
                LootItem lootItem = iterator.next();
                currentWeight += lootItem.rarity();
                if (randomValue < currentWeight) {
                    rewards.add(lootItem);
                    iterator.remove(); // 移除已选中的物品，防止重复
                    break;
                }
            }
        }

        return rewards;
    }

    private void loadInformation() {
        for (String key : config.getConfigurationSection("chest-types").getKeys(false)) {
            String name = config.getString("chest-types." + key + ".name");
            List<Map<?, ?>> configItems = config.getConfigurationSection("chest-types." + key).getMapList("items");
            List<String> itemNames = new ArrayList<>();
            for (Map<?, ?> itemData : configItems) {
                String itemID = (String) itemData.get("name");
                String itemType = (String) itemData.get("type");
                itemNames.add(MMOItems.plugin.getItem(itemType, itemID).getItemMeta().getDisplayName());
            }
            information.put(key, ChatColor.YELLOW + "這是一個 " + ChatColor.translateAlternateColorCodes('&', name != null ? name : "錯誤") + ChatColor.YELLOW + " 。可能會開出的材料有：" + String.join(ChatColor.YELLOW + " ， ", itemNames));
        }
    }

    public HashMap<String, String> getInformation() {
        return information;
    }
}
