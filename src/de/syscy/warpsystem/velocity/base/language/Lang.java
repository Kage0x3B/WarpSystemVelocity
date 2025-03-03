package de.syscy.warpsystem.velocity.base.language;

import de.codingair.codingapi.bungeecord.files.ConfigFile;
import de.syscy.warpsystem.velocity.base.WarpSystem;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Lang {
    public static void initPreDefinedLanguages(WarpSystem plugin) throws IOException {
        List<String> languages = new ArrayList<>();
        languages.add("ENG.yml");
        languages.add("GER.yml");

        File folder = new File(plugin.getDataFolder(), "/Languages/");
        if(!folder.exists()) mkDir(folder);

        for(String language : languages) {
            InputStream is = plugin.getResourceAsStream("languages/" + language);

            File file = new File(plugin.getDataFolder() + "/Languages/", language);
            if(!file.exists()) {
                file.createNewFile();
                copy(is, new FileOutputStream(file));
            }
        }
    }

    private static void mkDir(File file) {
        if(!file.getParentFile().exists()) mkDir(file.getParentFile());
        if(!file.exists()) {
            try {
                file.mkdir();
            } catch(SecurityException ex) {
                throw new IllegalArgumentException("Plugin is not permitted to create a folder!");
            }
        }
    }

    private static long copy(InputStream from, OutputStream to) throws IOException {
        if(from == null) return -1;
        if(to == null) throw new NullPointerException();

        byte[] buf = new byte[4096];
        long total = 0L;

        while(true) {
            int r = from.read(buf);
            if(r == -1) {
                return total;
            }

            to.write(buf, 0, r);
            total += (long) r;
        }
    }

    public static String getCurrentLanguage() {
        return getConfig().getString("WarpSystem.Language", "ENG");
    }

    public static void setCurrentLanguage(String lang) {
        save(() -> getConfig().set("WarpSystem.Language", lang.toUpperCase()));
    }

    public static String getPrefix() {
        return get("Prefix");
    }

    public static String get(String key) {
        String text = getLanguageFile(getCurrentLanguage()).getString(key);

        if(text == null) {
            if(key.equalsIgnoreCase("Yes") && get("true") != null) {
                String s = get("true");
                return s.equalsIgnoreCase("true") ? "Yes" : s;
            } else if(key.equalsIgnoreCase("No") && get("false") != null) {
                String s = get("false");
                return s.equalsIgnoreCase("false") ? "No" : s;
            }

            throw new IllegalStateException("Unknown translation key: '" + key + "' >> Check " + getCurrentLanguage() + ".yml at '" + key + "'");
        }

        text = ChatColor.translateAlternateColorCodes('&', text);
        return text;
    }

    private static Configuration getConfig() {
        try {
            ConfigFile file = WarpSystem.getInstance().getFileManager().getFile("Config");
            return file.getConfig();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Configuration getLanguageFile(String langTag) {
        try {
            ConfigFile file = WarpSystem.getInstance().getFileManager().getFile(langTag);
            if(file == null) {
                WarpSystem.getInstance().getFileManager().loadFile(langTag, "/Languages/", "languages/");
                return getLanguageFile(langTag);
            }
            return file.getConfig();
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void save(Runnable task) {
        ConfigFile file = WarpSystem.getInstance().getFileManager().getFile("Language");
        try {
            file.load();
            task.run();
            file.save();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
