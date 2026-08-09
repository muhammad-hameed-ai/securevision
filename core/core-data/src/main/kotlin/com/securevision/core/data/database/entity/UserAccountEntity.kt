package com.securevision.core.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for the app-login account.
 *
 * SecureVision is single-operator, so this table holds at most one row. The
 * unique index on [username] is kept anyway, because it is the constraint that
 * would matter the moment that assumption changes.
 *
 * Neither the password nor the recovery code is stored — only their BCrypt
 * hashes. Neither hash appears on the domain model, so no plaintext and no hash
 * can reach the presentation layer.
 *
 * @property uid Locally generated UUID; the session stores this and nothing else.
 * @property username Unique handle chosen at sign-up.
 * @property fullName Account holder's display name.
 * @property cnic National identity number, normalised to thirteen bare digits.
 * @property passwordHash BCrypt hash of the password.
 * @property recoveryCodeHash BCrypt hash of the normalised recovery code.
 * @property createdAt Account creation time, epoch milliseconds UTC.
 */
@Entity(
    tableName = UserAccountEntity.TABLE_NAME,
    indices = [Index(value = ["username"], unique = true)],
)
data class UserAccountEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "username")
    val username: String,

    @ColumnInfo(name = "full_name")
    val fullName: String,

    @ColumnInfo(name = "cnic")
    val cnic: String,

    @ColumnInfo(name = "password_hash")
    val passwordHash: String,

    @ColumnInfo(name = "recovery_code_hash")
    val recoveryCodeHash: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {

    /**
     * Redacts both hashes.
     *
     * A BCrypt hash is not a password, but it is offline-crackable material and
     * has no business appearing in a log line or a crash report.
     */
    override fun toString(): String =
        "UserAccountEntity(uid=$uid, username=$username, fullName=$fullName, " +
            "cnic=$cnic, passwordHash=<redacted>, recoveryCodeHash=<redacted>, " +
            "createdAt=$createdAt)"

    companion object {
        const val TABLE_NAME = "user_accounts"
    }
}
