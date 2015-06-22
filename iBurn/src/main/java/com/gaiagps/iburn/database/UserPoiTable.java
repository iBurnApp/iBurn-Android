package com.gaiagps.iburn.database;

import net.simonvt.schematic.annotation.DataType;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;

/**
 * Art SQL Table definition.
 */
public interface UserPoiTable extends PlayaItemTable {

    /**
     * Possible values for drawableResId
     * corresponding to icons used in the UI
     */
    int HEART = 0;
    int STAR  = 1;
    int BIKE  = 2;
    int HOME  = 3;

    @DataType(INTEGER) String drawableResId = "res_id";
}
