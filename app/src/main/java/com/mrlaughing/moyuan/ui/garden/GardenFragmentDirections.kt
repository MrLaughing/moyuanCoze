package com.mrlaughing.moyuan.ui.garden

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.ui.plant.PlantDetailFragmentArgs

/**
 * GardenFragment 的导航 Directions（Safe Args 生成替代）
 */
object GardenFragmentDirections {

    fun actionGardenFragmentToPlantDetailFragment(plantId: Long): androidx.navigation.NavDirections {
        return object : androidx.navigation.NavDirections {
            override val actionId: Int = R.id.action_gardenFragment_to_plantDetailFragment
            override val arguments: Bundle
                get() = PlantDetailFragmentArgs(plantId).toBundle()
        }
    }
}
