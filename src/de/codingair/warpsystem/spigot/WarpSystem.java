package de.codingair.warpsystem.spigot;

import de.codingair.codingapi.API;
import de.codingair.codingapi.bungeecord.BungeeCordHelper;
import de.codingair.codingapi.files.ConfigFile;
import de.codingair.codingapi.files.FileManager;
import de.codingair.codingapi.server.Version;
import de.codingair.codingapi.server.commands.CommandBuilder;
import de.codingair.codingapi.server.fancymessages.FancyMessage;
import de.codingair.codingapi.server.fancymessages.MessageTypes;
import de.codingair.codingapi.time.TimeFetcher;
import de.codingair.codingapi.time.Timer;
import de.codingair.codingapi.tools.Callback;
import de.codingair.warpsystem.spigot.commands.*;
import de.codingair.warpsystem.spigot.features.signs.SignListener;
import de.codingair.warpsystem.spigot.language.Lang;
import de.codingair.warpsystem.spigot.listeners.NotifyListener;
import de.codingair.warpsystem.spigot.listeners.PortalListener;
import de.codingair.warpsystem.spigot.listeners.TeleportListener;
import de.codingair.warpsystem.spigot.managers.GlobalWarpManager;
import de.codingair.warpsystem.spigot.managers.IconManager;
import de.codingair.warpsystem.spigot.managers.TeleportManager;
import de.codingair.warpsystem.spigot.utils.UpdateChecker;
import de.codingair.warpsystem.transfer.spigot.SpigotDataHandler;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;

public class WarpSystem extends JavaPlugin {
    public static final String PERMISSION_NOTIFY = "WarpSystem.Notify";
    public static final String PERMISSION_MODIFY = "WarpSystem.Modify";
    public static final String PERMISSION_MODIFY_ICONS = "WarpSystem.Modify.Icons";
    public static final String PERMISSION_MODIFY_GLOBAL_WARPS = "WarpSystem.Modify.GlobalWarps";
    public static final String PERMISSION_MODIFY_PORTALS = "WarpSystem.Modify.Portals";
    public static final String PERMISSION_USE = "WarpSystem.Use";
    public static final String PERMISSION_ByPass_Maintenance = "WarpSystem.ByPass.Maintenance";
    public static final String PERMISSION_ByPass_Teleport_Costs = "WarpSystem.ByPass.Teleport.Costs";
    public static final String PERMISSION_ByPass_Teleport_Delay = "WarpSystem.ByPass.Teleport.Delay";
    public static boolean OP_CAN_SKIP_DELAY = false;

    private static WarpSystem instance;
    public static boolean activated = false;
    public static boolean maintenance = false;

    private boolean onBungeeCord = false;
    private String server = null;

    private IconManager iconManager = new IconManager();
    private TeleportManager teleportManager = new TeleportManager();
    private FileManager fileManager = new FileManager(this);

    private UpdateChecker updateChecker = new UpdateChecker("https://www.spigotmc.org/resources/warpsystem-gui.29595/history");
    private int latestVersionId = -1;
    private boolean runningFirstTime = false;

    private Timer timer = new Timer();

    private static boolean updateAvailable = false;
    private boolean old = false;
    private boolean ERROR = true;
    private List<CommandBuilder> commands = new ArrayList<>();
    private boolean shouldSave = true;

    private SpigotDataHandler dataHandler = new SpigotDataHandler(this);
    private GlobalWarpManager globalWarpManager = new GlobalWarpManager();

