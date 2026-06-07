package quest.yuzhou.rcchest;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class RCChest extends JavaPlugin {

    private ChestAndCommandManager chestAndCommandManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        getLogger().info("Enabling RCChest...");
        chestAndCommandManager = new ChestAndCommandManager(this, new LootManager(getConfig()));
        Bukkit.getPluginManager().registerEvents(chestAndCommandManager, this);
        getCommand("reloadchestconfig").setExecutor(chestAndCommandManager);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        this.getLogger().info("Bye!");
    }

    public ChestAndCommandManager getChestAndCommandManager() {
        return chestAndCommandManager;
    }
}
