//app/src/main/java/com/daytradchat/papa/ui/SettingsFragment.kt
//ver 2.17-40
package com.daytradchat.papa.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daytradchat.papa.databinding.FragmentSettingsBinding
import com.daytradchat.papa.network.SocketConfig
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TradeViewModel by activityViewModels()
    private lateinit var codeAdapter: ArrayAdapter<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.textVersion.text = "ver 2.17-09"

        val editCode = view.findViewById<EditText>(R.id.editCode)
        val btnSend = view.findViewById<Button>(R.id.btnSend)

        btnSend.setOnClickListener {
            val code = editCode.text.toString()
            viewModel.sendAddCode(code)
        }
		
        codeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        binding.spinnerHoldingCode.adapter = codeAdapter

        binding.buttonSaveReconnect.setOnClickListener {
            val host = binding.editHostInline.text?.toString().orEmpty().trim()
            val sec = binding.spinnerReconnectSec.selectedItem?.toString().orEmpty()
            viewModel.saveSettingsAndReconnect(host, sec)
        }

        binding.buttonReset.setOnClickListener {
            viewModel.resetSettingsAndReconnect()
        }

        binding.buttonSendWatch.setOnClickListener {
            val inputText = binding.editSendCode.text?.toString().orEmpty().trim()
            viewModel.sendWatchCommand(inputText)
        }

        binding.buttonApplyHolding.setOnClickListener {
            val codeLabel = binding.spinnerHoldingCode.selectedItem?.toString().orEmpty()
            val buyPrice = binding.editBuyPrice.text?.toString().orEmpty().trim()
            viewModel.applyHolding(codeLabel, buyPrice)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentHost.collect { host -> binding.editHostInline.setText(host) } }
                launch { viewModel.availableCodes.collect { labels -> 
                    codeAdapter.clear()
                    codeAdapter.addAll(labels)
                    codeAdapter.notifyDataSetChanged()
                }}
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
