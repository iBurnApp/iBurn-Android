package com.gaiagps.iburn.database;

import com.gaiagps.iburn.PlayaClient;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

import java.util.ArrayList;

/**
 * SQL Database definition.
 *
 * Created by davidbrodsky on 7/28/14.
 */
@Database(version = PlayaClient.DATABASE_VERSION)
public class PlayaDatabase {

    /** Table Definition        Reference Name                        SQL Tablename */
    @Table(ArtTable.class)      public static final String  ART     = "art";
    @Table(CampTable.class)     public static final String  CAMPS   = "camps";
    @Table(EventTable.class)    public static final String  EVENTS  = "events";
    @Table(UserPoiTable.class)  public static final String  POIS    = "pois";

    static ArrayList<String> ALL_TABLES = new ArrayList<String>() {{ add(CAMPS); add(ART); add(EVENTS); add(POIS); }};
}