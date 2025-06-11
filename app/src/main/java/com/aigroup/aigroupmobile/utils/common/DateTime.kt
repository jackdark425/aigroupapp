package com.aigroup.aigroupmobile.utils.common

import com.aigroup.aigroupmobile.R
import com.aigroup.aigroupmobile.appQuantityStringResource
import com.aigroup.aigroupmobile.appStringResource
import io.ktor.utils.io.bits.highInt
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toLocalDateTime
import org.mongodb.kbson.ObjectId

// TODO: using jvm date directly?

val ObjectId.localDateTime: LocalDateTime
  get() {
    val seconds = timestamp
    val instant = Instant.fromEpochSeconds(seconds.toLong())
    return instant.local
  }

val Instant.local: LocalDateTime
  get() = toLocalDateTime(TimeZone.currentSystemDefault())

private val SIMPLE_TIME_FMT: DateTimeFormat<LocalTime> = LocalTime.Format {
  hour(Padding.ZERO)
  char(':')
  minute(Padding.ZERO)
}

private val SIMPLE_DATE_FMT: DateTimeFormat<LocalDate> = LocalDate.Format {
  year(Padding.SPACE)
  char('/')
  monthNumber(Padding.ZERO)
  char('/')
  dayOfMonth(Padding.ZERO)
}

val LocalDateTime.startOfDay: LocalDateTime
  get() = LocalDateTime(date, LocalTime.fromSecondOfDay(0))

val LocalDateTime.instant: Instant
  get() = toInstant(TimeZone.currentSystemDefault())

val LocalDateTime.Companion.now: LocalDateTime
  get() = Clock.System.now().local

val LocalDateTime.readableTimePeriod: String
  get() {
    return when (hour) {
      in 6..8 -> appStringResource(R.string.label_date_time_dawn)
      in 9..11 -> appStringResource(R.string.label_date_time_morning)
      in 12..13 -> appStringResource(R.string.label_date_time_noon)
      in 14..18 -> appStringResource(R.string.label_date_time_afternoon)
      else -> appStringResource(R.string.label_date_time_evening)
    }
  }

val LocalDateTime.simpleDateStr: String
  get() = date.format(SIMPLE_DATE_FMT)

val LocalDateTime.readableStr: String
  get() {
    val duration = LocalDateTime.now.startOfDay.instant - startOfDay.instant
    val days = duration.inWholeDays
    val time = time.format(SIMPLE_TIME_FMT)

    return when (days) {
      0L -> appStringResource(R.string.label_date_time_today)
      1L -> appStringResource(R.string.label_date_time_yesterday)
      2L -> appStringResource(R.string.label_date_time_two_days_ago)
      else -> date.format(SIMPLE_DATE_FMT)
    }.let { "$it $time" }
  }

val LocalDate.readableStr: String
  get() = format(SIMPLE_DATE_FMT)

// TODO: 考虑这种时间显示随着时间推移有可能的显示错误问题，不会试试更新
val LocalDateTime.readableStrExquisite: String
  get() {
    val duration = LocalDateTime.now.startOfDay.instant - startOfDay.instant
    val days = duration.inWholeDays
    val time = time.format(SIMPLE_TIME_FMT)

    return when (days) {
      0L -> {
        val duration = Clock.System.now() - instant
        when {
          duration.inWholeMinutes < 1L -> appStringResource(R.string.label_datetime_just_now)
          duration.inWholeHours < 1L -> appQuantityStringResource(
            R.plurals.label_date_time_min_ago,
            duration.inWholeMinutes.toInt(), // TODO: 溢出问题？
            duration.inWholeHours
          )

          else -> time
        }
      }

      else -> when (days) {
        1L -> appStringResource(R.string.label_date_time_yesterday)
        2L -> appStringResource(R.string.label_date_time_two_days_ago)
        else -> date.format(SIMPLE_DATE_FMT)
      }.let { "$it $time" }
    }
  }

/**
 * parse date string like 2024-01-01
 * TODO: 优化
 */
fun LocalDate.Companion.fromDateString(dateStr: String): LocalDate {
  val parts = dateStr.split("-")
  return LocalDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
}