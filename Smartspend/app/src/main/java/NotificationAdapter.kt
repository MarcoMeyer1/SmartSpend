import android.app.Notification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartspend.R

class NotificationAdapter(private val notifications: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.notificationIcon)
        val message: TextView = itemView.findViewById(R.id.notificationMessage)
        val time: TextView = itemView.findViewById(R.id.notificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]

        // Set the dynamic message based on the notification type
        holder.message.text = when {
            notification.dynamicTask.isNotEmpty() -> "Reminder Set: ${notification.dynamicTask} on ${notification.dynamicDate}"
            notification.dynamicAmount.isNotEmpty() -> {
                if (position == 0) "Groceries Budget down to R${notification.dynamicAmount}"
                else "R${notification.dynamicAmount} added to Income"
            }
            else -> "Congratulations! You’ve reached your savings goal of R${notification.dynamicAmount}"
        }

        holder.time.text = notification.timestamp

        // Set icon based on the type of notification
        holder.icon.setImageResource(
            when (position) {
                0 -> R.drawable.ic_warning  // Budget warning icon
                1 -> R.drawable.ic_increase    // Income update icon
                2 -> R.drawable.ic_reminder         // Reminder icon
                3 -> R.drawable.ic_goal_achievement // Goal achievement icon
                else -> R.drawable.ic_default  // Default icon
            }
        )
    }

    override fun getItemCount(): Int = notifications.size

    data class Notification(
        val dynamicAmount: String = "",  // For budget or income-related notifications
        val dynamicTask: String = "",    // For tasks like reminders
        val dynamicDate: String = "",    // For date-related tasks
        val timestamp: String            // Timestamp of the notification
    )
}