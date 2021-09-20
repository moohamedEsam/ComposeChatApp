package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.CHAT_LIST_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.FILL_PROFILE_DATA_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.LOG_IN_SCREEN
import android.mohamed.jepackcomposechatapp.utility.State.UserState.LoggedIn
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun SplashScreen(navController: NavController, viewModel: MainViewModel) {
    val userState by viewModel.userState.collectAsState()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "chat app", fontSize = 36.sp, fontStyle = FontStyle.Italic)
        }
        if (userState is LoggedIn) {
            navController.popBackStack()
            viewModel.firstLogIn {
                if (it)
                    navController.navigate(FILL_PROFILE_DATA_SCREEN)
                else
                    navController.navigate(CHAT_LIST_SCREEN)
            }
        } else {
            navController.popBackStack()
            navController.navigate(LOG_IN_SCREEN)
        }
    }
}