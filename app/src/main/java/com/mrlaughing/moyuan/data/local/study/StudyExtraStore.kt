package com.mrlaughing.moyuan.data.local.study

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 书案富数据快照
 * 微信读书 API 返回的展示型数据（书架封面/阅读偏好/最爱书/勋章/书摘），
 * 不参与花园引擎计算，以 JSON 快照形式持久化，避免 Room 迁移成本。
 */
@JsonClass(generateAdapter = true)
data class StudyExtraSnapshot(
    @Json(name = "shelfBooks") val shelfBooks: List<ShelfCoverItem> = emptyList(),
    @Json(name = "shelfTotal") val shelfTotal: Int = 0,
    @Json(name = "preferCategories") val preferCategories: List<PreferCategoryItem> = emptyList(),
    @Json(name = "preferCategoryWord") val preferCategoryWord: String? = null,
    @Json(name = "preferTimeWord") val preferTimeWord: String? = null,
    @Json(name = "preferAuthors") val preferAuthors: List<PreferAuthorItem> = emptyList(),
    @Json(name = "favoriteBooks") val favoriteBooks: List<FavoriteBookItem> = emptyList(),
    @Json(name = "medals") val medals: List<MedalSnapshotItem> = emptyList(),
    @Json(name = "notes") val notes: List<NoteSnapshotItem> = emptyList(),
    @Json(name = "totalNoteCount") val totalNoteCount: Int = 0,
    @Json(name = "rankText") val rankText: String? = null,
    @Json(name = "updatedAt") val updatedAt: Long = 0
)

@JsonClass(generateAdapter = true)
data class ShelfCoverItem(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "author") val author: String = "",
    @Json(name = "cover") val cover: String = "",
    @Json(name = "finished") val finished: Boolean = false,
    @Json(name = "readUpdateTime") val readUpdateTime: Long = 0
)

@JsonClass(generateAdapter = true)
data class PreferCategoryItem(
    @Json(name = "title") val title: String = "",
    @Json(name = "readingSeconds") val readingSeconds: Long = 0,
    @Json(name = "count") val count: Int = 0
)

@JsonClass(generateAdapter = true)
data class PreferAuthorItem(
    @Json(name = "name") val name: String = "",
    @Json(name = "count") val count: Int = 0,
    @Json(name = "readTimeText") val readTimeText: String = ""
)

@JsonClass(generateAdapter = true)
data class FavoriteBookItem(
    @Json(name = "bookId") val bookId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "author") val author: String = "",
    @Json(name = "cover") val cover: String = "",
    @Json(name = "readSeconds") val readSeconds: Long = 0,
    @Json(name = "tags") val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class MedalSnapshotItem(
    @Json(name = "name") val name: String = "",
    @Json(name = "hint") val hint: String = "",
    @Json(name = "displayText") val displayText: String = "",
    @Json(name = "level") val level: Int = 0
)

@JsonClass(generateAdapter = true)
data class NoteSnapshotItem(
    @Json(name = "bookTitle") val bookTitle: String = "",
    @Json(name = "text") val text: String = "",
    @Json(name = "chapter") val chapter: String? = null,
    @Json(name = "createTime") val createTime: Long = 0
)

/**
 * 书案富数据存储
 * filesDir/study_extra.json 单文件 JSON 快照，启动时懒加载，同步后覆盖写入。
 */
@Singleton
class StudyExtraStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(StudyExtraSnapshot::class.java)
    private val file: File get() = File(context.filesDir, "study_extra.json")

    private val _snapshot = MutableStateFlow<StudyExtraSnapshot?>(null)
    val snapshot: StateFlow<StudyExtraSnapshot?> = _snapshot.asStateFlow()

    @Volatile
    private var loaded = false

    /** 懒加载磁盘快照（幂等，可多次调用） */
    suspend fun ensureLoaded() {
        if (loaded) return
        withContext(Dispatchers.IO) {
            synchronized(this@StudyExtraStore) {
                if (loaded) return@synchronized
                loaded = true
                runCatching {
                    if (file.exists()) {
                        adapter.fromJson(file.readText())?.let { _snapshot.value = it }
                    }
                }
            }
        }
    }

    /** 保存快照：写盘 + 更新内存 Flow */
    suspend fun save(snapshot: StudyExtraSnapshot) {
        withContext(Dispatchers.IO) {
            runCatching {
                val tmp = File(context.filesDir, "study_extra.json.tmp")
                tmp.writeText(adapter.toJson(snapshot))
                tmp.renameTo(file)
            }
        }
        loaded = true
        _snapshot.value = snapshot
    }
}
