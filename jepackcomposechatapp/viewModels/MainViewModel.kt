package android.mohamed.jepackcomposechatapp.viewModels

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.mohamed.jepackcomposechatapp.dataModels.*
import android.mohamed.jepackcomposechatapp.dataModels.Message.PhotoMessage
import android.mohamed.jepackcomposechatapp.dataModels.Message.TextMessage
import android.mohamed.jepackcomposechatapp.repository.Repository
import android.mohamed.jepackcomposechatapp.utility.Constants.FIRST_NAME
import android.mohamed.jepackcomposechatapp.utility.Constants.FRIENDS
import android.mohamed.jepackcomposechatapp.utility.Constants.GENDER
import android.mohamed.jepackcomposechatapp.utility.Constants.ID
import android.mohamed.jepackcomposechatapp.utility.Constants.IMAGE_URL
import android.mohamed.jepackcomposechatapp.utility.Constants.LAST_NAME
import android.mohamed.jepackcomposechatapp.utility.Constants.NOT_FRIENDS
import android.mohamed.jepackcomposechatapp.utility.Constants.RECEIVED
import android.mohamed.jepackcomposechatapp.utility.Constants.SENT
import android.mohamed.jepackcomposechatapp.utility.Constants.USERNAME
import android.mohamed.jepackcomposechatapp.utility.Constants.UserImageBaseUrl
import android.mohamed.jepackcomposechatapp.utility.State
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class MainViewModel(private val repository: Repository) : ViewModel() {

    var currentUser = User()

    private val _userState: MutableStateFlow<State> = MutableStateFlow(
        if (repository.getCurrentUser()?.uid == null)
            State.UserState.LoggedOut()
        else {
            firstLogIn {
                if (!it)
                    initLoggedInUser()
            }
            State.UserState.LoggedIn()
        }
    )

    private fun initLoggedInUser() {
        syncUserOnline()
        getUserFriends(repository.currentUserId!!)
        getReceivedRequests()
        getSentRequests()
        updateUserStatues("online")
    }

    val userState: StateFlow<State>
        get() = _userState
    private val _processState: MutableStateFlow<State> = MutableStateFlow(State.Initialized())
    val processState: StateFlow<State>
        get() = _processState

    private val _userFriends: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    private val _sentRequests: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    private val _receivedRequests: MutableStateFlow<List<String>> = MutableStateFlow(listOf())

    private val _userMessages = MutableStateFlow(listOf<Message>())
    val userTextMessage: StateFlow<List<Message>>
        get() = _userMessages


    fun signIn(email: String?, password: String?) {
        updateProcessState(State.Loading())
        viewModelScope.launch {
            if (validCredentials(email, password) && repository.getCurrentUser() == null)
                if (repository.signIn(
                        email!!.trim(),
                        password!!.trim(),
                        onFailure = { updateProcessState(State.Error(it)) })
                )
                    updateUserState(State.UserState.LoggedIn())
                else
                    _processState.emit(State.Error("empty username or password"))
        }
    }

    fun signUp(email: String?, password: String?, confirmedPassword: String?) {
        updateProcessState(State.Loading())
        if (!validCredentials(email = email, password = password)) {
            updateProcessState(State.Error("make sure to input all required data"))
            return
        } else if (confirmedPassword != password) {
            updateProcessState(State.Error("passwords aren't matching"))
            return
        }
        viewModelScope.launch {
            if (repository.signUp(
                    email!!.trim(),
                    password!!.trim(),
                    onFailure = { updateProcessState(State.Error(it)) })
            )
                updateProcessState(State.Success())
            else
                updateProcessState(State.Error("error happened"))
        }
    }

    fun firstLogIn(onFinish: (Boolean) -> Unit) {
        viewModelScope.launch {
            onFinish(repository.firstLogIn { updateProcessState(State.Error(it)) })
        }
    }


    private fun validCredentials(email: String?, password: String?): Boolean {
        if (email.isNullOrEmpty() || password.isNullOrEmpty())
            return false
        if (email.isBlank() || password.isBlank())
            return false
        return true
    }


    fun reinitializeProcessState() = viewModelScope.launch {
        updateProcessState(State.Initialized())
    }

    private fun updateUserState(state: State.UserState) = viewModelScope.launch {
        if (state is State.UserState.LoggedIn) {
            firstLogIn {
                if (!it)
                    initLoggedInUser()
            }
            repository.currentUserId = repository.getCurrentUser()!!.uid
        } else {
            updateUserStatues("offline")
            currentUser = User()
            resetUserLists()
            repository.currentUserId = null
        }
        _userState.emit(state)
    }

    private fun updateProcessState(state: State) {
        if (state is State.Initialized)
            _processState.value.message = ""
        viewModelScope.launch {
            _processState.emit(state)
        }
    }


    fun saveUserProfileData(context: Context, userForProfile: ProfileUser) {
        updateProcessState(State.Loading())
        userForProfile.id = repository.currentUserId
        if (!validUser(userForProfile = userForProfile)) {
            updateProcessState(State.Error("not a valid input make sure to fill all fields"))
            return
        }
        viewModelScope.launch {
            val result = repository.saveUserProfileData(
                userForProfile,
                onFailure = { updateProcessState(State.Error(it)) },
                context = context
            )
            if (result[IMAGE_URL].isNullOrEmpty())
                updateProcessState(State.Error("something went wrong"))
            else {
                updateProcessState(State.Success())
                currentUser.imageUrl.emit(result[IMAGE_URL]!!)
                currentUser.id = result[ID]
            }
        }
    }

    private fun validUser(userForProfile: ProfileUser): Boolean {
        return checkUserMember(userForProfile.id) && checkUserMember(userForProfile.username) && checkUserMember(
            userForProfile.firstName
        ) && checkUserMember(
            userForProfile.gender
        ) && checkUserMember(userForProfile.lastName)
    }

    private fun checkUserMember(value: String?): Boolean =
        !(value == null || value.isEmpty() || value.isBlank())

    fun getUserType(id: String): String {
        return when {
            _userFriends.value.contains(id) -> FRIENDS
            _receivedRequests.value.contains(id) -> RECEIVED
            _sentRequests.value.contains(id) -> SENT
            else -> NOT_FRIENDS
        }
    }

    private fun syncUserOnline() {
        viewModelScope.launch {
            val result = repository.getUserData(
                onFailure = { error -> updateProcessState(State.Error(error)) },
            )
            result.apply {
                currentUser.id = this[ID]?.toStringOrNull()
                currentUser.username = this[USERNAME]?.toStringOrNull()
                currentUser.imageUrl.emit(this[IMAGE_URL]?.toStringOrNull() ?: "")
            }

        }
    }


    fun signOut() {
        viewModelScope.launch {
            if (repository.logOut(onFailure = { updateProcessState(State.Error(it)) }))
                updateUserState(State.UserState.LoggedOut())

        }
    }

    fun getAllUsers(onResult: (List<User>) -> Unit) {
        viewModelScope.launch {
            repository.getAllUsers(
                onFailure = {
                    updateProcessState(State.Error(it))
                },
                onFinish = {
                    val users = it.mapNotNull { documentSnapshot ->
                        documentSnapshot.data?.let { map ->
                            syncUser(map)
                        }
                    }
                    onResult(users)
                }
            )
        }
    }

    private fun syncProfileUser(map: MutableMap<String, Any>): ProfileUser {
        val user = ProfileUser()
        map.apply {
            user.id = this[ID]?.toStringOrNull()
            user.username = this[USERNAME]?.toStringOrNull()
            user.firstName = this[FIRST_NAME]?.toStringOrNull()
            user.lastName = this[LAST_NAME]?.toStringOrNull()
            user.gender = this[GENDER]?.toStringOrNull()
            user.currentStatus = "online"
            viewModelScope.launch {
                user.imageUrl.emit(this@apply[IMAGE_URL]?.toStringOrNull() ?: "")
            }
        }
        return user
    }

    private fun syncUser(map: MutableMap<String, Any>): User {
        val user = User()
        map.apply {
            user.id = this[ID]?.toStringOrNull()
            user.username = this[USERNAME]?.toStringOrNull()
            viewModelScope.launch {
                user.imageUrl.emit(
                    this@apply[IMAGE_URL]?.toStringOrNull() ?: ""
                )
            }
        }
        return user
    }

    private fun getTextMessageFromMap(map: MutableMap<String, Any>): TextMessage {
        val message = TextMessage("", "", "", "", Timestamp.now())
        map.apply {
            message.from = this["from"]!!.toStringOrNull() ?: ""
            message.to = this["to"]!!.toStringOrNull() ?: ""
            message.type = this["type"]!!.toStringOrNull() ?: ""
            message.data = this["data"]!!.toStringOrNull() ?: ""
            message.date = this["date"]!! as Timestamp
        }
        return message
    }

    private fun getPhotoMessageFromMap(map: MutableMap<String, Any>): PhotoMessage {
        val message = PhotoMessage("", "", "", "", null, Timestamp.now())
        map.apply {
            message.from = this["from"]!!.toStringOrNull() ?: ""
            message.to = this["to"]!!.toStringOrNull() ?: ""
            message.type = this["type"]!!.toStringOrNull() ?: ""
            message.data = this["data"]!!.toStringOrNull() ?: ""
            message.date = this["date"]!! as Timestamp
            message.fileName = this["fileName"]!!.toStringOrNull() ?: ""
        }
        return message
    }

    fun handleSearch(
        list: List<User>,
        searchQuery: String,
        onFinish: (List<User>) -> Unit
    ) {
        viewModelScope.launch {
            val results = list.filter { it.username?.contains(searchQuery) ?: false }
            onFinish(results)
        }
    }

    private fun Any.toStringOrNull(): String? {
        return if (this.toString() == "null")
            null
        else
            this.toString().trim()
    }

    override fun onCleared() {
        super.onCleared()
        updateUserStatues("offline")
        Log.d("MainViewModel", "onCleared ->  triggered")
    }

    private fun getUserFriends(id: String) {
        viewModelScope.launch {
            repository.getUserFriendsId(id) {
                Log.d("MainViewModel", "getUserFriends ->  called ${it.size}")
                if (_userFriends.tryEmit(it))
                    Log.d("MainViewModel", "getUserFriends ->  emitted")

            }
        }
    }


    private fun getSentRequests() {
        Log.d("MainViewModel", "getSentRequests ->  called")
        viewModelScope.launch {
            repository.getAllFieldRequest(repository.currentUserId ?: "", SENT) {
                it.forEach { id ->
                    Log.d("MainViewModel", "getSentRequests ->  $id")
                }
                if (_sentRequests.tryEmit(it))
                    Log.d("MainViewModel", "getSentRequests ->  emitted")
            }
        }
    }

    private fun getReceivedRequests() {
        Log.d("MainViewModel", "getReceivedRequests ->  called")
        viewModelScope.launch {
            repository.getAllFieldRequest(repository.currentUserId ?: "", RECEIVED) {
                it.forEach { value ->
                    Log.d("MainViewModel", "getReceivedRequests ->  changed $value")
                }
                if (_receivedRequests.tryEmit(it))
                    Log.d("MainViewModel", "getReceivedRequests ->  emitted")
            }
        }
    }

    fun sendFriendRequest(id: String) {
        viewModelScope.launch {
            repository.updateFriendRequest(
                SENT,
                FieldValue.arrayUnion(id),
                repository.currentUserId ?: ""
            )

            repository.updateFriendRequest(
                RECEIVED,
                FieldValue.arrayUnion(repository.currentUserId ?: ""),
                id
            )
        }
    }

    fun cancelFriendRequest(id: String) {
        viewModelScope.launch {
            repository.updateFriendRequest(
                SENT,
                FieldValue.arrayRemove(id),
                repository.currentUserId ?: ""
            )

            repository.updateFriendRequest(
                RECEIVED,
                FieldValue.arrayRemove(repository.currentUserId ?: ""),
                id
            )
        }
    }

    fun acceptFriendRequest(id: String) {
        viewModelScope.launch {
            repository.updateFriendRequest(
                RECEIVED,
                FieldValue.arrayRemove(id),
                repository.currentUserId ?: ""
            )

            repository.updateFriendRequest(
                SENT,
                FieldValue.arrayRemove(repository.currentUserId ?: ""),
                id
            )

            repository.updateFriend(repository.currentUserId ?: "", FieldValue.arrayUnion(id))

            repository.updateFriend(id, FieldValue.arrayUnion(repository.currentUserId ?: ""))
        }
    }

    private fun resetUserLists() {
        viewModelScope.launch {
            _userFriends.emit(emptyList())
            _sentRequests.emit(emptyList())
            _receivedRequests.emit(emptyList())
        }
    }

    fun getReceivedRequestUsers(onFinish: (List<User>) -> Unit) {
        Log.d("MainViewModel", "getReceivedRequestUsers ->  called")
        viewModelScope.launch {
            _receivedRequests.collect {
                Log.d("MainViewModel", "getReceivedRequestUsers ->  updated")
                getListRequestUsers(onFinish = onFinish, it)
            }
        }
    }

    fun getSentRequestUsers(onFinish: (List<User>) -> Unit) {
        Log.d("MainViewModel", "getSentRequestUsers ->  called ")
        viewModelScope.launch {
            _sentRequests.collect {
                Log.d("MainViewModel", "getSentRequestUsers ->  updated")
                getListRequestUsers(onFinish = onFinish, it)
            }
        }
    }

    fun getUserFriends(onFinish: (List<User>) -> Unit) {
        Log.d("MainViewModel", "getUserFriends ->  called")
        viewModelScope.launch {
            _userFriends.collect {
                Log.d("MainViewModel", "getUserFriends ->  updated")
                getListRequestUsers(onFinish = onFinish, it)
            }
        }
    }

    private fun getListRequestUsers(onFinish: (List<User>) -> Unit, list: List<String>) {
        viewModelScope.launch {
            repository.getUsersFromIdList(list) { list ->
                val users = list.mapNotNull {
                    it.data?.let { map ->
                        syncUser(map)
                    }
                }
                users.forEach {
                    Log.d("MainViewModel", "getListRequestUsers ->  ${it.id}")
                }
                onFinish(users)
            }
        }
    }


    fun declineFriendRequest(id: String) {
        viewModelScope.launch {
            repository.updateFriendRequest(
                RECEIVED,
                FieldValue.arrayRemove(id),
                repository.currentUserId!!
            )
            repository.updateFriendRequest(
                SENT,
                FieldValue.arrayRemove(repository.currentUserId!!),
                id
            )
        }
    }

    fun removeFriend(id: String) {
        viewModelScope.launch {
            repository.updateFriend(repository.currentUserId!!, FieldValue.arrayRemove(id))
            repository.updateFriend(id, FieldValue.arrayRemove(repository.currentUserId!!))
        }
    }

    fun sendMessage(message: Message, context: Context? = null) {
        viewModelScope.launch {
            if (repository.userFirstMessage(message.to)) {
                handleMessageKind(message, context)
                repository.handleUserChatId(message.from, message.to)
                getMessages(message.to)
            } else
                handleMessageKind(message = message, context)

        }
    }

    private suspend fun handleMessageKind(
        message: Message,
        context: Context?
    ) {
        if (message is TextMessage)
            repository.sendMessage(message, true)
        else {
            context?.let {
                savePhotoMessage(it, message.data) { url, fileName ->
                    message.data = url
                    message.fileName = fileName
                    viewModelScope.launch { repository.sendMessage(message, false) }
                        .invokeOnCompletion { throwable ->
                            if (throwable == null) {
                                Log.d("MainViewModel", "sendMessage ->  message uploaded")
                            }
                        }
                }
            }
        }
    }

    fun getMessages(id: String) {
        viewModelScope.launch {
            repository.getMessages(id) {
                val messages = it.mapNotNull { document ->
                    document.data?.let { map ->
                        if (map.containsKey("fileName") && map["fileName"] != null)
                            getPhotoMessageFromMap(map)
                        else
                            getTextMessageFromMap(map)
                    }
                }
                _userMessages.tryEmit(_userMessages.value.plus(messages))
            }
        }
    }

    fun getUserChatList(onFinish: (List<User>) -> Unit) {
        viewModelScope.launch {
            val usersId = repository.getUserChatList()
            repository.getUsersFromIdList(usersId) { documents ->
                val users = documents.mapNotNull { documentSnapshot ->
                    documentSnapshot.data?.let { map ->
                        syncUser(map)
                    }
                }
                users.forEach {
                    Log.d("MainViewModel", "getUserChatList ->  ${it.username}")
                }
                onFinish(users)
            }
        }
    }

    fun resetLastDocument() = repository.resetLastDocument()
    fun resetUserMessages() {
        viewModelScope.launch {
            _userMessages.emit(emptyList())
        }
    }

    private fun savePhotoMessage(
        context: Context,
        uri: String,
        onFinish: (url: String, fileName: String) -> Unit
    ) {
        viewModelScope.launch {
            val map = repository.uploadAndSavePhotoMessage(uri, context)
            onFinish(map["url"] ?: "", map["fileName"] ?: "")
        }
    }

    fun updateAndSavePhotoMessage(message: PhotoMessage, context: Context) {
        viewModelScope.launch {
            repository.updatePhotoMessage(message = message, context)
        }
    }

    fun getUserPersonalInfo(user: User, onFinish: (ProfileUser) -> Unit) {
        viewModelScope.launch {
            val document = repository.getUserPersonalInfo(user.id ?: "")
            document?.data?.let {
                val profileUser = ProfileUser(
                    user.username,
                    user.id,
                    user.imageUrl,
                    it[GENDER]?.toStringOrNull(),
                    it[FIRST_NAME]?.toStringOrNull(),
                    it[LAST_NAME]?.toStringOrNull(),
                )
                Log.d("MainViewModel", "getUserPersonalInfo ->  ${user.imageUrl.value}")
                onFinish(profileUser)
            }
        }
    }

    fun updateUserStatues(statues: String) {
        if (repository.currentUserId != null)
            repository.updateCurrentUserStatues(statues = statues)
    }

    fun savePhotoToInternalStorage(
        context: Context,
        url: String = currentUser.imageUrl.value,
        id: String = repository.currentUserId!!
    ) {
        viewModelScope.launch {
            val bitmap = repository.getBitmapFromUrl(context, url)
            bitmap?.let {
                if (bitmap is BitmapDrawable)
                    repository.saveImageToInternalStorage(
                        id,
                        bitmap.bitmap,
                        context
                    )
            }
        }
    }

    fun loadPhotoFromInternalStorage(
        context: Context,
        id: String? = repository.currentUserId
    ): Bitmap? {
        Log.d("MainViewModel", "loadPhotoFromInternalStorage ->  id $id")
        id?.let {
            return repository.loadImageFromInternalStorage(it, context).also { result ->
                Log.d("MainViewModel", "loadPhotoFromInternalStorage ->  result $result")
            }
        }
        return null
    }

    fun getUserImageUrl(id: String, token: String) =
        "$UserImageBaseUrl%2F$id?alt=media&token=$token"
/*fun writeFakeData (onFinish: (Boolean) -> Unit) = viewModelScope.launch {
if(repository.writeFakeData())
    onFinish(true)
}*/
}
