package com.safiri.ridedelivery.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.safiri.ridedelivery.ui.admin.AdminDashboardScreen
import com.safiri.ridedelivery.ui.auth.AuthScreen
import com.safiri.ridedelivery.ui.driver.DriverDashboardScreen
import com.safiri.ridedelivery.ui.food.CartScreen
import com.safiri.ridedelivery.ui.food.FoodScreen
import com.safiri.ridedelivery.ui.food.RestaurantMenuScreen
import com.safiri.ridedelivery.ui.home.HomeScreen
import com.safiri.ridedelivery.ui.payment.PaymentScreen
import com.safiri.ridedelivery.ui.profile.ProfileScreen
import com.safiri.ridedelivery.ui.restaurant.RestaurantDashboardScreen
import com.safiri.ridedelivery.ui.ride.RideScreen
import com.safiri.ridedelivery.ui.search.SearchScreen
import com.safiri.ridedelivery.ui.splash.SplashScreen
import com.safiri.ridedelivery.ui.tracking.OrderTrackingScreen
import com.safiri.ridedelivery.ui.tracking.RideTrackingScreen
import com.safiri.ridedelivery.viewmodel.*

/**
 * Single source of navigation truth. ViewModels are shared where state must persist
 * across screens (cart, auth, ride).
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    authVm: AuthViewModel,
    catalogVm: CatalogViewModel,
    cartVm: CartViewModel,
    rideVm: RideViewModel
) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) { SplashScreen(navController) }
        composable(Routes.HOME) { HomeScreen(navController, catalogVm, authVm, cartVm) }
        composable(Routes.FOOD) { FoodScreen(navController, catalogVm) }
        composable(Routes.RIDE) { RideScreen(navController, rideVm, authVm) }
        composable(Routes.SEARCH) { SearchScreen(navController, catalogVm) }
        composable(Routes.AUTH) { AuthScreen(navController, authVm) }
        composable(Routes.PROFILE) { ProfileScreen(navController, authVm) }
        composable(Routes.CART) { CartScreen(navController, cartVm) }
        composable(Routes.PAYMENT) { PaymentScreen(navController, cartVm, catalogVm) }
        composable(Routes.DRIVER_DASH) { DriverDashboardScreen(navController, viewModel()) }
        composable(Routes.RIDER_DASH) { DriverDashboardScreen(navController, viewModel(), rider = true) }
        composable(Routes.RESTAURANT_DASH) { RestaurantDashboardScreen(navController, viewModel()) }
        composable(Routes.ADMIN_DASH) { AdminDashboardScreen(navController, viewModel()) }

        composable(
            Routes.RESTAURANT_MENU,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { entry ->
            RestaurantMenuScreen(
                navController, catalogVm, cartVm, authVm,
                restaurantId = entry.arguments?.getString("id").orEmpty()
            )
        }
        composable(
            Routes.ORDER_TRACKING,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { entry ->
            OrderTrackingScreen(navController, orderId = entry.arguments?.getString("orderId").orEmpty())
        }
        composable(
            Routes.RIDE_TRACKING,
            arguments = listOf(navArgument("rideId") { type = NavType.StringType })
        ) { entry ->
            RideTrackingScreen(navController, rideVm, rideId = entry.arguments?.getString("rideId").orEmpty())
        }
    }
}
