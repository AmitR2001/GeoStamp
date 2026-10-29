package com.example.geostamp.data.media

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.IOException

/**
 * Copies a saved photo into a folder the person picked with the system folder chooser
 * (Storage Access Framework). The picked folder is only used for this one export.
 */
class FolderExporter(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun export(source: Uri, folderTree: Uri, displayName: String): Uri {
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            folderTree, DocumentsContract.getTreeDocumentId(folderTree)
        )
        val created = DocumentsContract.createDocument(resolver, parent, "image/jpeg", displayName)
            ?: throw IOException("That folder would not accept the file.")
        try {
            val input = resolver.openInputStream(source) ?: throw IOException("Could not read the saved photo.")
            input.use { from ->
                val output = resolver.openOutputStream(created, "w")
                    ?: throw IOException("Could not write into that folder.")
                output.use { to -> from.copyTo(to) }
            }
        } catch (t: Throwable) {
            runCatching { DocumentsContract.deleteDocument(resolver, created) }
            throw t
        }
        return created
    }
}
