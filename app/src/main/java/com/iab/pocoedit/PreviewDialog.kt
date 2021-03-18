package com.iab.photoeditor

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.iab.imagetext.model.ImageTextDataModel
import com.iab.pocoedit.ExifUtils.getImageInfoDataModel
import com.iab.pocoedit.MainActivity.RequestCode.ACTION_REQUEST_EDITIMAGE
import com.iab.pocoedit.R
import iamutkarshtiwari.github.io.ananas.editimage.EditImageActivity
import iamutkarshtiwari.github.io.ananas.editimage.ImageEditorIntentBuilder
import kotlinx.android.synthetic.main.preview_dialog_layout.*
import java.io.File
import java.lang.ref.WeakReference


class PreviewDialog(context: Context, private var imageTextDataModel: ImageTextDataModel, private var previewImageDialogInterface: PreviewImageDialogInterface) : DialogFragment() {
    private var weakReferenceContext = WeakReference(context)
    private var fullPath:String? = null
    companion object {
        fun newInstance(context: Context, imageTextDataModel: ImageTextDataModel, previewImageDialogInterface: PreviewImageDialogInterface, title:String?): PreviewDialog {
            val frag = PreviewDialog(context, imageTextDataModel, previewImageDialogInterface)
            val args = Bundle()
            args.putString("title", title)
            frag.arguments = args
            return frag
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.preview_dialog_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
        )
    }

    //    local functions of the class
    private fun setupView() {
        weakReferenceContext.get()?.let {
            Glide.with(it)
                    .load(imageTextDataModel.imageUri)
                    .into(previewview_imageView)
        }

        imageTextDataModel.text?.let { preview_file_name_textView.text = imageTextDataModel.text!!.substring(0, imageTextDataModel.text!!.indexOf('.')) }
        preview_bucket_name_textView.text = imageTextDataModel.bucketName

        fullPath = weakReferenceContext.get()?.let {
            imageTextDataModel.imageUri?.let { it2 ->  getRealPathFromURI(it2, it)
            }
        }
    }

    private fun setupClickListeners() {
        previewview_imageView.setOnClickListener {
            checkInfoMotionLayoutAndClose()
        }
        preview_back_btn.setOnClickListener { onBackPress() }
        preview_share_btn.setOnClickListener { shareImage(imageTextDataModel.imageUri?.let { it1 -> getRealPathFromURI(it1, weakReferenceContext.get()!!) }) }
        preview_edit_btn.setOnClickListener { editImage() }
        preview_del_btn.setOnClickListener { deleteImageAndCloseDialog() }
        preview_info_btn.setOnClickListener { infoOfImage() }
    }

