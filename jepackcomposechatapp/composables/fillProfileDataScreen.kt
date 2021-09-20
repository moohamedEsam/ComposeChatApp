package android.mohamed.jepackcomposechatapp.composables

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.mohamed.jepackcomposechatapp.dataModels.ProfileUser
import android.mohamed.jepackcomposechatapp.utility.ScreenNavigation
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.coil.rememberCoilPainter
import kotlinx.coroutines.flow.MutableStateFlow


@Composable
fun FillProfileDataScreen(
    navController: NavController,
    viewModel: MainViewModel,
) {
    var firstName by rememberSaveable {
        mutableStateOf("")
    }

    var lastName by rememberSaveable {
        mutableStateOf("")
    }

    var username by rememberSaveable {
        mutableStateOf("")
    }

    var gender by rememberSaveable {
        mutableStateOf("")
    }

    var imageUrl by rememberSaveable {
        mutableStateOf("")
    }

    var showGenderMenu by remember {
        mutableStateOf(false)
    }
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK)
            result.data?.let { intent ->
                intent.data?.let { url ->
                    imageUrl = url.toString()
                }
            }
    }
    val askPermission =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                pickImage.launch(Intent(Intent.ACTION_PICK).apply {
                    type = "image/*"
                })
            }
        }
    val processState by viewModel.processState.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
            .animateContentSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = "update your profile",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontStyle = FontStyle.Italic
        )
        if (imageUrl.isBlank())
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "",
                tint = MaterialTheme.colors.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable {
                        askPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    }

            )
        else {
            Image(
                painter = rememberCoilPainter(
                    request = imageUrl,
                    fadeIn = true
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(200.dp)
                    .clickable {
                        askPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    },
                contentDescription = "",
                contentScale = ContentScale.FillWidth,
            )
        }

        Row(Modifier.fillMaxWidth()) {
            TextFieldSetup(
                value = firstName,
                onValueChange = { firstName = it },
                label = "first name",
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            TextFieldSetup(
                value = lastName,
                onValueChange = { lastName = it },
                label = "last name",
                modifier = Modifier.weight(1f)
            )
        }

        TextFieldSetup(
            value = username,
            onValueChange = { username = it },
            label = "username",
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (gender.isEmpty())
                    "gender"
                else
                    gender
            )
            Box {
                if (!showGenderMenu)
                    IconButton(onClick = { showGenderMenu = true }) {
                        Icon(imageVector = Icons.Default.ExpandMore, contentDescription = "")
                    }
                else
                    IconButton(onClick = { showGenderMenu = false }) {
                        Icon(imageVector = Icons.Default.ExpandLess, contentDescription = "")
                    }
                DropdownMenu(
                    expanded = showGenderMenu,
                    onDismissRequest = { showGenderMenu = false }) {
                    DropdownMenuItem(onClick = {
                        gender = "male"
                        showGenderMenu = false
                    }) {
                        Text(text = "male")
                    }
                    DropdownMenuItem(onClick = {
                        gender = "female"
                        showGenderMenu = false
                    }) {
                        Text(text = "female")
                    }
                }
            }
        }

        val context = LocalContext.current
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = {
                viewModel.saveUserProfileData(
                    context,
                    ProfileUser(
                        username = username,
                        id = null,
                        imageUrl = MutableStateFlow(imageUrl),
                        gender = gender,
                        firstName = firstName,
                        lastName = lastName,
                        currentStatus = "online"
                    )
                )
            }) {
                Text(text = "confirm")
            }
        }
        ConfirmAndError(
            processState = processState,
            modifier = Modifier.fillMaxWidth(),
            onSuccess = {
                Toast.makeText(context, "data has been saved", Toast.LENGTH_SHORT)
                    .show()
                navController.navigate(ScreenNavigation.CHAT_LIST_SCREEN) {
                    clearBackStack(navController)
                }

                viewModel.reinitializeProcessState()
            }
        )

    }

}