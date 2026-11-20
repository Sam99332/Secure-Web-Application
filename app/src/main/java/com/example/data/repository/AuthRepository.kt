package com.example.data.repository

import com.example.data.local.AccountStatus
import com.example.data.local.AuditLogEntity
import com.example.data.local.LoginHistoryEntity
import com.example.data.local.LoginStatuses
import com.example.data.local.PasswordResetTokenEntity
import com.example.data.local.RevokedTokenEntity
import com.example.data.local.RoleNames
import com.example.data.local.SecureAuthDao
import com.example.data.local.UserEntity
import com.example.data.local.UserRoleCrossRef
import com.example.data.local.UserWithRoles
import com.example.security.JwtClaims
import com.example.security.JwtService
import com.example.security.LoginRateLimiter
import com.example.security.PasswordSecurity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val message: String, val retryAfterSeconds: Long = 0L) : AuthResult<Nothing>()
}

data class UserSession(
    val user: UserWithRoles,
    val claims: JwtClaims
) {
    val isAdmin: Boolean
        get() = claims.roles.contains(RoleNames.ROLE_ADMIN) &&
            user.roles.any { it.roleName == RoleNames.ROLE_ADMIN }
}

data class AdminOverview(
    val totalUsers: Int,
    val activeUsers: Int,
    val inactiveUsers: Int,
    val adminCount: Int,
    val recentRegistrations: List<UserWithRoles>
)

enum class UserSortOrder(val label: String) {
    NEWEST("Newest"),
    NAME_ASC("Name A-Z"),
    USERNAME_ASC("Username A-Z")
}

