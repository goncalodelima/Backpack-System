package com.twins.core.global.controller;

import ac.grim.grimac.GrimAPI;
import ac.grim.grimac.player.GrimPlayer;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.protocol.player.Equipment;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.util.Vector3d;
import ac.grim.grimac.shaded.com.github.retrooper.packetevents.wrapper.play.server.*;
import ac.grim.grimac.shaded.io.github.retrooper.packetevents.util.SpigotConversionUtil;
import com.twins.core.KeyConstants;
import com.twins.core.global.GlobalPlugin;
import com.twins.core.global.model.backpack.Backpack;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BackpackArmorStandController {

    public static final Set<UUID> PLAYER_UUIDS = ConcurrentHashMap.newKeySet(); // players que estão com a mochila na mão

    public static final Map<UUID, Integer> ARMOR_STAND_IDS = new ConcurrentHashMap<>();

    public static final Map<UUID, Set<GrimPlayer>> SEEN_ARMOR_STANDS = new ConcurrentHashMap<>(); // uuid do dono do armorstand; viewers

    public static double getDistance(double loc1X, double loc1Z, double loc2X, double loc2Z) {
        return Math.sqrt(Math.pow(loc1X - loc2X, 2) + Math.pow(loc1Z - loc2Z, 2));
    }

    public static void spawnOnJoin(int entityId, GrimPlayer grimPlayer) {

        Backpack backpack = GlobalPlugin.INSTANCE.getBackpackService().get(grimPlayer.uuid);
        List<Equipment> equipmentList = new ArrayList<>();

        if (backpack == null) {
            equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 15))));
        } else {

            int length = backpack.getContents().length;

            if (length == 9) {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 15))));
            } else if (length == 18) {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 12))));
            } else if (length == 27) {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 3))));
            } else {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.HAY_BLOCK))));
            }

        }

        WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.ARMOR_STAND,
                new Vector3d(grimPlayer.x, grimPlayer.y + 1, grimPlayer.z),
                grimPlayer.xRot,
                grimPlayer.yRot,
                grimPlayer.xRot,
                new Vector3d(0, 0, 0),
                List.of(new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20), new EntityData(10, EntityDataTypes.BYTE, KeyConstants.MARKER_FLAG))
        );

        WrapperPlayServerEntityEquipment equipmentPacket = new WrapperPlayServerEntityEquipment(
                entityId,
                equipmentList
        );

        for (GrimPlayer all : GrimAPI.INSTANCE.getPlayerDataManager().getEntries()) {

            if (!all.equals(grimPlayer)) {

                if (all.bukkitPlayer != null) {

                    if (grimPlayer.bukkitPlayer.getWorld().getUID().equals(all.bukkitPlayer.getWorld().getUID()) && getDistance(grimPlayer.x, grimPlayer.z, all.x, all.z) <= 62) {
                        all.user.sendPacket(packet);
                        all.user.sendPacket(equipmentPacket);
                        SEEN_ARMOR_STANDS.computeIfAbsent(grimPlayer.uuid, _ -> new HashSet<>()).add(all);
                    }

                }

            }

        }

        ARMOR_STAND_IDS.put(grimPlayer.uuid, entityId);

    }

    public static void spawn(int entityId, GrimPlayer grimPlayer, Set<GrimPlayer> viewers, boolean isSneaking) {

        Backpack backpack = GlobalPlugin.INSTANCE.getBackpackService().get(grimPlayer.uuid);
        List<Equipment> equipmentList = new ArrayList<>();

        if (backpack == null) {
            equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 15))));
        } else {

            int length = backpack.getContents().length;

            if (length == 9) {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 15))));
            } else if (length == 18) {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 12))));
            } else if (length == 27) {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 3))));
            } else {
                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.HAY_BLOCK))));
            }

        }

        WrapperPlayServerSpawnLivingEntity packet = new WrapperPlayServerSpawnLivingEntity(
                entityId,
                UUID.randomUUID(),
                EntityTypes.ARMOR_STAND,
                new Vector3d(grimPlayer.x, grimPlayer.y + 1, grimPlayer.z),
                grimPlayer.xRot,
                grimPlayer.yRot,
                grimPlayer.xRot,
                new Vector3d(0, 0, 0),
                List.of(new EntityData(0, EntityDataTypes.BYTE, (byte) (isSneaking ? 0x22 : 0x20)), new EntityData(10, EntityDataTypes.BYTE, KeyConstants.MARKER_FLAG))
        );

        WrapperPlayServerEntityEquipment equipmentPacket = new WrapperPlayServerEntityEquipment(
                entityId,
                equipmentList
        );

        for (GrimPlayer all : viewers) {
            all.user.sendPacket(packet);
            all.user.sendPacket(equipmentPacket);
        }

    }

    public static void teleport(int entityId, GrimPlayer grimPlayer) {

        WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport(entityId, new Vector3d(grimPlayer.x, grimPlayer.y + 1, grimPlayer.z), grimPlayer.xRot, grimPlayer.yRot, false);

        Set<GrimPlayer> seen = SEEN_ARMOR_STANDS.get(grimPlayer.uuid);

        if (seen != null) {
            for (GrimPlayer nearby : seen) {
                nearby.user.sendPacket(packet);
            }
        }

    }

    public static void updateNearbyPlayers(GrimPlayer grimPlayer, GrimPlayer otherPlayer, Set<GrimPlayer> viewers) {

        if (!viewers.contains(otherPlayer)) { //otherPlayer is not a viewer yet

            if (otherPlayer.bukkitPlayer != null) {

                if (grimPlayer.bukkitPlayer.getWorld().getUID().equals(otherPlayer.bukkitPlayer.getWorld().getUID()) && getDistance(grimPlayer.x, grimPlayer.z, otherPlayer.x, otherPlayer.z) <= 62) {

                    Integer standId = ARMOR_STAND_IDS.get(grimPlayer.uuid);

                    if (standId != null) {

                        Backpack backpack = GlobalPlugin.INSTANCE.getBackpackService().get(grimPlayer.uuid);
                        List<Equipment> equipmentList = new ArrayList<>();

                        if (backpack == null) {
                            equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 15))));
                        } else {

                            int length = backpack.getContents().length;

                            if (length == 9) {
                                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 15))));
                            } else if (length == 18) {
                                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 12))));
                            } else if (length == 27) {
                                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 3))));
                            } else {
                                equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.HAY_BLOCK))));
                            }

                        }

                        WrapperPlayServerSpawnLivingEntity spawnPacket = new WrapperPlayServerSpawnLivingEntity(
                                standId,
                                UUID.randomUUID(),
                                EntityTypes.ARMOR_STAND,
                                new Vector3d(grimPlayer.x, grimPlayer.y + 1, grimPlayer.z),
                                grimPlayer.xRot,
                                grimPlayer.yRot,
                                grimPlayer.xRot,
                                new Vector3d(0, 0, 0),
                                List.of(new EntityData(0, EntityDataTypes.BYTE, (byte) 0x20), new EntityData(10, EntityDataTypes.BYTE, KeyConstants.MARKER_FLAG))
                        );

                        WrapperPlayServerEntityEquipment equipmentPacket = new WrapperPlayServerEntityEquipment(standId, equipmentList);

                        otherPlayer.user.sendPacket(spawnPacket);
                        otherPlayer.user.sendPacket(equipmentPacket);

                        viewers.add(otherPlayer);
                    }

                }

            }

        } else if (!grimPlayer.bukkitPlayer.getWorld().getUID().equals(otherPlayer.bukkitPlayer.getWorld().getUID()) || getDistance(grimPlayer.x, grimPlayer.z, otherPlayer.x, otherPlayer.z) > 62) { // como o jogador já é um viewer, não é preciso verificar se o bukkitPlayer é diferente de null
            boolean removed = viewers.remove(otherPlayer);
            destroy(grimPlayer, otherPlayer, removed);
        }

    }

    public static void updateEquipment(GrimPlayer grimPlayer, int space) {

        Integer id = ARMOR_STAND_IDS.get(grimPlayer.uuid);

        if (id != null) {

            Set<GrimPlayer> viewers = SEEN_ARMOR_STANDS.get(grimPlayer.uuid);

            if (viewers != null && !viewers.isEmpty()) {

                List<Equipment> equipmentList = new ArrayList<>();

                if (space == 9) {
                    equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 15))));
                } else if (space == 18) {
                    equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 12))));
                } else if (space == 27) {
                    equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.WOOL, 1, (short) 3))));
                } else {
                    equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(new ItemStack(Material.HAY_BLOCK))));
                }

                WrapperPlayServerEntityEquipment equipmentPacket = new WrapperPlayServerEntityEquipment(
                        id,
                        equipmentList
                );

                for (GrimPlayer all : viewers) {
                    all.user.sendPacket(equipmentPacket);
                }

            }

        }

    }

    public static void sneak(GrimPlayer grimPlayer, boolean isSneaking) {

        Integer entityId = ARMOR_STAND_IDS.get(grimPlayer.uuid);

        if (entityId != null) {

            Set<GrimPlayer> viewers = SEEN_ARMOR_STANDS.get(grimPlayer.uuid);

            if (viewers != null && !viewers.isEmpty()) {

                WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(entityId, List.of(new EntityData(0, EntityDataTypes.BYTE, (byte) (isSneaking ? 0x22 : 0x20)), new EntityData(10, EntityDataTypes.BYTE, KeyConstants.MARKER_FLAG)));

                for (GrimPlayer viewer : viewers) {
                    viewer.user.sendPacket(packet);
                }

            }

        }

    }

    public static void destroy(GrimPlayer grimPlayer, GrimPlayer viewer, boolean removed) {

        Integer entityId = ARMOR_STAND_IDS.get(grimPlayer.uuid);

        if (entityId != null) {

            if (removed) {
                WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);
                viewer.user.sendPacket(packet);
            }

        }

    }

    public static void destroy(UUID uuid) {

        Integer entityId = ARMOR_STAND_IDS.get(uuid);

        if (entityId != null) {

            Set<GrimPlayer> viewers = SEEN_ARMOR_STANDS.get(uuid);

            if (viewers != null && !viewers.isEmpty()) {

                WrapperPlayServerDestroyEntities packet = new WrapperPlayServerDestroyEntities(entityId);

                for (GrimPlayer viewer : viewers) {
                    viewer.user.sendPacket(packet);
                }

            }

        }

    }


}
