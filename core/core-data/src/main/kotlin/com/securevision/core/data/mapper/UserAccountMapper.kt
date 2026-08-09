package com.securevision.core.data.mapper

import com.securevision.core.data.database.entity.UserAccountEntity
import com.securevision.core.model.UserAccount

/**
 * Projects a stored account onto its domain model.
 *
 * Deliberately lossy: `passwordHash` and `recoveryCodeHash` have no counterpart
 * on [UserAccount] and are dropped here. That is the mechanism by which no
 * password material can reach a ViewModel or a Composable — not a convention to
 * remember, but a type that has nowhere to put it.
 *
 * There is intentionally no `UserAccount.toEntity()`: building a row requires
 * both hashes, so only the repository, which can compute them, may construct one.
 */
fun UserAccountEntity.toDomain(): UserAccount = UserAccount(
    uid = uid,
    username = username,
    fullName = fullName,
    cnic = cnic,
    createdAt = createdAt,
)
