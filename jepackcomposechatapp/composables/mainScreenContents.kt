package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.dataModels.ProfileUser
import android.mohamed.jepackcomposechatapp.dataModels.User
import android.mohamed.jepackcomposechatapp.utility.Constants.FIRST_NAME
import android.mohamed.jepackcomposechatapp.utility.Constants.GENDER
import android.mohamed.jepackcomposechatapp.utility.Constants.ID
import android.mohamed.jepackcomposechatapp.utility.Constants.IMAGE_TOKEN
import android.mohamed.jepackcomposechatapp.utility.Constants.LAST_NAME
import android.mohamed.jepackcomposechatapp.utility.Constants.USERNAME
import android.mohamed.jepackcomposechatapp.utility.NavigationScreens
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import android.util.Log
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navArgument
import kotlinx.coroutines.flow.MutableStateFlow

@ExperimentalMaterialApi
@Composable
fun NavHostScreen(
    navController: NavHostController,
    viewModel: MainViewModel
) {
    val startDestination: String = ScreenNavigation.SPLASH_SCREEN

    NavHost(
        navController = navController, startDestination = startDestination
    ) {
        composable(ScreenNavigation.LOG_IN_SCREEN) {
            SignInScreen(navController = navController, viewModel = viewModel)
        }
        composable(ScreenNavigation.CHAT_LIST_SCREEN) {
            ChatList(viewModel = viewModel, navController = navController)
        }
        composable(ScreenNavigation.NEW_ACCOUNT_SCREEN) {
            SignUpScreen(navController = navController, viewModel = viewModel)
        }
        composable(ScreenNavigation.FILL_PROFILE_DATA_SCREEN) {
            FillProfileDataScreen(
                navController = navController,
                viewModel = viewModel
            )
            RequestScreen(viewModel = viewModel, navController = navController)
        }
        composable(ScreenNavigation.REQUESTS_SCREEN) {
            RequestsScreen(viewModel = viewModel, navController = navController)
        }
        composable(ScreenNavigation.FRIENDS_SCREEN) {
            FriendsScreen(viewModel = viewModel, navController = navController)
        }
        composable(ScreenNavigation.USERS_SCREEN) {
            UserScreen(viewModel = viewModel, navController = navController)
        }
        composable(ScreenNavigation.SPLASH_SCREEN) {
            SplashScreen(navController = navController, viewModel = viewModel)
        }
        composable(
            "${ScreenNavigation.PROFILE_SCREEN}/" +
                    "{$ID}/" +
                    "{$USERNAME}/" +
                    "{$FIRST_NAME}/" +
                    "{$LAST_NAME}/" +
                    "{$IMAGE_TOKEN}/" +
                    "{$GENDER}",
            arguments = listOf(
                navArgument(ID) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(USERNAME) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(FIRST_NAME) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(LAST_NAME) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(IMAGE_TOKEN) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(GENDER) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString(ID)
            val firstName = navBackStackEntry.arguments?.getString(FIRST_NAME)
            val lastName = navBackStackEntry.arguments?.getString(LAST_NAME)
            val imageToken = navBackStackEntry.arguments?.getString(IMAGE_TOKEN)
            val username = navBackStackEntry.arguments?.getString(USERNAME)
            val gender = navBackStackEntry.arguments?.getString(GENDER)
            val imageUrl = viewModel.getUserImageUrl(id ?: "", imageToken ?: "").also {
                Log.d("NavHostScreen", "NavHostScreen ->  imageUrl $it")
            }
            val user =
                ProfileUser(username, id, MutableStateFlow(imageUrl), gender, firstName, lastName)
            val type = viewModel.getUserType(id ?: "")
            ProfileScreen(
                viewModel = viewModel,
                navController = navController,
                userForProfile = user,
                type = type
            )
        }

        composable(
            "${ScreenNavigation.CHAT_SCREEN}/" +
                    "{$ID}/" +
                    "{$USERNAME}/" +
                    "{$IMAGE_TOKEN}",
            arguments = listOf(
                navArgument(ID) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(USERNAME) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(IMAGE_TOKEN) {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString(ID)
            val imageToken = navBackStackEntry.arguments?.getString(IMAGE_TOKEN)
            val username = navBackStackEntry.arguments?.getString(USERNAME)
            val imageUrl = viewModel.getUserImageUrl(id ?: "", imageToken ?: "").also {
                Log.d("NavHostScreen", "NavHostScreen ->  imageUrl $it")
            }
            val user = User(id = id, username = username, imageUrl = MutableStateFlow(imageUrl))

            ChatScreen(user = user, viewModel = viewModel, navController = navController)
        }


    }
}

@Composable
fun BottomBarSetup(navController: NavHostController) {

    BottomAppBar(
        backgroundColor = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.primary,
    ) {
        val infiniteTransient = rememberInfiniteTransition()
        val unSeenColorTransition by infiniteTransient.animateColor(
            initialValue = MaterialTheme.colors.primary,
            targetValue = Color.Red,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = 1000),
                repeatMode = RepeatMode.Reverse
            )
        )
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val screens = listOf(
            NavigationScreens.ChatListScreen(),
            NavigationScreens.Friends(),
            NavigationScreens.Requests()
        )

        screens.forEach { screen ->
            BottomNavigationItem(
                selected = currentRoute == screen.route,
                onClick = {

                    navController.popBackStack()
                    navController.navigate(screen.route) {
                        launchSingleTop = true
                    }
                },
                icon = { Icon(imageVector = screen.icon, contentDescription = "") },
                label = { Text(text = screen.screenName) },
                /*selectedContentColor = if (currentRoute == screen.route)
                    unSeenColorTransition
                else
                    LocalContentColor.current*/
            )
        }
    }
}

@Composable
fun TopBarSetup(
    onLogOutIconClick: () -> Unit,
    onUsersClicked: () -> Unit,
    viewModel: MainViewModel
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.background,
        contentColor = MaterialTheme.colors.primary,
        contentPadding = PaddingValues(start = 8.dp)
    ) {
        Text(text = "my chat app", fontSize = 28.sp)
        Spacer(modifier = Modifier.weight(1f))
        AppBarDropDownMenu(
            onLogOutIconClick = onLogOutIconClick,
            onUsersClicked = onUsersClicked,
            viewModel = viewModel
        )

    }
}

