package com.example.mymap.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.mymap.R
import com.example.mymap.databinding.FragmentSettingBinding
import com.example.mymap.model.MyApplication
import com.example.mymap.socket.SocketManager


class SettingFragment : Fragment() {

    private lateinit var binding: FragmentSettingBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingBinding.inflate(inflater, container, false)

        binding.btnConnectServer.setOnClickListener {
            val userId = binding.userId.text.toString()
            val application = context?.applicationContext as? MyApplication
            if (application == null) {
                Log.d("Tracking_SettingFragment", "Application is null")
                return@setOnClickListener
            }

            application.socketManager = SocketManager()
            try{
                application.socketManager.connect()
                application.socketManager.register(userId.toInt())
                Log.d("Tracking_SettingFragment", "Connect success")
                binding.btnConnectServer.text = "Connected to server"
                binding.btnConnectServer.background = ColorDrawable(Color.parseColor("#00BFFF"))
                binding.btnConnectServer.isEnabled = false

                val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return@setOnClickListener
                with (sharedPref.edit()) {
                    putString("userId", userId)
                    apply()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("Tracking_SettingFragment", "Error: ${e.message}")
            }

        }


        return binding.root
    }



}