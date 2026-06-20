package com.mrlaughing.moyuan.ui.profile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.mrlaughing.moyuan.R
import kotlinx.coroutines.launch

/**
 * 个人中心 Fragment
 */
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var gardenNameText: TextView
    private lateinit var plantCountText: TextView
    private lateinit var unlockProgressText: TextView
    private lateinit var wereadStatusText: TextView
    private lateinit var lastSyncText: TextView
    private lateinit var syncTimeText: TextView
    private lateinit var refreshModeText: TextView
    private lateinit var aboutButton: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        gardenNameText = view.findViewById(R.id.text_garden_name)
        plantCountText = view.findViewById(R.id.text_plant_count)
        unlockProgressText = view.findViewById(R.id.text_unlock_progress)
        wereadStatusText = view.findViewById(R.id.text_weread_status)
        lastSyncText = view.findViewById(R.id.text_last_sync)
        syncTimeText = view.findViewById(R.id.text_sync_time)
        refreshModeText = view.findViewById(R.id.text_refresh_mode)
        aboutButton = view.findViewById(R.id.layout_about)

        // 设置点击事件
        view.findViewById<View>(R.id.layout_sync_time)?.setOnClickListener {
            showSyncTimePicker()
        }

        view.findViewById<View>(R.id.layout_refresh_mode)?.setOnClickListener {
            showRefreshModeDialog()
        }

        // 微信读书卡片点击事件
        view.findViewById<View>(R.id.card_weread)?.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.wereadAuthorized) {
                showDeauthorizeConfirmDialog()
            } else {
                showTokenInputDialog()
            }
        }

        aboutButton.setOnClickListener {
            showAboutDialog()
        }

        // 观察数据
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(state: ProfileUiState) {
        gardenNameText.text = state.gardenName
        plantCountText.text = "${state.plantCount} 株植物"
        unlockProgressText.text = "${state.unlockedCount}/${state.totalCount} 已解锁"
        
        // 微信读书授权状态：已授权绿色，未授权灰色
        wereadStatusText.text = if (state.wereadAuthorized) "已授权" else "未授权"
        wereadStatusText.setTextColor(
            if (state.wereadAuthorized) Color.parseColor("#4CAF50") // 绿色
            else Color.parseColor("#9E9E9E") // 灰色
        )
        
        lastSyncText.text = "上次同步：${state.lastSyncTime}"
        syncTimeText.text = String.format("%02d:%02d", state.syncHour, state.syncMinute)
        refreshModeText.text = state.refreshMode
    }

    /**
     * 显示 Token 输入对话框
     */
    private fun showTokenInputDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "请输入微信读书 Token"
            setPadding(48, 24, 48, 24)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("微信读书授权")
            .setMessage("请在微信读书网页版登录后，从浏览器 Cookie 中获取 Token")
            .setView(editText)
            .setPositiveButton("授权") { _, _ ->
                val token = editText.text.toString().trim()
                if (token.isNotBlank()) {
                    viewModel.authorize(token)
                    Toast.makeText(context, "授权成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Token 不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示取消授权确认对话框
     */
    private fun showDeauthorizeConfirmDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("取消授权")
            .setMessage("确定要取消微信读书授权吗？取消后将无法自动同步阅读数据。")
            .setPositiveButton("确定") { _, _ ->
                viewModel.deauthorize()
                Toast.makeText(context, "已取消授权", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSyncTimePicker() {
        val state = viewModel.uiState.value
        val picker = android.app.TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                viewModel.updateSyncTime(hour, minute)
            },
            state.syncHour,
            state.syncMinute,
            true
        )
        picker.show()
    }

    private fun showRefreshModeDialog() {
        val options = arrayOf("局部刷新", "全刷模式")
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("刷新模式")
            .setItems(options) { _, which ->
                viewModel.updateRefreshMode(options[which])
            }
            .show()
    }

    private fun showAboutDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("关于墨园")
            .setMessage("墨园 v1.0.0\n\n墨水屏养成专注游戏\n水墨画风格 · 纯黑白灰配色\n\n用阅读浇灌花园，让知识生长繁茂。")
            .setPositiveButton("确定", null)
            .show()
    }
}
