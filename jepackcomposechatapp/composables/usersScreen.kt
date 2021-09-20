package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.dataModels.User
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


@ExperimentalMaterialApi
@Composable
fun UserScreen(
    viewModel: MainViewModel,
    navController: NavController
) {
    var users by remember {
        mutableStateOf(listOf<User>())
    }
    if (users.isEmpty())
        viewModel.getAllUsers {
            users = it
        }
    var searchQuery by remember {
        mutableStateOf("")
    }
    var searchMode by remember {
        mutableStateOf(false)
    }
    var searchResults by remember {
        mutableStateOf(users)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .animateContentSize(),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                if (it.isNotEmpty())
                    if (it.contains(searchQuery) && searchQuery.isNotEmpty())
                        viewModel.handleSearch(searchResults, it) { results ->
                            searchResults = results
                        }
                    else
                        viewModel.handleSearch(users, it) { results ->
                            searchResults = results
                        }
                searchQuery = it
                searchMode = it.isNotEmpty()
            },
            label = { Text("search") },
            modifier = Modifier
                .fillMaxWidth(),
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(
                if (searchMode)
                    searchResults
                else
                    users
            ) { user ->
                UserListItem(
                    user = user,
                    viewModel = viewModel,
                    navController = navController,
                    photoModifier = Modifier.size(48.dp)
                )
            }


        }
    }
}