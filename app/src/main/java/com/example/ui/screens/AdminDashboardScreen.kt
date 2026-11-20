package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import com.example.data.local.AccountStatus
import com.example.data.local.AuditLogEntity
import com.example.data.local.LoginHistoryEntity
import com.example.data.local.RoleNames
import com.example.data.local.UserWithRoles
import com.example.data.repository.AdminOverview
import com.example.data.repository.UserSession
import com.example.data.repository.UserSortOrder
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.MetricCard
import com.example.ui.components.RoleChip
import com.example.ui.components.StatusPill
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimestamp
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.EmeraldVerified

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
    session: UserSession,
    overview: AdminOverview?,
    users: List<UserWithRoles>,
    auditLogs: List<AuditLogEntity>,
    globalLogins: List<LoginHistoryEntity>,
    searchQuery: String,
    statusFilter: String?,
    roleFilter: String?,
    sortOrder: UserSortOrder,
    isBusy: Boolean,
    onFiltersChanged: (query: String, status: String?, role: String?, sort: UserSortOrder) -> Unit,
    onEditUser: (userId: Long, fullName: String, email: String, username: String, onDone: () -> Unit) -> Unit,
    onToggleStatus: (userId: Long, newStatus: String) -> Unit,
    onUpdateRoles: (userId: Long, roles: Set<String>, onDone: () -> Unit) -> Unit,
    onDeleteUser: (userId: Long) -> Unit,
    onUpdateOwnProfile: (fullName: String, email: String, avatarUri: String?) -> Unit,
    onChangeOwnPassword: (currentPassword: String, newPassword: String, onCleared: () -> Unit) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!session.isAdmin) {
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("403 Forbidden", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.error)
            Text("Administrator privileges are required to view this console.", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var editingUser by remember { mutableStateOf<UserWithRoles?>(null) }
    var roleDialogUser by remember { mutableStateOf<UserWithRoles?>(null) }
    var statusConfirmUser by remember { mutableStateOf<UserWithRoles?>(null) }
    var deleteConfirmUser by remember { mutableStateOf<UserWithRoles?>(null) }
    var detailUser by remember { mutableStateOf<UserWithRoles?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 960.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Admin Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    Column {
                        Text("Admin Control Center", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(
                            "Signed in as ${session.user.user.username} (RBAC Verified)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("admin_logout_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Sign Out")
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Overview") },
                    icon = { Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_overview")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Users") },
                    icon = { Icon(Icons.Filled.Group, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_users")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit Logs") },
                    icon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_audit")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Profile") },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("admin_tab_profile")
                )
            }

            when (selectedTab) {
                0 -> AdminOverviewTab(overview = overview)
                1 -> AdminUsersTab(
                    currentAdminId = session.user.user.id,
                    users = users,
                    searchQuery = searchQuery,
                    statusFilter = statusFilter,
                    roleFilter = roleFilter,
                    sortOrder = sortOrder,
                    onFiltersChanged = onFiltersChanged,
                    onViewDetails = { detailUser = it },
                    onEdit = { editingUser = it },
                    onManageRoles = { roleDialogUser = it },
                    onToggleStatus = { statusConfirmUser = it },
                    onDelete = { deleteConfirmUser = it }
                )
                2 -> AdminAuditTab(auditLogs = auditLogs, globalLogins = globalLogins)
                3 -> UserDashboardScreen(
                    session = session,
                    loginHistory = globalLogins.filter { it.userId == session.user.user.id },
                    isBusy = isBusy,
                    onUpdateProfile = onUpdateOwnProfile,
                    onChangePassword = onChangeOwnPassword,
                    onOpenAdminConsole = {},
                    onLogout = onLogout
                )
            }
        }
    }

    detailUser?.let { uwr ->
        AlertDialog(
            onDismissRequest = { detailUser = null },
            title = { Text(uwr.user.fullName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ID: #${uwr.user.id}", style = MaterialTheme.typography.labelMedium)
                    Text("Username: @${uwr.user.username}", style = MaterialTheme.typography.bodyMedium)
                    Text("Email: ${uwr.user.email}", style = MaterialTheme.typography.bodyMedium)
                    Text("Status: ${uwr.user.accountStatus}", style = MaterialTheme.typography.bodyMedium)
                    Text("Roles: ${uwr.roles.joinToString(", ") { it.roleName }}", style = MaterialTheme.typography.bodyMedium)
                    Text("Created: ${formatTimestamp(uwr.user.createdAt)}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = { detailUser = null }) { Text("Close") }
            }
        )
    }

    editingUser?.let { target ->
        var fullName by remember(target.user.id) { mutableStateOf(target.user.fullName) }
        var email by remember(target.user.id) { mutableStateOf(target.user.email) }
        var username by remember(target.user.id) { mutableStateOf(target.user.username) }
        AlertDialog(
            onDismissRequest = { editingUser = null },
            title = { Text("Edit User: @${target.user.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_edit_fullname")
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_edit_email")
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_edit_username")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onEditUser(target.user.id, fullName, email, username) { editingUser = null }
                    },
                    modifier = Modifier.testTag("admin_edit_save")
                ) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { editingUser = null }) { Text("Cancel") }
            }
        )
    }

    roleDialogUser?.let { target ->
        var hasUser by remember(target.user.id) {
            mutableStateOf(target.roles.any { it.roleName == RoleNames.ROLE_USER })
        }
        var hasAdmin by remember(target.user.id) {
            mutableStateOf(target.roles.any { it.roleName == RoleNames.ROLE_ADMIN })
        }
        AlertDialog(
            onDismissRequest = { roleDialogUser = null },
            title = { Text("Manage Roles: @${target.user.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasUser, onCheckedChange = { hasUser = it })
                        Text("ROLE_USER — Standard user access")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = hasAdmin, onCheckedChange = { hasAdmin = it })
                        Text("ROLE_ADMIN — Full admin console & audit logs")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val roles = buildSet {
                            if (hasUser) add(RoleNames.ROLE_USER)
                            if (hasAdmin) add(RoleNames.ROLE_ADMIN)
                        }
                        onUpdateRoles(target.user.id, roles) { roleDialogUser = null }
                    },
                    modifier = Modifier.testTag("admin_roles_save")
                ) { Text("Apply Roles") }
            },
            dismissButton = {
                OutlinedButton(onClick = { roleDialogUser = null }) { Text("Cancel") }
            }
        )
    }

    statusConfirmUser?.let { target ->
        val nextStatus = if (target.user.accountStatus == AccountStatus.ACTIVE) AccountStatus.INACTIVE else AccountStatus.ACTIVE
        ConfirmationDialog(
            title = "${if (nextStatus == AccountStatus.ACTIVE) "Activate" else "Deactivate"} Account",
            message = "Change account status for @${target.user.username} to $nextStatus?",
            confirmLabel = if (nextStatus == AccountStatus.ACTIVE) "Activate" else "Deactivate",
            isDestructive = nextStatus == AccountStatus.INACTIVE,
            onConfirm = {
                statusConfirmUser = null
                onToggleStatus(target.user.id, nextStatus)
            },
            onDismiss = { statusConfirmUser = null }
        )
    }

    deleteConfirmUser?.let { target ->
        ConfirmationDialog(
            title = "Delete User Permanently",
            message = "Are you sure you want to delete @${target.user.username} (${target.user.email})? This action is audited and irreversible.",
            confirmLabel = "Delete User",
            isDestructive = true,
            onConfirm = {
                deleteConfirmUser = null
                onDeleteUser(target.user.id)
            },
            onDismiss = { deleteConfirmUser = null }
        )
    }
}

