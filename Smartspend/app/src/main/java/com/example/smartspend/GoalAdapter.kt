package com.example.smartspend

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

class GoalAdapter(private val goals: List<GoalEntity>, private val listener: OnItemClickListener) :
    RecyclerView.Adapter<GoalAdapter.GoalViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(goal: GoalEntity)
    }

    class GoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvGoalTitle: TextView = itemView.findViewById(R.id.tvGoalTitle)
        val tvGoalProgress: TextView = itemView.findViewById(R.id.tvGoalProgress)
        val tvGoalPercentage: TextView = itemView.findViewById(R.id.tvGoalPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.goal_card_layout, parent, false)
        return GoalViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: GoalViewHolder, position: Int) {
        val goal = goals[position]
        holder.tvGoalTitle.text = goal.goalName

        val numberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        holder.tvGoalProgress.text =
            "${numberFormat.format(goal.savedAmount)}/${numberFormat.format(goal.totalAmount)}"

        val progressPercentage = (goal.savedAmount.divide(goal.totalAmount, 2, RoundingMode.HALF_UP)
            .multiply(BigDecimal(100))).toInt()
        holder.tvGoalPercentage.text = "$progressPercentage%"

        val percentageColor = when {
            progressPercentage <= 30 -> {
                Color.parseColor("#FFB3B3")
            }
            progressPercentage <= 60 -> {
                Color.parseColor("#FFD9B3")
            }
            progressPercentage <= 90 -> {
                Color.parseColor("#FFFFCC")
            }
            else -> {
                Color.parseColor("#B3FFB3")
            }
        }

        holder.tvGoalPercentage.setTextColor(percentageColor)

        // Set click listener
        holder.itemView.setOnClickListener {
            listener.onItemClick(goal)
        }
    }

    override fun getItemCount(): Int {
        return goals.size
    }
}
