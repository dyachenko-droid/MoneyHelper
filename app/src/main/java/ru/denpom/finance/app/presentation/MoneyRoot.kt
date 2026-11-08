package ru.denpom.finance.app.presentation

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import ru.denpom.finance.app.LaunchAction
import ru.denpom.finance.app.domain.model.ThemeMode
import ru.denpom.finance.app.presentation.debts.DebtsScreen
import ru.denpom.finance.app.presentation.goals.GoalsScreen
import ru.denpom.finance.app.presentation.imports.ImportScreen
import ru.denpom.finance.app.presentation.reminders.RemindersScreen
import ru.denpom.finance.app.presentation.security.LockScreen
import ru.denpom.finance.app.presentation.security.SecurityScreen
import ru.denpom.finance.app.presentation.accounts.AccountsScreen
import ru.denpom.finance.app.presentation.add.AddOperationSheet
import ru.denpom.finance.app.presentation.analytics.AnalyticsScreen
import ru.denpom.finance.app.presentation.budgets.BudgetsScreen
import ru.denpom.finance.app.presentation.categories.CategoriesScreen
import ru.denpom.finance.app.presentation.components.BottomNav
import ru.denpom.finance.app.presentation.components.TextAction
import ru.denpom.finance.app.presentation.format.LocalMoney
import ru.denpom.finance.app.presentation.format.MoneyFormat
import ru.denpom.finance.app.presentation.home.HomeScreen
import ru.denpom.finance.app.presentation.more.BackupScreen
import ru.denpom.finance.app.presentation.more.MoreScreen
import ru.denpom.finance.app.presentation.navigation.Routes
import ru.denpom.finance.app.presentation.planning.BalanceCheckScreen
import ru.denpom.finance.app.presentation.planning.MonthlyReportScreen
import ru.denpom.finance.app.presentation.planning.OperationTemplatesScreen
import ru.denpom.finance.app.presentation.planning.PaymentCalendarScreen
import ru.denpom.finance.app.presentation.onboarding.OnboardingScreen
import ru.denpom.finance.app.presentation.operations.OperationsScreen
import ru.denpom.finance.app.presentation.receipt.ReceiptScreen
import ru.denpom.finance.app.presentation.scan.ScanScreen
import ru.denpom.finance.app.presentation.subscriptions.SubscriptionsScreen
import ru.denpom.finance.app.presentation.theme.App
import ru.denpom.finance.app.presentation.theme.AppText
import ru.denpom.finance.app.presentation.theme.MoneyTheme

