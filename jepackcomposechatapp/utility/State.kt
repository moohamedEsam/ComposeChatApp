package android.mohamed.jepackcomposechatapp.utility

sealed class State(var message: String? = null) {
    class Error(message: String) : State(message)
    class Initialized(message: String = "") : State(message = message)
    class Success : State()
    class Loading() : State()

    sealed class UserState(message: String? = null) :
        State(message) {
        class LoggedIn : UserState()
        class LoggedOut : UserState()
    }
}