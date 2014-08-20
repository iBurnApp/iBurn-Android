package com.gaiagps.iburn.database;

import com.gaiagps.iburn.R;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.DefaultValue;
import net.simonvt.schematic.annotation.NotNull;
import net.simonvt.schematic.annotation.PrimaryKey;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.REAL;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Art SQL Table definition.
 */
public interface UserPoiTable extends PlayaItemTable{

    @DataType(TEXT)                                 String drawableResId    = "res_id";

    /** Schematic 0.5.1 does not support inheritance. Remove the following fields on
     * next release
     */
    @DataType(INTEGER)  @PrimaryKey@AutoIncrement   String id               = "_id";
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
