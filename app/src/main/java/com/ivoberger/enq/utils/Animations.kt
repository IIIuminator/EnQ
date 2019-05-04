package com.ivoberger.enq.utils

import android.view.animation.Animation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class KotlinAnimationListener : Animation.AnimationListener {
    private var _onAnimationRepeat: ((animation: Animation?) -> Unit)? = null
    private var _onAnimationEnd: ((animation: Animation?) -> Unit)? = null
    private var _onAnimationStart: ((animation: Animation?) -> Unit)? = null

    override fun onAnimationRepeat(animation: Animation?) {
        _onAnimationRepeat?.invoke(animation)
    }

    fun onAnimationRepeat(func: (animation: Animation?) -> Unit) {
        _onAnimationRepeat = func
    }

    override fun onAnimationEnd(animation: Animation?) {
        _onAnimationEnd?.invoke(animation)
    }

    fun onAnimationEnd(func: (animation: Animation?) -> Unit) {
        _onAnimationEnd = func
    }

    override fun onAnimationStart(animation: Animation?) {
        _onAnimationStart?.invoke(animation)
    }

    fun onAnimationStart(func: (animation: Animation?) -> Unit) {
        _onAnimationStart = func
    }
}

inline fun Animation.setAnimationListener(func: KotlinAnimationListener.() -> Unit) {
    val listener = KotlinAnimationListener()
    listener.func()
    setAnimationListener(listener)
}

suspend inline fun Animation.awaitEnd() = suspendCoroutine<Unit?> { continuation ->
    setAnimationListener { onAnimationEnd { continuation.resume(null) } }
}