package com.example.bluetoothbroadcaster

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bluetoothbroadcaster.databinding.ActivityMainBinding
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var connectedDevice: BluetoothDevice
    private lateinit var bluetoothSocket: BluetoothSocket
    lateinit var workerThread: Thread
    private lateinit var readBuffer: ByteArray
    var readBufferPosition = 0
    lateinit var mmOutputStream: OutputStream
    lateinit var mmInputStream: InputStream


    @Volatile
    var stopWorker = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.open.setOnClickListener {
            findBT()
            openBT()
        }

        binding.send.setOnClickListener {
            sendData()
        }

        binding.close.setOnClickListener {
            try {
                closeBT()
            } catch (ex: IOException) {
            }
        }
    }

    private fun findBT(): Boolean {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Toast.makeText(applicationContext, "No bluetooth adapter available", Toast.LENGTH_SHORT)
                .show()
            return false
        }
        if (!adapter.isEnabled) {
            val enableBluetooth = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBluetooth, 0)
        }
        val pairedDevices: Set<BluetoothDevice> = adapter.bondedDevices
        if (pairedDevices.isNotEmpty()) {
            for (device in pairedDevices) {
                Log.d("device", device.name)
                if (device.name == "test") {
                    Log.d("device", device.name)
                    connectedDevice = device
                    break
                }
            }
        }
        Toast.makeText(applicationContext, "Bluetooth Device Found", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun openBT() {
        val uuid: UUID =
            UUID.fromString("FFFD1EE4-093D-4F71-96AA-EE5A1AEA2B11") //Standard SerialPortService ID
        bluetoothSocket = connectedDevice.createRfcommSocketToServiceRecord(uuid)
        bluetoothSocket.connect()
        mmOutputStream = bluetoothSocket.outputStream;
        mmInputStream = bluetoothSocket.inputStream;
        Log.d("pook", mmOutputStream.toString())
        beginListenForData()
        Toast.makeText(applicationContext, "Bluetooth Opened", Toast.LENGTH_SHORT).show()
    }


    private fun beginListenForData() {
        val handler = Handler(Looper.getMainLooper())
        val delimiter: Byte = 10 //This is the ASCII code for a newline character
        stopWorker = false
        readBufferPosition = 0
        readBuffer = ByteArray(1024)
        workerThread = Thread {
            while (!Thread.currentThread().isInterrupted && !stopWorker) {
                try {
                    val bytesAvailable: Int = mmInputStream.available()
                    if (bytesAvailable > 0) {
                        val packetBytes = ByteArray(bytesAvailable)
                        mmInputStream.read(packetBytes)
                        for (i in 0 until bytesAvailable) {
                            val b = packetBytes[i]
                            if (b == delimiter) {
                                val encodedBytes = ByteArray(readBufferPosition)
                                System.arraycopy(
                                    readBuffer,
                                    0,
                                    encodedBytes,
                                    0,
                                    encodedBytes.size
                                )
                                val data = String(encodedBytes)
                                readBufferPosition = 0
                                handler.post { binding.label.text = data }
                            } else {
                                readBuffer[readBufferPosition++] = b
                            }
                        }
                    }
                } catch (ex: IOException) {
                    stopWorker = true
                }
            }
        }
        workerThread.start()
    }

    private fun sendData() {
        var msg: String = binding.entry.text.toString()
        msg += "\n"
        mmOutputStream.write(msg.toByteArray())
        Toast.makeText(applicationContext, "Data sent", Toast.LENGTH_SHORT).show()
    }

    private fun closeBT() {
        stopWorker = true
        mmInputStream.close()
        mmOutputStream.close()
        bluetoothSocket.close()
        Toast.makeText(applicationContext, "Bluetooth closed", Toast.LENGTH_SHORT).show()
    }
}