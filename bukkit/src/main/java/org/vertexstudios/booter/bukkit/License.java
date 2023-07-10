package org.vertexstudios.booter.bukkit;

import java.util.Arrays;

import lombok.Builder;
import lombok.Getter;

@Builder
public class License {
    @Getter private String id;
    @Getter private String pluginId;
    @Getter private String build;
    @Getter private String[] dependencies;
    @Getter private String key;
    @Getter private byte[] bytes;

    public boolean dependsOn(License license) {
        return Arrays.asList(dependencies).contains(license.pluginId);
    }
}