    @Override
    public void onEnable() {
        try {
            checkOldDirectory();

            instance = this;
            API.getInstance().onEnable(this);

            timer.start();

            updateAvailable = WarpSystem.this.updateChecker.needsUpdate();
            latestVersionId = UpdateChecker.getLatestVersionID();

            log(" ");
            log("__________________________________________________________");
            log(" ");
            log("                       WarpSystem [" + getDescription().getVersion() + "]");
            if(updateAvailable) {
                log(" ");
                log("New update available [v" + updateChecker.getVersion() + " - " + WarpSystem.this.updateChecker.getUpdateInfo() + "].");
                log("Download it on\n\n" + updateChecker.getDownload() + "\n");
            }
            log(" ");
            log("Status:");
            log(" ");
            log("MC-Version: " + Version.getVersion().getVersionName());
            log(" ");

            log("Loading files.");
            this.fileManager.loadAll();
            if(this.fileManager.getFile("ActionIcons") == null) this.fileManager.loadFile("ActionIcons", "/Memory/");
            if(this.fileManager.getFile("Teleporters") == null) this.fileManager.loadFile("Teleporters", "/Memory/");
            if(this.fileManager.getFile("Language") == null) this.fileManager.loadFile("Language", "/");
            if(this.fileManager.getFile("Config") == null) this.fileManager.loadFile("Config", "/");

            boolean createBackup = false;
            log("Loading icons.");
            if(!this.iconManager.load(true)) createBackup = true;
            log("Loading TeleportManager.");
            if(!this.teleportManager.load()) createBackup = true;

            if(createBackup) {
                log("Loading with errors > Create backup...");
                createBackup();
                log("Backup successfully created.");
            }

            maintenance = fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Maintenance", false);
            OP_CAN_SKIP_DELAY = fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Teleport.Op_Can_Skip_Delay", false);

            Bukkit.getPluginManager().registerEvents(new TeleportListener(), this);
            Bukkit.getPluginManager().registerEvents(new NotifyListener(), this);
            Bukkit.getPluginManager().registerEvents(new PortalListener(), this);
            Bukkit.getPluginManager().registerEvents(new SignListener(), this);

            if(fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Functions.Warps", true)) {
                CWarp cWarp = new CWarp();
                CWarps cWarps = new CWarps();

                this.commands.add(cWarp);
                this.commands.add(cWarps);

                cWarp.register(this);
                cWarps.register(this);
            }

            this.runningFirstTime = !fileManager.getFile("Config").getConfig().getString("Do_Not_Edit.Last_Version", "2.1.0").equals(getDescription().getVersion());

            CWarpSystem cWarpSystem = new CWarpSystem();
            this.commands.add(cWarpSystem);
            cWarpSystem.register(this);

            if(fileManager.getFile("Config").getConfig().getBoolean("WarpSystem.Functions.Portals", true)) {
                CPortal cPortal = new CPortal();
                this.commands.add(cPortal);
                cPortal.register(this);
            }

            this.startAutoSaver();

            timer.stop();

            log(" ");
            log("Done (" + timer.getLastStoppedTime() + "s)");
            log(" ");
            log("__________________________________________________________");
            log(" ");

            activated = true;
            Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> WarpSystem.getInstance().notifyPlayers(null), 20L);

            this.ERROR = false;

            this.dataHandler.onEnable();

            log("WarpSystem - Looking for a BungeeCord...");

            if(Bukkit.getOnlinePlayers().isEmpty()) {
                log("WarpSystem - Needs a player to search for a BungeeCord. Waiting...");

                Bukkit.getPluginManager().registerEvents(new Listener() {
                    private void unregister() {
                        HandlerList.unregisterAll(this);
                    }

                    @EventHandler
                    public void onJoin(PlayerJoinEvent e) {
                        Bukkit.getScheduler().runTaskLater(WarpSystem.this, () -> {
                            if(Bukkit.getOnlinePlayers().isEmpty()) return;

                            checkBungee();
                        }, 5);
                    }
                }, this);
            } else {
                checkBungee();
            }
        } catch(Exception ex) {
            //make error-report

            if(!getDataFolder().exists()) {
                try {
                    getDataFolder().createNewFile();
                } catch(IOException e) {
                    e.printStackTrace();
                }
            }

            BufferedWriter writer = null;
            try {
                File log = new File(getDataFolder(), "ErrorReport.txt");
                if(log.exists()) log.delete();

                writer = new BufferedWriter(new FileWriter(log));

                PrintWriter printWriter = new PrintWriter(writer);
                ex.printStackTrace(printWriter);
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    writer.close();
                } catch(Exception ignored) {
                }
            }


            log(" ");
            log("__________________________________________________________");
            log(" ");
            log("                       WarpSystem [" + getDescription().getVersion() + "]");
            log(" ");
            log("       COULD NOT ENABLE CORRECTLY!!");
            log(" ");
            log("       Please contact the author with the ErrorReport.txt");
            log("       file in the plugins/WarpSystem folder.");
            log(" ");
            log(" ");
            log("       Thanks for supporting!");
            log(" ");
            log("__________________________________________________________");
            log(" ");

            this.ERROR = true;
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    private void checkBungee() {
        BungeeCordHelper.getCurrentServer(WarpSystem.this, 20 * 10, new Callback<String>() {
            @Override
            public void accept(String server) {
                WarpSystem.this.onBungeeCord = server != null;

                if(onBungeeCord) {
                    log("WarpSystem - Found a BungeeCord > Init GlobalWarps");
                    WarpSystem.this.server = server;
                    new CGlobalWarp().register(WarpSystem.this);
                    globalWarpManager.loadAllGlobalWarps();
                } else {
                    log("WarpSystem - Did not find a BungeeCord > Ignore GlobalWarps");
                }
            }
        });
    }

