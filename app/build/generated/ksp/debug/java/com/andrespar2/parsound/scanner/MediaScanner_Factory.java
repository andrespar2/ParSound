package com.andrespar2.parsound.scanner;

import android.content.Context;
import com.andrespar2.parsound.data.local.SongDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class MediaScanner_Factory implements Factory<MediaScanner> {
  private final Provider<Context> contextProvider;

  private final Provider<SongDao> songDaoProvider;

  public MediaScanner_Factory(Provider<Context> contextProvider,
      Provider<SongDao> songDaoProvider) {
    this.contextProvider = contextProvider;
    this.songDaoProvider = songDaoProvider;
  }

  @Override
  public MediaScanner get() {
    return newInstance(contextProvider.get(), songDaoProvider.get());
  }

  public static MediaScanner_Factory create(Provider<Context> contextProvider,
      Provider<SongDao> songDaoProvider) {
    return new MediaScanner_Factory(contextProvider, songDaoProvider);
  }

  public static MediaScanner newInstance(Context context, SongDao songDao) {
    return new MediaScanner(context, songDao);
  }
}