@Composable
fun AppBarDropDownMenu(
    onLogOutIconClick: () -> Unit,
    onUsersClicked: () -> Unit,
    viewModel: MainViewModel
) {
    var expanded by remember {
        mutableStateOf(false)
    }
    Box(contentAlignment = Alignment.Center) {
        TopAppBarImage(viewModel) {
            expanded = !expanded
        }

        if (expanded) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(180.dp)
            ) {
                DropDownMenuItemSetup(
                    onClick = { onUsersClicked() },
                    title = "users",
                    icon = Icons.Default.People
                )
                DropDownMenuItemSetup(
                    onClick = { },
                    title = "settings",
                    icon = Icons.Default.Settings
                )
                DropDownMenuItemSetup(
                    onClick = { onLogOutIconClick() },
                    title = "log out",
                    icon = Icons.Default.Logout
                )
            }
        }
    }
}

@Composable
private fun TopAppBarImage(
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    val bitmap = viewModel.loadPhotoFromInternalStorage(LocalContext.current)
    Log.d("AppBarDropDownMenu", "AppBarDropDownMenu ->  $bitmap")
    bitmap?.let {
        CircleImage(bitmap = bitmap) {
            onClick()
        }
        Log.d("AppBarDropDownMenu", "AppBarDropDownMenu ->  photo loaded from storage")
    } ?: run {
        val url by viewModel.currentUser.imageUrl.collectAsState()
        if (url.isNotEmpty()) {
            viewModel.savePhotoToInternalStorage(
                LocalContext.current,
                url
            )
        }
        CircleImage(url = url) {
            onClick()
        }
        Log.d("AppBarDropDownMenu", "AppBarDropDownMenu ->  photo loaded from firestore")
    }
}

@Composable
fun DropDownMenuItemSetup(
    onClick: () -> Unit,
    title: String,
    icon: ImageVector
) {
    DropdownMenuItem(onClick = { onClick() }) {
        Icon(imageVector = icon, contentDescription = "")
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title)
    }
}

