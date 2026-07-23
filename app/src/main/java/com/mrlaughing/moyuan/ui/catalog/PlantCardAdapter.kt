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
 * v2.0：
 * - 已解锁：显示植物图 + 名字
 * - 未解锁：通用占位图标 + 植物名 + 随机发现提示
 */
class PlantCardAdapter(
    private val onPlantClick: (CatalogPlantItem) -> Unit
) : ListAdapter<CatalogPlantItem, PlantCardAdapter.ViewHolder>(PlantDiffCallback()) {

    private var fragment: Fragment? = null

    private val unlockedNameColor = 0xFF2C2416.toInt()
    private val lockedNameColor = 0xFFA89F91.toInt()
    private val lockedLevelColor = 0xFF78716C.toInt()

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

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val plantImage: ImageView = itemView.findViewById(R.id.image_plant)
        private val imageGradient: View = itemView.findViewById(R.id.view_image_gradient)
        private val plantName: TextView = itemView.findViewById(R.id.text_plant_name)
        private val unlockCondition: TextView = itemView.findViewById(R.id.text_unlock_condition)

        fun bind(item: CatalogPlantItem) {
            if (item.isUnlocked) {
                plantImage.setImageDrawable(null)
                loadPlantImage(item, silhouette = false)
                plantName.text = item.name
                plantName.setTextColor(unlockedNameColor)
                unlockCondition.visibility = View.GONE

                itemView.setBackgroundResource(R.drawable.bg_card_unlocked)
                imageGradient.setBackgroundResource(R.drawable.bg_card_image_gradient_unlocked)

            } else {
                plantImage.setImageDrawable(null)
                loadPlantImage(item, silhouette = true)
                plantName.text = item.name  // 显示真实植物名
                plantName.setTextColor(lockedNameColor)
                unlockCondition.visibility = View.VISIBLE
                unlockCondition.text = item.unlockCondition

                itemView.setBackgroundResource(R.drawable.bg_card_locked)
                imageGradient.setBackgroundResource(R.drawable.bg_card_image_gradient_locked)

            }

            itemView.contentDescription = itemView.context.getString(
                R.string.catalog_open_plant_detail,
                item.name
            )
            itemView.setOnClickListener { onPlantClick(item) }
        }

        private fun loadPlantImage(item: CatalogPlantItem, silhouette: Boolean) {
            val ctx = itemView.context ?: return
            val scope = fragment?.viewLifecycleOwner?.lifecycleScope ?: return
            val imageTag = "${item.plantStringId}:$silhouette"
            plantImage.tag = imageTag

            scope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    try {
                        if (silhouette) {
                            PlantImageLoader.loadSilhouetteByStringId(ctx, item.plantStringId)
                        } else {
                            PlantImageLoader.loadByStringId(ctx, item.plantStringId)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PlantCard", "加载植物图片失败: ${item.plantStringId}", e)
                        null
                    }
                }
                if (bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    plantImage.tag == imageTag
                ) {
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
