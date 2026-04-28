// app/src/main/java/com/daytradchat/papa/ui/BoardFragment.kt
//ver 2.17-00
package com.daytradchat.papa.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.daytradchat.papa.R

class BoardFragment : Fragment(R.layout.fragment_settings) {
    private val viewModel: TradeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Settingsレイアウト内のコンポーネントを操作
        val editCode = view.findViewById<EditText>(R.id.editSendCode)
        val btnSend = view.findViewById<Button>(R.id.buttonSendWatch)

        btnSend?.setOnClickListener {
            val code = editCode?.text?.toString()?.trim()
            if (!code.isNullOrBlank()) {
                viewModel.sendAddCodes(listOf(code))
                editCode.setText("")
            }
        }
    }
}
