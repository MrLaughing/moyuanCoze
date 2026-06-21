package com.mrlaughing.moyuan.ui.profile

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.db.entity.AchievementEntity

/**
 * 成就网格适配器
 * 展示印章墙风格的成就徽章，点击弹出详情 Dialog
 */
class AchievementAdapter : ListAdapter<AchievementEntity, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement_grid, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val frameSeal: FrameLayout = itemView.findViewById(R.id.frame_seal)
        private val sealText: TextView = itemView.findViewById(R.id.text_seal)
        private val nameText: TextView = itemView.findViewById(R.id.text_achievement_name)

        fun bind(achievement: AchievementEntity) {
            val context = itemView.context

            if (achievement.isUnlocked) {
                bindUnlocked(achievement)
            } else {
                bindLocked(achievement)
            }

            // 点击弹出详情 Dialog
            itemView.setOnClickListener {
                showAchievementDetailDialog(achievement)
            }
        }

        private fun bindUnlocked(achievement: AchievementEntity) {
            val context = itemView.context
            
            // 印章：深棕色背景 + 首字
            frameSeal.setBackgroundResource(R.drawable.bg_seal_stamp)
            sealText.text = achievement.name.firstOrNull()?.toString() ?: "?"
            sealText.setTextColor(context.getColor(R.color.text_on_dark))
            
            // 成就名：深墨色
            nameText.text = achievement.name
            nameText.setTextColor(context.getColor(R.color.ink_dark))
        }

        private fun bindLocked(achievement: AchievementEntity) {
            val context = itemView.context
            
            // 印章：灰色背景 + "???"
            frameSeal.setBackgroundResource(R.drawable.bg_seal_stamp_locked)
            sealText.text = "???"
            sealText.setTextColor(context.getColor(R.color.ink_light))
            
            // 成就名：极淡灰
            nameText.text = "???"
            nameText.setTextColor(context.getColor(R.color.ink_wash))
        }

        private fun showAchievementDetailDialog(achievement: AchievementEntity) {
            val context = itemView.context
            
            val title = if (achievement.isUnlocked) {
                achievement.name
            } else {
                "???"
            }

            val message = buildString {
                if (achievement.isUnlocked) {
                    appendLine("名称：${achievement.name}")
                    appendLine("描述：${achievement.description}")
                    appendLine()
                    appendLine("解锁条件：${achievement.condition}")
                    appendLine("获得日期：${achievement.unlockedDate ?: "未知"}")
                } else {
                    appendLine("名称：???")
                    appendLine()
                    appendLine("解锁条件：${achievement.condition}")
                    appendLine("当前进度：${achievement.currentValue}/${achievement.targetValue}")
                }
            }

            AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", null)
                .show()
        }
    }

    class AchievementDiffCallback : DiffUtil.ItemCallback<AchievementEntity>() {
        override fun areItemsTheSame(oldItem: AchievementEntity, newItem: AchievementEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: AchievementEntity, newItem: AchievementEntity): Boolean {
            return oldItem == newItem
        }
    }
}
