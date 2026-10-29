package com.marriagebureau.ai.data

import com.marriagebureau.ai.data.db.dao.ProfileDao
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.data.db.entity.ProfileStatus
import com.marriagebureau.ai.data.importexport.ProfileBenchmark
import com.marriagebureau.ai.data.importexport.ProfileImporter
import com.marriagebureau.ai.data.repository.ProfileRepositoryImpl
import com.marriagebureau.ai.data.repository.ProfileSearchCriteria
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ProfileImportAndBenchmarkTest {

    private class InMemDao : ProfileDao {
        val list = ArrayList<MarriageProfileEntity>()
        var counter = 1L

        override suspend fun insertProfile(profile: MarriageProfileEntity): Long {
            val id = if (profile.profileId == 0L) counter++ else profile.profileId
            list.add(profile.copy(profileId = id))
            return id
        }

        override suspend fun insertProfiles(profiles: List<MarriageProfileEntity>): List<Long> {
            return profiles.map { insertProfile(it) }
        }

        override suspend fun updateProfile(profile: MarriageProfileEntity) {
            val idx = list.indexOfFirst { it.profileId == profile.profileId }
            if (idx >= 0) list[idx] = profile
        }

        override suspend fun getProfileById(id: Long): MarriageProfileEntity? {
            return list.firstOrNull { it.profileId == id }
        }

        override suspend fun getProfileByCode(code: String): MarriageProfileEntity? {
            return list.firstOrNull { it.profileCode.equals(code, ignoreCase = true) }
        }

        override suspend fun getActiveProfilesPaged(limit: Int, offset: Int): List<MarriageProfileEntity> {
            return list.filter { it.status == ProfileStatus.ACTIVE }.drop(offset).take(limit)
        }

        override fun getAllActiveProfilesFlow(): kotlinx.coroutines.flow.Flow<List<MarriageProfileEntity>> {
            return kotlinx.coroutines.flow.flowOf(list.filter { it.status == ProfileStatus.ACTIVE })
        }

        override suspend fun getActiveProfileCount(): Int {
            return list.count { it.status == ProfileStatus.ACTIVE }
        }

        override suspend fun getTotalProfileCount(): Int = list.size

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
            return list.filter { p ->
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

        override suspend fun freeTextSearch(query: String, limit: Int, offset: Int): List<MarriageProfileEntity> {
            val q = query.lowercase()
            return list.filter { p ->
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
            return list.filter { p ->
                p.status == ProfileStatus.ACTIVE && (
                    p.profileCode.equals(code, ignoreCase = true) ||
                    (p.name.equals(name, ignoreCase = true) && p.city.equals(city, ignoreCase = true) && p.age == age) ||
                    (!contact.isNullOrBlank() && p.contactInformation == contact)
                )
            }
        }

        override suspend fun updateProfileStatus(profileId: Long, status: ProfileStatus, updatedAt: Long) {
            val idx = list.indexOfFirst { it.profileId == profileId }
            if (idx >= 0) list[idx] = list[idx].copy(status = status, updatedAt = updatedAt)
        }

        override suspend fun deleteProfile(profileId: Long) {
            list.removeAll { it.profileId == profileId }
        }

        override suspend fun deleteAllProfiles() {
            list.clear()
        }
    }

    private lateinit var dao: InMemDao
    private lateinit var repository: ProfileRepositoryImpl
    private lateinit var importer: ProfileImporter

    @Before
    fun setUp() {
        dao = InMemDao()
        repository = ProfileRepositoryImpl(dao)
        importer = ProfileImporter(repository)
    }

    @Test
    fun testCsvImportAndRollback() = runBlocking {
        val csv = """
            code,name,gender,age,city,education,occupation,contact
            CSV101,Aarti Singh,FEMALE,26,Gorakhpur,B.Tech,Teacher,9988776655
            CSV102,Sanjay Kumar,MALE,29,Lucknow,MBA,Bank Manager,9988776644
        """.trimIndent()

        val preview = importer.parseAndPreviewCsv(csv)
        assertEquals(2, preview.totalRecordsFound)
        assertEquals(2, preview.validCount)
        assertEquals(0, preview.duplicateCount)

        val result = importer.executeImport(preview)
        assertEquals(2, result.importedCount)
        assertEquals(2, repository.getActiveCount())

        val rolledBack = importer.rollbackImport(result.importedIds)
        assertTrue(rolledBack)
        assertEquals(0, repository.getActiveCount())
    }

    @Test
    fun testJsonImportWithValidation() = runBlocking {
        val json = """
            [
              {"profileCode": "J1", "name": "Deepika", "gender": "FEMALE", "age": 24, "city": "Gorakhpur", "education": "B.Sc"},
              {"profileCode": "J2", "name": "", "gender": "FEMALE", "age": 0, "city": "", "education": "N/A"}
            ]
        """.trimIndent()

        val preview = importer.parseAndPreviewJson(json)
        assertEquals(2, preview.totalRecordsFound)
        assertEquals(1, preview.validCount)
        assertEquals(1, preview.invalidCount)

        val result = importer.executeImport(preview)
        assertEquals(1, result.importedCount)
        assertEquals(1, repository.getActiveCount())
    }

    @Test
    fun test10kProfileBenchmark() = runBlocking {
        val benchmark = ProfileBenchmark(repository)
        val metrics = benchmark.runBenchmark(10000)

        assertEquals(10000, metrics.profileCount)
        assertEquals(10000, repository.getActiveCount())
        assertTrue(metrics.searchPassed)
        assertTrue(metrics.averageQueryTimeMs < 1000.0)
    }
}