    private fun shareImage(imagePath: String?){
        checkInfoMotionLayoutAndClose()
        imagePath?.let {
            weakReferenceContext.get()?.let { it2 ->
                val intent = Intent(Intent.ACTION_SEND)
                val file = File(it)
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_SUBJECT, it2.resources.getString(R.string.app_name))
                val apkURI = FileProvider.getUriForFile(
                        it2, it2.packageName + ".provider", file)
                val resolvedIntentActivities: List<ResolveInfo> = it2.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                for (resolvedIntentInfo in resolvedIntentActivities) {
                    val packageName = resolvedIntentInfo.activityInfo.packageName
                    it2.grantUriPermission(packageName, apkURI, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                intent.putExtra(Intent.EXTRA_STREAM, apkURI)
                this.startActivity(intent)
            }
        }
    }

    private fun onBackPress(){
        this.dismiss()
    }

    private fun editImage() {
        checkInfoMotionLayoutAndClose()
//                    getRealPathFromURI(data!!.data!!, this)
        weakReferenceContext.get()?.let {
            val intent = ImageEditorIntentBuilder(weakReferenceContext.get()!!,
                    imageTextDataModel.imageUri?.let { it1 -> getRealPathFromURI(it1, weakReferenceContext.get()!!) }, getTempFilename(weakReferenceContext.get()!!))
                    .withAddText()
                    .withPaintFeature()
                    .withFilterFeature()
                    .withRotateFeature()
                    .withCropFeature()
//                    .withBrightnessFeature()
//                    .withSaturationFeature()
//                    .withBeautyFeature()
//                    .withStickerFeature()
                    .withEditorTitle("Photo Editor")
                    .forcePortrait(true)
                    .setSupportActionBarVisibility(false)
                    .build()

            EditImageActivity.start(weakReferenceContext.get() as Activity, intent, ACTION_REQUEST_EDITIMAGE)
            this.dismiss()
        }
    }

    private fun infoOfImage() {
        checkInfoMotionLayout()
        val fullPath = weakReferenceContext.get()?.let {
            imageTextDataModel.imageUri?.let { it2 ->  getRealPathFromURI(it2, it)
            }
        }
        val fileName = imageTextDataModel.text?.let { it.substring(0, it.indexOf('.')) }
        val imageInfoDataModel: ImageInfoDataModel = getImageInfoDataModel(fullPath, imageTextDataModel.bucketName, fileName)
        name_textView.text = imageInfoDataModel.name
        size_textView.text = imageInfoDataModel.size
        dim_textView.text = imageInfoDataModel.dimension.getStringXY()
        date_time_textView.text = imageInfoDataModel.getDate()
        bucket_name_textView.text = imageInfoDataModel.bucketName
        full_path_textView.text = imageInfoDataModel.fullPath
    }

    private fun deleteImageAndCloseDialog(){
        fullPath?.let { it->
            weakReferenceContext.get()?.let { it2 ->
                // Set up the projection (we only need the ID)
                // Set up the projection (we only need the ID)
                val projection = arrayOf(MediaStore.Images.Media._ID)

                // Match on the file path

                // Match on the file path
                val selection = MediaStore.Images.Media.DATA + " = ?"
                val selectionArgs = arrayOf<String>(it)

                // Query for the ID of the media matching the file path

                // Query for the ID of the media matching the file path
                val queryUri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val contentResolver: ContentResolver = it2.getContentResolver()
                val c: Cursor? = contentResolver.query(queryUri, projection, selection, selectionArgs, null)
                c?.let { it3 ->
                    if (it3.moveToFirst()) {
                        // We found the ID. Deleting the item via the content provider will also remove the file
                        val id: Long = it3.getLong(it3.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                        val deleteUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        contentResolver.delete(deleteUri, null, null)
                        previewImageDialogInterface.onImageDeleteSuccessFull(imageTextDataModel.id)
                        this.dismiss()
                    } else {
                        // File not found in media store DB
                    }
                    it3.close()
                }
            }
        }



//        try {
//            // 1
//            weakReferenceContext.get()?.let { it ->
//                imageTextDataModel.imageUri?.let {it2->
//                    it.contentResolver.delete(
//                            it2,"${MediaStore.Images.Media._ID} = ?",
//                            arrayOf(image.id.toString()) )
//                }
//            }
//        }
//// 2
//        catch (securityException: SecurityException) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
//                val recoverableSecurityException =
//                        securityException as? RecoverableSecurityException
//                                ?: throw securityException
//                pendingDeleteImage = image
//                _permissionNeededForDelete.postValue(
//                        recoverableSecurityException.userAction.actionIntent.intentSender
//                )
//            } else {
//                throw securityException
//            }
//        }
    }

    fun checkInfoMotionLayout(){
        if(preview_motion_layout.progress == 0.0f){
            preview_motion_layout.transitionToEnd()
        }else{
            preview_motion_layout.transitionToStart()
        }
    }

    fun checkInfoMotionLayoutAndClose(){
        if(preview_motion_layout.progress != 0.0f){
            preview_motion_layout.transitionToStart()
        }
    }

}