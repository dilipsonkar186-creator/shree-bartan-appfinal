package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.FinancialSummaryScreen
import com.example.ui.screens.AddEditCustomerScreen
import com.example.ui.screens.AdminCustomerLocationsScreen
import com.example.ui.screens.CustomerAppControlCenterScreen
import com.example.ui.screens.CustomerDetailScreen
import com.example.ui.screens.FolderDetailScreen
import com.example.ui.screens.FolderListScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.ShopProductsScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.FolderViewModel

import com.example.ui.screens.BiometricLockScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle

object Routes {
    const val LOGIN = "login"
    const val BIOMETRIC_LOCK = "biometric_lock"
    const val FOLDERS = "folders"
    const val FOLDER_DETAIL = "folder_detail/{folderId}"
    const val CUSTOMER_DETAIL = "customer_detail/{customerId}"
    const val ADD_EDIT_CUSTOMER = "add_edit_customer/{folderId}?customerId={customerId}"
    const val FINANCIAL_SUMMARY = "financial_summary"
    const val RECYCLE_BIN = "recycle_bin"
    const val SHOP_PRODUCTS = "shop_products"
    const val ADMIN_CUSTOMERS = "admin_customers"
    const val CUSTOMER_APP_HUB = "customer_app_hub"

    fun folderDetail(folderId: Long): String = "folder_detail/$folderId"
    fun customerDetail(customerId: Long): String = "customer_detail/$customerId"
    fun addCustomer(folderId: Long): String = "add_edit_customer/$folderId"
    fun editCustomer(folderId: Long, customerId: Long): String = "add_edit_customer/$folderId?customerId=$customerId"
}

@Composable
fun AppNavHost(
    viewModel: FolderViewModel,
    authViewModel: AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val targetProductId by viewModel.notificationTargetProductId.collectAsStateWithLifecycle()
    val targetCustomerId by viewModel.notificationTargetCustomerId.collectAsStateWithLifecycle()
    val activeOfferProduct by viewModel.activeOfferProduct.collectAsStateWithLifecycle()
    val shopProfile by viewModel.shopProfile.collectAsStateWithLifecycle()

    val isOwnerAuthorized = authState.user != null &&
        authState.user?.email?.equals(AuthViewModel.AUTHORIZED_OWNER_EMAIL, ignoreCase = true) == true

    if (activeOfferProduct != null) {
        com.example.ui.components.ProductOfferDeepLinkDialog(
            offerProduct = activeOfferProduct!!,
            shopProfile = shopProfile,
            onDismiss = { viewModel.clearNotificationTargetProduct() }
        )
    }

    androidx.compose.runtime.LaunchedEffect(targetProductId, isOwnerAuthorized) {
        if (targetProductId != null && isOwnerAuthorized) {
            navController.navigate(Routes.SHOP_PRODUCTS)
        }
    }

    androidx.compose.runtime.LaunchedEffect(targetCustomerId, isOwnerAuthorized) {
        if (targetCustomerId != null && isOwnerAuthorized) {
            val cid = targetCustomerId!!
            navController.navigate(Routes.customerDetail(cid))
            viewModel.clearNotificationTargetCustomer()
        }
    }

    val startDestination = if (isOwnerAuthorized) {
        if (authState.isBiometricLockEnabled && !authState.isAppUnlocked) {
            Routes.BIOMETRIC_LOCK
        } else {
            Routes.FOLDERS
        }
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    val authorized = authState.user != null &&
                        authState.user?.email?.equals(AuthViewModel.AUTHORIZED_OWNER_EMAIL, ignoreCase = true) == true
                    if (authorized) {
                        viewModel.autoRestoreFromFirebase()
                        if (authState.isBiometricLockEnabled && !authState.isAppUnlocked) {
                            navController.navigate(Routes.BIOMETRIC_LOCK) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.FOLDERS) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }

        composable(Routes.BIOMETRIC_LOCK) {
            BiometricLockScreen(
                onUnlockSuccess = {
                    authViewModel.setAppUnlocked(true)
                    navController.navigate(Routes.FOLDERS) {
                        popUpTo(Routes.BIOMETRIC_LOCK) { inclusive = true }
                    }
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.FOLDERS) {
            FolderListScreen(
                viewModel = viewModel,
                authViewModel = authViewModel,
                onFolderClick = { folderId ->
                    navController.navigate(Routes.folderDetail(folderId))
                },
                onCustomerClick = { customerId ->
                    navController.navigate(Routes.customerDetail(customerId))
                },
                onOpenFinancialSummary = {
                    navController.navigate(Routes.FINANCIAL_SUMMARY)
                },
                onOpenRecycleBin = {
                    navController.navigate(Routes.RECYCLE_BIN)
                },
                onNavigateToProducts = {
                    navController.navigate(Routes.SHOP_PRODUCTS)
                },
                onNavigateToAdminCustomers = {
                    navController.navigate(Routes.ADMIN_CUSTOMERS)
                },
                onNavigateToCustomerAppHub = {
                    navController.navigate(Routes.CUSTOMER_APP_HUB)
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLockAppRequested = {
                    navController.navigate(Routes.BIOMETRIC_LOCK) {
                        popUpTo(Routes.FOLDERS) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.FOLDER_DETAIL,
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            FolderDetailScreen(
                folderId = folderId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCustomerClick = { customerId ->
                    navController.navigate(Routes.customerDetail(customerId))
                },
                onAddCustomerClick = { fId ->
                    navController.navigate(Routes.addCustomer(fId))
                },
                onEditCustomerClick = { fId, custId ->
                    navController.navigate(Routes.editCustomer(fId, custId))
                },
                onOpenFinancialSummary = {
                    navController.navigate(Routes.FINANCIAL_SUMMARY)
                },
                onOpenRecycleBin = {
                    navController.navigate(Routes.RECYCLE_BIN)
                }
            )
        }

        composable(
            route = Routes.CUSTOMER_DETAIL,
            arguments = listOf(
                navArgument("customerId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val customerId = backStackEntry.arguments?.getLong("customerId") ?: return@composable
            CustomerDetailScreen(
                customerId = customerId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onEditCustomerClick = { fId, custId ->
                    navController.navigate(Routes.editCustomer(fId, custId))
                }
            )
        }

        composable(
            route = Routes.ADD_EDIT_CUSTOMER,
            arguments = listOf(
                navArgument("folderId") { type = NavType.LongType },
                navArgument("customerId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getLong("folderId") ?: return@composable
            val customerIdArg = backStackEntry.arguments?.getLong("customerId") ?: -1L
            val customerId = if (customerIdArg == -1L) null else customerIdArg

            AddEditCustomerScreen(
                folderId = folderId,
                customerId = customerId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCustomerCreated = { newCustomerId ->
                    navController.popBackStack()
                    navController.navigate(Routes.customerDetail(newCustomerId))
                }
            )
        }

        composable(Routes.FINANCIAL_SUMMARY) {
            FinancialSummaryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SHOP_PRODUCTS) {
            ShopProductsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_CUSTOMERS) {
            val registeredCustomers by viewModel.registeredCustomers.collectAsStateWithLifecycle()
            AdminCustomerLocationsScreen(
                customers = registeredCustomers,
                onRefresh = { viewModel.loadRegisteredCustomers() },
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Routes.CUSTOMER_APP_HUB) {
            CustomerAppControlCenterScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToProducts = { navController.navigate(Routes.SHOP_PRODUCTS) },
                onNavigateToAdminCustomers = { navController.navigate(Routes.ADMIN_CUSTOMERS) }
            )
        }
    }
}

