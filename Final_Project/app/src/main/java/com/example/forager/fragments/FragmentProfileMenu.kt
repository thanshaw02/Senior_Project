/**
 * - LiveData is fed into here from my HomeViewModel
 * - That is from from the DataRepository LiveData
 * - That comes from the DB's
 */

package com.example.forager.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.forager.R
import com.example.forager.activities.FileDirectory
import com.example.forager.remotedata.model.User
import com.example.forager.activities.login.LoginActivity
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import java.io.File


private const val LOG = "FragmentProfileMenu:"

/**
 * Fragment profile menu class, loads and displays user information that is read in from my RealtimeDatabase.
 *
 * @author Tylor J. Hanshaw
 */
class FragmentProfileMenu : Fragment() {

    // User data passed from "MapsActivity"
    private lateinit var currentUser: User

    // setting up all variables needed
    private lateinit var profilePicture: ImageView
    private lateinit var usersName: TextView
    private lateinit var usersUsername: TextView
    private lateinit var usersEmail: TextView
    private lateinit var dateAccountCreated: TextView
    private lateinit var deleteAccountBtn: Button

    private lateinit var photoDir: File
    private lateinit var fileDir: FileDirectory

    // ViewModel
    /**
     * Private variable that lazily assigns MapsActivity's ViewModel.
     * @see com.example.forager.MapsActivity
     */
    private val homeVM: HomeViewModel by activityViewModels()

    // For choosing a profile picture from the user's photo gallery
    // Trying to updating the menu heading profile picture to, but not working lol
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d(LOG, "Result: $result")
            if (result.resultCode == Activity.RESULT_OK) {
                homeVM.updateProfilePicture(result.data!!.data!!)
                Glide.with(this).load(result.data!!.data).into(profilePicture)
            } else {
                Snackbar.make(
                    requireView(),
                    "The photo was not properly received.",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        fileDir = context as FileDirectory
    }

    /**
     * On create function
     *
     * Overriding the onCreate() function, allowing the fragment to acquire the [currentUser] from HomeViewModel.
     *
     * @see HomeViewModel
     * @param savedInstanceState SavedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(LOG, "onCreate() called")

        // Pulling the user's info from the LiveData in my HomeViewModel
        currentUser = homeVM.observeUserInfo.value!!.user!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile_menu, container, false)

        photoDir = fileDir.getOutputDirectory(homeVM.getCurrentDate())

        usersName = view.findViewById(R.id.fullName_tv)
        usersUsername = view.findViewById(R.id.username_tv)
        usersEmail = view.findViewById(R.id.email_tv)
        dateAccountCreated = view.findViewById(R.id.dateCreated_tv)
        deleteAccountBtn = view.findViewById(R.id.delete_account_btn)
        profilePicture = view.findViewById(R.id.profile_picture)

        setViewData()

        profilePicture.setOnClickListener {
            val pickPhotoIntent = Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            val fileProvider = FileProvider.getUriForFile(
                requireContext(),
                "com.example.forager.fragments.file_provider",
                photoDir
            )
            pickPhotoIntent.apply { putExtra(MediaStore.EXTRA_OUTPUT, fileProvider) }
            resultLauncher.launch(pickPhotoIntent)
        }

        // Issue is here, for some reason I cannot delete the user and the user's plant list from the alert box..
        deleteAccountBtn.setOnClickListener {
            setDialog(view)
        }

        return view
    }

    // Leaving this comment here, deleting the user's account still causes issues
    /**
     * Private function that is invoked when the "Delete Account" button is pressed.
     * An AlertDialog window is opened that prompts the user to re-enter their credentials before deleting their account.
     *
     * @param view View needed for the Snackbar text in the case the user enters incorrect login credentials.
     */
    // TODO: There is still a bug with the "Delete User Data" Firebase extension
    // TODO: Seems to only be deleting users info from one of the two nodes
    // TODO: And seems to be only deleting the first in the the "Realtime Database paths" list..
    private fun setDialog(view: View) {
        val layout = layoutInflater.inflate(R.layout.user_delete_account_alert_box, null)
        val alertBox = AlertDialog.Builder(requireContext())
        alertBox.setTitle("Please enter your credentials: ")
            .setCancelable(true)
            .setView(layout)
            .setPositiveButton("Enter") { dialog, i ->
                val email = layout.findViewById<EditText>(R.id.emailET_alert)
                val password = layout.findViewById<EditText>(R.id.passET_alert)
                if(email.text.toString() != "" || password.text.toString() != "") {
                    homeVM.deleteUserAccount(
                        layout.findViewById<EditText>(R.id.emailET_alert).text.toString(),
                        layout.findViewById<EditText>(R.id.passET_alert).text.toString()
                    )
                    //goToLoginPage()
                }
                else Snackbar.make(requireContext(), view, "Please fill out the form above.", Snackbar.LENGTH_SHORT).show()
                // The issue is here, I think I'm logging the user out before the async calls to Firebase are finished
                //logOutAPI?.logOut() // Using the LogOut interface I created here
            }
        alertBox.show()
    }

    /**
     * Private function that is called in the setDialog(view: View) function when deleting the user's account.
     * @see setDialog
     */
    private fun goToLoginPage() {
        Log.d(LOG, "Going to login page.. hopefully.")
        val intent = LoginActivity.newInstance(requireContext())
        startActivity(intent)
    }

    /**
     * Private function used to set the text for all widgets in the FragmentProfileMenu fragment.
     * This function is called once in onCreate().
     * @see FragmentProfileMenu for more information on its use.
     */
    private fun setViewData() {
        usersName.text = currentUser.fullName
        usersUsername.text = currentUser.userName
        usersEmail.text = currentUser.email
        dateAccountCreated.text = currentUser.dateCreated
    }

    companion object {

        /**
         * Use this companion object method to navigate to this fragment.
         *
         * @see [com.example.forager.MapsActivity] for more information.
         * @see com.example.forager.MapsActivity.goToMenuItem for its usage.
         * @return A new instance of fragment FragmentProfileMenu
         */
        fun newInstance(): FragmentProfileMenu = FragmentProfileMenu()
    }
}