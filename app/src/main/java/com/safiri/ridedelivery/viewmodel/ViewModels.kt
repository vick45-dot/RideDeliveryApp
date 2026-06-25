package com.safiri.ridedelivery.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safiri.ridedelivery.data.model.*
import com.safiri.ridedelivery.data.repository.*
import com.safiri.ridedelivery.util.FareCalculator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/** Holds session state. Exposes isLoggedIn so UI can gate protected actions. */
class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _user = MutableStateFlow<AppUser?>(null)
    val user: StateFlow<AppUser?> = _user

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val isLoggedIn: Boolean get() = repo.isLoggedIn

    init { refresh() }

    fun refresh() = viewModelScope.launch { _user.value = repo.currentUser() }

    fun login(email: String, pass: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        repo.login(email, pass)
            .onSuccess { _user.value = it; _error.value = null; onDone(true) }
            .onFailure { _error.value = it.message; onDone(false) }
    }

    fun register(name: String, email: String, phone: String, pass: String, role: UserRole, onDone: (Boolean) -> Unit) =
        viewModelScope.launch {
            repo.register(name, email, phone, pass, role)
                .onSuccess { _user.value = it; _error.value = null; onDone(true) }
                .onFailure { _error.value = it.message; onDone(false) }
        }

    fun logout() { repo.logout(); _user.value = null }
}

/** Drives Home / Food / Search screens — all browseable without login. */
class CatalogViewModel(
    private val repo: CatalogRepository = CatalogRepository()
) : ViewModel() {
    val restaurants = MutableStateFlow<List<Restaurant>>(emptyList())
    val shops = MutableStateFlow<List<Shop>>(emptyList())
    val drivers = MutableStateFlow<List<Driver>>(emptyList())
    val menu = MutableStateFlow<List<MenuItem>>(emptyList())
    val searchResults = MutableStateFlow<Pair<List<Restaurant>, List<Shop>>>(emptyList<Restaurant>() to emptyList())
    val loading = MutableStateFlow(false)

    fun loadHome() = viewModelScope.launch {
        loading.value = true
        restaurants.value = repo.restaurants()
        shops.value = repo.shops()
        drivers.value = repo.availableDrivers()
        loading.value = false
    }

    fun loadMenu(restaurantId: String) = viewModelScope.launch {
        menu.value = repo.menu(restaurantId)
    }

    fun search(q: String) = viewModelScope.launch {
        if (q.isBlank()) { searchResults.value = emptyList<Restaurant>() to emptyList(); return@launch }
        searchResults.value = repo.search(q)
    }
}

