/**
 * - LiveData is fed into here from my HomeViewModel
 * - That is from from the DataRepository LiveData
 * - That comes from the DB's
 */

package com.example.forager.fragments

import android.content.Context
import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import com.example.forager.R
import com.example.forager.remotedata.User
import com.example.forager.repository.login.LoginActivity
import com.example.forager.viewmodel.HomeViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

private const val USER_UID = "user_UID"
private const val LOG = "FragmentProfileMenu:"

class FragmentProfileMenu : Fragment() {

    // User data passed from "MapsActivity"
    private lateinit var currentUser: User

    // setting up all variables needed
    private lateinit var usersName: TextView
    private lateinit var usersUsername: TextView
    private lateinit var usersEmail: TextView
    private lateinit var dateAccountCreated: TextView
    private lateinit var deleteAccountBtn: Button

    // ViewModel
    private val homeVM: HomeViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        currentUser = homeVM.getUserLiveData.value!!
    }

    // trying to postpone the transition until all data has been loaded fully
    // This is one way to allow my data to load fully first
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile_menu, container, false)

        usersName = view.findViewById(R.id.fullName_tv)
        usersUsername = view.findViewById(R.id.username_tv)
        usersEmail = view.findViewById(R.id.email_tv)
        dateAccountCreated = view.findViewById(R.id.dateCreated_tv)
        deleteAccountBtn = view.findViewById(R.id.delete_account_btn)

        setViewData()

        // Issue is here, for some reason I cannot delete the user and the user's plant list from the alert box..
        deleteAccountBtn.setOnClickListener {
            setDialog(view)
        }

        return view
    }

    // I think I found the issue!!
    // I believe logging out does something bad when trying to delete the user
    // Maybe have a loading screen pop up  for like 2 seconds, then log the user out??
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
                        layout.findViewById<EditText>(R.id.passET_alert).text.toString(),
                        FirebaseAuth.getInstance().currentUser!!)
                    //goToLoginPage()
                }
                else Snackbar.make(requireContext(), view, "Please fill out the form above.", Snackbar.LENGTH_SHORT).show()
                // The issue is here, I think I'm logging the user out before the async calls to Firebase are finished
                //logOutAPI?.logOut() // Using the LogOut interface I created here
            }
        alertBox.show()
    }

    private fun goToLoginPage() {
        val intent = LoginActivity.newInstance(requireContext())
        startActivity(intent)
    }

    private fun setViewData() {
        usersName.text = currentUser.fullName
        usersUsername.text = currentUser.userName
        usersEmail.text = currentUser.email
        dateAccountCreated.text = currentUser.dateCreated
    }

    companion object {
        fun newInstance(): FragmentProfileMenu = FragmentProfileMenu()
    }
}