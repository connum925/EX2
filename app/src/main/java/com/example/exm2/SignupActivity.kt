package com.example.exm2

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

class SignupActivity : AppCompatActivity() {

    private lateinit var signupName: EditText
    private lateinit var signupEmail: EditText
    private lateinit var signupUsername: EditText
    private lateinit var signupPassword: EditText
    private lateinit var loginRedirectText: TextView
    private lateinit var signupButton: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        signupName = findViewById(R.id.signup_name)
        signupEmail = findViewById(R.id.signup_email)
        signupUsername = findViewById(R.id.signup_username)
        signupPassword = findViewById(R.id.signup_password)
        loginRedirectText = findViewById(R.id.loginRedirectText)
        signupButton = findViewById(R.id.signup_button)

        signupButton.setOnClickListener {
            database = FirebaseDatabase.getInstance()
            reference = database.getReference("users")

            val name = signupName.text.toString()
            val email = signupEmail.text.toString()
            val username = signupUsername.text.toString()
            val password = signupPassword.text.toString()

            val helperClass = HelperClass(name, email, username, password)
            reference.child(username).setValue(helperClass).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "You have signup successfully!", Toast.LENGTH_SHORT).show()
                    // Get and send the FCM token after successful signup
                    getAndSendFCMToken(username)
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        loginRedirectText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun getAndSendFCMToken(username: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }


            // Get new FCM registration token
            val token = task.result


            // Log and toast (for debugging)
            val msg = getString(R.string.msg_token_fmt, token)
            Log.d(TAG, msg)
            Toast.makeText(baseContext, token, Toast.LENGTH_SHORT).show()


            // Send the token to your server
            sendTokenToServer(username, token)
        })
    }

    private fun sendTokenToServer(username: String, token: String?) {
        val queue = Volley.newRequestQueue(this)
        val url = "http://10.0.2.2:3000/register" //  Replace with your server URL (see note below)
        //is a special alias that the Android emulator uses to refer to your development machine's localhost.

        val jsonBody = JSONObject()
        jsonBody.put("username", username)
        jsonBody.put("fcm_token", token)


        val request = JsonObjectRequest(
            Request.Method.POST, url, jsonBody,
            { response ->
                Log.d(TAG, "Token sent to server: $response")
            },
            { error ->
                Log.e(TAG, "Error sending token to server: $error")
                Toast.makeText(baseContext, "Failed to send token", Toast.LENGTH_SHORT).show()
            })


        queue.add(request)
    }
    companion object {
        private const val TAG = "SignupActivity"
    }
}