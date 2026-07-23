package com.mrlaughing.moyuan.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mrlaughing.moyuan.data.local.db.entity.BookTrackingEntity
import kotlinx.coroutines.flow.Flow

/**
 * 书目追踪 DAO
 */
@Dao
interface BookTrackingDao {

    /**
     * 获取所有追踪书目
     */
    @Query("SELECT * FROM book_tracking ORDER BY lastReadDate DESC")
    fun getAllBooks(): Flow<List<BookTrackingEntity>>

    /**
     * 按bookId查询书目
     */
    @Query("SELECT * FROM book_tracking WHERE bookId = :bookId")
    fun getBookById(bookId: String): Flow<BookTrackingEntity?>

    /**
     * 插入书目记录，冲突时替换
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(entity: BookTrackingEntity)

    /**
     * 更新书目记录
     */
    @Update
    suspend fun updateBook(entity: BookTrackingEntity)

    /**
     * 删除书目记录
     */
    @Query("DELETE FROM book_tracking WHERE bookId = :bookId")
    suspend fun deleteBook(bookId: String)

    /**
     * 更新单本书的阅读进度（按 bookId 匹配）
     */
    @Query("UPDATE book_tracking SET progressPercent = :progressPercent WHERE bookId = :bookId")
    suspend fun updateBookProgress(bookId: String, progressPercent: Int)

    /**
     * 获取已读书目数（进度 >= 90%）
     */
    @Query("SELECT COUNT(*) FROM book_tracking WHERE progressPercent >= 90")
    fun getCompletedBookCount(): Flow<Int>

    /**
     * 获取最近阅读的N本书
     */
    @Query("SELECT * FROM book_tracking ORDER BY lastReadDate DESC LIMIT :limit")
    fun getRecentBooks(limit: Int): Flow<List<BookTrackingEntity>>

    /**
     * 获取正在阅读的书目（进度 < 90%）
     */
    @Query("SELECT * FROM book_tracking WHERE progressPercent < 90 ORDER BY lastReadDate DESC")
    fun getReadingBooks(): Flow<List<BookTrackingEntity>>
}
