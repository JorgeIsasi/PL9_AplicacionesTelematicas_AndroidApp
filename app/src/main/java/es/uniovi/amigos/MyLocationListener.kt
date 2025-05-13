package es.uniovi.amigos

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

// Se define un Listener para escuchar por cambios en la posición
class MyLocationListener(private val activity: MainActivity) : LocationListener {
    override fun onLocationChanged(location: Location) { // Se llama cuando hay una nueva posición para ese location provider
        // Se obtiene la latitud y longitud
        val longi: Double = location.getLongitude()
        val lati: Double = location.getLatitude()

        // Llama a la AsyncTask UpdatePositionTask pasando la URL adecuada
        val url = "${activity.LIST_URL}/${activity.mUserName}"
        UpdatePositionTask(activity, activity.mUserName, longi, lati).execute(url)
    }

    // El resto de métodos que debemos implementar los podemos dejar vacíos
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    // Se llama cuando se activa el provider
    override fun onProviderEnabled(provider: String) {}
    // Se llama cuando se desactiva el provider
    override fun onProviderDisabled(provider: String) {}
}