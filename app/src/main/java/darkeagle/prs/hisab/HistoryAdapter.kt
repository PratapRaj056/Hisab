package darkeagle.prs.hisab

import android.content.Context
import android.graphics.Color
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.history_item.view.*

class HistoryAdapter(private val context: Context, val historyView: HistoryView, private val histories: ArrayList<History>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val classListView = LayoutInflater.from(context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(classListView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindData(histories[position])
    }

    override fun getItemCount(): Int {
        return histories.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        fun bindData(history: History) {
            if (history.amount > 0) {
                itemView.amount.setTextColor(Color.GREEN)
            } else {
                itemView.amount.setTextColor(Color.RED)
            }

            itemView.amount.text = history.amount.toString()
            itemView.reason.text = history.reason
            itemView.user.text = history.user.substring(0, history.user.indexOf('@'))
            itemView.time.text = history.time

            itemView.delete.setOnClickListener {
                onClickDelete(history)
            }
        }

        private fun onClickDelete(history: History) {
            Log.d("Adapter", "Deleting History")
            historyView.deleteHistory(history)
        }
    }
}