package com.example.esp32voltage

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var deviceSpinner: Spinner
    private lateinit var connectButton: Button
    private lateinit var statusText: TextView
    private lateinit var voltageText: TextView

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    private var socket: BluetoothSocket? = null
    private var readThread: Thread? = null
    private var devices = listOf<BluetoothDevice>()

    // Standard Bluetooth Classic SPP UUID
    private val sppUuid: UUID =
        UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    companion object {
        private const val REQUEST_BLUETOOTH = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()

        if (bluetoothAdapter == null) {
            statusText.text = "이 휴대폰은 Bluetooth를 지원하지 않습니다."
            connectButton.isEnabled = false
            return
        }

        requestBluetoothPermissionIfNeeded()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFFFFFF.toInt())
            setPadding(dp(24), dp(28), dp(24), dp(24))
        }

        val title = TextView(this).apply {
            text = "ESP32 전압"
            textSize = 22f
            setTextColor(0xFF000000.toInt())
        }

        deviceSpinner = Spinner(this)

        connectButton = Button(this).apply {
            text = "Bluetooth 연결"
            setOnClickListener { connectSelectedDevice() }
        }

        statusText = TextView(this).apply {
            text = "연결할 ESP32를 선택하세요."
            textSize = 15f
            setTextColor(0xFF555555.toInt())
        }

        voltageText = TextView(this).apply {
            text = "-- V"
            textSize = 64f
            setTextColor(0xFF000000.toInt())
            gravity = android.view.Gravity.CENTER
        }

        root.addView(title, LinearLayout.LayoutParams(-1, dp(50)))
        root.addView(deviceSpinner, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(connectButton, LinearLayout.LayoutParams(-1, dp(55)))
        root.addView(statusText, LinearLayout.LayoutParams(-1, dp(50)))
        root.addView(
            voltageText,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        setContentView(root)
    }

    private fun requestBluetoothPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BLUETOOTH
            )
        } else {
            loadPairedDevices()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH &&
            grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            loadPairedDevices()
        } else {
            statusText.text = "Bluetooth 권한이 필요합니다."
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadPairedDevices() {
        val adapter = bluetoothAdapter ?: return

        if (!adapter.isEnabled) {
            statusText.text = "휴대폰 Bluetooth를 켜주세요."
            return
        }

        devices = adapter.bondedDevices.toList().sortedBy { it.name ?: it.address }

        if (devices.isEmpty()) {
            deviceSpinner.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("페어링된 기기가 없습니다.")
            )
            statusText.text = "먼저 ESP32를 휴대폰 Bluetooth 설정에서 페어링하세요."
            connectButton.isEnabled = false
            return
        }

        val names = devices.map { "${it.name ?: "이름 없음"}\n${it.address}" }

        deviceSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            names
        )

        connectButton.isEnabled = true
        statusText.text = "ESP32를 선택하고 연결하세요."
    }

    @SuppressLint("MissingPermission")
    private fun connectSelectedDevice() {
        if (devices.isEmpty()) return

        connectButton.isEnabled = false
        statusText.text = "연결 중..."
        voltageText.text = "-- V"

        readThread?.interrupt()
        try { socket?.close() } catch (_: Exception) {}

        val device = devices[deviceSpinner.selectedItemPosition]

        readThread = thread {
            try {
                bluetoothAdapter?.cancelDiscovery()

                val newSocket = device.createRfcommSocketToServiceRecord(sppUuid)
                socket = newSocket
                newSocket.connect()

                runOnUiThread {
                    statusText.text = "연결됨: ${device.name ?: device.address}"
                    connectButton.text = "다시 연결"
                    connectButton.isEnabled = true
                }

                val reader = BufferedReader(
                    InputStreamReader(newSocket.inputStream)
                )

                while (!Thread.currentThread().isInterrupted) {
                    val line = reader.readLine() ?: break
                    val voltage = line.trim().toDoubleOrNull() ?: continue

                    runOnUiThread {
                        voltageText.text = formatVoltage(voltage)
                    }
                }

                runOnUiThread {
                    statusText.text = "연결이 끊어졌습니다."
                    connectButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    statusText.text = "연결 실패: ${e.message ?: "알 수 없는 오류"}"
                    connectButton.isEnabled = true
                }
            }
        }
    }

    private fun formatVoltage(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format("%.0f V", value)
        } else {
            String.format("%.2f V", value)
        }
    }

    override fun onDestroy() {
        readThread?.interrupt()
        try { socket?.close() } catch (_: Exception) {}
        super.onDestroy()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
