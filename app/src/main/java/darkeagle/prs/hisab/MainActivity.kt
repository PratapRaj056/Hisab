package darkeagle.prs.hisab

import android.content.Intent
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    private val amountReference: DatabaseReference by lazy {
        database.getReference("amount")
    }

    private val historyReference by lazy {
        database.getReference("history")
    }

    private val mAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    private val gso: GoogleSignInOptions by lazy {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
    }

    private val mGoogleSignInClient: GoogleApiClient by lazy {
        GoogleApiClient.Builder(this)
                .enableAutoManage(this, {
                    Log.d(TAG, "onConnectionFailed:$it")
                    Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show()
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()
    }

    private var amount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        showButton.setOnClickListener { onClickShowButton() }

        addButton.setOnClickListener { onClickAddButton() }
        deleteButton.setOnClickListener { onClickDeleteButton() }

    }

    private fun initialise() {
        // Read from the database
        amountReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue(Double::class.java)
                Log.d(TAG, "Value is: " + value!!)

                amount = value
                setAmountValue()
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    private fun onClickAddButton() {

        val amountToAdd = amountET.text.toString()
        val reasonToAdd = reasonET.text.toString()

        if (validate(amountToAdd, reasonToAdd)) {
            amount += amountToAdd.toDouble()
            setAmountValue()

            addHistory(amountToAdd, reasonToAdd, true)
        }
    }

    private fun onClickDeleteButton() {
        val amountToDelete = amountET.text.toString()
        val reasonToDelete = reasonET.text.toString()

        if (validate(amountToDelete, reasonToDelete)) {
            amount -= amountToDelete.toDouble()
            setAmountValue()

            addHistory(amountToDelete, reasonToDelete, false)
        }
    }

    private fun onClickShowButton() {
        startActivity(Intent(this, HistoryActivity::class.java))
    }

    private fun setAmountValue() {
        val amt = String.format("%.2f", amount)

        amountReference.setValue(amount)

        amountTV.text = amt
        amountET.setText("")


        reasonET.setText("")
    }

    private fun addHistory(amount: String, reason: String, isAdded: Boolean) {
        val map = HashMap<String, Any>()
        map["user"] = mAuth.currentUser!!.email!!
        map["time"] = SimpleDateFormat("dd-MM-yy hh:mm:ss a", Locale.getDefault()).format(Date())

        if (isAdded) {
            map["amount"] = amount
        } else {
            map["amount"] = (0 - amount.toDouble()).toString()
        }

        map["reason"] = reason

        historyReference.push().updateChildren(map)
    }

    private fun validate(amount: String, reason: String): Boolean {
        if (TextUtils.isEmpty(amount)) {
            Toast.makeText(this, "You should enter some amount!", Toast.LENGTH_SHORT).show()
            return false
        }

        if (TextUtils.isEmpty(reason)) {
            Toast.makeText(this, "You should enter some reason!", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    override fun onStart() {
        super.onStart()

        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = mAuth.currentUser

        if (currentUser == null) {
            openLoginPage()
        } else {
            initialise()
        }
    }

    private fun openLoginPage() {
        Log.d(TAG, "User Logged out")
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun signOut() {
        mGoogleSignInClient.connect()
        mGoogleSignInClient.registerConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
            override fun onConnected(@Nullable bundle: Bundle?) {

                // sign out Firebase
                mAuth.signOut()

                // sign out Google
                if (mGoogleSignInClient.isConnected) {
                    Auth.GoogleSignInApi.signOut(mGoogleSignInClient).setResultCallback { status ->
                        if (status.isSuccess) {
                            openLoginPage()
                        }
                    }
                }
            }

            override fun onConnectionSuspended(i: Int) {
                Log.d(TAG, "Google API Client Connection Suspended")
            }
        })
    }

    private fun revokeAccess() {
        mGoogleSignInClient.connect()
        mGoogleSignInClient.registerConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
            override fun onConnected(@Nullable bundle: Bundle?) {

                // sign out Firebase
                mAuth.signOut()

                // revoke access Google
                if (mGoogleSignInClient.isConnected) {
                    Auth.GoogleSignInApi.revokeAccess(mGoogleSignInClient).setResultCallback { status ->
                        if (status.isSuccess) {
                            openLoginPage()
                        }
                    }
                }
            }

            override fun onConnectionSuspended(i: Int) {
                Log.d(TAG, "Google API Client Connection Suspended")
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
            R.id.logout -> {
                signOut()
                true
            }
            R.id.disconnect -> {
                revokeAccess()
                true
            }
            R.id.reset -> {
                amountReference.setValue(0.0)
                historyReference.removeValue()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


}
