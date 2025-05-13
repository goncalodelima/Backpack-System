package com.twins.core.global.model.backpack.repository;

import com.minecraftsolutions.database.Database;
import com.minecraftsolutions.database.executor.DatabaseExecutor;
import com.minecraftsolutions.utils.ItemSerializer;
import com.twins.core.CorePlugin;
import com.twins.core.global.model.backpack.Backpack;
import com.twins.core.global.model.backpack.adapter.BackpackAdapter;
import com.twins.core.utils.BukkitUtils;
import com.twins.core.utils.UUIDConverter;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class BackpackRepository implements BackpackFoundationRepository {

    private final Database database;

    private final BackpackAdapter adapter = new BackpackAdapter();

    public BackpackRepository(Database database) {
        this.database = database;
    }

    @Override
    public void setup() {
        try (DatabaseExecutor executor = database.execute()) {
            executor.query("CREATE TABLE IF NOT EXISTS backpack (uuid BINARY(16), contents TEXT, PRIMARY KEY(uuid))")
                    .write();
        }
    }

    @Override
    public CompletableFuture<Boolean> insertOrUpdate(Backpack backpack) {

        return CompletableFuture.supplyAsync(() -> {

            if (backpack.getContents().length == 9 && BukkitUtils.isArrayAllNull(backpack.getContents())) {
                return true;
            }

            try (DatabaseExecutor executor = database.execute()) {
                executor.query("INSERT INTO backpack (uuid, contents) VALUES (?,?) ON DUPLICATE KEY UPDATE contents = VALUES(contents)")
                        .write(statement -> {
                            statement.set(1, UUIDConverter.convert(backpack.getUuid()));
                            statement.set(2, ItemSerializer.itemStackArrayToBase64(backpack.getContents()));
                        });
            }

            return true;
        }, CorePlugin.INSTANCE.getAsyncExecutor()).exceptionally(e -> {
            CorePlugin.INSTANCE.getLogger().log(Level.SEVERE, "Failed to insert backpack user data", e);
            return false;
        });

    }

    @Override
    public CompletableFuture<Boolean> update(Map<UUID, Backpack> backpacks) {
        return CompletableFuture.supplyAsync(() -> {

            try (DatabaseExecutor executor = database.execute()) {
                executor.query("INSERT INTO backpack (uuid, contents) VALUES (?,?) ON DUPLICATE KEY UPDATE contents = VALUES(contents)")
                        .batch(backpacks.values(), ((backpack, statement) -> {
                            ItemStack[] contents = backpack.getContents();
                            statement.set(1, UUIDConverter.convert(backpack.getUuid()));
                            statement.set(2, ItemSerializer.itemStackArrayToBase64(Arrays.copyOf(contents, contents.length)));
                        }));

                return true;
            }

        }, CorePlugin.INSTANCE.getAsyncExecutor()).exceptionally(e -> {
            CorePlugin.INSTANCE.getLogger().log(Level.SEVERE, "Failed to update all backpacks data", e);
            return false;
        });
    }

    @Override
    public boolean updateOnDisable(Map<UUID, Backpack> backpacks) {

        try (DatabaseExecutor executor = database.execute()) {
            executor.query("INSERT INTO backpack (uuid, contents) VALUES (?,?) ON DUPLICATE KEY UPDATE contents = VALUES(contents)")
                    .batch(backpacks.values(), ((backpack, statement) -> {
                        ItemStack[] contents = backpack.getContents();
                        statement.set(1, UUIDConverter.convert(backpack.getUuid()));
                        statement.set(2, ItemSerializer.itemStackArrayToBase64(Arrays.copyOf(contents, contents.length)));
                    }));

            return true;
        } catch (Exception e) {
            CorePlugin.INSTANCE.getLogger().log(Level.SEVERE, "Failed to update all backpacks data on disable", e);
            return false;
        }

    }

    @Override
    public CompletableFuture<Boolean> delete(Backpack backpack) {
        return CompletableFuture.supplyAsync(() -> {

            try (DatabaseExecutor executor = database.execute()) {
                executor.query("DELETE FROM backpack WHERE uuid = ?")
                        .write(statement -> statement.set(1, UUIDConverter.convert(backpack.getUuid())));
            }

            return true;
        }, CorePlugin.INSTANCE.getAsyncExecutor()).exceptionally(e -> {
            CorePlugin.INSTANCE.getLogger().log(Level.SEVERE, "Failed to delete backpack data", e);
            return false;
        });
    }

    @Override
    public CompletableFuture<Boolean> delete(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {

            try (DatabaseExecutor executor = database.execute()) {
                executor.query("DELETE FROM backpack WHERE uuid = ?")
                        .write(statement -> statement.set(1, UUIDConverter.convert(uuid)));
            }

            return true;

        }, CorePlugin.INSTANCE.getAsyncExecutor()).exceptionally(e -> {
            CorePlugin.INSTANCE.getLogger().log(Level.SEVERE, "Failed to delete backpack data", e);
            return false;
        });
    }

    @Override
    public Optional<Backpack> findOne(UUID uuid) {
        try (DatabaseExecutor executor = database.execute()) {
            return executor
                    .query("SELECT * FROM backpack WHERE uuid = ?")
                    .readOne(statement -> statement.set(1, UUIDConverter.convert(uuid)), adapter);
        } catch (Exception e) {
            CorePlugin.INSTANCE.getLogger().log(Level.SEVERE, "Failed to retrieve backpack data", e);
            return Optional.empty();
        }
    }

}
