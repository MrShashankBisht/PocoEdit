 package com.iab.pocoedit;

import android.media.ExifInterface;
import android.text.TextUtils;

import com.iab.galleryandlibrary.utils.Size;import com.iab.photoeditor.ImageInfoDataModel;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;


 public class ExifUtils {
    private ExifUtils() {
    }

    public static int getExifRotation(String imgPath) {
        try {
            ExifInterface exif = new ExifInterface(imgPath);
            String rotationAmount = exif.getAttribute(ExifInterface.TAG_ORIENTATION);
            if (!TextUtils.isEmpty(rotationAmount)) {
                int rotationParam = Integer.parseInt(rotationAmount);
                switch (rotationParam) {
                    case ExifInterface.ORIENTATION_NORMAL:
                        return 0;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        return 90;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        return 180;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        return 270;
                    default:
                        return 0;
                }
            } else {
                return 0;
            }
        } catch (Exception ex) {
            return 0;
        }
    }

    public static ImageInfoDataModel getImageInfoDataModel(String path, String bucketName, String name)
    {
        ImageInfoDataModel imageInfoDataModel = new ImageInfoDataModel();
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            String date = exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);
            String length = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH);
            String width = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH);
            String size = calculateFileSize(path);
//            creating ImageInfoDataModel
            imageInfoDataModel.setName(name);
            imageInfoDataModel.setSize(size);
            imageInfoDataModel.setDimension(new Size(Float.parseFloat(width), Float.parseFloat(length)));
            imageInfoDataModel.setDate(date);
            imageInfoDataModel.setBucketName(bucketName);
            imageInfoDataModel.setFullPath(path);
            return imageInfoDataModel;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imageInfoDataModel;
    }

     public static String calculateFileSize(String filepath) {
         //String filepathstr=filepath.toString();
         File file = new File(filepath);
//         calculating file size in Bites
         float fileSizeInBytes = file.length();
//         now calculating file size in KB
         float fileSizeInKB = fileSizeInBytes / 1024;
         // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
         float fileSizeInMB = fileSizeInKB / 1024;
         return Float.toString(fileSizeInMB);
     }
}
