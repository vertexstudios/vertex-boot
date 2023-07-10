package org.vertexstudios.booter.bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.Yaml;

import com.google.common.base.Charsets;

public class VertexBooter extends JavaPlugin {

    @Override
    public void onLoad() {

        File licensesFolder = new File(this.getDataFolder(), "licenses");
        try {
            if(!licensesFolder.exists()) {
                licensesFolder.mkdir();
            }
        } catch (Exception ex) {
            this.getLogger().log(Level.SEVERE, "Couldn't create licenses folder.");
            Bukkit.shutdown();
        }

        for(File license : licensesFolder.listFiles((f) -> f.getName().endsWith(".yml"))) {

            String licenseId = license.getName().split(".")[0];

            try {
                
                InputStream inputStream = new FileInputStream(license);
                Yaml yaml = new Yaml();
                Map<String, Object> data = yaml.load(inputStream);

                String build = data.get("build").toString();
                byte[] privateKey = data.get("privateKey").toString().getBytes(Charsets.UTF_8);


                Axis.Response response = Axis.fetch(licenseId, build, privateKey);
                
                Path temp = Files.createTempFile(UUID.randomUUID().toString(), ".jar");
                Files.write(temp, response.getBody());

                Plugin plugin = Bukkit.getPluginManager().loadPlugin(temp.toFile());
                this.getLogger().log(Level.INFO, "Successfully loaded the plugin " + plugin.getName() + " with VertexBooter");
            } catch (InvalidPluginException ex) {
                this.getLogger().log(Level.SEVERE, "The license " + licenseId + " contains a invalid plugin. Skipping.");
                continue;
            } catch (InvalidDescriptionException ex) {
                this.getLogger().log(Level.SEVERE, "The license " + licenseId + " contains a invalid description. Skipping.");
                continue;
            }catch(UnknownDependencyException ex){
                this.getLogger().log(Level.SEVERE, "The license " + license + " contains an unknown dependency that may not exists or not have been loaded yet.");
                continue;
            }catch (IOException ex) {
                ex.printStackTrace();
            }

        }
    }

}
