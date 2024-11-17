package com.fadedhood.fadveil.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fadedhood.fadveil.databinding.FragmentHomeBinding
import com.fadedhood.fadveil.service.OverlayService
import com.fadedhood.fadveil.utils.FontUtils

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FontUtils.applyFontToFragment(this, view)

        binding.btnRequestPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(requireContext())) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                startActivity(intent)
            }
        }

        binding.btnStartOverlay.setOnClickListener {
            if (Settings.canDrawOverlays(requireContext())) {
                val intent = Intent(requireContext(), OverlayService::class.java)
                requireContext().startService(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission required to show overlay",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}