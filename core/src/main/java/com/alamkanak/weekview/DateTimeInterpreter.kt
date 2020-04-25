package com.alamkanak.weekview

import java.util.Calendar

interface DateTimeInterpreter {
    fun interpretDate(date: Calendar): String
    fun interpretTime(hour: Int): String
}
