package com.mrlaughing.moyuan.ui.onboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mrlaughing.moyuan.R
import com.bumptech.glide.Glide

class OnboardingAdapter(
    private val pages: List<OnboardingActivity.OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val title: TextView = view.findViewById(R.id.tvTitle)
        val subtitle: TextView = view.findViewById(R.id.tvSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        Glide.with(holder.itemView)
            .load("file:///android_asset/plants/${page.assetName}.png")
            .fitCenter()
            .into(holder.icon)
        holder.title.text = page.title
        holder.subtitle.text = page.subtitle
    }

    override fun getItemCount() = pages.size
}