class SecureAuthRepository(
    private val dao: SecureAuthDao,
    private val jwtService: JwtService,
    private val rateLimiter: LoginRateLimiter = LoginRateLimiter()
) {
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val usernameRegex = Regex("^[a-zA-Z0-9._-]{3,32}$")

    suspend fun register(
        fullName: String,
        email: String,
        username: String,
        password: String
    ): AuthResult<UserSession> = withContext(Dispatchers.IO) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim().lowercase()

        if (cleanName.length < 2) {
            return@withContext AuthResult.Error("Full name must be at least 2 characters.")
        }
        if (!emailRegex.matches(cleanEmail)) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (!usernameRegex.matches(cleanUsername)) {
            return@withContext AuthResult.Error("Username must be 3-32 chars (letters, digits, ., _, -).")
        }
        if (!PasswordSecurity.evaluateStrength(password).isAcceptable) {
            return@withContext AuthResult.Error("Password must be 8+ chars with upper, lower, digit, and symbol.")
        }
        if (dao.countByEmail(cleanEmail) > 0) {
            return@withContext AuthResult.Error("An account with this email already exists.")
        }
        if (dao.countByUsername(cleanUsername) > 0) {
            return@withContext AuthResult.Error("This username is already taken.")
        }

        val roleUser = dao.findRoleByName(RoleNames.ROLE_USER)
            ?: return@withContext AuthResult.Error("System roles are not initialized.")

        val now = System.currentTimeMillis()
        val userId = dao.insertUser(
            UserEntity(
                fullName = cleanName,
                username = cleanUsername,
                email = cleanEmail,
                passwordHash = PasswordSecurity.hashPassword(password),
                accountStatus = AccountStatus.ACTIVE,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.insertUserRole(UserRoleCrossRef(userId = userId, roleId = roleUser.id))
        dao.insertLoginHistory(
            LoginHistoryEntity(
                userId = userId,
                loginTime = now,
                ipAddress = "127.0.0.1 (Local App)",
                loginStatus = LoginStatuses.SUCCESS
            )
        )

        val created = dao.getUserWithRolesById(userId)
            ?: return@withContext AuthResult.Error("Registration failed unexpectedly.")
        val roleNames = created.roles.map { it.roleName }
        val claims = jwtService.generateToken(created.user.id, created.user.username, roleNames, rememberMe = false, nowMs = now)
        AuthResult.Success(UserSession(created, claims))
    }

    suspend fun login(
        identifier: String,
        password: String,
        rememberMe: Boolean
    ): AuthResult<UserSession> = withContext(Dispatchers.IO) {
        val cleanId = identifier.trim()
        if (cleanId.isBlank() || password.isEmpty()) {
            return@withContext AuthResult.Error("Email/username and password are required.")
        }

        val rateStatus = rateLimiter.checkAllowed(cleanId)
        if (!rateStatus.allowed) {
            return@withContext AuthResult.Error(
                message = "Too many failed login attempts. Try again in ${rateStatus.retryAfterSeconds}s.",
                retryAfterSeconds = rateStatus.retryAfterSeconds
            )
        }

        val userWithRoles = dao.findUserWithRolesByIdentifier(cleanId)
        val now = System.currentTimeMillis()
        if (userWithRoles == null || !PasswordSecurity.verifyPassword(password, userWithRoles.user.passwordHash)) {
            if (userWithRoles != null) {
                dao.insertLoginHistory(
                    LoginHistoryEntity(
                        userId = userWithRoles.user.id,
                        loginTime = now,
                        ipAddress = "127.0.0.1 (Local App)",
                        loginStatus = LoginStatuses.FAILED
                    )
                )
            }
            val afterFail = rateLimiter.recordFailure(cleanId)
            val msg = if (!afterFail.allowed) {
                "Account temporarily locked due to repeated failed attempts. Retry in ${afterFail.retryAfterSeconds}s."
            } else {
                "Invalid credentials or account unavailable."
            }
            return@withContext AuthResult.Error(msg, afterFail.retryAfterSeconds)
        }

        if (userWithRoles.user.accountStatus != AccountStatus.ACTIVE) {
            dao.insertLoginHistory(
                LoginHistoryEntity(
                    userId = userWithRoles.user.id,
                    loginTime = now,
                    ipAddress = "127.0.0.1 (Local App)",
                    loginStatus = LoginStatuses.INACTIVE_BLOCKED
                )
            )
            // Anti-enumeration: keep failure message consistent with invalid/unavailable account
            return@withContext AuthResult.Error("Invalid credentials or account unavailable.")
        }

        rateLimiter.reset(cleanId)
        dao.insertLoginHistory(
            LoginHistoryEntity(
                userId = userWithRoles.user.id,
                loginTime = now,
                ipAddress = "127.0.0.1 (Local App)",
                loginStatus = LoginStatuses.SUCCESS
            )
        )
        val claims = jwtService.generateToken(
            userId = userWithRoles.user.id,
            username = userWithRoles.user.username,
            roles = userWithRoles.roles.map { it.roleName },
            rememberMe = rememberMe,
            nowMs = now
        )
        AuthResult.Success(UserSession(userWithRoles, claims))
    }

    suspend fun restoreSession(rawToken: String): UserSession? = withContext(Dispatchers.IO) {
        val claims = validateActiveToken(rawToken) ?: return@withContext null
        val user = dao.getUserWithRolesById(claims.userId) ?: return@withContext null
        if (user.user.accountStatus != AccountStatus.ACTIVE) return@withContext null
        UserSession(user, claims)
    }

    suspend fun logout(rawToken: String) = withContext(Dispatchers.IO) {
        val claims = jwtService.parseAndVerify(rawToken) ?: return@withContext
        dao.insertRevokedToken(RevokedTokenEntity(claims.jti, claims.expiresAtMs))
        dao.pruneExpiredRevokedTokens(System.currentTimeMillis())
    }

    suspend fun requestPasswordReset(email: String): AuthResult<String?> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (!emailRegex.matches(cleanEmail)) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        val userWithRoles = dao.getUserWithRolesByEmail(cleanEmail)
        if (userWithRoles == null || userWithRoles.user.accountStatus != AccountStatus.ACTIVE) {
            // Anti-enumeration: return Success(null) without revealing whether email exists
            return@withContext AuthResult.Success(null)
        }
        dao.invalidateActiveResetTokensForUser(userWithRoles.user.id)
        val rawToken = PasswordSecurity.generateSecureToken(18)
        val tokenHash = PasswordSecurity.sha256Hex(rawToken)
        val expiry = System.currentTimeMillis() + 15L * 60_000L
        dao.insertPasswordResetToken(
            PasswordResetTokenEntity(
                userId = userWithRoles.user.id,
                tokenHash = tokenHash,
                expiryDate = expiry,
                used = false
            )
        )
        AuthResult.Success(rawToken)
    }

    suspend fun resetPassword(rawToken: String, newPassword: String): AuthResult<Unit> = withContext(Dispatchers.IO) {
        if (rawToken.isBlank()) {
            return@withContext AuthResult.Error("Reset token is required.")
        }
        if (!PasswordSecurity.evaluateStrength(newPassword).isAcceptable) {
            return@withContext AuthResult.Error("Password must be 8+ chars with upper, lower, digit, and symbol.")
        }
        val tokenEntity = dao.findResetTokenByHash(PasswordSecurity.sha256Hex(rawToken.trim()))
            ?: return@withContext AuthResult.Error("Invalid or expired password reset token.")
        if (tokenEntity.used || System.currentTimeMillis() > tokenEntity.expiryDate) {
            return@withContext AuthResult.Error("Invalid or expired password reset token.")
        }
        val user = dao.getUserWithRolesById(tokenEntity.userId)?.user
            ?: return@withContext AuthResult.Error("Account not found.")
        dao.updateUser(
            user.copy(
                passwordHash = PasswordSecurity.hashPassword(newPassword),
                updatedAt = System.currentTimeMillis()
            )
        )
        dao.markResetTokenUsed(tokenEntity.id)
        AuthResult.Success(Unit)
    }

    suspend fun updateOwnProfile(
        rawToken: String,
        fullName: String,
        email: String,
        avatarUri: String?
    ): AuthResult<UserSession> = withContext(Dispatchers.IO) {
        val current = authenticate(rawToken) ?: return@withContext AuthResult.Error("Session expired. Please sign in again.")
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        if (cleanName.length < 2) {
            return@withContext AuthResult.Error("Full name must be at least 2 characters.")
        }
        if (!emailRegex.matches(cleanEmail)) {
            return@withContext AuthResult.Error("Please enter a valid email address.")
        }
        if (dao.countByEmailExcluding(cleanEmail, current.user.user.id) > 0) {
            return@withContext AuthResult.Error("Email is already associated with another account.")
        }
        val updated = current.user.user.copy(
            fullName = cleanName,
            email = cleanEmail,
            avatarUri = avatarUri,
            updatedAt = System.currentTimeMillis()
        )
        dao.updateUser(updated)
        val refreshed = dao.getUserWithRolesById(updated.id)
            ?: return@withContext AuthResult.Error("Profile refresh failed.")
        AuthResult.Success(UserSession(refreshed, current.claims))
    }

    suspend fun changeOwnPassword(
        rawToken: String,
        currentPassword: String,
        newPassword: String
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {
        val current = authenticate(rawToken) ?: return@withContext AuthResult.Error("Session expired. Please sign in again.")
        if (!PasswordSecurity.verifyPassword(currentPassword, current.user.user.passwordHash)) {
            return@withContext AuthResult.Error("Current password is incorrect.")
        }
        if (!PasswordSecurity.evaluateStrength(newPassword).isAcceptable) {
            return@withContext AuthResult.Error("New password must be 8+ chars with upper, lower, digit, and symbol.")
        }
        dao.updateUser(
            current.user.user.copy(
                passwordHash = PasswordSecurity.hashPassword(newPassword),
                updatedAt = System.currentTimeMillis()
            )
        )
        AuthResult.Success(Unit)
    }

    suspend fun getOwnLoginHistory(rawToken: String): AuthResult<List<LoginHistoryEntity>> = withContext(Dispatchers.IO) {
        val current = authenticate(rawToken) ?: return@withContext AuthResult.Error("Session expired. Please sign in again.")
        AuthResult.Success(dao.getLoginHistoryForUser(current.user.user.id))
    }

    // ==================== ADMIN OPERATIONS (STRICT BACKEND RBAC) ====================

    suspend fun getAdminOverview(rawToken: String): AuthResult<AdminOverview> = withContext(Dispatchers.IO) {
        authenticateAdmin(rawToken) ?: return@withContext AuthResult.Error("403 Forbidden: Administrator privileges required.")
        val allUsers = dao.getAllUsersWithRoles()
        val active = allUsers.count { it.user.accountStatus == AccountStatus.ACTIVE }
        val inactive = allUsers.size - active
        val admins = allUsers.count { u -> u.roles.any { it.roleName == RoleNames.ROLE_ADMIN } }
        AuthResult.Success(
            AdminOverview(
                totalUsers = allUsers.size,
                activeUsers = active,
                inactiveUsers = inactive,
                adminCount = admins,
                recentRegistrations = allUsers.sortedByDescending { it.user.createdAt }.take(5)
            )
        )
    }

    suspend fun getAdminUsers(
        rawToken: String,
        searchQuery: String,
        statusFilter: String?,
        roleFilter: String?,
        sortOrder: UserSortOrder
    ): AuthResult<List<UserWithRoles>> = withContext(Dispatchers.IO) {
        authenticateAdmin(rawToken) ?: return@withContext AuthResult.Error("403 Forbidden: Administrator privileges required.")
        val query = searchQuery.trim().lowercase()
        val filtered = dao.getAllUsersWithRoles().filter { uwr ->
            val matchesQuery = query.isEmpty() ||
                uwr.user.fullName.lowercase().contains(query) ||
                uwr.user.username.lowercase().contains(query) ||
                uwr.user.email.lowercase().contains(query)
            val matchesStatus = statusFilter == null || uwr.user.accountStatus == statusFilter
            val matchesRole = roleFilter == null || uwr.roles.any { it.roleName == roleFilter }
            matchesQuery && matchesStatus && matchesRole
        }
        val sorted = when (sortOrder) {
            UserSortOrder.NEWEST -> filtered.sortedByDescending { it.user.createdAt }
            UserSortOrder.NAME_ASC -> filtered.sortedBy { it.user.fullName.lowercase() }
            UserSortOrder.USERNAME_ASC -> filtered.sortedBy { it.user.username.lowercase() }
        }
        AuthResult.Success(sorted)
    }

    suspend fun updateUserByAdmin(
        rawToken: String,
        targetUserId: Long,
        fullName: String,
        email: String,
        username: String
    ): AuthResult<UserWithRoles> = withContext(Dispatchers.IO) {
        val admin = authenticateAdmin(rawToken) ?: return@withContext AuthResult.Error("403 Forbidden: Administrator privileges required.")
        val target = dao.getUserWithRolesById(targetUserId) ?: return@withContext AuthResult.Error("Target user not found.")
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanUsername = username.trim().lowercase()

        if (cleanName.length < 2 || !emailRegex.matches(cleanEmail) || !usernameRegex.matches(cleanUsername)) {
            return@withContext AuthResult.Error("Invalid name, email, or username format.")
        }
        if (dao.countByEmailExcluding(cleanEmail, targetUserId) > 0) {
            return@withContext AuthResult.Error("Email is already used by another user.")
        }
        if (dao.countByUsernameExcluding(cleanUsername, targetUserId) > 0) {
            return@withContext AuthResult.Error("Username is already used by another user.")
        }

        val now = System.currentTimeMillis()
        dao.updateUser(
            target.user.copy(
                fullName = cleanName,
                email = cleanEmail,
                username = cleanUsername,
                updatedAt = now
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = admin.user.user.id,
                adminUsername = admin.user.user.username,
                action = "USER_UPDATED",
                targetUserId = targetUserId,
                targetUsername = cleanUsername,
                details = "Updated profile metadata for $cleanUsername",
                timestamp = now
            )
        )
        AuthResult.Success(dao.getUserWithRolesById(targetUserId)!!)
    }

    suspend fun setUserStatus(
        rawToken: String,
        targetUserId: Long,
        newStatus: String
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {
        val admin = authenticateAdmin(rawToken) ?: return@withContext AuthResult.Error("403 Forbidden: Administrator privileges required.")
        if (admin.user.user.id == targetUserId && newStatus != AccountStatus.ACTIVE) {
            return@withContext AuthResult.Error("Administrators cannot deactivate their own active account.")
        }
        val target = dao.getUserWithRolesById(targetUserId) ?: return@withContext AuthResult.Error("User not found.")
        val now = System.currentTimeMillis()
        dao.updateUser(target.user.copy(accountStatus = newStatus, updatedAt = now))
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = admin.user.user.id,
                adminUsername = admin.user.user.username,
                action = "STATUS_CHANGED",
                targetUserId = targetUserId,
                targetUsername = target.user.username,
                details = "Account status changed from ${target.user.accountStatus} to $newStatus",
                timestamp = now
            )
        )
        AuthResult.Success(Unit)
    }

    suspend fun updateUserRoles(
        rawToken: String,
        targetUserId: Long,
        roleNames: Set<String>
    ): AuthResult<Unit> = withContext(Dispatchers.IO) {
        val admin = authenticateAdmin(rawToken) ?: return@withContext AuthResult.Error("403 Forbidden: Administrator privileges required.")
        if (roleNames.isEmpty()) {
            return@withContext AuthResult.Error("A user must have at least one assigned role.")
        }
        if (admin.user.user.id == targetUserId && !roleNames.contains(RoleNames.ROLE_ADMIN)) {
            return@withContext AuthResult.Error("You cannot revoke your own ADMIN role.")
        }
        val target = dao.getUserWithRolesById(targetUserId) ?: return@withContext AuthResult.Error("User not found.")
        val allRoles = dao.getAllRoles().associateBy { it.roleName }
        val resolvedIds = roleNames.mapNotNull { allRoles[it]?.id }
        if (resolvedIds.size != roleNames.size) {
            return@withContext AuthResult.Error("Invalid role selection.")
        }
        dao.clearUserRoles(targetUserId)
        resolvedIds.forEach { roleId ->
            dao.insertUserRole(UserRoleCrossRef(userId = targetUserId, roleId = roleId))
        }
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = admin.user.user.id,
                adminUsername = admin.user.user.username,
                action = "ROLES_UPDATED",
                targetUserId = targetUserId,
                targetUsername = target.user.username,
                details = "Assigned roles: ${roleNames.sorted().joinToString(", ")}",
                timestamp = System.currentTimeMillis()
            )
        )
        AuthResult.Success(Unit)
    }

    suspend fun deleteUser(rawToken: String, targetUserId: Long): AuthResult<Unit> = withContext(Dispatchers.IO) {
        val admin = authenticateAdmin(rawToken) ?: return@withContext AuthResult.Error("403 Forbidden: Administrator privileges required.")
        if (admin.user.user.id == targetUserId) {
            return@withContext AuthResult.Error("Administrators cannot delete their own account.")
        }
        val target = dao.getUserWithRolesById(targetUserId) ?: return@withContext AuthResult.Error("User not found.")
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = admin.user.user.id,
                adminUsername = admin.user.user.username,
                action = "USER_DELETED",
                targetUserId = targetUserId,
                targetUsername = target.user.username,
                details = "Permanently removed user ${target.user.username} (${target.user.email})",
                timestamp = System.currentTimeMillis()
            )
        )
        dao.deleteUserById(targetUserId)
        AuthResult.Success(Unit)
    }

    suspend fun getAuditAndActivity(rawToken: String): AuthResult<Pair<List<AuditLogEntity>, List<LoginHistoryEntity>>> =
        withContext(Dispatchers.IO) {
            authenticateAdmin(rawToken) ?: return@withContext AuthResult.Error("403 Forbidden: Administrator privileges required.")
            AuthResult.Success(dao.getAuditLogs() to dao.getRecentGlobalLoginHistory())
        }

    private suspend fun validateActiveToken(rawToken: String): JwtClaims? {
        val claims = jwtService.parseAndVerify(rawToken) ?: return null
        if (dao.isTokenRevoked(claims.jti)) return null
        return claims
    }

    private suspend fun authenticate(rawToken: String): UserSession? {
        val claims = validateActiveToken(rawToken) ?: return null
        val user = dao.getUserWithRolesById(claims.userId) ?: return null
        if (user.user.accountStatus != AccountStatus.ACTIVE) return null
        return UserSession(user, claims)
    }

    private suspend fun authenticateAdmin(rawToken: String): UserSession? {
        val session = authenticate(rawToken) ?: return null
        val hasAdminClaim = session.claims.roles.contains(RoleNames.ROLE_ADMIN)
        val hasAdminInDb = session.user.roles.any { it.roleName == RoleNames.ROLE_ADMIN }
        return if (hasAdminClaim && hasAdminInDb) session else null
    }
}
