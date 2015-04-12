package com.gaiagps.iburn.database;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.DefaultValue;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.REAL;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Camp SQL table definition.
 */
public interface CampTable extends PlayaItemTable {

    /** SQL type        Modifiers                   Reference Name            SQL Column Name */
    @DataType(TEXT)                                 String hometown         = "hometown";
}
