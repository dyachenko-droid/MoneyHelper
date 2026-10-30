package ru.denpom.finance.app.presentation.operations

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.presentation.components.CategoryIcons
import ru.denpom.finance.app.presentation.format.MoneyFormat
import ru.denpom.finance.app.presentation.theme.App

data class OperationVisual(
    val title: String,
    val subtitle: String,
    val amount: String,
    val color: Color,
    val hue: Float,
    val icon: ImageVector,
)

@Composable
fun operationVisuals(op: Operation, categories: Map<Long, Category>, accounts: Map<Long, String>, fmt: MoneyFormat): OperationVisual {
    val c = App.colors
    val cat = op.categoryId?.let { categories[it] }
    val account = accounts[op.accountId].orEmpty()
    return when (op.type) {
        OperationType.TRANSFER -> OperationVisual(
            title = op.title.ifBlank { "Перевод" },
            subtitle = listOf("$account → ${accounts[op.toAccountId].orEmpty()}", op.note).filter { it.isNotBlank() }.joinToString(" · "),
            amount = fmt.format(op.amount),
            color = c.on,
            hue = 225f,
            icon = CategoryIcons.of("swap_horiz"),
        )
        else -> {
            val extra = when {
                op.note.isNotBlank() -> op.note
                op.splits.size > 1 -> "${op.splits.size} ${ru.denpom.finance.app.presentation.format.Dates.plural(op.splits.size.toLong(), "категория", "категории", "категорий")}"
                else -> ""
            }
            OperationVisual(
                title = op.title.ifBlank { cat?.name ?: "Без категории" },
                subtitle = listOf(account, extra).filter { it.isNotBlank() }.joinToString(" · "),
                amount = fmt.signed(op.signedAmount),
                color = if (op.type == OperationType.INCOME) c.inc else c.exp,
                hue = cat?.hue ?: 165f,
                icon = CategoryIcons.of(cat?.icon),
            )
        }
    }
}
