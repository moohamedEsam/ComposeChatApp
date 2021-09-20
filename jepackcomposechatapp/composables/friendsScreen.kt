package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.dataModels.User
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@ExperimentalMaterialApi
@Composable
fun FriendsScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    var users by remember {
        mutableStateOf(listOf<User>())
    }

    viewModel.getUserFriends {
        users = it
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(users) { user ->
            UserListItem(
                user = user,
                viewModel = viewModel,
                navController = navController,
                savePhoto = true,
                photoModifier = Modifier.size(48.dp)
            )
        }
    }

}