package ru.vasiliev.sandbox.security

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object SecureFileProvider {

    const val FILEPROVIDER_SECURE_IMAGE_DIR = "images"
    private const val FILEPROVIDER_AUTHORITY_DOMAIN_POSTFIX = "fileprovider"

    fun getSecureImageUri(context: Context, imageFile: File): Uri {
        return FileProvider.getUriForFile(context, getAuthority(context), imageFile)
    }

    fun getAuthority(context: Context): String {
        return context.packageName + "." + FILEPROVIDER_AUTHORITY_DOMAIN_POSTFIX
    }

}