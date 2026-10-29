package com.example.geostamp.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens a maps app (Google Maps if installed). Returns false when no app can show a map. */
fun openMaps(context: Context, latitude: Double?, longitude: Double?): Boolean {
    val uri = if (latitude != null && longitude != null) {
        Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
    } else {
        Uri.parse("geo:0,0?q=")
    }
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** The text currently on the clipboard, or null when it is empty. Only read when the person taps Paste. */
fun readClipboardText(context: Context): String? {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = manager.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()?.takeIf { it.isNotBlank() }
}
