package com.greendome.adhkar.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.greendome.adhkar.R

fun copyTextToClipboard(context: Context, text: String, label: String? = null) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label ?: "text", text))
    Toast.makeText(context, R.string.azkar_text_copied, Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String, chooserTitle: String? = null) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
}
