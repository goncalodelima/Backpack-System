package com.twins.core.global.model.backpack.repository;

import com.twins.core.global.model.backpack.Backpack;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface BackpackFoundationRepository {

    void setup();

    CompletableFuture<Boolean> insertOrUpdate(Backpack backpack);

    CompletableFuture<Boolean> update(Map<UUID, Backpack> backpacks);

    boolean updateOnDisable(Map<UUID, Backpack> backpacks);

    CompletableFuture<Boolean> delete(Backpack backpack);

    CompletableFuture<Boolean> delete(UUID uuid);

    Optional<Backpack> findOne(UUID uuid);

}
