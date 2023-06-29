package org.vertexstudios.booter.bukkit;

import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.java.JavaPlugin;

public class VertexBooter extends JavaPlugin {

    @Override
    public void onLoad() {
        for(String license : this.getConfig().getStringList("licenses")) {

        }
    }
}
