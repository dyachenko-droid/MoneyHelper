package ru.denpom.finance.app.data.repository

import ru.denpom.finance.app.data.db.AccountEntity
import ru.denpom.finance.app.data.db.CategoryEntity
import ru.denpom.finance.app.data.db.OperationEntity
import ru.denpom.finance.app.data.db.OperationWithSearch
import ru.denpom.finance.app.data.db.ReceiptEntity
import ru.denpom.finance.app.data.db.ReceiptItemEntity
import ru.denpom.finance.app.data.db.RecurringEntity
import ru.denpom.finance.app.data.db.SplitEntity
import ru.denpom.finance.app.domain.model.Account
import ru.denpom.finance.app.domain.model.AccountType
import ru.denpom.finance.app.domain.model.Category
import ru.denpom.finance.app.domain.model.CategoryKind
import ru.denpom.finance.app.domain.model.Operation
import ru.denpom.finance.app.domain.model.OperationType
import ru.denpom.finance.app.domain.model.Receipt
import ru.denpom.finance.app.domain.model.ReceiptItem
import ru.denpom.finance.app.domain.model.ReceiptStatus
import ru.denpom.finance.app.domain.model.RecurrencePeriod
import ru.denpom.finance.app.domain.model.Recurring
import ru.denpom.finance.app.domain.model.Split

fun AccountEntity.toDomain(balance: Long) = Account(
    id = id,
    name = name,
    type = AccountType.valueOf(type),
    subtitle = subtitle,
    openingBalance = openingBalance,
    creditLimit = creditLimit,
    balance = balance,
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    type = type.name,
    subtitle = subtitle,
    openingBalance = openingBalance,
    creditLimit = creditLimit,
)

fun CategoryEntity.toDomain() = Category(id, name, icon, hue, CategoryKind.valueOf(kind), sort)

fun Category.toEntity() = CategoryEntity(id, name, icon, hue, kind.name, sort)

fun OperationWithSearch.toDomain(splits: List<SplitEntity>) = Operation(
    id = id,
    type = OperationType.valueOf(type),
    amount = amount,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    title = title,
    note = note,
    timestamp = timestamp,
    receiptId = receiptId,
    splits = splits.map { Split(it.categoryId, it.amount) },
    searchText = items.orEmpty(),
)

fun OperationEntity.toDomain(splits: List<SplitEntity>) = Operation(
    id = id,
    type = OperationType.valueOf(type),
    amount = amount,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    title = title,
    note = note,
    timestamp = timestamp,
    receiptId = receiptId,
    splits = splits.map { Split(it.categoryId, it.amount) },
)

fun Operation.toEntity() = OperationEntity(
    id = id,
    type = type.name,
    amount = amount,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    title = title,
    note = note,
    timestamp = timestamp,
    receiptId = receiptId,
)

fun ReceiptEntity.toDomain(items: List<ReceiptItemEntity>, operationId: Long?) = Receipt(
    id = id,
    shop = shop,
    address = address,
    timestamp = timestamp,
    total = total,
    qrRaw = qrRaw,
    status = ReceiptStatus.valueOf(status),
    photoPath = photoPath,
    warrantyUntil = warrantyUntil,
    items = items.map { ReceiptItem(it.id, it.name, it.quantity, it.unit, it.price, it.sum, it.categoryId) },
    operationId = operationId,
)

fun Receipt.toEntity() = ReceiptEntity(
    id = id,
    shop = shop,
    address = address,
    timestamp = timestamp,
    total = total,
    qrRaw = qrRaw,
    status = status.name,
    photoPath = photoPath,
    warrantyUntil = warrantyUntil,
)

fun ReceiptItem.toEntity(position: Int) = ReceiptItemEntity(
    id = 0,
    receiptId = 0,
    position = position,
    name = name,
    quantity = quantity,
    unit = unit,
    price = price,
    sum = sum,
    categoryId = categoryId,
)

fun RecurringEntity.toDomain() = Recurring(
    id = id,
    title = title,
    amount = amount,
    type = OperationType.valueOf(type),
    accountId = accountId,
    categoryId = categoryId,
    period = RecurrencePeriod.valueOf(period),
    nextDate = nextDate,
    isSubscription = isSubscription,
    autoCreate = autoCreate,
)

fun Recurring.toEntity() = RecurringEntity(
    id = id,
    title = title,
    amount = amount,
    type = type.name,
    accountId = accountId,
    categoryId = categoryId,
    period = period.name,
    nextDate = nextDate,
    isSubscription = isSubscription,
    autoCreate = autoCreate,
)
