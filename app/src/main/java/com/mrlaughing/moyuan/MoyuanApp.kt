package com.mrlaughing.moyuan

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.mrlaughing.moyuan.render.EinkHelper

@HiltAndroidApp
class MoyuanApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        // 全局初始化：墨水屏辅助禁用动画
        EinkHelper.disableAnimations(this)
    }

    companion object {
        @Volatile
        private lateinit var instance: MoyuanApp

        fun getInstance(): MoyuanApp = instance
    }
}
