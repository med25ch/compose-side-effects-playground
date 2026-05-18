package com.sideeffects.playground.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// DI module — currently no external dependencies needed
// ViewModels are provided by Hilt automatically via @HiltViewModel
@Module
@InstallIn(SingletonComponent::class)
object AppModule
