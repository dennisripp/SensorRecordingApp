package dev.ostfalia.iotcam.ui

import android.content.Context
import android.content.Intent
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import dev.ostfalia.iotcam.network.APIRequestHandler
import dev.ostfalia.iotcam.network.VideoRepository
import dev.ostfalia.iotcam.network.oauth.Authenticator
import dev.ostfalia.iotcam.ui.fragments.VideoListViewFragment

class ToastFactory  {

    fun createSnackbar(text: String, view: VideoListViewFragment) : Snackbar {
        val snackbar = Snackbar.make(view.requireView(), "${text}, try again?",
            Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE

        val textView =
            snackbarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView

        return snackbar
    }

    fun createSnackbar(text: String, view: View) : Snackbar {
        val snackbar = Snackbar.make(view, "${text}",
            Snackbar.LENGTH_LONG)
        val snackbarView = snackbar.view
        snackbar.animationMode = BaseTransientBottomBar.ANIMATION_MODE_FADE

        val textView =
            snackbarView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView

        return snackbar
    }

    fun showToast(context: Context, text: String) {
        val toast = Toast.makeText(
            context,
            text,
            Toast.LENGTH_SHORT
        )
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show()
    }
}