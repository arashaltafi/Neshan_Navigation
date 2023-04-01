package com.arash.neshan.test2.utils

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

fun Location.hasMock(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        this.isMock
    else
        this.isFromMockProvider

fun distanceInMeter(
    startLat: Double,
    startLon: Double,
    endLat: Double,
    endLon: Double
): Float {
    val startPoint = Location("locationA")
    startPoint.latitude = startLat
    startPoint.longitude = startLon

    val endPoint = Location("locationB")
    endPoint.latitude = endLat
    endPoint.longitude = endLon
    return startPoint.distanceTo(endPoint)
}

fun Activity.isDarkTheme(): Boolean {
    return this.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}

fun View.toShow() {
    this.visibility = View.VISIBLE
}

fun View.isShow(): Boolean {
    return this.visibility == View.VISIBLE
}

fun View.toHide() {
    this.visibility = View.INVISIBLE
}

fun View.isHide(): Boolean {
    return this.visibility == View.INVISIBLE
}

fun View.toGone() {
    this.visibility = View.GONE
}

fun View.isGone(): Boolean {
    return this.visibility == View.GONE
}


fun Activity.openGoogleMapNavigation(markerLatitude: Double, markerLongitude: Double) =
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$markerLatitude,$markerLongitude&travelmode=driving")
        )
    )

fun Activity.openNeshanNavigation(markerLatitude: Double, markerLongitude: Double) =
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("nshn:$markerLatitude,$markerLongitude")
        )
    )

fun Activity.openChooseNavigation(markerLatitude: Double, markerLongitude: Double) =
    startActivity(
        Intent(
            Intent.ACTION_VIEW,
            Uri.parse("geo: $markerLatitude,$markerLongitude")
        )
    )


inline fun <reified T> NavController.getBackStackLiveData(key: String): MutableLiveData<T>? =
    this.currentBackStackEntry?.savedStateHandle?.getLiveData(key)

fun <T> NavController.setBackStackLiveData(
    key: String,
    value: T?,
    destinationId: Int? = null,
    doBack: Boolean = true,
    inclusive: Boolean = false
) {
    destinationId?.let {
        this.getBackStackEntry(it).savedStateHandle[key] = value
        if (doBack)
            popBackStack(it, inclusive)
    } ?: kotlin.run {
        this.previousBackStackEntry?.savedStateHandle?.set(key, value)
        if (doBack)
            popBackStack()
    }

}

fun <T> flowIO(
    block: suspend FlowCollector<T>.() -> Unit,
) = flow { block() }.flowOn(Dispatchers.IO)

fun <T> channelFlowIO(
    block: suspend ProducerScope<T>.() -> Unit,
) = channelFlow { block() }.flowOn(Dispatchers.IO)

fun <T> flowCompute(
    block: suspend FlowCollector<T>.() -> Unit,
) = flow { block() }.flowOn(Dispatchers.Default)

fun <T> flowMain(
    block: suspend FlowCollector<T>.() -> Unit,
) = flow { block() }.flowOn(Dispatchers.Main)

fun CoroutineScope.superLaunch(
    context: CoroutineContext? = null,
    block: suspend CoroutineScope.() -> Unit,
) = if (context != null)
    launch(context) { supervisorScope(block) }
else launch { supervisorScope(block) }

suspend fun <T> withCompute(
    block: suspend CoroutineScope.() -> T,
) = withContext(Dispatchers.Default) { block(this) }

suspend fun <T> withIO(
    block: suspend CoroutineScope.() -> T,
) = withContext(Dispatchers.IO) { block(this) }

suspend fun <T> withMain(
    block: suspend CoroutineScope.() -> T,
) = withContext(Dispatchers.Main.immediate) { block(this) }

fun <T> CoroutineScope.launchCompute(
    block: suspend CoroutineScope.() -> T
) = launch(Dispatchers.Default) { block() }

fun <T> CoroutineScope.launchIO(
    block: suspend CoroutineScope.() -> T
) = launch(Dispatchers.IO) { block() }

fun <T> CoroutineScope.launchMain(
    block: suspend CoroutineScope.() -> T
) = launch(Dispatchers.Main.immediate) { block() }

fun <T> CoroutineScope.superlaunchCompute(
    block: suspend CoroutineScope.() -> T
) = superLaunch(Dispatchers.Default) { block() }

fun <T> CoroutineScope.superlaunchIO(
    block: suspend CoroutineScope.() -> T
) = superLaunch(Dispatchers.IO) { block() }

fun <T> CoroutineScope.superlaunchMain(
    block: suspend CoroutineScope.() -> T
) = superLaunch(Dispatchers.Main.immediate) { block() }

fun <T> CoroutineScope.asyncCompute(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = async(Dispatchers.Default, start, block)

fun <T> CoroutineScope.asyncIO(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = async(Dispatchers.IO, start, block)

fun <T> CoroutineScope.asyncMain(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
) = async(Dispatchers.Main.immediate, start, block)

fun ViewModel.viewModelCompute(
    block: suspend CoroutineScope.() -> Unit,
) = viewModelScope.launchCompute(block)

fun ViewModel.viewModelMain(
    block: suspend CoroutineScope.() -> Unit,
) = viewModelScope.launchMain(block)

fun ViewModel.viewModelIO(
    block: suspend CoroutineScope.() -> Unit,
) = viewModelScope.launchIO(block)

fun <T> runBlockingCompute(block: suspend CoroutineScope.() -> T) {
    runBlocking(Dispatchers.Default) { block() }
}

fun <T> runBlockingIO(block: suspend CoroutineScope.() -> T) {
    runBlocking(Dispatchers.IO) { block() }
}

fun <T> runBlockingMain(block: suspend CoroutineScope.() -> T) {
    runBlocking(Dispatchers.Main.immediate) { block() }
}

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableExtra(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelable(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelable(key) as? T
}

inline fun <reified T : Parcelable> Bundle.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelableArrayList(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayList(key)
}

inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? = when {
    Build.VERSION.SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
    else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
}

inline fun <reified NEW> Any.cast(): NEW? {
    return if (this.isCastable<NEW>())
        this as NEW
    else null
}

inline fun <reified NEW> Any.isCastable(): Boolean {
    return this is NEW
}

fun <F> runAfter(
    delay: Long, total: Long, fn: (Long) -> F, fc: () -> F,
    unit: TimeUnit = TimeUnit.MILLISECONDS
): Disposable {
    return Flowable.interval(0, delay, unit)
        .observeOn(AndroidSchedulers.mainThread())
        .takeWhile { it != total }
        .doOnNext { fn(it) }
        .doOnComplete { fc() }
        .subscribe()
}

fun <F> runAfter(
    delay: Long, fx: () -> F, unit: TimeUnit = TimeUnit.MILLISECONDS
): Disposable {
    return Completable.timer(delay, unit)
        .observeOn(AndroidSchedulers.mainThread())
        .doOnComplete { fx() }
        .subscribe()
}