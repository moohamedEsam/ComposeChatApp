package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.R
import android.mohamed.jepackcomposechatapp.dataModels.ProfileUser
import android.mohamed.jepackcomposechatapp.utility.Constants.FRIENDS
import android.mohamed.jepackcomposechatapp.utility.Constants.NOT_FRIENDS
import android.mohamed.jepackcomposechatapp.utility.Constants.RECEIVED
import android.mohamed.jepackcomposechatapp.utility.Constants.SENT
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Message
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.transform.CircleCropTransformation
import com.google.accompanist.coil.rememberCoilPainter

@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    navController: NavController,
    userForProfile: ProfileUser,
    type: String
) {
    Log.d("ProfileScreen", "ProfileScreen ->  $userForProfile")
    Log.d("ProfileScreen", "ProfileScreen ->  ${userForProfile.imageUrl.value}")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        ProfileImage(viewModel, userForProfile)
        ProfileButtons(type, viewModel, userForProfile)
        Text(
            text = "name: ${userForProfile.firstName} ${userForProfile.lastName}",
            fontSize = 24.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(text = "gender: ${userForProfile.gender}", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProfileButtons(
    type: String,
    viewModel: MainViewModel,
    userForProfile: ProfileUser
) {
    var userType by remember {
        mutableStateOf(type)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (userType) {
            FRIENDS -> {
                Buttons(
                    firstButtonOnClick = {},
                    firstText = "message",
                    secondButtonOnClick = {
                        viewModel.removeFriend(userForProfile.id!!)
                        userType = NOT_FRIENDS
                    },
                    secondText = "unfriend",
                    icon = Icons.Default.Message
                )
            }
            SENT -> {
                Buttons(
                    firstButtonOnClick = {
                        viewModel.cancelFriendRequest(userForProfile.id!!)
                        userType = NOT_FRIENDS
                    },
                    firstText = "cancel request",
                    secondButtonOnClick = { },
                    secondText = "block",
                    icon = Icons.Default.Cancel
                )
            }
            RECEIVED -> {
                Buttons(
                    firstButtonOnClick = {
                        viewModel.acceptFriendRequest(userForProfile.id!!)
                        userType = FRIENDS
                    },
                    firstText = "accept",
                    secondButtonOnClick = {
                        viewModel.declineFriendRequest(userForProfile.id!!)
                        userType = NOT_FRIENDS
                    },
                    secondText = "decline",
                    icon = null
                )
            }
            NOT_FRIENDS -> {
                if (userForProfile.id != viewModel.currentUser.id!!)
                    Buttons(
                        firstButtonOnClick = {
                            viewModel.sendFriendRequest(userForProfile.id!!)
                            userType = SENT
                        },
                        firstText = "send request",
                        icon = Icons.Default.Add,
                        secondButtonOnClick = { },
                        secondText = "block"
                    )
            }
        }
    }
}

@Composable
private fun ProfileImage(
    viewModel: MainViewModel,
    userForProfile: ProfileUser
) {
    val bitmap =
        viewModel.loadPhotoFromInternalStorage(LocalContext.current, userForProfile.id ?: "")
    Spacer(modifier = Modifier.height(8.dp))
    bitmap?.let {
        Image(
            painter = rememberCoilPainter(
                request = it,
                requestBuilder = { transformations(CircleCropTransformation()) }),
            contentDescription = "",
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(300.dp)
                .border(width = 8.dp, color = Color.Black, shape = CircleShape),
            contentScale = ContentScale.FillBounds
        )
    } ?: run {
        val url by userForProfile.imageUrl.collectAsState()
        Log.d("ProfileImage", "ProfileImage ->  url : $url")
        if (url.isNotEmpty()) {
            viewModel.savePhotoToInternalStorage(
                LocalContext.current,
                url,
                userForProfile.id ?: ""
            )
            Image(
                painter = rememberCoilPainter(
                    request = url,
                    requestBuilder = { transformations(CircleCropTransformation()) }),
                contentDescription = "",
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(300.dp)
                    .border(width = 8.dp, color = Color.Black, shape = CircleShape),
                contentScale = ContentScale.FillBounds
            )
        }
    }
}

@Composable
private fun Buttons(
    firstButtonOnClick: () -> Unit,
    firstText: String,
    icon: ImageVector?,
    secondButtonOnClick: () -> Unit,
    secondText: String
) {
    Button(onClick = { firstButtonOnClick() }, modifier = Modifier.height(48.dp)) {
        icon?.let { icon ->
            Icon(imageVector = icon, contentDescription = "")
        } ?: run {
            Icon(
                painter = painterResource(id = R.drawable.accept_friend_foreground),
                contentDescription = "",
                modifier = Modifier
                    .width(48.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = firstText)
    }
    OutlinedButton(onClick = { secondButtonOnClick() }) {
        Text(text = secondText, color = Color.Red)
    }
}

