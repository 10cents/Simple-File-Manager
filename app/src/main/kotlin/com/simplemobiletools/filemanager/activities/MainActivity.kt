package com.simplemobiletools.filemanager.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.ActivityCompat
import android.view.Menu
import android.view.MenuItem
import com.simplemobiletools.filemanager.Constants
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.Utils
import com.simplemobiletools.filemanager.fragments.ItemsFragment
import com.simplemobiletools.filepicker.extensions.getInternalPath
import com.simplemobiletools.filepicker.models.FileDirItem
import com.simplemobiletools.filepicker.views.Breadcrumbs
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : SimpleActivity(), ItemsFragment.ItemInteractionListener, Breadcrumbs.BreadcrumbsListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        breadcrumbs.setListener(this)
        tryInitFileManager()
    }

    override fun onResume() {
        super.onResume()
        if (Utils.hasStoragePermission(applicationContext)) {
            val showFullPath = mConfig!!.showFullPath
            if (showFullPath != mShowFullPath)
                initRootFileManager()

            mShowFullPath = showFullPath
        }
    }

    override fun onPause() {
        super.onPause()
        mShowFullPath = mConfig!!.showFullPath
    }

    override fun onDestroy() {
        super.onDestroy()
        mConfig.isFirstRun = false
    }

    private fun tryInitFileManager() {
        if (Utils.hasStoragePermission(applicationContext)) {
            initRootFileManager()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION)
        }
    }

    private fun initRootFileManager() {
        openPath(getInternalPath())
    }

    private fun openPath(path: String) {
        breadcrumbs.setBreadcrumb(path, getInternalPath())
        val bundle = Bundle()
        bundle.putString(Constants.PATH, path)

        val fragment = ItemsFragment()
        fragment.arguments = bundle
        fragment.setListener(this)
        supportFragmentManager.beginTransaction().replace(R.id.fragment_holder, fragment).addToBackStack(path).commitAllowingStateLoss()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
                true
            }
            R.id.about -> {
                startActivity(Intent(applicationContext, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (breadcrumbs.childCount <= 1) {
            if (!mWasBackJustPressed) {
                mWasBackJustPressed = true
                Utils.showToast(applicationContext, R.string.press_back_again)
                Handler().postDelayed({ mWasBackJustPressed = false }, BACK_PRESS_TIMEOUT.toLong())
            } else {
                finish()
            }
        } else {
            breadcrumbs.removeBreadcrumb()
            val item = breadcrumbs.lastItem
            openPath(item.path)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initRootFileManager()
            } else {
                Utils.showToast(applicationContext, R.string.no_permissions)
                finish()
            }
        }
    }

    override fun itemClicked(item: FileDirItem) {
        openPath(item.path)
    }

    override fun breadcrumbClicked(id: Int) {
        val item = breadcrumbs.getChildAt(id).tag as FileDirItem
        openPath(item.path)
    }

    companion object {

        private val STORAGE_PERMISSION = 1
        private val BACK_PRESS_TIMEOUT = 5000

        private var mShowFullPath: Boolean = false
        private var mWasBackJustPressed: Boolean = false
    }
}
