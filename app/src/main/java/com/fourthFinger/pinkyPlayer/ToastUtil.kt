package com.fourthFinger.pinkyPlayer

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.os.Build
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat

class ToastUtil {

    companion object {

        fun showToast(context: Context, @StringRes idMessage: Int) {
            val toast: Toast = Toast.makeText(context, idMessage, Toast.LENGTH_LONG)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    toast.view?.background?.colorFilter = BlendModeColorFilter(
                        ContextCompat.getColor(context, R.color.colorPrimary),
                        BlendMode.SRC_IN
                    )
                } else{
                    toast.view?.background?.setColorFilter(
                        ContextCompat.getColor(context, R.color.colorPrimary),
                        PorterDuff.Mode.SRC_IN
                    )

                }
                toast.view?.findViewById<TextView?>(R.id.message)?.textSize = 16f
            }
            toast.show()
        }

    }
}