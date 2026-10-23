package com.andrespar2.parsound.service;

import androidx.media3.exoplayer.ExoPlayer;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class PlaybackService_MembersInjector implements MembersInjector<PlaybackService> {
  private final Provider<ExoPlayer> exoPlayerProvider;

  public PlaybackService_MembersInjector(Provider<ExoPlayer> exoPlayerProvider) {
    this.exoPlayerProvider = exoPlayerProvider;
  }

  public static MembersInjector<PlaybackService> create(Provider<ExoPlayer> exoPlayerProvider) {
    return new PlaybackService_MembersInjector(exoPlayerProvider);
  }

  @Override
  public void injectMembers(PlaybackService instance) {
    injectExoPlayer(instance, exoPlayerProvider.get());
  }

  @InjectedFieldSignature("com.andrespar2.parsound.service.PlaybackService.exoPlayer")
  public static void injectExoPlayer(PlaybackService instance, ExoPlayer exoPlayer) {
    instance.exoPlayer = exoPlayer;
  }
}
