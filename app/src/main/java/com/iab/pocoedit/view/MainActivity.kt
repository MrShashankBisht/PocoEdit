package com.iab.pocoedit.view


import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.iab.galleryandlibrary.SpinnerGallery
import com.iab.galleryandlibrary.librarry.presenter.LibraryPresenterImpl
import com.iab.galleryandlibrary.librarry.presenter.LibraryPresenterInterface
import com.iab.imagetext.model.ImageTextDataModel
import com.iab.photoeditor.createTempImageFile
import com.iab.photoeditor.getRealPathFromURI
import com.iab.pocoedit.R
import iamutkarshtiwari.github.io.ananas.editimage.EditImageActivity.start
import iamutkarshtiwari.github.io.ananas.editimage.ImageEditorIntentBuilder
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException


class MainActivity : AppCompatActivity(), LibraryPresenterInterface.LibraryListener,
    PreviewImageDialogInterface, ActivityCompat.OnRequestPermissionsResultCallback{

    lateinit var libraryPresenterInterface: LibraryPresenterInterface
    lateinit var mAdView: AdView
    lateinit var mInterstitialAd: com.google.android.gms.ads.InterstitialAd
    var tempFileUri: Uri? = null
    var realPath:String? = null
    val TEMP_FILE_URI_STRING = "Temp_File_String"

    companion object RequestCode{
        val OPEN_GALLERY_CODE = 101
        val ACTION_REQUEST_EDITIMAGE = 102
        val CAMERA_REQUEST_CODE = 103
        val WRITE_EXTERNAL_STORAGE = 104
        val isSupportActionBarEnabled = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (supportActionBar != null) {
            if (isSupportActionBarEnabled) {
                supportActionBar!!.show()
            } else {
                supportActionBar!!.hide()
            }
        }

//        AdMob init
        MobileAds.initialize(this) {}
//        initializing interstitial ads
        mInterstitialAd = com.google.android.gms.ads.InterstitialAd(this)
        mInterstitialAd.adUnitId = getString(R.string.interstitial_ad_unit_id)
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        mInterstitialAd.adListener = object : AdListener(){
            override fun onAdClosed() {
                mInterstitialAd.loadAd(AdRequest.Builder().build())
                super.onAdClosed()
            }

            override fun onAdOpened() {
                super.onAdOpened()
            }

            override fun onAdLoaded() {
                super.onAdLoaded()
            }

            override fun onAdClicked() {
                mInterstitialAd.loadAd(AdRequest.Builder().build())
                super.onAdClicked()
            }

            override fun onAdImpression() {
                super.onAdImpression()
            }
        }
//        initializing banner ads
        mAdView = findViewById(R.id.main_activity_adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        libraryPresenterInterface = LibraryPresenterImpl.newBuilder(
            this,
            this
        )
                .withSpanCount(2)
                .withPaddingInRecyclerItem(5)
                .build()
        main_activity_gallery_fab.setOnClickListener {
            val intent = Intent(this, SpinnerGallery::class.java)
            startActivityForResult(intent, OPEN_GALLERY_CODE)
        }

        main_activity_camera_feb.setOnClickListener {
            dispatchTakePictureIntent()
        }
        libraryPresenterInterface.createView(resources.getString(R.string.app_name))
        container_top.addView(libraryPresenterInterface.getView())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putString(TEMP_FILE_URI_STRING, realPath)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        realPath = savedInstanceState.getString(TEMP_FILE_URI_STRING)
        tempFileUri = Uri.parse(realPath)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode){
            OPEN_GALLERY_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.let {
                        it.data?.let { it2 ->
                            Log.d("ImagePath", "" + it2)
                            val realPath = getRealPathFromURI(it2, this);
                            Log.d("ImagePath", "" + realPath)
                            realPath.let { it3 ->
                                startEditImageActivity(it3)
                            }
                        }
                    }
                }
            }
            ACTION_REQUEST_EDITIMAGE -> {
                libraryPresenterInterface.reloadView()
//                container_top.removeAllViews()
//                libraryPresenterInterface.createView(folderName)
//                container_top.addView(libraryPresenterInterface.getView())

            }
            CAMERA_REQUEST_CODE -> {
                if (resultCode == Activity.RESULT_OK) {
                    realPath?.let { it ->
                        startEditImageActivity(it)
                    }
                }

            }
        }
    }

    override fun onImageViewClicked(imageTextDataModel: ImageTextDataModel) {
        val fm = this.supportFragmentManager
        val previewDialog = PreviewDialog.newInstance(
            this,
            imageTextDataModel,
            this,
            "Image Preview"
        )
        previewDialog.show(fm, "Preview_Image")
    }

    override fun onImageDeleteSuccessFull(id: Int) {
        libraryPresenterInterface.removeItemFromRecyclerView(id)
    }

    //    @RequiresApi(Build.VERSION_CODES.R)
    private fun startEditImageActivity(realPath: String){
//        EditImageActivity.imageUri = data
//                    getRealPathFromURI(data!!.data!!, this)
        getOutputImagePath()?.let {
//            val intent = ImageEditorIntentBuilder(this,
//                    realPath, it)
//                    .withAddText()
//                    .withPaintFeature()
//                    .withFilterFeature()
//                    .withRotateFeature()
//                    .withCropFeature()
//                    .withBrightnessFeature()
//                    .withSaturationFeature()
//                    .withBeautyFeature()
//                    .withEditorTitle("Photo Editor")
//                    .forcePortrait(true)
//                    .setSupportActionBarVisibility(false)
//                    .build()
            val intent = ImageEditorIntentBuilder(this, realPath, it)
//                .withAddText()
                .withPaintFeature()
                .withFilterFeature()
                .withRotateFeature()
                .withCropFeature()
                .withEditorTitle("Photo Editor")
                .withBucketName(resources.getString(R.string.app_name))
                .forcePortrait(true)
                .setSupportActionBarVisibility(false)
                .build()
            start(this, intent, ACTION_REQUEST_EDITIMAGE)
            if(mInterstitialAd.isLoaded){
                mInterstitialAd.show()
            }else{
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
//             Create the File where the photo should go
            var photoFile: File? = null
            try {
                photoFile = createTempImageFile(this)
            } catch (ex: IOException) {
            }
//             Continue only if the File was successfully created
            if (photoFile != null) {
                realPath = photoFile.absolutePath
                tempFileUri = FileProvider.getUriForFile(
                    this,
                    "com.iab.pocoedit.provider",
                    photoFile
                )
//            tempFileUri = initImageSaving(this)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempFileUri)
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        }

        fun getRealPath(uriString: String, context: Context): String {
            var realPath: String = uriString
            context.contentResolver.query(Uri.parse(uriString), null, null, null, null)?.let { it1 ->
                it1.moveToFirst()
                var document_id: String = it1.getString(0)
                Log.d("document_id", "" + document_id)
                document_id = document_id.substring(document_id.lastIndexOf(":") + 1)
                Log.d("document_id_2", "" + document_id)
                it1.close()
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null,
                    MediaStore.Images.Media._ID + " = ? ",
                    arrayOf(document_id),
                    null
                )?.let { it2 ->
                    it2.moveToFirst()
                    Log.d("coursor_count", "" + it2.count)
                    if (it2.count > 0) {
                        val path: String = it2.getString(it2.getColumnIndex(MediaStore.Images.Media.DATA))
                        val id = it2.getLong(it2.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        // Add this to the Model
                        tempFileUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                        realPath = path
                    }
                    it2.close()
                }
            }
            return realPath
        }

    }

    fun getUriFromStringPath(path: String): Uri {
        return MediaStore.Files.getContentUri(path)
    }

    private fun saveBitmapInFileSystem(): String? {
        val filename: String?
        try {
            val sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            val pictureFileDir = File(sdDir, resources.getString(R.string.app_name))
            return if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                Log.d("", "Can't create directory to save image.")
                //Toast.makeText(getApplicationContext(), getResources().getString(R.string.create_dir_err), Toast.LENGTH_LONG).show();
                null
            } else {
                val photoFile = "Photo_" + System.currentTimeMillis() + ".png"
                filename = pictureFileDir.path + File.separator + photoFile
                //                File pictureFile = new File(filename);
                val pictureFile = File(filename)
                if (!pictureFile.exists()) {
                    pictureFile.createNewFile()
                }
                filename
            }
        } catch (e: Exception) {
            checkPermission()
            e.printStackTrace()
        }
        return null
    }


    private fun getOutputImagePath(): String? {
        var filename:String? = null
        if(Build.VERSION_CODES.P > Build.VERSION.SDK_INT){
            try {
                val sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                val pictureFileDir = File(sdDir, resources.getString(R.string.app_name))
                return if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                    Log.d("", "Can't create directory to save image.")
                    null
                } else {
                    val photoFile = "Photo_" + System.currentTimeMillis() + ".png"
                    filename = pictureFileDir.path + File.separator + photoFile
                    //                File pictureFile = new File(filename);
                    val pictureFile = File(filename)
                    if (!pictureFile.exists()) {
                        pictureFile.createNewFile()
                    }
                    filename
                }
            }catch (e: Exception) {
                e.printStackTrace()
            }
        }else{
            val relativeLocation = Environment.DIRECTORY_DCIM + File.separator + resources.getString(
                R.string.app_name
            )
            val contentValues = ContentValues()
            val photoFile = "Photo_" + System.currentTimeMillis() + ".png"
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, photoFile)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
//            contentValues.put(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME, resources.getString(R.string.app_name))
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativeLocation)
            contentValues.put(MediaStore.Images.Media.IS_PENDING, true)
            val resolver = this.contentResolver;
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            val uri = resolver.insert(contentUri, contentValues)
                    ?: throw IOException("Failed to create new MediaStore record.");
            return getRealPathFromURI(uri, this)

        }
        return null
    }

    fun createFile(): String? {
        // Add a specific media item.
        val resolver = applicationContext.contentResolver

// Find all audio files on the primary external storage device.
        val audioCollection =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

// Publish a new song.
        val newSongDetails = ContentValues().apply {
            val photoFile = "Photo_" + System.currentTimeMillis() + ".png"
            put(MediaStore.Images.Media.DISPLAY_NAME, photoFile)
        }

// Keeps a handle to the new song's URI in case we need to modify it
// later.
        val myFavoriteSongUri = resolver
                .insert(audioCollection, newSongDetails)
        Log.d("abc", "" + myFavoriteSongUri?.path)
        return myFavoriteSongUri?.path
    }


    //    @RequiresApi(Build.VERSION_CODES.R)
    fun checkPermission(){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.MANAGE_EXTERNAL_STORAGE
                )) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
                    WRITE_EXTERNAL_STORAGE
                );
            }
        }
    }
}
