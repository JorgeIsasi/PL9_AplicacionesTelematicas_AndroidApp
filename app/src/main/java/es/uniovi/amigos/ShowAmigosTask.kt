import android.os.AsyncTask
import android.util.Log
import android.widget.Toast
import es.uniovi.amigos.Amigo
import es.uniovi.amigos.MainActivity
import org.json.JSONArray
import org.json.JSONException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ShowAmigosTask(private val activity: MainActivity) : AsyncTask<String, Void, String?>() {
    override fun doInBackground(vararg urls: String): String? {
        try {
            return getFriendLocation(urls[0])
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    fun getFriendLocation(urlString: String): String {
        return readStream(openUrl(urlString))
    }

    override fun onPostExecute(result: String?) {
        Log.d("ShowAmigosTask", "Resultado recibido: $result")
        if (result != null){
            var friendsList: List<Amigo> = parseDataFromNetwork(result)
            activity.updateMap(friendsList)
        }
        else{
            Toast.makeText(activity, "An error occured trying to get friends info", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    fun openUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.readTimeout = 10000
        conn.connectTimeout = 15000
        conn.requestMethod = "GET"
        conn.doInput = true

        // Starts the query
        conn.connect()

        val responseCode = conn.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("HTTP error code: $responseCode")
        }

        return conn.inputStream
    }

    @Throws(IOException::class)
    fun readStream(urlStream: InputStream?): String {
        val r = BufferedReader(InputStreamReader(urlStream))
        val total = StringBuilder()
        var line: String?
        while ((r.readLine().also { line = it }) != null) {
            total.append(line)
        }
        return total.toString()
    }

    @Throws(IOException::class, JSONException::class)
    private fun parseDataFromNetwork(data: String): List<Amigo> {
        val amigosList: MutableList<Amigo> = ArrayList<Amigo>()

        val amigos = JSONArray(data)

        for (i in 0..<amigos.length()) {
            val amigoObject = amigos.getJSONObject(i)

            val name = amigoObject.getString("name")
            val longi = amigoObject.getString("longi")
            val lati = amigoObject.getString("lati")

            val longiNumber: Double
            val latiNumber: Double
            try {
                longiNumber = longi.toDouble()
                latiNumber = lati.toDouble()
            } catch (nfe: NumberFormatException) {
                continue
            }

            amigosList.add(Amigo(name, longiNumber, latiNumber))
        }
        return amigosList
    }
}