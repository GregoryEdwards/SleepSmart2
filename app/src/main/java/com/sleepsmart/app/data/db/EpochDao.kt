package com.sleepsmart.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.sleepsmart.app.data.db.entity.EpochEntity

@Dao
interface EpochDao {
    @Insert
    suspend fun insertAll(epochs: List<EpochEntity>)

    @Query("SELECT * FROM epochs WHERE sessionId = :sid ORDER BY `index` ASC")
    suspend fun forSession(sid: String): List<EpochEntity>

    @Query("SELECT COUNT(*) FROM epochs WHERE sessionId = :sid")
    suspend fun countForSession(sid: String): Int
}
