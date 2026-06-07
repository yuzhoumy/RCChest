package quest.yuzhou.rcchest;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class ChestAndCommandManager implements Listener, CommandExecutor {

    private final RCChest plugin;
    private FileConfiguration fileConfiguration;
    private final LootManager lootManager;
    private final int exp;
    private List<Block> blockAboveList;
    public String prefix;

    public ChestAndCommandManager(RCChest plugin, LootManager lootManager) {
        this.plugin = plugin;
        this.fileConfiguration = plugin.getConfig();
        this.lootManager = lootManager;
        this.exp = fileConfiguration.getInt("exp");
        this.blockAboveList = new ArrayList<>();
        this.prefix = ChatColor.translateAlternateColorCodes('&', fileConfiguration.getString("prefix"));
    }

    @EventHandler
    public void onPlayerOpenChest(PlayerInteractEvent event) {

        if (!event.hasBlock()) return;

        Block block = event.getClickedBlock();
        World world = Bukkit.getWorld(fileConfiguration.getString("world"));

        if (!(block.getType() == Material.CHEST)) return;
        if (!(block.getLocation().getWorld() == world)) return;

        Chest chest = (Chest) block.getState();

        if (chest.getCustomName() == null) return;

        for (String chestType : fileConfiguration.getConfigurationSection("chest-types").getKeys(false)) {
            if (chest.getCustomName().equalsIgnoreCase(chestType)) {

                Player player = event.getPlayer();

                if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                    Block blockAbove = block.getRelative(BlockFace.UP);
                    Material blockAboveMaterial = Material.valueOf(fileConfiguration.getString("chest-types." + chestType + ".block-above-material"));
                    event.setCancelled(true);

                    if (blockAbove.getType() == blockAboveMaterial) {
                        player.sendMessage(prefix + ChatColor.RED + "這個箱子已經被人掠奪過了！");
                        player.sendMessage(prefix + ChatColor.WHITE + ChatColor.ITALIC + "若要查看該箱子可能會開出的物品，請點擊左鍵。");
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 2, 2);
                        return;
                    }

                    blockAbove.setType(blockAboveMaterial);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 2, 1);
                    player.sendMessage(prefix + ChatColor.AQUA + "你找到了一個資源箱！你必須待在此箱子距離 " + fileConfiguration.getInt("distance") + " 格之内，持續 " + fileConfiguration.getInt("wait-time") + " 秒，才能打開箱子");
                    player.sendMessage(prefix + ChatColor.WHITE + ChatColor.ITALIC + "若要查看該箱子可能會開出的物品，請點擊左鍵。");
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.getLocation().distance(block.getLocation()) > fileConfiguration.getInt("distance")) {
                                blockAbove.setType(Material.AIR);
                                player.sendMessage(prefix + ChatColor.RED + "你已離開寶箱 " + fileConfiguration.getInt("distance") + " 遠之外。");
                            } else {
                                lootManager.giveRandomLoot(player, chestType);
                                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2, 2);
                                player.sendMessage(prefix + ChatColor.GREEN + "成功開啓箱子！");
                                player.giveExp(exp);
                                blockAboveList.add(blockAbove);
                            }
                        }
                    }.runTaskLater(plugin, (long) fileConfiguration.getInt("wait-time") * 20);
                    return;
                } else if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                    player.sendMessage(prefix + lootManager.getInformation().get(chestType));
                }
            }
        }
    }

    /**
     * clear all block above chest to unlock all chests
     * @return affected blocks
     */
    public int clearBlockAbove() {
        for (Block block : blockAboveList) {
            block.setType(Material.AIR);
        }
        int affected = blockAboveList.size();
        blockAboveList.clear();
        return affected;
    }

    public List<Block> getBlockAboveList() {
        return blockAboveList;
    }

    public void setBlockAboveList(List<Block> blockAboveList) {
        this.blockAboveList = blockAboveList;
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (command.getName().equalsIgnoreCase("reloadchestconfig")) {
            plugin.reloadConfig();
            this.fileConfiguration = plugin.getConfig();
            lootManager.loadLootTable();
            commandSender.sendMessage("RCChest configuration reloaded successfully.");
        }
        return true;
    }
}
