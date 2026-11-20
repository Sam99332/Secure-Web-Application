package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.PasswordSecurity

@Database(
    entities = [
        UserEntity::class,
        RoleEntity::class,
        UserRoleCrossRef::class,
        PasswordResetTokenEntity::class,
        LoginHistoryEntity::class,
        AuditLogEntity::class,
        RevokedTokenEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SecureAuthDatabase : RoomDatabase() {
    abstract fun secureAuthDao(): SecureAuthDao

    suspend fun ensureSeeded() {
        val dao = secureAuthDao()
        val userRole = dao.findRoleByName(RoleNames.ROLE_USER)
            ?: RoleEntity(roleName = RoleNames.ROLE_USER).let {
                val id = dao.insertRole(it)
                it.copy(id = id)
            }
        val adminRole = dao.findRoleByName(RoleNames.ROLE_ADMIN)
            ?: RoleEntity(roleName = RoleNames.ROLE_ADMIN).let {
                val id = dao.insertRole(it)
                it.copy(id = id)
            }

        val existingUsers = dao.getAllUsersWithRoles()
        if (existingUsers.isNotEmpty()) return

        val now = System.currentTimeMillis()
        // Seed local development showcase accounts
        val adminId = dao.insertUser(
            UserEntity(
                fullName = "Elena Vance (Admin)",
                username = "admin",
                email = "admin@secureauth.dev",
                passwordHash = PasswordSecurity.hashPassword("Admin@Secure123!"),
                accountStatus = AccountStatus.ACTIVE,
                createdAt = now - 14L * 86_400_000L,
                updatedAt = now - 86_400_000L
            )
        )
        dao.insertUserRole(UserRoleCrossRef(adminId, userRole.id))
        dao.insertUserRole(UserRoleCrossRef(adminId, adminRole.id))

        val alexId = dao.insertUser(
            UserEntity(
                fullName = "Alex Morgan",
                username = "alex.morgan",
                email = "alex@secureauth.dev",
                passwordHash = PasswordSecurity.hashPassword("User@Secure123!"),
                accountStatus = AccountStatus.ACTIVE,
                createdAt = now - 7L * 86_400_000L,
                updatedAt = now - 2L * 86_400_000L
            )
        )
        dao.insertUserRole(UserRoleCrossRef(alexId, userRole.id))

        val priyaId = dao.insertUser(
            UserEntity(
                fullName = "Priya Nair",
                username = "priya.nair",
                email = "priya@secureauth.dev",
                passwordHash = PasswordSecurity.hashPassword("User@Secure123!"),
                accountStatus = AccountStatus.INACTIVE,
                createdAt = now - 3L * 86_400_000L,
                updatedAt = now - 86_400_000L
            )
        )
        dao.insertUserRole(UserRoleCrossRef(priyaId, userRole.id))

        dao.insertLoginHistory(
            LoginHistoryEntity(
                userId = adminId,
                loginTime = now - 3_600_000L,
                ipAddress = "10.0.2.2 (Android TLS)",
                loginStatus = LoginStatuses.SUCCESS
            )
        )
        dao.insertLoginHistory(
            LoginHistoryEntity(
                userId = alexId,
                loginTime = now - 7_200_000L,
                ipAddress = "192.168.1.42",
                loginStatus = LoginStatuses.SUCCESS
            )
        )
        dao.insertAuditLog(
            AuditLogEntity(
                adminId = adminId,
                adminUsername = "admin",
                action = "STATUS_CHANGED",
                targetUserId = priyaId,
                targetUsername = "priya.nair",
                details = "Account deactivated during quarterly access review",
                timestamp = now - 86_400_000L
            )
        )
    }

    companion object {
        @Volatile
        private var instance: SecureAuthDatabase? = null

        fun getInstance(context: Context): SecureAuthDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    SecureAuthDatabase::class.java,
                    "secureauth.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build().also { instance = it }
            }
        }
    }
}