    @Override
    public void onDisable() {
        API.getInstance().onDisable(this);
        save(false);
        teleportManager.getTeleports().forEach(t -> t.cancel(false, false));

        //Disable all functions
        OP_CAN_SKIP_DELAY = false;
        activated = false;
        maintenance = false;
        onBungeeCord = false;
        server = null;
        updateAvailable = false;
        old = false;
        ERROR = true;
        shouldSave = true;
        BungeeCordHelper.bungeeMessenger = null;

        HandlerList.unregisterAll(this);
        this.dataHandler.onDisable();

        for(int i = 0; i < this.commands.size(); i++) {
            this.commands.remove(0).unregister(this);
        }

        this.globalWarpManager.getGlobalWarps().clear();
    }

    public void reload(boolean save) {
        this.shouldSave = save;

        Bukkit.getPluginManager().disablePlugin(WarpSystem.getInstance());
        Bukkit.getPluginManager().enablePlugin(WarpSystem.getInstance());
    }

    private void startAutoSaver() {
        WarpSystem.log("Starting AutoSaver.");
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(WarpSystem.getInstance(), () -> save(true), 20 * 60 * 20, 20 * 60 * 20);
    }

    private void save(boolean saver) {
        if(!this.shouldSave) return;
        try {
            if(!saver) {
                timer.start();

                log(" ");
                log("__________________________________________________________");
                log(" ");
                log("                       WarpSystem [" + getDescription().getVersion() + "]");
                if(updateAvailable) {
                    log(" ");
                    log("New update available [v" + updateChecker.getVersion() + " - " + WarpSystem.this.updateChecker.getUpdateInfo() + "]. Download it on \n\n" + updateChecker.getDownload() + "\n");
                }
                log(" ");
                log("Status:");
                log(" ");
                log("MC-Version: " + Version.getVersion().name());
                log(" ");

                if(!this.ERROR) log("Saving icons.");
                else {
                    log("Does not save data, because of errors at enabling this plugin.");
                    log(" ");
                    log("Please submit the ErrorReport.txt file to CodingAir.");
                }
            }

            if(!this.ERROR) {
                iconManager.save(true);
                if(!saver) log("Saving options.");
                fileManager.getFile("Config").loadConfig();
                fileManager.getFile("Config").getConfig().set("WarpSystem.Maintenance", maintenance);
                fileManager.getFile("Config").getConfig().set("WarpSystem.Teleport.Op_Can_Skip_Delay", OP_CAN_SKIP_DELAY);
                if(!saver) log("Saving features.");
                teleportManager.save(saver);
            }

            if(!saver) {
                timer.stop();

                log(" ");
                log("Done (" + timer.getLastStoppedTime() + "s)");
                log(" ");
                log("__________________________________________________________");
                log(" ");
            }
        } catch(Exception ex) {
            getLogger().log(Level.SEVERE, "Error at saving data! Exception: \n\n");
            ex.printStackTrace();
            getLogger().log(Level.SEVERE, "\n");
        }
    }

    private void checkOldDirectory() {
        File file = getDataFolder();

        if(file.exists()) {
            File warps = new File(file, "Memory/Warps.yml");

            if(warps.exists()) {
                old = true;
                renameUnnecessaryFiles();
            }
        }
    }

    private void renameUnnecessaryFiles() {
        File file = getDataFolder();

        new File(file, "Config.yml").renameTo(new File(file, "OldConfig_Update_2.0.yml"));
        new File(file, "Language.yml").renameTo(new File(file, "OldLanguage_Update_2.0.yml"));
    }

