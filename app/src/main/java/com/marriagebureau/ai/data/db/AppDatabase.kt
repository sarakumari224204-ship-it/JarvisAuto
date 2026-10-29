package com.marriagebureau.ai.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.marriagebureau.ai.data.db.dao.CustomerDao
import com.marriagebureau.ai.data.db.dao.ProfileDao
import com.marriagebureau.ai.data.db.entity.ConversationMessageEntity
import com.marriagebureau.ai.data.db.entity.CustomerEntity
import com.marriagebureau.ai.data.db.entity.CustomerRequirementEntity
import com.marriagebureau.ai.data.db.entity.MarriageProfileEntity
import com.marriagebureau.ai.knowledge.dao.KnowledgeDao
import com.marriagebureau.ai.knowledge.entity.KnowledgeChunkEntity
import com.marriagebureau.ai.knowledge.entity.KnowledgeDocumentEntity
import com.marriagebureau.ai.knowledge.entity.WorkflowStepEntity
import com.marriagebureau.ai.service.QueueDao
import com.marriagebureau.ai.service.QueueMessageEntity
import com.marriagebureau.ai.teach.dao.TeachDao
import com.marriagebureau.ai.teach.entity.AgentIdentityEntity
import com.marriagebureau.ai.teach.entity.ExampleConversationEntity
import com.marriagebureau.ai.teach.entity.InstructionEntity
import com.marriagebureau.ai.teach.entity.RuleEntity
import com.marriagebureau.ai.teach.entity.TrainingVersionEntity

@Database(
    entities = [
        MarriageProfileEntity::class,
        CustomerEntity::class,
        ConversationMessageEntity::class,
        CustomerRequirementEntity::class,
        AgentIdentityEntity::class,
        InstructionEntity::class,
        RuleEntity::class,
        ExampleConversationEntity::class,
        TrainingVersionEntity::class,
        KnowledgeDocumentEntity::class,
        KnowledgeChunkEntity::class,
        WorkflowStepEntity::class,
        QueueMessageEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun customerDao(): CustomerDao
    abstract fun teachDao(): TeachDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun queueDao(): QueueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "marriage_bureau_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun createInMemoryDatabase(context: Context): AppDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                AppDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        }
    }
}
