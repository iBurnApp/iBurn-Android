-- Add POI table
CREATE TABLE "pois" (
       "res_id"     INTEGER,
       "_id"        INTEGER PRIMARY KEY AUTOINCREMENT,
       "name"       TEXT NOT NULL,
       "desc"       TEXT,
       "url"        TEXT,
       "contact"    TEXT,
       "p_addr"     TEXT,
       "p_id"       TEXT,
       "lat"        REAL,
       "lon"        REAL,
       "fav"        INTEGER DEFAULT 0);