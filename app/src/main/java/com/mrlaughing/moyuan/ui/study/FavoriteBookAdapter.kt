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
import com.mrlaughing.moyuan.data.local.study.FavoriteBookItem
import com.mrlaughing.moyuan.util.formatMinutes

/**
 * 读得最久的书列表适配器
 */
class FavoriteBookAdapter : ListAdapter<FavoriteBookItem, FavoriteBookAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<FavoriteBookItem>() {
            override fun areItemsTheSame(oldItem: FavoriteBookItem, newItem: FavoriteBookItem) =
                oldItem.bookId == newItem.bookId

            override fun areContentsTheSame(oldItem: FavoriteBookItem, newItem: FavoriteBookItem) =
                oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rank: TextView = itemView.findViewById(R.id.text_rank)
        private val cover: ImageView = itemView.findViewById(R.id.image_cover)
        private val title: TextView = itemView.findViewById(R.id.text_title)
        private val author: TextView = itemView.findViewById(R.id.text_author)
        private val readTime: TextView = itemView.findViewById(R.id.text_read_time)

        fun bind(item: FavoriteBookItem, position: Int) {
            rank.text = (position + 1).toString().padStart(2, '0')
            title.text = item.title
            author.text = item.author
            readTime.text = (item.readSeconds / 60).toInt().formatMinutes()
            val radius = (itemView.resources.displayMetrics.density * 4).toInt()
            Glide.with(cover)
                .load(item.cover)
                .transform(CenterCrop(), RoundedCorners(radius))
                .placeholder(R.drawable.bg_book_cover)
                .into(cover)
        }
    }
}
