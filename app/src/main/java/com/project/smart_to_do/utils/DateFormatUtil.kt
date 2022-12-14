package com.project.smart_to_do.utils

import java.text.SimpleDateFormat
import java.util.*

open class DateFormatUtil {

    companion object{
        fun hourly() = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        fun dateFormat() = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        fun dateFormatWithChar() = SimpleDateFormat("EE dd MMM yyyy", Locale.US)
        fun monthFormat() = SimpleDateFormat("MMMM-yyyy", Locale.getDefault())
        fun timeFormat() = SimpleDateFormat("HH:mm", Locale.getDefault())
    }



}