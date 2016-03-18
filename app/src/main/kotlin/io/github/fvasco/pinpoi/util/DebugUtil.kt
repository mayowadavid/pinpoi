package io.github.fvasco.pinpoi.util

import android.content.Context
import android.util.Log
import io.github.fvasco.pinpoi.BuildConfig
import io.github.fvasco.pinpoi.dao.PlacemarkCollectionDao
import io.github.fvasco.pinpoi.dao.PlacemarkDao
import io.github.fvasco.pinpoi.model.Placemark
import io.github.fvasco.pinpoi.model.PlacemarkCollection

/**
 * Debug utilities

 * @author Francesco Vasco
 */
fun setUpDebugDatabase(context: Context) {
    if (!BuildConfig.DEBUG) throw AssertionError()

    val placemarkCollectionDao = PlacemarkCollectionDao(context)
    placemarkCollectionDao.open()
    val placemarkDao = PlacemarkDao(context)
    placemarkDao.open()
    try {
        val placemarkCollectionDatabase = placemarkCollectionDao.database!!
        val placemarkDatabase = placemarkDao.database!!
        placemarkCollectionDatabase.beginTransaction()
        placemarkDatabase.beginTransaction()
        try {
            // clear all database
            for (placemarkCollection in placemarkCollectionDao.findAllPlacemarkCollection()) {
                placemarkDao.deleteByCollectionId(placemarkCollection.id)
                placemarkCollectionDao.delete(placemarkCollection)
            }

            // recreate test database
            val placemarkCollection = PlacemarkCollection()
            for (pci in 15 downTo 0) {
                placemarkCollection.id = 0
                placemarkCollection.name = "Placemark Collection '" + pci + '\''
                placemarkCollection.category = if (pci == 0) "" else "Category " + pci % 7
                placemarkCollection.description = placemarkCollection.name + " long long long description"
                placemarkCollection.source = "http://www.example.org/poi-collection-$pci.csv"
                placemarkCollection.poiCount = pci
                placemarkCollection.lastUpdate = pci * 10000000L
                placemarkCollectionDao.insert(placemarkCollection)
                Log.i("setUpDebugDatabase", "inserted " + placemarkCollection)

                val placemark = Placemark()
                for (lat in -60..59) {
                    var lon = -90
                    while (lon < 90) {
                        placemark.id = 0
                        placemark.name = "Placemark $lat,$lon / $pci"
                        placemark.description = if ((lat + lon) % 10 == 0)
                            ""
                        else if (pci == 0)
                            placemark.name + "<u>beautiful</u> description"
                        else
                            placemark.name + " description"
                        placemark.coordinates = Coordinates((lat + Math.sin((lat + pci).toDouble())).toFloat(), (lon + Math.sin((lon - pci).toDouble())).toFloat())
                        placemark.collectionId = placemarkCollection.id
                        placemarkDao.insert(placemark)

                        if ((lat + lon + pci) % 9 == 0) {
                            val placemarkAnnotation = placemarkDao.loadPlacemarkAnnotation(placemark)
                            placemarkAnnotation.isFlagged = (lat + lon + pci) % 3 == 0
                            placemarkAnnotation.note = "Placemark annotation for " + placemark.name
                            placemarkDao.update(placemarkAnnotation)
                        }
                        lon += 2
                    }
                }
            }

            placemarkDatabase.setTransactionSuccessful()
            placemarkCollectionDatabase.setTransactionSuccessful()
        } finally {
            placemarkDatabase.endTransaction()
            placemarkCollectionDatabase.endTransaction()
        }
    } finally {
        placemarkDao.close()
        placemarkCollectionDao.close()
    }
}
