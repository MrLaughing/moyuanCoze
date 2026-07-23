package com.mrlaughing.moyuan.ui.catalog

import androidx.navigation.NavDirections
import android.os.Bundle
import com.mrlaughing.moyuan.R
import com.mrlaughing.moyuan.ui.plant.PlantDetailFragmentArgs

/**
 * CatalogFragment 的导航 Directions
 */
object CatalogFragmentDirections {

    fun actionCatalogFragmentToPlantDetailFragment(plantId: Long): NavDirections {
        return object : NavDirections {
            override val actionId: Int = R.id.action_catalogFragment_to_plantDetailFragment
            override val arguments: Bundle
                get() = PlantDetailFragmentArgs(plantId).toBundle()
        }
    }
}
