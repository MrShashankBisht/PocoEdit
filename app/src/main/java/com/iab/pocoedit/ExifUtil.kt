package com.iab.pocoedit

import android.content.Context
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.text.TextUtils
import com.iab.galleryandlibrary.utils.Size
import com.iab.photoeditor.ImageInfoDataModel
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun getExifRotation(imgPath: String?): Int {
    return try {
        val exif = ExifInterface(imgPath!!)
        val rotationAmount = exif.getAttribute(ExifInterface.TAG_ORIENTATION)
        if (!TextUtils.isEmpty(rotationAmount)) {
            val rotationParam = rotationAmount!!.toInt()
            when (rotationParam) {
                ExifInterface.ORIENTATION_NORMAL -> 0
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } else {
            0
        }
    } catch (ex: Exception) {
        0
    }
}

fun getImageInfoDataModel(
    context: Context,
    imageUri: Uri,
    path: String,
    bucketName: String?,
    name: String?
): ImageInfoDataModel {
    val imageInfoDataModel = ImageInfoDataModel()
    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//            apply Media Query on image Uri and get info of image

        val projection = arrayOf(
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATE_TAKEN
        )
        val selection = null
        val selectionArgs = null
        val sortOrder = null

        context.contentResolver.query(
            imageUri,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                imageInfoDataModel.name = name!!
                val size:Float = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media.SIZE)).toFloat()
                imageInfoDataModel.size = calculateSizeInMB(size).toString() + " MB"
                imageInfoDataModel.dimension =  Size(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.WIDTH)).toFloat(), cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.HEIGHT)).toFloat())
//                calculating date and time
                val dateTime = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN)).toLong()
//                val calendar = Calendar.getInstance()
//                calendar.timeInMillis = dateTime
                val simpleDateFormat = SimpleDateFormat("EE, dd-MM-yyyy 'at' HH:mm a")
                imageInfoDataModel.setDate(simpleDateFormat.format(dateTime))
                imageInfoDataModel.bucketName = bucketName!!
                imageInfoDataModel.fullPath = path
            }
        }
    } else {
        try {
            val exifInterface = ExifInterface(path!!)
            val date = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            val length = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
            val width = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
            val size = calculateFileSize(path)
//            creating ImageInfoDataModel
            imageInfoDataModel.name = name!!
            imageInfoDataModel.size = size
            imageInfoDataModel.dimension = Size(width!!.toFloat(), length!!.toFloat())
            imageInfoDataModel.setDate(date)
            imageInfoDataModel.bucketName = bucketName!!
            imageInfoDataModel.fullPath = path
            return imageInfoDataModel
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    return imageInfoDataModel
}

fun calculateFileSize(filepath: String?): String {
    //String filepathstr=filepath.toString();
    val file = File(filepath)
    return calculateSizeInMB(file.length().toFloat()).toString() + " MB"
}

fun calculateSizeInMB(size:Float):Float{
    //         now calculating file size in KB
    val fileSizeInKB = size/1024
    // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
    return fileSizeInKB/1024
}