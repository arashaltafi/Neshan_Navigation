package com.arash.neshan.test2.ui.main

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arash.neshan.test2.R
import com.arash.neshan.test2.databinding.BottomsheetLocationDetailBinding
import com.arash.neshan.test2.utils.Constants
import com.arash.neshan.test2.utils.setBackStackLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class LocationDetailBottomSheet : BottomSheetDialogFragment() {

    private val binding by lazy {
        BottomsheetLocationDetailBinding.inflate(layoutInflater)
    }

    private val args by navArgs<LocationDetailBottomSheetArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        val dialog = dialog
        if (dialog != null) {
            // avoid default dim background for bottom sheet
            dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

            // when bottom sheet closed -> trigger dismiss listener
            dialog.setOnDismissListener(DialogInterface.OnDismissListener {
                close(true)
            })
        }
    }

    private fun init() = binding.apply {
        route.setOnClickListener {
            if (dialog != null) {
                showNavigationActivity()
                dialog!!.dismiss()
            }
        }

        args.addressResponse?.let {
            binding.address.text = it.address
            binding.title.text = it.routeName
        }

        args.routingResponse?.let {
            try {
                val (summaryResponse, distanceResponse, durationResponse, stepsResponse) = Objects.requireNonNull(
                    it.routes
                )!![0].legs[0]
                distance.text = distanceResponse.text
                duration.text = durationResponse.text
            } catch (exception: NullPointerException) {
                exception.printStackTrace()
            }
        }
    }

    private fun showNavigationActivity() {
        close(Constants.OPEN_NAVIGATION)
    }

    private fun close(value: Any) {
        dismiss()
        findNavController().apply {
            setBackStackLiveData(
                Constants.BACK_FROM_BOTTOM_SHEET,
                value
            )
        }
    }
}