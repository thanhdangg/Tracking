package com.example.mymap.model

import android.app.Application
import com.example.mymap.socket.SocketManager

class MyApplication : Application() {
    lateinit var socketManager: SocketManager
}