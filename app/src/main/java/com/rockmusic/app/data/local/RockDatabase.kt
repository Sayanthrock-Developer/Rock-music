package com.rockmusic.app.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String,
    val artworkUri: String?,
    val sourceType: String,
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val addedAt: Long = System.currentTimeMillis(),
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY addedAt DESC")
    fun observeAll(): Flow<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(items: List<TrackEntity>)
}

@Database(
    entities = [TrackEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class RockDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
}
