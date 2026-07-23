package com.mrlaughing.moyuan.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.data.local.prefs.UserPrefs
import com.mrlaughing.moyuan.data.repository.GardenRepository
import com.mrlaughing.moyuan.data.repository.WereadRepository
import com.mrlaughing.moyuan.sync.SyncScheduler
import com.mrlaughing.moyuan.ui.common.MoyuanBottomNavigationView
import com.mrlaughing.moyuan.ui.onboarding.OnboardingActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: MoyuanBottomNavigationView

    @Inject lateinit var userPrefs: UserPrefs
    @Inject lateinit var wereadRepository: WereadRepository
    @Inject lateinit var gardenRepository: GardenRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查是否需要显示引导页
        if (!OnboardingActivity.isOnboardingDone(this)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        // 设置 Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 设置 BottomNavigationView
        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = if (destination.id == R.id.plantDetailFragment) View.GONE else View.VISIBLE
        }

        // 禁用底部导航的 reselect 动画
        bottomNav.setOnItemReselectedListener { /* 不做任何事，防止动画 */ }

        configureSync()
    }

    private fun configureSync() {
        lifecycleScope.launch {
            if (!wereadRepository.isAuthorized()) return@launch

            val hour = userPrefs.syncHour.first()
            val minute = userPrefs.syncMinute.first()
            SyncScheduler.scheduleDailySync(this@MainActivity, hour, minute)

            val meta = gardenRepository.observeMeta().filterNotNull().first()
            if (meta.lastSyncDate != LocalDate.now().toString()) {
                SyncScheduler.enqueueImmediateSync(this@MainActivity)
            }
        }
    }

}
