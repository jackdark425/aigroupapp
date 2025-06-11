package com.aigroup.aigroupmobile.utils.validators

sealed class ValidationResult {
  data object Success : ValidationResult()
  data class Error(val message: String) : ValidationResult()
}