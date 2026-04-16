//app/src/main/java/com/daytradchat/papa/MainActivity.kt
//ver 2.16-14b
package com.daytradchat.papa

import android.os.Bundle
import android.util.TypedValue
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.daytradchat.papa.databinding.ActivityMainBinding
import com.daytradchat.papa.ui.MainPagerAdapter
import com.daytradchat.papa.ui.TradeViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TradeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainPagerAdapter(this)
        val titles = listOf("シグナル", "ログ", "設定1", "設定2", "システム")
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val title = titles[position]
            tab.customView = TextView(this).apply {
                text = title
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    when (title) {
                        "シグナル", "システム" -> 8f
                        else -> 9f
                    }
                )
                setSingleLine(true)
                includeFontPadding = false
                setPadding(4, 0, 4, 0)
            }
        }.attach()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.statusLeft.collect { binding.textStatusLeft.text = it } }
                launch { viewModel.statusRight.collect { binding.textStatusRight.text = it } }
                launch { viewModel.hostLine.collect { binding.textHostLine.text = it } }
            }
        }

        viewModel.startSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) viewModel.stopSocket()
    }
}
