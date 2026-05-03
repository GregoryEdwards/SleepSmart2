package com.sleepsmart.app.data.db.di

import android.content.Context
import androidx.room.Room
import com.sleepsmart.app.data.db.DatabaseKey
import com.sleepsmart.app.data.db.DisturbanceDao
import com.sleepsmart.app.data.db.EpochDao
import com.sleepsmart.app.data.db.SessionDao
import com.sleepsmart.app.data.db.SleepDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        key: DatabaseKey
    ): SleepDatabase {
        SQLiteDatabase.loadLibs(context)
        val factory = SupportFactory(key.passphrase())
        return Room.databaseBuilder(context, SleepDatabase::class.java, "sleepsmart.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides fun provideSessionDao(db: SleepDatabase): SessionDao = db.sessionDao()
    @Provides fun provideEpochDao(db: SleepDatabase): EpochDao = db.epochDao()
    @Provides fun provideDisturbanceDao(db: SleepDatabase): DisturbanceDao = db.disturbanceDao()
}
