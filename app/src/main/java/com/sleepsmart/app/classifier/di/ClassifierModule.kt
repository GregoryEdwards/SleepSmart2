package com.sleepsmart.app.classifier.di

import com.sleepsmart.app.audio.AudioCapture
import com.sleepsmart.app.audio.AndroidAudioCapture
import com.sleepsmart.app.audio.DefaultFeatureExtractor
import com.sleepsmart.app.audio.FeatureExtractor
import com.sleepsmart.app.classifier.HeuristicS4M
import com.sleepsmart.app.classifier.SleepStageClassifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ClassifierModule {
    @Binds
    @Singleton
    abstract fun bindClassifier(impl: HeuristicS4M): SleepStageClassifier

    @Binds
    @Singleton
    abstract fun bindFeatureExtractor(impl: DefaultFeatureExtractor): FeatureExtractor

    @Binds
    @Singleton
    abstract fun bindAudioCapture(impl: AndroidAudioCapture): AudioCapture
}
