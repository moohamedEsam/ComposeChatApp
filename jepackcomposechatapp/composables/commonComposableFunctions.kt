package android.mohamed.jepackcomposechatapp.composables

import android.graphics.Bitmap
import android.mohamed.jepackcomposechatapp.dataModels.User
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation
import android.mohamed.jepackcomposechatapp.utility.State
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import coil.transform.CircleCropTransformation
import com.google.accompanist.coil.rememberCoilPainter

@Composable
fun LoginSetup(
    email: String,
    password: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        TextFieldSetup(
            value = email,
            onValueChange = { onEmailChange(it) },
            label = "Email",
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = Icons.Default.Email
        )
        Spacer(modifier = Modifier.height(20.dp))

        TextFieldSetup(
            value = password,
            onValueChange = { onPasswordChange(it) },
            label = "Password",
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isPassword = true,
            leadingIcon = Icons.Default.Lock
        )
    }

}

@Composable
fun TextFieldSetup(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions(),
    isPassword: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    var passwordVisible by remember {
        mutableStateOf(false)
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = keyboardOptions,
        leadingIcon = {
            if (leadingIcon != null)
                Icon(imageVector = leadingIcon, contentDescription = "")
        },
        visualTransformation = if (isPassword && !passwordVisible)
            PasswordVisualTransformation('*')
        else
            VisualTransformation.None,
        trailingIcon = {
            if (isPassword) {
                if (passwordVisible)
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "",
                        modifier = Modifier.clickable { passwordVisible = false })
                else
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "",
                        modifier = Modifier.clickable { passwordVisible = true })
            }

        }
    )
}

@Composable
fun CircleImage(url: String, onPhotoClicked: () -> Unit) {
    Image(
        painter = rememberCoilPainter(
            request = url,
            requestBuilder = { transformations(CircleCropTransformation()) },
            fadeIn = true
        ),
        contentDescription = "",
        modifier = Modifier
            .width(58.dp)
            .height(58.dp)
            .clickable { onPhotoClicked() }
            .border(width = 4.dp, color = Color.Black, shape = CircleShape)
    )


}

@Composable
fun CircleImage(bitmap: Bitmap, onPhotoClicked: () -> Unit) {
    Image(
        painter = rememberCoilPainter(
            request = bitmap,
            requestBuilder = { transformations(CircleCropTransformation()) },
            fadeIn = true
        ),
        contentDescription = "",
        modifier = Modifier
            .width(58.dp)
            .height(58.dp)
            .clickable { onPhotoClicked() }
            .border(width = 4.dp, color = Color.Black, shape = CircleShape)
    )
}

@Composable
fun ConfirmAndError(
    processState: State,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    onErrorModifier: Modifier = Modifier
) {
    when (processState) {
        is State.Loading -> {
            Row(horizontalArrangement = Arrangement.Center, modifier = modifier) {
                CircularProgressIndicator(color = MaterialTheme.colors.primary)
            }
        }
        is State.Error -> {
            Text(
                text = processState.message.toString(),
                color = Color.Red,
                modifier = onErrorModifier,
                fontSize = 14.sp
            )
        }
        is State.Success -> {
            onSuccess()
        }
        else -> {
        }
    }
}

fun NavOptionsBuilder.clearBackStack(navController: NavController) {
    navController.backQueue[1].destination.route?.let {
        popUpTo(it) {
            inclusive = true
        }
    }
}

@ExperimentalMaterialApi
@Composable
fun UserListItem(
    user: User,
    viewModel: MainViewModel,
    navController: NavController,
    route: String = ScreenNavigation.PROFILE_SCREEN,
    savePhoto: Boolean = false,
    photoModifier: Modifier
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                listItemOnClick(user, route, viewModel, navController)
            },
        icon = {
            ListItemImage(viewModel, user, savePhoto, photoModifier)
        },
        text = { Text(text = user.username ?: "") },
    )
    Divider()
}


private fun listItemOnClick(
    user: User,
    route: String,
    viewModel: MainViewModel,
    navController: NavController
) {
    val token = user.imageUrl.value
        .takeLastWhile { char ->
            char != '='
        }
        .also { Log.d("UserListItem", "UserListItem ->  token $it") }
    when (route) {
        ScreenNavigation.PROFILE_SCREEN -> {
            viewModel.getUserPersonalInfo(user) {
                val profileUser = it
                navController.navigate(
                    "${ScreenNavigation.PROFILE_SCREEN}/" +
                            "${user.id}/" +
                            "${user.username}/" +
                            "${profileUser.firstName}/" +
                            "${profileUser.lastName}/" +
                            "${token}/" +
                            "${profileUser.gender}"
                )
            }
        }
        ScreenNavigation.CHAT_SCREEN -> {
            navController.navigate(
                "$route/${user.id}/${user.username}/${token}"
            )
        }
    }
}

@Composable
fun ListItemImage(
    viewModel: MainViewModel,
    user: User,
    savePhoto: Boolean,
    modifier: Modifier,
) {
    val bitmap = viewModel.loadPhotoFromInternalStorage(LocalContext.current, user.id)
    bitmap?.let {
        Image(
            painter = rememberCoilPainter(
                request = it,
                requestBuilder = { transformations(CircleCropTransformation()) }),
            contentDescription = "",
            modifier = modifier
        )
    } ?: run {
        val imageUrl by user.imageUrl.collectAsState()
        if (savePhoto)
            viewModel.savePhotoToInternalStorage(LocalContext.current, imageUrl, user.id!!)
        if (imageUrl.isNotEmpty())
            Image(
                painter = rememberCoilPainter(
                    request = imageUrl,
                    requestBuilder = { transformations(CircleCropTransformation()) }),
                contentDescription = "",
                modifier = modifier
            )
    }
}