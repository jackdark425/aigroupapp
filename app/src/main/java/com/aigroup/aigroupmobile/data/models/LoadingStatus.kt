package com.aigroup.aigroupmobile.data.models

import androidx.compose.runtime.mutableStateOf

sealed class LoadingStatus<out T>() {
  data object Loading : LoadingStatus<Nothing>()
  data class Success<T>(val data: T) : LoadingStatus<T>()
  data class Error(val message: String) : LoadingStatus<Nothing>()
}

fun <T> mutableLoadingStatusOf(initial: T) =
  mutableStateOf<LoadingStatus<T>>(LoadingStatus.Success(initial))

fun <T> mutableLoadingStatusOf() =
  mutableStateOf<LoadingStatus<T>>(LoadingStatus.Loading)

fun <T, R> LoadingStatus<T>.mapSuccess(block: (T) -> R): LoadingStatus<R> = when (this) {
  is LoadingStatus.Loading -> LoadingStatus.Loading
  is LoadingStatus.Success -> LoadingStatus.Success(block(data))
  is LoadingStatus.Error -> LoadingStatus.Error(message)
}

// TODO: better solution
val <T> LoadingStatus<T>.label: LoadingStatusLabel
  get() = when (this) {
    is LoadingStatus.Loading -> LoadingStatusLabel.LOADING
    is LoadingStatus.Success -> LoadingStatusLabel.SUCCESS
    is LoadingStatus.Error -> LoadingStatusLabel.ERROR
  }

enum class LoadingStatusLabel {
  LOADING, SUCCESS, ERROR
}

// TODO: rename this because Success(data) !!! and remove redunt !!
val <T> LoadingStatus<T>.data: T?
  get() = when (this) {
    is LoadingStatus.Success -> data
    else -> null
  }

val <T> LoadingStatus<T>.loading: Boolean
  get() = this is LoadingStatus.Loading