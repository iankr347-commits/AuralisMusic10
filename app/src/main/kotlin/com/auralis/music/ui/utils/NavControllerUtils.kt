// Private Test Build  Not for Redistribution

package com.auralis.music.ui.utils

import androidx.navigation.NavController
import com.auralis.music.ui.screens.Screens

fun NavController.backToMain() {
    val mainRoutes = Screens.MainScreens.map { it.route }

    while (previousBackStackEntry != null &&
        currentBackStackEntry?.destination?.route !in mainRoutes
    ) {
        popBackStack()
    }
}
