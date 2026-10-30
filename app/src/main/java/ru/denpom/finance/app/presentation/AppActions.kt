package ru.denpom.finance.app.presentation

import androidx.compose.runtime.staticCompositionLocalOf
import ru.denpom.finance.app.domain.model.OperationType

data class AddRequest(
    val type: OperationType = OperationType.EXPENSE,
    val amount: Long = 0,
    val title: String = "",
    val note: String = "",
    val timestamp: Long? = null,
    val editId: Long? = null,
)

data class Snack(val text: String, val action: String? = null, val onAction: (() -> Unit)? = null, val id: Long = System.nanoTime())

interface AppActions {
    fun openAdd(request: AddRequest = AddRequest())
    fun snack(text: String, action: String? = null, onAction: (() -> Unit)? = null)
}

val LocalAppActions = staticCompositionLocalOf<AppActions> {
    object : AppActions {
        override fun openAdd(request: AddRequest) = Unit
        override fun snack(text: String, action: String?, onAction: (() -> Unit)?) = Unit
    }
}
