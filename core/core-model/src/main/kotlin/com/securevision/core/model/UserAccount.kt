package com.securevision.core.model

/**
 * The signed-in **app login account**.
 *
 * This is the only entity in SecureVision that is backed by Firebase (Auth for
 * credentials, Firestore for the profile fields) so that it survives an app
 * reinstall. It is entirely separate from [EnrolledProfile], which represents a
 * person the app can recognise and never leaves the device.
 *
 * @property uid Firebase Auth user id; stable for the lifetime of the account.
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
