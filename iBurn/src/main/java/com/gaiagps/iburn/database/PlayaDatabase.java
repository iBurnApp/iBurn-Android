package com.gaiagps.iburn.database;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

/**
 * SQL Database definition.
 *
 * Created by davidbrodsky on 7/28/14.
 */
@Database(version = PlayaDatabase.VERSION)
public class PlayaDatabase {
    public static final int VERSION = 1;

    /** Table Definition        Reference Name                        SQL Tablename */
    @Table(ArtTable.class)      public static final String  ART     = "art";
    @Table(CampTable.class)     public static final String  CAMPS   = "camps";
    @Table(EventTable.class)    public static final String  EVENTS  = "events";
}