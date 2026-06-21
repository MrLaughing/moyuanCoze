package com.mrlaughing.moyuan.ui.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.render.PlantImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 图鉴植物卡片 Adapter
 * 已解锁：显示植物图 + 名字 + 等级 + 稀有度星标
 * 未解锁：灰色剪影 + "???"
 */
class PlantCardAdapter(
    private val onPlantClick: (CatalogPlantItem) -> Unit
) : RecyclerView.Adapter<PlantCardAdapter.ViewHolder>() {

    private var items: List<CatalogPlantItem> = emptyList()

    // 暖色调颜色定义
    private val unlockedNameColor = 0xFF2C2416.toInt()      // ink_dark
    private val unlockedLevelColor = 0xFF78716C.toInt()     // text_secondary
    private val unlockedStarsColor = 0xFFA89F91.toInt()     // ink_light
    private val lockedNameColor = 0xFFA89F91.toInt()        // ink_light
    private val lockedLevelColor = 0xFFA89F91.toInt()       // ink_light
    private val lockedStarsColor = 0xFFD4C9B8.toInt()       // border/ink_wash

    fun submitList(newItems: List<CatalogPlantItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * 根据等级数字获取等级名称
     */
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
                // 已解锁：显示正常图片
                loadPlantImage(item)
                plantName.text = item.name
                // 显示等级和等级名称：Lv.3 墨枝
                val levelName = getLevelName(item.level)
                plantLevel.text = "Lv.${item.level} $levelName"
                plantStars.text = "★".repeat(item.rarity) + "☆".repeat(5 - item.rarity)
                
                // 已解锁暖色调
                plantName.setTextColor(unlockedNameColor)
                plantLevel.setTextColor(unlockedLevelColor)
                plantStars.setTextColor(unlockedStarsColor)
                
                // 已解锁卡片背景：白色+边框
                itemView.setBackgroundResource(R.drawable.bg_card_unlocked)
                // 淡墨渐变底色
                imageGradient.setBackgroundResource(R.drawable.bg_card_image_gradient_unlocked)

                itemView.setOnClickListener {
                    onPlantClick(item)
                }
            } else {
                // 未解锁：灰色剪影
                loadSilhouette(item)
                plantName.text = "???"
                plantLevel.text = ""
                plantStars.text = "★".repeat(item.rarity) + "☆".repeat(5 - item.rarity)
                
                // 未解锁暖色调
                plantName.setTextColor(lockedNameColor)
                plantLevel.setTextColor(lockedLevelColor)
                plantStars.setTextColor(lockedStarsColor)
                
                // 未解锁卡片背景：surface色
                itemView.setBackgroundResource(R.drawable.bg_card_locked)
                // 浅绿灰色渐变
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
            val context = itemView.context
            // 使用Activity的lifecycleScope代替裸Thread，避免Fragment销毁后崩溃
            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        PlantImageLoader.load(context, item.plantId, item.level, 0)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    bitmap?.let {
                        plantImage.setImageBitmap(it)
                    }
                }
            }
        }

        private fun loadSilhouette(item: CatalogPlantItem) {
            val context = itemView.context
            (context as? LifecycleOwner)?.lifecycleScope?.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        PlantImageLoader.loadSilhouette(context, item.plantId)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    bitmap?.let {
                        plantImage.setImageBitmap(it)
                    }
                }
            }
        }
    }
}
