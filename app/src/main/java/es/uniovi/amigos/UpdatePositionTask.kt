package es.uniovi.amigos

import android.os.AsyncTask
import org.json.JSONException
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL


class UpdatePositionTask(private val activity: MainActivity, private val name: String, private val longi: Double, private val lati: Double) : AsyncTask<String, Void, String?>() {
    override fun doInBackground(vararg urls: String): String? {
        try {
            return sendPutRequest(urls[0], name, longi, lati)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun onPostExecute(result: String?) {
        //no hacemos nada con la respuesta
    }

    @Throws(IOException::class, JSONException::class)
    protected fun sendPutRequest(URL: String?, name: String, lati: Double, longi: Double): String {
        val url = URL(URL)
        val httpCon = url.openConnection() as HttpURLConnection
        httpCon.readTimeout = 10000
        httpCon.connectTimeout = 15000

        httpCon.doOutput = true
        httpCon.doInput = true
        httpCon.setRequestProperty("Content-Type", "application/JSON")

        httpCon.requestMethod = "PUT"
        val out = OutputStreamWriter(
            httpCon.outputStream
        )

        val jObjSent = "{\"name\":\"$name\",\"lati\":\"$lati\",\"longi\":\"$longi\"}"
        out.write(jObjSent)
        out.close()

        // Leer respuesta (si la hay)
        return httpCon.inputStream.bufferedReader().use { it.readText() }
    }
}

/*

 */