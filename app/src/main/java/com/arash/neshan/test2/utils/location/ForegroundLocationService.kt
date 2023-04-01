package com.arash.neshan.test2.utils.location

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import java.util.concurrent.TimeUnit

/**
 * Service tracks location when requested and updates Activity via binding. If Activity is
 * stopped/unbinds and tracking is enabled, the service promotes itself to a foreground service to
 * insure location updates aren't interrupted.
 *
 * For apps running in the background on O+ devices, location is computed much less than previous
 * versions. Please reference documentation for details.
 */
class ForegroundLocationService : Service() {

    companion object {
        const val ACTION_FOREGROUND_LOCATION_BROADCAST =
            "org.neshan.action.FOREGROUND_LOCATION_BROADCAST"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_LAST_LOCATION = "last_location"

        private const val TAG = "LocationService"
        private const val EXTRA_CANCEL_LOCATION_TRACKING_FROM = "cancel_location_tracking"
        private const val NOTIFICATION_ID = 2001
    }

    /**
     * Checks whether the bound activity has really gone away (foreground service with notification
     * created) or simply orientation change (no-op).
     */
    private var mConfigurationChange = false

    private var mServiceRunningInForeground = false

    private val mLocalBinder = LocalBinder()

    private lateinit var mNotificationManager: NotificationManager

    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    // LocationRequest - Requirements for the location updates, i.e., how often you should receive
    // updates, the priority, etc.
    private var mLocationRequest: LocationRequest? = null

    // LocationCallback - Called when FusedLocationProviderClient has a new Location.
    private lateinit var mLocationCallback: LocationCallback

    // Used only for local storage of the last known location. Usually, this would be saved to your
    // database, but because this is a simplified sample without a full database, we only need the
    // last location to create a Notification if the user navigates away from the app.
    private var currentLocation: Location? = null

    override fun onCreate() {

        Log.d(TAG, "onCreate()")

        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize the LocationCallback.
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                currentLocation = locationResult.lastLocation
                Log.d(
                    TAG,
                    "latitude: ${currentLocation?.latitude} longitude: ${currentLocation?.longitude}"
                )

                // Notify our Activity that a new location was added.
                val intent = Intent(ACTION_FOREGROUND_LOCATION_BROADCAST)
                intent.putExtra(EXTRA_LOCATION, currentLocation)
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            }
        }

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        Log.d(TAG, "onStartCommand()")

        val cancelLocationTrackingFromNotification =
            intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM, false)

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        // Tells the system not to recreate the service after it's been killed.
        return START_NOT_STICKY

    }

    override fun onBind(intent: Intent): IBinder {

        Log.d(TAG, "onBind()")

        // Activity (client) comes into foreground and binds to service, so the service can
        // become a background services.
        stopForeground(true)
        mServiceRunningInForeground = false
        mConfigurationChange = false

        return mLocalBinder

    }

    override fun onRebind(intent: Intent) {

        Log.d(TAG, "onRebind()")

        // Activity (client) returns to the foreground and rebinds to service, so the service
        // can become a background services.
        stopForeground(true)
        mServiceRunningInForeground = false
        mConfigurationChange = false

        super.onRebind(intent)

    }

    override fun onUnbind(intent: Intent): Boolean {

        Log.d(TAG, "onUnbind()")

        // MainActivity (client) leaves foreground, so service needs to become a foreground service
        // to maintain the 'while-in-use' label.
        // NOTE: If this method is called due to a configuration change in Activity,
        // we do nothing.
//        if (!mConfigurationChange && SharedPreferenceUtil.getLocationTrackingPref(this)) {
        if (!mConfigurationChange) {
            Log.d(TAG, "Start foreground service")
            startForeground(NOTIFICATION_ID, generateNotification())
            mServiceRunningInForeground = true
        }

        // Ensures onRebind() is called if Activity (client) rebinds.
        return true

    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy()")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        mConfigurationChange = true

    }

    fun setLocationRequest(request: LocationRequest) {
        mLocationRequest = request
    }

    fun getLocationRequest(): LocationRequest? {
        return mLocationRequest
    }

    fun subscribeToLocationUpdates() {

        Log.d(TAG, "subscribeToLocationUpdates()")

//        SharedPreferenceUtil.saveLocationTrackingPref(this, true)

        // Binding to this service doesn't actually trigger onStartCommand(). That is needed to
        // ensure this Service can be promoted to a foreground service, i.e., the service needs to
        // be officially started (which we do here).
        startService(Intent(applicationContext, ForegroundLocationService::class.java))

        try {
            // use default location request if not already set
            if (mLocationRequest == null) {
                mLocationRequest = getDefaultLocationRequest()
            }

            // Subscribe to location changes.
            mFusedLocationProviderClient.requestLocationUpdates(
                mLocationRequest!!,
                mLocationCallback,
                Looper.getMainLooper()
            )
            // check for last cached location
            mFusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    // Notify our Activity that last location loaded
                    val intent = Intent(ACTION_FOREGROUND_LOCATION_BROADCAST)
                    intent.putExtra(EXTRA_LAST_LOCATION, location)
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
                }
            }
        } catch (unlikely: SecurityException) {
//            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }

    }

    fun unsubscribeToLocationUpdates() {

        Log.d(TAG, "unsubscribeToLocationUpdates()")

        try {
            // Unsubscribe to location changes.
            val removeTask = mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
            removeTask.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Location Callback removed.")
                    stopSelf()
                } else {
                    Log.d(TAG, "Failed to remove Location Callback.")
                }
            }
