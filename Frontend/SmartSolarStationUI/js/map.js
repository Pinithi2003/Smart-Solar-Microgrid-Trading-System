/* Read-only station map preview (Leaflet). Showcase only — full map features
   belong to Member 4. Never writes data; markers reflect /api/solarstations. */
(function () {
  const COLORS = { Active: "#198754", Inactive: "#6c757d", Maintenance: "#e0a800" };
  let map = null;
  let layer = null;
  let inited = false;

  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
  }

  function showFallback(message) {
    const fb = document.getElementById("mapFallback");
    if (fb) {
      fb.textContent = message;
      fb.classList.remove("d-none");
    }
  }

  function dotIcon(color) {
    return window.L.divIcon({
      className: "",
      html: `<span style="display:block;width:18px;height:18px;border-radius:50%;`
        + `background:${color};border:3px solid #fff;box-shadow:0 1px 5px rgba(0,0,0,.45);"></span>`,
      iconSize: [18, 18],
      iconAnchor: [9, 9],
      popupAnchor: [0, -10]
    });
  }

  function ensure() {
    if (inited) return true;
    if (typeof window.L === "undefined") {
      showFallback("Map library (Leaflet CDN) could not load — check connection. Station data above is unaffected.");
      return false;
    }
    try {
      map = window.L.map("stationMap", { scrollWheelZoom: false }).setView([7.0, 80.7], 7);
      window.L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 18,
        attribution: "&copy; OpenStreetMap contributors"
      }).addTo(map);
      layer = window.L.layerGroup().addTo(map);
      inited = true;
      return true;
    } catch {
      showFallback("Map failed to start. Station data above is unaffected.");
      return false;
    }
  }

  function refresh(stations) {
    if (!ensure() || !Array.isArray(stations)) return;
    layer.clearLayers();
    const pts = stations.filter((s) => Number.isFinite(Number(s.latitude)) && Number.isFinite(Number(s.longitude)));
    pts.forEach((s) => {
      const color = COLORS[s.status] || "#333333";
      window.L.marker([Number(s.latitude), Number(s.longitude)], { icon: dotIcon(color) })
        .bindPopup(
          `<strong>${esc(s.stationName)}</strong><br>`
          + `<span>${esc(s.stationId)} • ${esc(s.location)}</span><br>`
          + `<span>${esc(s.status)} • ${esc(s.availableCapacity)} kWh available</span><br>`
          + `<button class="btn btn-sm btn-primary mt-2" data-view="${esc(s.stationId)}">View details</button>`
        )
        .addTo(layer);
    });
    if (pts.length === 1) {
      map.setView([Number(pts[0].latitude), Number(pts[0].longitude)], 10);
    } else if (pts.length > 1) {
      map.fitBounds(window.L.latLngBounds(pts.map((s) => [Number(s.latitude), Number(s.longitude)])).pad(0.2));
    }
  }

  // Popup "View details" buttons live inside Leaflet's DOM — delegate globally.
  document.addEventListener("click", (event) => {
    const btn = event.target.closest ? event.target.closest("[data-view]") : null;
    if (btn && window.SolarUI.ui && typeof window.SolarUI.ui.showDetails === "function") {
      window.SolarUI.ui.showDetails(btn.dataset.view);
    }
  });

  window.SolarUI.map = { refresh };
})();
