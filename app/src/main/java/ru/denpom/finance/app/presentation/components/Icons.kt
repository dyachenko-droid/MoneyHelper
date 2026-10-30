package ru.denpom.finance.app.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Chair
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.CreditScore
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalGasStation
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Percent
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingBasket
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Theaters
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Category
import androidx.compose.ui.graphics.vector.ImageVector
import ru.denpom.finance.app.domain.model.AccountType

object CategoryIcons {
    val all: Map<String, ImageVector> = linkedMapOf(
        "shopping_basket" to Icons.Rounded.ShoppingBasket,
        "local_cafe" to Icons.Rounded.LocalCafe,
        "restaurant" to Icons.Rounded.Restaurant,
        "local_taxi" to Icons.Rounded.LocalTaxi,
        "directions_car" to Icons.Rounded.DirectionsCar,
        "local_gas_station" to Icons.Rounded.LocalGasStation,
        "shopping_bag" to Icons.Rounded.ShoppingBag,
        "checkroom" to Icons.Rounded.Checkroom,
        "chair" to Icons.Rounded.Chair,
        "home" to Icons.Rounded.Home,
        "bolt" to Icons.Rounded.Bolt,
        "medication" to Icons.Rounded.Medication,
        "fitness_center" to Icons.Rounded.FitnessCenter,
        "theaters" to Icons.Rounded.Theaters,
        "sports_esports" to Icons.Rounded.SportsEsports,
        "spa" to Icons.Rounded.Spa,
        "autorenew" to Icons.Rounded.Autorenew,
        "smartphone" to Icons.Rounded.Smartphone,
        "child_care" to Icons.Rounded.ChildCare,
        "school" to Icons.Rounded.School,
        "book" to Icons.Rounded.Book,
        "pets" to Icons.Rounded.Pets,
        "flight" to Icons.Rounded.Flight,
        "redeem" to Icons.Rounded.Redeem,
        "payments" to Icons.Rounded.Payments,
        "work" to Icons.Rounded.Work,
        "percent" to Icons.Rounded.Percent,
        "savings" to Icons.Rounded.Savings,
        "swap_horiz" to Icons.Rounded.SwapHoriz,
    )

    fun of(key: String?): ImageVector = all[key] ?: Icons.Rounded.Category

    fun account(type: AccountType): ImageVector = when (type) {
        AccountType.CARD -> Icons.Rounded.CreditCard
        AccountType.CASH -> Icons.Rounded.Payments
        AccountType.DEPOSIT -> Icons.Rounded.Savings
        AccountType.CREDIT -> Icons.Rounded.CreditScore
    }
}
