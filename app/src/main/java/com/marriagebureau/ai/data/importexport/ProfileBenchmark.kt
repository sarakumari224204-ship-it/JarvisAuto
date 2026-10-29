package com.marriagebureau.ai.data.importexport

import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.data.db.entity.ProfileStatus
import com.marriagebureau.ai.data.repository.ProfileRepository
import com.marriagebureau.ai.data.repository.ProfileSearchCriteria
import kotlin.random.Random
import kotlin.system.measureTimeMillis

data class BenchmarkMetrics(
    val profileCount: Int,
    val bulkInsertTimeMs: Long,
    val singleCriteriaSearchMs: Long,
    val multiCriteriaSearchMs: Long,
    val freeTextSearchMs: Long,
    val averageQueryTimeMs: Double,
    val searchPassed: Boolean
)

class ProfileBenchmark(
    private val profileRepository: ProfileRepository
) {

    private val sampleNamesMale = listOf("Rahul", "Amit", "Vikram", "Suresh", "Rajesh", "Ankit", "Deepak", "Rohan", "Siddharth", "Gaurav")
    private val sampleNamesFemale = listOf("Priya", "Anjali", "Neha", "Pooja", "Kavita", "Sneha", "Shweta", "Ritu", "Divya", "Swati")
    private val sampleCities = listOf("Gorakhpur", "Lucknow", "Varanasi", "Kanpur", "Delhi", "Noida", "Agra", "Prayagraj", "Patna", "Jaipur")
    private val sampleEducations = listOf("B.Tech", "MBA", "MBBS", "B.Com", "M.Tech", "B.Sc", "Ph.D", "BCA", "MCA", "CA")
    private val sampleOccupations = listOf("Software Engineer", "Doctor", "Teacher", "Bank Manager", "Civil Engineer", "Business", "Architect", "Data Analyst")

    fun generateMockProfiles(count: Int, startCodeIndex: Int = 1): List<MarriageProfileEntity> {
        val list = ArrayList<MarriageProfileEntity>(count)
        val random = Random(42) // Fixed seed for reproducible benchmarks

        for (i in 0 until count) {
            val isMale = random.nextBoolean()
            val gender = if (isMale) "MALE" else "FEMALE"
            val name = if (isMale) sampleNamesMale[random.nextInt(sampleNamesMale.size)] else sampleNamesFemale[random.nextInt(sampleNamesFemale.size)]
            val city = sampleCities[random.nextInt(sampleCities.size)]
            val edu = sampleEducations[random.nextInt(sampleEducations.size)]
            val occ = sampleOccupations[random.nextInt(sampleOccupations.size)]
            val age = random.nextInt(21, 45)
            val code = "MB-PERF-${startCodeIndex + i}"

            list.add(
                MarriageProfileEntity(
                    profileCode = code,
                    name = "$name ${startCodeIndex + i}",
                    gender = gender,
                    age = age,
                    city = city,
                    state = "Uttar Pradesh",
                    education = edu,
                    occupation = occ,
                    incomeRange = "5-10 LPA",
                    maritalStatus = "NEVER_MARRIED",
                    profileDescription = "$gender $age years old $occ from $city looking for suitable match.",
                    source = "SEED_BENCHMARK",
                    status = ProfileStatus.ACTIVE
                )
            )
        }
        return list
    }

    suspend fun runBenchmark(targetCount: Int): BenchmarkMetrics {
        val mockData = generateMockProfiles(targetCount)

        val insertTime = measureTimeMillis {
            profileRepository.addProfilesBulk(mockData)
        }

        val singleTime = measureTimeMillis {
            val criteria = ProfileSearchCriteria(city = "Gorakhpur", gender = "FEMALE")
            profileRepository.searchProfiles(criteria, limit = 50)
        }

        val multiTime = measureTimeMillis {
            val criteria = ProfileSearchCriteria(
                gender = "FEMALE",
                minAge = 22,
                maxAge = 28,
                city = "Lucknow",
                education = "B.Tech"
            )
            profileRepository.searchProfiles(criteria, limit = 50)
        }

        val freeTextTime = measureTimeMillis {
            profileRepository.freeTextSearch("Software Engineer", limit = 50)
        }

        val avgTime = (singleTime + multiTime + freeTextTime) / 3.0
        val searchPassed = avgTime < 1000.0 // Under 1 second query target for 6GB phone

        return BenchmarkMetrics(
            profileCount = targetCount,
            bulkInsertTimeMs = insertTime,
            singleCriteriaSearchMs = singleTime,
            multiCriteriaSearchMs = multiTime,
            freeTextSearchMs = freeTextTime,
            averageQueryTimeMs = avgTime,
            searchPassed = searchPassed
        )
    }
}
