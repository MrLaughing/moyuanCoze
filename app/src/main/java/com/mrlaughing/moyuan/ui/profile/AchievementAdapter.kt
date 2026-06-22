package com.mrlaughing.moyuan.ui.profile

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.db.entity.AchievementEntity
import com.mrlaughing.moyuan.ui.profile.ink.InkIconView

/**
 * 成就圆形墨章网格适配器
 * 5列网格展示圆形墨章徽章，点击弹出详情弹窗
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
        private val badgeRing: View = itemView.findViewById(R.id.view_badge_ring)
        private val inkIcon: InkIconView = itemView.findViewById(R.id.ink_icon)
        private val nameText: TextView = itemView.findViewById(R.id.text_achievement_name)

        fun bind(achievement: AchievementEntity) {
            inkIcon.iconType = InkIconView.iconTypeForAchievement(achievement.id)
            if (achievement.isUnlocked) {
                bindUnlocked(achievement)
            } else {
                bindLocked(achievement)
            }
            itemView.setOnClickListener { showAchievementDetailDialog(achievement) }
        }

        private fun bindUnlocked(achievement: AchievementEntity) {
            badgeRing.setBackgroundResource(R.drawable.bg_ink_badge_ring)
            inkIcon.isUnlocked = true
            nameText.text = achievement.name
            nameText.setTextColor(Color.parseColor("#333333"))
        }

        private fun bindLocked(achievement: AchievementEntity) {
            badgeRing.setBackgroundResource(R.drawable.bg_ink_badge_ring_locked)
            inkIcon.isUnlocked = false
            nameText.text = "???"
            nameText.setTextColor(Color.parseColor("#CCCCCC"))
        }

        private fun showAchievementDetailDialog(achievement: AchievementEntity) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(
                R.layout.dialog_achievement_detail, null, false
            )
            val dialogRing: View = dialogView.findViewById(R.id.view_dialog_ring)
            val dialogIcon: InkIconView = dialogView.findViewById(R.id.ink_icon_dialog)
            val dialogName: TextView = dialogView.findViewById(R.id.text_dialog_name)
            val dialogCondition: TextView = dialogView.findViewById(R.id.text_dialog_condition)
            val dialogDate: TextView = dialogView.findViewById(R.id.text_dialog_date)
            val dialogClose: TextView = dialogView.findViewById(R.id.button_close)

            dialogIcon.iconType = InkIconView.iconTypeForAchievement(achievement.id)
            if (achievement.isUnlocked) {
                dialogRing.setBackgroundResource(R.drawable.bg_ink_badge_ring_dialog)
                dialogIcon.isUnlocked = true
                dialogName.text = achievement.name
                dialogName.setTextColor(Color.parseColor("#1A1A1A"))
                dialogCondition.text = "解锁条件：${achievement.condition}"
                dialogDate.text = "获得日期：${achievement.unlockedDate ?: "未知"}"
                dialogDate.visibility = View.VISIBLE
            } else {
                dialogRing.setBackgroundResource(R.drawable.bg_ink_badge_ring_locked_dialog)
                dialogIcon.isUnlocked = false
                dialogName.text = "???"
                dialogName.setTextColor(Color.parseColor("#CCCCCC"))
                dialogCondition.text = "解锁条件：${achievement.condition}\n当前进度：${achievement.currentValue}/${achievement.targetValue}"
                dialogDate.visibility = View.GONE
            }

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialogClose.setOnClickListener { dialog.dismiss() }
            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_dialog_achievement)
            dialog.show()
        }
    }

    class AchievementDiffCallback : DiffUtil.ItemCallback<AchievementEntity>() {
        override fun areItemsTheSame(oldItem: AchievementEntity, newItem: AchievementEntity): Boolean =
            oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: AchievementEntity, newItem: AchievementEntity): Boolean =
            oldItem == newItem
    }
}
