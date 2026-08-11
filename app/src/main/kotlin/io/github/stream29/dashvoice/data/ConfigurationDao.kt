package io.github.stream29.dashvoice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConfigurationDao {
    @Query("SELECT * FROM configuration WHERE singleton_id = 1")
    suspend fun load(): ConfigurationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(configuration: ConfigurationEntity)
}
