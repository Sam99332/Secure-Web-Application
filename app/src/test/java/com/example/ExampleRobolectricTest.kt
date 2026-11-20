package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AccountStatus
import com.example.data.local.RoleEntity
import com.example.data.local.RoleNames
import com.example.data.local.SecureAuthDatabase
import com.example.data.repository.AuthResult
import com.example.data.repository.SecureAuthRepository
import com.example.data.repository.UserSortOrder
import com.example.security.JwtService
import com.example.security.LoginRateLimiter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: SecureAuthDatabase
    private lateinit var repository: SecureAuthRepository

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SecureAuthDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        db.ensureSeeded()
        repository = SecureAuthRepository(
            dao = db.secureAuthDao(),
            jwtService = JwtService(secretKeyBytes = ByteArray(32) { 42 }),
            rateLimiter = LoginRateLimiter(maxAttempts = 3, lockoutDurationMs = 60_000L)
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches SecureAuth`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("SecureAuth", context.getString(R.string.app_name))
    }

    @Test
    fun `registration prevents duplicate email and hashes password`() = runTest {
        val first = repository.register("Jordan Lee", "jordan@example.com", "jordan.lee", "Strong@Pass1!")
        assertTrue(first is AuthResult.Success)
        val session = (first as AuthResult.Success).data
        assertTrue(session.user.user.passwordHash.startsWith("\$pbkdf2\$"))
        assertFalse(session.isAdmin)

        val duplicate = repository.register("Jordan Duplicate", "jordan@example.com", "jordan2", "Strong@Pass1!")
        assertTrue(duplicate is AuthResult.Error)
    }

    @Test
    fun `rbac blocks standard user from admin operations`() = runTest {
        val login = repository.login("alex@secureauth.dev", "User@Secure123!", rememberMe = false)
        assertTrue(login is AuthResult.Success)
        val userToken = (login as AuthResult.Success).data.claims.rawToken

        val forbiddenOverview = repository.getAdminOverview(userToken)
        assertTrue(forbiddenOverview is AuthResult.Error)
        assertTrue((forbiddenOverview as AuthResult.Error).message.contains("403"))
    }

    @Test
    fun `admin can manage users and audit logs are recorded`() = runTest {
        val adminLogin = repository.login("admin@secureauth.dev", "Admin@Secure123!", rememberMe = false)
        assertTrue(adminLogin is AuthResult.Success)
        val adminToken = (adminLogin as AuthResult.Success).data.claims.rawToken

        val usersRes = repository.getAdminUsers(adminToken, "alex", null, null, UserSortOrder.NEWEST)
        assertTrue(usersRes is AuthResult.Success)
        val alex = (usersRes as AuthResult.Success).data.first()

        val statusRes = repository.setUserStatus(adminToken, alex.user.id, AccountStatus.INACTIVE)
        assertTrue(statusRes is AuthResult.Success)

        val auditRes = repository.getAuditAndActivity(adminToken)
        assertTrue(auditRes is AuthResult.Success)
        assertTrue((auditRes as AuthResult.Success).data.first.any { it.action == "STATUS_CHANGED" })
    }

    @Test
    fun `password reset token is single use and anti-enumeration returns success on unknown email`() = runTest {
        val unknown = repository.requestPasswordReset("nobody@example.com")
        assertTrue(unknown is AuthResult.Success)
        assertNull((unknown as AuthResult.Success).data)

        val known = repository.requestPasswordReset("alex@secureauth.dev")
        assertTrue(known is AuthResult.Success)
        val rawToken = (known as AuthResult.Success).data
        assertNotNull(rawToken)

        val resetOk = repository.resetPassword(rawToken!!, "NewStrong@99!")
        assertTrue(resetOk is AuthResult.Success)

        val replayFail = repository.resetPassword(rawToken, "Another@Pass99!")
        assertTrue(replayFail is AuthResult.Error)
    }

    @Test
    fun `logout revokes jwt token`() = runTest {
        val login = repository.login("alex@secureauth.dev", "User@Secure123!", rememberMe = false)
        val token = (login as AuthResult.Success).data.claims.rawToken
        assertNotNull(repository.restoreSession(token))

        repository.logout(token)
        assertNull(repository.restoreSession(token))
    }
}
