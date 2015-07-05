package com.gaiagps.iburn.database;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

import java.util.ArrayList;

/**
 * SQL Database definition.
 *
 * Created by davidbrodsky on 7/28/14.
 */
@Database(version = (int) DataProvider.BUNDLED_DATABASE_VERSION) // Vulnerable to Year 2038 bug
public class PlayaDatabase {

    /** Table Definition        Reference Name                        SQL Tablename */
    @Table(ArtTable.class)      public static final String  ART     = "art";
    @Table(CampTable.class)     public static final String  CAMPS   = "camps";
    @Table(EventTable.class)    public static final String  EVENTS  = "events";
    @Table(UserPoiTable.class)  public static final String  POIS    = "pois";

    public static final ArrayList<String> ALL_TABLES = new ArrayList<String>() {{ add(CAMPS); add(ART); add(EVENTS); add(POIS); }};
}