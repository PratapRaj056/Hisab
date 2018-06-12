package darkeagle.prs.hisab

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_history.*

class HistoryActivity : AppCompatActivity(), HistoryView {

    val TAG = "HistoryActivity"

    val histories = ArrayList<History>()

    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    private val amountReference: DatabaseReference by lazy {
        database.getReference("amount")
    }

    private val historyReference by lazy {
        database.getReference("history")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initialise()
    }

    private fun initialise() {
        recycler_view.setHasFixedSize(true)


        // Read from the database
        historyReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                histories.clear()

                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                try {
                    val values = dataSnapshot.value as Map<String, Any>

                    Log.d(TAG, "Value $values")

                    for ((key, value) in values) {
                        Log.d(TAG, "$key = $value")
                        val map = value as Map<String, String>

                        histories.add(History(key, map["amount"]!!.toDouble(), map["reason"]!!, map["user"]!!, map["time"]!!))
                    }

                    recycler_view.adapter = HistoryAdapter(applicationContext, this@HistoryActivity, histories)
                } catch (e: TypeCastException) {
                    Toast.makeText(applicationContext, "No history yet!", Toast.LENGTH_LONG).show()
                    finish()
                }


            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })

        amountReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                val value = dataSnapshot.getValue(Double::class.java)
                Log.d(TAG, "Value is: " + value!!)

                title = String.format("%.2f", value)
            }

            override fun onCancelled(error: DatabaseError) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun deleteHistory(history: History) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Delete?")
        builder.setMessage("Do you want to delete entry \"${history.reason}\" with amount ₹ ${history.amount} ?")
        builder.setCancelable(false)
        builder.setPositiveButton("Yes", { dialog, _ ->
            historyReference.child(history.key).removeValue()

            val amount = title.toString().toDouble() - history.amount
            amountReference.setValue(amount)

            dialog.dismiss()
        })
        builder.setNegativeButton("No", { dialog, _ ->
            dialog.dismiss()
        })
        builder.show()
    }
}
