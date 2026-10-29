package com.marriagebureau.ai

import android.app.Application
import com.marriagebureau.ai.data.db.AppDatabase

class MarriageBureauApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: MarriageBureauApp
            private set
    }
}
