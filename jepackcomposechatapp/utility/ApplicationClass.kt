package android.mohamed.jepackcomposechatapp.utility

import android.app.Application
import android.mohamed.jepackcomposechatapp.koinModules.module
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ApplicationClass: Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ApplicationClass)
            modules(listOf(module))
        }
    }
}