package com.securevision.core.model

/**
 * The signed-in **app login account**.
 *
 * Stored in the local Room database with a BCrypt password hash. Nothing about
 * this account is uploaded — an account therefore does **not** survive an
 * uninstall, which is the deliberate trade for a product that promises no data
 * leaves the phone. The recovery code exists because of it.
 *
 * Entirely separate from [EnrolledProfile], which represents a person the app can
 * recognise.
 *
 * @property uid Locally generated account id; stable for the account's lifetime.
 * @property username Unique handle chosen at sign-up.
 * @property fullName Account holder's display name.
 * @property cnic National identity number captured at sign-up.
 * @property createdAt Account creation time, epoch milliseconds UTC.
 */
data class UserAccount(
    val uid: String,
    val username: String,
    val fullName: String,
    val cnic: String,
    val createdAt: Long,
)
