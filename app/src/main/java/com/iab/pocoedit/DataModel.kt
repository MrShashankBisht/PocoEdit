package com.iab.photoeditor
import com.iab.galleryandlibrary.utils.Size
public class ImageInfoDataModel{
    var name:String = ""
    var size:String = ""
    var dimension:Size = Size(0f,0f)
    private var date:String = ""
    var time:String = ""
    var bucketName: String = ""
    var fullPath:String = ""

    fun setDate(dateTime :String?){
        date = dateTime ?: "No data found"
    }
    fun getDate():String{
        return date
    }
}