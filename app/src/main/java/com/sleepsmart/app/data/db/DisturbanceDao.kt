package com.sleepsmart.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sleepsmart.app.data.db.entity.DisturbanceEntity

@Dao
interface DisturbanceDao {
    @Insert
    suspend fun insertAll(items: List<DisturbanceEntity>)

    @Query("SELECT * FROM disturbances WHERE sessionId = :sid ORDER BY atMs ASC")
    suspend fun forSession(sid: String): List<DisturbanceEntity>

    @Query("SELECT COUNT(*) FROM disturbances WHERE sessionId = :sid")
    suspend fun countForSession(sid: String): Int
}
