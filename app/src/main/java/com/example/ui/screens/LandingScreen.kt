package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeepIndigo
import com.example.ui.theme.EmeraldSoft
import com.example.ui.theme.MidnightNavy

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LandingScreen(
    isDark: Boolean,
    isAuthenticated: Boolean,
    isAdmin: Boolean,
    onToggleTheme: () -> Unit,
    onNavigateLogin: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 880.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Brand Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = "SecureAuth Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Column {
                        Text("SecureAuth", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Identity & Access Platform",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("landing_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = if (isDark) "Switch to light mode" else "Switch to dark mode"
                    )
                }
            }

            // Custom Cryptographic Hero Banner with Canvas Geometry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(MidnightNavy, DeepIndigo, Color(0xFF0F172A))
                        )
                    )
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val step = 42.dp.toPx()
                    var x = 0f
                    while (x < size.width) {
                        drawLine(
                            color = CyanPulse.copy(alpha = 0.08f),
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1f
                        )
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = CyanPulse.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += step
                    }
                    // Accent shield vector outline on right
                    val cx = size.width * 0.86f
                    val cy = size.height * 0.45f
                    val r = size.minDimension * 0.36f
                    val shield = Path().apply {
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.85f, cy - r * 0.55f)
                        lineTo(cx + r * 0.85f, cy + r * 0.25f)
                        lineTo(cx, cy + r)
                        lineTo(cx - r * 0.85f, cy + r * 0.25f)
                        lineTo(cx - r * 0.85f, cy - r * 0.55f)
                        close()
                    }
                    drawPath(shield, color = CyanPulse.copy(alpha = 0.22f), style = Stroke(width = 3f))
                    drawCircle(
                        color = EmeraldSoft.copy(alpha = 0.16f),
                        radius = r * 0.42f,
                        center = Offset(cx, cy)
                    )
                }

                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = CyanPulse.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "ZERO-TRUST AUTHENTICATION SUITE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPulse,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Text(
                        text = "Enterprise User Management & Cryptographic Security",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Role-based access control, salted password hashing, signed JWT lifecycle, login rate limiting, and immutable admin audit logs—engineered across Android, Spring Boot 3, React, and MySQL 8.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isAuthenticated) {
                            Button(
                                onClick = onNavigateDashboard,
                                modifier = Modifier.testTag("landing_open_dashboard_button")
                            ) {
                                Text(if (isAdmin) "Open Admin Console" else "Open Dashboard")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Button(
                                onClick = onNavigateLogin,
                                modifier = Modifier.testTag("landing_login_button")
                            ) {
                                Text("Sign In")
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            OutlinedButton(
                                onClick = onNavigateRegister,
                                border = BorderStroke(1.dp, CyanPulse.copy(alpha = 0.6f)),
                                modifier = Modifier.testTag("landing_register_button")
                            ) {
                                Text("Create Account", color = Color.White)
                            }
                        }
                    }
                }
            }

            // Security Architecture Highlights
            Text(
                text = "Security Architecture Highlights",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            val highlights = listOf(
                Triple(Icons.Filled.Lock, "Salted Password Hashing", "120k-iteration PBKDF2 / BCrypt hashing with constant-time verification."),
                Triple(Icons.Filled.Key, "Signed JWT & Revocation", "HMAC-SHA256 tokens with expiry checks and JTI denylist on sign-out."),
                Triple(Icons.Filled.AdminPanelSettings, "Strict Backend RBAC", "USER and ADMIN roles verified at repository/controller boundary."),
                Triple(Icons.Filled.History, "Audit & Login Telemetry", "Tracks login timestamps, status, and every admin account action."),
                Triple(Icons.Filled.Fingerprint, "Brute-Force Protection", "Sliding-window rate limiter locks repeated failed attempts."),
                Triple(Icons.Filled.VerifiedUser, "Anti-Enumeration", "Uniform responses on login and password reset protect user privacy.")
            )

            highlights.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { (icon, title, desc) ->
                        FeatureHighlightCard(
                            icon = icon,
                            title = title,
                            description = desc,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            // Full-Stack Technology Stack Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Full-Stack Monorepo Included",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "This workspace pairs the live Android Jetpack Compose client with the complete Spring Boot 3 + React + Vite + Tailwind + MySQL 8 + Flyway + Docker monorepo under secureauth/.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Kotlin + Compose",
                            "Room SQLite",
                            "Spring Boot 3",
                            "Spring Security 6",
                            "React + Vite",
                            "Tailwind CSS",
                            "MySQL 8 + Flyway",
                            "Swagger / OpenAPI"
                        ).forEach { tech ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = tech,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Footer
            Text(
                text = "SecureAuth v1.0 • MCA Full-Stack Security Showcase • MIT License",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun FeatureHighlightCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(20.dp)
                )
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
