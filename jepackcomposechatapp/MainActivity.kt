package android.mohamed.jepackcomposechatapp

import android.mohamed.jepackcomposechatapp.composables.BottomBarSetup
import android.mohamed.jepackcomposechatapp.composables.NavHostScreen
import android.mohamed.jepackcomposechatapp.composables.TopBarSetup
import android.mohamed.jepackcomposechatapp.composables.clearBackStack
import android.mohamed.jepackcomposechatapp.ui.theme.JepackComposeChatAppTheme
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.CHAT_LIST_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.CHAT_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.FILL_PROFILE_DATA_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.FRIENDS_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.LOG_IN_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.REQUESTS_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.SPLASH_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.USERS_SCREEN
import android.mohamed.jepackcomposechatapp.utility.State
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.viewmodel.ext.android.viewModel


class MainActivity : ComponentActivity() {
    private val noTopAppBarRoutes = listOf(FILL_PROFILE_DATA_SCREEN, SPLASH_SCREEN, CHAT_SCREEN)
    private val viewModel: MainViewModel by viewModel()

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            JepackComposeChatAppTheme {
                // A surface container using the 'background' color from the theme
                val navController = rememberNavController()
                val userState by viewModel.userState.collectAsState()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                Surface(color = MaterialTheme.colors.background) {
                    Scaffold(
                        bottomBar = {
                            when (currentRoute) {
                                CHAT_LIST_SCREEN -> BottomBarSetup(navController = navController)
                                FRIENDS_SCREEN -> BottomBarSetup(navController = navController)
                                REQUESTS_SCREEN -> BottomBarSetup(navController = navController)
                            }
                        },
                        topBar = {
                            Log.d("MainActivity", "onCreate ->  current Route: $currentRoute")
                            if (userState is State.UserState.LoggedIn && !noTopAppBarRoutes.contains(
                                    currentRoute?.takeWhile { it != '/' })
                            )
                                TopBarSetup(
                                    onLogOutIconClick = {
                                        viewModel.signOut()
                                        viewModel.reinitializeProcessState()
                                        navController.navigate(LOG_IN_SCREEN) {
                                            clearBackStack(navController)
                                        }
                                    },
                                    onUsersClicked = {
                                        navController.navigate(USERS_SCREEN) {
                                            launchSingleTop = true
                                        }
                                    },
                                    viewModel = viewModel
                                )
                        }
                    ) {
                        NavHostScreen(
                            navController = navController,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

    }

    override fun onBackPressed() {
        super.onBackPressed()
        viewModel.reinitializeProcessState()
    }

    override fun onStop() {
        super.onStop()
        viewModel.updateUserStatues("offline")
    }


    override fun onStart() {
        super.onStart()
        viewModel.updateUserStatues("online")
    }

    override fun onRestart() {
        super.onRestart()
        viewModel.updateUserStatues("online")
    }
}