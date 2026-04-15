//app/src/main/java/com/daytradchat/papa/ui/SettingsFragment.kt
//ver 2.16-00
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
    private lateinit var shareAdapter: ArrayAdapter<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.textVersion.text = "ver 2.16-00"
        binding.textPort.text = "Port: ${SocketConfig.SERVER_PORT}"
        binding.textSendSpec.text = """送信仕様
JSON: {"type":"watch_codes","codes":["5726","186A"]}
文字列全入替: 5726,186A,6323
文字列1銘柄交換: 5726#7011"""

        codeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        codeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTargetCode.adapter = codeAdapter
        binding.spinnerHoldingCode.adapter = codeAdapter

        shareAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            (1..10).map { (it * 100).toString() }.toMutableList()
        )
        shareAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerHoldingShares.adapter = shareAdapter

        binding.buttonSaveReconnect.setOnClickListener {
            viewModel.saveSettingsAndReconnect(
                binding.editHost.text?.toString().orEmpty().trim(),
                binding.editReconnectSec.text?.toString().orEmpty().trim()
            )
        }
        binding.buttonReset.setOnClickListener { viewModel.resetSettingsAndReconnect() }

        binding.buttonSendWatch.setOnClickListener {
            val targetLabel = binding.spinnerTargetCode.selectedItem?.toString().orEmpty()
            val inputText = binding.editSendCode.text?.toString().orEmpty().trim()
            viewModel.sendWatchCommand(targetLabel, inputText)
        }

        binding.buttonApplyHolding.setOnClickListener {
            val codeLabel = binding.spinnerHoldingCode.selectedItem?.toString().orEmpty()
            val shares = binding.spinnerHoldingShares.selectedItem?.toString().orEmpty().toIntOrNull() ?: 0
            val buyPrice = binding.editBuyPrice.text?.toString().orEmpty().trim()
            viewModel.applyHolding(codeLabel, shares, buyPrice)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentHost.collect { host ->
                        val current = binding.editHost.text?.toString().orEmpty()
                        if (current != host) {
                            binding.editHost.setText(host)
                            binding.editHost.setSelection(host.length)
                        }
                    }
                }
                launch {
                    viewModel.reconnectSec.collect { sec ->
                        val secText = sec.toString()
                        val current = binding.editReconnectSec.text?.toString().orEmpty()
                        if (current != secText) {
                            binding.editReconnectSec.setText(secText)
                            binding.editReconnectSec.setSelection(secText.length)
                        }
                    }
                }
                launch {
                    viewModel.availableCodes.collect { labels ->
                        codeAdapter.clear()
                        codeAdapter.addAll(labels)
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
