package com.mrlaughing.moyuan.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.db.entity.AchievementEntity

/**
 * 成就列表适配器
 */
class AchievementAdapter : ListAdapter<AchievementEntity, AchievementAdapter.AchievementViewHolder>(AchievementDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sealText: TextView = itemView.findViewById(R.id.text_seal)
        private val nameText: TextView = itemView.findViewById(R.id.text_achievement_name)
        private val conditionText: TextView = itemView.findViewById(R.id.text_achievement_condition)
        private val statusText: TextView = itemView.findViewById(R.id.text_achievement_status)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_achievement)
        private val cardView: View = itemView.findViewById(R.id.card_achievement)

        fun bind(achievement: AchievementEntity) {
            // 印章文字：取成就名首字
            val sealChar = achievement.name.firstOrNull()?.toString() ?: "?"
            sealText.text = sealChar

            // 成就名和条件
            nameText.text = achievement.name
            conditionText.text = achievement.condition

            if (achievement.isUnlocked) {
                // 已解锁状态
                bindUnlocked(achievement)
            } else {
                // 未解锁状态
                bindLocked(achievement)
            }
        }

        private fun bindUnlocked(achievement: AchievementEntity) {
            // 背景：实线边框
            cardView.setBackgroundResource(R.drawable.bg_achievement_unlocked)
            
            // 印章：深棕色背景
            sealText.setBackgroundResource(R.drawable.bg_seal_stamp)
            sealText.setTextColor(itemView.context.getColor(R.color.text_on_dark))
            
            // 文字：正常颜色
            nameText.setTextColor(itemView.context.getColor(R.color.ink_dark))
            conditionText.setTextColor(itemView.context.getColor(R.color.text_secondary))
            
            // 状态文字
            val dateStr = achievement.unlockedDate ?: ""
            statusText.text = itemView.context.getString(R.string.label_unlocked_date, dateStr)
            statusText.setTextColor(itemView.context.getColor(R.color.text_tertiary))
            statusText.visibility = View.VISIBLE
            
            // 进度条：已达成，显示满进度
            progressBar.max = achievement.targetValue
            progressBar.progress = achievement.targetValue
            progressBar.visibility = View.GONE
        }

        private fun bindLocked(achievement: AchievementEntity) {
            // 背景：虚线边框
            cardView.setBackgroundResource(R.drawable.bg_achievement_locked)
            
            // 印章：灰色背景
            sealText.setBackgroundResource(R.drawable.bg_seal_stamp_locked)
            sealText.setTextColor(itemView.context.getColor(R.color.ink_dark))
            
            // 文字：浅墨色
            nameText.setTextColor(itemView.context.getColor(R.color.ink_light))
            conditionText.setTextColor(itemView.context.getColor(R.color.ink_light))
            
            // 状态文字：显示进度
            val remaining = achievement.targetValue - achievement.currentValue
            statusText.text = itemView.context.getString(R.string.label_remaining, remaining)
            statusText.setTextColor(itemView.context.getColor(R.color.text_tertiary))
            statusText.visibility = View.VISIBLE
            
            // 进度条
            progressBar.max = achievement.targetValue
            progressBar.progress = achievement.currentValue
            progressBar.visibility = View.VISIBLE
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
