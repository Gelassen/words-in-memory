package io.github.gelassen.wordinmemory.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.databinding.ViewItemDasboardItemBinding
import io.github.gelassen.wordinmemory.model.SubjectToStudy

class DashboardAdapter(val clickListener: ClickListener) : RecyclerView.Adapter<DashboardAdapter.ViewHolder>() {

    interface ClickListener {
        fun onClick(data: SubjectToStudy)
        fun onNonComplete(selectedSubject: SubjectToStudy)
        fun onComplete(selectedSubject: SubjectToStudy)
        fun onLongPress(selectedSubject: SubjectToStudy)
    }

    private val data: MutableList<SubjectToStudy> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewItemDasboardItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, false)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedSubject = data.get(position)
        holder.binding.toTranslate.text = selectedSubject.toTranslate
        prepareOnCompleteClickCase(holder, selectedSubject)
        prepareToTranslateClickCase(holder, selectedSubject)
        prepareResponseOnSelectionCase(holder, selectedSubject)
        prepareLongPressCase(holder, selectedSubject)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun prepareOnCompleteClickCase(holder: ViewHolder, selectedSubject: SubjectToStudy) {
        holder.binding.completeIcon.setOnClickListener {
            if (selectedSubject.isCompleted) {
                clickListener.onNonComplete(selectedSubject)
            } else {
                clickListener.onComplete(selectedSubject)
            }
        }
    }

    fun prepareToTranslateClickCase(holder: ViewHolder, selectedSubject: SubjectToStudy) {
        holder.binding.root.setOnClickListener { it ->
            clickListener.onClick(selectedSubject)
            holder.translationIsOn = !holder.translationIsOn
            if (holder.translationIsOn) {
                holder.binding.toTranslate.text = selectedSubject.toTranslate + " / " + selectedSubject.translation
            } else {
                holder.binding.toTranslate.text = selectedSubject.toTranslate
            }
        }
    }

    fun prepareResponseOnSelectionCase(holder: ViewHolder, selectedSubject: SubjectToStudy) {
        val selectedFlag = 1
        val notSelectedFlag = 0
        if (selectedSubject.isCompleted) {
            holder.binding.root.background.level = selectedFlag
            holder.binding.completeIcon.background.setLevel(selectedFlag)
            holder.binding.toTranslate.setTextColor(
                holder.binding.root.context.resources.getColor(
                    R.color.disabled_text
                )
            )
        } else {
            holder.binding.root.background.level = notSelectedFlag
            holder.binding.completeIcon.background.setLevel(notSelectedFlag)
            holder.binding.toTranslate.setTextColor(
                holder.binding.root.context.resources.getColor(
                    R.color.enabled_text
                )
            )
        }
    }

    fun prepareLongPressCase(holder: ViewHolder, selectedSubject: SubjectToStudy) {
        holder.binding.root.setOnLongClickListener(object: View.OnLongClickListener {
            override fun onLongClick(v: View?): Boolean {
                clickListener.onLongPress(selectedSubject)
                return true
            }

        })
    }

    fun updateData(newData: List<SubjectToStudy>) {
        val diffCallback = DiffUtilCallback(data, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        data.clear()
        data.addAll(newData)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(val binding: ViewItemDasboardItemBinding, var translationIsOn: Boolean) : RecyclerView.ViewHolder(binding.root)

    class DiffUtilCallback(private val oldList: List<Any>, private val newList: List<Any>) :
        DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.javaClass == newItem.javaClass
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]

            return oldItem.hashCode() == newItem.hashCode()
        }
    }
}