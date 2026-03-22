package com.myname.jarvisai.automation

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log

/**
 * Contact Management - Voice-based contact operations
 */
class ContactManager(private val context: Context) {

    companion object {
        private const val TAG = "ContactManager"
    }

    /**
     * Find contact by name
     */
    fun findContact(name: String): Contact? {
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactName = it.getString(0)
                val phoneNumber = it.getString(1)
                val photoUri = it.getString(2)
                
                return Contact(contactName, phoneNumber, photoUri)
            }
        }
        
        return null
    }

    /**
     * Call contact by name
     */
    fun callContact(name: String): Result {
        val contact = findContact(name)
        
        return if (contact != null) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${contact.phoneNumber}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Result.Success("Calling ${contact.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Call failed: ${e.message}")
                Result.Error("Failed to call: ${e.message}")
            }
        } else {
            Result.Error("Contact '$name' not found")
        }
    }

    /**
     * Send SMS to contact
     */
    fun sendSmsToContact(name: String, message: String): Result {
        val contact = findContact(name)
        
        return if (contact != null) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:${contact.phoneNumber}")
                    putExtra("sms_body", message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Result.Success("Opening SMS to ${contact.name}")
            } catch (e: Exception) {
                Result.Error("Failed to open SMS: ${e.message}")
            }
        } else {
            Result.Error("Contact '$name' not found")
        }
    }

    /**
     * Open WhatsApp chat with contact
     */
    fun whatsappContact(name: String, message: String = ""): Result {
        val contact = findContact(name)
        
        return if (contact != null) {
            try {
                val phoneNumber = contact.phoneNumber.replace("+", "").replace(" ", "")
                val url = if (message.isNotEmpty()) {
                    "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"
                } else {
                    "https://wa.me/$phoneNumber"
                }
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Result.Success("Opening WhatsApp chat with ${contact.name}")
            } catch (e: Exception) {
                Result.Error("WhatsApp failed: ${e.message}")
            }
        } else {
            Result.Error("Contact '$name' not found")
        }
    }

    /**
     * Search all contacts
     */
    fun searchContacts(query: String): List<Contact> {
        val contacts = mutableListOf<Contact>()
        
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$query%"),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val number = it.getString(1)
                val photoUri = it.getString(2)
                
                contacts.add(Contact(name, number, photoUri))
            }
        }
        
        return contacts
    }

    /**
     * Contact data class
     */
    data class Contact(
        val name: String,
        val phoneNumber: String,
        val photoUri: String?
    )

    /**
     * Result sealed class
     */
    sealed class Result {
        data class Success(val message: String) : Result()
        data class Error(val message: String) : Result()
    }
}