@Composable
private fun AdminOverviewTab(overview: AdminOverview?) {
    if (overview == null) return
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Total Users", overview.totalUsers.toString(), MaterialTheme.colorScheme.primary, "Registered accounts", Modifier.weight(1f))
            MetricCard("Active", overview.activeUsers.toString(), EmeraldVerified, "Permitted to sign in", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard("Inactive", overview.inactiveUsers.toString(), CrimsonAlert, "Suspended / locked", Modifier.weight(1f))
            MetricCard("Admins", overview.adminCount.toString(), AmberWarning, "Elevated RBAC", Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Recent Registrations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                overview.recentRegistrations.forEach { uwr ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            UserAvatar(uwr.user.fullName, uwr.user.avatarUri, size = 36.dp)
                            Column {
                                Text(uwr.user.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "@${uwr.user.username} • ${formatTimestamp(uwr.user.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusPill(uwr.user.accountStatus)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminUsersTab(
    currentAdminId: Long,
    users: List<UserWithRoles>,
    searchQuery: String,
    statusFilter: String?,
    roleFilter: String?,
    sortOrder: UserSortOrder,
    onFiltersChanged: (query: String, status: String?, role: String?, sort: UserSortOrder) -> Unit,
    onViewDetails: (UserWithRoles) -> Unit,
    onEdit: (UserWithRoles) -> Unit,
    onManageRoles: (UserWithRoles) -> Unit,
    onToggleStatus: (UserWithRoles) -> Unit,
    onDelete: (UserWithRoles) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { onFiltersChanged(it, statusFilter, roleFilter, sortOrder) },
            label = { Text("Search users by name, username, or email") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("admin_user_search")
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = statusFilter == null,
                onClick = { onFiltersChanged(searchQuery, null, roleFilter, sortOrder) },
                label = { Text("All Status") }
            )
            FilterChip(
                selected = statusFilter == AccountStatus.ACTIVE,
                onClick = { onFiltersChanged(searchQuery, AccountStatus.ACTIVE, roleFilter, sortOrder) },
                label = { Text("Active") }
            )
            FilterChip(
                selected = statusFilter == AccountStatus.INACTIVE,
                onClick = { onFiltersChanged(searchQuery, AccountStatus.INACTIVE, roleFilter, sortOrder) },
                label = { Text("Inactive") }
            )
            FilterChip(
                selected = roleFilter == RoleNames.ROLE_ADMIN,
                onClick = {
                    val next = if (roleFilter == RoleNames.ROLE_ADMIN) null else RoleNames.ROLE_ADMIN
                    onFiltersChanged(searchQuery, statusFilter, next, sortOrder)
                },
                leadingIcon = { Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(16.dp)) },
                label = { Text("Admins Only") }
            )
            FilterChip(
                selected = true,
                onClick = {
                    val values = UserSortOrder.entries
                    val next = values[(values.indexOf(sortOrder) + 1) % values.size]
                    onFiltersChanged(searchQuery, statusFilter, roleFilter, next)
                },
                label = { Text("Sort: ${sortOrder.label}") }
            )
        }

        if (users.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "No users match the current search and filter criteria.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            users.forEach { uwr ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                UserAvatar(uwr.user.fullName, uwr.user.avatarUri, size = 40.dp)
                                Column {
                                    Text(uwr.user.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "@${uwr.user.username} • ${uwr.user.email}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            StatusPill(uwr.user.accountStatus)
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            uwr.roles.forEach { RoleChip(it.roleName) }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(onClick = { onViewDetails(uwr) }) { Text("Details") }
                            OutlinedButton(onClick = { onEdit(uwr) }) {
                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit")
                            }
                            OutlinedButton(onClick = { onManageRoles(uwr) }) { Text("Roles") }
                            if (uwr.user.id != currentAdminId) {
                                OutlinedButton(onClick = { onToggleStatus(uwr) }) {
                                    Text(if (uwr.user.accountStatus == AccountStatus.ACTIVE) "Deactivate" else "Activate")
                                }
                                IconButton(
                                    onClick = { onDelete(uwr) },
                                    modifier = Modifier.testTag("admin_delete_user_${uwr.user.id}")
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "Delete ${uwr.user.username}",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAuditTab(
    auditLogs: List<AuditLogEntity>,
    globalLogins: List<LoginHistoryEntity>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Admin Audit Trail", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (auditLogs.isEmpty()) {
                    Text("No admin audit events logged.", style = MaterialTheme.typography.bodySmall)
                } else {
                    auditLogs.forEach { log ->
                        HorizontalDivider()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(log.action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                Text(formatTimestamp(log.timestamp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(log.details, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Actor: @${log.adminUsername} • Target: @${log.targetUsername ?: "N/A"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("System Login Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                globalLogins.take(25).forEach { entry ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("User #${entry.userId} • ${entry.ipAddress}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(formatTimestamp(entry.loginTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(entry.loginStatus)
                    }
                }
            }
        }
    }
}
