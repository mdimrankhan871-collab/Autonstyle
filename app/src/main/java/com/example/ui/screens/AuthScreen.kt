package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.AutonstyleBrandMark
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonRedSoft
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWhite

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AutonCanvas)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = AutonWhite),
                border = BorderStroke(1.dp, AutonBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_autonstyle_banner_1794699893816),
                            contentDescription = "Autonstyle Automotive Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            AutonNavy.copy(alpha = 0.92f),
                                            AutonNavy.copy(alpha = 0.45f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            AutonstyleBrandMark(
                                darkBackground = true,
                                subtitle = "AUTONSTYLE.COM"
                            )
                            Column {
                                Text(
                                    text = "Complete Business Accounting",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = AutonWhite
                                )
                                Text(
                                    text = "BDT (৳)  •  Asia/Dhaka  •  Atomic Inventory & P&L",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AutonLime
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    ) {
                        Box(modifier = Modifier.weight(0.65f).height(4.dp).background(AutonRed))
                        Box(modifier = Modifier.weight(0.35f).height(4.dp).background(AutonLime))
                    }

                    Column(modifier = Modifier.padding(22.dp)) {
                        FeatureRow(
                            icon = Icons.Rounded.Inventory2,
                            title = "Atomic Stock & Packaging Control",
                            desc = "Auto-deduct product & assigned packaging on pack/dispatch without duplicates."
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureRow(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            title = "Real-Time Profit & Loss Ledger",
                            desc = "Exact COGS, packaging, courier, return & ad expense tracking in BDT."
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        FeatureRow(
                            icon = Icons.Rounded.CloudDone,
                            title = "Secured Cloud Firestore & Exports",
                            desc = "Permanent cloud sync, JSON backup/restore, plus CSV, Excel & PDF reports."
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = AutonRedSoft,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = AutonRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                isLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess()
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isLoading = false }
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AutonNavy,
                                contentColor = AutonWhite
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = AutonLime,
                                    strokeWidth = 2.5.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = "Google Sign In",
                                    tint = AutonLime
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(AutonCanvas),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = AutonNavy, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleSmall, color = AutonNavy)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = AutonTextSecondary)
        }
    }
}
