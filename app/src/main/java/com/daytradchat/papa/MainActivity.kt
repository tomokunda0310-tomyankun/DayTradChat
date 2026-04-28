//app/src/main/java/com/daytradchat/papa/MainActivity.kt
//ver 2.16-15
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
// app/src/main/java/com/daytradchat/papa/MainActivity.kt
// ver 2.17-22
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: TradeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainPagerAdapter(this)
        // タブ名を「ロング」「ショート」に変更
        val titles = listOf("ロング", "ショート", "ログ", "設定", "システム")
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            val title = titles[position]
            tab.customView = TextView(this).apply {
                text = title
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    when (title) {
                        "ロング", "ショート", "システム" -> 6.5f
                        else -> 7.5f
                    }
                )
                setSingleLine(true)
                includeFontPadding = false
                setPadding(2, 0, 2, 0)
            }
        }.attach()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.statusLeft.collect { binding.textStatusLeft.text = it } }
                launch { viewModel.statusRight.collect { binding.textStatusRight.text = it } }
            }
        }

        viewModel.startSocket()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) viewModel.stopSocket()
    }
}
