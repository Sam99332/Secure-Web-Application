package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.LoginHistoryEntity
import com.example.data.repository.UserSession
import com.example.security.PasswordSecurity
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.MetricCard
import com.example.ui.components.PasswordStrengthCard
import com.example.ui.components.RoleChip
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.EmeraldVerified

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UserDashboardScreen(
    session: UserSession,
    loginHistory: List<LoginHistoryEntity>,
    isBusy: Boolean,
    onUpdateProfile: (fullName: String, email: String, avatarUri: String?) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String, onCleared: () -> Unit) -> Unit,
    onOpenAdminConsole: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val user = session.user.user
    val roles = session.user.roles

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 880.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    UserAvatar(fullName = user.fullName, avatarUri = user.avatarUri, size = 58.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Welcome back, ${user.fullName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${user.username} • ${user.email}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusPill(user.accountStatus)
                            roles.forEach { RoleChip(it.roleName) }
                        }
                    }
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("user_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out")
                    }
                }
            }

            if (session.isAdmin) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Administrator privileges active", style = MaterialTheme.typography.labelLarge)
                        }
                        Button(
                            onClick = onOpenAdminConsole,
                            modifier = Modifier.testTag("user_open_admin_button")
                        ) {
                            Text("Admin Console")
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                    icon = { Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("user_tab_overview")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Profile & Security") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("user_tab_profile")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Activity") },
                    icon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("user_tab_activity")
                )
            }

            when (selectedTab) {
                0 -> UserOverviewTab(session = session, loginHistoryCount = loginHistory.size)
                1 -> UserProfileAndPasswordTab(
                    session = session,
                    isBusy = isBusy,
                    onUpdateProfile = onUpdateProfile,
                    onChangePassword = onChangePassword
                )
                2 -> UserActivityTab(loginHistory = loginHistory)
            }
        }
    }

    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign Out",
            message = "This will revoke your active JWT session token and return to the landing screen.",
            confirmLabel = "Sign Out",
            onConfirm = {
                showLogoutDialog = false
                onLogout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun UserOverviewTab(session: UserSession, loginHistoryCount: Int) {
    val user = session.user.user
    val claims = session.claims
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = "Account Status",
                value = user.accountStatus,
                accent = EmeraldVerified,
                subtitle = "Verified identity",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                title = "Login Events",
                value = loginHistoryCount.toString(),
                accent = MaterialTheme.colorScheme.primary,
                subtitle = "Recorded sessions",
                modifier = Modifier.weight(1f)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account Metadata", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                InfoLine("Account Created", formatTimestamp(user.createdAt))
                InfoLine("Last Updated", formatTimestamp(user.updatedAt))
                InfoLine("Assigned Roles", session.user.roles.joinToString(", ") { it.roleName })
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Active JWT Session Inspector (HS256)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                InfoLine("Subject (sub)", claims.subject)
                InfoLine("Token ID (jti)", claims.jti)
                InfoLine("Issued At", formatTimestamp(claims.issuedAtMs))
                InfoLine("Expires At", formatTimestamp(claims.expiresAtMs))
                Text(
                    text = "Signature: ${claims.rawToken.take(42)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UserProfileAndPasswordTab(
    session: UserSession,
    isBusy: Boolean,
    onUpdateProfile: (fullName: String, email: String, avatarUri: String?) -> Unit,
    onChangePassword: (currentPassword: String, newPassword: String, onCleared: () -> Unit) -> Unit
) {
    val user = session.user.user
    var fullName by remember(user.id, user.fullName) { mutableStateOf(user.fullName) }
    var email by remember(user.id, user.email) { mutableStateOf(user.email) }
    var avatarUri by remember(user.id, user.avatarUri) { mutableStateOf(user.avatarUri) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val strength = remember(newPassword) { PasswordSecurity.evaluateStrength(newPassword) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            avatarUri = uri.toString()
            onUpdateProfile(fullName, email, avatarUri)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Profile Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    UserAvatar(fullName = fullName, avatarUri = avatarUri, size = 56.dp)
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.testTag("user_change_avatar_button")
                    ) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Update Photo")
                    }
                }
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_fullname_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("profile_email_input")
                )
                Button(
                    onClick = { onUpdateProfile(fullName, email, avatarUri) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("profile_save_button")
                ) {
                    Text("Save Profile Changes")
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.LockReset, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Change Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it; passwordError = null },
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_current_password_input")
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; passwordError = null },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_new_password_input")
                )
                PasswordStrengthCard(strength = strength)
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null },
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("change_confirm_password_input")
                )
                if (passwordError != null) {
                    Text(passwordError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = {
                        when {
                            currentPassword.isEmpty() -> passwordError = "Current password is required."
                            !strength.isAcceptable -> passwordError = "New password must satisfy complexity rules."
                            newPassword != confirmPassword -> passwordError = "New passwords do not match."
                            else -> onChangePassword(currentPassword, newPassword) {
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("change_password_submit_button")
                ) {
                    Text("Update Password")
                }
            }
        }
    }
}

@Composable
private fun UserActivityTab(loginHistory: List<LoginHistoryEntity>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Personal Login History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (loginHistory.isEmpty()) {
                Text(
                    "No login events recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                loginHistory.forEach { entry ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(formatTimestamp(entry.loginTime), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Origin: ${entry.ipAddress}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(entry.loginStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
