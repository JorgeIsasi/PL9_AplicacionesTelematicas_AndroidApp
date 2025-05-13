package es.uniovi.amigos

import ShowAmigosTask
import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Timer
import java.util.TimerTask


class MainActivity : AppCompatActivity() {
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private var map: MapView? = null // Este atributo guarda una referencia al objeto MapView a través del cual podremos manipular el mapa que se muestre

    val LIST_URL = "http://192.168.56.1:5094/api/amigo" // URL del servicio REST que proporciona la lista de amigos
    private val UPDATE_PERIOD = 10000L // 10 segundos

    var mUserName: String = null.toString() // Nombre del usuario

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Leer la configuración de la aplicación
        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        // Crear el mapa desde el layout y asignarle la fuente de la que descargará las imágenes del mapa
        setContentView(R.layout.activity_main)
        map = findViewById<View>(R.id.map) as MapView
        map!!.setTileSource(TileSourceFactory.MAPNIK)

        // Solicitar al usuario los permisos "peligrosos". El usuario debe autorizarlos
        // Cuando los autorice, Android llamará a la función onRequestPermissionsResult
        // que implementamos más adelante
        requestPermissionsIfNecessary(
            arrayOf( // WRITE_EXTERNAL_STORAGE este permiso es necesario para guardar las imagenes del mapa
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION
            )
        )

        centerMapOnEurope() // centrar el mapa en Europa
        //addMarker(40.416775, -3.703790, "Madrid") // marcador de prueba

        // Actualizar la posicion de los amigos periodicamente
        val timer: Timer = Timer()
        val updateAmigos: TimerTask = UpdateAmigosPosition(this@MainActivity)
        timer.scheduleAtFixedRate(updateAmigos, 0, UPDATE_PERIOD)

        // Pregunta nombre al usuario. Usaremos Juan (usuario ya creado en la base de datos que puede actualizar su ubicación)
        askUserName()

        // Actualiza la posicion del usuario actual (cada vez que el telefono se mueva mas de 10 metros)
        SetupLocation()
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
        // Esta función será invocada cuando el usuario conceda los permisos
        // De momento hay que dejarla como está
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionsToRequest = ArrayList<String>()
        for (i in grantResults.indices) {
            permissionsToRequest.add(permissions[i])
        }
        if (permissionsToRequest.size > 0) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray<String>(),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
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


    // Esta funcion es llamada periodicamente para actualizar la posicion de los amigos en onCreate
    class UpdateAmigosPosition(private val activity: MainActivity) : TimerTask() {
        override fun run() {
            ShowAmigosTask(activity).execute(activity.LIST_URL)
        }
    }

    // Esta funcion es llamada finalmente por la tarea asincrona (onPostExecute) para añadir chinchetas al mapa
    fun updateMap(friendsList: List<Amigo>) {
        map!!.getOverlays().clear(); // borrar chinchetas previas

        for (amigo in friendsList) { // iterar sobre la lista de amigos y pintar chinchetas
            addMarker(amigo.lati, amigo.longi, amigo.name)
        }

        map!!.getController().scrollBy(0,0) //forzar repintado del mapa
    }


    // Esta funcion es llamada para que el usuario introduzca su nombre
    fun askUserName() {
        val alert: AlertDialog.Builder = AlertDialog.Builder(this)

        alert.setTitle("Settings")
        alert.setMessage("User name:")

        // Crear un EditText para obtener el nombre
        val input: EditText = EditText(this)
        alert.setView(input)

        alert.setPositiveButton("Ok", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, whichButton: Int) {
                mUserName = input.getText().toString()
            }
        })

        alert.setNegativeButton("Cancel", object : DialogInterface.OnClickListener {
            override fun onClick(dialog: DialogInterface?, whichButton: Int) {
                // Canceled.
            }
        })

        alert.show()
    }


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