package com.example.mymap.socket

import android.content.Context
import android.util.Log
import com.example.mymap.database.AppDatabase
import com.example.mymap.model.ZoneAlert
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.*


class SocketManager(private val context: Context) {
    private var socket: Socket? = null
    var onFriendRequestReceived: ((String) -> Unit)? = null
    var onFriendAccepted: ((String) -> Unit)? = null
    var onFindFriendResult: ((JSONObject) -> Unit)? = null
    var onLocationUpdateReceived: ((JSONArray) -> Unit)? = null
    var onUserInfoReceived: ((JSONObject) -> Unit)? = null

    var onFriendEnterZone: ((String, ZoneAlert) -> Unit)? = null
    var onFriendLeaveZone: ((String, ZoneAlert) -> Unit)? = null


    fun connect() {
        try {
//            socket = IO.socket("http://192.168.1.216:5000")
            socket = IO.socket("http://192.168.2.131:5000")
            socket?.connect()
        }
        catch (e: Exception) {
            Log.d("Tracking_Exception Socket", "Connection error: ${e.message}")
        }

        socket?.on(Socket.EVENT_CONNECT, Emitter.Listener {
            Log.d("Tracking_Socket", "Connected")
        })
        socket?.on(Socket.EVENT_DISCONNECT, Emitter.Listener {
            Log.d("Tracking_Socket", "Disconnected")
        })
        socket?.on(Socket.EVENT_CONNECT_ERROR, Emitter.Listener { args ->
            val error = args[0] as Exception
            Log.d("Tracking_Socket", "Connection error: ${error.message}")
        })
        socket?.on("check-friend", Emitter.Listener { args ->
            try {
                val data = args[0] as JSONObject
                Log.d("Tracking_Socket", "Check friend result: $data")
                onFindFriendResult?.invoke(data)
            } catch (e: Exception) {
                Log.e("Tracking_Socket", "Error processing 'check-friend' event: ${e.message}")
            }
        })

        socket?.on("friend-request", Emitter.Listener { args ->
            val data = args[0] as JSONObject
            Log.d("Tracking_Socket", "Friend request received: $data")
            val final = data.getJSONObject("final")
            val userId = final.getString("id")
//            val userName = final.getString("userName")
//            val phoneNumber = final.getString("phoneNumber")
//            val locationX = final.getDouble("locationX")
//            val locationY = final.getDouble("locationY")
            Log.d("Tracking_Socket", "Friend request received with userId: $userId")
            onFriendRequestReceived?.invoke(userId)
            // Handle friend request
        })
        socket?.on("friend-request") { args ->
            val data = args[0] as JSONObject
            Log.d("Tracking_Socket", "Friend request received: $data")
            val final = data.getJSONObject("final")
            val userId = final.getString("id") // Change this line
//            val userName = final.getString("userName")
//            val phoneNumber = final.getString("phoneNumber")
//            val locationX = final.getDouble("locationX")
//            val locationY = final.getDouble("locationY")
            Log.d("Tracking_Socket", "Friend request received with userId: $userId")
            onFriendRequestReceived?.invoke(userId)
        }

        socket?.on("friend-accepted", Emitter.Listener { args ->
            val data = args[0] as JSONObject
            val receiverInfo = data.getJSONObject("receiverInfo")
            val userId = receiverInfo.getString("id")
//            val userName = receiverInfo.getString("userName")
//            val phoneNumber = receiverInfo.getString("phoneNumber")
//            val locationX = receiverInfo.getDouble("locationX")
//            val locationY = receiverInfo.getDouble("locationY")
            onFriendAccepted?.invoke(userId)

        })

        socket?.on("location-update", Emitter.Listener { args ->
            val data = args[0] as JSONArray
            Log.d("Tracking_Socket", "Location update received: $data")
            onLocationUpdateReceived?.invoke(data)
        })

        socket?.on("user-info", Emitter.Listener { args ->
            val data = args[0] as JSONObject
            Log.d("Tracking_Socket", "User info received: $data")
            onUserInfoReceived?.invoke(data)
            handleLocationUpdate(data)

        })
    }
    private fun handleLocationUpdate(data: JSONObject) {
        if (data.has("locations")) {
            val locationArray = data.getJSONArray("locations")
            for (i in 0 until locationArray.length()) {
                val friendLocation = locationArray.getJSONObject(i)
                val userId = friendLocation.getString("id")
                val locationX = friendLocation.getDouble("locationX")
                val locationY = friendLocation.getDouble("locationY")

                // Fetch zones from RoomDatabase
                val zones = getZonesFromDatabase()

                for (zone in zones) {
                    if (isWithinZone(locationX, locationY, zone)) {
                        onFriendEnterZone?.invoke(userId, zone)
                    } else {
                        onFriendLeaveZone?.invoke(userId, zone)
                    }
                }
            }
        } else {
            Log.e("SocketManager", "No value for locations")
        }
    }

