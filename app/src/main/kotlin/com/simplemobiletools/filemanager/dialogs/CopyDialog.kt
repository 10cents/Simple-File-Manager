package com.simplemobiletools.filemanager.dialogs

import android.support.v4.util.Pair
import android.support.v7.app.AlertDialog
import com.simplemobiletools.commons.asynctasks.CopyMoveTask
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.filemanager.R
import com.simplemobiletools.filemanager.activities.SimpleActivity
import com.simplemobiletools.filemanager.extensions.config
import kotlinx.android.synthetic.main.dialog_copy_item.view.*
import java.io.File
import java.util.*

class CopyDialog(val activity: SimpleActivity, val files: ArrayList<File>, val copyMoveListener: CopyMoveTask.CopyMoveListener) {

    init {
        val context = activity
        val view = activity.layoutInflater.inflate(R.layout.dialog_copy_item, null)
        val sourcePath = files[0].parent.trimEnd('/')
        var destinationPath = ""

        val config = context.config
        view.destination.setOnClickListener {
            FilePickerDialog(activity, sourcePath, false, config.showHidden, true) {
                destinationPath = it
                view.destination.text = context.humanizePath(it)
            }
        }

        AlertDialog.Builder(context)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create().apply {
            activity.setupDialogStuff(view, this, R.string.copy_move)
            getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener({
                if (destinationPath == context.resources.getString(R.string.select_destination) || destinationPath.isEmpty()) {
                    context.toast(R.string.please_select_destination)
                    return@setOnClickListener
                }

                if (sourcePath == destinationPath.trimEnd('/')) {
                    context.toast(R.string.source_and_destination_same)
                    return@setOnClickListener
                }

                val destinationDir = File(destinationPath)
                if (!destinationDir.exists()) {
                    context.toast(R.string.invalid_destination)
                    return@setOnClickListener
                }

                if (files.size == 1) {
                    if (File(destinationPath, files[0].name).exists()) {
                        context.toast(R.string.name_taken)
                        return@setOnClickListener
                    }
                }

                if (activity.isShowingPermDialog(destinationDir)) {
                    return@setOnClickListener
                }

                if (view.dialog_radio_group.checkedRadioButtonId == R.id.dialog_radio_copy) {
                    context.toast(R.string.copying)
                    val pair = Pair<ArrayList<File>, File>(files, destinationDir)
                    CopyMoveTask(context, false, config.treeUri, false, copyMoveListener).execute(pair)
                    dismiss()
                } else {
                    if (context.isPathOnSD(sourcePath) || context.isPathOnSD(destinationPath)) {
                        if (activity.isShowingPermDialog(files[0])) {
                            return@setOnClickListener
                        }

                        context.toast(R.string.moving)
                        val pair = Pair<ArrayList<File>, File>(files, destinationDir)
                        CopyMoveTask(context, true, config.treeUri, false, copyMoveListener).execute(pair)
                        dismiss()
                    } else {
                        val updatedFiles = ArrayList<File>(files.size * 2)
                        updatedFiles.addAll(files)
                        for (file in files) {
                            val destination = File(destinationDir, file.name)
                            if (file.renameTo(destination))
                                updatedFiles.add(destination)
                        }

                        context.scanFiles(updatedFiles) {
                            activity.runOnUiThread {
                                copyMoveListener.copySucceeded(true, files.size * 2 == updatedFiles.size)
                                dismiss()
                            }
                        }
                    }
                }
            })
        }

        view.destination.performClick()
    }
}
