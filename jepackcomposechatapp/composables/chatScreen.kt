package android.mohamed.jepackcomposechatapp.composables

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.mohamed.jepackcomposechatapp.dataModels.Message
import android.mohamed.jepackcomposechatapp.dataModels.Message.PhotoMessage
import android.mohamed.jepackcomposechatapp.dataModels.Message.TextMessage
import android.mohamed.jepackcomposechatapp.dataModels.User
import android.mohamed.jepackcomposechatapp.utility.Constants.PHOTO
import android.mohamed.jepackcomposechatapp.utility.Constants.TEXT
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import com.google.accompanist.coil.rememberCoilPainter
import java.util.*

@Composable
fun ChatScreen(
    user: User,
    viewModel: MainViewModel,
    navController: NavController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize()
    ) {
        DefaultChatTopAppBar(viewModel = viewModel, user = user) {

        }
        ChatMessages(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .animateContentSize(),
            viewModel = viewModel,
            user = user
        )
        BottomChatScreen(user = user, viewModel = viewModel)
    }
    BackHandler {
        navController.backQueue.let {
            val previousRoute = it[it.size - 2].destination.route
            previousRoute?.let { route ->
                viewModel.resetLastDocument()
                viewModel.resetUserMessages()
                navController.popBackStack()
                navController.popBackStack()
                navController.navigate(route)
            }
        }
    }
}

@Composable
fun ChatMessages(modifier: Modifier, viewModel: MainViewModel, user: User) {
    val messages by viewModel.userTextMessage.collectAsState()
    val calendar = Calendar.getInstance()
    viewModel.getMessages(user.id!!)

    LazyColumn(
        modifier = modifier,
        reverseLayout = true,
        contentPadding = PaddingValues(4.dp)
    ) {
        items(messages) { item: Message ->

            calendar.time = item.date.toDate()
            var date = "${calendar.get(Calendar.HOUR)}:${calendar.get(Calendar.MINUTE)} "
            date = if (calendar.get(Calendar.AM) == 1)
                date.plus("Pm")
            else
                date.plus("Am")


            if (item.to == user.id) {
                CurrentUserMessageListItem(item, date, viewModel)
            } else {
                MessageCard(item = item, viewModel = viewModel, date = date)
            }
        }
    }

    BackHandler {
        viewModel.resetLastDocument()
    }
}


@Composable
private fun CurrentUserMessageListItem(item: Message, date: String, viewModel: MainViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Spacer(Modifier.weight(1f))
        MessageCard(item, viewModel, date)
    }
}

