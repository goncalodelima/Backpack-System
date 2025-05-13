package com.twins.core.global.listener;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.api.GrimExemptAddEvent;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.PacketEvents;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import ac.grim.grimac.shaded.io.github.retrooper.packetevents.util.SpigotReflectionUtil;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketLoginSendEvent;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.minecraftsolutions.utils.ItemBuilder;
import com.minecraftsolutions.utils.ItemNBT;
import com.twins.core.CorePlugin;
import com.twins.core.KeyConstants;
import com.twins.core.cupboard.controller.ItemController;
import com.twins.core.api.events.PlayerBackpackDeathEvent;
import com.twins.core.global.GlobalPlugin;
import com.twins.core.global.controller.BackpackArmorStandController;
import com.twins.core.global.model.backpack.Backpack;
import com.twins.core.global.model.user.GlobalUser;
import com.twins.core.global.model.user.language.LanguageType;
import com.twins.core.utils.BukkitUtils;
import com.twins.core.utils.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.logging.Level;

public class BackpackListener extends SimplePacketListenerAbstract implements Listener {

    public final GlobalPlugin plugin;

    public BackpackListener(GlobalPlugin plugin) {
        super(PacketListenerPriority.MONITOR);
        this.plugin = plugin;
    }

    @Override
    public void onPacketLoginSend(PacketLoginSendEvent event) {

        if (event.getPacketType() == PacketType.Login.Server.DISCONNECT) {

            Player player = event.getPlayer();

            if (player == null) {
                return;
            }

            GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);

            if (grimPlayer != null) {
                BackpackArmorStandController.destroy(grimPlayer.uuid);
            }

            BackpackArmorStandController.PLAYER_UUIDS.remove(event.getUser().getUUID());
            BackpackArmorStandController.ARMOR_STAND_IDS.remove(event.getUser().getUUID());
            BackpackArmorStandController.SEEN_ARMOR_STANDS.remove(event.getUser().getUUID());
        }

    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {

        if (event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION || event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION || event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION) {

            Player player = event.getPlayer();

            if (player == null) {
                return;
            }

            GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);

            if (grimPlayer == null) {
                return;
            }

            Integer entityId = BackpackArmorStandController.ARMOR_STAND_IDS.get(grimPlayer.uuid);

            if (entityId != null) {
                BackpackArmorStandController.teleport(entityId, grimPlayer);
            } else if (grimPlayer.bukkitPlayer != null) {
                BackpackArmorStandController.spawnOnJoin(SpigotReflectionUtil.generateEntityId(), grimPlayer);
            }

        }

    }

    @EventHandler
    public void onGrimExemptAdd(GrimExemptAddEvent event) {
        BackpackArmorStandController.destroy(event.getUuid());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {

        Backpack backpack = plugin.getBackpackService().getPendingUpdates().get(event.getUniqueId());

        if (backpack != null) {
            plugin.getBackpackService().put(backpack);
        } else {

            backpack = plugin.getBackpackService().getData(event.getUniqueId()).orElse(null);

            if (backpack != null) {
                plugin.getBackpackService().put(backpack);
            }

        }

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(event.getPlayer());

        if (grimPlayer == null || grimPlayer.user == null || grimPlayer.user.getChannel() == null) {
            return;
        }

        boolean isSneaking = event.isSneaking();

        ChannelHelper.runInEventLoop(grimPlayer.user.getChannel(), () -> {

            if (grimPlayer.isFlying) {
                return;
            }

            BackpackArmorStandController.sneak(grimPlayer, isSneaking);

        });

    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {

        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);

        if (grimPlayer == null) {
            return;
        }

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());

        if (newItem != null && ItemNBT.hasTag(newItem, KeyConstants.BACKPACK)) {
            BackpackArmorStandController.destroy(grimPlayer.uuid);
            BackpackArmorStandController.PLAYER_UUIDS.add(grimPlayer.uuid);
        } else if (BackpackArmorStandController.PLAYER_UUIDS.contains(grimPlayer.uuid)) {

            Integer entityId = BackpackArmorStandController.ARMOR_STAND_IDS.get(grimPlayer.uuid);

            BackpackArmorStandController.PLAYER_UUIDS.remove(grimPlayer.uuid);

            if (entityId != null) {
                boolean isSneaking = player.isSneaking();
                ChannelHelper.runInEventLoop(grimPlayer.user.getChannel(), () -> BackpackArmorStandController.spawn(entityId, grimPlayer, BackpackArmorStandController.SEEN_ARMOR_STANDS.computeIfAbsent(grimPlayer.uuid, _ -> new HashSet<>()), isSneaking));
            }

        }

    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {

        ItemStack itemStack = event.getItem();

        if (itemStack == null) {
            return;
        }

        Material itemType = itemStack.getType();
        byte data = itemStack.getData().getData();

        if (itemType != Material.HAY_BLOCK && (itemType != Material.WOOL || (data != 15 && data != 12 && data != 3))) {
            return;
        }

        event.setCancelled(true);

        Player player = event.getPlayer();
        GlobalUser globalUser = GlobalPlugin.INSTANCE.getUserService().get(player.getUniqueId());

        if (globalUser == null) {
            return;
        }

        if (plugin.getBackpackService().containsTemporaryCache(player.getUniqueId())) {
            player.sendMessage(plugin.getLang().getString(globalUser.getLanguageType(), "wait"));
            return;
        }

        if (ItemNBT.getBoolean(itemStack, KeyConstants.BACKPACK)) {

            Backpack backpack = plugin.getBackpackService().get(player.getUniqueId());

            if (backpack == null) {

                int space;

                if (itemType == Material.WOOL) {

                    if (data == 15) {
                        space = 9;
                    } else if (data == 12) {
                        space = 18;
                    } else {
                        space = 27;
                    }

                } else {
                    space = 45;
                }

                Backpack newBackpack = new Backpack(player.getUniqueId(), new ItemStack[space]);

                plugin.getBackpackService().putData(newBackpack).thenAcceptAsync(success -> {

                    if (success) {
                        plugin.getBackpackService().put(newBackpack);
                        plugin.getBackpackInventory().openBackpack(player, newBackpack);
                    }

                }, CorePlugin.INSTANCE.getMainExecutor());
            } else {
                plugin.getBackpackInventory().openBackpack(player, backpack);
            }

        } else {

            int space;

            if (itemType == Material.WOOL) {

                if (data == 12) {
                    space = 18;
                } else if (data == 3) {
                    space = 27;
                } else {
                    space = 0;
                }

            } else {
                space = 45;
            }

            if (space == 0) {
                return;
            }

            Backpack backpack = plugin.getBackpackService().get(player.getUniqueId());
            LanguageType type = globalUser.getLanguageType();

            if (backpack != null) {

                if (space <= backpack.getContents().length) {
                    player.sendMessage(plugin.getLang().getString(type, "backpack-error"));
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1);
                    return;
                }

                if (player.getItemInHand().getAmount() <= 1) {
                    player.setItemInHand(null);
                } else {
                    player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
                }

                XMaterial newBackpackType;

                if (space == 18) {
                    newBackpackType = XMaterial.BROWN_WOOL;
                } else if (space == 27) {
                    newBackpackType = XMaterial.LIGHT_BLUE_WOOL;
                } else {
                    newBackpackType = XMaterial.HAY_BLOCK;
                }

                player.getInventory().setItem(8,
                        new ItemBuilder(
                                ItemController.getByXMaterial(newBackpackType).clone())
                                .changeItemMeta(meta -> meta.setLore(null))
                                .setNBT(nbt -> nbt.setBoolean(KeyConstants.BACKPACK, true))
                                .build()
                );


                ItemStack[] contents = backpack.getContents();

                backpack.clearContents(space);
                BukkitUtils.transferElements(contents, backpack.getContents());
                plugin.getBackpackService().update(backpack);

            } else { // player never interact with a backpack or he have flame backpack

                if (player.getInventory().getItem(8).getType() == Material.HAY_BLOCK) { // player have flame backpack
                    player.sendMessage(plugin.getLang().getString(type, "backpack-error"));
                    player.playSound(player.getLocation(), Sound.VILLAGER_NO, 1, 1);
                    return;
                }

                // player never interact with a backpack

                ItemStack itemToRemove = player.getItemInHand();
                Backpack newBackpack = new Backpack(player.getUniqueId(), new ItemStack[space]);

                plugin.getBackpackService().addTemporaryCache(player.getUniqueId());

                plugin.getBackpackService().putData(newBackpack).thenAcceptAsync(success -> {

                    if (success) {

                        if (BukkitUtils.getMaterialQuantity(player.getInventory(), itemToRemove) > 0) {

                            XMaterial newBackpackType;

                            if (space == 18) {
                                newBackpackType = XMaterial.BROWN_WOOL;
                            } else if (space == 27) {
                                newBackpackType = XMaterial.LIGHT_BLUE_WOOL;
                            } else {
                                newBackpackType = XMaterial.HAY_BLOCK;
                            }

                            BukkitUtils.removeItemsByTypeAndData(player, itemToRemove, 1);

                            plugin.getBackpackService().put(newBackpack);
                            player.getInventory().setItem(8,
                                    new ItemBuilder(
                                            ItemController.getByXMaterial(newBackpackType).clone())
                                            .changeItemMeta(meta -> meta.setLore(null))
                                            .setNBT(nbt -> nbt.setBoolean(KeyConstants.BACKPACK, true))
                                            .build()
                            );

                        }

                    }

                    plugin.getBackpackService().removeTemporaryCache(player.getUniqueId());

                }, CorePlugin.INSTANCE.getMainExecutor());

            }

            player.sendMessage(plugin.getLang().getString(type, "backpack-changed"));
            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 1);

            GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(player);

            if (grimPlayer != null) {
                ChannelHelper.runInEventLoop(grimPlayer.user.getChannel(), () -> BackpackArmorStandController.updateEquipment(grimPlayer, space));
            }

        }

    }

    // events to block the backpack interact
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        ItemStack itemStack = event.getCurrentItem();

        if (!event.getWhoClicked().hasPermission("backpack.admin") && itemStack != null && itemStack.getType() != Material.AIR && ItemNBT.getBoolean(itemStack, KeyConstants.BACKPACK)) {
            event.setCancelled(true);
        }

        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {

            Player player = (Player) event.getWhoClicked();
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());

            if (hotbarItem != null && hotbarItem.getType() != Material.AIR && ItemNBT.getBoolean(hotbarItem, KeyConstants.BACKPACK)) {
                event.setCancelled(true);
            }

        }

    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        ItemStack itemStack = event.getItemDrop().getItemStack();

        if (itemStack != null && ItemNBT.getBoolean(itemStack, KeyConstants.BACKPACK)) {
            event.setCancelled(true);
        }

    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {

        Player killed = event.getEntity();
        GrimPlayer grimPlayer = GrimAPI.INSTANCE.getPlayerDataManager().getPlayer(killed);

        if (grimPlayer != null) {
            BackpackArmorStandController.destroy(grimPlayer.uuid);
        }

        GlobalUser globalUser = plugin.getUserService().get(killed.getUniqueId());

        if (globalUser == null) {
            return;
        }

        if (globalUser.isBeginner()) {
            return;
        }

        Location killedLocation = killed.getLocation();
        Backpack backpack = plugin.getBackpackService().get(killed.getUniqueId());

        if (backpack != null) {

            plugin.getBackpackService().addTemporaryCache(killed.getUniqueId());

            plugin.getBackpackService().removeData(backpack).thenAcceptAsync(success -> {

                if (success) {
                    Bukkit.getPluginManager().callEvent(new PlayerBackpackDeathEvent(killed, killedLocation, backpack, event.getDrops()));

                    if (backpack.getContents().length != 45) {
                        plugin.getBackpackService().remove(backpack.getUuid());
                    }

                }

                plugin.getBackpackService().removeTemporaryCache(killed.getUniqueId());

            }, CorePlugin.INSTANCE.getMainExecutor());
        } else {
            Bukkit.getPluginManager().callEvent(new PlayerBackpackDeathEvent(killed, killedLocation, null, event.getDrops()));
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getBackpackService().remove(event.getPlayer().getUniqueId());
    }

}
