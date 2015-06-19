package com.gaiagps.iburn.database;

import net.simonvt.schematic.annotation.DataType;

import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Camp SQL table definition.
 */
public interface CampTable extends PlayaItemTable {

    /** SQL type        Modifiers                   Reference Name            SQL Column Name */
    @DataType(TEXT)                                 String hometown         = "hometown";
}
