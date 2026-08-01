package de.andrechan2008.damagelogSystemGM;

import de.andrechan2008.damagelogSystemGM.builder.DamagelogBuilder;
import de.andrechan2008.damagelogSystemGM.cmds.DamageLogCommand;
import de.andrechan2008.damagelogSystemGM.listener.PlayerHitListener;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class DamagelogSystemGM extends JavaPlugin {

    @Getter
    public static DamagelogSystemGM instance;

    @Override
    public void onEnable() {
        instance = this;
        getCommand("damagelog").setExecutor(new DamageLogCommand());
        Bukkit.getPluginManager().registerEvents(new PlayerHitListener(), this);

        Bukkit.getScheduler().runTaskTimer(this, DamagelogBuilder::removeExpiredHits, 0L, 100L);
    }
}
