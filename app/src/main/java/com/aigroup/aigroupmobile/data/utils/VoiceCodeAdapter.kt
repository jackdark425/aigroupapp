package com.aigroup.aigroupmobile.data.utils

import com.aigroup.aigroupmobile.connect.voice.VoiceCode
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

class VoiceCodeAdapter(
  private val realmCodeProperty: KMutableProperty<String>
) {
  operator fun getValue(thisRef: Any?, property: KProperty<*>): VoiceCode {
    val code = realmCodeProperty.call(thisRef)
    return VoiceCode.fromFullCode(code)
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: VoiceCode) {
    val code = value.fullCode()
    realmCodeProperty.setter.call(thisRef, code)
  }
}