/** Cart + checkout. Placing an order requires a logged-in customer. */
class CartViewModel(
    private val orderRepo: OrderRepository = OrderRepository(),
    private val auth: AuthRepository = AuthRepository()
) : ViewModel() {
    private val _items = MutableStateFlow<List<CartItem>>(emptyList())
    val items: StateFlow<List<CartItem>> = _items

    val selectedAddress = MutableStateFlow<GeoPoint?>(null)

    val subtotal: StateFlow<Double> = _items.map { it.sumOf { ci -> ci.lineTotal } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    fun add(item: MenuItem) {
        val existing = _items.value.find { it.menuItem.id == item.id }
        _items.value = if (existing == null) _items.value + CartItem(item, 1)
        else _items.value.map { if (it.menuItem.id == item.id) it.copy(quantity = it.quantity + 1) else it }
    }

    fun remove(itemId: String) {
        _items.value = _items.value.mapNotNull {
            if (it.menuItem.id == itemId) (if (it.quantity > 1) it.copy(quantity = it.quantity - 1) else null) else it
        }
    }

    fun clear() { _items.value = emptyList() }

    fun checkout(restaurant: Restaurant, dropoff: GeoPoint, method: PaymentMethod, onDone: (String?) -> Unit) =
        viewModelScope.launch {
            val uid = auth.currentUid ?: return@launch onDone(null) // gated by UI
            val sub = _items.value.sumOf { it.lineTotal }
            val order = Order(
                customerUid = uid,
                restaurantId = restaurant.id,
                restaurantName = restaurant.name,
                items = _items.value,
                subtotal = sub,
                deliveryFee = restaurant.deliveryFee,
                total = sub + restaurant.deliveryFee,
                pickup = restaurant.location,
                dropoff = dropoff,
                paymentMethod = method
            )
            orderRepo.placeOrder(order)
                .onSuccess { clear(); onDone(it) }
                .onFailure { onDone(null) }
        }
}

/** Ride booking: fare estimate -> request -> live tracking. */
class RideViewModel(
    private val rideRepo: RideRepository = RideRepository(),
    private val auth: AuthRepository = AuthRepository()
) : ViewModel() {
    val pickup = MutableStateFlow<GeoPoint?>(null)
    val destination = MutableStateFlow<GeoPoint?>(null)
    val vehicleType = MutableStateFlow(VehicleType.CAR)
    val fareEstimate = MutableStateFlow(0.0)
    val activeRideId = MutableStateFlow<String?>(null)

    fun recomputeFare() {
        val p = pickup.value; val d = destination.value
        fareEstimate.value = if (p != null && d != null)
            FareCalculator.estimate(vehicleType.value, p, d) else 0.0
    }

    fun bookRide(method: PaymentMethod, onDone: (String?) -> Unit) = viewModelScope.launch {
        val uid = auth.currentUid ?: return@launch onDone(null)
        val p = pickup.value ?: return@launch onDone(null)
        val d = destination.value ?: return@launch onDone(null)
        val ride = Ride(
            customerUid = uid,
            vehicleType = vehicleType.value,
            pickup = p, destination = d,
            fareEstimate = fareEstimate.value,
            distanceKm = FareCalculator.distanceKm(p, d),
            paymentMethod = method
        )
        rideRepo.requestRide(ride)
            .onSuccess { activeRideId.value = it; onDone(it) }
            .onFailure { onDone(null) }
    }

    fun observeRide(id: String) = rideRepo.observeRide(id)
}

/** Driver dashboard: online toggle, incoming requests, earnings. */
class DriverViewModel(
    private val driverRepo: DriverRepository = DriverRepository(),
    private val rideRepo: RideRepository = RideRepository(),
    private val auth: AuthRepository = AuthRepository()
) : ViewModel() {
    val profile = MutableStateFlow<Driver?>(null)
    val requests: StateFlow<List<Ride>> =
        rideRepo.pendingRequests().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load() = viewModelScope.launch {
        auth.currentUid?.let { profile.value = driverRepo.driver(it) }
    }

    fun toggleOnline(online: Boolean) = viewModelScope.launch {
        auth.currentUid?.let { driverRepo.setOnline(it, online); load() }
    }

    fun respond(ride: Ride, accept: Boolean) = viewModelScope.launch {
        profile.value?.let { rideRepo.respondToRide(ride.id, it, accept) }
    }

    fun saveVehicle(driver: Driver) = viewModelScope.launch {
        driverRepo.saveVehicle(driver); load()
    }
}

/** Admin: approvals and oversight. */
class AdminViewModel(
    private val driverRepo: DriverRepository = DriverRepository()
) : ViewModel() {
    val pending = MutableStateFlow<List<Driver>>(emptyList())

    fun load() = viewModelScope.launch { pending.value = driverRepo.pendingApprovals() }

    fun decide(uid: String, approve: Boolean) = viewModelScope.launch {
        driverRepo.setApproval(uid, if (approve) ApprovalStatus.APPROVED else ApprovalStatus.REJECTED)
        load()
    }
}

/** Restaurant dashboard: manage menu + view orders. */
class RestaurantViewModel(
    private val repo: RestaurantRepository = RestaurantRepository(),
    private val auth: AuthRepository = AuthRepository()
) : ViewModel() {
    val orders = MutableStateFlow<List<Order>>(emptyList())

    fun saveItem(item: MenuItem) = viewModelScope.launch { repo.saveMenuItem(item) }
    fun saveRestaurant(r: Restaurant, onDone: (String) -> Unit) =
        viewModelScope.launch { onDone(repo.saveRestaurant(r)) }
    fun loadOrders(restaurantId: String) = viewModelScope.launch {
        orders.value = repo.restaurantOrders(restaurantId)
    }
}
