package com.fourthFinger.pinkyPlayer

import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class NavUtil {

    companion object {

        fun popBackStack(fragment: Fragment) {
            NavHostFragment.findNavController(fragment).popBackStack()
        }

    }
}