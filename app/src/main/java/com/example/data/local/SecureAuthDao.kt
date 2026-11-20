package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureAuthDao {
    // Roles
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRole(role: RoleEntity): Long

    @Query("SELECT * FROM roles WHERE role_name = :roleName LIMIT 1")
    suspend fun findRoleByName(roleName: String): RoleEntity?

    @Query("SELECT * FROM roles ORDER BY id ASC")
    suspend fun getAllRoles(): List<RoleEntity>

    // Users
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: Long)

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserWithRolesById(userId: Long): UserWithRoles?

    @Transaction
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserWithRolesByEmail(email: String): UserWithRoles?

    @Transaction
    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserWithRolesByUsername(username: String): UserWithRoles?

    @Transaction
    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:identifier) OR LOWER(username) = LOWER(:identifier) LIMIT 1")
    suspend fun findUserWithRolesByIdentifier(identifier: String): UserWithRoles?

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email)")
    suspend fun countByEmail(email: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username)")
    suspend fun countByUsername(username: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(email) = LOWER(:email) AND id != :excludeUserId")
    suspend fun countByEmailExcluding(email: String, excludeUserId: Long): Int

    @Query("SELECT COUNT(*) FROM users WHERE LOWER(username) = LOWER(:username) AND id != :excludeUserId")
    suspend fun countByUsernameExcluding(username: String, excludeUserId: Long): Int

    @Transaction
    @Query("SELECT * FROM users ORDER BY created_at DESC")
    fun observeAllUsersWithRoles(): Flow<List<UserWithRoles>>

    @Transaction
    @Query("SELECT * FROM users ORDER BY created_at DESC")
    suspend fun getAllUsersWithRoles(): List<UserWithRoles>

    // User Roles
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserRole(crossRef: UserRoleCrossRef)

    @Query("DELETE FROM user_roles WHERE user_id = :userId")
    suspend fun clearUserRoles(userId: Long)

    // Password Reset Tokens
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPasswordResetToken(token: PasswordResetTokenEntity): Long

    @Query("SELECT * FROM password_reset_tokens WHERE token_hash = :tokenHash LIMIT 1")
    suspend fun findResetTokenByHash(tokenHash: String): PasswordResetTokenEntity?

    @Query("UPDATE password_reset_tokens SET used = 1 WHERE id = :tokenId")
    suspend fun markResetTokenUsed(tokenId: Long)

    @Query("UPDATE password_reset_tokens SET used = 1 WHERE user_id = :userId AND used = 0")
    suspend fun invalidateActiveResetTokensForUser(userId: Long)

    // Login History
    @Insert
    suspend fun insertLoginHistory(entry: LoginHistoryEntity): Long

    @Query("SELECT * FROM login_history WHERE user_id = :userId ORDER BY login_time DESC LIMIT 50")
    suspend fun getLoginHistoryForUser(userId: Long): List<LoginHistoryEntity>

    @Query("SELECT * FROM login_history ORDER BY login_time DESC LIMIT 100")
    suspend fun getRecentGlobalLoginHistory(): List<LoginHistoryEntity>

    // Audit Logs
    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100")
    suspend fun getAuditLogs(): List<AuditLogEntity>

    // Revoked Tokens
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevokedToken(token: RevokedTokenEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM revoked_tokens WHERE jti = :jti)")
    suspend fun isTokenRevoked(jti: String): Boolean

    @Query("DELETE FROM revoked_tokens WHERE expires_at < :nowMs")
    suspend fun pruneExpiredRevokedTokens(nowMs: Long)
}
