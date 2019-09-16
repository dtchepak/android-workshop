# AndroidWorkshop

Project for hacking up quick examples to run on Android.

## CBL Example

Attempt to demonstrate <https://github.com/couchbase/couchbase-lite-android-ce/issues/25>. 

### Steps

* Start app and begin profiling.
* Tap "CBL Example" button.
* Navigate back without starting replication.
* In profiler, force GC then do a heap dump. Observe that heap does not contain any CBL `Database` or `Replicator` objects.
* Tap "CBL Example" button.
* Enter syncgateway details and start replication (it does not matter if you don't put in valid replication details, just attempting to hit the end point is sufficient).
* Do the replication a total of `x` times.
* Navigate back to main activity. All CBL objects should now be eligible for GC.
* In profiler, force GC then do a heap dump. Observe that heap contains `x` number of `Replicator` instances, and also a `Database` instance that is kept alive by the `Replicator` instance.

See <https://github.com/couchbase/couchbase-lite-android-ce/issues/25> for more information.

