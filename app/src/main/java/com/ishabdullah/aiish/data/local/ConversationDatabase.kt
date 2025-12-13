package com.ishabdullah.aiish.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class ConversationDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao

    companion object {
        @Volatile
        private var INSTANCE: ConversationDatabase? = null

        fun getDatabase(context: Context): ConversationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ConversationDatabase::class.java,
                    "conversation_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
