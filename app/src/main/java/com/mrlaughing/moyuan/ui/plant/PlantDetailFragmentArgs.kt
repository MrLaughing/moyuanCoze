package com.mrlaughing.moyuan.ui.plant

import android.os.Bundle
import androidx.navigation.NavArgs
import kotlin.jvm.JvmStatic

/**
 * PlantDetailFragment 的参数（Safe Args 生成替代）
 */
data class PlantDetailFragmentArgs(val plantId: Long) : NavArgs {

    fun toBundle(): Bundle {
        return Bundle().apply {
            putLong("plantId", plantId)
        }
    }

    companion object {
        @JvmStatic
        fun fromBundle(bundle: Bundle): PlantDetailFragmentArgs {
            val plantId = bundle.getLong("plantId")
            return PlantDetailFragmentArgs(plantId)
        }
    }
}
