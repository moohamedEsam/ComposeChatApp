package android.mohamed.jepackcomposechatapp.koinModules

import android.mohamed.jepackcomposechatapp.repository.Repository
import android.mohamed.jepackcomposechatapp.viewModels.MainViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val module = module {
    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }
    single { FirebaseStorage.getInstance() }
    single { Repository(get(), get(), get()) }
    viewModel { MainViewModel(get()) }
}

