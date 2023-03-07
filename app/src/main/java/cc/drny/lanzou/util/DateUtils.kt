package cc.drny.lanzou.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private val simpleDateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE)
    private val nowCalendar = Calendar.getInstance()
    private val beforeCalendar = Calendar.getInstance()

    @JvmStatic
    fun Long.handleTime(): String {
        nowCalendar.timeInMillis = System.currentTimeMillis()
        beforeCalendar.timeInMillis = this
        if (beforeCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)) {
            return showTime()
        }
        return simpleDateFormat.format(this)
    }

    private fun showTime(): String {
        val beforeMonth = beforeCalendar[Calendar.MONTH]
        val nowMonth = nowCalendar[Calendar.MONTH]
        return if (beforeMonth == nowMonth) {
            val beforeDay = beforeCalendar[Calendar.DAY_OF_MONTH]
            val nowDay = nowCalendar[Calendar.DAY_OF_MONTH]
            val day = nowDay - beforeDay
            val beforeHour = beforeCalendar[Calendar.HOUR_OF_DAY]
            val beforeMinute = beforeCalendar[Calendar.MINUTE]
            if (day == 0) {
                val nowHour = nowCalendar[Calendar.HOUR_OF_DAY]
                val hour = nowHour - beforeHour
                val nowMinute = nowCalendar[Calendar.MINUTE]
                val minute = nowMinute - beforeMinute
                when (hour) {
                    0 -> {
                        if (minute < 1) {
                            "刚刚"
                        } else {
                            "${minute}分钟前"
                        }
                    }
                    1 ->  {
                        val m = nowMinute + 60 - beforeMinute
                        if (m < 60) {
                            "${m}分钟前"
                        } else {
                            "1小时前"
                        }
                    }
                    else -> "${hour}小时前"
                }
            } else {
                when {
                    day == 1 -> "昨天"  /*${beforeHour}点${beforeMinute}分*/
                    day == 2 -> "前天"
                    day % 7 == 0 -> (day / 7).toCN() + "星期前" /*${beforeDay}号*/
                    else -> "${day}天前"
                }
            }
        } else {
            (beforeMonth + 1).toString() + "月${beforeCalendar[Calendar.DAY_OF_MONTH]}日"
        }

    }

    private fun Int.toCN(): String {
        return when (this) {
            1 -> "一"
            2 -> "两"
            3 -> "三"
            4 -> "四"
            else -> this.toString()
        }
    }
}

/*fun String.date2Long(): Long {
    return try {
        Timestamp.valueOf(this).time
    } catch (e: Exception) {
        e.printStackTrace()
        0
    }
}*/

/*fun Long.handleTime(): String {
    val nowCalendar = Calendar.getInstance()
    nowCalendar.time = Date()
    val beforeCalendar = Calendar.getInstance()
    beforeCalendar.time = Date(this)
    if (beforeCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR)) {
        return showTime(beforeCalendar, nowCalendar)
    }
    return SimpleDateFormat("yyyy年MM月dd日", Locale.CHINESE).format(this)
}*/

