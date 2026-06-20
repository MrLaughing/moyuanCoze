package com.mrlaughing.moyuan.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.render.EinkHelper
import com.mrlaughing.moyuan.ui.common.EinkBottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: EinkBottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 禁用所有过渡动画
        overridePendingTransition(0, 0)
        EinkHelper.disableAnimations(this)

        setContentView(R.layout.activity_main)

        // 设置 Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 设置 BottomNavigationView
        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        // 禁用底部导航的 reselect 动画
        bottomNav.setOnItemReselectedListener { /* 不做任何事，防止动画 */ }
    }

    override fun onPause() {
        overridePendingTransition(0, 0)
        super.onPause()
    }

    override fun onResume() {
        overridePendingTransition(0, 0)
        super.onResume()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
