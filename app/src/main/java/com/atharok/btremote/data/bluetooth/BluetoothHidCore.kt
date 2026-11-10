package com.atharok.btremote.data.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.ServiceListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresPermission
import com.atharok.btremote.common.utils.DELAY_BETWEEN_KEY_PRESSES_IN_MILLIS
import com.atharok.btremote.common.utils.KEYBOARD_REPORT_ID
import com.atharok.btremote.common.utils.REMOTE_INPUT_NONE
import com.atharok.btremote.common.utils.checkBluetoothConnectPermission
import com.atharok.btremote.common.utils.isMacAddress
import com.atharok.btremote.domain.entities.DeviceHidConnectionState
import com.atharok.btremote.domain.entities.remoteInput.RemoteButtonProperties
import com.atharok.btremote.domain.entities.remoteInput.keyboard.virtualKeyboard.VirtualKeyboardLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

class BluetoothHidCore(
    private val context: Context,
    private val adapter: BluetoothAdapter?,
    private val hidSettings: BluetoothHidDeviceAppSdpSettings
) {
    private var bluetoothHidDevice: BluetoothHidDevice? = null

    private val _isBluetoothServiceRunning: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isBluetoothServiceRunning: StateFlow<Boolean> = _isBluetoothServiceRunning

    // True while registered or still trying to be; false only once the tries have run out,
    // which is when the screen offers Retry.
    private val _isBluetoothHidProfileRegistered: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isBluetoothHidProfileRegistered: StateFlow<Boolean> = _isBluetoothHidProfileRegistered

    // True from the first try until Bluetooth confirms, the tries run out, or it is stopped.
    private val _isRegistering: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRegistering: StateFlow<Boolean> = _isRegistering

    private var autoConnectDeviceAddress: String = ""

    // Bluetooth callbacks arrive on a binder thread; everything here runs on the main thread.
    private val mainHandler = Handler(Looper.getMainLooper())

    private val registration = HidRegistration(
        register = {
            val hidDevice = bluetoothHidDevice
            hidDevice != null && checkBluetoothConnectPermission(context) &&
                hidDevice.registerApp(hidSettings, null, null, Runnable::run, callback)
        },
        unregister = {
            val hidDevice = bluetoothHidDevice
            hidDevice != null && checkBluetoothConnectPermission(context) && hidDevice.unregisterApp()
        },
        schedule = { delayMillis, action -> mainHandler.postDelayed(action, delayMillis) },
        onGiveUp = {
            _isRegistering.value = false
            _isBluetoothHidProfileRegistered.value = false
        }
    )

    // The host Remote was last connected to, kept until the person disconnects, so a connection
    // the phone dropped by itself (the screen locked) is made again once Remote is registered.
    @Volatile private var lastHostAddress: String? = null
    private var reconnectAddress: String? = null

    // ---- HID Profile ----

    fun startBluetoothHidProfile(autoConnectDeviceAddress: String) {
        this.autoConnectDeviceAddress = autoConnectDeviceAddress
        val hidDevice = bluetoothHidDevice
        if (hidDevice != null) {
            // Already has Bluetooth's HID service: register only if it is not, or trying to be.
            if (!_isBluetoothHidProfileRegistered.value) startRegistration()
        } else {
            adapter?.getProfileProxy(context, serviceListener, BluetoothProfile.HID_DEVICE)
        }
    }

    /**
     * Back on screen (unlocked, or reopened): if Bluetooth dropped the registration and the
     * tries ran out while the phone was locked, try again now, before the error shows.
     */
    fun ensureRegistered() {
        if (_isBluetoothServiceRunning.value && !_isBluetoothHidProfileRegistered.value &&
            bluetoothHidDevice != null) {
            reconnectAddress = lastHostAddress
            startRegistration()
        }
    }

    private fun startRegistration() {
        _isBluetoothHidProfileRegistered.value = true
        _isRegistering.value = true
        registration.start()
    }

    fun stopBluetoothHidProfile() {
        registration.cancel()
        _isRegistering.value = false
        mainHandler.removeCallbacksAndMessages(null)
        lastHostAddress = null
        reconnectAddress = null
        bluetoothHidDevice?.let { hidDevice ->
            if (checkBluetoothConnectPermission(context)) {
                disconnectDevice(hidDevice)
                hidDevice.unregisterApp()
            }
            adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        }
        bluetoothHidDevice = null
        _deviceHidConnectionState.value = DeviceHidConnectionState()
        _isBluetoothServiceRunning.value = false
    }

    // ---- BluetoothProfile.ServiceListener implementation ----

    private val serviceListener = object : ServiceListener {
        override fun onServiceConnected(i: Int, bluetoothProfile: BluetoothProfile?) {
            if(i == BluetoothProfile.HID_DEVICE) {
                bluetoothHidDevice = bluetoothProfile as? BluetoothHidDevice?
                if (bluetoothHidDevice != null && checkBluetoothConnectPermission(context)) {
                    startRegistration()
                }
            }
            _isBluetoothServiceRunning.value = true
        }

        // Bluetooth's HID service went away (Bluetooth restarting). The proxy is dead; Android
        // connects it again by itself, and onServiceConnected registers afresh.
        override fun onServiceDisconnected(i: Int) {
            if (i == BluetoothProfile.HID_DEVICE) {
                registration.cancel()
                _isRegistering.value = false
                bluetoothHidDevice = null
                bluetoothDevice = null
                _deviceHidConnectionState.value = DeviceHidConnectionState()
            }
        }
    }

    // ---- BluetoothHidDevice.Callback implementation ----

    private val callback = object : BluetoothHidDevice.Callback() {

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            super.onAppStatusChanged(pluggedDevice, registered)
            mainHandler.post { onAppStatus(registered) }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice?, state: Int) {
            super.onConnectionStateChanged(device, state)
            if (checkBluetoothConnectPermission(context)) {
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    device?.address?.let { lastHostAddress = it }
                }
                _deviceHidConnectionState.value = DeviceHidConnectionState(state, device?.name ?: "")
            }
        }
    }

    private fun onAppStatus(registered: Boolean) {
        if (bluetoothHidDevice == null) return
        if (registered) {
            registration.confirmed()
            _isRegistering.value = false
            _isBluetoothHidProfileRegistered.value = true
            val address = reconnectAddress ?: autoConnectDeviceAddress
            reconnectAddress = null
            if (isMacAddress(address)) {
                connectDevice(address)
            }
        } else if (!registration.isTrying) {
            // Bluetooth dropped the registration (the screen locked): register again, and go
            // back to the host that was connected. While trying, a "not registered" is only the
            // old registration being cleared.
            reconnectAddress = lastHostAddress
            startRegistration()
        }
    }

    // ---- Device Connection ----

    private var bluetoothDevice: BluetoothDevice? = null

    fun connectDevice(deviceAddress: String): Boolean {
        if (checkBluetoothConnectPermission(context)) {
            bluetoothHidDevice?.let {
                return connectDevice(it, deviceAddress)
            }
        }
        return false
    }

    fun disconnectDevice(): Boolean {
        lastHostAddress = null
        reconnectAddress = null
        if (checkBluetoothConnectPermission(context)) {
            bluetoothHidDevice?.let {
                return disconnectDevice(it)
            }
        }
        return false
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun connectDevice(hidDevice: BluetoothHidDevice, deviceAddress: String): Boolean {
        var isConnected = false
        bluetoothDevice = adapter?.getRemoteDevice(deviceAddress.uppercase(Locale.getDefault()))
        bluetoothDevice?.let {
            isConnected = hidDevice.connect(it)
        }
        return isConnected
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private fun disconnectDevice(hidDevice: BluetoothHidDevice): Boolean {
        var isDisconnected = false
        bluetoothDevice?.let {
            // disconnect() is called twice because, during my tests on a Pixel 3a, the state
            // changes to DISCONNECTING but never to DISCONNECTED if the call is made only once.
            // This error occurs only on this device and not on the others I tested. It seems to
            // be due to a bug in the library or the device.
            hidDevice.disconnect(it)
            isDisconnected = hidDevice.disconnect(it)
        }
        bluetoothDevice = null
        return isDisconnected
    }

    // ---- StateFlow ----

    private val _deviceHidConnectionState: MutableStateFlow<DeviceHidConnectionState> = MutableStateFlow(
        DeviceHidConnectionState()
    )
    val deviceHidConnectionState: StateFlow<DeviceHidConnectionState> = _deviceHidConnectionState

    // ---- Send Report ----

    fun sendReport(id: Int, bytes: ByteArray): Boolean {
        var success = false
        bluetoothDevice?.let { device ->
            bluetoothHidDevice?.let { hidDevice ->
                if (checkBluetoothConnectPermission(context)) {
                    success = hidDevice.sendReport(device, id, bytes)
                }
            }
        }
        return success
    }

    suspend fun sendTextReport(
        text: String,
        virtualKeyboardLayout: VirtualKeyboardLayout,
        shouldSendEnter: Boolean
    ): Boolean {
        var success = false
        bluetoothDevice?.let { device ->
            bluetoothHidDevice?.let { hidDevice ->
                if (checkBluetoothConnectPermission(context)) {
                    success = true
                    text.forEach {
                        if(!hidDevice.sendReport(device, KEYBOARD_REPORT_ID, virtualKeyboardLayout.getKeyboardKey(it))) {
                            success = false
                        }
                        delay(DELAY_BETWEEN_KEY_PRESSES_IN_MILLIS)
                        hidDevice.sendReport(device, KEYBOARD_REPORT_ID, REMOTE_INPUT_NONE)
                        delay(DELAY_BETWEEN_KEY_PRESSES_IN_MILLIS)
                    }
                    if(shouldSendEnter) {
                        hidDevice.sendReport(device, KEYBOARD_REPORT_ID, RemoteButtonProperties.KeyboardEnterButton.input)
                        delay(DELAY_BETWEEN_KEY_PRESSES_IN_MILLIS)
                        hidDevice.sendReport(device, KEYBOARD_REPORT_ID, REMOTE_INPUT_NONE)
                    }
                }
            }
        }
        return success
    }
}