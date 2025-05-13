package com.twins.core.global.model.backpack.service;

import com.minecraftsolutions.database.Database;
import com.twins.core.global.model.backpack.Backpack;
import com.twins.core.global.model.backpack.repository.BackpackFoundationRepository;
import com.twins.core.global.model.backpack.repository.BackpackRepository;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class BackpackService implements BackpackFoundationService {

    private final Map<UUID, Backpack> cache = new ConcurrentHashMap<>();

    private final Map<UUID, Backpack> pendingUpdates = new ConcurrentHashMap<>();

    private final Set<UUID> temporaryCache = new HashSet<>();

    private final BackpackFoundationRepository backpackRepository;

    public BackpackService(Database database) {
        backpackRepository = new BackpackRepository(database);
        backpackRepository.setup();
    }

    @Override
    public void addTemporaryCache(UUID uuid) {
        temporaryCache.add(uuid);
    }

    @Override
    public void removeTemporaryCache(UUID uuid) {
        temporaryCache.remove(uuid);
    }

    @Override
    public boolean containsTemporaryCache(UUID uuid) {
        return temporaryCache.contains(uuid);
    }

    @Override
    public void put(Backpack backpack) {
        cache.put(backpack.getUuid(), backpack);
    }

    @Override
    public CompletableFuture<Boolean> putData(Backpack backpack) {
        return backpackRepository.insertOrUpdate(backpack);
    }

    @Override
    public void update(Backpack backpack) {
        pendingUpdates.put(backpack.getUuid(), backpack);
    }

    @Override
    public CompletableFuture<Boolean> update(Map<UUID, Backpack> backpacks) {
        return backpackRepository.update(backpacks);
    }

    @Override
    public boolean updateOnDisable(Map<UUID, Backpack> backpacks) {
        return backpackRepository.updateOnDisable(backpacks);
    }

    @Override
    public Backpack remove(UUID uuid) {
        return cache.remove(uuid);
    }

    @Override
    public CompletableFuture<Boolean> removeData(Backpack backpack) {
        return backpackRepository.delete(backpack);
    }

    @Override
    public CompletableFuture<Boolean> removeData(UUID uuid) {
        return backpackRepository.delete(uuid);
    }

    @Override
    public Backpack get(UUID uuid) {
        return cache.get(uuid);
    }

    @Override
    public Optional<Backpack> getData(UUID uuid) {
        return backpackRepository.findOne(uuid);
    }

    @Override
    public Map<UUID, Backpack> getPendingUpdates() {
        return pendingUpdates;
    }


}
