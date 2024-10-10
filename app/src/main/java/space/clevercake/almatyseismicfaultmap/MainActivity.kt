package space.clevercake.almatyseismicfaultmap

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.WindowMetrics
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import android.view.inputmethod.EditorInfo
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd

const val R_EARTH: Double = 6371.0 // Радиус Земли в километрах
class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
//    private lateinit var textSearch: AutoCompleteTextView
    private lateinit var suggestionsAdapter: ArrayAdapter<String>

    private val ALMATY_CENTER = LatLng(43.238949, 76.889709)
    private val RADIUS_ALMATY = 40.0

    private lateinit var adView: AdView
    private lateinit var addContainerView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}
        loadBanner()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
//        textSearch = findViewById(R.id.autocomplete_text)
        suggestionsAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line)
//        textSearch.setAdapter(suggestionsAdapter)
//
//        textSearch.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                s?.let { getSuggestions(it.toString()) }
//            }
//
//            override fun afterTextChanged(s: Editable?) {}
//        })
//
//        textSearch.setOnEditorActionListener { _, actionId, _ ->
//            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
//                performSearch(textSearch.text.toString())
//                true
//            } else {
//                false
//            }
//        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

//    private fun getSuggestions(query: String) {
//        val geocoder = Geocoder(this, Locale.getDefault())
//        try {
//            val addresses = geocoder.getFromLocationName(query, 5)
//            val suggestions = addresses?.filter {
//                isWithinRadius(LatLng(it.latitude, it.longitude), ALMATY_CENTER, RADIUS_ALMATY)
//            }?.map { it.getAddressLine(0) } ?: emptyList()
//            suggestionsAdapter.clear()
//            suggestionsAdapter.addAll(suggestions)
//            suggestionsAdapter.notifyDataSetChanged()
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Error retrieving location", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun performSearch(query: String) {
//        val geocoder = Geocoder(this, Locale.getDefault())
//        try {
//            val addresses = geocoder.getFromLocationName(query, 1)
//            val filteredAddresses = addresses?.filter {
//                isWithinRadius(LatLng(it.latitude, it.longitude), ALMATY_CENTER, RADIUS_ALMATY)
//            }
//            if (filteredAddresses != null && filteredAddresses.isNotEmpty()) {
//                val address = filteredAddresses[0]
//                val latLng = LatLng(address.latitude, address.longitude)
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
//                mMap.addMarker(MarkerOptions().position(latLng).title(address.getAddressLine(0)))
//            } else {
//                Toast.makeText(this, "Location not found in the specified area", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Toast.makeText(this, "Error performing search", Toast.LENGTH_SHORT).show()
//        }
//    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap //обозначили карту
        mMap.isBuildingsEnabled = true //подключили здания
        mMap.isIndoorEnabled = true //для того чтобы видеть построение сдания

        drawLines(mMap)

