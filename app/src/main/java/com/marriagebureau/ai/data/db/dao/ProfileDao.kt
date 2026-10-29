package com.marriagebureau.ai.data.db.dao

import androidx.room.*
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.data.db.entity.ProfileStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: MarriageProfileEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<MarriageProfileEntity>): List<Long>

    @Update
    suspend fun updateProfile(profile: MarriageProfileEntity)

    @Query("SELECT * FROM marriage_profiles WHERE profileId = :id")
    suspend fun getProfileById(id: Long): MarriageProfileEntity?

    @Query("SELECT * FROM marriage_profiles WHERE profileCode = :code LIMIT 1")
    suspend fun getProfileByCode(code: String): MarriageProfileEntity?

    @Query("SELECT * FROM marriage_profiles WHERE status = 'ACTIVE' ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActiveProfilesPaged(limit: Int, offset: Int): List<MarriageProfileEntity>

    @Query("SELECT * FROM marriage_profiles WHERE status = 'ACTIVE' ORDER BY updatedAt DESC")
    fun getAllActiveProfilesFlow(): Flow<List<MarriageProfileEntity>>

    @Query("SELECT COUNT(*) FROM marriage_profiles WHERE status = 'ACTIVE'")
    suspend fun getActiveProfileCount(): Int

    @Query("SELECT COUNT(*) FROM marriage_profiles")
    suspend fun getTotalProfileCount(): Int

    @Query("""
        SELECT * FROM marriage_profiles
        WHERE status = 'ACTIVE'
        AND (:gender IS NULL OR UPPER(gender) = UPPER(:gender))
        AND (:minAge IS NULL OR age >= :minAge)
        AND (:maxAge IS NULL OR age <= :maxAge)
        AND (:city IS NULL OR LOWER(city) LIKE LOWER('%' || :city || '%'))
        AND (:maritalStatus IS NULL OR LOWER(maritalStatus) = LOWER(:maritalStatus))
        AND (:education IS NULL OR LOWER(education) LIKE LOWER('%' || :education || '%'))
        AND (:occupation IS NULL OR LOWER(occupation) LIKE LOWER('%' || :occupation || '%'))
        ORDER BY age ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchProfiles(
        gender: String? = null,
        minAge: Int? = null,
        maxAge: Int? = null,
        city: String? = null,
        maritalStatus: String? = null,
        education: String? = null,
        occupation: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<MarriageProfileEntity>

    @Query("""
        SELECT * FROM marriage_profiles
        WHERE status = 'ACTIVE'
        AND (
            LOWER(profileCode) LIKE LOWER('%' || :query || '%') OR
            LOWER(name) LIKE LOWER('%' || :query || '%') OR
            LOWER(city) LIKE LOWER('%' || :query || '%') OR
            LOWER(education) LIKE LOWER('%' || :query || '%') OR
            LOWER(occupation) LIKE LOWER('%' || :query || '%') OR
            LOWER(profileDescription) LIKE LOWER('%' || :query || '%')
        )
        ORDER BY updatedAt DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun freeTextSearch(query: String, limit: Int = 50, offset: Int = 0): List<MarriageProfileEntity>

    @Query("""
        SELECT * FROM marriage_profiles
        WHERE status = 'ACTIVE'
        AND (
            LOWER(profileCode) = LOWER(:code)
            OR (LOWER(name) = LOWER(:name) AND LOWER(city) = LOWER(:city) AND age = :age)
            OR (contactInformation IS NOT NULL AND contactInformation = :contact AND contactInformation != '')
        )
        LIMIT 10
    """)
    suspend fun findDuplicates(code: String, name: String, city: String, age: Int, contact: String?): List<MarriageProfileEntity>

    @Query("UPDATE marriage_profiles SET status = :status, updatedAt = :updatedAt WHERE profileId = :profileId")
    suspend fun updateProfileStatus(profileId: Long, status: ProfileStatus, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM marriage_profiles WHERE profileId = :profileId")
    suspend fun deleteProfile(profileId: Long)

    @Query("DELETE FROM marriage_profiles")
    suspend fun deleteAllProfiles()
}
