package com.mrlaughing.moyuan.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.mrlaughing.moyuan.BuildConfig
import com.bumptech.glide.Glide
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.sync.SyncScheduler
import com.mrlaughing.moyuan.sync.SyncWorker
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: ProfileViewModel by viewModels()

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    private lateinit var totalPlantsText: TextView
    private lateinit var plantsProgress: ProgressBar
    private lateinit var wereadStatusText: TextView
    private lateinit var lastSyncText: TextView
    private lateinit var aboutVersionText: TextView
    private lateinit var syncRow: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupClickListeners(view)
        observeData()
    }

    private fun initViews(view: View) {
        Glide.with(this)
            .load("file:///android_asset/plants/莲.png")
            .fitCenter()
            .into(view.findViewById(R.id.image_avatar))

        // 第一层：植物收集进度
        totalPlantsText = view.findViewById(R.id.text_total_plants)
        plantsProgress = view.findViewById(R.id.progress_plants)

        // 设置区
        wereadStatusText = view.findViewById(R.id.text_weread_status)
        lastSyncText = view.findViewById(R.id.text_last_sync)
        aboutVersionText = view.findViewById(R.id.text_about_version)
        syncRow = view.findViewById(R.id.layout_sync_now)
    }

    private fun setupClickListeners(view: View) {
        // 查看图鉴
        view.findViewById<View>(R.id.layout_view_catalog)?.setOnClickListener {
            // 使用底部导航切换而非直接navigate，避免导航栈不同步导致页面点击失灵
            val bottomNav = requireActivity().findViewById<com.mrlaughing.moyuan.ui.common.MoyuanBottomNavigationView>(R.id.bottom_nav)
            bottomNav.selectedItemId = R.id.catalogFragment
        }

        // 立即同步按钮
        view.findViewById<View>(R.id.layout_sync_now)?.setOnClickListener {
            if (viewModel.uiState.value.wereadAuthorized) {
                triggerManualSync()
            } else {
                showTokenInputDialog()
            }
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
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderProfileState(state)
                    }
                }
            }
        }
    }

    private fun renderProfileState(state: ProfileUiState) {
        // 植物收集进度
        val progressText = "${state.unlockedCount} / ${state.totalCount} 植物"
        val ss = SpannableString(progressText)
        ss.setSpan(StyleSpan(android.graphics.Typeface.BOLD), 0, state.unlockedCount.toString().length, 0)
        totalPlantsText.text = ss
        plantsProgress.max = state.totalCount
        plantsProgress.progress = state.unlockedCount

        // 微信读书状态
        val statusText = if (state.wereadAuthorized) getString(R.string.label_authorized) else getString(R.string.label_unauthorized)
        val statusColor = if (state.wereadAuthorized) requireContext().getColor(R.color.ink_dark) else requireContext().getColor(R.color.text_secondary)
        wereadStatusText.text = statusText
        wereadStatusText.setTextColor(statusColor)

        // 同步信息
        lastSyncText.text = getString(R.string.label_last_sync, state.lastSyncTime)
        aboutVersionText.text = BuildConfig.VERSION_NAME
    }

    /**
     * 手动触发同步
     */
    private fun triggerManualSync() {
        val syncId = SyncScheduler.enqueueImmediateSync(requireContext(), replaceRunning = true)
        observeSync(syncId, showQueuedToast = true)
    }

    private fun observeSync(syncId: java.util.UUID, showQueuedToast: Boolean) {
        if (showQueuedToast) {
            Toast.makeText(context, getString(R.string.sync_queued), Toast.LENGTH_SHORT).show()
        }

        WorkManager.getInstance(requireContext()).getWorkInfoByIdLiveData(syncId)
            .observe(viewLifecycleOwner) { workInfo ->
                workInfo ?: return@observe
                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED,
                    WorkInfo.State.BLOCKED,
                    WorkInfo.State.RUNNING -> {
                        syncRow.isEnabled = false
                        syncRow.alpha = 0.55f
                        lastSyncText.text = getString(R.string.sync_in_progress)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        restoreSyncRow()
                        Toast.makeText(context, R.string.msg_sync_complete, Toast.LENGTH_SHORT).show()
                        offerPlantNotificationPermission()
                    }
                    WorkInfo.State.FAILED -> {
                        restoreSyncRow()
                        val errorMsg = workInfo.outputData.getString(SyncWorker.KEY_ERROR_MSG)
                            ?: getString(R.string.msg_sync_failed)
                        Toast.makeText(context, "同步失败：$errorMsg", Toast.LENGTH_LONG).show()
                    }
                    WorkInfo.State.CANCELLED -> {
                        restoreSyncRow()
                        Toast.makeText(context, R.string.sync_cancelled, Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun restoreSyncRow() {
        syncRow.isEnabled = true
        syncRow.alpha = 1f
        renderProfileState(viewModel.uiState.value)
    }

    private fun offerPlantNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val prefs = requireContext().getSharedPreferences(
            NOTIFICATION_PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        if (prefs.getBoolean(KEY_NOTIFICATION_PROMPTED, false)) return

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("发现新植物时提醒你")
            .setMessage("允许通知后，墨园会在后台同步发现新植物时轻声提醒。不会发送阅读催促。")
            .setPositiveButton("允许") { _, _ ->
                prefs.edit().putBoolean(KEY_NOTIFICATION_PROMPTED, true).apply()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("暂不") { _, _ ->
                prefs.edit().putBoolean(KEY_NOTIFICATION_PROMPTED, true).apply()
            }
            .show()
    }
    private fun showTokenInputDialog() {
        val horizontalPadding = (16 * resources.displayMetrics.density).toInt()
        val verticalPadding = (12 * resources.displayMetrics.density).toInt()
        val editText = EditText(requireContext()).apply {
            hint = "请输入微信读书 API Key"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("连接微信读书")
            .setMessage("请输入微信读书开放接口 API Key（通常以 wrk- 开头）。密钥仅加密保存在本机。")
            .setView(editText)
            .setPositiveButton("连接") { _, _ ->
                val token = editText.text.toString().trim()
                if (token.isNotBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val syncId = viewModel.authorize(token)
                        Toast.makeText(
                            context,
                            "凭据已加密保存，正在验证并同步",
                            Toast.LENGTH_LONG
                        ).show()
                        observeSync(syncId, showQueuedToast = false)
                    }
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

    private fun showAboutDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.label_about_moyuan))
            .setMessage(getString(R.string.about_content))
            .setPositiveButton("确定", null)
            .show()
    }
    private companion object {
        const val NOTIFICATION_PREFS_NAME = "moyuan_notification_prefs"
        const val KEY_NOTIFICATION_PROMPTED = "plant_notification_prompted"
    }
}
