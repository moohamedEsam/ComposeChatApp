package android.mohamed.jepackcomposechatapp.composables

import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController


@Composable
fun SignUpScreen(
    navController: NavController,
    viewModel: MainViewModel
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        var email by remember {
            mutableStateOf("")
        }

        var password by remember {
            mutableStateOf("")
        }

        var confirmedPassword by remember {
            mutableStateOf("")
        }

        val processState by viewModel.processState.collectAsState()

        val (welcomeText, login, confirmPasswordTF, signUpButton, errorAndProgress) = createRefs()
        Text(
            text = "create an account",
            fontSize = 28.sp,
            fontStyle = FontStyle.Italic,
            modifier = Modifier.constrainAs(welcomeText) {
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                top.linkTo(parent.top)
                bottom.linkTo(login.top)
            }
        )

        LoginSetup(
            email = email,
            password = password,
            onEmailChange = { email = it },
            onPasswordChange = { password = it },
            modifier = Modifier
                .constrainAs(login) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth()
                .padding(8.dp)
        )
        TextFieldSetup(
            value = confirmedPassword,
            onValueChange = { confirmedPassword = it },
            label = "confirm password",
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(confirmPasswordTF) {
                    start.linkTo(login.start)
                    top.linkTo(login.bottom, 8.dp)
                }
                .padding(8.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isPassword = true,
            leadingIcon = Icons.Default.Lock
        )
        Button(
            onClick = {
                viewModel.signUp(
                    email = email,
                    password = password,
                    confirmedPassword = confirmedPassword
                )
            },
            modifier = Modifier
                .constrainAs(signUpButton) {
                    top.linkTo(confirmPasswordTF.bottom, 16.dp)
                    start.linkTo(confirmPasswordTF.start)
                }
                .padding(8.dp)
        ) {
            Text(text = "sign up")
        }
        val context = LocalContext.current
        ConfirmAndError(
            processState = processState,
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(errorAndProgress) {
                    top.linkTo(signUpButton.bottom, 16.dp)
                    start.linkTo(parent.start)
                },
            onErrorModifier = Modifier.constrainAs(errorAndProgress) {
                start.linkTo(parent.start)
                top.linkTo(signUpButton.bottom, 16.dp)
            },
            onSuccess = {
                navController.navigate(ScreenNavigation.LOG_IN_SCREEN)
                Toast.makeText(
                    context,
                    "account has been created please verify the email",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.reinitializeProcessState()
            }
        )
    }

}