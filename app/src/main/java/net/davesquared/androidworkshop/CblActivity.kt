package net.davesquared.androidworkshop

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.couchbase.lite.*
import com.couchbase.lite.Function
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_cbl.*
import java.net.URI
import java.util.*

class CblActivity : AppCompatActivity() {

    companion object {
        const val TAG = "CblActivity"
    }

    private lateinit var database: Database

    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cbl)

        CouchbaseLite.init(applicationContext)
        database = Database("sample", DatabaseConfiguration())

        txt_status.text = "Database contains ${database.count} docs"

        btn_replicate.setOnClickListener {
            btn_replicate.isEnabled = false
            try {
                replicate()
            } catch (ex: Throwable) {
                txt_status.text = "Replication Exception: $ex"
                btn_replicate.isEnabled = true
            }
        }
        runAsyncOnClick(btn_query, "Query",
            runQuery()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { summary -> txt_status.text = "Query returned:\n$summary" }
        )
        runAsyncOnClick(btn_insert, "Insert",
            runInsert()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { docCount ->
                    txt_status.text = "Insert completed. Db now has $docCount docs (${Date()})"
                }
        )
        runAsyncOnClick(btn_clear, "Clear",
            runClear()
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess {
                    txt_status.text = "Cleared DB"
                }
        )
    }

    private fun <T> runAsyncOnClick(btn: View, commandName: String, async: Single<T>) {
        btn.setOnClickListener {
            btn.isEnabled = false
            txt_status.text = "Starting $commandName... (${Date()})"
            disposables.add(
                async
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally {
                        btn.isEnabled = true
                    }
                    .doOnError { t ->
                        txt_status.text = "$commandName exception: $t"
                        Log.e(TAG, "$commandName failed", t)
                    }
                    .ignoreElement()
                    .onErrorComplete()
                    .subscribe()
            )
        }
    }

    private fun runClear() = Single.fromCallable {
        database.delete()
        database = Database("sample", DatabaseConfiguration())
    }

    private fun randomPokemon() =
        listOf("Pikachu", "Squirtle", "Eevee", "Bulbasaur", "Arcanine", "Charizard", null).random()

    private fun runInsert() = Single.fromCallable {
        database.inBatch {
            (0..14000).map { randomPokemon() }.forEach { p ->
                val doc = MutableDocument()
                doc.setString("name", "Doc-$p")
                if (p != null) {
                    doc.setValue(
                        "data",
                        mapOf("type" to p, "version" to "1.2", "class" to "pokemon")
                    )
                }
                database.save(doc)
            }
        }
        database.count
    }

    private fun runQuery() = Single.fromCallable {
        val result = QueryBuilder
            .select(
                SelectResult.property("data"),
                SelectResult.expression(Function.count(Expression.string("*")))
            )
            .from(
                DataSource.database(database)
            )
            .groupBy(Expression.property("data"))
            .execute()

        result.allResults()
            .sortedBy { it.getInt(1) }
            .joinToString("\n") { r ->
                "${r.getDictionary("data")?.getString("type")} ${r.getInt(
                    1
                )}"
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

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }
}
