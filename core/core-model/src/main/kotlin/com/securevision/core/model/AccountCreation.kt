package com.securevision.core.model

/**
 * The result of creating the app-login account.
 *
 * Carries the [recoveryCode] in plaintext because this is the **only** moment it
 * can ever be shown. The account store keeps a BCrypt hash of it and nothing
 * else, so once this object is discarded the code cannot be recovered from the
 * device by anyone — including the app itself.
 *
 * That matters because the account is offline-only: without a recovery code, a
 * forgotten password would leave no way back in short of clearing app data,
 * which would also destroy every enrolled person profile.
 *
 * @property account The newly created account.
 * @property recoveryCode Twelve characters, grouped for legibility. Show it once,
 *   tell the user to write it down, and never log it.
 */
data class AccountCreation(
    val account: UserAccount,
    val recoveryCode: String,
) {
    /** Deliberately omits [recoveryCode] so it cannot reach a log or crash report. */
    override fun toString(): String = "AccountCreation(account=$account, recoveryCode=<redacted>)"
}
