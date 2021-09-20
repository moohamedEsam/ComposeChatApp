package android.mohamed.jepackcomposechatapp.utility

import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.CHAT_LIST_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.FRIENDS_SCREEN
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation.REQUESTS_SCREEN
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.ui.graphics.vector.ImageVector

object ScreenNavigation {
    const val LOG_IN_SCREEN = "loginScreen"
    const val CHAT_LIST_SCREEN = "chatListScreen"
    const val NEW_ACCOUNT_SCREEN = "newAccountScreen"
    const val FILL_PROFILE_DATA_SCREEN = "fillProfileDataScreen"
    const val FRIENDS_SCREEN = "friendsScreen"
    const val REQUESTS_SCREEN = "requestsScreen"
    const val USERS_SCREEN = "usersScreen"
    const val SPLASH_SCREEN = "splashScreen"
    const val PROFILE_SCREEN = "profileScreen"
    const val CHAT_SCREEN = "chatScreen"
}

sealed class NavigationScreens(val route: String, val screenName: String, val icon: ImageVector) {
    class ChatListScreen : NavigationScreens(CHAT_LIST_SCREEN, "chat", Icons.Default.Chat)

    class Friends : NavigationScreens(FRIENDS_SCREEN, "friends", Icons.Default.People)

    class Requests : NavigationScreens(REQUESTS_SCREEN, "requests", Icons.Default.Add)
}
