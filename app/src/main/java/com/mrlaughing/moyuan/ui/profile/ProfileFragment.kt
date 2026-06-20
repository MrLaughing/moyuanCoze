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
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.sync.SyncWorker
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private lateinit var plantCountText: TextView
    private lateinit var unlockProgressText: TextView
    private lateinit var wereadStatusText: TextView
    private lateinit var lastSyncText: TextView
    private lateinit var syncTimeText: TextView
    private lateinit var refreshModeText: TextView
    private lateinit var aboutVersionText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        plantCountText = view.findViewById(R.id.text_plant_count)
        unlockProgressText = view.findViewById(R.id.text_unlock_progress)
        wereadStatusText = view.findViewById(R.id.text_weread_status)
        lastSyncText = view.findViewById(R.id.text_last_sync)
        syncTimeText = view.findViewById(R.id.text_sync_time)
        refreshModeText = view.findViewById(R.id.text_refresh_mode)
        aboutVersionText = view.findViewById(R.id.text_about_version)

        // 立即同步按钮
        view.findViewById<View>(R.id.layout_sync_now)?.setOnClickListener {
            triggerManualSync()
        }

        // 同步时间设置
        view.findViewById<View>(R.id.layout_sync_time)?.setOnClickListener {
            showSyncTimePicker()
        }

        // 刷新模式设置
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

        // 关于卡片点击
        view.findViewById<View>(R.id.layout_about)?.setOnClickListener {
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
        plantCountText.text = "植物 ${state.plantCount}/27·枯萎${state.witheredCount}"
        unlockProgressText.text = "首次种植 ${state.firstPlantDate}"

        val statusText = if (state.wereadAuthorized) getString(R.string.label_authorized) else getString(R.string.label_unauthorized)
        val statusColor = if (state.wereadAuthorized) Color.parseColor("#333333") else Color.parseColor("#999999")
        wereadStatusText.text = statusText
        wereadStatusText.setTextColor(statusColor)

        lastSyncText.text = "上次同步${state.lastSyncTime}"
        syncTimeText.text = String.format("%02d:%02d", state.syncHour, state.syncMinute)
        refreshModeText.text = state.refreshMode
        aboutVersionText.text = "v1.0.0"
    }

    /**
     * 手动触发同步
     */
    private fun triggerManualSync() {
        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(requireContext()).enqueue(syncRequest)

        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(syncRequest.id)
            .observe(viewLifecycleOwner) { workInfo ->
                when {
                    workInfo.state == WorkInfo.State.SUCCEEDED -> {
                        Toast.makeText(context, "同步完成", Toast.LENGTH_SHORT).show()
                    }
                    workInfo.state == WorkInfo.State.FAILED -> {
                        Toast.makeText(context, "同步失败，请检查网络和Token", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun showTokenInputDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "请输入微信读书 API Key"
            setPadding(48, 24, 48, 24)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("微信读书授权")
            .setMessage("请在微信读书 App 设置中获取 API Key（格式：wrk-xxx）")
            .setView(editText)
            .setPositiveButton("授权") { _, _ ->
                val token = editText.text.toString().trim()
                if (token.isNotBlank()) {
                    viewModel.authorize(token)
                    Toast.makeText(context, "授权成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "API Key 不能为空", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

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
            .setTitle(getString(R.string.label_about_moyuan))
            .setMessage(getString(R.string.about_content))
            .setPositiveButton("确定", null)
            .show()
    }
}
