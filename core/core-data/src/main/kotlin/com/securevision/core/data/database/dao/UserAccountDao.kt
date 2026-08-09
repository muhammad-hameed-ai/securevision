package com.securevision.core.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.securevision.core.data.database.entity.UserAccountEntity
import kotlinx.coroutines.flow.Flow

/** Reads and writes for the single-row app-login account table. */
@Dao
interface UserAccountDao {

    /**
     * Inserts or replaces the account, keyed on its uid.
     *
     * `REPLACE` is what makes a password reset a single write rather than a
     * read-modify-delete-insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: UserAccountEntity)

    /**
     * @param username Handle to look up.
     * @return The account, or `null` if no account has that username.
     */
    @Query("SELECT * FROM user_accounts WHERE username = :username LIMIT 1")
    suspend fun getByUsername(username: String): UserAccountEntity?

    /** The account, or `null` if none has been created. */
    @Query("SELECT * FROM user_accounts LIMIT 1")
    suspend fun getAccount(): UserAccountEntity?

    /** The account as a stream, so the launch gate reacts to it being created. */
    @Query("SELECT * FROM user_accounts LIMIT 1")
    fun observeAccount(): Flow<UserAccountEntity?>

    /** How many accounts exist. Used to enforce the single-operator rule. */
    @Query("SELECT COUNT(*) FROM user_accounts")
    suspend fun count(): Int

    /** @param uid Account to remove. */
    @Query("DELETE FROM user_accounts WHERE uid = :uid")
    suspend fun delete(uid: String)
}
