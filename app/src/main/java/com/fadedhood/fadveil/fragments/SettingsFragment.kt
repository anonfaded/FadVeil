package com.fadedhood.fadveil.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.fadedhood.fadveil.databinding.FragmentSettingsBinding
import com.fadedhood.fadveil.theme.ThemeManager

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize switch state
        binding.switchAmoledMode.isChecked = ThemeManager.isAmoledMode(requireContext())

        // Set up switch listener
        binding.switchAmoledMode.setOnCheckedChangeListener { _, isChecked ->
            (activity as? AppCompatActivity)?.let { 
                ThemeManager.setAmoledMode(it, isChecked)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}