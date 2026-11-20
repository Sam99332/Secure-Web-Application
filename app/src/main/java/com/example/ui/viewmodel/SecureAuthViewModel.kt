package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AuditLogEntity
import com.example.data.local.LoginHistoryEntity
import com.example.data.local.SecureAuthDatabase
import com.example.data.local.UserWithRoles
import com.example.data.preferences.SessionPreferences
import com.example.data.repository.AdminOverview
import com.example.data.repository.AuthResult
import com.example.data.repository.SecureAuthRepository
import com.example.data.repository.UserSession
import com.example.data.repository.UserSortOrder
import com.example.security.JwtService
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppRoute(val route: String) {
    LANDING("landing"),
    LOGIN("login"),
    REGISTER("register"),
    FORGOT_PASSWORD("forgot_password"),
    USER_DASHBOARD("user_dashboard"),
    ADMIN_DASHBOARD("admin_dashboard")
}

sealed class UiToast {
    data class Success(val message: String) : UiToast()
    data class Error(val message: String) : UiToast()
}

data class SecureAuthUiState(
    val isInitializing: Boolean = true,
    val isBusy: Boolean = false,
    val session: UserSession? = null,
    val darkThemeOverride: Boolean? = null,
    val userLoginHistory: List<LoginHistoryEntity> = emptyList(),
    val adminOverview: AdminOverview? = null,
    val adminUsers: List<UserWithRoles> = emptyList(),
    val adminAuditLogs: List<AuditLogEntity> = emptyList(),
    val adminGlobalLogins: List<LoginHistoryEntity> = emptyList(),
    val adminSearchQuery: String = "",
    val adminStatusFilter: String? = null,
    val adminRoleFilter: String? = null,
    val adminSortOrder: UserSortOrder = UserSortOrder.NEWEST,
    val devResetTokenPreview: String? = null,
    val resetSubmittedNotice: String? = null
)

class SecureAuthViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = SessionPreferences(application)
    private val database = SecureAuthDatabase.getInstance(application)
    private lateinit var repository: SecureAuthRepository

    private val _uiState = MutableStateFlow(SecureAuthUiState())
    val uiState: StateFlow<SecureAuthUiState> = _uiState.asStateFlow()

    private val _toasts = MutableSharedFlow<UiToast>(extraBufferCapacity = 8)
    val toasts: SharedFlow<UiToast> = _toasts.asSharedFlow()

    private val _navEvents = MutableSharedFlow<AppRoute>(extraBufferCapacity = 4)
    val navEvents: SharedFlow<AppRoute> = _navEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            preferences.darkModeFlow.collect { dark ->
                _uiState.update { it.copy(darkThemeOverride = dark) }
            }
        }
        viewModelScope.launch {
            database.ensureSeeded()
            val secret = preferences.getOrCreateJwtSecretBytes()
            repository = SecureAuthRepository(
                dao = database.secureAuthDao(),
                jwtService = JwtService(secretKeyBytes = secret)
            )
            val savedToken = preferences.getSavedSessionToken()
            val restored = savedToken?.let { repository.restoreSession(it) }
            _uiState.update {
                it.copy(
                    isInitializing = false,
                    session = restored
                )
            }
            if (restored != null) {
                loadUserDashboardData()
                if (restored.isAdmin) {
                    loadAdminData()
                }
            }
        }
    }

    fun toggleDarkMode(currentlyDark: Boolean) {
        viewModelScope.launch {
            preferences.setDarkMode(!currentlyDark)
        }
    }

    fun register(fullName: String, email: String, username: String, password: String) {
        if (!::repository.isInitialized) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = repository.register(fullName, email, username, password)) {
                is AuthResult.Success -> {
                    preferences.saveSession(result.data.claims.rawToken, rememberMe = false)
                    _uiState.update { it.copy(isBusy = false, session = result.data) }
                    loadUserDashboardData()
                    _toasts.tryEmit(UiToast.Success("Account created. Welcome, ${result.data.user.user.fullName}!"))
                    _navEvents.tryEmit(AppRoute.USER_DASHBOARD)
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isBusy = false) }
                    _toasts.tryEmit(UiToast.Error(result.message))
                }
            }
        }
    }

    fun login(identifier: String, password: String, rememberMe: Boolean) {
        if (!::repository.isInitialized) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = repository.login(identifier, password, rememberMe)) {
                is AuthResult.Success -> {
                    val session = result.data
                    preferences.saveSession(session.claims.rawToken, rememberMe)
                    _uiState.update { it.copy(isBusy = false, session = session) }
                    loadUserDashboardData()
                    if (session.isAdmin) {
                        loadAdminData()
                        _toasts.tryEmit(UiToast.Success("Authenticated with ADMIN privileges."))
                        _navEvents.tryEmit(AppRoute.ADMIN_DASHBOARD)
                    } else {
                        _toasts.tryEmit(UiToast.Success("Signed in as ${session.user.user.username}."))
                        _navEvents.tryEmit(AppRoute.USER_DASHBOARD)
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isBusy = false) }
                    _toasts.tryEmit(UiToast.Error(result.message))
                }
            }
        }
    }

    fun logout() {
        if (!::repository.isInitialized) return
        val token = _uiState.value.session?.claims?.rawToken
        viewModelScope.launch {
            if (token != null) {
                repository.logout(token)
            }
            preferences.clearSession()
            _uiState.update {
                it.copy(
                    session = null,
                    userLoginHistory = emptyList(),
                    adminOverview = null,
                    adminUsers = emptyList(),
                    adminAuditLogs = emptyList(),
                    adminGlobalLogins = emptyList()
                )
            }
            _toasts.tryEmit(UiToast.Success("Signed out securely and JWT revoked."))
            _navEvents.tryEmit(AppRoute.LANDING)
        }
    }

    fun requestPasswordReset(email: String) {
        if (!::repository.isInitialized) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, devResetTokenPreview = null, resetSubmittedNotice = null) }
            when (val result = repository.requestPasswordReset(email)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            devResetTokenPreview = result.data,
                            resetSubmittedNotice = "If an active account matches that email, a 15-minute reset token has been issued."
                        )
                    }
                    _toasts.tryEmit(UiToast.Success("Password reset request processed."))
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isBusy = false) }
                    _toasts.tryEmit(UiToast.Error(result.message))
                }
            }
        }
    }

    fun resetPassword(token: String, newPassword: String, onSuccess: () -> Unit) {
        if (!::repository.isInitialized) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = repository.resetPassword(token, newPassword)) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(isBusy = false, devResetTokenPreview = null, resetSubmittedNotice = null)
                    }
                    _toasts.tryEmit(UiToast.Success("Password updated. You may now sign in."))
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isBusy = false) }
                    _toasts.tryEmit(UiToast.Error(result.message))
                }
            }
        }
    }

    fun clearResetNotice() {
        _uiState.update { it.copy(devResetTokenPreview = null, resetSubmittedNotice = null) }
    }

    fun loadUserDashboardData() {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            when (val history = repository.getOwnLoginHistory(token)) {
                is AuthResult.Success -> _uiState.update { it.copy(userLoginHistory = history.data) }
                is AuthResult.Error -> handleSessionOrError(history.message)
            }
        }
    }

    fun updateProfile(fullName: String, email: String, avatarUri: String?) {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = repository.updateOwnProfile(token, fullName, email, avatarUri)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, session = result.data) }
                    if (result.data.isAdmin) loadAdminData()
                    _toasts.tryEmit(UiToast.Success("Profile updated successfully."))
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isBusy = false) }
                    handleSessionOrError(result.message)
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, onSuccess: () -> Unit) {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = repository.changeOwnPassword(token, currentPassword, newPassword)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isBusy = false) }
                    _toasts.tryEmit(UiToast.Success("Password changed securely."))
                    onSuccess()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isBusy = false) }
                    handleSessionOrError(result.message)
                }
            }
        }
    }

    // ==================== ADMIN ACTIONS ====================

    fun loadAdminData() {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            val state = _uiState.value
            when (val overview = repository.getAdminOverview(token)) {
                is AuthResult.Success -> _uiState.update { it.copy(adminOverview = overview.data) }
                is AuthResult.Error -> {
                    handleSessionOrError(overview.message)
                    return@launch
                }
            }
            when (
                val users = repository.getAdminUsers(
                    token,
                    state.adminSearchQuery,
                    state.adminStatusFilter,
                    state.adminRoleFilter,
                    state.adminSortOrder
                )
            ) {
                is AuthResult.Success -> _uiState.update { it.copy(adminUsers = users.data) }
                is AuthResult.Error -> handleSessionOrError(users.message)
            }
            when (val audit = repository.getAuditAndActivity(token)) {
                is AuthResult.Success -> _uiState.update {
                    it.copy(adminAuditLogs = audit.data.first, adminGlobalLogins = audit.data.second)
                }
                is AuthResult.Error -> handleSessionOrError(audit.message)
            }
        }
    }

    fun updateAdminFilters(
        query: String = _uiState.value.adminSearchQuery,
        status: String? = _uiState.value.adminStatusFilter,
        role: String? = _uiState.value.adminRoleFilter,
        sort: UserSortOrder = _uiState.value.adminSortOrder
    ) {
        _uiState.update {
            it.copy(
                adminSearchQuery = query,
                adminStatusFilter = status,
                adminRoleFilter = role,
                adminSortOrder = sort
            )
        }
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            when (val users = repository.getAdminUsers(token, query, status, role, sort)) {
                is AuthResult.Success -> _uiState.update { it.copy(adminUsers = users.data) }
                is AuthResult.Error -> handleSessionOrError(users.message)
            }
        }
    }

    fun adminEditUser(targetUserId: Long, fullName: String, email: String, username: String, onDone: () -> Unit) {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            when (val result = repository.updateUserByAdmin(token, targetUserId, fullName, email, username)) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isBusy = false) }
                    _toasts.tryEmit(UiToast.Success("Updated user ${result.data.user.username}."))
                    loadAdminData()
                    onDone()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(isBusy = false) }
                    handleSessionOrError(result.message)
                }
            }
        }
    }

    fun adminToggleUserStatus(targetUserId: Long, newStatus: String) {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            when (val result = repository.setUserStatus(token, targetUserId, newStatus)) {
                is AuthResult.Success -> {
                    _toasts.tryEmit(UiToast.Success("Account status set to $newStatus."))
                    loadAdminData()
                }
                is AuthResult.Error -> handleSessionOrError(result.message)
            }
        }
    }

    fun adminUpdateRoles(targetUserId: Long, roles: Set<String>, onDone: () -> Unit) {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            when (val result = repository.updateUserRoles(token, targetUserId, roles)) {
                is AuthResult.Success -> {
                    _toasts.tryEmit(UiToast.Success("User roles updated."))
                    loadAdminData()
                    onDone()
                }
                is AuthResult.Error -> handleSessionOrError(result.message)
            }
        }
    }

    fun adminDeleteUser(targetUserId: Long) {
        val token = _uiState.value.session?.claims?.rawToken ?: return
        viewModelScope.launch {
            when (val result = repository.deleteUser(token, targetUserId)) {
                is AuthResult.Success -> {
                    _toasts.tryEmit(UiToast.Success("User deleted and audit log recorded."))
                    loadAdminData()
                }
                is AuthResult.Error -> handleSessionOrError(result.message)
            }
        }
    }

    private fun handleSessionOrError(message: String) {
        _toasts.tryEmit(UiToast.Error(message))
        if (message.contains("Session expired", ignoreCase = true)) {
            logout()
        }
    }
}
