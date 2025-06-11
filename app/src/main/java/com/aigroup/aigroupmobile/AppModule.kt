package com.aigroup.aigroupmobile

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.aigroup.aigroupmobile.data.AppDatabase
import com.aigroup.aigroupmobile.data.dao.ChatDao
import com.aigroup.aigroupmobile.data.models.AppPreferences
import com.aigroup.aigroupmobile.data.utils.AppPreferencesSerializer
import com.aigroup.aigroupmobile.services.chat.plugins.ChatPluginExecutor
import com.aigroup.aigroupmobile.repositories.CustomLLMProviderRepository
import com.aigroup.aigroupmobile.repositories.ModelRepository
import com.aigroup.aigroupmobile.utils.system.PathManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.realm.kotlin.Realm
import javax.inject.Singleton

val Context.dataStore: DataStore<AppPreferences> by dataStore(
  fileName = "settings.pb",
  serializer = AppPreferencesSerializer,
)

@Module
@InstallIn(SingletonComponent::class)
class AppModule {


  @Provides
  @Singleton
  fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
    return AppDatabase.create(context)
  }

  @Provides
  @Singleton
  fun providePathManager(@ApplicationContext context: Context): PathManager {
    return PathManager(context)
  }

  @Provides
  @Singleton
  fun provideDataStore(@ApplicationContext context: Context): DataStore<AppPreferences> {
    return context.dataStore
  }

  @Provides
  @Singleton
  fun provideRealm(appDatabase: AppDatabase): Realm {
    return appDatabase.realm
  }

  // TODO: should using singleton for Repository?
  @Provides
  @Singleton
  fun provideModelRepository(
    dataStore: DataStore<AppPreferences>,
    customLLMProviderRepository: CustomLLMProviderRepository
  ): ModelRepository {
    return ModelRepository(dataStore, customLLMProviderRepository)
  }
}