package es.uniovi.amigos

import ShowAmigosTask
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker


class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private var map: MapView? = null // Este atributo guarda una referencia al objeto MapView a través del cual podremos manipular el mapa que se muestre

    // URLs para el servicio REST
    //val LIST_URL = "http://10.0.2.2:5094/api/amigo" // URL para emulador
    val LIST_URL = "https://1ff9-37-35-182-169.ngrok-free.app/api/amigo" // URL proporcionada por ngrok para implementar HTTPS

    // Nombre del usuario
    var mUserName: String? = null

    // Botón y EditText para establecer el nombre de usuario
    private lateinit var btnsetusername: Button
    private lateinit var usernameInput: EditText


    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leer la configuración de la aplicación
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        // Solicitar al usuario los permisos "peligrosos". El usuario debe autorizarlos
        // Cuando los autorice, Android llamará a la función onRequestPermissionsResult
        // que implementamos más adelante
        requestPermissionsIfNecessary(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
            )
        )

        // Crear el mapa desde el layout y asignarle la fuente de la que descargará las imágenes del mapa
        setContentView(R.layout.activity_main)
        map = findViewById<View>(R.id.map) as MapView
        map!!.setTileSource(TileSourceFactory.MAPNIK)

        // Centrar el mapa en Europa
        centerMapOnEurope()
        //addMarker(40.416775, -3.703790, "Madrid") // marcador de prueba

        // Obtener la posicion de los amigos.
        ShowAmigosTask(this).execute(this.LIST_URL)

        // Definimos boton y cuadro de texto para establecer el nombre de usuario
        btnsetusername = findViewById(R.id.btn_set_username)
        usernameInput = findViewById(R.id.username_input)

        // Asignamos la acción al botón
        btnsetusername.setOnClickListener {
            // Mostrar el cuadro de texto
            usernameInput.visibility = View.VISIBLE
            usernameInput.requestFocus()

            // Mostrar el teclado
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(usernameInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)

            // Cambiamos el texto del boton: ahora indica que sirve para guardar el nombre
            btnsetusername.text = getString(R.string.save_name)

            // Cambiamos el comportamiento del boton
            btnsetusername.setOnClickListener {
                // Guardar el nombre si es válido
                val name = usernameInput.text.toString()

                if (name.isNotBlank()) {
                    mUserName = name
                    Toast.makeText(this, "Name saved: $name", Toast.LENGTH_SHORT).show()
                    usernameInput.visibility = View.GONE
                    // Si ya tenemos permisos, iniciar SetupLocation
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        SetupLocation()
                    }

                } else {
                    Toast.makeText(this, "Introduce a valid name", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    public override fun onResume() {
        super.onResume()
        map!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        map!!.onPause()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissiongs granted", Toast.LENGTH_SHORT).show()
            // Aquí puedes iniciar SetupLocation si el nombre ya está definido
            if (mUserName != null) {
                SetupLocation()
            }
        } else {
            Toast.makeText(this, "Permissiongs are needed to obtain location", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestPermissionsIfNecessary(permissions: Array<String>) {
        // Itera por la lista de permisos y los va solicitando de uno en uno
        // a menos que estén ya concedidos (de ejecuciones previas)
        val permissionsToRequest = ArrayList<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is not granted
                permissionsToRequest.add(permission)
            }
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray<String>(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }


    private fun centerMapOnEurope() {
        // Esta función mueve el centro del mapa a Paris y ajusta el zoom
        // para que se vea Europa
        val mapController = map!!.controller
        mapController.setZoom(5.5)
        val startPoint = GeoPoint(48.8583, 2.2944)
        mapController.setCenter(startPoint)
    }

    private fun addMarker(latitud: Double, longitud: Double, name: String) {
        val coords = GeoPoint(latitud, longitud)
        val startMarker = Marker(map)
        startMarker.position = coords
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        startMarker.title = name
        map!!.overlays.add(startMarker)
    }

    // Esta funcion es llamada finalmente por la tarea asincrona (onPostExecute) para añadir chinchetas al mapa
    fun updateMap(friendsList: List<Amigo>) {
        map!!.getOverlays().clear(); // borrar chinchetas previas

        for (amigo in friendsList) { // iterar sobre la lista de amigos y pintar chinchetas
            addMarker(amigo.lati, amigo.longi, amigo.name)
        }

        map!!.getController().scrollBy(0,0) //forzar repintado del mapa
    }


    // Esta funcion se utiliza para actualizar la localizacion del usuario
    fun SetupLocation() {
        if ((ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
                    != PackageManager.PERMISSION_GRANTED)
            && (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
                    != PackageManager.PERMISSION_GRANTED)
        ) {
            // Verificar por si acaso si tenemos el permiso, y si no
            // no hacemos nada
            return
        }

        // Se debe adquirir una referencia al Location Manager del sistema
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // Se obtiene el mejor provider de posición
        val criteria: Criteria = Criteria()
        val provider: String = locationManager.getBestProvider(criteria, false).toString()

        // Se crea un listener de la clase que se va a definir luego
        val locationListener: MyLocationListener = MyLocationListener(this)

        // Se registra el listener con el Location Manager para recibir actualizaciones
        // En este caso pedimos que nos notifique la nueva localización si el teléfono se ha movido más de 10 metros
        locationManager.requestLocationUpdates(provider, 0L, 10f, locationListener)

        // Comprobar si se puede obtener la posición ahora mismo
        val location: Location? = locationManager.getLastKnownLocation(provider)
        if (location != null) {
            // La posición actual es location
        } else {
            // Actualmente no se puede obtener la posición
        }
    }
}