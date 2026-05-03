package com.sleepsmart.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val startedAt: Long,
    val endedAt: Long?,
    val targetWakeAt: Long,
    val wakeWindowMinutes: Int,
    val firedAt: Long?,
    val score: Int?,
    val timeAsleepMinutes: Int?,
    val disturbances: Int = 0
)
