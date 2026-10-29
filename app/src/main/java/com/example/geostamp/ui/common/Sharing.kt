// geostamp-iteration: 8
package com.example.geostamp.ui.common

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens the Android share sheet for a saved photo. Only a content URI is shared, never a file path. */
fun shareImage(context: Context, uri: Uri): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(send, "Share photo").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** Shares several saved photos at once through the Android share sheet. */
fun shareImages(context: Context, uris: List<Uri>): Boolean {
    if (uris.isEmpty()) return false
    val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/jpeg"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        val clip = ClipData.newRawUri("", uris.first())
        uris.drop(1).forEach { clip.addItem(ClipData.Item(it)) }
        clipData = clip
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(send, "Share photos").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
