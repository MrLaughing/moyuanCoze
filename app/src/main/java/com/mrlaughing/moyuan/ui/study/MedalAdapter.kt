package com.mrlaughing.moyuan.ui.study

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.study.MedalSnapshotItem

/**
 * 阅读勋章横向列表适配器
 */
class MedalAdapter : ListAdapter<MedalSnapshotItem, MedalAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MedalSnapshotItem>() {
            override fun areItemsTheSame(oldItem: MedalSnapshotItem, newItem: MedalSnapshotItem) =
                oldItem.name == newItem.name

            override fun areContentsTheSame(oldItem: MedalSnapshotItem, newItem: MedalSnapshotItem) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medal, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val value: TextView = itemView.findViewById(R.id.text_medal_value)
        private val name: TextView = itemView.findViewById(R.id.text_medal_name)

        fun bind(item: MedalSnapshotItem) {
            value.text = when {
                item.displayText.isNotBlank() -> item.displayText
                item.level > 0 -> "Lv.${item.level}"
                else -> "\u2726" // ✦
            }
            name.text = item.name
        }
    }
}
