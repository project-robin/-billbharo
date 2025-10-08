package com.billbharo.domain.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for sharing invoices via WhatsApp and other apps
 * 
 * Features:
 * - Direct WhatsApp sharing
 * - General share intent
 * - FileProvider URI handling
 * - PDF file validation
 */
@Singleton
class ShareHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Share PDF invoice via WhatsApp
     * @param pdfFilePath Absolute path to the PDF file
     * @param phoneNumber Optional WhatsApp phone number (format: +919876543210)
     * @return Result indicating success or failure
     */
    fun shareViaWhatsApp(pdfFilePath: String, phoneNumber: String? = null): Result<Unit> {
        return try {
            val file = File(pdfFilePath)
            if (!file.exists()) {
                return Result.failure(Exception("PDF file not found"))
            }

            // Get URI using FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Please find the invoice attached.")
                putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${file.nameWithoutExtension}")
                
                // Try to target WhatsApp directly
                if (phoneNumber != null) {
                    // Format: https://wa.me/919876543210
                    val whatsappUrl = "https://wa.me/$phoneNumber"
                    setPackage("com.whatsapp")
                    data = Uri.parse(whatsappUrl)
                } else {
                    setPackage("com.whatsapp")
                }
                
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Check if WhatsApp is installed
            if (isWhatsAppInstalled()) {
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                // Fall back to general share if WhatsApp not installed
                shareViaIntent(pdfFilePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share PDF via general Android share sheet
     * @param pdfFilePath Absolute path to the PDF file
     * @return Result indicating success or failure
     */
    fun shareViaIntent(pdfFilePath: String): Result<Unit> {
        return try {
            val file = File(pdfFilePath)
            if (!file.exists()) {
                return Result.failure(Exception("PDF file not found"))
            }

            // Get URI using FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Invoice: ${file.nameWithoutExtension}")
                putExtra(Intent.EXTRA_SUBJECT, "Invoice")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Create chooser
            val chooser = Intent.createChooser(intent, "Share Invoice").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Share invoice to specific WhatsApp contact
     * Opens WhatsApp with specific contact selected
     */
    fun shareToWhatsAppContact(pdfFilePath: String, phoneNumber: String): Result<Unit> {
        return try {
            val file = File(pdfFilePath)
            if (!file.exists()) {
                return Result.failure(Exception("PDF file not found"))
            }

            // Remove any formatting from phone number (spaces, dashes, etc.)
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")

            // Get URI using FileProvider
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Here is your invoice. Thank you!")
                putExtra("jid", "$cleanNumber@s.whatsapp.net") // WhatsApp contact ID format
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (isWhatsAppInstalled()) {
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                Result.failure(Exception("WhatsApp is not installed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Open PDF file with default PDF viewer
     */
    fun openPdf(pdfFilePath: String): Result<Unit> {
        return try {
            val file = File(pdfFilePath)
            if (!file.exists()) {
                return Result.failure(Exception("PDF file not found"))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if WhatsApp is installed on device
     */
    fun isWhatsAppInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if WhatsApp Business is installed
     */
    fun isWhatsAppBusinessInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.whatsapp.w4b", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Share via WhatsApp Business
     */
    fun shareViaWhatsAppBusiness(pdfFilePath: String): Result<Unit> {
        return try {
            val file = File(pdfFilePath)
            if (!file.exists()) {
                return Result.failure(Exception("PDF file not found"))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Invoice attached.")
                setPackage("com.whatsapp.w4b")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (isWhatsAppBusinessInstalled()) {
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                Result.failure(Exception("WhatsApp Business is not installed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
