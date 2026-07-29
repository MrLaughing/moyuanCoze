package com.mrlaughing.moyuan.ui.study

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.study.ShelfCoverItem

/**
 * 书架封面横向列表适配器
 */
class ShelfBookAdapter : ListAdapter<ShelfCoverItem, ShelfBookAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<ShelfCoverItem>() {
            override fun areItemsTheSame(oldItem: ShelfCoverItem, newItem: ShelfCoverItem) =
                oldItem.bookId == newItem.bookId

            override fun areContentsTheSame(oldItem: ShelfCoverItem, newItem: ShelfCoverItem) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shelf_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cover: ImageView = itemView.findViewById(R.id.image_cover)
        private val title: TextView = itemView.findViewById(R.id.text_title)
        private val finishedBadge: TextView = itemView.findViewById(R.id.text_finished_badge)

        fun bind(item: ShelfCoverItem) {
            title.text = item.title
            finishedBadge.visibility = if (item.finished) View.VISIBLE else View.GONE
            val radius = itemView.resources.getDimensionPixelSize(R.dimen.book_cover_radius)
            Glide.with(cover)
                .load(item.cover)
                .transform(CenterCrop(), RoundedCorners(radius))
                .placeholder(R.drawable.bg_book_cover)
                .into(cover)
        }
    }
}
