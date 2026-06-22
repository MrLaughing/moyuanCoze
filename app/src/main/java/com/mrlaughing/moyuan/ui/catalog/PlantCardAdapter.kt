package com.mrlaughing.moyuan.ui.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.render.PlantImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图鉴植物卡片 Adapter
 * 使用 ListAdapter + DiffUtil 替代 notifyDataSetChanged，避免闪烁
 *
 * 已解锁：显示植物图 + 名字 + 等级 + 稀有度星标
 * 未解锁：灰色剪影 + "???"
 */
class PlantCardAdapter(
    private val onPlantClick: (CatalogPlantItem) -> Unit
) : ListAdapter<CatalogPlantItem, PlantCardAdapter.ViewHolder>(PlantDiffCallback()) {

    private var fragment: Fragment? = null

    private val unlockedNameColor = 0xFF2C2416.toInt()
    private val unlockedLevelColor = 0xFF78716C.toInt()
    private val unlockedStarsColor = 0xFFA89F91.toInt()
    private val lockedNameColor = 0xFFA89F91.toInt()
    private val lockedLevelColor = 0xFFA89F91.toInt()
    private val lockedStarsColor = 0xFFD4C9B8.toInt()

    fun setFragment(fragment: Fragment) {
        this.fragment = fragment
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        fragment = null
    }

    private fun getLevelName(level: Int): String {
        return when (level) {
            1 -> "墨芽"
            2 -> "墨枝"
            3 -> "墨苞"
            4 -> "墨花"
            5 -> "墨韵"
            else -> ""
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantImage: ImageView = itemView.findViewById(R.id.image_plant)
        private val imageGradient: View = itemView.findViewById(R.id.view_image_gradient)
        private val plantName: TextView = itemView.findViewById(R.id.text_plant_name)
        private val plantLevel: TextView = itemView.findViewById(R.id.text_plant_level)
        private val plantStars: TextView = itemView.findViewById(R.id.text_plant_stars)

        fun bind(item: CatalogPlantItem) {
            if (item.isUnlocked) {
                loadPlantImage(item)
                plantName.text = item.name
                val levelName = getLevelName(item.level)
                plantLevel.text = "Lv.${item.level} $levelName"
                plantStars.text = "\u2605".repeat(item.rarity) + "\u2606".repeat(5 - item.rarity)

                plantName.setTextColor(unlockedNameColor)
                plantLevel.setTextColor(unlockedLevelColor)
                plantStars.setTextColor(unlockedStarsColor)

                itemView.setBackgroundResource(R.drawable.bg_card_unlocked)
                imageGradient.setBackgroundResource(R.drawable.bg_card_image_gradient_unlocked)

                itemView.setOnClickListener {
                    try { onPlantClick(item) } catch (e: Exception) {
                        android.util.Log.e("PlantCardAdapter", "点击植物卡片失败", e)
                    }
                }
            } else {
                loadSilhouette(item)
                plantName.text = "???"
                plantLevel.text = ""
                plantStars.text = "\u2605".repeat(item.rarity) + "\u2606".repeat(5 - item.rarity)

                plantName.setTextColor(lockedNameColor)
                plantLevel.setTextColor(lockedLevelColor)
                plantStars.setTextColor(lockedStarsColor)

                itemView.setBackgroundResource(R.drawable.bg_card_locked)
                imageGradient.setBackgroundResource(R.drawable.bg_card_image_gradient_locked)

                itemView.setOnClickListener {
                    android.widget.Toast.makeText(
                        itemView.context,
                        "解锁条件：${item.unlockCondition}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        private fun loadPlantImage(item: CatalogPlantItem) {
            val ctx = itemView.context ?: return
            val scope = fragment?.viewLifecycleOwner?.lifecycleScope ?: return

            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        PlantImageLoader.loadByStringId(ctx, item.plantStringId, item.level, 0)
                    } catch (e: Exception) {
                        android.util.Log.e("PlantCard", "加载植物图片失败: ${item.plantStringId}", e)
                        null
                    }
                }
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    bitmap?.let { plantImage.setImageBitmap(it) }
                }
            }
        }

        private fun loadSilhouette(item: CatalogPlantItem) {
            val ctx = itemView.context ?: return
            val scope = fragment?.viewLifecycleOwner?.lifecycleScope ?: return

            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        PlantImageLoader.loadSilhouetteByStringId(ctx, item.plantStringId)
                    } catch (e: Exception) {
                        android.util.Log.e("PlantCard", "加载剪影图失败: ${item.plantStringId}", e)
                        null
                    }
                }
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    bitmap?.let { plantImage.setImageBitmap(it) }
                }
            }
        }
    }
}

/**
 * DiffUtil 回调：只有数据真正变化时才更新，避免整列表刷新闪烁
 */
class PlantDiffCallback : DiffUtil.ItemCallback<CatalogPlantItem>() {
    override fun areItemsTheSame(oldItem: CatalogPlantItem, newItem: CatalogPlantItem): Boolean {
        return oldItem.plantId == newItem.plantId
    }

    override fun areContentsTheSame(oldItem: CatalogPlantItem, newItem: CatalogPlantItem): Boolean {
        return oldItem == newItem
    }
}
