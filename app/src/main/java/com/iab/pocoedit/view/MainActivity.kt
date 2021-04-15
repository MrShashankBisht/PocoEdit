package com.iab.pocoedit.view


import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.iab.galleryandlibrary.SpinnerGallery
import com.iab.galleryandlibrary.librarry.presenter.LibraryPresenterImpl
import com.iab.galleryandlibrary.librarry.presenter.LibraryPresenterInterface
import com.iab.imagetext.model.ImageTextDataModel
import com.iab.photoeditor.createTempImageFile
import com.iab.photoeditor.getRealPathFromURI
import com.iab.pocoedit.Privacy
import com.iab.pocoedit.R
import com.msl.permission_dialog.PermissionPresenterImpl
import com.msl.permission_dialog.PermissionPresenterInterface
import com.msl.permission_dialog.PermissionPresenterInterface.PermissionListener
import com.msl.permission_dialog.PermissionViewDataModel
import com.msl.permission_dialog.permissiondiscriptiv.PermissionDescriptiveDataModel
import iamutkarshtiwari.github.io.ananas.editimage.EditImageActivity.start
import iamutkarshtiwari.github.io.ananas.editimage.ImageEditorIntentBuilder
import iamutkarshtiwari.github.io.ananas.general_dialog.GeneralDialogPresenterImpl
import iamutkarshtiwari.github.io.ananas.general_dialog.GeneralDialogPresenterInterface
import iamutkarshtiwari.github.io.ananas.general_dialog.GenrealDialogDataModel
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.util.*


class MainActivity : AppCompatActivity(), LibraryPresenterInterface.LibraryListener,
    PreviewImageDialogInterface, ActivityCompat.OnRequestPermissionsResultCallback{

    lateinit var libraryPresenterInterface: LibraryPresenterInterface
    lateinit var mAdView: AdView
    lateinit var mInterstitialAd: com.google.android.gms.ads.InterstitialAd
    var tempFileUri: Uri? = null
    var realPath:String? = null
    val TEMP_FILE_URI_STRING = "Temp_File_String"


//    permission View Variables
    var permissionViewDataModel: PermissionViewDataModel? = null
    private var isDoNotAskAgain: Boolean = false
    lateinit var permissionPresenterInterface: PermissionPresenterInterface
    var permissionListener: PermissionListener = object : PermissionListener{
        override fun onRightBtnClicked() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                (this@MainActivity as Activity).requestPermissions(
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA),
                        PERMISSIONS_REQUEST)
            }
            permissionPresenterInterface.dismissDialog()
        }

        override fun onLeftBtnClicked() {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", this@MainActivity.getPackageName(), null))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            (this@MainActivity as Activity).startActivityForResult(intent, PERMISSIONS_SETTING_REQUEST)
            permissionPresenterInterface.dismissDialog()
        }
    }

//    general Dialog variables
    var generalDialogPresenterListener: GeneralDialogPresenterInterface.GeneralDialogPresenterListener = object: GeneralDialogPresenterInterface.GeneralDialogPresenterListener{
        override fun onRightBtnClicked() {
            generalDialogPresenterInterface.dismissDialog()
        }

        override fun onLeftBtnClicked() {
            generalDialogPresenterInterface.dismissDialog()
            this@MainActivity.finish()
        }
    }
    lateinit var generalDialogPresenterInterface: GeneralDialogPresenterInterface
    val genrealDialogDataModel = GenrealDialogDataModel()


//    companion object
    companion object RequestCode{
        const val OPEN_GALLERY_CODE = 101
        const val ACTION_REQUEST_EDITIMAGE = 102
        const val CAMERA_REQUEST_CODE = 103
        const val WRITE_EXTERNAL_STORAGE = 104
        private const val PERMISSIONS_SETTING_REQUEST = 921
        private const val PERMISSIONS_REQUEST = 922
        const val isSupportActionBarEnabled = false
    }

// onCreate Method
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

    main_privacy_btn.setOnClickListener {
        val intent = Intent(this, Privacy::class.java)
        startActivity(intent)
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                        this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED||
                        this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
            createPermissionViewDataModel()
            permissionPresenterInterface = PermissionPresenterImpl.newBuilder(this, permissionListener)
                    .withHeaderBackgroundColor(Color.TRANSPARENT)
                    .withHeaderTextColor(this.getResources().getColor(R.color.white))
                    .withHeaderTextSize(20)
                    .withTextSize(14)
//                    .withHeaderTextFontFaceName("header_regular.ttf")
//                    .withTextFontFaceName("text_light.ttf")
                    .withBackgroundColor(this.getResources().getColor(R.color.status_bar))
                    .withTextColor(this.getResources().getColor(R.color.white))
                    .withButtonTextColor(this.getResources().getColor(R.color.status_bar))
                    .withButtonBackgroundColor(this.getResources().getColor(R.color.white, null))
                    .withButtonTextSize(15)
                    .withHeaderUnderlineViewBackgroundColorAndVisibility(this.getResources().getColor(R.color.white, null), View.VISIBLE)
                    .withPermissionDescriptiveBackgroundColor(Color.TRANSPARENT)
                    .withPermissionDescriptiveSubTextSize(10)
                    .build()
            permissionPresenterInterface.onCreateView(permissionViewDataModel)
            if (isDoNotAskAgain) {
                permissionPresenterInterface.onLeftBtnVisibility(View.VISIBLE)
            } else {
                permissionPresenterInterface.onLeftBtnVisibility(View.GONE)
                permissionPresenterInterface.showNoteText(View.GONE)
            }
            permissionPresenterInterface.showDialog()
        }

        generalDialogPresenterInterface = GeneralDialogPresenterImpl.newBuilder(this, generalDialogPresenterListener)
                .withHeaderTextColor(this.resources.getColor(R.color.title_bar))
                .withTextColor(resources.getColor(R.color.title_bar))
                .withButtonBackgroundColor(resources.getColor(R.color.title_bar))
                .withButtonTextColor(this.resources.getColor(R.color.white))
                .withHeaderBackgroundColor(Color.TRANSPARENT)
                .withHeaderTextSize(20)
