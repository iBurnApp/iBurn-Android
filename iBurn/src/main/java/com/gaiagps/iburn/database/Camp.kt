package com.gaiagps.iburn.database

//import com.gaiagps.iburn.database.Camp.Companion.TableName
//import com.gaiagps.iburn.database.PlayaItem.Companion.ColFavorite
//import com.gaiagps.iburn.database.PlayaItem.Companion.ColName


/**
 * Created by dbro on 6/6/17.
 */
//@Entity(tableName = TableName)
//class Camp(
//        id:             Int = 0,
//        name:           String,
//        description:    String,
//        url:            String,
//        contact:        String,
//        playaAddress:   String,
//        playaId:        String,
//        location:       Location,
//        isFavorite:     Boolean,
//
//        @ColumnInfo(name = ColHometown)     val hometown: String)
//
//    : PlayaItem(id, name, description, url, contact, playaAddress, playaId, location, isFavorite) {
//
//    companion object {
//        const val TableName = "camps"
//
//        const val ColHometown = "hometown"
//    }
//}

//@Dao
//interface CampDao {
//
//    @Query("SELECT * FROM $TableName")
//    fun getAll(): Flowable<List<Camp>>
//
//    @Query("SELECT * FROM $TableName WHERE $ColFavorite = 1")
//    fun getFavorites(): Flowable<List<Camp>>
//
//    // TODO : 'p0' is used vs 'name' b/c Kotlin isn't preserving function parameter names properly
//    // https://youtrack.jetbrains.com/issue/KT-17959
////    @Query("SELECT * FROM $TableName WHERE $ColName LIKE :p0")
////    fun findByName(name: String): Flowable<List<Camp>>
//
//    @Insert
//    fun insert(vararg camps: Camp)
//
//    @Update
//    fun update(vararg camps: Camp)
//}