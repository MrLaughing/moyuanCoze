package com.mrlaughing.moyuan.ui.study

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.study.NoteSnapshotItem

/**
 * 书摘卡片列表适配器
 */
class NoteAdapter : ListAdapter<NoteSnapshotItem, NoteAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<NoteSnapshotItem>() {
            override fun areItemsTheSame(oldItem: NoteSnapshotItem, newItem: NoteSnapshotItem) =
                oldItem.text == newItem.text && oldItem.bookTitle == newItem.bookTitle

            override fun areContentsTheSame(oldItem: NoteSnapshotItem, newItem: NoteSnapshotItem) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val content: TextView = itemView.findViewById(R.id.text_note_content)
        private val source: TextView = itemView.findViewById(R.id.text_note_source)

        fun bind(item: NoteSnapshotItem) {
            content.text = item.text
            source.text = buildString {
                append("\u2014\u2014 \u300a${item.bookTitle}\u300b") // ——《书名》
                if (!item.chapter.isNullOrBlank()) {
                    append(" \u00b7 ${item.chapter}")
                }
            }
        }
    }
}