//                .withHeaderTextFontFace("header_regular.ttf")
//                .withTextFontFace("text_light.ttf")
                .withTextSize(12)
                .withButtonTextSize(16)
                .withCancelBtnBackgroundColor(resources.getColor(R.color.title_bar))
                .withViewBackgroundColor(resources.getColor(R.color.white))
                .build()


        genrealDialogDataModel.setTitleText(this.getString(R.string.exit_dialog_title))
        genrealDialogDataModel.setDescriptionText(this.getString(R.string.exit_dialog_message))
        genrealDialogDataModel.setLeftBtnText(this.getString(R.string.yes))
        genrealDialogDataModel.setRightBtnText(this.getString(R.string.no))
        generalDialogPresenterInterface.createView(genrealDialogDataModel)


        main_back_btn.setOnClickListener {
            generalDialogPresenterInterface.showDialog()
        }
    }

    override fun onBackPressed() {
        generalDialogPresenterInterface.showDialog()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.run {
            putString(TEMP_FILE_URI_STRING, realPath)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        realPath = savedInstanceState.getString(TEMP_FILE_URI_STRING)
        if(realPath != null){
            tempFileUri = Uri.parse(realPath)
        }
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    (this.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                            this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)) {
                if (!ActivityCompat.shouldShowRequestPermissionRationale((this as Activity), permissions[0])) {
                    Toast.makeText(this, "user press don't ask again message !!!", Toast.LENGTH_SHORT).show()
                    isDoNotAskAgain = true
                }
                if (isDoNotAskAgain) {
                    permissionPresenterInterface.showNoteText(View.VISIBLE)
                    permissionPresenterInterface.onLeftBtnVisibility(View.VISIBLE)
                }else{
                    permissionPresenterInterface.showNoteText(View.GONE)
                    permissionPresenterInterface.onLeftBtnVisibility(View.GONE)
                }
                permissionPresenterInterface.showDialog()
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
                .withAddText()
                .withPaintFeature()
                .withFilterFeature()
//                .withRotateFeature()
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

//    private fun saveBitmapInFileSystem(): String? {
//        val filename: String?
//        try {
//            val sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
//            val pictureFileDir = File(sdDir, resources.getString(R.string.app_name))
//            return if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
//                Log.d("", "Can't create directory to save image.")
//                //Toast.makeText(getApplicationContext(), getResources().getString(R.string.create_dir_err), Toast.LENGTH_LONG).show();
//                null
//            } else {
//                val photoFile = "Photo_" + System.currentTimeMillis() + ".png"
//                filename = pictureFileDir.path + File.separator + photoFile
//                //                File pictureFile = new File(filename);
//                val pictureFile = File(filename)
//                if (!pictureFile.exists()) {
//                    pictureFile.createNewFile()
//                }
//                filename
//            }
//        } catch (e: Exception) {
//            checkPermission()
//            e.printStackTrace()
//        }
//        return null
//    }


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
//    fun checkPermission(){
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
//            // Permission is not granted
//            if (ActivityCompat.shouldShowRequestPermissionRationale(
//                            this,
//                            Manifest.permission.MANAGE_EXTERNAL_STORAGE
//                    )) {
//                // Show an explanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//            } else {
//                // No explanation needed; request the permission
//                ActivityCompat.requestPermissions(
//                        this,
//                        arrayOf(Manifest.permission.MANAGE_EXTERNAL_STORAGE),
//                        WRITE_EXTERNAL_STORAGE
//                );
//            }
//        }
//    }


    private fun createPermissionViewDataModel(): PermissionViewDataModel {
        val permissionDescriptiveDataModels = ArrayList<PermissionDescriptiveDataModel>()

//        Camera Permission
        val permissionDescriptiveDataModel_camera = PermissionDescriptiveDataModel()
        permissionDescriptiveDataModel_camera.permissionName = this.getString(R.string.camera)
        permissionDescriptiveDataModel_camera.permissionDescription = this.getString(R.string.camera_description)
        permissionDescriptiveDataModel_camera.permissionDrawableName = "ic_baseline_camera_alt_24"

//        File Permission
        val permissionDescriptiveDataModel_files = PermissionDescriptiveDataModel()
        permissionDescriptiveDataModel_files.permissionName = this.getString(R.string.file)
        permissionDescriptiveDataModel_files.permissionDescription = this.getString(R.string.file_description)
        permissionDescriptiveDataModel_files.permissionDrawableName = "ic_baseline_folder_open_24"
        //        permissionDescriptiveDataModels.add(permissionDescriptiveDataModel_camera);
        permissionDescriptiveDataModels.add(permissionDescriptiveDataModel_camera)
        permissionDescriptiveDataModels.add(permissionDescriptiveDataModel_files)
        permissionViewDataModel = PermissionViewDataModel()
        permissionViewDataModel!!.headerText = this.getString(R.string.permission_text)
        permissionViewDataModel!!.subHeaderText = this.getString(R.string.permission_sub_text)
        permissionViewDataModel!!.noteText = this.getString(R.string.permission_note)
        permissionViewDataModel!!.leftBtnText = this.getString(R.string.setting)
        permissionViewDataModel!!.rightBtnText = this.getString(R.string.ok)
        permissionViewDataModel!!.permissionTypeDataArrayList = permissionDescriptiveDataModels
        return permissionViewDataModel as PermissionViewDataModel
    }
}
