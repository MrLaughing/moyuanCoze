package com.mrlaughing.moyuan.ui.study

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.util.formatMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 书目列表 Adapter：每行显示路径颜色条 + 书名 + 最后阅读日期 + 累计时长
 */
class BookListAdapter : RecyclerView.Adapter<BookListAdapter.ViewHolder>() {

    private var books: List<BookItem> = emptyList()

    // 路径颜色映射（按路径分配不同暖色）
    private val pathColors = listOf(
        0xFF8B7E6A.toInt(),  // 积墨 - 中墨色
        0xFFB89878.toInt(),  // 秉烛 - 暖棕色
        0xFFA8B89A.toInt(),  // 岁寒 - 淡绿色
        0xFFC49A6C.toInt(),  // 寻芳 - 橙黄色
        0xFFD4C9B8.toInt()   // 隐藏 - 浅灰色
    )

    fun submitList(newBooks: List<BookItem>) {
        books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(books[position], position)
    }

    override fun getItemCount(): Int = books.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pathColorView: View = itemView.findViewById(R.id.view_path_color)
        private val bookTitle: TextView = itemView.findViewById(R.id.text_book_title)
        private val lastReadDate: TextView = itemView.findViewById(R.id.text_book_last_read)
        private val bookTime: TextView = itemView.findViewById(R.id.text_book_time)

        // 路径颜色（与布局中的默认值）
        private val pathColors = listOf(
            0xFF8B7E6A.toInt(),  // 积墨
            0xFFB89878.toInt(),  // 秉烛
            0xFFA8B89A.toInt(),  // 岁寒
            0xFFC49A6C.toInt(),  // 寻芳
            0xFFD4C9B8.toInt()   // 隐藏
        )

        fun bind(item: BookItem, position: Int) {
            bookTitle.text = item.title

            // 设置路径颜色条（按位置循环分配）
            val colorIndex = position % pathColors.size
            pathColorView.setBackgroundColor(pathColors[colorIndex])

            // 设置累计时长
            bookTime.text = if (item.totalReadMinutes > 0) {
                item.totalReadMinutes.formatMinutes()
            } else {
                ""
            }

            // 设置最后阅读日期
            item.lastReadDate?.let { dateStr ->
                try {
                    val ld = LocalDate.parse(dateStr)
                    val formatter = DateTimeFormatter.ofPattern("M月d日阅读")
                    lastReadDate.text = ld.format(formatter)
                    lastReadDate.visibility = View.VISIBLE
                } catch (_: Exception) {
                    lastReadDate.visibility = View.GONE
                }
            } ?: run {
                lastReadDate.visibility = View.GONE
            }
        }
    }
}
