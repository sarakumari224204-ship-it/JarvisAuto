package com.marriagebureau.ai.data

import com.marriagebureau.ai.data.db.dao.ProfileDao
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.data.db.entity.ProfileStatus
import com.marriagebureau.ai.data.repository.ProfileRepositoryImpl
import com.marriagebureau.ai.data.repository.ProfileSearchCriteria
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MarriageProfileTest {

    private class FakeProfileDao : ProfileDao {
        private val storage = mutableMapOf<Long, MarriageProfileEntity>()
        private var idCounter = 1L

        override suspend fun insertProfile(profile: MarriageProfileEntity): Long {
            val id = if (profile.profileId == 0L) idCounter++ else profile.profileId
            val saved = profile.copy(profileId = id)
            storage[id] = saved
            return id
        }

        override suspend fun insertProfiles(profiles: List<MarriageProfileEntity>): List<Long> {
            return profiles.map { insertProfile(it) }
        }

        override suspend fun updateProfile(profile: MarriageProfileEntity) {
            storage[profile.profileId] = profile
        }

        override suspend fun getProfileById(id: Long): MarriageProfileEntity? = storage[id]

        override suspend fun getProfileByCode(code: String): MarriageProfileEntity? {
            return storage.values.firstOrNull { it.profileCode.equals(code, ignoreCase = true) }
        }

        override suspend fun getActiveProfilesPaged(limit: Int, offset: Int): List<MarriageProfileEntity> {
            return storage.values
                .filter { it.status == ProfileStatus.ACTIVE }
                .drop(offset)
                .take(limit)
        }

        override fun getAllActiveProfilesFlow(): kotlinx.coroutines.flow.Flow<List<MarriageProfileEntity>> {
            return kotlinx.coroutines.flow.flowOf(storage.values.filter { it.status == ProfileStatus.ACTIVE })
        }

        override suspend fun getActiveProfileCount(): Int {
            return storage.values.count { it.status == ProfileStatus.ACTIVE }
        }

        override suspend fun getTotalProfileCount(): Int {
            return storage.size
        }

        override suspend fun searchProfiles(
            gender: String?,
            minAge: Int?,
            maxAge: Int?,
            city: String?,
            maritalStatus: String?,
            education: String?,
            occupation: String?,
            limit: Int,
            offset: Int
        ): List<MarriageProfileEntity> {
            return storage.values.filter { p ->
                p.status == ProfileStatus.ACTIVE &&
                (gender == null || p.gender.equals(gender, ignoreCase = true)) &&
                (minAge == null || p.age >= minAge) &&
                (maxAge == null || p.age <= maxAge) &&
                (city == null || p.city.contains(city, ignoreCase = true)) &&
                (maritalStatus == null || p.maritalStatus.equals(maritalStatus, ignoreCase = true)) &&
                (education == null || p.education.contains(education, ignoreCase = true)) &&
                (occupation == null || p.occupation.contains(occupation, ignoreCase = true))
            }.drop(offset).take(limit)
        }

        override suspend fun freeTextSearch(
            query: String,
            limit: Int,
            offset: Int
        ): List<MarriageProfileEntity> {
            val q = query.lowercase()
            return storage.values.filter { p ->
                p.status == ProfileStatus.ACTIVE && (
                    p.profileCode.lowercase().contains(q) ||
                    p.name.lowercase().contains(q) ||
                    p.city.lowercase().contains(q) ||
                    p.education.lowercase().contains(q) ||
                    p.occupation.lowercase().contains(q) ||
                    p.profileDescription.lowercase().contains(q)
                )
            }.drop(offset).take(limit)
        }

        override suspend fun findDuplicates(
            code: String,
            name: String,
            city: String,
            age: Int,
            contact: String?
        ): List<MarriageProfileEntity> {
            return storage.values.filter { p ->
                p.status == ProfileStatus.ACTIVE && (
                    p.profileCode.equals(code, ignoreCase = true) ||
                    (p.name.equals(name, ignoreCase = true) && p.city.equals(city, ignoreCase = true) && p.age == age) ||
                    (!contact.isNullOrBlank() && p.contactInformation == contact)
                )
            }
        }

        override suspend fun updateProfileStatus(profileId: Long, status: ProfileStatus, updatedAt: Long) {
            storage[profileId]?.let {
                storage[profileId] = it.copy(status = status, updatedAt = updatedAt)
            }
        }

        override suspend fun deleteProfile(profileId: Long) {
            storage.remove(profileId)
        }

        override suspend fun deleteAllProfiles() {
            storage.clear()
        }
    }

    private lateinit var repository: ProfileRepositoryImpl
    private lateinit var fakeDao: FakeProfileDao

    @Before
    fun setUp() {
        fakeDao = FakeProfileDao()
        repository = ProfileRepositoryImpl(fakeDao)
    }

    @Test
    fun testAddAndRetrieveProfile() = runBlocking {
        val profile = MarriageProfileEntity(
            profileCode = "MB101",
            name = "Priya Sharma",
            gender = "FEMALE",
            age = 25,
            city = "Gorakhpur",
            education = "B.Tech",
            occupation = "Software Engineer"
        )
        val id = repository.addProfile(profile)
        assertTrue(id > 0)

        val retrieved = repository.getProfileById(id)
        assertNotNull(retrieved)
        assertEquals("MB101", retrieved?.profileCode)
        assertEquals("Gorakhpur", retrieved?.city)
    }

    @Test
    fun testSearchProfilesByCriteria() = runBlocking {
        repository.addProfile(
            MarriageProfileEntity(profileCode = "MB101", name = "Priya", gender = "FEMALE", age = 25, city = "Gorakhpur", education = "B.Tech")
        )
        repository.addProfile(
            MarriageProfileEntity(profileCode = "MB102", name = "Rahul", gender = "MALE", age = 28, city = "Lucknow", education = "MBA")
        )
        repository.addProfile(
            MarriageProfileEntity(profileCode = "MB103", name = "Sneha", gender = "FEMALE", age = 27, city = "Gorakhpur", education = "MBBS")
        )

        val criteria = ProfileSearchCriteria(
            gender = "FEMALE",
            minAge = 24,
            maxAge = 26,
            city = "Gorakhpur"
        )
        val results = repository.searchProfiles(criteria)
        assertEquals(1, results.size)
        assertEquals("MB101", results[0].profileCode)
    }

    @Test
    fun testDuplicateDetection() = runBlocking {
        val p1 = MarriageProfileEntity(profileCode = "MB201", name = "Ankit Verma", gender = "MALE", age = 29, city = "Delhi", contactInformation = "9876543210")
        repository.addProfile(p1)

        val p2 = MarriageProfileEntity(profileCode = "MB202", name = "Ankit Verma", gender = "MALE", age = 29, city = "Delhi", contactInformation = "9876543210")
        val duplicates = repository.findDuplicates(p2)

        assertFalse(duplicates.isEmpty())
        assertEquals("MB201", duplicates[0].profileCode)
    }
}
