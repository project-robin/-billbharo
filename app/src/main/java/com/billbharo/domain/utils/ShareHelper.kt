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
 * A helper class for sharing files, particularly PDF invoices, via WhatsApp and other applications.
 *
 * This class handles the complexities of creating share intents, generating content URIs with
 * [FileProvider], and targeting specific apps like WhatsApp.
 *
 * @property context The application context, used for creating intents and accessing the package manager.
 */
@Singleton
class ShareHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Shares a PDF file via WhatsApp.
     *
     * If WhatsApp is not installed, it falls back to the general Android share sheet.
     *
     * @param pdfFilePath The absolute path to the PDF file to be shared.
     * @param phoneNumber An optional WhatsApp phone number (e.g., "+919876543210") to pre-fill.
     * @return A [Result] indicating whether the share intent was successfully launched.
     */
    fun shareViaWhatsApp(pdfFilePath: String, phoneNumber: String? = null): Result<Unit> {
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
                putExtra(Intent.EXTRA_TEXT, "Please find the invoice attached.")
                putExtra(Intent.EXTRA_SUBJECT, "Invoice - ${file.nameWithoutExtension}")

                // Target WhatsApp directly
                if (phoneNumber != null) {
                    val whatsappUrl = "https://wa.me/$phoneNumber"
                    setPackage("com.whatsapp")
                    data = Uri.parse(whatsappUrl)
                } else {
                    setPackage("com.whatsapp")
                }

                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (isWhatsAppInstalled()) {
                context.startActivity(intent)
                Result.success(Unit)
            } else {
                // Fall back to the general share sheet if WhatsApp is not found
                shareViaIntent(pdfFilePath)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shares a PDF file using the general Android share sheet.
     *
     * This allows the user to choose any compatible app to share the file with.
     *
     * @param pdfFilePath The absolute path to the PDF file.
     * @return A [Result] indicating whether the share intent was successfully launched.
     */
    fun shareViaIntent(pdfFilePath: String): Result<Unit> {
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
                putExtra(Intent.EXTRA_TEXT, "Invoice: ${file.nameWithoutExtension}")
                putExtra(Intent.EXTRA_SUBJECT, "Invoice")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

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
     * Shares a PDF file directly to a specific WhatsApp contact.
     *
     * @param pdfFilePath The absolute path to the PDF file.
     * @param phoneNumber The phone number of the contact to share with.
     * @return A [Result] indicating success or failure.
     */
    fun shareToWhatsAppContact(pdfFilePath: String, phoneNumber: String): Result<Unit> {
        return try {
            val file = File(pdfFilePath)
            if (!file.exists()) {
                return Result.failure(Exception("PDF file not found"))
            }

            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
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
                putExtra("jid", "$cleanNumber@s.whatsapp.net") // WhatsApp-specific contact JID
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
     * Opens a PDF file using the default PDF viewer application.
     *
     * @param pdfFilePath The absolute path to the PDF file.
     * @return A [Result] indicating whether the view intent was successfully launched.
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
     * Checks if the standard WhatsApp application is installed on the device.
     *
     * @return `true` if WhatsApp is installed, `false` otherwise.
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
     * Checks if the WhatsApp Business application is installed on the device.
     *
     * @return `true` if WhatsApp Business is installed, `false` otherwise.
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
     * Shares a PDF file via the WhatsApp Business application.
     *
     * @param pdfFilePath The absolute path to the PDF file.
     * @return A [Result] indicating success or failure.
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
