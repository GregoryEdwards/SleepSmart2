package com.sleepsmart.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sleepsmart.app.data.db.entity.DisturbanceEntity
import com.sleepsmart.app.data.db.entity.EpochEntity
import com.sleepsmart.app.data.db.entity.SessionEntity

@Database(
    entities = [SessionEntity::class, EpochEntity::class, DisturbanceEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SleepDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun epochDao(): EpochDao
    abstract fun disturbanceDao(): DisturbanceDao
}
