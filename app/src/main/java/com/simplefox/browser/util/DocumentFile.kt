package com.simplefox.browser.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document

/**
 * Minimal SAF wrapper built directly on [DocumentsContract].
 * Replaces com.simplefox.browser.util.DocumentFile.
 */
class DocumentFile private constructor(
    private val context: Context,
    val uri: Uri
) {

    private fun <T> queryColumn(column: String, read: (android.database.Cursor) -> T?): T? =
        try {
            context.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { c ->
                if (c.moveToFirst() && !c.isNull(0)) read(c) else null
            }
        } catch (_: Exception) {
            null
        }

    /** Reads MIME type and flags with one provider query. */
    private fun queryMimeAndFlags(): Pair<String?, Int>? =
        try {
            context.contentResolver.query(
                uri, arrayOf(Document.COLUMN_MIME_TYPE, Document.COLUMN_FLAGS), null, null, null
            )?.use { c ->
                if (!c.moveToFirst()) null
                else Pair(if (c.isNull(0)) null else c.getString(0), if (c.isNull(1)) 0 else c.getInt(1))
            }
        } catch (_: Exception) {
            null
        }

    private fun queryString(column: String): String? = queryColumn(column) { it.getString(0) }
    private fun queryLong(column: String): Long = queryColumn(column) { it.getLong(0) } ?: 0L

    val name: String? get() = queryString(Document.COLUMN_DISPLAY_NAME)

    val type: String?
        get() {
            val mime = queryString(Document.COLUMN_MIME_TYPE)
            return if (mime == Document.MIME_TYPE_DIR) null else mime
        }

    val isDirectory: Boolean get() = queryString(Document.COLUMN_MIME_TYPE) == Document.MIME_TYPE_DIR

    val isFile: Boolean
        get() {
            val mime = queryString(Document.COLUMN_MIME_TYPE)
            return mime != Document.MIME_TYPE_DIR && !mime.isNullOrEmpty()
        }

    fun length(): Long = queryLong(Document.COLUMN_SIZE)

    fun lastModified(): Long = queryLong(Document.COLUMN_LAST_MODIFIED)

    fun exists(): Boolean =
        try {
            context.contentResolver.query(uri, arrayOf(Document.COLUMN_DOCUMENT_ID), null, null, null)
                ?.use { it.count > 0 } ?: false
        } catch (_: Exception) {
            false
        }

    fun canRead(): Boolean {
        if (context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        return !queryString(Document.COLUMN_MIME_TYPE).isNullOrEmpty()
    }

    fun canWrite(): Boolean {
        if (context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            != PackageManager.PERMISSION_GRANTED
        ) return false
        val (mime, flags) = queryMimeAndFlags() ?: return false
        if (mime.isNullOrEmpty()) return false
        if (flags and Document.FLAG_SUPPORTS_DELETE != 0) return true
        if (mime == Document.MIME_TYPE_DIR && flags and Document.FLAG_DIR_SUPPORTS_CREATE != 0) return true
        return flags and Document.FLAG_SUPPORTS_WRITE != 0
    }

    fun delete(): Boolean =
        try {
            DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (_: Exception) {
            false
        }

    fun renameTo(displayName: String): Boolean =
        try {
            DocumentsContract.renameDocument(context.contentResolver, uri, displayName) != null
        } catch (_: Exception) {
            false
        }

    fun createFile(mimeType: String, displayName: String): DocumentFile? =
        try {
            DocumentsContract.createDocument(context.contentResolver, uri, mimeType, displayName)
                ?.let { DocumentFile(context, it) }
        } catch (_: Exception) {
            null
        }

    fun createDirectory(displayName: String): DocumentFile? = createFile(Document.MIME_TYPE_DIR, displayName)

    fun listFiles(): Array<DocumentFile> {
        val result = ArrayList<DocumentFile>()
        forEachChild { id, _ -> result += DocumentFile(context, DocumentsContract.buildDocumentUriUsingTree(uri, id)) ; false }
        return result.toTypedArray()
    }

    fun findFile(displayName: String): DocumentFile? {
        var found: DocumentFile? = null
        forEachChild { id, name ->
            if (name == displayName) {
                found = DocumentFile(context, DocumentsContract.buildDocumentUriUsingTree(uri, id))
                true
            } else false
        }
        return found
    }

    /** Iterates children with a single provider query; return true from [action] to stop early. */
    private inline fun forEachChild(action: (id: String, name: String?) -> Boolean) {
        try {
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, DocumentsContract.getDocumentId(uri))
            context.contentResolver.query(
                children, arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME), null, null, null
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    val name = if (c.isNull(1)) null else c.getString(1)
                    if (action(id, name)) break
                }
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        fun fromSingleUri(context: Context, singleUri: Uri): DocumentFile? = DocumentFile(context, singleUri)

        fun fromTreeUri(context: Context, treeUri: Uri): DocumentFile? {
            return try {
                val documentId: String = (
                    if (DocumentsContract.isDocumentUri(context, treeUri)) DocumentsContract.getDocumentId(treeUri)
                    else DocumentsContract.getTreeDocumentId(treeUri)
                ) ?: return null
                DocumentFile(context, DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId))
            } catch (_: IllegalArgumentException) {
                null
            }
        }
    }
}
