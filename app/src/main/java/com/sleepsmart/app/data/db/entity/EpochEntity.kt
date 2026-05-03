package com.sleepsmart.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epochs",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionId")]
)
data class EpochEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val index: Int,
    val startMs: Long,
    val durationMs: Long,
    val stage: String,           // SleepStage enum name
    val confidence: Float,
    val rms: Float,
    val movementIndex: Float,
    val breathRate: Float?,
    val breathVariability: Float?
)
