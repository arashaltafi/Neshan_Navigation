package com.arash.neshan.test2.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.view.View
import com.arash.neshan.test2.R
import com.arash.neshan.test2.domain.model.error.*
import com.arash.neshan.test2.utils.snackbar.SnackBar
import com.arash.neshan.test2.utils.snackbar.SnackBarType
import org.neshan.common.model.LatLng
import retrofit2.HttpException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.atan2

fun Drawable.toBitmap(): Bitmap {

    if (this is BitmapDrawable) {
        return this.bitmap
    }

    val bitmap: Bitmap = Bitmap.createBitmap(
        this.intrinsicWidth,
        this.intrinsicHeight,
        Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    this.setBounds(0, 0, canvas.width, canvas.height)
    this.draw(canvas)

    return bitmap

}

/**
 * get error detail from Throwable object
 * */
fun Throwable.getError(): GeneralError {

    this.printStackTrace()

    return when (this) {
        is UnknownHostException, is SocketException -> {
            NetworkError.instance()
        }
        is SocketTimeoutException -> {
            TimeoutError.instance()
        }
        is HttpException -> {
            // TODO: improve parsing server errors
            return ServerError(this.response()?.code() ?: 0)
        }
        else -> {
            UnknownError.instance()
        }
    }
}

/**
 * show error as snack bar
 * */
fun showError(rootView: View, error: GeneralError) {
    when (error) {
        is NetworkError -> {
            SnackBar.make(rootView, R.string.network_connection_error, SnackBarType.ERROR).show()
        }
        is ServerError -> {
            SnackBar.make(rootView, R.string.unknown_server_error, SnackBarType.ERROR).show()
        }
        is SimpleError -> {
            SnackBar.make(rootView, error.errorMessage, SnackBarType.ERROR).show()
        }
        is UnknownError -> {
            SnackBar.make(rootView, R.string.unknown_error, SnackBarType.ERROR).show()
        }
    }
}

/**
 * calculates distance to target point
 * */
fun LatLng.distanceFrom(latLng: LatLng): FloatArray {

    val distanceResult = FloatArray(3)

    Location.distanceBetween(
        this.latitude,
        this.longitude,
        latLng.latitude,
        latLng.longitude,
        distanceResult
    )

    return distanceResult;

}

/**
 * checks points are the same
 * */
fun LatLng.equalsTo(latLng: LatLng): Boolean {

    return (this.latitude == latLng.latitude && this.longitude == latLng.longitude)

}

/**
 * calculate angle between two point (LatLng) with north axis
 * */
fun angleWithNorthAxis(p1: LatLng, p2: LatLng): Double {

    val longDiff = p2.longitude - p1.longitude

    val a = atan2(
        StrictMath.sin(longDiff) * StrictMath.cos(p2.latitude),
        StrictMath.cos(p1.latitude) * StrictMath.sin(p2.latitude)
                - StrictMath.sin(p1.latitude)
                * StrictMath.cos(p2.latitude)
                * StrictMath.cos(longDiff)
    ) * 180 / StrictMath.PI

    return (a + 360) % 360

}