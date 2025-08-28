package com.karthek.android.s.files2.ui.screens.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Screen() : NavKey {
	@Serializable
	object Home : Screen()

	@Serializable
	object Btm: Screen()

	@Serializable
	data class PathScreen(val path: String): Screen()

	@Serializable
	object Settings : Screen()

	@Serializable
	object Licenses : Screen()
}
