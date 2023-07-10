package org.vertexstudios.booter.bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
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

    private static Queue<License> bootPipeline;
    private static List<License> licenses;

    @Override
    public void onLoad() {

        licenses = new ArrayList<>();

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

                String[] dependencies = new String[((ArrayList<String>) data.get("dependencies")).size()];
                dependencies = ((ArrayList<String>) data.get("dependencies")).toArray(dependencies);

                licenses.add(
                    License.builder().build(build).bytes(response.getBody()).id(licenseId)
                    .dependencies(dependencies).key((String) data.get("private_key"))
                    .build()
                );

            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        this.getLogger().log(Level.INFO, licenses.size() + " licenses found. They'll be loaded in the end of loading process.");
    }

    @Override
    public void onEnable() {
        this.sort();
        this.loadPlugins();
    }

    public void sort() {
        for(int i = 0; i<licenses.size(); i++) {
            for(int j = 0; j < licenses.size()-1; j++) {
                License first = licenses.get(i);
                License second = licenses.get(j);
                if(first.dependsOn(second) && second.dependsOn(first)) {
                    this.getLogger().log(Level.SEVERE, "Circular dependency found between licenses " + first.getId() + " and " + second.getId() + ". Shutting down server.");
                    Bukkit.shutdown();
                    return;
                }
                if(first.dependsOn(second)) {
                    licenses.set(j, second);
                    licenses.set(j + 1, first);
                }
            }
        }
        bootPipeline.addAll(licenses);
    }

    public void loadPlugins() {
        while(!bootPipeline.isEmpty()) {
            License license = bootPipeline.poll();
            try {
                Path temp = Files.createTempFile(UUID.randomUUID().toString(), ".jar");
                Files.write(temp, license.getBytes());

                Plugin plugin = Bukkit.getPluginManager().loadPlugin(temp.toFile());
                this.getLogger().log(Level.INFO, "Successfully loaded the plugin " + plugin.getName() + " with VertexBooter");

            } catch (InvalidPluginException ex) {
                this.getLogger().log(Level.SEVERE, "The license " + license.getId() + " contains a invalid plugin. Skipping.");
                continue;
            } catch (InvalidDescriptionException ex) {
                this.getLogger().log(Level.SEVERE, "The license " + license.getId() + " contains a invalid description. Skipping.");
                continue;
            }catch(UnknownDependencyException ex){
                this.getLogger().log(Level.SEVERE, "The license " + license.getId() + " contains an unknown dependency that may not exists or not have been loaded yet.");
                continue;
            }catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}
