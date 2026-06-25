package com.safiri.ridedelivery.ui.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.safiri.ridedelivery.data.model.PaymentMethod
import com.safiri.ridedelivery.navigation.Routes
import com.safiri.ridedelivery.viewmodel.CartViewModel
import com.safiri.ridedelivery.viewmodel.CatalogViewModel

/**
 * Payment selection. M-Pesa STK push would be triggered server-side
 * (Daraja API) via a Cloud Function — see the implementation guide.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    cartVm: CartViewModel,
    catalogVm: CatalogViewModel
) {
    var method by remember { mutableStateOf(PaymentMethod.MPESA) }
    var phone by remember { mutableStateOf("") }
    val address by cartVm.selectedAddress.collectAsState()
    val items by cartVm.items.collectAsState()
    val restaurants by catalogVm.restaurants.collectAsState()

    val restaurantId = items.firstOrNull()?.menuItem?.restaurantId
    val restaurant = restaurants.find { it.id == restaurantId }

    Scaffold(topBar = { TopAppBar(title = { Text("Payment") }) }) { pad ->
        Column(Modifier.padding(pad).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {

            address?.let {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Delivering to:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(it.address, fontSize = 14.sp)
                    }
                }
            }

            Text("Select payment method", style = MaterialTheme.typography.titleLarge)
            PaymentMethod.values().forEach { m ->
                Row(Modifier.fillMaxWidth().selectable(method == m, onClick = { method = m }),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = method == m, onClick = null)
                    Spacer(Modifier.width(8.dp)); Text(m.name)
                }
            }
            if (method == PaymentMethod.MPESA) {
                OutlinedTextField(phone, { phone = it }, label = { Text("M-Pesa phone (2547…)") },
                    modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = {
                    if (restaurant != null && address != null) {
                        cartVm.checkout(restaurant, address!!, method) { orderId ->
                            if (orderId != null) {
                                navController.navigate(Routes.orderTracking(orderId)) {
                                    popUpTo(Routes.HOME)
                                }
                            }
                        }
                    }
                },
                enabled = address != null && (method != PaymentMethod.MPESA || phone.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (method == PaymentMethod.MPESA) "Pay with M-Pesa" else "Confirm payment")
            }
        }
    }
}
