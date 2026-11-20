package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.UserDashboardScreen
import com.example.ui.theme.SecureAuthTheme
import com.example.ui.viewmodel.AppRoute
import com.example.ui.viewmodel.SecureAuthViewModel
import com.example.ui.viewmodel.UiToast

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: SecureAuthViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val isDark = state.darkThemeOverride ?: isSystemInDarkTheme()

            SecureAuthTheme(darkTheme = isDark) {
                SecureAuthApp(viewModel = viewModel, isDark = isDark)
            }
        }
    }
}

private data class NavDestination(
    val route: AppRoute,
    val label: String,
    val icon: ImageVector,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureAuthApp(
    viewModel: SecureAuthViewModel,
    isDark: Boolean
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(Unit) {
        viewModel.toasts.collect { toast ->
            val message = when (toast) {
                is UiToast.Success -> toast.message
                is UiToast.Error -> toast.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navEvents.collect { target ->
            navController.navigate(target.route) {
                popUpTo(AppRoute.LANDING.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    if (state.isInitializing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val session = state.session
    val navItems = buildList {
        add(NavDestination(AppRoute.LANDING, "Home", Icons.Filled.Home, "nav_home"))
        if (session == null) {
            add(NavDestination(AppRoute.LOGIN, "Sign In", Icons.AutoMirrored.Filled.Login, "nav_login"))
            add(NavDestination(AppRoute.REGISTER, "Register", Icons.Filled.PersonAdd, "nav_register"))
        } else {
            add(NavDestination(AppRoute.USER_DASHBOARD, "Dashboard", Icons.Filled.Dashboard, "nav_user_dashboard"))
            if (session.isAdmin) {
                add(NavDestination(AppRoute.ADMIN_DASHBOARD, "Admin", Icons.Filled.AdminPanelSettings, "nav_admin_dashboard"))
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            topBar = {
                if (currentRoute != AppRoute.LANDING.route) {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (currentRoute) {
                                    AppRoute.LOGIN.route -> "Sign In"
                                    AppRoute.REGISTER.route -> "Create Account"
                                    AppRoute.FORGOT_PASSWORD.route -> "Password Recovery"
                                    AppRoute.USER_DASHBOARD.route -> "User Dashboard"
                                    AppRoute.ADMIN_DASHBOARD.route -> "Admin Console"
                                    else -> "SecureAuth"
                                }
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.toggleDarkMode(isDark) },
                                modifier = Modifier.testTag("topbar_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode"
                                )
                            }
                        }
                    )
                }
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route.route,
                                onClick = {
                                    navController.navigate(item.route.route) {
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        navItems.forEach { item ->
                            NavigationRailItem(
                                selected = currentRoute == item.route.route,
                                onClick = {
                                    navController.navigate(item.route.route) {
                                        launchSingleTop = true
                                    }
                                },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag("${item.tag}_rail")
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (session == null) {
                        AppRoute.LANDING.route
                    } else if (session.isAdmin) {
                        AppRoute.ADMIN_DASHBOARD.route
                    } else {
                        AppRoute.USER_DASHBOARD.route
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppRoute.LANDING.route) {
                        LandingScreen(
                            isDark = isDark,
                            isAuthenticated = session != null,
                            isAdmin = session?.isAdmin == true,
                            onToggleTheme = { viewModel.toggleDarkMode(isDark) },
                            onNavigateLogin = { navController.navigate(AppRoute.LOGIN.route) },
                            onNavigateRegister = { navController.navigate(AppRoute.REGISTER.route) },
                            onNavigateDashboard = {
                                val dest = if (session?.isAdmin == true) AppRoute.ADMIN_DASHBOARD else AppRoute.USER_DASHBOARD
                                navController.navigate(dest.route)
                            }
                        )
                    }
                    composable(AppRoute.LOGIN.route) {
                        LoginScreen(
                            isBusy = state.isBusy,
                            onLogin = viewModel::login,
                            onNavigateRegister = { navController.navigate(AppRoute.REGISTER.route) },
                            onNavigateForgotPassword = {
                                viewModel.clearResetNotice()
                                navController.navigate(AppRoute.FORGOT_PASSWORD.route)
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(AppRoute.REGISTER.route) {
                        RegisterScreen(
                            isBusy = state.isBusy,
                            onRegister = viewModel::register,
                            onNavigateLogin = { navController.navigate(AppRoute.LOGIN.route) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(AppRoute.FORGOT_PASSWORD.route) {
                        ForgotPasswordScreen(
                            isBusy = state.isBusy,
                            resetNotice = state.resetSubmittedNotice,
                            devTokenPreview = state.devResetTokenPreview,
                            onRequestReset = viewModel::requestPasswordReset,
                            onConfirmReset = { token, newPass ->
                                viewModel.resetPassword(token, newPass) {
                                    navController.navigate(AppRoute.LOGIN.route) {
                                        popUpTo(AppRoute.LANDING.route)
                                    }
                                }
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(AppRoute.USER_DASHBOARD.route) {
                        if (session == null) {
                            LaunchedEffect(Unit) {
                                navController.navigate(AppRoute.LOGIN.route) {
                                    popUpTo(AppRoute.LANDING.route)
                                }
                            }
                        } else {
                            UserDashboardScreen(
                                session = session,
                                loginHistory = state.userLoginHistory,
                                isBusy = state.isBusy,
                                onUpdateProfile = viewModel::updateProfile,
                                onChangePassword = viewModel::changePassword,
                                onOpenAdminConsole = {
                                    viewModel.loadAdminData()
                                    navController.navigate(AppRoute.ADMIN_DASHBOARD.route)
                                },
                                onLogout = viewModel::logout
                            )
                        }
                    }
                    composable(AppRoute.ADMIN_DASHBOARD.route) {
                        if (session == null) {
                            LaunchedEffect(Unit) {
                                navController.navigate(AppRoute.LOGIN.route) {
                                    popUpTo(AppRoute.LANDING.route)
                                }
                            }
                        } else {
                            AdminDashboardScreen(
                                session = session,
                                overview = state.adminOverview,
                                users = state.adminUsers,
                                auditLogs = state.adminAuditLogs,
                                globalLogins = state.adminGlobalLogins,
                                searchQuery = state.adminSearchQuery,
                                statusFilter = state.adminStatusFilter,
                                roleFilter = state.adminRoleFilter,
                                sortOrder = state.adminSortOrder,
                                isBusy = state.isBusy,
                                onFiltersChanged = { q, s, r, sort -> viewModel.updateAdminFilters(q, s, r, sort) },
                                onEditUser = viewModel::adminEditUser,
                                onToggleStatus = viewModel::adminToggleUserStatus,
                                onUpdateRoles = viewModel::adminUpdateRoles,
                                onDeleteUser = viewModel::adminDeleteUser,
                                onUpdateOwnProfile = viewModel::updateProfile,
                                onChangeOwnPassword = viewModel::changePassword,
                                onLogout = viewModel::logout
                            )
                        }
                    }
                }
            }
        }
    }
}
