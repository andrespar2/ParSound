package com.andrespar2.parsound.ui.screen.library;

import android.content.Context;
import androidx.media3.exoplayer.ExoPlayer;
import com.andrespar2.parsound.data.local.SongDao;
import com.andrespar2.parsound.scanner.MediaScanner;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class LibraryViewModel_Factory implements Factory<LibraryViewModel> {
  private final Provider<SongDao> songDaoProvider;

  private final Provider<MediaScanner> mediaScannerProvider;

  private final Provider<ExoPlayer> exoPlayerProvider;

  private final Provider<Context> contextProvider;

  public LibraryViewModel_Factory(Provider<SongDao> songDaoProvider,
      Provider<MediaScanner> mediaScannerProvider, Provider<ExoPlayer> exoPlayerProvider,
      Provider<Context> contextProvider) {
    this.songDaoProvider = songDaoProvider;
    this.mediaScannerProvider = mediaScannerProvider;
    this.exoPlayerProvider = exoPlayerProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public LibraryViewModel get() {
    return newInstance(songDaoProvider.get(), mediaScannerProvider.get(), exoPlayerProvider.get(), contextProvider.get());
  }

  public static LibraryViewModel_Factory create(Provider<SongDao> songDaoProvider,
      Provider<MediaScanner> mediaScannerProvider, Provider<ExoPlayer> exoPlayerProvider,
      Provider<Context> contextProvider) {
    return new LibraryViewModel_Factory(songDaoProvider, mediaScannerProvider, exoPlayerProvider, contextProvider);
  }

  public static LibraryViewModel newInstance(SongDao songDao, MediaScanner mediaScanner,
      ExoPlayer exoPlayer, Context context) {
    return new LibraryViewModel(songDao, mediaScanner, exoPlayer, context);
  }
}
