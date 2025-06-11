package com.aigroup.aigroupmobile.utils.encrypt

import java.security.MessageDigest

fun sha256(input: String): String {
  val md = MessageDigest.getInstance("SHA-256")
  val hash = md.digest(input.toByteArray(Charsets.UTF_8))
  return hash.joinToString("") {
    "%02x".format(it)
  }
}