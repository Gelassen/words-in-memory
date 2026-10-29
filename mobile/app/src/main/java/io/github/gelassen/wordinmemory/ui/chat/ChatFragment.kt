package io.github.gelassen.wordinmemory.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
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
    private var closing: Boolean = false

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
        // Activity scope: survives brief fragment replace while endSession finishes
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[ChatViewModel::class.java]
        binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Always (re)start a practice session when the screen is shown
        viewModel.startSession()

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

        // System Back → same path as toolbar Back
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    leaveChat()
                }
            }
        )

        lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                adapter.submitList(state.messages)
                if (state.messages.isNotEmpty()) {
                    binding.chatList.scrollToPosition(state.messages.size - 1)
                }
                binding.progressBar.visibility =
                    if (state.isLoading) View.VISIBLE else View.GONE
                binding.sendButton.isEnabled = !state.isLoading && !state.sessionClosing
                binding.inputEdit.isEnabled = !state.isLoading && !state.sessionClosing
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                leaveChat()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Finish session (extract + save vocabulary), then open dashboard.
     * Guarded against double taps.
     */
    private fun leaveChat() {
        if (closing) return
        closing = true
        binding.sendButton.isEnabled = false
        binding.inputEdit.isEnabled = false
        viewModel.endSession {
            if (!isAdded) return@endSession
            requireActivity().supportFragmentManager
                .beginTransaction()
                .replace(R.id.container, DashboardFragment.newInstance())
                .commitAllowingStateLoss()
        }
    }

    private fun sendCurrentInput() {
        val text = binding.inputEdit.text?.toString().orEmpty()
        if (text.isBlank()) return
        binding.inputEdit.setText("")
        viewModel.sendUserMessage(text)
    }
}
