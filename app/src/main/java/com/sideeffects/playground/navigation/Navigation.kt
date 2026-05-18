package com.sideeffects.playground.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sideeffects.playground.ui.screens.derivedstate.DerivedStateScreen
import com.sideeffects.playground.ui.screens.disposableeffect.DisposableEffectScreen
import com.sideeffects.playground.ui.screens.home.HomeScreen
import com.sideeffects.playground.ui.screens.launchedeffect.LaunchedEffectScreen
import com.sideeffects.playground.ui.screens.producestate.ProduceStateScreen
import com.sideeffects.playground.ui.screens.remember.RememberScreen
import com.sideeffects.playground.ui.screens.remembercoroutinescope.RememberCoroutineScopeScreen
import com.sideeffects.playground.ui.screens.rememberupdatedstate.RememberUpdatedStateScreen
import com.sideeffects.playground.ui.screens.sideeffect.SideEffectScreen
import com.sideeffects.playground.ui.screens.snapshotflow.SnapshotFlowScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object LaunchedEffect : Screen("launched_effect")
    data object RememberCoroutineScope : Screen("remember_coroutine_scope")
    data object RememberUpdatedState : Screen("remember_updated_state")
    data object DisposableEffect : Screen("disposable_effect")
    data object SideEffect : Screen("side_effect")
    data object ProduceState : Screen("produce_state")
    data object DerivedState : Screen("derived_state")
    data object SnapshotFlow : Screen("snapshot_flow")
    data object Remember : Screen("remember")
}

@Composable
fun PlaygroundNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(onNavigate = { navController.navigate(it) })
        }
        composable(Screen.LaunchedEffect.route) {
            LaunchedEffectScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.RememberCoroutineScope.route) {
            RememberCoroutineScopeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.RememberUpdatedState.route) {
            RememberUpdatedStateScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DisposableEffect.route) {
            DisposableEffectScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SideEffect.route) {
            SideEffectScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ProduceState.route) {
            ProduceStateScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.DerivedState.route) {
            DerivedStateScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.SnapshotFlow.route) {
            SnapshotFlowScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Remember.route) {
            RememberScreen(onBack = { navController.popBackStack() })
        }
    }
}
