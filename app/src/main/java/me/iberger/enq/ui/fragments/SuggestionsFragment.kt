package me.iberger.enq.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import me.iberger.enq.ui.fragments.parents.TabbedSongListFragment
import me.iberger.jmusicbot.JMusicBot
import me.iberger.jmusicbot.model.MusicBotPlugin

class SuggestionsFragment : TabbedSongListFragment() {
    companion object {

        fun newInstance() = SuggestionsFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProviderPlugins = mBackgroundScope.async { JMusicBot.getSuggesters() }
        mBackgroundScope.launch {
            mProviderPlugins.await() ?: return@launch
            mConfig.lastSuggester?.also {
                if (mProviderPlugins.await()!!.contains(it)) mSelectedPlugin = it
            }
        }
    }

    override fun initializeTabs() {
        mBackgroundScope.launch {
            val provider = JMusicBot.getSuggesters()
            mFragmentPagerAdapter =
                    async {
                        SuggestionsFragmentPager(childFragmentManager, provider)
                    }
            mUIScope.launch { view_pager.adapter = mFragmentPagerAdapter.await() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mConfig.lastSuggester = mSelectedPlugin
    }

    inner class SuggestionsFragmentPager(fm: FragmentManager, provider: List<MusicBotPlugin>) :
        TabbedSongListFragment.SongListFragmentPager(fm, provider) {

        override fun getItem(position: Int): Fragment =
            SuggestionResultsFragment.newInstance(provider[position].id)
    }
}