    public void createBackup() {
        try {
            getDataFolder().createNewFile();
        } catch(IOException e) {
            e.printStackTrace();
        }

        File backupFolder = new File(getDataFolder().getPath() + "/Backups/", TimeFetcher.getYear() + "_" + (TimeFetcher.getMonthNum() + 1) + "_" + TimeFetcher.getDay() + " " + TimeFetcher.getHour() + "_" + TimeFetcher.getMinute() + "_" + TimeFetcher.getSecond());
        backupFolder.mkdirs();

        for(File file : getDataFolder().listFiles()) {
            if(file.getName().equals("Backups") || file.getName().equals("ErrorReport.txt")) continue;
            File dest = new File(backupFolder, file.getName());

            try {
                if(file.isDirectory()) {
                    copyFolder(file, dest);
                    continue;
                }

                copyFileUsingFileChannels(file, dest);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyFolder(File source, File dest) throws IOException {
        dest.mkdirs();
        for(File file : source.listFiles()) {
            File copy = new File(dest, file.getName());

            if(file.isDirectory()) {
                copyFolder(file, copy);
                continue;
            }

            copyFileUsingFileChannels(file, copy);
        }
    }

    private void copyFileUsingFileChannels(File source, File dest) throws IOException {
        FileChannel inputChannel = null;
        FileChannel outputChannel = null;
        try {
            inputChannel = new FileInputStream(source).getChannel();
            outputChannel = new FileOutputStream(dest).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
        } finally {
            inputChannel.close();
            outputChannel.close();
        }
    }

    public void notifyPlayers(Player player) {
        if(player == null) {
            for(Player p : Bukkit.getOnlinePlayers()) {
                notifyPlayers(p);
            }
        } else {
            if(player.hasPermission(WarpSystem.PERMISSION_NOTIFY) && WarpSystem.updateAvailable) {
                TextComponent tc0 = new TextComponent(Lang.getPrefix() + "§7A new update is available §8[§bv" + WarpSystem.getInstance().updateChecker.getVersion() + "§8 - §b" + WarpSystem.getInstance().updateChecker.getUpdateInfo() + "§8]§7. Download it §7»");
                TextComponent click = new TextComponent("§chere");
                TextComponent tc1 = new TextComponent("§7«!");

                click.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/warps-portals-and-warpsigns-warp-system-only-gui.29595/update?update=" + latestVersionId));
                click.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {new TextComponent("§7»Click«")}));

                tc0.addExtra(click);
                tc0.addExtra(tc1);
                tc0.setColor(ChatColor.GRAY);

                int updateId = WarpSystem.getInstance().getLatestVersionId();

                TextComponent command0 = new TextComponent(Lang.getPrefix() + "§7Run \"§c/WarpSystem news§7\" or click »");
                TextComponent command1 = new TextComponent("§chere");
                TextComponent command2 = new TextComponent("§7« to read all new stuff!");

                command1.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/warps-portals-and-warpsigns-warp-system-only-gui.29595/update?update=" + updateId));
                command1.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.BaseComponent[] {new TextComponent("§7»Click«")}));

                command0.addExtra(command1);
                command0.addExtra(command2);
                command0.setColor(ChatColor.GRAY);

                player.sendMessage("");
                player.sendMessage("");
                player.spigot().sendMessage(tc0);
                player.sendMessage("");
                player.spigot().sendMessage(command0);
                player.sendMessage("");
                player.sendMessage("");
            } else if(player.hasPermission(WarpSystem.PERMISSION_NOTIFY) && this.runningFirstTime) {
                ConfigFile file = fileManager.getFile("Config");
                file.getConfig().set("Do_Not_Edit.Last_Version", getDescription().getVersion());
                file.saveConfig();

                FancyMessage message = new FancyMessage(player, MessageTypes.INFO_MESSAGE, true);
                message.addMessages("                                       §c§l§n" + getDescription().getName() + " §c- §l" + getDescription().getVersion());
                message.addMessages("");
                message.addMessages("    §7Hey there,");
                message.addMessages("    §7This is the first time for this server running my latest");
                message.addMessages("    §7version! If you're struggling with all the §cnew stuff§7, run");
                message.addMessages("    §7\"§c/WarpSystem news§7\". And if you'll find some new §cbugs§7,");
                message.addMessages("    §7please run \"§c/WarpSystem report§7\" to report the bug!");
                message.addMessages("");
                message.addMessages("    §7Thank you for using my plugin!");
                message.addMessages("");
                message.addMessages("    §bCodingAir");
                message.send();
            }
        }
    }

    public static WarpSystem getInstance() {
        return instance;
    }

    public IconManager getIconManager() {
        return iconManager;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public boolean isOnBungeeCord() {
        return onBungeeCord;
    }

    public TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public boolean isOld() {
        return old;
    }

    public SpigotDataHandler getDataHandler() {
        return dataHandler;
    }

    public GlobalWarpManager getGlobalWarpManager() {
        return globalWarpManager;
    }

    public String getCurrentServer() {
        return server;
    }

    public List<CommandBuilder> getCommands() {
        return commands;
    }

    public int getLatestVersionId() {
        return latestVersionId;
    }
}
