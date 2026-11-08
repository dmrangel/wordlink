package io.github.dmrangel.wordlink.game

import java.time.LocalDate

// The shared engine (:core) counts days as plain epoch days; Android code keeps using LocalDate.

val LevelId.Daily.date: LocalDate get() = LocalDate.ofEpochDay(epochDay.toLong())

fun LevelGenerator.daily(date: LocalDate): Level = daily(date.toEpochDay().toInt())
