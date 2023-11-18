package de.simon.dankelmann.bluetoothlespam

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.onNavDestinationSelected
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.navigation.NavigationView
import de.simon.dankelmann.bluetoothlespam.AppContext.AppContext
import de.simon.dankelmann.bluetoothlespam.Constants.Constants
import de.simon.dankelmann.bluetoothlespam.Database.AppDatabase
import de.simon.dankelmann.bluetoothlespam.Helpers.BluetoothHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.DatabaseHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.QueueHandlerHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers
import de.simon.dankelmann.bluetoothlespam.Helpers.StringHelpers.Companion.toHexString
import de.simon.dankelmann.bluetoothlespam.PermissionCheck.PermissionCheck
import de.simon.dankelmann.bluetoothlespam.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val _logTag = "MainActivity"
    private lateinit var sharedPreferenceChangedListener:OnSharedPreferenceChangeListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AppContext, Activity, Advertisement Service and QueHandler
        AppContext.setContext(this)
        AppContext.setActivity(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Custom toolbar
        val toolbar = findViewById<Toolbar>(R.id.customToolbar)
        setSupportActionBar(toolbar)

        // Listen to Preference changes
        var prefs = PreferenceManager.getDefaultSharedPreferences(this);

        sharedPreferenceChangedListener = OnSharedPreferenceChangeListener { sharedPreferences, key ->
            run {
                var legacyAdvertisingKey = AppContext.getActivity().resources.getString(R.string.preference_key_use_legacy_advertising)
                if (key == legacyAdvertisingKey) {
                    val advertisementService = BluetoothHelpers.getAdvertisementService()
                    AppContext.setAdvertisementService(advertisementService)
                    AppContext.getAdvertisementSetQueueHandler().setAdvertisementService(advertisementService)
                }

                var intervalKey = AppContext.getActivity().resources.getString(R.string.preference_key_interval_advertising_queue_handler)
                if (key == intervalKey) {
                    var newInterval = QueueHandlerHelpers.getInterval()
                    Log.d(_logTag, "Setting new Interval: $newInterval")
                    AppContext.getAdvertisementSetQueueHandler().setInterval(newInterval)
                }
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangedListener);

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_start,
                /*
                R.id.nav_fast_pairing,
                R.id.nav_swift_pair,
                R.id.nav_continuity_action_modals,
                R.id.nav_continuity_device_popups,
                R.id.nav_kitchen_sink*/
            ), drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun Context.hasRequiredRuntimePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if(PermissionCheck.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, this)){
                startActivityForResult(enableBtIntent, Constants.REQUEST_CODE_ENABLE_BLUETOOTH)
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean
    {
        val actionSettingsMenuItem = menu?.findItem(R.id.nav_preferences)
        val title = actionSettingsMenuItem?.title.toString()
        val spannable = SpannableString(title)

        val textColor = resources.getColor(R.color.text_color, AppContext.getContext().theme)
        spannable.setSpan(ForegroundColorSpan(textColor), 0, spannable.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        actionSettingsMenuItem?.title = spannable

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.nav_preferences -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                onNavDestinationSelected(item, navController)
            }
        }

        return super.onOptionsItemSelected(item)
    }
}