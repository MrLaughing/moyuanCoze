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
import com.mrlaughing.moyuan.render.EinkHelper
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
            iconRes = R.drawable.ic_onboarding_sprout,
            title = "欢迎来到墨园",
            subtitle = "用微信读书的阅读时长\n浇灌一座水墨花园"
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_onboarding_book,
            title = "读书即浇水",
            subtitle = "每次阅读都会化作甘霖\n滋养护花竹木，阅读越久，生长越盛"
        ),
        OnboardingPage(
            iconRes = R.drawable.ic_onboarding_paths,
            title = "五条探索路径",
            subtitle = "积墨、秉烛、岁寒、寻芳、隐藏\n每条路径藏着不同的植物，等待你的发现"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0)
        EinkHelper.disableAnimations(this)

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

        // 禁用ViewPager2手势滑动（E-ink适配）
        viewPager.isUserInputEnabled = false
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
                viewPager.setCurrentItem(current + 1, false)
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
        overridePendingTransition(0, 0)
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

    data class OnboardingPage(
        val iconRes: Int,
        val title: String,
        val subtitle: String
    )
}
