package com.zelun.screenshotgraveyard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "screenshots", indices = [Index(value = ["sha256"], unique = true)])
data class ScreenshotEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "rowid") val id: Long = 0,
    val imageName: String,
    val sha256: String,
    val title: String,
    val extractedText: String = "",
    val tags: String = "",
    val titleEdited: Boolean = false,
    val tagsEdited: Boolean = false,
    val status: String = "PENDING",
    val ocrState: String = "QUEUED",
    val createdAt: Long = System.currentTimeMillis(),
    val bytes: Long,
    val isDemo: Boolean = false,
)

@Fts4(contentEntity = ScreenshotEntity::class)
@Entity(tableName = "screenshots_fts")
data class ScreenshotFts(val title: String, val extractedText: String, val tags: String)

@Dao
interface ScreenshotDao {
    @Query("SELECT * FROM screenshots ORDER BY createdAt DESC, rowid DESC")
    fun observeAll(): Flow<List<ScreenshotEntity>>

    @Query("SELECT rowid FROM screenshots_fts WHERE screenshots_fts MATCH :expression")
    fun searchIds(expression: String): Flow<List<Long>>

    @Query("SELECT * FROM screenshots WHERE rowid = :id")
    suspend fun get(id: Long): ScreenshotEntity?

    @Query("SELECT * FROM screenshots WHERE sha256 = :hash LIMIT 1")
    suspend fun getByHash(hash: String): ScreenshotEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(screenshot: ScreenshotEntity): Long

    @Query("UPDATE screenshots SET status = :next WHERE rowid = :id AND status = :expected")
    suspend fun transition(id: Long, expected: String, next: String): Int

    @Query("UPDATE screenshots SET title = :title, tags = :tags, titleEdited = 1, tagsEdited = 1 WHERE rowid = :id")
    suspend fun edit(id: Long, title: String, tags: String)

    @Query("UPDATE screenshots SET ocrState = :state WHERE rowid = :id")
    suspend fun setOcrState(id: Long, state: String)

    @Query("UPDATE screenshots SET extractedText = :text, title = CASE WHEN titleEdited = 0 AND title = :originalTitle THEN :suggestedTitle ELSE title END, tags = CASE WHEN tagsEdited = 0 AND TRIM(tags) = '' THEN :suggestedTags ELSE tags END, ocrState = 'READY' WHERE rowid = :id")
    suspend fun completeOcr(id: Long, text: String, originalTitle: String, suggestedTitle: String, suggestedTags: String)

    @Query("SELECT rowid FROM screenshots WHERE ocrState IN ('QUEUED', 'PROCESSING')")
    suspend fun unfinishedIds(): List<Long>

    @Delete suspend fun delete(screenshot: ScreenshotEntity)
}

@Database(entities = [ScreenshotEntity::class, ScreenshotFts::class], version = 1, exportSchema = true)
abstract class ArchiveDatabase : RoomDatabase() { abstract fun screenshots(): ScreenshotDao }
