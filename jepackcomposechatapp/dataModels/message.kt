package android.mohamed.jepackcomposechatapp.dataModels

import com.google.firebase.Timestamp

sealed class Message(
    open var from: String,
    open var to: String,
    open var type: String,
    open var data: String,
    open var fileName: String? = null,
    open var date: Timestamp = Timestamp.now()
) {

    data class TextMessage(
        override var from: String,
        override var to: String,
        override var type: String,
        override var data: String,
        override var date: Timestamp = Timestamp.now()
    ) : Message(from, to, type, data, null, date)

    data class PhotoMessage(
        override var from: String,
        override var to: String,
        override var type: String,
        override var data: String,
        override var fileName: String?,
        override var date: Timestamp = Timestamp.now()
    ) : Message(from, to, type, data, fileName, date)

    fun toTextMessageHashMap() = hashMapOf<String, Any>(
        "from" to from,
        "to" to to,
        "type" to type,
        "data" to data,
        "date" to date
    )
}
