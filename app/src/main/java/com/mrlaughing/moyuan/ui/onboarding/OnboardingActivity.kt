package com.mrlaughing.moyuan.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.ui.MainActivity
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "moyuan_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"

        fun isOnboardingDone(context: Context): Boolean {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ONBOARDING_DONE, false)
        }

        fun markOnboardingDone(context: Context) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_ONBOARDING_DONE, true)
                .apply()
        }
    }

    private lateinit var viewPager: ViewPager2
    private lateinit var dotsContainer: LinearLayout
    private lateinit var btnNext: MaterialButton
    private lateinit var btnSkip: TextView

    private val pages = listOf(
        OnboardingPage(
            assetName = "夏",
            title = "墨园",
            subtitle = "让每一次阅读\n在时间里长成一座花园"
        ),
        OnboardingPage(
            assetName = "文竹",
            title = "阅读留下生长的痕迹",
            subtitle = "连接微信读书后自动记录时长\n不打断阅读，也不催促你完成任务"
        ),
        OnboardingPage(
            assetName = "莲",
            title = "慢慢收集一座花园",
            subtitle = "累计阅读跨过新的里程碑\n就会随机发现一株属于你的植物"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 已完成引导 → 直接跳转主页
        if (isOnboardingDone(this)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        dotsContainer = findViewById(R.id.dotsContainer)
        btnNext = findViewById(R.id.btnNext)
        btnSkip = findViewById(R.id.btnSkip)

        viewPager.isUserInputEnabled = true
        viewPager.adapter = OnboardingAdapter(pages)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButton(position)
            }
        })

        setupDots()
        updateDots(0)
        updateButton(0)

        btnNext.setOnClickListener {
            val current = viewPager.currentItem
            if (current < pages.size - 1) {
                viewPager.setCurrentItem(current + 1, true)
            } else {
                finishOnboarding()
            }
        }

        btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupDots() {
        dotsContainer.removeAllViews()
        val dotSize = resources.getDimensionPixelSize(R.dimen.spacing_sm)
        val dotMargin = resources.getDimensionPixelSize(R.dimen.spacing_xs)
        for (i in pages.indices) {
            val dot = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                    marginStart = dotMargin
                    marginEnd = dotMargin
                }
                setImageResource(R.drawable.dot_inactive)
            }
            dotsContainer.addView(dot)
        }
    }

    private fun updateDots(position: Int) {
        for (i in 0 until dotsContainer.childCount) {
            val dot = dotsContainer.getChildAt(i) as ImageView
            dot.setImageResource(if (i == position) R.drawable.dot_active else R.drawable.dot_inactive)
        }
    }

    private fun updateButton(position: Int) {
        if (position == pages.size - 1) {
            btnNext.text = "开始我的墨园"
            btnSkip.visibility = TextView.GONE
        } else {
            btnNext.text = "继续"
            btnSkip.visibility = TextView.VISIBLE
        }
    }

    private fun finishOnboarding() {
        markOnboardingDone(this)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    data class OnboardingPage(
        val assetName: String,
        val title: String,
        val subtitle: String
    )
}