    private fun getZonesFromDatabase(): List<ZoneAlert> {
        val db = AppDatabase.getDatabase(context)
        return runBlocking {
            db.zoneAlertDao().getAllZoneAlerts()
        }
    }

    private fun isWithinZone(locationX: Double, locationY: Double, zone: ZoneAlert): Boolean {
        val earthRadius = 6371e3 // Earth radius in meters

        val lat1 = Math.toRadians(locationX)
        val lon1 = Math.toRadians(locationY)
        val lat2 = Math.toRadians(zone.latitude)
        val lon2 = Math.toRadians(zone.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distance = earthRadius * c

        return distance <= zone.radius
    }


    fun getUserInfo(userId: Int) {
        val data = JSONObject()
        data.put("userId", userId)
        socket?.emit("get-user-info", userId)
        onUserInfoReceived?.invoke(data)
    }
    fun on(eventName: String, listener: Emitter.Listener) {
        socket?.on(eventName, listener)
    }

    fun onFriendRequest(listener: (String) -> Unit) {
        socket?.on("friend-request") { args ->
            val data = args[0] as JSONObject
            val final = data.getJSONObject("final")
            val userId = final.getString("id")
            listener.invoke(userId)
        }
    }
    fun onFindFriendResult(listener: (JSONObject) -> Unit) {
        socket?.on("check-friend") { args ->
            val data = args[0] as JSONObject
            listener.invoke(data)
            Log.d("Tracking_Socket", "onFindFriendResult: $data")
        }
    }


    fun isConnected(): Boolean {
        return socket?.connected() ?: false
    }

    fun disconnect() {
        socket?.disconnect()
    }

    fun register(userId: Int) {
        socket?.emit("register", userId)
    }

    fun sendFriendRequest(senderId: String, receiverId: String) {
        val data = JSONObject()
        data.put("senderId", senderId.toInt())
        data.put("receiverId", receiverId.toInt())
        try {
            socket?.emit("send-friend-request", data)
            Log.d("Tracking_Socket", "Send friend request success")
        } catch (e: Exception) {
            Log.d("Tracking_Socket", "Error: ${e.message}")
        }
    }

    fun acceptFriendRequest(senderId: String, receiverId: String) {
        val data = JSONObject()
        data.put("senderId", senderId.toInt())
        data.put("receiverId", receiverId.toInt())
        socket?.emit("accept-friend-request", data)
    }

    fun findFriend(phoneNumber: String, userId: String) {
        val data = JSONObject()
        data.put("phoneNumber", phoneNumber)
        data.put("userId", userId)
        socket?.emit("find-friend", data)
    }

    fun sendTrackingInfo(userId: String, userName: String, phoneNumber: String, locationX: Double, locationY: Double) {
        val data = JSONObject()
        data.put("userId", userId)
        data.put("userName", userName)
        data.put("phoneNumber", phoneNumber)
        data.put("locationX", locationX)
        data.put("locationY", locationY)
        socket?.emit("tracking", data)
    }

}