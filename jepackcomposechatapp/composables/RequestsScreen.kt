package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.R
import android.mohamed.jepackcomposechatapp.dataModels.User
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.transform.CircleCropTransformation
import com.google.accompanist.coil.rememberCoilPainter

@ExperimentalMaterialApi
@Composable
fun RequestScreen(viewModel: MainViewModel, navController: NavController) {
    var users by remember {
        mutableStateOf(listOf<User>())
    }
    viewModel.getReceivedRequestUsers {
        users = it
    }
    LazyColumn {
        items(users) { user ->
            RequestListItem(user = user, viewModel = viewModel) {
                users = users.minus(user)
            }
            Divider()
        }
    }

}

@ExperimentalMaterialApi
@Composable
fun RequestListItem(user: User, viewModel: MainViewModel, onOperationDone: () -> Unit) {
    val imageUrl by user.imageUrl.collectAsState()

    ListItem(
        modifier = Modifier.fillMaxWidth(),
        icon = {
            if (imageUrl.isNotEmpty())
                Image(
                    painter = rememberCoilPainter(
                        request = imageUrl,
                        requestBuilder = { transformations(CircleCropTransformation()) }
                    ),
                    contentDescription = "",
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                )
        },
        text = { Text(user.username ?: "", fontSize = 24.sp) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.accept_friend_foreground),
                    contentDescription = "",
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .clickable {
                            viewModel.acceptFriendRequest(user.id!!)
                            onOperationDone()
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "",
                    modifier = Modifier.clickable {
                        viewModel.declineFriendRequest(user.id!!)
                        onOperationDone()
                    }
                )
            }
        }
    )
}
