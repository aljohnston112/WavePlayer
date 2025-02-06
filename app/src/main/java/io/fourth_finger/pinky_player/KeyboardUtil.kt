package io.fourth_finger.pinky_player
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity

class KeyboardUtil {

    companion object {
        fun hideKeyboard(view: View) {
            (view.context.getSystemService(
                AppCompatActivity.INPUT_METHOD_SERVICE
            ) as InputMethodManager).hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}