@Composable
fun MoneyRoot(systemDark: Boolean, viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val locked by viewModel.locked.collectAsStateWithLifecycle()
    val launch by viewModel.launch.collectAsStateWithLifecycle()
    val s = settings ?: return
    val dark = when (s.theme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemDark
    }
    val start = remember { if (s.onboardingDone) Routes.HOME else Routes.ONBOARDING }
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    var addRequest by remember { mutableStateOf<AddRequest?>(null) }
    var snack by remember { mutableStateOf<Snack?>(null) }

    val actions = remember {
        object : AppActions {
            override fun openAdd(request: AddRequest) {
                addRequest = request
            }

            override fun snack(text: String, action: String?, onAction: (() -> Unit)?) {
                snack = Snack(text, action, onAction)
            }
        }
    }

    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val controller = WindowCompat.getInsetsController(window, view)
        val lightBars = !dark && route != Routes.SCAN
        controller.isAppearanceLightStatusBars = lightBars
        controller.isAppearanceLightNavigationBars = lightBars
    }

    MoneyTheme(dark) {
        CompositionLocalProvider(
            LocalMoney provides MoneyFormat(s.currency.symbol, s.hideAmounts),
            LocalAppActions provides actions,
        ) {
            val c = App.colors
            Box(Modifier.fillMaxSize().background(c.bg)) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        AppNavHost(nav, start, actions)
                    }
                    if (route in Routes.tabs) {
                        BottomNav(route) { target ->
                            if (target == Routes.SCAN) nav.navigate(Routes.SCAN) else nav.navigate(target) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
                SnackHost(snack, bottomPadding = if (route in Routes.tabs && addRequest == null) 112 else 24) { snack = null }
                if (locked && s.pinEnabled) {
                    val activity = LocalView.current.context as? Activity
                    LockScreen(
                        biometric = s.biometric,
                        check = viewModel::checkPin,
                        onUnlock = viewModel::unlock,
                        onExit = { activity?.finish() },
                    )
                }
            }
            LaunchedEffect(launch, locked, s.onboardingDone) {
                val action = launch ?: return@LaunchedEffect
                if ((locked && s.pinEnabled) || !s.onboardingDone) return@LaunchedEffect
                viewModel.consumeLaunch()
                when (action) {
                    LaunchAction.SCAN -> nav.navigate(Routes.SCAN) { launchSingleTop = true }
                    LaunchAction.ADD -> addRequest = AddRequest()
                }
            }
            addRequest?.let { request ->
                AddOperationSheet(
                    request = request,
                    onDismiss = { addRequest = null },
                    onOpenReceipt = { id -> nav.navigate(Routes.receipt(id)) },
                )
            }
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, start: String, actions: AppActions) {
    NavHost(nav, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = { name ->
                nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                actions.snack("Счёт «$name» создан")
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onScan = { nav.navigate(Routes.SCAN) },
                onAllOperations = { nav.navigate(Routes.OPS) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onBudgets = { nav.navigate(Routes.BUDGETS) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onAccounts = { nav.navigate(Routes.ACCOUNTS) },
                onSubscriptions = { nav.navigate(Routes.PAYMENTS) },
                onMore = { nav.navigate(Routes.MORE) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true } },
                onOperation = { op -> openOperation(nav, actions, op) },
            )
        }
        composable(Routes.OPS) { OperationsScreen(onOpen = { op -> openOperation(nav, actions, op) }) }
        composable(Routes.BUDGETS) { BudgetsScreen() }
        composable(Routes.MORE) { MoreScreen(onNavigate = { nav.navigate(it) }) }
        composable(Routes.SCAN) {
            ScanScreen(
                onClose = { nav.popBackStack() },
                onDraft = { nav.navigate(Routes.RECEIPT_DRAFT) { popUpTo(Routes.SCAN) { inclusive = true } } },
            )
        }
        composable(Routes.RECEIPT_DRAFT) {
            ReceiptScreen(
                onBack = { nav.popBackStack() },
                onSaved = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = false }; launchSingleTop = true } },
            )
        }
        composable(Routes.RECEIPT) {
            ReceiptScreen(onBack = { nav.popBackStack() }, onSaved = { nav.popBackStack() })
        }
        composable(Routes.ACCOUNTS) { AccountsScreen(onBack = { nav.popBackStack() }, onReconcile = { nav.navigate(Routes.RECONCILE) }) }
        composable(Routes.SUBSCRIPTIONS) { SubscriptionsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.CATEGORIES) { CategoriesScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ANALYTICS) { AnalyticsScreen(onBack = { nav.popBackStack() }, onMonthlyReport = { nav.navigate(Routes.MONTHLY_REPORT) }) }
        composable(Routes.BACKUP) { BackupScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.GOALS) { GoalsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DEBTS) { DebtsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SECURITY) { SecurityScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.REMINDERS) { RemindersScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.IMPORT) { ImportScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PAYMENTS) {
            PaymentCalendarScreen(
                onBack = { nav.popBackStack() },
                onSchedules = { nav.navigate(Routes.SUBSCRIPTIONS) },
                onOperation = { openOperation(nav, actions, it) },
            )
        }
        composable(Routes.RECONCILE) {
            BalanceCheckScreen(onBack = { nav.popBackStack() }, onOperation = { openOperation(nav, actions, it) })
        }
        composable(Routes.TEMPLATES) { OperationTemplatesScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.MONTHLY_REPORT) {
            MonthlyReportScreen(onBack = { nav.popBackStack() }, onOperation = { openOperation(nav, actions, it) })
        }
    }
}

private fun openOperation(nav: NavHostController, actions: AppActions, op: ru.denpom.finance.app.domain.model.Operation) {
    val receiptId = op.receiptId
    if (receiptId != null) nav.navigate(Routes.receipt(receiptId)) else actions.openAdd(AddRequest(editId = op.id))
}

@Composable
private fun SnackHost(snack: Snack?, bottomPadding: Int, onDismiss: () -> Unit) {
    val c = App.colors
    LaunchedEffect(snack?.id) {
        if (snack != null) {
            delay(4500)
            onDismiss()
        }
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = snack != null,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
        ) {
            val current = snack ?: return@AnimatedVisibility
            Row(
                Modifier
                    .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding.dp)
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .shadow(12.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.on)
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(current.text, style = AppText.label.copy(fontWeight = FontWeight.Normal), color = c.bg, modifier = Modifier.weight(1f).padding(vertical = 12.dp))
                if (current.action != null) {
                    TextAction(current.action, color = c.invPri) {
                        current.onAction?.invoke()
                        onDismiss()
                    }
                }
            }
        }
    }
}
