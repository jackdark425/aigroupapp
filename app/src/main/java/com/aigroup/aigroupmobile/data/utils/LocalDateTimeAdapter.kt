package com.aigroup.aigroupmobile.data.utils

import com.aigroup.aigroupmobile.utils.common.instant
import com.aigroup.aigroupmobile.utils.common.local
import io.realm.kotlin.types.RealmInstant
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty

class LocalDateTimeAdapter(
  private val realmInstantProperty: KMutableProperty<RealmInstant>
) {
  operator fun getValue(thisRef: Any?, property: KProperty<*>): LocalDateTime {
    val seconds = realmInstantProperty.call(thisRef).epochSeconds
    return Instant.fromEpochSeconds(seconds, 0).local
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: LocalDateTime) {
    val epochSeconds = value.instant.epochSeconds
    val reamInstant = RealmInstant.from(epochSeconds, 0)
    realmInstantProperty.setter.call(thisRef, reamInstant)
  }
}