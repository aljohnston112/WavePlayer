package com.fourthFinger.pinkyPlayer

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment

class NavUtil {

    companion object {

        fun navigateTo(fragment: Fragment, id: Int) {
            val navController: NavController = NavHostFragment.findNavController(fragment)
            val d = navController.currentDestination
            if(d != null) {
                if (d.id != id) {
                    navController.navigate(id)
                }
            } else {
                navController.navigate(id)
            }
        }

        fun navigate(
            navController: NavController,
            action: NavDirections
        ) {
            navController.navigate(action)
        }

        fun popBackStack(fragment: Fragment) {
            NavHostFragment.findNavController(fragment).popBackStack()
        }

    }
}