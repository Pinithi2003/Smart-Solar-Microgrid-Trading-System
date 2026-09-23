package com.smartsolar.stations.stations.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.smartsolar.stations.R
import com.smartsolar.stations.stations.model.SolarStation
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.MarkerInfoWindow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Stations map preview with real Sri Lanka OSM tiles (no API key needed,
 * same source as the web Leaflet map). Full interactive maps belong to
 * Member 4. Offline devices fall back to the schematic canvas.
 */
class MapPreviewFragment : Fragment() {

    private var stations: List<SolarStation> = emptyList()
    private var highlightId: String? = null

    private var mapView: MapView? = null
    private var legacyView: View? = null

    fun setStations(list: List<SolarStation>, highlight: String? = null) {
        stations = list
        highlightId = highlight
        if (mapView != null) {
            refreshMarkers()
        } else {
            legacyView?.invalidate()
        }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View =
        i.inflate(R.layout.fragment_map_preview, c, false)

    override fun onViewCreated(v: View, s: Bundle?) {
        val host = v.findViewById<FrameLayout>(R.id.mockMapCanvas)
        if (isOnline()) {
            setupTiles(host)
        } else {
            legacyView = buildLegacyView()
            // Offline schematic: tap the highlighted pin area -> full details.
            legacyView?.setOnClickListener {
                val target = highlightId ?: stations.firstOrNull()?.stationId ?: return@setOnClickListener
                context?.startActivity(
                    android.content.Intent(
                        context,
                        com.smartsolar.stations.stations.ui.StationFullDetailActivity::class.java
                    ).putExtra("stationId", target)
                )
            }
            host.addView(
                legacyView, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        mapView?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        mapView?.onDetach()
        mapView = null
        legacyView = null
        super.onDestroyView()
    }

    private fun isOnline(): Boolean {
        val ctx = context ?: return false
        val mgr = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val net = mgr.activeNetwork ?: return false
        val caps = mgr.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun validStations(): List<SolarStation> =
        stations.filter { it.latitude != 0.0 || it.longitude != 0.0 }

    private fun colorFor(status: String): String = when (status) {
        "Active" -> "#22C55E"
        "Maintenance" -> "#E0A800"
        else -> "#DC3545"
    }

    // Ash lite bubble with dark text + tap bubble -> full details.
    private fun stationInfoWindow(map: MapView, stationId: String): MarkerInfoWindow =
        object : MarkerInfoWindow(R.layout.bubble_station, map) {
            override fun onOpen(item: Any?) {
                super.onOpen(item)
                val marker = item as? Marker ?: return
                mView.findViewById<TextView>(R.id.bubble_title)?.text = marker.title
                mView.findViewById<TextView>(R.id.bubble_description)?.text = marker.snippet
                mView.setOnClickListener {
                    marker.closeInfoWindow()
                    try {
                        mView.context.startActivity(
                            Intent(mView.context, StationFullDetailActivity::class.java)
                                .putExtra("stationId", stationId)
                        )
                    } catch (_: Exception) {
                        // Preview only: never crash over the map bubble.
                    }
                }
            }
        }

    // ---- Real tiles (online) ----

    private fun setupTiles(host: FrameLayout) {
        val ctx = requireContext()
        Configuration.getInstance().load(
            ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        // OSM tile usage policy requires identifying the app.
        Configuration.getInstance().userAgentValue = ctx.packageName
        val map = MapView(ctx)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        map.controller.setZoom(7.0)
        map.controller.setCenter(GeoPoint(7.0, 80.7))
        host.addView(
            map, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )
        mapView = map
        refreshMarkers()
    }

    private fun refreshMarkers() {
        val map = mapView ?: return
        map.overlays.removeAll { it is Marker }
        val pts = mutableListOf<GeoPoint>()
        var highlighted: Marker? = null
        validStations().forEach { st ->
            val m = Marker(map)
            m.position = GeoPoint(st.latitude, st.longitude)
            m.title = st.stationName
            m.snippet = "${st.stationId} • ${st.status} • ${st.availableCapacity} kWh available"
            // Ash lite bubble (clearly readable) instead of the default white one.
            m.infoWindow = stationInfoWindow(map, st.stationId)
            // Tap marker bubble again (or marker twice) -> full station details.
            // First tap shows the info window, second tap opens the details page.
            m.setOnMarkerClickListener { marker, _ ->
                if (marker.isInfoWindowShown) {
                    context?.startActivity(
                        android.content.Intent(
                            context,
                            com.smartsolar.stations.stations.ui.StationFullDetailActivity::class.java
                        ).putExtra("stationId", st.stationId)
                    )
                    true
                } else {
                    false // let osmdroid show the info window
                }
            }
            val isHi = st.stationId == highlightId
            m.icon = dotDrawable(colorFor(st.status), isHi)
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            map.overlays.add(m)
            pts += m.position
            if (isHi) highlighted = m
        }
        map.invalidate()
        val d = resources.displayMetrics.density
        map.post {
            try {
                when {
                    pts.size == 1 -> {
                        map.controller.setZoom(11.0)
                        map.controller.setCenter(pts[0])
                    }
                    pts.size > 1 -> {
                        map.zoomToBoundingBox(
                            BoundingBox.fromGeoPoints(pts), false, (48 * d).toInt()
                        )
                    }
                    else -> {
                        map.controller.setZoom(7.0)
                        map.controller.setCenter(GeoPoint(7.0, 80.7))
                    }
                }
                highlighted?.showInfoWindow()
            } catch (_: Exception) {
                // Preview only: never crash the host screen over the map.
            }
        }
    }

    // Web-style pin: colored dot, white ring, soft shadow.
    private fun dotDrawable(colorHex: String, big: Boolean): BitmapDrawable {
        val d = resources.displayMetrics.density
        val r = (if (big) 12f else 10f) * d
        val ring = 3f * d
        val shadow = 4f * d
        val size = ((r + ring + shadow) * 2).toInt()
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val cx = size / 2f
        val cy = size / 2f
        val shade = Paint().apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        c.drawCircle(cx, cy + 2f * d, r + ring, shade)
        val dot = Paint().apply {
            color = Color.parseColor(colorHex)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        c.drawCircle(cx, cy, r, dot)
        val edge = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = ring
            isAntiAlias = true
        }
        c.drawCircle(cx, cy, r + ring / 2, edge)
        return BitmapDrawable(resources, bmp)
    }

    // ---- Schematic canvas (offline fallback) ----

    private fun buildLegacyView(): View {
        return object : View(requireContext()) {
            init {
                setLayerType(LAYER_TYPE_SOFTWARE, null)
            }

            private fun project(): List<Triple<SolarStation, Float, Float>> {
                val w = width.toFloat()
                val h = height.toFloat()
                if (w == 0f || h == 0f) return emptyList()
                val d = resources.displayMetrics.density
                val pad = 64f * d
                val pts = validStations()
                val lats = pts.map { it.latitude }.ifEmpty { listOf(5.9, 9.1) }
                val lngs = pts.map { it.longitude }.ifEmpty { listOf(79.6, 81.9) }
                var minLat = (lats.minOrNull() ?: 5.9) - 0.15
                var maxLat = (lats.maxOrNull() ?: 9.1) + 0.15
                var minLng = (lngs.minOrNull() ?: 79.6) - 0.15
                var maxLng = (lngs.maxOrNull() ?: 81.9) + 0.15
                if (maxLat - minLat < 0.4) {
                    val mid = (maxLat + minLat) / 2
                    minLat = mid - 0.2
                    maxLat = mid + 0.2
                }
                if (maxLng - minLng < 0.4) {
                    val mid = (maxLng + minLng) / 2
                    minLng = mid - 0.2
                    maxLng = mid + 0.2
                }
                val placed = mutableListOf<Pair<Float, Float>>()
                val minDist = 52f * d
                return pts.map { st ->
                    var px = (pad + (st.longitude - minLng) / (maxLng - minLng) * (w - 2 * pad)).toFloat()
                    var py = (pad + (1 - (st.latitude - minLat) / (maxLat - minLat)) * (h - 2 * pad)).toFloat()
                    var tries = 0
                    while (placed.any { sqrt((it.first - px) * (it.first - px) + (it.second - py) * (it.second - py)) < minDist } && tries < 8) {
                        val ang = 2 * Math.PI * tries / 8
                        val baseX = (pad + (st.longitude - minLng) / (maxLng - minLng) * (w - 2 * pad)).toFloat()
                        val baseY = (pad + (1 - (st.latitude - minLat) / (maxLat - minLat)) * (h - 2 * pad)).toFloat()
                        px = (baseX + minDist * 0.6 * cos(ang)).toFloat()
                        py = (baseY + minDist * 0.6 * sin(ang)).toFloat()
                        tries++
                    }
                    placed += px to py
                    Triple(st, px, py)
                }
            }

            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val d = resources.displayMetrics.density
                val grid = Paint().apply {
                    color = Color.parseColor("#CBE0D2")
                    strokeWidth = 1f * d
                }
                val step = 28f * d
                var x = 0f
                while (x < width) {
                    canvas.drawLine(x, 0f, x, height.toFloat(), grid)
                    x += step
                }
                var y = 0f
                while (y < height) {
                    canvas.drawLine(0f, y, width.toFloat(), y, grid)
                    y += step
                }
                val frame = Paint().apply {
                    color = Color.parseColor("#A9C7B2")
                    style = Paint.Style.STROKE
                    strokeWidth = 2f * d
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), frame)
                val dots = project()
                dots.forEach { (st, px, py) ->
                    val dot = Paint().apply {
                        color = Color.parseColor(colorFor(st.status))
                        style = Paint.Style.FILL
                        setShadowLayer(6f * d, 0f, 3f * d, Color.argb(90, 0, 0, 0))
                    }
                    val ring = Paint().apply {
                        color = Color.WHITE
                        style = Paint.Style.STROKE
                        strokeWidth = 3f * d
                    }
                    val r = (if (st.stationId == highlightId) 11f else 9f) * d
                    canvas.drawCircle(px, py, r, dot)
                    canvas.drawCircle(px, py, r, ring)
                    if (st.stationId == highlightId) {
                        canvas.drawCircle(px, py, r + 5f * d, ring)
                    }
                }
                val ink = Paint().apply {
                    color = Color.parseColor("#0E3B22")
                    textSize = 12f * d
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                val inkBold = Paint().apply {
                    color = Color.parseColor("#0E3B22")
                    textSize = 12f * d
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val pillFill = Paint().apply {
                    color = Color.argb(235, 255, 255, 255)
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val pillEdge = Paint().apply {
                    color = Color.parseColor("#A9C7B2")
                    style = Paint.Style.STROKE
                    strokeWidth = 1f * d
                    isAntiAlias = true
                }
                val link = Paint().apply {
                    color = Color.parseColor("#6B7B72")
                    strokeWidth = 1.5f * d
                    isAntiAlias = true
                }
                val fm = ink.fontMetrics
                val pillH = (fm.descent - fm.ascent) + 8f * d
                val corner = 8f * d
                val gap = 6f * d
                val placed = mutableListOf<RectF>()
                dots.sortedBy { it.third }.forEach { (st, px, py) ->
                    val isHi = st.stationId == highlightId
                    val paint = if (isHi) inkBold else ink
                    val name = st.stationName
                        .replace(" Solar Station", "", ignoreCase = true)
                        .trim().take(14).ifBlank { st.stationId }
                    val r = (if (isHi) 11f else 9f) * d
                    val pillW = paint.measureText(name) + 16f * d
                    var top = py - r - gap - pillH
                    var below = false
                    var rect = pillRect(px, top, pillW, pillH, d)
                    if (placed.any { RectF.intersects(rect, it) }) {
                        top = py + r + gap
                        rect = pillRect(px, top, pillW, pillH, d)
                        below = true
                    }
                    var nudges = 0
                    while (placed.any { RectF.intersects(rect, it) } && nudges < 6) {
                        rect.offset(0f, if (below) pillH + 4f * d else -(pillH + 4f * d))
                        nudges++
                    }
                    placed += rect
                    val edgeY = if (below) rect.top else rect.bottom
                    canvas.drawLine(px, py + (if (below) r else -r), px, edgeY, link)
                    canvas.drawRoundRect(rect, corner, corner, pillFill)
                    val edge = Paint(pillEdge).apply {
                        if (isHi) {
                            color = Color.parseColor("#22C55E")
                            strokeWidth = 2f * d
                        }
                    }
                    canvas.drawRoundRect(rect, corner, corner, edge)
                    val baseline = rect.centerY() - (paint.fontMetrics.ascent + paint.fontMetrics.descent) / 2
                    canvas.drawText(name, rect.centerX(), baseline, paint)
                }
            }

            private fun pillRect(px: Float, top: Float, pillW: Float, pillH: Float, d: Float): RectF {
                var left = px - pillW / 2
                if (left < 4f * d) left = 4f * d
                if (left + pillW > width - 4f * d) left = width - 4f * d - pillW
                return RectF(left, top, left + pillW, top + pillH)
            }
        }
    }

    companion object {
        fun args(stationId: String): Bundle = Bundle().apply { putString("highlight", stationId) }
    }
}
