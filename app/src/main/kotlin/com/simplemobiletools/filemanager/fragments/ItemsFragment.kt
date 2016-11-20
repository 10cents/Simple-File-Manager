package com.simplemobiletools.filemanager.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import com.simplemobiletools.filemanager.Config
import com.simplemobiletools.filemanager.PATH
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.adapters.ItemsAdapter
import com.simplemobiletools.filemanager.dialogs.CopyDialog
import com.simplemobiletools.filemanager.dialogs.CreateNewItemDialog
import com.simplemobiletools.filepicker.asynctasks.CopyMoveTask
import com.simplemobiletools.filepicker.extensions.*
import com.simplemobiletools.filepicker.models.FileDirItem
import com.simplemobiletools.filepicker.views.RecyclerViewDivider
import kotlinx.android.synthetic.main.items_fragment.*
import java.io.File
import java.util.*

class ItemsFragment : android.support.v4.app.Fragment(), AdapterView.OnItemClickListener, ItemsAdapter.ItemOperationsListener, View.OnTouchListener {
    private var mListener: ItemInteractionListener? = null
    private var mSnackbar: Snackbar? = null

    lateinit var mItems: List<FileDirItem>
    lateinit var mConfig: Config
    lateinit var mToBeDeleted: MutableList<String>

    private var mPath = ""
    private var mShowHidden = false

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.items_fragment, container, false)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mConfig = Config.newInstance(context)
        mShowHidden = mConfig.showHidden
        mItems = ArrayList<FileDirItem>()
        mToBeDeleted = ArrayList<String>()
        fillItems()

        items_swipe_refresh.setOnRefreshListener({ fillItems() })
        items_fab.setOnClickListener { createNewItem() }
    }

    override fun onResume() {
        super.onResume()
        if (mShowHidden != mConfig.showHidden) {
            mShowHidden = !mShowHidden
            fillItems()
        }
    }

    override fun onPause() {
        super.onPause()
        deleteItems()
    }

    private fun fillItems() {
        mPath = arguments.getString(PATH)
        val newItems = getItems(mPath)
        Collections.sort(newItems)
        items_swipe_refresh.isRefreshing = false
        if (newItems.toString() == mItems.toString()) {
            return
        }

        mItems = newItems

        val adapter = ItemsAdapter(activity as SimpleActivity, mItems, this) {

        }
        items_list.adapter = adapter
        items_list.addItemDecoration(RecyclerViewDivider(context))
        items_list.setOnTouchListener(this)
    }

    fun setListener(listener: ItemInteractionListener) {
        mListener = listener
    }

    private fun getItems(path: String): List<FileDirItem> {
        val items = ArrayList<FileDirItem>()
        val files = File(path).listFiles()
        if (files != null) {
            for (file in files) {
                val curPath = file.absolutePath
                val curName = curPath.getFilenameFromPath()
                if (!mShowHidden && curName.startsWith("."))
                    continue

                if (mToBeDeleted.contains(curPath))
                    continue

                val children = getChildren(file)
                val size = file.length()

                items.add(FileDirItem(curPath, curName, file.isDirectory, children, size))
            }
        }
        return items
    }

    private fun getChildren(file: File): Int {
        if (file.listFiles() == null)
            return 0

        if (file.isDirectory) {
            return if (mShowHidden) {
                file.listFiles().size
            } else {
                file.listFiles { file -> !file.isHidden }.size
            }
        }
        return 0
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val item = mItems[position]
        if (item.isDirectory) {
            mListener?.itemClicked(item)
        } else {
            val path = item.path
            val file = File(path)
            var mimeType: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(path.getFilenameExtension().toLowerCase())
            if (mimeType == null)
                mimeType = "text/plain"

            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.fromFile(file), mimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                try {
                    startActivity(this)
                } catch (e: ActivityNotFoundException) {
                    if (!tryGenericMimeType(this, mimeType!!, file)) {
                        context.toast(R.string.no_app_found)
                    }
                }
            }
        }
    }

    private fun tryGenericMimeType(intent: Intent, mimeType: String, file: File): Boolean {
        val genericMimeType = getGenericMimeType(mimeType)
        intent.setDataAndType(Uri.fromFile(file), genericMimeType)
        return try {
            startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun createNewItem() {
        CreateNewItemDialog(context, mPath, object : CreateNewItemDialog.OnCreateNewItemListener {
            override fun onSuccess() {
                fillItems()
            }
        })
    }

    private fun getGenericMimeType(mimeType: String): String {
        if (!mimeType.contains("/"))
            return mimeType

        val type = mimeType.substring(0, mimeType.indexOf("/"))
        return type + "/*"
    }

    /*override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val menuItem = menu.findItem(R.id.cab_rename)
        menuItem.isVisible = mSelectedItemsCnt == 1
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.cab_rename -> {
                displayRenameDialog()
                mode.finish()
            }
            R.id.cab_copy -> {
                displayCopyDialog()
                mode.finish()
            }
            R.id.cab_delete -> {
                prepareForDeleting()
                mode.finish()
            }
            else -> return false
        }

        return true
    }*/

    private fun displayCopyDialog() {
        val fileIndexes = getSelectedItemIndexes()
        if (fileIndexes.isEmpty())
            return

        val files = ArrayList<File>(fileIndexes.size)
        fileIndexes.mapTo(files) { File(mItems[it].path) }

        CopyDialog(activity as SimpleActivity, files, object : CopyMoveTask.CopyMoveListener {
            override fun copySucceeded(deleted: Boolean, copiedAll: Boolean) {
                if (deleted) {
                    context.toast(if (copiedAll) R.string.moving_success else R.string.moving_success_partial)
                } else {
                    context.toast(if (copiedAll) R.string.copying_success else R.string.copying_success_partial)
                }
                fillItems()
            }

            override fun copyFailed() {
                context.toast(R.string.copy_move_failed)
            }
        })
    }

    private fun getSelectedItem(): FileDirItem? {
        val itemIndexes = getSelectedItemIndexes()
        if (itemIndexes.isEmpty())
            return null

        val itemIndex = itemIndexes[0]
        return mItems[itemIndex]
    }

    private fun getSelectedItemIndexes(): List<Int> {
        /*val items = items_list.checkedItemPositions
        val cnt = items.size()
        val selectedItems = (0..cnt - 1)
                .filter { items.valueAt(it) }
                .map { items.keyAt(it) }
        return selectedItems*/
        return ArrayList()
    }

    private fun prepareForDeleting() {
        mToBeDeleted.clear()
        /*val items = items_list.checkedItemPositions
        val cnt = items.size()
        var deletedCnt = 0
        for (i in 0..cnt - 1) {
            if (items.valueAt(i)) {
                val id = items.keyAt(i)
                val path = mItems[id].path
                mToBeDeleted.add(path)
                deletedCnt++
            }
        }

        notifyDeletion(deletedCnt)*/
    }

    private fun notifyDeletion(cnt: Int) {
        val res = resources
        val msg = res.getQuantityString(R.plurals.items_deleted, cnt, cnt)
        mSnackbar = Snackbar.make(items_holder, msg, Snackbar.LENGTH_INDEFINITE)
        mSnackbar!!.apply {
            setAction(res.getString(R.string.undo), undoDeletion)
            setActionTextColor(Color.WHITE)
            show()
        }
        fillItems()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (mSnackbar != null && mSnackbar!!.isShown) {
            deleteItems()
        }

        return false
    }

    private fun deleteItems() {
        if (mToBeDeleted.isEmpty())
            return

        mSnackbar?.dismiss()
        mToBeDeleted
                .map(::File)
                .filter(File::exists)
                .forEach { deleteItem(it) }

        mToBeDeleted.clear()
    }

    private fun deleteItem(item: File) {
        if (item.isDirectory) {
            for (child in item.listFiles()) {
                deleteItem(child)
            }
        }

        if (context.needsStupidWritePermissions(item.absolutePath)) {
            val document = context.getFileDocument(item.absolutePath, mConfig.treeUri)
            document.delete()
        } else {
            item.delete()
        }
    }

    private val undoDeletion = View.OnClickListener {
        mToBeDeleted.clear()
        mSnackbar!!.dismiss()
        fillItems()
    }

    override fun refreshItems() {
        fillItems()
    }

    interface ItemInteractionListener {
        fun itemClicked(item: FileDirItem)
    }
}
