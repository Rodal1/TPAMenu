package org.terminum.tpamenu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Command implements CommandExecutor, Listener {

    String targetName;
    Logger log = Main.getPlugin().getLogger();
    Map<UUID, UUID> pendingRequests = new HashMap<>();
    FileConfiguration config = Main.getPlugin().getConfig();

    public String color(String s) {
        return Main.getPlugin().colorize(s);
    }

    @Deprecated
    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color("&cOnly players can run this command!"));
            return true;
        }
        Player p = (Player) sender;
        if (args.length < 1) {
            switch (cmd.getName()) {
                case "tpa":
                    p.openInventory(createMainMenu(p, 1));
                    break;
                case "tpaccept":
                    for (Map.Entry<UUID, UUID> map : pendingRequests.entrySet()) {
                        if (map.getKey() == p.getUniqueId()) {
                            Player tpaSender = Bukkit.getPlayer(map.getValue());
                            if (tpaSender == null) {
                                p.sendMessage(color(config.getString("TargetDisconnected")));
                                return true;
                            }
                            tpaSender.sendMessage(color(Objects.requireNonNull(config.getString("PlayerAccepted")).replace("%player%", p.getName())));
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        tpaSender.teleport(p.getLocation());
                                    } catch (NullPointerException e) {
                                        log.warning(e.getMessage());
                                    }
                                }
                            }.runTaskLater(Main.getPlugin(), 3 * 20);
                        }
                    }
                case "tpadeny":
                    pendingRequests.remove(p.getUniqueId());
                    p.sendMessage(color(config.getString("PlayerCancelled")));
                    Objects.requireNonNull(Bukkit.getPlayer(targetName)).sendMessage(color(Objects.requireNonNull(config.getString("TargetCancelled")).replace("%player%", p.getName())));
                    break;
            }
        }
        return false;
    }

    private Inventory createMainMenu(Player p, int page) {
        Inventory inventory = Bukkit.createInventory(null, 9 * 3, color(config.getString("MenuTitle")));

        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        List<ItemStack> playerHeads = players.stream()
                .filter(player -> !player.equals(p)) // filter out the own player
                .map(player -> {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    meta.setOwningPlayer(player);
                    meta.setDisplayName(color(Objects.requireNonNull(config.getString("PlayerName")).replace("%player%", player.getName())));
                    meta.setLore(Collections.singletonList((color(Objects.requireNonNull(config.getString("ItemDescription"))))));
                    head.setItemMeta(meta);
                    return head;
                })
                .collect(Collectors.toList());

        int pageSize = inventory.getSize() - 2;
        int startSlot = (page - 1) * pageSize;
        int endSlot = Math.min(startSlot + pageSize, playerHeads.size());

        int slot = 0;
        for (int i = startSlot; i < endSlot; i++) {
            inventory.setItem(slot, playerHeads.get(i));
            slot++;
        }

        if (endSlot < playerHeads.size()) {
            ItemStack nextPageItem = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPageItem.getItemMeta();
            meta.setDisplayName(color(config.getString("NextPage")));
            List<String> nextPageLore = new ArrayList<>();
            nextPageLore.add("CurrentPage:" + page);
            meta.setLore(nextPageLore);
            nextPageItem.setItemMeta(meta);
            inventory.setItem(inventory.getSize() - 2, nextPageItem);
        }

        if (page > 1) {
            ItemStack previousPageItem = new ItemStack(Material.ARROW);
            ItemMeta meta = previousPageItem.getItemMeta();
            meta.setDisplayName(color(config.getString("PreviousPage")));
            List<String> previousPageLore = new ArrayList<>();
            previousPageLore.add("CurrentPage:" + page);
            meta.setLore(previousPageLore);
            previousPageItem.setItemMeta(meta);
            inventory.setItem(inventory.getSize() - 1, previousPageItem);
        }

        return inventory;
    }

    private Inventory createSubMenu() {
        Inventory subMenu = Bukkit.createInventory(null, 9 * 3, color(Objects.requireNonNull(config.getString("SubMenuTitle")).replace("%player%", targetName)));

        ItemStack requestTpaItem = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(config.getString("RequestTPAItem")))));
        ItemStack infoItem = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(config.getString("WarningItem")))));
        ItemStack closeItem = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(config.getString("CancelItem")))));
        ItemStack backItem = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(config.getString("BackItem")))));

        ItemMeta requestTpaMeta = requestTpaItem.getItemMeta();
        requestTpaMeta.displayName(Component.text(color(config.getString("SubMenuTPA"))));
        requestTpaMeta.setLore(Main.getPlugin().colorizeList(config.getStringList("SubMenuTPALore")));
        requestTpaItem.setItemMeta(requestTpaMeta);

        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.displayName(Component.text(color(config.getString("SubMenuWarning"))));
        infoMeta.setLore(Main.getPlugin().colorizeList(config.getStringList("SubMenuWarningLore")));
        infoItem.setItemMeta(infoMeta);

        ItemMeta closeMeta = closeItem.getItemMeta();
        closeMeta.displayName(Component.text(color(config.getString("SubMenuCancel"))));
        closeMeta.setLore(Main.getPlugin().colorizeList(config.getStringList("SubMenuCancelLore")));
        closeItem.setItemMeta(closeMeta);

        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(Component.text(color(config.getString("SubMenuBack"))));
        backItem.setItemMeta(backMeta);

        subMenu.setItem(10, requestTpaItem);
        subMenu.setItem(13, infoItem);
        subMenu.setItem(16, closeItem);
        subMenu.setItem(18, backItem);

        for (int i = 0; i < subMenu.getSize(); i++) {
            ItemStack item = subMenu.getItem(i);
            if (item == null) {
                item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text(" "));
                item.setItemMeta(meta);
                subMenu.setItem(i, item);
            }
        }
        return subMenu;
    }

    @Deprecated
    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().equals(color(config.getString("MenuTitle"))) && !e.getView().getTitle().equals(color(Objects.requireNonNull(config.getString("SubMenuTitle")).replace("%player%", targetName)))) {
            log.warning("Not passed on Inventory Click.");
            log.warning(e.getView().getTitle());
            return;
        }
        ItemStack clickedItem = e.getCurrentItem();
        Player p = (Player) e.getWhoClicked();
        if (clickedItem == null) {
            return;
        }
        //Abrir menu general
        if (e.getView().getTitle().equals(color(config.getString("MenuTitle")))) {
            log.warning("Passed on Inventory Click to MenuTitle");
            //Clickear una cabeza
            switch (clickedItem.getType()) {
                case PLAYER_HEAD:
                    targetName = clickedItem.getItemMeta().getDisplayName().substring(16);
                    e.setCancelled(true);
                    p.closeInventory();
                    p.openInventory(createSubMenu());
                    break;
                case ARROW:
                    e.setCancelled(true);
                    List<String> lore = clickedItem.getItemMeta().getLore();
                    int currentPage = 1;
                    if (lore != null && !lore.isEmpty()) {
                        String loreText = ChatColor.stripColor(lore.get(0));
                        currentPage = Integer.parseInt(loreText.split(":")[1]);
                    }

                    if (clickedItem.getItemMeta().getDisplayName().equals(color(config.getString("NextPage")))) {
                        p.openInventory(createMainMenu(p, currentPage + 1));
                    } else {
                        p.openInventory(createMainMenu(p, currentPage - 1));
                    }
                    break;
            }
        }
        //Abrir cabeza de usuario
        log.warning(e.getView().getTitle());
        if (e.getView().getTitle().equals(color(Objects.requireNonNull(config.getString("SubMenuTitle")).replace("%player%", targetName)))) {
            log.warning("Passed on Inventory Click to SubMenuTitle");
            //Clickear TPA
            if (Objects.equals(clickedItem.getItemMeta().displayName(), Component.text(color(Objects.requireNonNull(Objects.requireNonNull(config.getString("SubMenuTPA"))))))) {
                if (p.getName().equals(targetName)) {
                    p.sendMessage(color(config.getString("SelfTpa")));
                    e.setCancelled(true);
                    p.closeInventory();
                    return;
                }
                log.warning("Passed on Inventory Click to TPA");
                p.closeInventory();
                if (Bukkit.getPlayer(targetName) == null) {
                    log.warning("TARGET NULL: " + targetName);
                    return;
                }
                if (pendingRequests != null) {
                    if (!pendingRequests.isEmpty()) {
                        for (Map.Entry<UUID, UUID> map : pendingRequests.entrySet()) {
                            log.warning(map.getKey() + ", " + map.getValue());
                            if (map.getValue() == p.getUniqueId() && map.getKey() == Bukkit.getPlayerUniqueId(targetName)) {
                                p.sendMessage(color(config.getString("AlreadySent")));
                                return;
                            }
                        }
                    }
                }
                tpaRequest(p, Bukkit.getPlayer(targetName));
                log.warning("Command performed.");
            } else if (Objects.equals(clickedItem.getItemMeta().displayName(), Component.text(color(Objects.requireNonNull(Objects.requireNonNull(config.getString("SubMenuWarning"))))))) {
                for (String i : config.getStringList("WarningMessage")) {
                    p.sendMessage(color(i));
                    e.setCancelled(true);
                    p.closeInventory();
                }
            } else if (Objects.equals(clickedItem.getItemMeta().displayName(), Component.text(color(Objects.requireNonNull(Objects.requireNonNull(config.getString("SubMenuCancel"))))))) {
                e.setCancelled(true);
                p.closeInventory();
            } else if (Objects.equals(clickedItem.getItemMeta().displayName(), Component.text(color(Objects.requireNonNull(Objects.requireNonNull(config.getString("SubMenuBack"))))))) {
                e.setCancelled(true);
                p.openInventory(createMainMenu(p, 1));
            }
        }
    }

    private void waitDelay(Player p, Player t) {
        pendingRequests.put(t.getUniqueId(), p.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                pendingRequests.remove(t.getUniqueId(), p.getUniqueId());
            }
        }.runTaskLater(Main.getPlugin(), 120 * 20); // 30 seconds
    }

    private void tpaRequest(Player p, Player target) {
        if (p == null) {
            return;
        }
        if (target == null) {
            p.sendMessage(color(config.getString("TargetDisconnected")));
            return;
        }
        Component accept = Component.text(color(config.getString("RequestAccept")))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND, "/tpaccept"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.hoverEvent(net.kyori.adventure.text.event.HoverEvent.Action.SHOW_TEXT, Component.text(color(Objects.requireNonNull(config.getString("HoverMessageAccept"))))));

        Component deny = Component.text(color(config.getString("RequestDeny")))
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.clickEvent(net.kyori.adventure.text.event.ClickEvent.Action.RUN_COMMAND, "/tpadeny"))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.hoverEvent(net.kyori.adventure.text.event.HoverEvent.Action.SHOW_TEXT, Component.text(color(Objects.requireNonNull(config.getString("HoverMessageDeny"))))));


        Component space1 = Component.text(color(config.getString("BeforeAccept")));
        Component space2 = Component.text(color(config.getString("AfterAccept")));
        Component message = Component.text("");

        message = message.append(space1);
        message = message.append(accept);
        message = message.append(space2);
        message = message.append(deny);

        waitDelay(p, Objects.requireNonNull(Bukkit.getPlayer(targetName)));
        target.sendMessage(color(Objects.requireNonNull(config.getString("RequestTarget")).replace("%player%", p.getName())));
        target.sendMessage(message);
    }
}