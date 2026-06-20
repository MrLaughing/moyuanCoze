package com.mrlaughing.moyuan.ui.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.render.PlantImageLoader

/**
 * 图鉴植物卡片 Adapter
 * 已解锁：显示植物图 + 名字 + 等级 + 稀有度星标
 * 未解锁：灰色剪影 + "???"
 */
class PlantCardAdapter(
    private val onPlantClick: (CatalogPlantItem) -> Unit
) : RecyclerView.Adapter<PlantCardAdapter.ViewHolder>() {

    private var items: List<CatalogPlantItem> = emptyList()

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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantImage: ImageView = itemView.findViewById(R.id.image_plant)
        private val plantName: TextView = itemView.findViewById(R.id.text_plant_name)
        private val plantLevel: TextView = itemView.findViewById(R.id.text_plant_level)
        private val plantStars: TextView = itemView.findViewById(R.id.text_plant_stars)

        fun bind(item: CatalogPlantItem) {
            if (item.isUnlocked) {
                // 已解锁：显示正常图片
                loadPlantImage(item)
                plantName.text = item.name
                plantLevel.text = "Lv.${item.level}"
                plantStars.text = "★".repeat(item.rarity) + "☆".repeat(5 - item.rarity)
                plantName.setTextColor(0xFF1A1A1A.toInt())
                plantLevel.setTextColor(0xFF333333.toInt())
                plantStars.setTextColor(0xFF666666.toInt())

                itemView.setOnClickListener {
                    onPlantClick(item)
                }
            } else {
                // 未解锁：灰色剪影
                loadSilhouette(item)
                plantName.text = "???"
                plantLevel.text = ""
                plantStars.text = "★".repeat(item.rarity) + "☆".repeat(5 - item.rarity)
                plantName.setTextColor(0xFF999999.toInt())
                plantLevel.setTextColor(0xFF999999.toInt())
                plantStars.setTextColor(0xFFCCCCCC.toInt())

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
            // 使用 Glide 从 assets 加载
            val context = itemView.context
            Thread {
                val bitmap = PlantImageLoader.load(context, item.plantId, item.level, 0)
                bitmap?.let {
                    (itemView.rootView?.context as? android.app.Activity)?.runOnUiThread {
                        plantImage.setImageBitmap(it)
                    }
                }
            }.start()
        }

        private fun loadSilhouette(item: CatalogPlantItem) {
            val context = itemView.context
            Thread {
                val bitmap = PlantImageLoader.loadSilhouette(context, item.plantId)
                bitmap?.let {
                    (itemView.rootView?.context as? android.app.Activity)?.runOnUiThread {
                        plantImage.setImageBitmap(it)
                    }
                }
            }.start()
        }
    }
}
