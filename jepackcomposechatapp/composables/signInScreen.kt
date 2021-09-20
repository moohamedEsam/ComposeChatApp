package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation
import android.mohamed.jepackcomposechatapp.utility.State
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController


@Composable
fun SignInScreen(
    navController: NavController,
    viewModel: MainViewModel,
) {
    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }

    val userState by viewModel.userState.collectAsState()
    val processState by viewModel.processState.collectAsState()


    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val (welcomeText, login, signInButton, signUpButton, progressBarAndErrorText) = createRefs()

        Text(
            text = "welcome to my app", modifier = Modifier.constrainAs(welcomeText) {
                top.linkTo(parent.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                bottom.linkTo(login.top)
            },
            fontSize = 28.sp,
            fontStyle = FontStyle.Italic
        )

        LoginSetup(
            email = email, password = password,
            onEmailChange = {
                email = it
            },
            onPasswordChange = {
                password = it
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .constrainAs(login) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
        )

        Button(
            onClick = { viewModel.signIn(email, password) },
            modifier = Modifier.constrainAs(signInButton) {
                start.linkTo(parent.start)
                top.linkTo(login.bottom, 16.dp)
            }
        ) {
            Text(text = "sign in")
        }

        TextButton(onClick = {
            navController.navigate(ScreenNavigation.NEW_ACCOUNT_SCREEN)
            viewModel.reinitializeProcessState()
        },
            modifier = Modifier.constrainAs(signUpButton) {
                start.linkTo(signInButton.end, 8.dp)
                top.linkTo(login.bottom, 16.dp)
            }
        ) {
            Text(text = "create new account")
        }
        ConfirmAndError(
            processState = processState,
            onSuccess = {},
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(progressBarAndErrorText) {
                    start.linkTo(parent.start)
                    top.linkTo(signInButton.bottom)
                    bottom.linkTo(parent.bottom)
                },
            onErrorModifier = Modifier.constrainAs(progressBarAndErrorText) {
                start.linkTo(parent.start, 16.dp)
                top.linkTo(signInButton.bottom)
                bottom.linkTo(parent.bottom)
            })
        if (userState is State.UserState.LoggedIn) {
            viewModel.firstLogIn {
                if (it)
                    navController.navigate(ScreenNavigation.FILL_PROFILE_DATA_SCREEN) {
                        clearBackStack(navController = navController)
                    }
                else
                    navController.navigate(ScreenNavigation.CHAT_LIST_SCREEN) {
                        clearBackStack(navController = navController)
                    }
            }
            viewModel.reinitializeProcessState()
        }
    }

}
