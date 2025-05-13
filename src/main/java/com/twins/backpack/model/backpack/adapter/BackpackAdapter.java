package com.twins.core.global.model.backpack.adapter;

import com.minecraftsolutions.database.adapter.DatabaseAdapter;
import com.minecraftsolutions.database.executor.DatabaseQuery;
import com.minecraftsolutions.utils.ItemSerializer;
import com.twins.core.global.model.backpack.Backpack;
import com.twins.core.utils.UUIDConverter;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class BackpackAdapter implements DatabaseAdapter<Backpack> {

    @Override
    public Backpack adapt(DatabaseQuery databaseQuery) throws SQLException {

        UUID uuid = UUIDConverter.convert((byte[]) databaseQuery.get("uuid"));

        ItemStack[] contents;

        try {
            contents = ItemSerializer.itemStackArrayFromBase64((String) databaseQuery.get("contents"));
        } catch (IOException e) {
            return null;
        }

        return new Backpack(uuid, contents);
    }

}
