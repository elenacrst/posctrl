package `is`.posctrl.posctrl_android.util.extensions

import `is`.posctrl.posctrl_android.util.GestureListener
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.View

fun View.setOnSwipeListener(onSwipeLeft: () -> Unit = {}, onSwipeRight: () -> Unit = {}, onSwipeTop: () -> Unit = {}, onSwipeBottom: () -> Unit = {}) {
    val gestureDetector = GestureDetector(context, GestureListener(onSwipeLeft, onSwipeRight, onSwipeTop, onSwipeBottom))
    setOnTouchListener { _, event ->
        if (event.action == KeyEvent.ACTION_UP) {
            performClick()
        }

        return@setOnTouchListener gestureDetector.onTouchEvent(event)
    }
}