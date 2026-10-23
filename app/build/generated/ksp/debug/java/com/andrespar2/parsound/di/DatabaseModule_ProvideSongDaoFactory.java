package com.andrespar2.parsound.di;

import com.andrespar2.parsound.data.local.ParSoundDatabase;
import com.andrespar2.parsound.data.local.SongDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class DatabaseModule_ProvideSongDaoFactory implements Factory<SongDao> {
  private final Provider<ParSoundDatabase> databaseProvider;

  public DatabaseModule_ProvideSongDaoFactory(Provider<ParSoundDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SongDao get() {
    return provideSongDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideSongDaoFactory create(
      Provider<ParSoundDatabase> databaseProvider) {
    return new DatabaseModule_ProvideSongDaoFactory(databaseProvider);
  }

  public static SongDao provideSongDao(ParSoundDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSongDao(database));
  }
}
