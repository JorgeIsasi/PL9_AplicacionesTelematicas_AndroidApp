package es.uniovi.amigos

import android.os.AsyncTask
import android.widget.Toast
import org.json.JSONException
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class UpdatePositionTask(private val activity: MainActivity, private val name: String, private val longi: Double, private val lati: Double) : AsyncTask<String, Void, String?>() {

    private var serverResponseCode: Int = -1
    private var errorMessage: String? = null

    override fun doInBackground(vararg urls: String): String? {
        try {
            return sendPutRequest(urls[0], name, longi, lati)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(result: String?) {
        if (serverResponseCode in 200..299) {
            Toast.makeText(activity, "Location updated succesfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                activity,
                "Error updating location: $errorMessage",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Throws(IOException::class, JSONException::class)
    protected fun sendPutRequest(URL: String?, name: String, longi: Double, lati: Double): String? {
        val url = URL(URL)
        val httpCon = url.openConnection() as HttpURLConnection
        httpCon.readTimeout = 10000
        httpCon.connectTimeout = 15000
        httpCon.doOutput = true
        httpCon.doInput = true
        httpCon.setRequestProperty("Content-Type", "application/JSON")
        httpCon.requestMethod = "PUT"

        val out = OutputStreamWriter(httpCon.outputStream)
        val jObjSent = "{\"name\":\"$name\",\"longi\":\"$longi\",\"lati\":\"$lati\"}"
        out.write(jObjSent)
        out.close()

        serverResponseCode = httpCon.responseCode

        return if (serverResponseCode in 200..299) {
            httpCon.inputStream.bufferedReader().use { it.readText() }
        } else {
            errorMessage = httpCon.errorStream?.bufferedReader()?.use { it.readText() }
            null
        }
    }
}