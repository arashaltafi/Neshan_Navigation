package com.arash.neshan.test2.main

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.lifecycle.ViewModelProvider
import com.arash.neshan.test2.R
import com.arash.neshan.test2.data.model.LatLng
import com.arash.neshan.test2.data.model.enums.RoutingType
import com.arash.neshan.test2.data.model.response.RoutingResponse
import com.arash.neshan.test2.databinding.BottomsheetLocationDetailBinding
import com.arash.neshan.test2.navigation.NavigationActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class LocationDetailBottomSheet : BottomSheetDialogFragment() {
    private var mBinding: BottomsheetLocationDetailBinding? = null
    private var mSharedViewModel: MainViewModel? = null
    private var mapStyle: Int = 1

    fun getMapStyle(mapStyle: Int) {
        this.mapStyle = mapStyle
    }

    // trigger action when bottom sheet closes
    private var mOnDismissListener: DialogInterface.OnDismissListener? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set bottom sheet theme
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mBinding = BottomsheetLocationDetailBinding.inflate(layoutInflater, container, false)
        return mBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dialog = dialog
        if (dialog != null) {
            // avoid default dim background for bottom sheet
            dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            // when bottom sheet closed -> trigger dismiss listener
            dialog.setOnDismissListener(DialogInterface.OnDismissListener { d: DialogInterface? ->
                dismiss()
                if (mOnDismissListener != null) mOnDismissListener!!.onDismiss(d)
            })
        }
        mSharedViewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        observeViewModelChange(mSharedViewModel)
        // bind data to view
        mBinding?.viewModel = mSharedViewModel
        setViewListeners()
    }

    fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        this.mOnDismissListener = listener
    }

    private fun setViewListeners() {
        mBinding?.route?.setOnClickListener {
            if (dialog != null) {
                showNavigationActivity()
                dialog!!.dismiss()
            }
        }
    }

    private fun observeViewModelChange(viewModel: MainViewModel?) {
        // load direction (here by default for car)
        viewModel!!.loadDirection(RoutingType.CAR)
        viewModel.routingDetailLiveData.observe(
            viewLifecycleOwner
        ) { routingDetail: RoutingResponse? ->
            if (routingDetail != null) {
                mBinding?.route?.isEnabled = true
                mBinding?.route?.setBackgroundResource(R.drawable.bg_radius_primary_25)
                try {
                    val (summary, distance, duration, steps) = Objects.requireNonNull(routingDetail.routes)!![0].legs[0]
                    mBinding?.distance?.text = distance.text
                    mBinding?.duration?.text = duration.text
                } catch (exception: NullPointerException) {
                    exception.printStackTrace()
                }
            }
        }
    }

    private fun showNavigationActivity() {
        val intent = Intent(requireActivity(), NavigationActivity::class.java)
        val start = mSharedViewModel!!.startPoint
        val end = mSharedViewModel!!.endPoint
        if (start != null && end != null) {
            val startPoint = LatLng(
                start.latitude,
                start.longitude
            )
            val endPoint = LatLng(
                end.latitude,
                end.longitude
            )
            intent.putExtra(NavigationActivity.EXTRA_START_POINT, startPoint)
            intent.putExtra(NavigationActivity.EXTRA_END_POINT, endPoint)
            intent.putExtra("MAP_STYLE", mapStyle)
            startActivity(intent)
        }
    }
}