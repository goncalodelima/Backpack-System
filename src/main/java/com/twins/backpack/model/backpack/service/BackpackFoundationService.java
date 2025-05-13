package com.twins.core.global.model.backpack.service;


import com.twins.core.global.model.backpack.Backpack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BackpackFoundationService {

    void removeTemporaryCache(UUID uuid);

    boolean containsTemporaryCache(UUID uuid);

    void addTemporaryCache(UUID uuid);

    void put(Backpack backpack);

    CompletableFuture<Boolean> putData(Backpack backpack);

    void update(Backpack backpack);

    CompletableFuture<Boolean> update(Map<UUID, Backpack> backpacks);

    boolean updateOnDisable(Map<UUID, Backpack> backpacks);

    Backpack remove(UUID uuid);

    CompletableFuture<Boolean> removeData(Backpack backpack);

    CompletableFuture<Boolean> removeData(UUID uuid);

    Backpack get(UUID uuid);

    Optional<Backpack> getData(UUID uuid);

    Map<UUID, Backpack> getPendingUpdates();

}
