package com.safiri.ridedelivery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.safiri.ridedelivery.data.model.Restaurant
import com.safiri.ridedelivery.navigation.Routes
import com.safiri.ridedelivery.ui.components.*
import com.safiri.ridedelivery.ui.theme.BrandGreen
import com.safiri.ridedelivery.ui.theme.BrandGreenDark
import com.safiri.ridedelivery.util.VehicleInfo
import com.safiri.ridedelivery.viewmodel.AuthViewModel
import com.safiri.ridedelivery.viewmodel.CartViewModel
import com.safiri.ridedelivery.viewmodel.CatalogViewModel

private val foodCategories = listOf(
    "🍽️" to "All", "🍕" to "Pizza", "🍖" to "Barbecue", "🍗" to "Fast Food",
    "🍛" to "Indian", "☕" to "Cafe", "🍰" to "Dessert", "🥗" to "Healthy", "🍲" to "Swahili"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    catalogVm: CatalogViewModel,
    authVm: AuthViewModel,
    cartVm: CartViewModel
) {
    LaunchedEffect(Unit) { catalogVm.loadHome() }
    val restaurants by catalogVm.restaurants.collectAsState()
    val drivers by catalogVm.drivers.collectAsState()
    val loading by catalogVm.loading.collectAsState()
    val user by authVm.user.collectAsState()
    val selectedAddress by cartVm.selectedAddress.collectAsState()
    var category by remember { mutableStateOf("All") }

    val filtered = if (category == "All") restaurants
        else restaurants.filter { it.category.equals(category, ignoreCase = true) }

    Scaffold(bottomBar = { AppBottomBar(navController) }) { pad ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandGreen)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(pad).fillMaxSize()) {
            item {
                GradientHero(
                    greeting = if (user != null) "Karibu back," else "Karibu 👋",
                    subtitle = if (user != null) "${user!!.name.split(" ").first()}, what would you like?"
                               else "Order food or book a ride"
                )
            }
            // Address selector pill
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-20).dp)
                        .clickable { navController.navigate(Routes.CART) },
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 4.dp,
                    color = Color.White
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = BrandGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = selectedAddress?.address ?: "Set delivery location",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Rounded.ChevronRight, null, tint = Color.Gray)
                    }
                }
            }
            // Two big quick-action tiles
            item {
                Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction("Order food", "🍔", BrandGreen, Modifier.weight(1f)) {
                        navController.navigate(Routes.FOOD)
                    }
                    QuickAction("Book a ride", "🚗", BrandGreenDark, Modifier.weight(1f)) {
                        navController.navigate(Routes.RIDE)
                    }
                }
            }
            // Drivers nearby
            item { SectionHeader("Drivers nearby (${drivers.size})", "See all") { navController.navigate(Routes.RIDE) } }
            item {
                LazyRow(
                    Modifier.padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(drivers) { d ->
                        val meta = VehicleInfo.of(d.vehicleType)
                        Card(
                            Modifier.width(150.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Text(meta.emoji, fontSize = 30.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(meta.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(d.vehicleModel, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Text("⭐ ${"%.1f".format(d.rating)}", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            // Category filter
            item { SectionHeader("Categories") }
            item { CategoryRow(foodCategories, category) { category = it } }
            // Restaurants
            item { SectionHeader(if (category == "All") "All restaurants" else "$category spots") }
            items(filtered) { r ->
                RestaurantRow(r) { navController.navigate(Routes.restaurantMenu(r.id)) }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun QuickAction(label: String, emoji: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = tint)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(emoji, fontSize = 30.sp)
            Spacer(Modifier.height(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
        }
    }
}

@Composable
fun RestaurantRow(r: Restaurant, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(r.emoji, size = 60)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(r.description, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RatingChip(r.rating, r.etaMinutes)
                    Spacer(Modifier.width(8.dp))
                    Text("🛵 KES ${r.deliveryFee.toInt()}", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
