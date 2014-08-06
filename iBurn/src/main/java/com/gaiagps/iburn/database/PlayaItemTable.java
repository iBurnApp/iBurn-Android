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
 * Created by davidbrodsky on 7/28/14.
 */
public interface PlayaItemTable {

    /** SQL type        Modifiers                   Reference Name            SQL Column Name */
    @DataType(INTEGER)  @PrimaryKey @AutoIncrement  String id               = "_id";
    @DataType(TEXT)     @NotNull                    String name             = "name";
    @DataType(TEXT)                                 String description      = "desc";
    @DataType(TEXT)                                 String url              = "url";
    @DataType(TEXT)                                 String contact          = "contact";
    @DataType(TEXT)                                 String playaAddress     = "p_addr";
    @DataType(INTEGER)                              String playaId          = "p_id";
    @DataType(REAL)                                 String latitude         = "lat";
    @DataType(REAL)                                 String longitude        = "lon";
    @DataType(INTEGER)  @DefaultValue("0")          String favorite         = "fav";
}
