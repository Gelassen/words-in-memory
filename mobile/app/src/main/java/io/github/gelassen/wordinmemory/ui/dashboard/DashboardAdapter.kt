package io.github.gelassen.wordinmemory.ui.dashboard

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.databinding.ViewItemDasboardItemBinding
import io.github.gelassen.wordinmemory.model.SubjectToStudy

class DashboardAdapter(val clickListener: ClickListener) :
    RecyclerView.Adapter<DashboardAdapter.ViewHolder>() {

    interface ClickListener {
        fun onClick(data: SubjectToStudy)
        fun onNonComplete(selectedSubject: SubjectToStudy)
        fun onComplete(selectedSubject: SubjectToStudy)
        fun onLongPress(selectedSubject: SubjectToStudy)
        fun onItemSwiped(removedItem: SubjectToStudy)
    }

    private val data: MutableList<SubjectToStudy> = mutableListOf()
    private var isTutoring = false

    /** Uids already shown — animate only brand-new rows, not DiffUtil rebinds */
    private val seenUids: MutableSet<Int> = mutableSetOf()
    private var firstBindPass = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewItemDasboardItemBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding, false)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selectedSubject = data[position]
        holder.binding.toTranslate.text = selectedSubject.toTranslate
        holder.itemView.alpha = 1f
        prepareOnCompleteClickCase(holder, selectedSubject)
        prepareToTranslateClickCase(holder, selectedSubject)
        prepareResponseOnSelectionCase(holder, selectedSubject)
        prepareLongPressCase(holder, selectedSubject)
        decorateTutoringMode(holder)

        val isNewRow = selectedSubject.uid != 0 && selectedSubject.uid !in seenUids
        if (isNewRow && !firstBindPass) {
            seenUids.add(selectedSubject.uid)
            animateNewItem(holder.itemView)
        } else if (selectedSubject.uid != 0) {
            seenUids.add(selectedSubject.uid)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.itemView.clearAnimation()
        holder.itemView.alpha = 1f
    }

    fun updateData(newData: List<SubjectToStudy>) {
        val diffCallback = SubjectDiffCallback(data, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback, true)
        data.clear()
        data.addAll(newData)
        diffResult.dispatchUpdatesTo(this)
        firstBindPass = false
        val current = newData.map { it.uid }.toSet()
        seenUids.retainAll(current)
    }

    fun getDataset(): MutableList<SubjectToStudy> {
        return data
    }

    fun turnOnTutoring(turnOn: Boolean) {
        if (this.isTutoring == turnOn) return
        this.isTutoring = turnOn
        notifyDataSetChanged()
    }

    fun onRightSwipe(position: Int) {
        if (position < 0 || position >= data.size) return
        val removedItem = data.removeAt(position)
        seenUids.remove(removedItem.uid)
        notifyItemRemoved(position)
        clickListener.onItemSwiped(removedItem)
    }

    private fun animateNewItem(view: View) {
        view.alpha = 0f
        ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
            duration = 250
            start()
        }
    }

    private fun decorateTutoringMode(holder: ViewHolder) {
        // Tutoring screens: never show "completed" chrome — all cards look normal
        if (isTutoring) {
            val notSelectedFlag = 0
            holder.binding.completeIcon.visibility = View.GONE
            holder.binding.root.background.level = notSelectedFlag
            holder.binding.toTranslate.setTextColor(
                holder.binding.root.context.resources.getColor(R.color.enabled_text)
            )
        } else {
            holder.binding.completeIcon.visibility = View.VISIBLE
        }
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
        holder.binding.root.setOnClickListener {
            clickListener.onClick(selectedSubject)
            holder.translationIsOn = !holder.translationIsOn
            if (holder.translationIsOn) {
                holder.binding.toTranslate.text =
                    selectedSubject.toTranslate + " / " + selectedSubject.translation
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
            holder.binding.completeIcon.background.level = selectedFlag
            holder.binding.toTranslate.setTextColor(
                holder.binding.root.context.resources.getColor(R.color.disabled_text)
            )
        } else {
            holder.binding.root.background.level = notSelectedFlag
            holder.binding.completeIcon.background.level = notSelectedFlag
            holder.binding.toTranslate.setTextColor(
                holder.binding.root.context.resources.getColor(R.color.enabled_text)
            )
        }
    }

    fun prepareLongPressCase(holder: ViewHolder, selectedSubject: SubjectToStudy) {
        holder.binding.root.setOnLongClickListener {
            clickListener.onLongPress(selectedSubject)
            true
        }
    }

    class ViewHolder(
        val binding: ViewItemDasboardItemBinding,
        var translationIsOn: Boolean
    ) : RecyclerView.ViewHolder(binding.root)

    /**
     * Identity by Room uid; content by visible fields.
     * Old callback compared only javaClass → every update looked like a full change → flicker.
     */
    class SubjectDiffCallback(
        private val oldList: List<SubjectToStudy>,
        private val newList: List<SubjectToStudy>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            val old = oldList[oldPos]
            val new = newList[newPos]
            return if (old.uid != 0 && new.uid != 0) {
                old.uid == new.uid
            } else {
                old.toTranslate == new.toTranslate
            }
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            val old = oldList[oldPos]
            val new = newList[newPos]
            return old.toTranslate == new.toTranslate &&
                    old.translation == new.translation &&
                    old.isCompleted == new.isCompleted &&
                    old.isRedundant == new.isRedundant &&
                    old.tutorCounter == new.tutorCounter
        }
    }
}
