package com.mv.livebodyexample

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aronc.aisllibrary.activity.LocalDetectActivity
import com.aronc.aisllibrary.utils.FileUtils
import java.util.*

class LocalRecordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_record)
        findViewById<AppCompatButton>(R.id.btn_start).setOnClickListener {
            checkPermission()
        }
    }

    private fun checkPermission() {
        val permissionList = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        val deniPermissionList = mutableListOf<String>()
        for (permission in permissionList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                deniPermissionList.add(permission)
            }
        }
        if (deniPermissionList.isEmpty()) {
            chooseImage()
        } else {
            val requestPermissionArray = deniPermissionList.toTypedArray()
            ActivityCompat.requestPermissions(this, requestPermissionArray, 1001)
        }
    }

    private fun gotoRecord(picPath: String) {
        Intent(this, LocalDetectActivity::class.java).apply {
            putExtra("picPath", picPath)
        }.also {
            startActivity(it)
        }
    }

    private fun chooseImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 20002)
    }

    @SuppressLint("Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 20002) {
            if (data != null) {
                val uri = data.data
                if (!uri?.authority.isNullOrEmpty()) {
                    val cursor = uri?.let {
                        contentResolver.query(
                            it,
                            arrayOf(MediaStore.Images.Media.DATA),
                            null, null, null
                        )
                    }
                    if (cursor == null) return
                    cursor.moveToFirst()
                    val picPath =
                        cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                    gotoRecord(
                        FileUtils.compressImagePixels(
                            this@LocalRecordActivity,
                            picPath
                        ).absolutePath
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            for (permission in grantResults) {
                if (permission != PackageManager.PERMISSION_GRANTED) {
                    checkPermission()
                    return
                }
            }
            chooseImage()
        }
    }
}