//app/src/main/java/com/daytradchat/papa/ui/SettingsFragment.kt
//ver 2.17-45
package com.daytradchat.papa.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daytradchat.papa.R
import com.daytradchat.papa.databinding.FragmentSettingsBinding
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
        
        binding.textVersion.text = "ver 2.17-41"

        val editCode = view.findViewById<EditText>(R.id.editCode)
        val btnSend = view.findViewById<Button>(R.id.btnSend)

        btnSend?.setOnClickListener {
            val code = editCode?.text?.toString()?.trim() ?: ""
            if (code.isNotEmpty()) {
                viewModel.sendAddCodes(listOf(code))
                editCode?.setText("")
            }
        }
        
        codeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, mutableListOf<String>())
        binding.spinnerHoldingCode.adapter = codeAdapter

        binding.buttonSaveReconnect.setOnClickListener {
            val host = binding.editHostInline.text?.toString().orEmpty().trim()
            val port = binding.editPort.text?.toString().orEmpty().trim()
            val sec = binding.spinnerReconnectSec.selectedItem?.toString().orEmpty()
            
            if (host.isEmpty() || port.isEmpty()) {
                // 必要に応じてトーストなどで警告を出す
                return@setOnClickListener
            }
            viewModel.saveSettingsAndReconnect(host, port, sec)
        }

        binding.buttonReset.setOnClickListener { viewModel.resetSettingsAndReconnect() }
        binding.buttonSendWatch.setOnClickListener { viewModel.sendWatchCommand(binding.editSendCode.text?.toString().orEmpty().trim()) }
        binding.buttonApplyHolding.setOnClickListener { viewModel.applyHolding(binding.spinnerHoldingCode.selectedItem?.toString().orEmpty(), binding.editBuyPrice.text?.toString().orEmpty().trim()) }

        // 2. 起動時にポート番号を表示する処理を追加
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.currentHost.collect { binding.editHostInline.setText(it) } }
                launch { viewModel.availableCodes.collect { labels -> 
                    codeAdapter.clear()
                    for (label in labels) {
                        codeAdapter.add(label)
                    }
                    codeAdapter.notifyDataSetChanged()
                }}
                launch { 
                    viewModel.currentPort.collect { port -> 
                        binding.editPort.setText(port) 
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
