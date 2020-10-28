package `is`.posctrl.posctrl_android.ui.stores

import `is`.posctrl.posctrl_android.data.model.StoreResponse
import `is`.posctrl.posctrl_android.databinding.ItemStoreBinding
import android.view.LayoutInflater
import android.view.ViewGroup

import androidx.recyclerview.widget.RecyclerView

class StoresAdapter(private val clickListener: StoreCellListener) :
        RecyclerView.Adapter<StoreCellViewHolder>() {

    private var data = ArrayList<StoreResponse>()

    fun setData(data: Array<StoreResponse>?) {
        this.data = ArrayList()
        if (data != null) {
            this.data.addAll(data)
        }
        notifyDataSetChanged()
    }


    override fun getItemCount(): Int {
        return data.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoreCellViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemStoreBinding.inflate(layoutInflater, parent, false)

        return StoreCellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoreCellViewHolder, position: Int) {
        val currentItem = data[position]
        holder.bind(clickListener, currentItem)
    }
}

open class StoreCellListener(val clickListener: (store: StoreResponse) -> Unit) {
    fun onClick(store: StoreResponse) = clickListener(store)
}

class StoreCellViewHolder(private val binding: ItemStoreBinding) :
        RecyclerView.ViewHolder(binding.root) {

    fun bind(clickListener: StoreCellListener, item: StoreResponse) {
        binding.clickListener = clickListener
        binding.item = item
    }
}

