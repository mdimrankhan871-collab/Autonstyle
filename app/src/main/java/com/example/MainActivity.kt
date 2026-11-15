package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.AssignmentReturn
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.repository.AutonstyleRepository
import com.example.ui.components.AutonstyleBrandMark
import com.example.ui.screens.AppDestination
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.LedgerAndBackupScreen
import com.example.ui.screens.OrdersScreen
import com.example.ui.screens.PackagingScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ProfitLossScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ReturnsScreen
import com.example.ui.screens.StockAlertsScreen
import com.example.ui.theme.AutonBorder
import com.example.ui.theme.AutonCanvas
import com.example.ui.theme.AutonLime
import com.example.ui.theme.AutonNavy
import com.example.ui.theme.AutonNavySoft
import com.example.ui.theme.AutonRed
import com.example.ui.theme.AutonTextSecondary
import com.example.ui.theme.AutonWhite
import com.example.ui.theme.AutonstyleTheme
import com.example.ui.viewmodel.AutonstyleViewModel
import com.example.ui.viewmodel.BusinessSnapshot
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AutonstyleTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AutonstyleAppShell(
                currentUserId = user.uid,
                userEmail = user.email
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutonstyleAppShell(
    currentUserId: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: AutonstyleViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val dbId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                AutonstyleViewModel(AutonstyleRepository(db), currentUserId)
            }
        }
    )

    val uiState by viewModel.businessState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var currentDest by remember { mutableStateOf(AppDestination.DASHBOARD) }
    var openNewOrderTrigger by remember { mutableStateOf(false) }
    var openNewPurchaseTrigger by remember { mutableStateOf(false) }
    var openNewExpenseTrigger by remember { mutableStateOf(false) }

    if (currentDest != AppDestination.DASHBOARD) {
        BackHandler {
            currentDest = AppDestination.DASHBOARD
        }
    }

    val alertCount = (uiState as? UiState.Success)?.data?.stockAlerts?.totalAlerts ?: 0

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideLayout = maxWidth >= 840.dp

        if (isWideLayout) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(
                        modifier = Modifier.width(264.dp),
                        drawerContainerColor = AutonNavy,
                        drawerContentColor = AutonWhite
                    ) {
                        SidebarDrawerContent(
                            currentDest = currentDest,
                            alertCount = alertCount,
                            userEmail = userEmail,
                            onSelect = { currentDest = it }
                        )
                    }
                }
            ) {
                Scaffold(
                    containerColor = AutonCanvas,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainScreenBody(
                            uiState = uiState,
                            isBusy = isBusy,
                            currentDest = currentDest,
                            viewModel = viewModel,
                            userEmail = userEmail,
                            openNewOrderTrigger = openNewOrderTrigger,
                            openNewPurchaseTrigger = openNewPurchaseTrigger,
                            openNewExpenseTrigger = openNewExpenseTrigger,
                            onNavigate = { currentDest = it },
                            onResetTriggers = {
                                openNewOrderTrigger = false
                                openNewPurchaseTrigger = false
                                openNewExpenseTrigger = false
                            },
                            onQuickNewOrder = {
                                openNewOrderTrigger = true
                                currentDest = AppDestination.ORDERS
                            },
                            onQuickNewPurchase = {
                                openNewPurchaseTrigger = true
                                currentDest = AppDestination.PRODUCTS
                            },
                            onQuickNewExpense = {
                                openNewExpenseTrigger = true
                                currentDest = AppDestination.EXPENSES
                            },
                            onSignOut = {
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }
                }
            }
        } else {
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        modifier = Modifier.width(280.dp),
                        drawerContainerColor = AutonNavy,
                        drawerContentColor = AutonWhite
                    ) {
                        SidebarDrawerContent(
                            currentDest = currentDest,
                            alertCount = alertCount,
                            userEmail = userEmail,
                            onSelect = { dest ->
                                currentDest = dest
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            ) {
                Scaffold(
                    containerColor = AutonCanvas,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                AutonstyleBrandMark(
                                    darkBackground = true,
                                    subtitle = currentDest.label.uppercase()
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_drawer_button")
                                ) {
                                    Icon(Icons.Rounded.Menu, contentDescription = "Open Navigation Menu", tint = AutonWhite)
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { currentDest = AppDestination.ALERTS },
                                    modifier = Modifier.testTag("top_alerts_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (alertCount > 0) {
                                                Badge(containerColor = AutonRed, contentColor = AutonWhite) {
                                                    Text(alertCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.WarningAmber, contentDescription = "Stock Alerts", tint = AutonLime)
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = AutonNavy)
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = AutonWhite,
                            tonalElevation = 4.dp,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            val bottomItems = listOf(
                                AppDestination.DASHBOARD,
                                AppDestination.ORDERS,
                                AppDestination.PRODUCTS,
                                AppDestination.PACKAGING,
                                AppDestination.REPORTS
                            )
                            bottomItems.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentDest == dest,
                                    onClick = { currentDest = dest },
                                    icon = { Icon(destinationIcon(dest), contentDescription = dest.label) },
                                    label = { Text(dest.label, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AutonNavy,
                                        selectedTextColor = AutonNavy,
                                        indicatorColor = AutonLime
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MainScreenBody(
                            uiState = uiState,
                            isBusy = isBusy,
                            currentDest = currentDest,
                            viewModel = viewModel,
                            userEmail = userEmail,
                            openNewOrderTrigger = openNewOrderTrigger,
                            openNewPurchaseTrigger = openNewPurchaseTrigger,
                            openNewExpenseTrigger = openNewExpenseTrigger,
                            onNavigate = { currentDest = it },
                            onResetTriggers = {
                                openNewOrderTrigger = false
                                openNewPurchaseTrigger = false
                                openNewExpenseTrigger = false
                            },
                            onQuickNewOrder = {
                                openNewOrderTrigger = true
                                currentDest = AppDestination.ORDERS
                            },
                            onQuickNewPurchase = {
                                openNewPurchaseTrigger = true
                                currentDest = AppDestination.PRODUCTS
                            },
                            onQuickNewExpense = {
                                openNewExpenseTrigger = true
                                currentDest = AppDestination.EXPENSES
                            },
                            onSignOut = {
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarDrawerContent(
    currentDest: AppDestination,
    alertCount: Int,
    userEmail: String?,
    onSelect: (AppDestination) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
    ) {
        AutonstyleBrandMark(
            darkBackground = true,
            subtitle = "AUTONSTYLE.COM"
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = userEmail ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = AutonWhite.copy(alpha = 0.7f),
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = AutonWhite.copy(alpha = 0.16f))
        Spacer(modifier = Modifier.height(10.dp))

        AppDestination.entries.forEach { dest ->
            val selected = currentDest == dest
            NavigationDrawerItem(
                label = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dest.label, style = MaterialTheme.typography.labelLarge)
                        if (dest == AppDestination.ALERTS && alertCount > 0) {
                            Badge(containerColor = AutonRed, contentColor = AutonWhite) {
                                Text(alertCount.toString())
                            }
                        }
                    }
                },
                selected = selected,
                onClick = { onSelect(dest) },
                icon = { Icon(destinationIcon(dest), contentDescription = dest.label) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = AutonLime,
                    selectedIconColor = AutonNavy,
                    selectedTextColor = AutonNavy,
                    unselectedContainerColor = AutonNavy,
                    unselectedIconColor = AutonWhite.copy(alpha = 0.85f),
                    unselectedTextColor = AutonWhite.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun MainScreenBody(
    uiState: UiState<BusinessSnapshot>,
    isBusy: Boolean,
    currentDest: AppDestination,
    viewModel: AutonstyleViewModel,
    userEmail: String?,
    openNewOrderTrigger: Boolean,
    openNewPurchaseTrigger: Boolean,
    openNewExpenseTrigger: Boolean,
    onNavigate: (AppDestination) -> Unit,
    onResetTriggers: () -> Unit,
    onQuickNewOrder: () -> Unit,
    onQuickNewPurchase: () -> Unit,
    onQuickNewExpense: () -> Unit,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = AutonRed,
                trackColor = AutonNavySoft
            )
        }

        when (uiState) {
            UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = AutonNavy)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Syncing Autonstyle Cloud Ledger...", color = AutonTextSecondary)
                    }
                }
            }

            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AutonRed
                    )
                }
            }

            is UiState.Success -> {
                val snapshot = uiState.data
                when (currentDest) {
                    AppDestination.DASHBOARD -> DashboardScreen(
                        snapshot = snapshot,
                        onNavigate = onNavigate,
                        onQuickNewOrder = onQuickNewOrder,
                        onQuickNewPurchase = onQuickNewPurchase,
                        onQuickNewExpense = onQuickNewExpense
                    )

                    AppDestination.ORDERS -> OrdersScreen(
                        snapshot = snapshot,
                        viewModel = viewModel,
                        openCreateOnLaunch = openNewOrderTrigger,
                        onCreateHandled = onResetTriggers
                    )

                    AppDestination.PRODUCTS -> ProductsScreen(
                        snapshot = snapshot,
                        viewModel = viewModel,
                        openPurchaseOnLaunch = openNewPurchaseTrigger,
                        onPurchaseHandled = onResetTriggers
                    )

                    AppDestination.PACKAGING -> PackagingScreen(
                        snapshot = snapshot,
                        viewModel = viewModel
                    )

                    AppDestination.RETURNS -> ReturnsScreen(
                        snapshot = snapshot,
                        viewModel = viewModel
                    )

                    AppDestination.EXPENSES -> ExpensesScreen(
                        snapshot = snapshot,
                        viewModel = viewModel,
                        openAddOnLaunch = openNewExpenseTrigger,
                        onAddHandled = onResetTriggers
                    )

                    AppDestination.PROFIT_LOSS -> ProfitLossScreen(snapshot = snapshot)

                    AppDestination.ALERTS -> StockAlertsScreen(
                        snapshot = snapshot,
                        viewModel = viewModel,
                        onNavigateToProducts = { onNavigate(AppDestination.PRODUCTS) },
                        onNavigateToPackaging = { onNavigate(AppDestination.PACKAGING) }
                    )

                    AppDestination.REPORTS -> ReportsScreen(
                        snapshot = snapshot,
                        viewModel = viewModel
                    )

                    AppDestination.LEDGER -> LedgerAndBackupScreen(
                        snapshot = snapshot,
                        viewModel = viewModel,
                        userEmail = userEmail,
                        onSignOut = onSignOut
                    )
                }
            }
        }
    }
}

private fun destinationIcon(dest: AppDestination): ImageVector = when (dest) {
    AppDestination.DASHBOARD -> Icons.Rounded.Dashboard
    AppDestination.ORDERS -> Icons.Rounded.ShoppingBag
    AppDestination.PRODUCTS -> Icons.Rounded.Inventory2
    AppDestination.PACKAGING -> Icons.Rounded.Layers
    AppDestination.RETURNS -> Icons.Rounded.AssignmentReturn
    AppDestination.EXPENSES -> Icons.Rounded.Payments
    AppDestination.PROFIT_LOSS -> Icons.Rounded.TrendingUp
    AppDestination.ALERTS -> Icons.Rounded.WarningAmber
    AppDestination.REPORTS -> Icons.Rounded.Assessment
    AppDestination.LEDGER -> Icons.Rounded.History
}
