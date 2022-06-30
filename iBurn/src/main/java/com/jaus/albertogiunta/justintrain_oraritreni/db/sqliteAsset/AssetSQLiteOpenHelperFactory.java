package com.jaus.albertogiunta.justintrain_oraritreni.db.sqliteAsset;

import androidx.sqlite.db.SupportSQLiteOpenHelper;
import android.database.DefaultDatabaseErrorHandler;

/**
 * Implements {@link SupportSQLiteOpenHelper.Factory} using the SQLite implementation in the
 * framework.
 */
@SuppressWarnings("unused")
public class AssetSQLiteOpenHelperFactory implements SupportSQLiteOpenHelper.Factory {

    @Override
    public SupportSQLiteOpenHelper create(SupportSQLiteOpenHelper.Configuration configuration) {
        DefaultDatabaseErrorHandler errorHandler = new DefaultDatabaseErrorHandler();
        return new AssetSQLiteOpenHelper(
                configuration.context, configuration.name, null,
                errorHandler, configuration.callback
        );
    }
}
