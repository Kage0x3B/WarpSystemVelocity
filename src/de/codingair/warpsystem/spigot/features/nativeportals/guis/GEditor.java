package de.codingair.warpsystem.spigot.features.nativeportals.guis;

import de.codingair.codingapi.player.MessageAPI;
import de.codingair.codingapi.player.gui.anvil.AnvilClickEvent;
import de.codingair.codingapi.player.gui.anvil.AnvilCloseEvent;
import de.codingair.codingapi.player.gui.anvil.AnvilGUI;
import de.codingair.codingapi.player.gui.anvil.AnvilListener;
import de.codingair.codingapi.player.gui.inventory.gui.GUI;
import de.codingair.codingapi.player.gui.inventory.gui.GUIListener;
import de.codingair.codingapi.player.gui.inventory.gui.itembutton.ItemButton;
import de.codingair.codingapi.player.gui.inventory.gui.itembutton.ItemButtonOption;
import de.codingair.codingapi.server.Sound;
import de.codingair.codingapi.tools.items.ItemBuilder;
import de.codingair.codingapi.tools.items.XMaterial;
import de.codingair.codingapi.utils.TextAlignment;
import de.codingair.warpsystem.spigot.base.WarpSystem;
import de.codingair.warpsystem.spigot.base.language.Example;
import de.codingair.warpsystem.spigot.base.language.Lang;
import de.codingair.warpsystem.spigot.features.globalwarps.guis.GGlobalWarpList;
import de.codingair.warpsystem.spigot.features.nativeportals.Portal;
import de.codingair.warpsystem.spigot.features.nativeportals.PortalEditor;
import de.codingair.warpsystem.spigot.features.nativeportals.managers.NativePortalManager;
import de.codingair.warpsystem.spigot.features.nativeportals.utils.PortalType;
import de.codingair.warpsystem.spigot.features.warps.guis.GWarps;
import de.codingair.warpsystem.spigot.features.warps.guis.affiliations.Warp;
import de.codingair.warpsystem.spigot.features.warps.guis.utils.Head;
import de.codingair.warpsystem.spigot.features.warps.guis.utils.Task;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class GEditor extends GUI {
    private Portal backup;
    private Portal portal;
    private Menu menu;

    private String name;
    private PortalType type;

    private Warp warp;
    private String globalWarp;
    private boolean changed = false;

    public GEditor(Player p) {
        this(p, null);
    }

    public GEditor(Player p, Portal backup) {
        this(p, backup, Menu.MAIN);
    }

    public GEditor(Player p, Portal backup, Menu page) {
        super(p, Lang.get("Native_Portals", new Example("ENG", "&cNative Portals &7- &cEditor"), new Example("GER", "&cNative Portals &7- &cEditor")), 27, WarpSystem.getInstance(), false);

        this.backup = backup;
        if(this.backup != null) {
            this.name = this.backup.getDisplayName();
            this.type = this.backup.getType();
            this.warp = this.backup.getWarp();
            this.globalWarp = this.backup.getGlobalWarp();

            this.backup.setVisible(false);
            this.portal = backup.clone();
            this.portal.setVisible(true);
        }

        this.menu = page == null ? Menu.MAIN : page;

        addListener(new GUIListener() {
            @Override
            public void onInvClickEvent(InventoryClickEvent e) {

            }

            @Override
            public void onInvOpenEvent(InventoryOpenEvent e) {

            }

            @Override
            public void onInvCloseEvent(InventoryCloseEvent e) {
                if(isClosingByButton() || isClosingByOperation()) return;

                if(menu == Menu.DELETE) {
                    if(backup != null) {
                        portal.clear();
                        backup.setVisible(true);
                    }

                    p.sendMessage(Lang.getPrefix() + Lang.get("NativePortal_Not_Deleted", new Example("ENG", "&7The portal was &cnot &7deleted."), new Example("GER", "&7Das Portal wurde &cnicht &7gelöscht.")));
                    return;
                }

                if(!changed) {
                    if(backup != null) {
                        portal.clear();
                        backup.setVisible(true);
                    }

                    return;
                }

                Sound.CLICK.playSound(getPlayer());
                if(menu != Menu.CLOSE) menu = menu == Menu.MAIN ? Menu.CLOSE : Menu.MAIN;

                reinitialize(menu == Menu.CLOSE ?
                        Lang.get("NativePortals_Confirm_Close", new Example("ENG", "&cNative Portals &7- &4Save?"), new Example("GER", "&cNative Portals &7- &4Speichern?"))
                        : getTitle());
                Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), () -> open(), 1);
            }

            @Override
            public void onInvDragEvent(InventoryDragEvent e) {

            }

            @Override
            public void onMoveToTopInventory(ItemStack item, int oldRawSlot, List<Integer> newRawSlots) {

            }

            @Override
            public void onCollectToCursor(ItemStack item, List<Integer> oldRawSlots, int newRawSlot) {

            }
        });

        initialize(p);
    }

    @Override
    public void initialize(Player p) {
        ItemButtonOption option = new ItemButtonOption();
        option.setOnlyLeftClick(true);
        option.setClickSound(Sound.CLICK.bukkitSound());

        ItemStack black = new ItemBuilder(XMaterial.BLACK_STAINED_GLASS_PANE).setHideName(true).getItem();
        ItemStack gray = new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).setHideName(true).getItem();
        ItemStack leaves = new ItemBuilder(XMaterial.OAK_LEAVES).setHideName(true).getItem();

        setItem(0, 0, leaves);
        setItem(8, 0, leaves);

        setItem(1, 0, black);
        setItem(0, 1, black);
        setItem(0, 2, black);
        setItem(7, 0, black);
        setItem(8, 1, black);
        setItem(8, 2, black);

        setItem(2, 0, gray);
        setItem(1, 1, gray);
        setItem(1, 2, gray);
        setItem(6, 0, gray);
        setItem(7, 1, gray);
        setItem(7, 2, gray);

        String waterPortal = Lang.get("Water_Portal", new Example("ENG", "&bWater backup"), new Example("GER", "&bWasser-Portal"));
        String lavaPortal = Lang.get("Lava_Portal", new Example("ENG", "&cLava backup"), new Example("GER", "&cLava-Portal"));
        String netherPortal = Lang.get("Nether_Portal", new Example("ENG", "&4Nether backup"), new Example("GER", "&4Nether-Portal"));
        String endPortal = Lang.get("End_Portal", new Example("ENG", "&9End backup"), new Example("GER", "&9End-Portal"));

        if(menu != Menu.CLOSE) {
            boolean ready = name != null && type != null && (warp != null || globalWarp != null) && portal != null && !portal.getBlocks().isEmpty();
            ItemBuilder builder = new ItemBuilder(ready ? XMaterial.LIME_TERRACOTTA : XMaterial.RED_TERRACOTTA)
                    .setText((ready ? "§a" : "§c") + "§n" + Lang.get("Status", new Example("ENG", "Status"), new Example("GER", "Status")));

            builder.addText("§7" + Lang.get("Name", new Example("ENG", "Name"), new Example("GER", "Name")) + ": " + (name == null ? "§c§m-" : name));
            builder.addText("§7" + Lang.get("NativePortal_Material", new Example("ENG", "Portal material"), new Example("GER", "Portal-Material")) + ": " + (type == null ? "§c§m-" : type.name()));
            builder.addText("§7" + Lang.get("Teleport_Link", new Example("ENG", "Teleport link"), new Example("GER", "Teleport-Verlinkung")) + ": " + (warp == null && globalWarp == null ? "§c§m-" : warp == null ? globalWarp : warp.getNameWithoutColor()));
            builder.addText("§7" + Lang.get("Portal_Blocks", new Example("ENG", "Portal blocks"), new Example("GER", "Portal-Blöcke")) + ": " + (portal == null ? "§c0" : portal.getBlocks().size()));

            builder.addText("");

            if(ready) {
                builder.addText("§8» §a" + Lang.get("Save", new Example("ENG", "Save"), new Example("GER", "Speichern")));

                addButton(new ItemButton(4, builder.getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        if(backup == null) {
                            portal.setType(type);
                            portal.setDisplayName(name);
                            portal.setGlobalWarp(globalWarp);
                            portal.setWarp(warp);
                            NativePortalManager.getInstance().addPortal(portal);
                        } else {
                            backup.setType(type);
                            backup.setDisplayName(name);
                            backup.setGlobalWarp(globalWarp);
                            backup.setWarp(warp);

                            backup.apply(portal);
                            portal.clear();
                            backup.setVisible(true);
                        }
                    }
                }.setOption(option).setCloseOnClick(true));
            } else {
                builder.addText("§8» " + Lang.get("Not_Ready_For_Saving", new Example("ENG", "&cThe portal cannot be saved yet"), new Example("GER", "&cDas Portal kann noch nicht gespeichert werden")));
                setItem(4, builder.getItem());
            }
        }

        switch(menu) {
            case MAIN:
                addButton(new ItemButton(2, 2, new ItemBuilder(XMaterial.NAME_TAG).setName("§8» §b" + Lang.get("Set_Portal_Name", new Example("ENG", "Set portal name"), new Example("GER", "Name setzen"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        AnvilGUI.openAnvil(WarpSystem.getInstance(), getPlayer(), new AnvilListener() {
                            @Override
                            public void onClick(AnvilClickEvent e) {
                                playSound(getPlayer());
                                e.setCancelled(true);
                                e.setClose(false);

                                if(e.getInput() == null) {
                                    getPlayer().sendMessage(Lang.getPrefix() + Lang.get("Enter_Name", new Example("ENG", "&cPlease enter a name."), new Example("GER", "&cBitte gib einen Namen ein.")));
                                    return;
                                }

                                e.setClose(true);
                            }

                            @Override
                            public void onClose(AnvilCloseEvent e) {
                                if(e.getSubmittedText() != null) {
                                    GEditor.this.name = e.getSubmittedText();
                                    changed = true;
                                }

                                initialize(p);
                                e.setPost(GEditor.this::open);
                            }
                        }, new ItemBuilder(XMaterial.PAPER).setName(Lang.get("Portal_Name", new Example("ENG", "Portal name"), new Example("GER", "Portal-Name")) + "...").getItem());
                    }
                }.setOption(option).setCloseOnClick(true));

                addButton(new ItemButton(3, 2, new ItemBuilder(XMaterial.END_PORTAL_FRAME).setName("§8» §b" + Lang.get("NativePortal_Material", new Example("ENG", "Portal material"), new Example("GER", "Portal-Material"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        menu = Menu.MATERIAL;
                        reinitialize();
                    }
                }.setOption(option));

                addButton(new ItemButton(5, 2, new ItemBuilder(XMaterial.ENDER_PEARL).setName("§8» §b" + Lang.get("Teleport_Link", new Example("ENG", "Teleport link"), new Example("GER", "Teleport-Verlinkung"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        menu = Menu.TYPE;
                        reinitialize();
                    }
                }.setOption(option));

                addButton(new ItemButton(6, 2, new ItemBuilder(XMaterial.IRON_PICKAXE).setHideStandardLore(true).setName("§8» §b" + Lang.get("NativePortals_Set_Blocks", new Example("ENG", "Set teleport blocks"), new Example("GER", "Setze Teleport-Blöcke"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        PortalEditor editor = portal == null ? new PortalEditor(getPlayer(), type) : new PortalEditor(getPlayer(), portal);
                        int size = editor.getPortal().getBlocks().size();
                        editor.init();

                        MessageAPI.sendActionBar(getPlayer(), Lang.get("Drop_To_Leave", new Example("ENG", "&cDrop the item to go to the menu"), new Example("GER", "&cLasse den Block fallen, um zum Menü zu kommen")), Integer.MAX_VALUE);

                        Bukkit.getPluginManager().registerEvents(new Listener() {
                            @EventHandler
                            public void onDrop(PlayerDropItemEvent e) {
                                if(e.getItemDrop().getItemStack().equals(PortalEditor.PORTAL_ITEM.getItem())) {
                                    e.setCancelled(true);

                                    Bukkit.getScheduler().runTask(WarpSystem.getInstance(), () -> {
                                        MessageAPI.stopSendingActionBar(getPlayer());
                                        portal = editor.end();
                                        if(size != portal.getBlocks().size()) changed = true;
                                        reinitialize();
                                        open();
                                        HandlerList.unregisterAll(this);
                                    });
                                }
                            }
                        }, WarpSystem.getInstance());
                    }
                }.setOption(option).setCloseOnClick(true));
                break;

            case MATERIAL:
                addButton(new ItemButton(2, 2, new ItemBuilder(XMaterial.WATER_BUCKET).setName("§8» §b" + waterPortal).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        PortalType next = PortalType.WATER;
                        if(type != next) {
                            changed = true;
                            type = next;
                            if(portal != null) portal.setType(type);
                        }

                        menu = Menu.MAIN;
                        reinitialize();
                    }
                }.setOption(option));

                addButton(new ItemButton(3, 2, new ItemBuilder(XMaterial.LAVA_BUCKET).setName("§8» §b" + lavaPortal).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        PortalType next = PortalType.LAVA;
                        if(type != next) {
                            changed = true;
                            type = next;
                            if(portal != null) portal.setType(type);
                        }

                        menu = Menu.MAIN;
                        reinitialize();
                    }
                }.setOption(option));

                addButton(new ItemButton(5, 2, new ItemBuilder(XMaterial.FLINT_AND_STEEL).setName("§8» §b" + netherPortal).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        PortalType next = PortalType.NETHER;
                        if(type != next) {
                            changed = true;
                            type = next;
                            if(portal != null) portal.setType(type);
                        }

                        menu = Menu.MAIN;
                        reinitialize();
                    }
                }.setOption(option));

                addButton(new ItemButton(6, 2, new ItemBuilder(XMaterial.END_PORTAL_FRAME).setName("§8» §b" + endPortal).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        PortalType next = PortalType.END;
                        if(type != next) {
                            changed = true;
                            type = next;
                            if(portal != null) portal.setType(type);
                        }

                        menu = Menu.MAIN;
                        reinitialize();
                    }
                }.setOption(option));
                break;

            case TYPE:
                addButton(new ItemButton(WarpSystem.getInstance().isOnBungeeCord() ? 3 : 4, 2, new ItemBuilder(XMaterial.ENDER_PEARL).setName("§8» §b" + Lang.get("Choose_A_Warp", new Example("ENG", "&bChoose a warp"), new Example("GER", "&bWähle ein Warp"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        new GWarps(getPlayer(), null, false, new de.codingair.warpsystem.spigot.features.warps.guis.utils.GUIListener() {

                            @Override
                            public String getTitle() {
                                return Lang.get("Native_Portal_Choose_Warp", new Example("ENG", "&cChoose a warp"), new Example("GER", "&cWähle ein Warp"));
                            }

                            @Override
                            public Task onClickOnWarp(Warp warp, boolean editing) {
                                p.closeInventory();

                                if(GEditor.this.warp != warp) {
                                    GEditor.this.warp = warp;
                                    changed = true;
                                }

                                menu = Menu.MAIN;
                                reinitialize();
                                Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), GEditor.this::open, 1);
                                return new Task();
                            }

                            @Override
                            public void onClose() {
                            }
                        }, false).open();
                    }
                }.setOption(option).setCloseOnClick(true));

                if(WarpSystem.getInstance().isOnBungeeCord()) {
                    addButton(new ItemButton(5, 2, new ItemBuilder(XMaterial.ENDER_CHEST).setName("§8» §b" + Lang.get("Choose_A_GlobalWarp", new Example("ENG", "&bChoose a global warp"), new Example("GER", "&bWähle ein globalen Warp"))).getItem()) {
                        @Override
                        public void onClick(InventoryClickEvent e) {
                            new GGlobalWarpList(p, new GGlobalWarpList.Listener() {
                                @Override
                                public void onClickOnGlobalWarp(String warp, InventoryClickEvent e) {
                                    if(GEditor.this.globalWarp == null || !GEditor.this.globalWarp.equals(warp)) {
                                        GEditor.this.globalWarp = warp;
                                        changed = true;
                                    }

                                    p.closeInventory();
                                }

                                @Override
                                public void onClose() {
                                    menu = Menu.MAIN;
                                    reinitialize();
                                    Bukkit.getScheduler().runTaskLater(WarpSystem.getInstance(), GEditor.this::open, 1);
                                }

                                @Override
                                public String getLeftclickDescription() {
                                    return ChatColor.DARK_GRAY + "» " + ChatColor.GRAY + Lang.get("GlobalWarp_Leftclick_To_Choose", new Example("ENG", "&3Leftclick: &bChoose"), new Example("GER", ChatColor.GRAY + "&3Linksklick: &bWählen"));
                                }
                            }).open();
                        }
                    }.setOption(option).setCloseOnClick(true));
                }
                break;

            case CLOSE:
                setItem(4, 0, new ItemBuilder(XMaterial.NETHER_STAR.parseMaterial()).setText(TextAlignment.lineBreak(Lang.get("Sure_That_You_Want_To_Loose_Your_Data", new Example("ENG", "&7Are you sure that you &cdon't &7want to &csave &7your changes?"), new Example("GER", "&7Bist du sicher, dass du deine Änderungen &cnicht Speichern &7möchtest?")), 100)).getItem());

                addButton(new ItemButton(2, 2, new ItemBuilder(Head.CYAN_ARROW_LEFT.getItem()).setHideName(false).setName("§8» §b" + Lang.get("Back", new Example("ENG", "Back"), new Example("GER", "Zurück"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        menu = Menu.MAIN;
                        reinitialize(Lang.get("Native_Portals", new Example("ENG", "&cNative Portals &7- &cEditor"), new Example("GER", "&cNative Portals &7- &cEditor")));
                        getPlayer().updateInventory();
                    }
                }.setOption(option));

                addButton(new ItemButton(6, 2, new ItemBuilder(XMaterial.BARRIER).setName("§8» §c" + Lang.get("Close", new Example("ENG", "Close"), new Example("GER", "Schließen"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        portal.clear();
                        if(backup != null) backup.setVisible(true);
                    }
                }.setOption(option).setCloseOnClick(true));
                break;

            case DELETE:
                setItem(4, 0, new ItemBuilder(XMaterial.NETHER_STAR.parseMaterial()).setText(TextAlignment.lineBreak(Lang.get("NativePortal_Confirm_Delete", new Example("ENG", "&7Do you really want to &4delete &7the backup?"), new Example("GER", "&7Möchtest du das Portal wirklich &4löschen&7?")), 100)).getItem());

                addButton(new ItemButton(2, 2, new ItemBuilder(XMaterial.LIME_TERRACOTTA).setName("§8» §b" + Lang.get("No_Keep", new Example("ENG", "&aNo, keep"), new Example("GER", "&aNein, behalten"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        if(backup != null) {
                            p.sendMessage(Lang.getPrefix() + Lang.get("NativePortal_Not_Deleted", new Example("ENG", "&7The portal was &cnot &7deleted."), new Example("GER", "&7Das Portal wurde &cnicht &7gelöscht.")));
                            portal.clear();
                            backup.setVisible(true);
                        }
                    }
                }.setOption(option).setCloseOnClick(true));

                addButton(new ItemButton(6, 2, new ItemBuilder(XMaterial.RED_TERRACOTTA).setName("§8» §c" + Lang.get("Yes_Delete", new Example("ENG", "&cYes, &4delete"), new Example("GER", "&cJa, &4löschen"))).getItem()) {
                    @Override
                    public void onClick(InventoryClickEvent e) {
                        if(backup != null) {
                            portal.clear();
                            backup.clear();
                            NativePortalManager.getInstance().getPortals().remove(backup);
                            p.sendMessage(Lang.getPrefix() + Lang.get("NativePortal_Deleted", new Example("ENG", "&cThe portal was deleted successfully."), new Example("GER", "&cDas Portal wurde erfolgreich gelöscht.")));
                        }
                    }
                }.setOption(option).setCloseOnClick(true));
                break;
        }
    }

    public enum Menu {
        MAIN, MATERIAL, TYPE, CLOSE, DELETE
    }
}