//        // Перемещение камеры к первой точке линии
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ALMATY_CENTER, 15f))
        mMap.addMarker(MarkerOptions().position(ALMATY_CENTER).title(getString(R.string.center_Almaty)))
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(line[0], 15f))

        // Проверка разрешений
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }



        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                if (isWithinRadius(currentLatLng, ALMATY_CENTER, RADIUS_ALMATY)) {//проверка ты в алмат или нет
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title(getString(R.string.here)))
                } else {
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ALMATY_CENTER, 15f))
                    mMap.addMarker(MarkerOptions().position(ALMATY_CENTER).title(getString(R.string.center_Almaty)))
                }
            } ?: run {//не вернули значение доступа к меестоположению пользователя
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ALMATY_CENTER, 15f))
                mMap.addMarker(MarkerOptions().position(ALMATY_CENTER).title(getString(R.string.center_Almaty)))
            }
        }
    }
    private fun isWithinRadius(location: LatLng, center: LatLng, radius: Double): Boolean {
//        cos(d) = sin(φА)·sin(φB) + cos(φА)·cos(φB)·cos(λА − λB),
//        где φА и φB — широты, λА, λB — долготы данных пунктов, d — расстояние между пунктами, измеряемое в радианах длиной дуги большого круга земного шара.
//        Расстояние между пунктами, измеряемое в километрах, определяется по формуле:
//        L = d·R,
        val latitudeDistance = Math.toRadians(location.latitude - center.latitude)
        val longitudeDistance = Math.toRadians(location.longitude - center.longitude)
        val a = sin(latitudeDistance / 2) * sin(latitudeDistance / 2) +
                cos(Math.toRadians(center.latitude)) * cos(Math.toRadians(location.latitude)) *
                sin(longitudeDistance / 2) * sin(longitudeDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val distance = R_EARTH * c
        return distance <= radius
    }

    private fun loadBanner() {
        // Создание нового AdView
        adView = AdView(this)
        adView.adUnitId = "ca-app-pub-9702271696859992/8395912726"
        adView.setAdSize(adSize)


        // Находим контейнер FrameLayout
        addContainerView = findViewById(R.id.adView)
        addContainerView.removeAllViews()
        addContainerView.addView(adView)


        // Создание Bundle с параметром "collapsible"
        val extras = Bundle()
        extras.putString("collapsible", "bottom")  // Или "top", в зависимости от того, как вы хотите расположить расширение


        // Создание запроса объявления с дополнительным параметром
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(AdMobAdapter::class.java,extras)
            .build()


        // Загрузка объявления
        adView.loadAd(adRequest)
    }
    private val adSize: AdSize
        get() {
            val displayMetrics = resources.displayMetrics
            val adWidthPixels =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val windowMetrics: WindowMetrics = this.windowManager.currentWindowMetrics
                    windowMetrics.bounds.width()
                } else {
                    displayMetrics.widthPixels
                }
            val density = displayMetrics.density
            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private fun drawLines(gMap: GoogleMap) {
        // Пример координат для рисования линий
        val tectonicFaults = listOf(
//            1
            listOf(
                LatLng(43.291683, 76.775275),
                LatLng(43.302654, 76.791748),
                LatLng(43.320444, 76.807947),
                LatLng(43.336963, 76.824599),
                LatLng(43.350650, 76.846544)
            ),
            //            2
            listOf(
                LatLng(43.22883350228765, 76.77025524768968),
                LatLng(43.23783821302262, 76.7828390120516),
                LatLng(43.24495124541126, 76.79139319960393),
                LatLng(43.249299635582666, 76.79372649731086),
                LatLng(43.26622797724026, 76.81643207249482),

                LatLng(43.284662676235506, 76.82713346221769),
                LatLng(43.29859022519495, 76.84405080433326),
                LatLng(43.380740581391784, 76.94034516205451)
            ),
            //            3
            listOf(
                LatLng(43.2206573630873, 76.78019504423747),
                LatLng(43.239950404294795, 76.81357662282376),
                LatLng(43.247283814186304, 76.8348126094468),
                LatLng(43.248963690136655, 76.8385147886768),

                LatLng(43.25776017834177, 76.84573707259993),
                LatLng(43.264168879642654, 76.86400520230752),
                LatLng(43.27040800579354, 76.88355967809285),
                LatLng(43.275048587293334, 76.90781037812273),
                LatLng(43.27764555657972, 76.9321904784612),

                LatLng(43.27901530184571, 76.94008036847003),
                LatLng(43.282422207949594, 76.95528983576392),
                LatLng(43.293235983399974, 76.99595234794197),
                LatLng(43.297209147451696, 77.01788029834692)
            ),
            //            4
            listOf(
                LatLng(43.248963690136655, 76.8385147886768),
                LatLng(43.24529150625776, 76.87281013613614),
                LatLng(43.24713045186991, 76.892948614132),
                LatLng(43.24766094534224, 76.90265924851582),
                LatLng(43.248946619133555, 76.91171982265787),

                LatLng(43.25120743399271, 76.92569810620645),
                LatLng(43.25825542030448, 76.96391282071411),
                LatLng(43.2624230396221, 76.97970285489379),
                LatLng(43.27495571419241, 77.01316930516086)
            ),
            //            5
            listOf(
                LatLng(43.199479727878554, 76.77921941401074),
                LatLng(43.21014643235847, 76.81504868757429),
                LatLng(43.218883017864634, 76.85158082685676),
                LatLng(43.22223399340525, 76.8791669619892),
                LatLng(43.226661631709284, 76.90044085246969),

                LatLng(43.23151330013376, 76.91982534511997),
                LatLng(43.23934154834526, 76.94701229682667),
                LatLng(43.2624230396221, 76.97970285489379)
            ),
            //            6
            listOf(
                LatLng(43.22223399340525, 76.8791669619892),
                LatLng(43.235072526730654, 76.84008706717535),
                LatLng(43.239950404294795, 76.81357662282376)
            ),
            //            7
            listOf(
                LatLng(43.23934154834526, 76.94701229682667),
                LatLng(43.23242728131147, 76.93772760493266),
                LatLng(43.22355252984899, 76.92865440870294),
                LatLng(43.21681738717785, 76.92449199485597),
                LatLng(43.20323718001454, 76.91394985980087),

                LatLng(43.196225676394235, 76.90451796157623),
                LatLng(43.18921388866149, 76.889424476442),
                LatLng(43.18235224053286, 76.86722016866614),
                LatLng(43.16199322835647, 76.81926471220173),
                LatLng(43.16274450580544, 76.78922397391325)
            ),
            //            8
            listOf(
                LatLng(43.16199322835647, 76.81926471220173),
                LatLng(43.17313414190275, 76.8509880944446),
                LatLng(43.18086523496139, 76.88942292814619),
                LatLng(43.18833005203855, 76.90800641526606),
                LatLng(43.19366671388313, 76.91241401281121),

                LatLng(43.209460207059465, 76.96343357251946),
                LatLng(43.21152893381685, 76.97372961983211),
                LatLng(43.26788048480976, 76.99928344971238)
            ),
            //            9
            listOf(
                LatLng(43.18833005203855, 76.90800641526606),
                LatLng(43.13259729692808, 76.86737057821736)
            ),
            //            10
            listOf(
                LatLng(43.209460207059465, 76.96343357251946),
                LatLng(43.24533583431253, 76.9781338084015),
                LatLng(43.26717585885717, 76.98854524081257)
            ),
//            //            11
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            12
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            13
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            14
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            15
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            16
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            17
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            18
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            19
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            20
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            21
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            ),
//            //            22
//            listOf(
//                LatLng(43.291683, 76.775275),
//                LatLng(43.302654, 76.791748),
//                LatLng(43.320444, 76.807947),
//                LatLng(43.336963, 76.824599),
//                LatLng(43.350650, 76.846544)
//            )
        )

        // Рисование линий на карте
        tectonicFaults.forEach { fault ->
            gMap.addPolyline(
                PolylineOptions()
                    .addAll(fault)
                    .width(50f)
                    .color(android.graphics.Color.YELLOW)
            )
        }

    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}
