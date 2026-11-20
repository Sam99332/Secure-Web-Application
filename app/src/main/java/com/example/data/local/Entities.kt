package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

object AccountStatus {
    const val ACTIVE = "ACTIVE"
    const val INACTIVE = "INACTIVE"
}

object RoleNames {
    const val ROLE_USER = "ROLE_USER"
    const val ROLE_ADMIN = "ROLE_ADMIN"
}

object LoginStatuses {
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    const val LOCKED = "LOCKED"
    const val INACTIVE_BLOCKED = "INACTIVE_BLOCKED"
}

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true),
        Index(value = ["account_status"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "full_name")
    val fullName: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "password_hash")
    val passwordHash: String,
    @ColumnInfo(name = "account_status")
    val accountStatus: String = AccountStatus.ACTIVE,
    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)

@Entity(
    tableName = "roles",
    indices = [Index(value = ["role_name"], unique = true)]
)
data class RoleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "role_name")
    val roleName: String
)

@Entity(
    tableName = "user_roles",
    primaryKeys = ["user_id", "role_id"],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoleEntity::class,
            parentColumns = ["id"],
            childColumns = ["role_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["role_id"])]
)
data class UserRoleCrossRef(
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "role_id")
    val roleId: Long
)

@Entity(
    tableName = "password_reset_tokens",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["token_hash"], unique = true),
        Index(value = ["user_id"])
    ]
)
data class PasswordResetTokenEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "token_hash")
    val tokenHash: String,
    @ColumnInfo(name = "expiry_date")
    val expiryDate: Long,
    @ColumnInfo(name = "used")
    val used: Boolean = false
)

@Entity(
    tableName = "login_history",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"]), Index(value = ["login_time"])]
)
data class LoginHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "login_time")
    val loginTime: Long,
    @ColumnInfo(name = "ip_address")
    val ipAddress: String,
    @ColumnInfo(name = "login_status")
    val loginStatus: String
)

@Entity(
    tableName = "audit_logs",
    indices = [Index(value = ["admin_id"]), Index(value = ["target_user_id"]), Index(value = ["timestamp"])]
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "admin_id")
    val adminId: Long,
    @ColumnInfo(name = "admin_username")
    val adminUsername: String,
    @ColumnInfo(name = "action")
    val action: String,
    @ColumnInfo(name = "target_user_id")
    val targetUserId: Long?,
    @ColumnInfo(name = "target_username")
    val targetUsername: String?,
    @ColumnInfo(name = "details")
    val details: String,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)

@Entity(tableName = "revoked_tokens")
data class RevokedTokenEntity(
    @PrimaryKey
    val jti: String,
    @ColumnInfo(name = "expires_at")
    val expiresAt: Long
)

data class UserWithRoles(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = UserRoleCrossRef::class,
            parentColumn = "user_id",
            entityColumn = "role_id"
        )
    )
    val roles: List<RoleEntity>
)
