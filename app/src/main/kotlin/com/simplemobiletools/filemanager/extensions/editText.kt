package com.simplemobiletools.filemanager.extensions

import android.widget.EditText

val EditText.value: String get() = this.text.toString().trim()
