package android.mohamed.jepackcomposechatapp.dataModels

import android.mohamed.jepackcomposechatapp.utility.Constants.FIRST_NAME
import android.mohamed.jepackcomposechatapp.utility.Constants.GENDER
import android.mohamed.jepackcomposechatapp.utility.Constants.ID
import android.mohamed.jepackcomposechatapp.utility.Constants.IMAGE_URL
import android.mohamed.jepackcomposechatapp.utility.Constants.LAST_NAME
import android.mohamed.jepackcomposechatapp.utility.Constants.USERNAME
import android.mohamed.jepackcomposechatapp.utility.Constants.USER_STATUES
import kotlinx.coroutines.flow.MutableStateFlow

data class ProfileUser(
    var username: String? = null,
    var id: String? = null,
    var imageUrl: MutableStateFlow<String> = MutableStateFlow(""),
    var gender: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var currentStatus: String = "online"
) {
    fun toHashMap(): HashMap<String, MutableStateFlow<String>> {
        return HashMap(
            mapOf(
                FIRST_NAME to MutableStateFlow(firstName ?: ""),
                LAST_NAME to MutableStateFlow(lastName ?: ""),
                USERNAME to MutableStateFlow(username ?: ""),
                GENDER to MutableStateFlow(gender ?: ""),
                IMAGE_URL to MutableStateFlow(imageUrl.value)
            )
        )
    }

    fun toSimpleUserHashMap() = HashMap(
        mapOf(
            FIRST_NAME to firstName,
            LAST_NAME to lastName,
            GENDER to gender,
            ID to id
        )
    )

    fun toSimpleUser() = User(id, username, imageUrl, currentStatus)
}

data class User(
    var id: String? = null,
    var username: String? = null,
    var imageUrl: MutableStateFlow<String> = MutableStateFlow(""),
    var currentStatus: String? = "online"
) {
    fun toSimpleUserHashMap() = HashMap(
        mapOf(
            USERNAME to username,
            IMAGE_URL to imageUrl.value,
            ID to id,
            USER_STATUES to currentStatus
        )
    )

}