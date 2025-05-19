package com.example.exm2.ui.slideshow

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.exm2.MainActivity
import com.example.exm2.HelperClass
import com.example.exm2.SignupActivity
import com.example.exm2.databinding.FragmentSlideshowBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SlideshowFragment : Fragment() {


    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!


    private lateinit var slideshowViewModel: SlideshowViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var databaseReference: DatabaseReference
    private lateinit var tvUserInfo: TextView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnUpdate: Button
    private lateinit var etNotificationTitle: EditText
    private lateinit var etNotificationMessage: EditText
    private lateinit var btnSendNotification: Button
    private lateinit var progressBar: ProgressBar
    private var originalUsername: String = "" // Store the original username


    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // FCM SDK (and your app) can post notifications.
                Log.d(TAG, "Notification permission granted")
            } else {
                // Inform user that that your app will not show notifications.
                Log.w(TAG, "Notification permission denied")
                // You might want to show a dialog or a snackbar here to explain why notifications are important
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        slideshowViewModel = ViewModelProvider(this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SlideshowViewModel(requireActivity().application) as T
                }
            }).get(SlideshowViewModel::class.java)


        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root


        val username = MainActivity.username  // Access static username


        databaseReference = FirebaseDatabase.getInstance().getReference("users")
        slideshowViewModel.setDatabaseReference(databaseReference, username)


        initializeUI()
        // populateUserData()  // Remove this line
        setupListeners()
        observeViewModelData() //  Important: Observe for updates and errors


        // Fetch user data immediately
        slideshowViewModel.fetchUsers()


        return root
    }


    private fun initializeUI() {
        recyclerView = binding.recyclerViewUsers
        tvUserInfo = binding.tvUserInfo
        etName = binding.etName
        etEmail = binding.etEmail
        etUsername = binding.etUsername
        etPassword = binding.etPassword
        btnUpdate = binding.btnUpdate
        etNotificationTitle = binding.etNotificationTitle
        etNotificationMessage = binding.etNotificationMessage
        btnSendNotification = binding.btnSendNotification
        progressBar = binding.progressBar


        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }


    private fun populateUserData(user: HelperClass) {
        etName.setText(user.name)
        etEmail.setText(user.email)
        etUsername.setText(user.username)
        etPassword.setText(user.password)
        originalUsername = user.username ?: ""
        btnUpdate.isEnabled = true
    }


    private fun setupListeners() {
        btnUpdate.setOnClickListener {
            val updatedUser = HelperClass(
                etName.text.toString(),
                etEmail.text.toString(),
                etUsername.text.toString(),
                etPassword.text.toString()
            )


            //  Crucially, we're still using originalUsername to identify the user
            slideshowViewModel.updateUser(updatedUser, originalUsername)
            progressBarVisibility(true)
        }


        btnSendNotification.setOnClickListener {
            // Request permission before sending notification
            askNotificationPermission()
            slideshowViewModel.sendPushNotification(
                etNotificationTitle.text.toString(),
                etNotificationMessage.text.toString()
            )
            progressBarVisibility(true)
        }
    }


    private fun observeViewModelData() {
        slideshowViewModel.users.observe(viewLifecycleOwner) { users ->
            progressBarVisibility(false)
            updateUIForUserType(users)
        }


        slideshowViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBarVisibility(isLoading)
        }


        slideshowViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            progressBarVisibility(false)
            if (!errorMessage.isNullOrEmpty()) {
                showToast(errorMessage)
            }
        }


        slideshowViewModel.notificationSent.observe(viewLifecycleOwner) { sent ->
            if (sent == true) {
                progressBarVisibility(false)
                showToast("Notification Sent!")
                slideshowViewModel.resetNotificationSent() // Reset the flag
            }
        }
        slideshowViewModel.shouldLogout.observe(viewLifecycleOwner) { shouldLogout ->
            if (shouldLogout == true) {
                logout()
                slideshowViewModel.resetShouldLogout()  // Reset the flag
            }
        }
    }


    private fun updateUIForUserType(users: List<HelperClass>) {
        if (slideshowViewModel.isAdmin.value == true) {
            displayUserListForAdmin(users)
        } else {
            displayUserInfoForRegularUser(users)
            if (users.isNotEmpty()) {
                populateUserData(users.first()) // Populate with fetched data
            }
        }
    }


    private fun displayUserListForAdmin(users: List<HelperClass>) {
        val adapter = UserAdapter(users) { user ->
            populateUpdateFields(user)
        }
        recyclerView.adapter = adapter
        recyclerView.visibility = View.VISIBLE
        tvUserInfo.visibility = View.GONE
        binding.notificationForm.visibility = View.VISIBLE
        binding.userUpdateForm.visibility = View.VISIBLE
    }


    private fun displayUserInfoForRegularUser(users: List<HelperClass>) {
        if (users.isNotEmpty()) {
            val user = users.first()
            tvUserInfo.text = "Name: ${user.name}\nEmail: ${user.email}\nUsername: ${user.username}"
            etName.setText(user.name)
            etEmail.setText(user.email)
            etUsername.setText(user.username)
            etPassword.setText(user.password)
        } else {
            tvUserInfo.text = "No user data found."
            clearInputFields()
        }
        recyclerView.visibility = View.GONE
        tvUserInfo.visibility = View.VISIBLE
        binding.notificationForm.visibility = View.GONE
        binding.userUpdateForm.visibility = View.VISIBLE
    }


    private fun clearInputFields() {
        etName.text.clear()
        etEmail.text.clear()
        etUsername.text.clear()
        etPassword.text.clear()
    }


    private fun toggleAdminViews(isAdmin: Boolean) {
        recyclerView.visibility = if (isAdmin) View.VISIBLE else View.GONE
        binding.notificationForm.visibility = if (isAdmin) View.VISIBLE else View.GONE
    }


    private fun populateUpdateFields(user: HelperClass) {
        etName.setText(user.name)
        etEmail.setText(user.email)
        etUsername.setText(user.username)
        etPassword.setText(user.password)
        originalUsername = user.username.toString()  //  <-- THIS IS ESSENTIAL
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    private fun progressBarVisibility(isVisible: Boolean) {
        progressBar.visibility = if (isVisible) View.VISIBLE else View.GONE
    }


    private fun logout() {
        val intent = Intent(requireContext(), SignupActivity::class.java)
        startActivity(intent)
        requireActivity().finish() //  Ensure the MainActivity is closed
    }


    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.POST_NOTIFICATIONS // Corrected line
                ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) { // Corrected line
                // Display an educational UI explaining to the user why your app needs notifications
                // This is optional
                // For example:
                // AlertDialog.Builder(requireContext())
                //     .setTitle("Notification Permission Required")
                //     .setMessage("This app needs permission to send you important updates.")
                //     .setPositiveButton("OK") { dialog, which ->
                //         requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                //     }
                //     .setNegativeButton("Cancel", null)
                //     .show()
                Log.i(TAG, "Showing notification permission rationale")
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)  // Corrected line
                Log.d(TAG, "Requesting notification permission")
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    companion object {
        private const val TAG = "SlideshowFragment"
    }
}