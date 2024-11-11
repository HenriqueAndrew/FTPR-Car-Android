package com.example.myapitest.database

import android.content.Context
import androidx.room.Room

object DatabaseBuilder {

    private var instance: AppDatabase? = null

    fun getInstance(context: Context? = null): AppDatabase{
        return instance ?: synchronized(this){
            if(context == null){
                throw Exception()
            }
            val newInstance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "app_database")
                .addMigrations(DatabaseMigrations.MIGRATION_1_2)
                .build()
            instance = newInstance
            newInstance
        }
    }

}