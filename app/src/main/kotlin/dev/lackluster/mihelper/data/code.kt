package dev.lackluster.mihelper.data

import android.content.Context
import dev.lackluster.mihelper.R
import java.io.BufferedReader
import java.io.InputStreamReader

object Codes {
    fun getCode(context: Context): String {
        return context.resources.openRawResource(R.raw.code)
            .bufferedReader().use { it.readText() }
    }
}