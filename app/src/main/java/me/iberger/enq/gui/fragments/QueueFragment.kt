package me.iberger.enq.gui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.mikepenz.community_material_typeface_library.CommunityMaterial
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import kotlinx.android.synthetic.main.fragment_queue.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.iberger.enq.R
import me.iberger.enq.gui.adapter.QueueItem
import me.iberger.enq.utils.changeFavoriteStatus
import me.iberger.enq.utils.setupSwipeActions
import me.iberger.enq.utils.toastShort
import me.iberger.jmusicbot.KEY_QUEUE
import me.iberger.jmusicbot.MusicBot
import me.iberger.jmusicbot.data.QueueEntry
import me.iberger.jmusicbot.exceptions.AuthException
import me.iberger.jmusicbot.listener.QueueUpdateListener
import timber.log.Timber

class QueueFragment : Fragment(), QueueUpdateListener, SimpleSwipeCallback.ItemSwipeCallback {
    companion object {

        fun newInstance() = QueueFragment()
    }

    private val mUIScope = CoroutineScope(Dispatchers.Main)
    private val mBackgroundScope = CoroutineScope(Dispatchers.IO)

    private var mQueue: List<QueueEntry> = listOf()
    private lateinit var mFastItemAdapter: FastItemAdapter<QueueItem>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MusicBot.instance.startQueueUpdates(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_queue, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mFastItemAdapter = FastItemAdapter()
        queue.layoutManager = LinearLayoutManager(context).apply { reverseLayout = true }
        queue.adapter = mFastItemAdapter
        savedInstanceState?.also { mFastItemAdapter.withSavedInstanceState(it, KEY_QUEUE) }

        setupSwipeActions(
            context!!, queue, this,
            CommunityMaterial.Icon2.cmd_star, R.color.favorites,
            CommunityMaterial.Icon.cmd_delete, R.color.delete
        )
    }

    override fun onQueueChanged(newQueue: List<QueueEntry>) {
        if (mQueue == newQueue) return
        mQueue = newQueue
        val itemQueue = mQueue.map { QueueItem((it)) }
        mUIScope.launch {
            mFastItemAdapter.set(itemQueue)
        }
    }

    override fun onUpdateError(e: Exception) {
        Timber.e(e)
        Toast.makeText(context, "Something horrific just happened", Toast.LENGTH_SHORT).show()
    }

    override fun itemSwiped(position: Int, direction: Int) {
        mBackgroundScope.launch {
            val entry = mFastItemAdapter.getAdapterItem(position)
            when (direction) {
                ItemTouchHelper.RIGHT -> {
                    try {
                        MusicBot.instance.dequeue(entry.song).await()
                    } catch (e: AuthException) {
                        Timber.e("AuthException with reason ${e.reason}")
                        withContext(Dispatchers.Main) {
                            context!!.toastShort(R.string.msg_no_permission)
                            mFastItemAdapter.notifyAdapterItemChanged(position)
                        }
                    }
                }
                ItemTouchHelper.LEFT -> {
                    changeFavoriteStatus(context!!, entry.song)
                    withContext(Dispatchers.Main) {
                        mFastItemAdapter.notifyAdapterItemChanged(position)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(mFastItemAdapter.saveInstanceState(outState, KEY_QUEUE))
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicBot.instance.stopQueueUpdates(this)
    }
}