//app/src/main/java/com/daytradchat/papa/ui/MainPagerAdapter.kt
//ver 2.13-00
package com.daytradchat.papa.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 4

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SignalFragment()
        1 -> LogFragment()
        2 -> SettingsFragment()
        else -> SystemLogFragment()
    }
}
