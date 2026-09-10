package io.github.gelassen.wordinmemory.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.gelassen.wordinmemory.AppApplication
import io.github.gelassen.wordinmemory.R
import io.github.gelassen.wordinmemory.databinding.FragmentChatBinding
import io.github.gelassen.wordinmemory.di.ViewModelFactory
import io.github.gelassen.wordinmemory.ui.dashboard.DashboardFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChatFragment : Fragment() {

    companion object {
        const val TAG = "ChatFragment"

        fun newInstance(): Fragment = ChatFragment()
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private lateinit var viewModel: ChatViewModel
    private lateinit var binding: FragmentChatBinding
    private val adapter = ChatAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity().application as AppApplication).getComponent().inject(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.chat_title)
        }

        binding.chatList.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.chatList.adapter = adapter

        binding.sendButton.setOnClickListener { sendCurrentInput() }
        binding.inputEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentInput()
                true
            } else {
                false
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                adapter.submitList(state.messages)
                if (state.messages.isNotEmpty()) {
                    binding.chatList.scrollToPosition(state.messages.size - 1)
                }
                binding.progressBar.visibility =
                    if (state.isLoading) View.VISIBLE else View.GONE
                binding.sendButton.isEnabled = !state.isLoading
                binding.inputEdit.isEnabled = !state.isLoading
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // back to dashboard
                requireActivity().supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.container, DashboardFragment.newInstance())
                    .commit()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun sendCurrentInput() {
        val text = binding.inputEdit.text?.toString().orEmpty()
        if (text.isBlank()) return
        binding.inputEdit.setText("")
        viewModel.sendUserMessage(text)
    }
}
