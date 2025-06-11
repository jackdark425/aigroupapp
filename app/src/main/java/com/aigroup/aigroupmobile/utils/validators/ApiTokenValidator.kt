package com.aigroup.aigroupmobile.utils.validators

fun validateApiToken(token: String): ValidationResult {
  if (token.isEmpty()) {
    return ValidationResult.Error("Token 不能为空")
  }

  if (token.length < 5) {
    return ValidationResult.Error("Token 长度不能小于 5")
  }

  return ValidationResult.Success
}