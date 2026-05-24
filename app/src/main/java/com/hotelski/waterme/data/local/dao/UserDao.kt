package com.hotelski.waterme.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.hotelski.waterme.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUser(userId: String): UserEntity?

    @Upsert
    suspend fun upsertUser(user: UserEntity)
}
