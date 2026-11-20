package com.example.groceryapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.groceryapp.databinding.ItemOrderBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One card per buyer order: shop, time, items, total, status and (while pending) a Cancel button. */
class BuyerOrderAdapter(
    private val onCancel: (Order) -> Unit
) : RecyclerView.Adapter<BuyerOrderAdapter.VH>() {

    private val items = mutableListOf<Order>()
    private val timeFormat = SimpleDateFormat("d MMM, h:mm a", Locale.getDefault())

    fun submitList(newItems: List<Order>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    inner class VH(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val order = items[position]
        val b = holder.binding

        b.tvBuyer.text = order.shopName.ifBlank { "Shop" }
        b.tvTime.text = timeFormat.format(Date(order.timestamp))
        b.tvItems.text = order.items.values.joinToString("\n") { "${it.quantity} × ${it.name}" }
        b.tvFee.text = "Subtotal ${formatMoney(order.subtotal)} · Delivery " +
                if (order.deliveryFee <= 0.0) "free" else formatMoney(order.deliveryFee)
        b.tvTotal.text = formatMoney(order.total)
        bindOrderStatus(b.tvStatus, order.status)

        // Only an order the seller hasn't handled yet can be cancelled.
        b.actionRow.visibility = if (order.status == Order.STATUS_PENDING) View.VISIBLE else View.GONE
        b.btnAccept.visibility = View.GONE
        b.btnReject.text = "Cancel order"
        b.btnReject.setOnClickListener { onCancel(order) }
    }

    override fun getItemCount() = items.size
}
