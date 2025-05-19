package com.example.exm2.ui.slideshow

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.exm2.HelperClass
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.Exception
import com.example.exm2.SignupActivity
import org.json.JSONObject
import android.app.Application // Import Application

class SlideshowViewModel(application: Application) : ViewModel() { // Modify the constructor


    private val _users = MutableLiveData<List<HelperClass>>()
    val users: LiveData<List<HelperClass>> = _users


    private val _isAdmin = MutableLiveData<Boolean>()
    val isAdmin: LiveData<Boolean> = _isAdmin


    private val _notificationSent = MutableLiveData<Boolean>()
    private val _resetNotificationSent = MutableLiveData<Boolean>()
    val notificationSent: LiveData<Boolean> = _resetNotificationSent


    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading


    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage


    private var databaseReference: DatabaseReference? = null
    private var currentUsername: String? = null


    //  New LiveData to trigger logout
    private val _shouldLogout = MutableLiveData<Boolean>()
    val shouldLogout: LiveData<Boolean> = _shouldLogout


    //  Add this to your ViewModel:
    @SuppressLint("StaticFieldValueLeak")
    private var application: Application = application // Store the application context


    fun setDatabaseReference(ref: DatabaseReference, username: String?) {
        databaseReference = ref
        currentUsername = username
        checkAdminStatus()
    }


    private fun checkAdminStatus() {
        // Replace "admin" with your actual admin check logic
        _isAdmin.value = currentUsername == "admin"
    }


    fun fetchUsers() {
        if (databaseReference == null || currentUsername == null) {
            _errorMessage.value = "Database reference or username not initialized."
            return
        }


        _isLoading.value = true


        if (isAdmin.value == true) {
            viewModelScope.launch {
                try {
                    val snapshot = databaseReference!!.get().await()
                    val userList = mutableListOf<HelperClass>()
                    for (childSnapshot in snapshot.children) {
                        val user = childSnapshot.getValue(HelperClass::class.java)
                        user?.let { userList.add(it) }
                    }
                    _users.value = userList
                    _isLoading.value = false
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching users: ${e.message}"
                    Log.e("SlideshowViewModel", "Error fetching users", e)
                    _isLoading.value = false
                }
            }
        } else {
            viewModelScope.launch {
                try {
                    //  Retrieve user data directly using the username as the key
                    val snapshot = databaseReference!!.child(currentUsername!!).get().await()
                    val user = snapshot.getValue(HelperClass::class.java)
                    user?.let { _users.value = listOf(it) }
                    _isLoading.value = false
                } catch (e: Exception) {
                    _errorMessage.value = "Error fetching your data: ${e.message}"
                    Log.e("SlideshowViewModel", "Error fetching user data", e)
                    _isLoading.value = false
                }
            }
        }
    }


    fun updateUser(user: HelperClass, originalUsername: String) {
        if (databaseReference == null || originalUsername.isEmpty()) {
            _errorMessage.value = "Database reference or original username not initialized.UpdtError."
            return
        }

        //  **NEW: Validation Check**
        if (user.name.isNullOrEmpty() || user.email.isNullOrEmpty() ||
            user.username.isNullOrEmpty() || user.password.isNullOrEmpty()) {
            _errorMessage.value = "All fields are required."
            return
        }
        _isLoading.value = true


        viewModelScope.launch {
            try {
                //  Crucially, use the originalUsername to update the correct node!
                val userUpdates = HashMap<String, Any>()
                userUpdates["name"] = user.name ?: ""
                userUpdates["email"] = user.email ?: ""
                userUpdates["username"] = user.username ?: ""
                userUpdates["password"] = user.password ?: ""


                if (originalUsername != user.username) {
                    // Username is being changed!
                    // 1. Write data to the new username location
                    user.username?.let { databaseReference!!.child(it).setValue(user).await() }


                    // 2. Delete the old username's data
                    databaseReference!!.child(originalUsername).removeValue().await()
                } else {
                    // Only updating other fields
                    databaseReference!!.child(originalUsername).updateChildren(userUpdates).await()
                }


                _resetNotificationSent.value = true
                _isLoading.value = false


                //  Signal to the Fragment to logout
                _shouldLogout.postValue(true)  // Use postValue from background thread


            } catch (e: Exception) {
                _errorMessage.value = "Error updating user: ${e.message}"
                Log.e("SlideshowViewModel", "Error updating user", e)
                _isLoading.value = false
            }
        }
    }


    fun sendPushNotification(title: String, message: String) {
        if (isAdmin.value == true) {
            _isLoading.value = true
            //  **IMPORTANT:** Implement your server-side FCM logic here!
            //  You CANNOT reliably send FCM messages directly from the Android app.
            //  This is a placeholder!
            Log.d("SlideshowViewModel", "Sending push notification: Title='$title', Message='$message'")
            triggerServerToSendNotification(title, message)
            _resetNotificationSent.value = true
            _isLoading.value = false
        } else {
            _errorMessage.value = "You are not authorized to send notifications."
        }
    }


    private fun triggerServerToSendNotification(title: String, message: String) {
        val queue = Volley.newRequestQueue(application) //  Use the stored Application context
        val url = "http://10.0.2.2:3000/send-notification" //  Replace with your server URL


        val jsonBody = JSONObject()
        jsonBody.put("title", title)
        jsonBody.put("body", message)


        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                Log.d(TAG, "Notification request to server successful: $response")
            },
            { error ->
                Log.e(TAG, "Notification request to server failed: $error")
                _errorMessage.postValue("Failed to send notification")
            })


        queue.add(request)
    }


    fun resetNotificationSent() {
        _resetNotificationSent.value = false
    }


    fun resetShouldLogout() {
        _shouldLogout.value = false
    }


    companion object {
        private const val TAG = "SlideshowViewModel"
    }
}