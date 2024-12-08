package me.mudkip.moememos.data.module

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.mudkip.moememos.data.local.MoeMemosDatabase
import me.mudkip.moememos.data.local.FileStorage
import me.mudkip.moememos.data.local.dao.MemoDao
import me.mudkip.moememos.data.repository.LocalDatabaseRepository
import me.mudkip.moememos.data.local.UserPreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): MoeMemosDatabase {
        return MoeMemosDatabase.getDatabase(context)
    }
    
    @Singleton
    @Provides
    fun provideMemoDao(database: MoeMemosDatabase) = database.memoDao()

    @Singleton
    @Provides
    fun provideLocalDatabaseRepository(
        memoDao: MemoDao,
        fileStorage: FileStorage,
        userPreferences: UserPreferences
    ) = LocalDatabaseRepository(memoDao, fileStorage, userPreferences)
} 