package com.fourthFinger.pinkyPlayer

import androidx.core.os.LocaleListCompat
import java.util.concurrent.TimeUnit

class TextUtil {

    companion object {
        fun formatMillis(millis: Int): String {
            return String.format(
                LocaleListCompat.getDefault()[0],
                "%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(millis.toLong()),
                TimeUnit.MILLISECONDS.toMinutes(millis.toLong()) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis.toLong())),
                TimeUnit.MILLISECONDS.toSeconds(millis.toLong()) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis.toLong())))
        }

    }
}