//            SharedPreferenceUtil.saveLocationTrackingPref(this, false)
        } catch (unlikely: SecurityException) {
//            SharedPreferenceUtil.saveLocationTrackingPref(this, true)
            Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
        }

    }

    private fun getDefaultLocationRequest(): LocationRequest {

        return LocationRequest.create().apply {
            // Sets the desired interval for active location updates. This interval is inexact. You
            // may not receive updates at all if no location sources are available, or you may
            // receive them less frequently than requested. You may also receive updates more
            // frequently than requested if other applications are requesting location at a more
            // frequent interval.
            //
            // IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
            // targetSdkVersion) may receive updates less frequently than this interval when the app
            // is no longer in the foreground.
            interval = TimeUnit.SECONDS.toMillis(60)

            // Sets the fastest rate for active location updates. This interval is exact, and your
            // application will never receive updates more frequently than this value.
            fastestInterval = TimeUnit.SECONDS.toMillis(30)

            // Sets the maximum time when batched location updates are delivered. Updates may be
            // delivered sooner than this interval.
            maxWaitTime = TimeUnit.MINUTES.toMillis(2)

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

    }

    /*
     * generates notification for foreground service
     */
    private fun generateNotification(): Notification {

        Log.d(TAG, "generateNotification()")

        // create Notification Channel for O+ and beyond devices (26+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val notificationChannel = NotificationChannel(
                getString(com.arash.neshan.test2.R.string.notification_channel_id),
                getString(com.arash.neshan.test2.R.string.app_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // adds NotificationChannel to system. Attempting to create an
            // existing notification channel with its original values performs
            // no operation, so it's safe to perform the below sequence.
            mNotificationManager.createNotificationChannel(notificationChannel)
        }

        // set up main Intent/Pending Intents for notification.
        val launchActivityIntent =
            Intent(this, Class.forName(getString(com.arash.neshan.test2.R.string.main_activity_class_name)))

        val cancelIntent = Intent(this, ForegroundLocationService::class.java)
        cancelIntent.putExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM, true)

        val servicePendingIntent = PendingIntent.getService(
            this, 0, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val activityPendingIntent = PendingIntent.getActivity(
            this, 0, launchActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // build and issue the notification.
        // Notification Channel Id is ignored for Android pre O (26).
        val notificationCompatBuilder =
            NotificationCompat.Builder(
                applicationContext,
                getString(com.arash.neshan.test2.R.string.notification_channel_id)
            )

        val notificationLayout = RemoteViews(packageName, com.arash.neshan.test2.R.layout.layout_notification)
        notificationLayout.setOnClickPendingIntent(com.arash.neshan.test2.R.id.exit, servicePendingIntent)
        notificationLayout.setOnClickPendingIntent(com.arash.neshan.test2.R.id.container, activityPendingIntent)

        return notificationCompatBuilder
            .setSmallIcon(com.arash.neshan.test2.R.drawable.ic_app_logo)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .setCustomContentView(notificationLayout)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        val service: ForegroundLocationService
            get() = this@ForegroundLocationService
    }

}