@Composable
private fun MessageCard(
    item: Message,
    viewModel: MainViewModel,
    date: String
) {
    Card(
        backgroundColor = MaterialTheme.colors.background,
        contentColor = Color.LightGray,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        ConstraintLayout(modifier = Modifier.padding(2.dp)) {
            val (messageT, dateT) = createRefs()
            when (item) {
                is TextMessage -> Text(
                    text = item.data,
                    fontSize = 18.sp,
                    modifier = Modifier.constrainAs(messageT) {
                        start.linkTo(parent.start)
                        top.linkTo(parent.top)
                    })
                is PhotoMessage -> {
                    val bitmap = viewModel.loadPhotoFromInternalStorage(
                        LocalContext.current,
                        item.fileName
                    )
                    Log.d("MessageCard", "MessageCard ->  bitmap $bitmap")
                    bitmap?.let {
                        Log.d("MessageCard", "MessageCard ->  loaded from bitmap")
                        Image(
                            painter = rememberCoilPainter(request = it),
                            contentDescription = "",
                            modifier = Modifier
                                .background(Color.Black)
                                .size(200.dp)
                                .constrainAs(messageT) {
                                    start.linkTo(parent.start)
                                    top.linkTo(parent.top)
                                },
                            contentScale = ContentScale.FillWidth
                        )
                    } ?: run {
                        Log.d("MessageCard", "MessageCard ->  loaded from url")
                        val url = item.data
                        viewModel.updateAndSavePhotoMessage(item, LocalContext.current)
                        Image(
                            painter = rememberCoilPainter(request = url),
                            contentDescription = "",
                            modifier = Modifier
                                .background(Color.Black)
                                .size(200.dp)
                                .constrainAs(messageT) {
                                    start.linkTo(parent.start)
                                    top.linkTo(parent.top)
                                },
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }

            Text(
                text = date,
                fontSize = 12.sp,
                modifier = Modifier.constrainAs(dateT) {
                    end.linkTo(parent.end)
                    if (item is TextMessage)
                        start.linkTo(messageT.end)
                    top.linkTo(messageT.bottom)
                })
        }
    }
}

@Composable
fun BottomChatScreen(user: User, viewModel: MainViewModel) {
    val context = LocalContext.current
    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        when (it.resultCode) {
            Activity.RESULT_OK -> {
                it.data?.let { intent ->
                    intent.data?.let { uri ->
                        val photoMessage = PhotoMessage(
                            from = viewModel.currentUser.id ?: "", to = user.id ?: "",
                            type = PHOTO, data = uri.toString(), fileName = null
                        )
                        viewModel.sendMessage(photoMessage, context)
                    }
                }
            }
        }

    }
    val askPermission =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(
            )
        ) { granted ->
            if (granted)
                pickImage.launch(Intent(Intent.ACTION_PICK).apply {
                    type = "image/*"
                })
        }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var messageContent by remember {
            mutableStateOf("")
        }
        val messageType = TEXT
        OutlinedTextField(
            value = messageContent,
            onValueChange = { messageContent = it },
            modifier = Modifier
                .weight(1f),
            label = {
                Text(
                    text = "type a message "
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = "",
                    modifier = Modifier
                        .clickable {
                            askPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        .size(40.dp)
                )
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        IconButton(onClick = {
            if (messageType == TEXT) {
                if (messageContent.isNotBlank()) {
                    val message = TextMessage(
                        from = viewModel.currentUser.id!!,
                        to = user.id!!,
                        type = messageType,
                        data = messageContent
                    )
                    viewModel.sendMessage(message)
                    messageContent = ""
                }
            }
        }) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "",
                modifier = Modifier
                    .width(60.dp)
                    .height(60.dp)
            )
        }
    }
}

@Composable
fun ChatScreenTopAppBar(user: User, viewModel: MainViewModel) {
    var searchMode by remember {
        mutableStateOf(false)
    }
    if (!searchMode)
        DefaultChatTopAppBar(viewModel = viewModel, user = user) {
            searchMode = true
        }
    else
        ChatTopAppBarSearchMode(
            viewModel = viewModel,
            onSearchClicked = {},
            onDismiss = { searchMode = false }
        )
}

@Composable
fun ChatTopAppBarSearchMode(
    viewModel: MainViewModel,
    onSearchClicked: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember {
        mutableStateOf("")
    }
    TopAppBar(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
        )
        IconButton(onClick = { onSearchClicked() }) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "")
        }
        IconButton(onClick = { onDismiss() }) {
            Icon(imageVector = Icons.Default.ArrowLeft, contentDescription = "")
        }
    }
}

@Composable
fun DefaultChatTopAppBar(viewModel: MainViewModel, user: User, onSearchClicked: () -> Unit) {
    Log.d("ChatScreenTopAppBar", "ChatScreenTopAppBar ->  url ${user.imageUrl.value}")
    TopAppBar(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 8.dp)) {
        val bitmap = viewModel.loadPhotoFromInternalStorage(LocalContext.current, user.id!!)
        bitmap?.let {
            CircleImage(bitmap = it) {

            }
        } ?: run {
            val url by user.imageUrl.collectAsState()
            CircleImage(url = url) {

            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = user.username ?: "", fontSize = 18.sp)
        Spacer(modifier = Modifier.weight(1f))
        ChatOptions { onSearchClicked() }
    }
}

@Composable
private fun ChatOptions(
    onSearchClicked: () -> Unit
) {
    var showMenu by remember {
        mutableStateOf(false)
    }
    Box(contentAlignment = Alignment.Center) {
        IconButton(onClick = {
            showMenu = !showMenu
        }) {
            Icon(imageVector = Icons.Default.Menu, contentDescription = "")
        }
        if (showMenu) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.width(150.dp)
            ) {
                DropDownMenuItemSetup(
                    onClick = { onSearchClicked() },
                    title = "search",
                    icon = Icons.Default.Search
                )
            }
        }
    }
}
