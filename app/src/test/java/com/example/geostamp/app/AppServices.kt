// geostamp-iteration: 7
package com.example.geostamp.app

import android.content.Context
import com.example.geostamp.data.camera.CaptureCoordinator
import com.example.geostamp.data.location.AddressResolver
import com.example.geostamp.data.location.LocationProvider
import com.example.geostamp.data.maps.HttpRedirectSource
import com.example.geostamp.data.maps.MapTileProvider
import com.example.geostamp.data.media.BitmapLoader
import com.example.geostamp.data.media.ExifReader
import com.example.geostamp.data.media.FolderExporter
import com.example.geostamp.data.media.ImportCopier
import com.example.geostamp.data.media.MediaStoreSaver
import com.example.geostamp.data.media.MetadataService
import com.example.geostamp.data.media.PhotoSaver
import com.example.geostamp.data.media.ThumbnailCache
import com.example.geostamp.data.store.PhotoLibrary
import com.example.geostamp.data.store.PhotoStore
import com.example.geostamp.data.store.PlaceStore
import com.example.geostamp.data.store.StampPrefs
import com.example.geostamp.data.store.WorkingCopyCleaner
import com.example.geostamp.domain.maps.MapLinkResolver
import java.io.File

/** One place that creates the shared helpers (no dependency-injection library needed). */
class AppServices private constructor(context: Context) {
    val originalsDir = File(context.filesDir, "originals")
    val photoStore = PhotoStore(File(context.filesDir, "photos.json"))
    val stampPrefs = StampPrefs(context)
    val locationProvider = LocationProvider(context)
    val addressResolver = AddressResolver(context)
    val bitmapLoader = BitmapLoader(context)
    val mediaSaver = MediaStoreSaver(context)
    val thumbnails = ThumbnailCache(bitmapLoader, originalsDir)
    val mapTiles = MapTileProvider(context)
    val photoSaver = PhotoSaver(context, bitmapLoader, mediaSaver, photoStore, thumbnails, mapTiles)
    val captureCoordinator = CaptureCoordinator(context, locationProvider, originalsDir)
    val library = PhotoLibrary(photoStore, mediaSaver, originalsDir)
    val importCopier = ImportCopier(context, originalsDir)
    val folderExporter = FolderExporter(context)
    val exifReader = ExifReader(context)
    val metadata = MetadataService(context, exifReader, mediaSaver, photoStore)
    val workingCopies = WorkingCopyCleaner(photoStore, originalsDir)
    val placeStore = PlaceStore(File(context.filesDir, "places.json"))
    val mapLinks = MapLinkResolver(HttpRedirectSource())

    companion object {
        @Volatile
        private var instance: AppServices? = null

        fun get(context: Context): AppServices =
            instance ?: synchronized(this) {
                instance ?: AppServices(context.applicationContext).also { instance = it }
            }
    }
}
