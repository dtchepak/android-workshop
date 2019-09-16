package net.davesquared.androidworkshop

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.couchbase.lite.*
import kotlinx.android.synthetic.main.activity_cbl.*
import java.net.URI
import java.util.*

class CblActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CblActivity"
    }

    private lateinit var database: Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cbl)

        CouchbaseLite.init(applicationContext)
        database = Database("sample", DatabaseConfiguration())

        btn_replicate.setOnClickListener {
            btn_replicate.isEnabled = false
            try {
                replicate()
            } catch (ex: Throwable) {
                txt_status.text = "Exception: $ex"
                btn_replicate.isEnabled = true
            }
        }
    }

    private fun replicate() {
        val uriText = edit_uri.text.toString()
        val username = edit_username.text.toString()
        val pwd = edit_password.text.toString()
        val uri = URI.create(uriText)

        val endpoint = URLEndpoint(uri)
        val config = ReplicatorConfiguration(database, endpoint)
        config.authenticator = BasicAuthenticator(username, pwd)
        val replicator = Replicator(config)

        var token: ListenerToken? = null
        token = replicator.addChangeListener { change ->
            val t = token
            if (change.status.activityLevel == AbstractReplicator.ActivityLevel.STOPPED) {
                Log.i(TAG, "Replication stopped")
                if (t != null) {
                    Log.i(TAG, "Removing listener")
                    replicator.removeChangeListener(t)
                }
                btn_replicate.isEnabled = true
            }
            txt_status.text = "${Date()}: ${change.status.activityLevel}\n${change.status.error}"
        }
        replicator.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
