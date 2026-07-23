package com.mrlaughing.moyuan.ui.study

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.util.formatMinutes
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 书目列表 Adapter：状态色 + 书名 + 最后阅读日期 + 累计时长。
 */
class BookListAdapter : ListAdapter<BookItem, BookListAdapter.ViewHolder>(BookDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pathColorView: View = itemView.findViewById(R.id.view_path_color)
        private val bookTitle: TextView = itemView.findViewById(R.id.text_book_title)
        private val lastReadDate: TextView = itemView.findViewById(R.id.text_book_last_read)
        private val bookTime: TextView = itemView.findViewById(R.id.text_book_time)
        private val bookProgress: TextView = itemView.findViewById(R.id.text_book_progress)

        fun bind(item: BookItem) {
            bookTitle.text = item.title

            val statusColor = when {
                item.progressPercent >= 90 -> ContextCompat.getColor(itemView.context, R.color.accent_green)
                item.progressPercent > 0 -> ContextCompat.getColor(itemView.context, R.color.accent_warm)
                else -> ContextCompat.getColor(itemView.context, R.color.border)
            }
            pathColorView.setBackgroundColor(statusColor)

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

            // 设置阅读进度
            if (item.progressPercent > 0) {
                bookProgress.text = "阅读进度 ${item.progressPercent}%"
                bookProgress.visibility = View.VISIBLE
            } else {
                bookProgress.visibility = View.GONE
            }
        }
    }

    private object BookDiffCallback : DiffUtil.ItemCallback<BookItem>() {
        override fun areItemsTheSame(oldItem: BookItem, newItem: BookItem): Boolean =
            oldItem.title == newItem.title

        override fun areContentsTheSame(oldItem: BookItem, newItem: BookItem): Boolean =
            oldItem == newItem
    }
}
