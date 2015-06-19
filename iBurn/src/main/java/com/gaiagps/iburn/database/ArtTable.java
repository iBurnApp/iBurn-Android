package com.gaiagps.iburn.database;

import net.simonvt.schematic.annotation.DataType;

import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Art SQL Table definition.
 */
public interface ArtTable extends PlayaItemTable {

    /** SQL type        Modifiers                   Reference Name            SQL Column Name */
    @DataType(TEXT)                                 String artist           = "artist";
    @DataType(TEXT)                                 String artistLoc        = "a_loc";
    @DataType(TEXT)                                 String imageUrl         = "i_url";
}
