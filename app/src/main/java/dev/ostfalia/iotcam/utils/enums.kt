package dev.ostfalia.iotcam.utils

import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class enums {



}

enum class EnvironmentMeta(val id: Int) {
    UNKNOWN(0),
    INDOOR(1),
    OUTDOOR(2),
    DAY(3),
    NIGHT(4),
}

fun getById(id: Int?): EnvironmentMeta? {
    for (e in EnvironmentMeta.values()) {
        if (e.id.equals(id)) return e
    }
    return EnvironmentMeta.UNKNOWN
}

fun getEnvironmentEnum(context: Context) : EnvironmentMeta? {

    var builder: MaterialAlertDialogBuilder? = null
    builder = MaterialAlertDialogBuilder(context)

    val asc = Array<CharSequence>(EnvironmentMeta.values().size) { i -> getById(i).toString() }
    var env : EnvironmentMeta? = null

    builder!!.setItems(asc,
        DialogInterface.OnClickListener { dialog, which ->
            env = getById(which)

        })


    val alert = builder!!.create()
    alert.setTitle("pick your environment")
    alert.show()

    return env
}