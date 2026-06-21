package com.mrlaughing.moyuan.ui.study

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.util.formatMinutes

/**
 * 书目列表 Adapter：每行显示书名 + 累计时长
 */
class BookListAdapter : RecyclerView.Adapter<BookListAdapter.ViewHolder>() {

    private var books: List<BookItem> = emptyList()

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
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookTitle: TextView = itemView.findViewById(R.id.text_book_title)
        private val bookTime: TextView = itemView.findViewById(R.id.text_book_time)

        fun bind(item: BookItem) {
            bookTitle.text = item.title
            bookTime.text = if (item.totalReadMinutes > 0) {
                item.totalReadMinutes.formatMinutes()
            } else {
                item.lastReadDate?.let { date ->
                    try {
                        val ld = java.time.LocalDate.parse(date)
                        "${ld.monthValue}月${ld.dayOfMonth}日"
                    } catch (_: Exception) { "" }
                } ?: ""
            }
        }
    }
}
