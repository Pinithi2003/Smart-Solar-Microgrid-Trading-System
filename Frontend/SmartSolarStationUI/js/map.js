/* Read-only station map preview (Leaflet). Showcase only — full map features
   belong to Member 4. Never writes data; markers reflect /api/solarstations. */
(function () {
  const COLORS = { Active: "#198754", Inactive: "#dc3545", Maintenance: "#e0a800" };
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

  function colorFor(status) {
    const k = String(status ?? "").trim().toLowerCase();
    if (k === "active") return COLORS.Active;
    if (k === "inactive") return COLORS.Inactive;
    if (k === "maintenance") return COLORS.Maintenance;
    return COLORS[status] || "#333333";
  }

  function spreadOverlaps(pts) {
    const used = [];
    const out = [];
    pts.forEach((s) => {
      let lat = Number(s.latitude);
      let lng = Number(s.longitude);
      let tries = 0;
      while (used.some((p) => Math.hypot(p.lat - lat, p.lng - lng) < 0.18) && tries < 8) {
        const ang = (2 * Math.PI * tries) / 8;
        lat = Number(s.latitude) + 0.15 * Math.cos(ang);
        lng = Number(s.longitude) + 0.15 * Math.sin(ang);
        tries++;
      }
      used.push({ lat, lng });
      out.push({ s, lat, lng });
    });
    return out;
  }

  function refresh(stations) {
    if (!ensure() || !Array.isArray(stations)) return;
    try { map.invalidateSize(); } catch { /* best-effort */ }
    layer.clearLayers();
    const pts = stations.filter((s) => Number.isFinite(Number(s.latitude)) && Number.isFinite(Number(s.longitude)));
    spreadOverlaps(pts).forEach(({ s, lat, lng }) => {
      const color = colorFor(s.status);
      window.L.marker([lat, lng], { icon: dotIcon(color), title: s.stationName })
        .bindTooltip(`<strong>${esc(s.stationName)}</strong> • ${esc(s.status)}`, {
          direction: "top",
          offset: [0, -12],
          opacity: 0.95,
          className: "station-tip"
        })
        .bindPopup(
          `<strong>${esc(s.stationName)}</strong><br>`
          + `<span>${esc(s.stationId)} • ${esc(s.location)}</span><br>`
          + `<span>${esc(s.status)} • ${esc(s.availableCapacity)} kWh available</span><br>`
          + `<button class="btn btn-sm btn-sky mt-2" data-view="${esc(s.stationId)}">View details</button>`
        )
        .addTo(layer);
    });
    if (pts.length === 1) {
      map.setView([Number(pts[0].latitude), Number(pts[0].longitude)], 10);
    } else if (pts.length > 1) {
      map.fitBounds(window.L.latLngBounds(pts.map((s) => [Number(s.latitude), Number(s.longitude)])).pad(0.3), { maxZoom: 9 });
    } else {
      map.setView([7.0, 80.7], 7);
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
