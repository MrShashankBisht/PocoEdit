package com.iab.photoeditor

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.iab.pocoedit.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

fun getRealPathFromURI(contentURI: Uri, context: Context): String {
    try {
        var result:String
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(
                contentURI, projection, null,
                null, null
        )
        if (cursor == null) {
            result = contentURI.toString()
        } else {
            cursor.moveToFirst()
            val idx = cursor
                .getColumnIndex(MediaStore.Images.Media.DATA)
            result = cursor.getString(idx)
            cursor.close()
        }
        return result
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return contentURI.toString()
}


fun getUriFromRealPath(realPath: String, context: Context): Uri? {
    try {
        val selection = MediaStore.Images.Media.DATA + " = ?"
        val selectionArgs = arrayOf(realPath)
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        )
        cursor?.let {
            it.moveToFirst()
            val idx = it.getColumnIndex(MediaStore.Images.Media._ID)
            val id: Long = it.getLong(idx)
            it.close()
            return ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
            )
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}


@Throws(IOException::class)
fun getTempFilename(context: Context): String {
    val outputDir = context.cacheDir
    val outputFile = File.createTempFile("image", "tmp", outputDir)
    return outputFile.absolutePath
}

@Throws(IOException::class)
fun createTempImageFile(context: Context): File? {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val fileName = "JPEG_${timeStamp}"
    context.getExternalFilesDir(Environment.DIRECTORY_DCIM + "/.CameraTutorial")?.let { it1->
        return File.createTempFile(
                fileName, /* prefix */
                ".jpg", /* suffix */
                it1 /* directory */
        )
    }
    return null
}

@Throws(IOException::class)
fun createOutImageFile(context: Context): String? {
    // Create an image file name
    val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
    val fileName = "JPEG_${timeStamp}"
    context.getExternalFilesDir(Environment.DIRECTORY_DCIM + File.separator + "${context.resources.getString(
        R.string.app_name)}")?.let { it1->
        return File.createTempFile(
                fileName, /* prefix */
                ".jpg", /* suffix */
                it1 /* directory */
        ).absolutePath
    }
    return null
}

fun initImageSaving(context: Context):Uri? {
    val relativeLocation = Environment.DIRECTORY_DCIM + File.separator + ".PhotoEditor"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString())
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    val resolver = context.contentResolver

    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)?.let {
        return it
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
    return null
}