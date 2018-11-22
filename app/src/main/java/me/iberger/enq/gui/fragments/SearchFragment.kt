package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import kotlinx.android.synthetic.main.fragment_search.*
import kotlinx.coroutines.*
import me.iberger.enq.R
import me.iberger.enq.gui.MainActivity
import me.iberger.jmusicbot.data.MusicBotPlugin
import timber.log.Timber

class SearchFragment : Fragment() {

    companion object {
        fun newInstance() = SearchFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private lateinit var mProvider: Deferred<List<MusicBotPlugin>>
    private lateinit var mPagerAdapter: Deferred<Pager>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mProvider = mBackgroundScope.async { MainActivity.musicBot.provider }

        val searchView = ((activity as MainActivity).optionsMenu.findItem(R.id.app_bar_search).actionView as SearchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private var oldText = ""
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.also {
                    oldText = it
                    search(it)
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText == oldText) return true
                newText?.also { oldText = it }
                mBackgroundScope.launch {
                    delay(300)
                    if (oldText != newText) return@launch
                    search(oldText)
                }
                return true
            }
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mBackgroundScope.launch {
            mPagerAdapter = async { Pager(childFragmentManager, mProvider.await()) }
            mUIScope.launch { search_view_pager.adapter = mPagerAdapter.await() }
        }
    }

    fun search(query: String) {
        mBackgroundScope.launch { mPagerAdapter.await().search(query) }
    }

    class Pager(fm: FragmentManager, private val provider: List<MusicBotPlugin>) : FragmentStatePagerAdapter(fm) {

        private val searchResultFragments: MutableList<SearchResultsFragment> = mutableListOf()

        override fun getItem(position: Int): Fragment = SearchResultsFragment.newInstance(provider[position].id)

        override fun getCount(): Int = provider.size

        override fun getPageTitle(position: Int): CharSequence? = provider[position].name

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val currentFragment = super.instantiateItem(container, position) as SearchResultsFragment
            searchResultFragments.add(currentFragment)
            return currentFragment
        }

        fun search(query: String) {
            Timber.d("Searching for $query")
            searchResultFragments.forEach { it.search(query) }
        }
    }
}