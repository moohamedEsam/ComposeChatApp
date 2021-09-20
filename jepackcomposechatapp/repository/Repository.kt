package android.mohamed.jepackcomposechatapp.repository

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.mohamed.jepackcomposechatapp.dataModels.Message
import android.mohamed.jepackcomposechatapp.dataModels.Message.PhotoMessage
import android.mohamed.jepackcomposechatapp.dataModels.ProfileUser
import android.mohamed.jepackcomposechatapp.dataModels.User
import android.mohamed.jepackcomposechatapp.utility.Constants.ID
import android.mohamed.jepackcomposechatapp.utility.Constants.IMAGE_URL
import android.util.Log
import androidx.core.net.toUri
import coil.Coil
import coil.request.ImageRequest
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap


class Repository(
    private val authentication: FirebaseAuth,
    firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val users = firestore.collection("users")
    private val usersPersonalInfo = firestore.collection("userPersonalInfo")
    private val usersFriends = firestore.collection("friends")
    private val userFriendRequests = firestore.collection("requests")
    private val userImageRef = storage.reference.root.child("usersImages")
    private val messages = firestore.collection("messages")
    var currentUserId = getCurrentUser()?.uid
    private var lastDocument: DocumentSnapshot? = null

    suspend fun signUp(email: String, password: String, onFailure: (String) -> Unit): Boolean {
        return try {
            authentication.createUserWithEmailAndPassword(email, password).await()
            authentication.currentUser?.sendEmailVerification()?.await()
            true
        } catch (exception: Exception) {
            if (currentUserId != null)
                logOut(onFailure)
            onFailure(exception.message ?: "")
            exception.printStackTrace()
            Log.d("Repository", "signUp ->  ${exception.message}")
            false
        }
    }

    suspend fun signIn(email: String, password: String, onFailure: (String) -> Unit): Boolean {
        return try {
            authentication.signInWithEmailAndPassword(email, password).await()
            if (authentication.currentUser?.isEmailVerified == true)
                true
            else {
                logOut { onFailure(it) }
                onFailure("email not verified")
                false
            }
        } catch (exception: Exception) {
            exception.printStackTrace()
            authentication.currentUser?.let {
                logOut { onFailure("") }
            }
            onFailure(exception.message ?: "")
            Log.d("Repository", "signIn ->  ${exception.message}")
            false
        }
    }

    private suspend fun uploadUserPhoto(
        user: User,
        onFailure: (String) -> Unit,
        context: Context
    ): String {
        return try {
            getBitmapFromUrl(context, user.imageUrl.value)?.let { drawable ->
                if (drawable is BitmapDrawable) {
                    if (saveImageToInternalStorage(user.id!!, drawable.bitmap, context))
                        Log.d("Repository", "uploadUserPhoto ->  photo saved in the storage")
                }
            }
            val updatedUrl =
                userImageRef.child(user.id!!).putFile(user.imageUrl.value.toUri()).await()
                    .storage.downloadUrl.await().toString()
            Log.d("Repository", "uploadUserPhoto ->  photo uploaded successfully")
            updatedUrl
        } catch (exception: Exception) {
            onFailure(exception.message ?: "")
            exception.printStackTrace()
            Log.d("Repository", "uploadUserPhoto: ${exception.message}")
            ""
        }
    }

    suspend fun getBitmapFromUrl(
        context: Context,
        url: String
    ) = Coil.execute(
        ImageRequest.Builder(context).data(url).build()
    ).drawable

    private suspend fun saveUserData(user: ProfileUser, onFailure: (String) -> Unit): String {
        return try {
            val simpleUser = User(user.id, user.username, user.imageUrl, user.currentStatus)
            users.document(user.id!!).set(simpleUser.toSimpleUserHashMap()).await()
            usersPersonalInfo.document(user.id!!).set(user.toSimpleUserHashMap()).await()
            userFriendRequests.document(user.id!!)
                .set(mutableMapOf("sent" to emptyList<String>(), "received" to emptyList()))
            usersFriends.document(user.id!!).set(mutableMapOf("friends" to emptyList<String>()))
            messages.document(user.id!!).let {
                it.collection("userMessages")
                it.set(mapOf("userChatId" to listOf<String>()))
            }

            getCurrentUser()?.uid ?: ""
        } catch (exception: Exception) {
            exception.printStackTrace()
            onFailure(exception.message ?: "")
            Log.d("Repository", "saveUserData: ${exception.message}")
            ""
        }
    }

    suspend fun saveUserProfileData(
        user: ProfileUser,
        onFailure: (String) -> Unit,
        context: Context
    ): HashMap<String, String> {
        val map = HashMap<String, String>()
        return try {
            map[IMAGE_URL] = uploadUserPhoto(user.toSimpleUser(), onFailure, context)
            user.imageUrl.value = map[IMAGE_URL]!!
            map[ID] = saveUserData(user, onFailure)
            map
        } catch (exception: Exception) {
            onFailure(exception.message ?: "")
            map
        }
    }

    suspend fun getUserData(
        onFailure: (String) -> Unit,
        id: String = currentUserId ?: ""
    ): MutableMap<String, Any> {
        return try {
            Log.d("Repository", "getCurrentUserData ->  called")
            users.document(id).get().await().data ?: mutableMapOf()
        } catch (exception: Exception) {
            onFailure(exception.message ?: "")
            exception.printStackTrace()
            Log.d("Repository", "getCurrentUserData ->  ${exception.message}")
            mutableMapOf()
        }
    }

    suspend fun firstLogIn(onFailure: (String) -> Unit): Boolean {
        return try {
            !users.document(currentUserId ?: "").get().await().exists()
        } catch (exception: Exception) {
            onFailure(exception.message ?: "")
            Log.d("Repository", "firstLogIn ->  ${exception.message}")
            true
        }
    }

    fun getCurrentUser() = authentication.currentUser

    fun logOut(onFailure: (String) -> Unit): Boolean {
        return try {
            authentication.signOut()
            getCurrentUser() == null
        } catch (exception: Exception) {
            onFailure(exception.message ?: "")
            Log.d("Repository", "logOut ->  ${exception.message}")
            false
        }
    }

    suspend fun getUserProfile(id: String = currentUserId!!): MutableMap<String, Any> {
        return try {
            val map = users.document(id).get().await().data
            val personalInfo = getUserPersonalInfo(id)
            map?.plusAssign(personalInfo?.data ?: mutableMapOf())
            map ?: mutableMapOf()
        } catch (exception: Exception) {
            Log.d("Repository", "getUserProfile ->  ${exception.message}")
            mutableMapOf()
        }
    }

    suspend fun getUserPersonalInfo(id: String): DocumentSnapshot? {
        return try {
            usersPersonalInfo.document(id).get().await()
        } catch (exception: Exception) {
            Log.d("Repository", "getUserPersonalInfo ->  ${exception.message}")
            null
        }
    }


    suspend fun getAllUsers(
        onFailure: (String) -> Unit,
        onFinish: (MutableList<DocumentSnapshot>) -> Unit
    ) {

        try {
            users.addSnapshotListener { value, error ->
                error?.let {
                    onFailure(it.message ?: "")
                }
                value?.documents?.let { list ->
                    onFinish(list)
                }
            }
        } catch (exception: Exception) {
            onFailure(exception.message ?: "")
            exception.printStackTrace()
        }
    }

    suspend fun getUserFriendsId(id: String, onFinish: (List<String>) -> Unit) {
        try {
            usersFriends.document(id).addSnapshotListener { value, error ->
                error?.let { firebaseFirestoreException ->
                    Log.d(
                        "Repository",
                        "getUserFriendsId ->  ${firebaseFirestoreException.message}"
                    )
                }
                value?.let {
                    (it["friends"] as List<String>).forEach { userId ->
                        Log.d("Repository", "getUserFriendsId ->  list item $userId")
                    }
                    onFinish(it["friends"] as List<String>)
                }
            }
        } catch (exception: Exception) {
            Log.d("Repository", "getUserFriendsId ->  ${exception.message}")
        }
    }

    suspend fun getAllFieldRequest(id: String, field: String, onFinish: (List<String>) -> Unit) {
        try {
            Log.d("Repository", "getAllFieldRequest ->  $field called")
            userFriendRequests.document(id).addSnapshotListener { value, error ->
                error?.let { firebaseFirestoreException ->
                    Log.d(
                        "Repository",
                        "getAllFieldRequest ->  $field -> ${firebaseFirestoreException.message}"
                    )
                }

                value?.data?.get(field)?.let {
                    onFinish(it as List<String>)
                }
            }
        } catch (exception: Exception) {
            Log.d("Repository", "getAllFieldRequest -> $field ->  ${exception.message}")
        }
    }

    fun updateFriendRequest(field: String, value: FieldValue, id: String): Boolean {
        return try {
            userFriendRequests.document(id).update(field, value)
            true
        } catch (exception: Exception) {
            Log.d("Repository", "updateRequestList -> $field ->  ${exception.message}")
            false
        }
    }


    fun updateFriend(id: String, value: FieldValue): Boolean {
        return try {
            usersFriends.document(id).update("friends", value)
            true
        } catch (exception: Exception) {
            Log.d("Repository", "addFriend ->  ${exception.message}")
            false
        }
    }

    suspend fun getUsersFromIdList(
        list: List<String>,
        onFinish: (MutableList<DocumentSnapshot>) -> Unit
    ) {

        try {
            users.whereIn(ID, list).addSnapshotListener { value, error ->
                error?.let { exception ->
                    Log.d("Repository", "getFieldRequestUsers ->  ${exception.message}")
                }
                value?.documents?.let { list ->
                    CoroutineScope(Dispatchers.IO).launch {
                        onFinish(list)
                    }
                }
            }

        } catch (exception: Exception) {
            Log.d("Repository", "getReceivedRequestUsers ->  ${exception.message}")

        }
    }

    fun saveImageToInternalStorage(fileName: String, bitmap: Bitmap, context: Context): Boolean {
        return try {
            with(context) {
                openFileOutput("$fileName.jpg", MODE_PRIVATE).use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream))
                        throw IOException("couldn't save photo")
                }
            }
            Log.d("Repository", "saveImageToInternalStorage ->  photo saved successfully")
            true
        } catch (exception: Exception) {
            Log.d("Repository", "saveImageToInternalStorage ->  ${exception.message}")
            false
        }
    }

    fun loadImageFromInternalStorage(fileName: String, context: Context): Bitmap? {
        return try {
            with(context) {
                val files = filesDir
                files?.let {
                    it.listFiles()?.let { files ->
                        files.filter { file -> file.isFile && file.canRead() && file.name.contains("$fileName.jpg") }
                            .let { images ->
                                if (images.size == 1) {
                                    val bytes = images[0].readBytes()
                                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        .also {
                                            Log.d(
                                                "Repository",
                                                "loadImageFromInternalStorage ->  image loaded successfully"
                                            )
                                        }
                                }
                            }
                    }
                }
            }
            null
        } catch (exception: Exception) {
            Log.d("Repository", "loadImageFromInternalStorage ->  ${exception.message}")
            null
        }

    }

    suspend fun sendMessage(message: Message, isTextMessage: Boolean): Boolean {
        return try {
            CoroutineScope(Dispatchers.IO).launch {
                launch {
                    Log.d("Repository", "sendMessage ->  uploading message to who sent")
                    messages.document(message.from).collection(message.to).document()
                        .set(
                            if (isTextMessage)
                                message.toTextMessageHashMap()
                            else
                                message
                        )
                }.invokeOnCompletion {
                    Log.d("Repository", "sendMessage ->  done")
                }
                launch {
                    Log.d("Repository", "sendMessage ->  uploading message to who received")
                    messages.document(message.to).collection(message.from).document()
                        .set(
                            if (isTextMessage)
                                message.toTextMessageHashMap()
                            else
                                message
                        )
                }
            }.join()
            true
        } catch (exception: Exception) {
            Log.d("Repository", "sendMessage ->  ${exception.message}")
            false
        }
    }

    suspend fun handleUserChatId(from: String, to: String) {
        if (!messageDocumentExist(from)) {
            messages.document(from)
                .set(mapOf("userChatId" to listOf(to)))
            messages.document(to)
                .set(mapOf("userChatId" to listOf(from)))
        } else
            messages.document(from)
                .update("userChatId", FieldValue.arrayUnion(to))
        messages.document(to)
            .update("userChatId", FieldValue.arrayUnion(from))
    }

    private suspend fun messageDocumentExist(otherId: String): Boolean {
        return try {
            messages.document(currentUserId!!).get()
                .await().data?.get("userChatId").run {
                    (this as List<*>).contains(otherId)
                }
        } catch (exception: Exception) {
            Log.d("Repository", "checkMessageDocumentExist ->  ${exception.message}")
            false
        }
    }

    suspend fun userFirstMessage(id: String) = !messageDocumentExist(id)
    suspend fun getMessages(id: String, onFinish: (MutableList<DocumentSnapshot>) -> Unit) {
        if (!messageDocumentExist(id) || lastDocument == messages.document(currentUserId!!)
                .collection(id)
                .orderBy("date").limit(1)
        ) {
            Log.d("Repository", "getMessages ->  list finished")
            return
        }
        lastDocument?.let {
            messages.document(currentUserId!!).collection(id)
                .orderBy("date", Query.Direction.DESCENDING).limit(20)
                .startAfter(lastDocument)
                .addSnapshotListener { value, error ->
                    error?.let { firebaseFirestoreException ->
                        Log.d(
                            "Repository",
                            "getMessages ->  ${firebaseFirestoreException.message}"
                        )
                    }
                    value?.documents?.let { list ->
                        onFinish(list)
                        Log.d("Repository", "getMessages ->  called")
                        lastDocument = list.last()
                    }
                }
        } ?: run {
            messages.document(currentUserId!!).collection(id)
                .orderBy("date", Query.Direction.DESCENDING).limit(20)
                .addSnapshotListener { value, error ->
                    error?.let { firebaseFirestoreException ->
                        Log.d(
                            "Repository",
                            "getMessages ->  ${firebaseFirestoreException.message}"
                        )
                    }
                    value?.documents?.let { list ->
                        onFinish(list)
                        Log.d("Repository", "getMessages ->  called")
                        lastDocument = list.last()
                    }
                }
        }
    }

    fun resetLastDocument() {
        lastDocument = null
    }

    fun updateCurrentUserStatues(statues: String) {
        users.document(currentUserId!!).update(
            "userStatues",
            when (statues) {
                "online" -> "online"
                else -> "last seen ${Timestamp.now().seconds}"
            }
        )
    }

    suspend fun getUserChatList(): List<String> {
        return try {
            val document = messages.document(currentUserId ?: "").get().await()
            document.data?.run {
                this["userChatId"] as List<String>
            } ?: listOf()
        } catch (exception: Exception) {
            Log.d("Repository", "getUserChatList ->  ${exception.message}")
            listOf()
        }
    }

    private suspend fun uploadPhotoMessage(uri: String, fileName: String): String {
        return storage.reference.root.child("userPhotoMessage").child(currentUserId!!)
            .child(fileName)
            .putFile(uri.toUri()).await().storage.downloadUrl.await().toString()
    }

    suspend fun uploadAndSavePhotoMessage(uri: String, context: Context): HashMap<String, String> {
        val fileName = UUID.randomUUID().toString()
        val bitmap = getBitmapFromUrl(context = context, url = uri)
        bitmap?.let {
            if (it is BitmapDrawable)
                saveImageToInternalStorage(fileName, it.bitmap, context)
        }
        val url = uploadPhotoMessage(uri = uri, fileName = fileName)
        return hashMapOf(Pair("url", url), Pair("fileName", fileName))
    }

    suspend fun updatePhotoMessage(message: PhotoMessage, context: Context) {
        val bitmap = getBitmapFromUrl(context, message.data)
        val fileName = UUID.randomUUID().toString()
        Log.d("Repository", "updatePhotoMessage ->  file name : $fileName")
        bitmap?.let {
            if (it is BitmapDrawable) {
                saveImageToInternalStorage(fileName, it.bitmap, context)
            }
            messages.document(currentUserId!!).collection(
                if (message.from == currentUserId)
                    message.to
                else
                    message.from
            )
                .whereEqualTo("data", message.data).get().await().let { query ->
                    if (query.documents.size == 1) {
                        val id = query.documents[0].id
                        messages.document(currentUserId!!).collection(
                            if (message.from == currentUserId)
                                message.to
                            else
                                message.from
                        ).document(id)
                            .update("fileName", fileName).await()
                        Log.d("Repository", "updatePhotoMessage ->  photo file name updated")
                    }
                }
        }
    }
    /*fun writeFakeData(): Boolean {
       val names = listOf(
           "mohamed",
           "ahmed",
           "ibrahim",
           "yahia",
           "islam",
           "said",
           "alaa",
           "osama",
           "hussin",
           "abdo",
           "nadeen",
           "iman",
           "israa",
           "toqa",
           "salma"
       )
       return try {
           for (i in 0..60)
               users.document().set(User(username = names.random(), id = UUID.randomUUID().toString()).toSimpleUserHashMap())
           true
       } catch (exception: Exception) {
           Log.d("Repository", "writeFakeData ->  ${exception.message}")
           false
       }
   }*/

}