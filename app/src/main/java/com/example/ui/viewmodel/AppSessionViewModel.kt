package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthRepository
import com.example.data.firebase.toReadableFirestoreMessage
import com.example.data.local.OfflineCachePreferences
import com.example.data.model.SystemSetting
import com.example.data.model.UiState
import com.example.data.model.UserAccount
import com.example.data.model.UserRole
import com.example.data.repository.AdminAndUserRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppSessionViewModel(
    private val context: Context,
    private val firebaseUser: FirebaseUser
) : ViewModel() {

    private val userRepo = AdminAndUserRepository(context)
    private val offlinePrefs = OfflineCachePreferences(context)

    val userState: StateFlow<UiState<UserAccount?>> = userRepo.observeUserAccount(firebaseUser.uid)
        .map<UserAccount?, UiState<UserAccount?>> { UiState.Success(it) }
        .catch { e ->
            if (e is CancellationException) throw e
            val readable = (e as? FirebaseFirestoreException)?.toReadableFirestoreMessage()
                ?: e.toReadableFirestoreMessage()
            emit(UiState.Error(readable))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val systemSetting: StateFlow<SystemSetting> = userRepo.observeSystemSetting()
        .catch { e ->
            if (e is CancellationException) throw e
            emit(SystemSetting())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SystemSetting())

    val downloadedModules: StateFlow<Set<String>> = offlinePrefs.downloadedModuleIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    val isBootstrapAdminEmail: Boolean =
        firebaseUser.email.orEmpty().equals(AuthRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) &&
            firebaseUser.isEmailVerified

    init {
        if (isBootstrapAdminEmail) {
            viewModelScope.launch {
                userState.collect { state ->
                    if (state is UiState.Success && state.data == null && !_busy.value) {
                        provisionProfile(
                            nama = firebaseUser.displayName ?: "Admin Muria",
                            role = UserRole.ADMIN
                        )
                    }
                }
            }
        }
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun provisionProfile(nama: String, role: UserRole) {
        viewModelScope.launch {
            _busy.value = true
            userRepo.provisionUserProfile(firebaseUser, nama, role)
                .onSuccess { account ->
                    _feedbackMessage.value = "Selamat datang, ${account.nama} (${account.roleEnum().label})!"
                }
                .onFailure { err ->
                    _feedbackMessage.value = err.toReadableFirestoreMessage()
                }
            _busy.value = false
        }
    }

    fun updateDisplayName(nama: String) {
        viewModelScope.launch {
            _busy.value = true
            userRepo.updateOwnDisplayName(firebaseUser.uid, nama)
                .onSuccess { _feedbackMessage.value = "Profil berhasil diperbarui." }
                .onFailure { _feedbackMessage.value = it.toReadableFirestoreMessage() }
            _busy.value = false
        }
    }

    fun toggleOfflineModule(moduleId: String, currentlyDownloaded: Boolean) {
        viewModelScope.launch {
            if (currentlyDownloaded) {
                offlinePrefs.removeDownloadedModule(moduleId)
                _feedbackMessage.value = "Cache offline modul dihapus."
            } else {
                offlinePrefs.markModuleDownloaded(moduleId)
                _feedbackMessage.value = "Modul disimpan untuk akses offline!"
            }
        }
    }

    companion object {
        fun factory(context: Context, firebaseUser: FirebaseUser): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AppSessionViewModel(context.applicationContext, firebaseUser) as T
                }
            }
    }
}
