//app/src/main/java/com/daytradchat/papa/ui/SettingsFragment.kt
//ver 2.16-14
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
    private lateinit var reconnectAdapter: ArrayAdapter<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textVersion.text = "ver 2.16-14"
        binding.textPortInline.text = "port: ${SocketConfig.SERVER_PORT}"

        codeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        codeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHoldingCode.adapter = codeAdapter

        reconnectAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            (1..10).map { it.toString() }.toMutableList()
        )
        reconnectAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerReconnectSec.adapter = reconnectAdapter

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
                launch {
                    viewModel.currentHost.collect { host ->
                        val current = binding.editHostInline.text?.toString().orEmpty()
                        if (current != host) {
                            binding.editHostInline.setText(host)
                            binding.editHostInline.setSelection(host.length)
                        }
                    }
                }
                launch {
                    viewModel.reconnectSec.collect { sec ->
                        val index = sec.coerceIn(1, 10) - 1
                        if (binding.spinnerReconnectSec.selectedItemPosition != index) {
                            binding.spinnerReconnectSec.setSelection(index)
                        }
                    }
                }
                launch {
                    viewModel.availableCodes.collect { labels ->
                        codeAdapter.clear()
                        codeAdapter.addAll(labels.toList())
                        codeAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
