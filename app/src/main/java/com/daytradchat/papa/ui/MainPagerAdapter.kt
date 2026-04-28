//app/src/main/java/com/daytradchat/papa/ui/MainPagerAdapter.kt
//ver 2.17-30
package com.daytradchat.papa.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class MainPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 5

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> SignalFragment.newInstance("LONG")
            1 -> SignalFragment.newInstance("SHORT")
            2 -> LogFragment()
            3 -> SettingsFragment()
            4 -> SystemLogFragment()
            else -> LogFragment()
        }
    }
}
