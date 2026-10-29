package com.marriagebureau.ai.data.repository

import com.marriagebureau.ai.data.db.dao.ProfileDao
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.data.db.entity.ProfileStatus
import kotlinx.coroutines.flow.Flow

data class ProfileSearchCriteria(
    val gender: String? = null,
    val minAge: Int? = null,
    val maxAge: Int? = null,
    val city: String? = null,
    val maritalStatus: String? = null,
    val education: String? = null,
    val occupation: String? = null,
    val freeText: String? = null
)

interface ProfileRepository {
    suspend fun addProfile(profile: MarriageProfileEntity): Long
    suspend fun addProfilesBulk(profiles: List<MarriageProfileEntity>): List<Long>
    suspend fun updateProfile(profile: MarriageProfileEntity)
    suspend fun getProfileById(id: Long): MarriageProfileEntity?
    suspend fun getProfileByCode(code: String): MarriageProfileEntity?
    suspend fun searchProfiles(criteria: ProfileSearchCriteria, limit: Int = 50, offset: Int = 0): List<MarriageProfileEntity>
    suspend fun freeTextSearch(query: String, limit: Int = 50, offset: Int = 0): List<MarriageProfileEntity>
    suspend fun getActiveProfilesPaged(limit: Int = 50, offset: Int = 0): List<MarriageProfileEntity>
    fun getAllActiveProfilesFlow(): Flow<List<MarriageProfileEntity>>
    suspend fun findDuplicates(profile: MarriageProfileEntity): List<MarriageProfileEntity>
    suspend fun setProfileStatus(profileId: Long, status: ProfileStatus)
    suspend fun deleteProfile(profileId: Long)
    suspend fun getActiveCount(): Int
    suspend fun getTotalCount(): Int
    suspend fun deleteAllProfiles()
}

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao
) : ProfileRepository {

    override suspend fun addProfile(profile: MarriageProfileEntity): Long {
        return profileDao.insertProfile(profile)
    }

    override suspend fun addProfilesBulk(profiles: List<MarriageProfileEntity>): List<Long> {
        return profileDao.insertProfiles(profiles)
    }

    override suspend fun updateProfile(profile: MarriageProfileEntity) {
        profileDao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun getProfileById(id: Long): MarriageProfileEntity? {
        return profileDao.getProfileById(id)
    }

    override suspend fun getProfileByCode(code: String): MarriageProfileEntity? {
        return profileDao.getProfileByCode(code)
    }

    override suspend fun searchProfiles(
        criteria: ProfileSearchCriteria,
        limit: Int,
        offset: Int
    ): List<MarriageProfileEntity> {
        val freeText = criteria.freeText
        if (!freeText.isNullOrBlank()) {
            val freeResults = profileDao.freeTextSearch(freeText, limit = limit * 2, offset = offset)
            return freeResults.filter { profile ->
                (criteria.gender == null || profile.gender.equals(criteria.gender, ignoreCase = true)) &&
                (criteria.minAge == null || profile.age >= criteria.minAge) &&
                (criteria.maxAge == null || profile.age <= criteria.maxAge) &&
                (criteria.city == null || profile.city.contains(criteria.city, ignoreCase = true)) &&
                (criteria.maritalStatus == null || profile.maritalStatus.equals(criteria.maritalStatus, ignoreCase = true))
            }.take(limit)
        }

        return profileDao.searchProfiles(
            gender = criteria.gender,
            minAge = criteria.minAge,
            maxAge = criteria.maxAge,
            city = criteria.city,
            maritalStatus = criteria.maritalStatus,
            education = criteria.education,
            occupation = criteria.occupation,
            limit = limit,
            offset = offset
        )
    }

    override suspend fun freeTextSearch(query: String, limit: Int, offset: Int): List<MarriageProfileEntity> {
        return profileDao.freeTextSearch(query, limit, offset)
    }

    override suspend fun getActiveProfilesPaged(limit: Int, offset: Int): List<MarriageProfileEntity> {
        return profileDao.getActiveProfilesPaged(limit, offset)
    }

    override fun getAllActiveProfilesFlow(): Flow<List<MarriageProfileEntity>> {
        return profileDao.getAllActiveProfilesFlow()
    }

    override suspend fun findDuplicates(profile: MarriageProfileEntity): List<MarriageProfileEntity> {
        return profileDao.findDuplicates(
            code = profile.profileCode,
            name = profile.name,
            city = profile.city,
            age = profile.age,
            contact = profile.contactInformation
        )
    }

    override suspend fun setProfileStatus(profileId: Long, status: ProfileStatus) {
        profileDao.updateProfileStatus(profileId, status)
    }

    override suspend fun deleteProfile(profileId: Long) {
        profileDao.deleteProfile(profileId)
    }

    override suspend fun getActiveCount(): Int {
        return profileDao.getActiveProfileCount()
    }

    override suspend fun getTotalCount(): Int {
        return profileDao.getTotalProfileCount()
    }

    override suspend fun deleteAllProfiles() {
        profileDao.deleteAllProfiles()
    }
}
