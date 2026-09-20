/* Coordinate picker for the Add/Edit station modal: mini Leaflet map with a
   draggable marker (two-way synced with the latitude/longitude inputs) plus a
   free Nominatim name lookup. Writes only the form fields — nothing is saved
   until the user presses Save Station. */
(function () {
  const DEFAULT_VIEW = [7.0, 80.7];
  const DEFAULT_ZOOM = 7;
  let map = null;
  let marker = null;
  let inited = false;

  const $ = (id) => document.getElementById(id);

  function num(value, fallback) {
    const n = Number(value);
    return Number.isFinite(n) ? n : fallback;
  }

  function readFields() {
    return {
      lat: num($("f_latitude").value, DEFAULT_VIEW[0]),
      lng: num($("f_longitude").value, DEFAULT_VIEW[1])
    };
  }

  function writeFields(lat, lng) {
    $("f_latitude").value = Number(lat).toFixed(4);
    $("f_longitude").value = Number(lng).toFixed(4);
  }

  function showGeoError(message) {
    const box = $("geoError");
    if (!message) {
      box.classList.add("d-none");
      box.textContent = "";
      return;
    }
    box.classList.remove("d-none");
    box.textContent = message;
  }

  function ensureMap() {
    if (inited) return true;
    if (typeof window.L === "undefined") {
      showGeoError("Mini-map unavailable (map library could not load) — type the coordinates manually.");
      return false;
    }
    try {
      map = window.L.map("pickerMap", { scrollWheelZoom: false }).setView(DEFAULT_VIEW, DEFAULT_ZOOM);
      window.L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
        maxZoom: 18,
        attribution: "&copy; OpenStreetMap contributors"
      }).addTo(map);
      marker = window.L.marker(DEFAULT_VIEW, { draggable: true }).addTo(map);
      marker.on("dragend", () => {
        const p = marker.getLatLng();
        writeFields(p.lat, p.lng);
      });
      map.on("click", (e) => {
        marker.setLatLng(e.latlng);
        writeFields(e.latlng.lat, e.latlng.lng);
      });
      inited = true;
      return true;
    } catch {
      showGeoError("Mini-map failed to start — type the coordinates manually.");
      return false;
    }
  }

  function syncMarkerFromFields(zoomTo = false) {
    if (!ensureMap()) return;
    const { lat, lng } = readFields();
    marker.setLatLng([lat, lng]);
    if (zoomTo) map.setView([lat, lng], Math.max(map.getZoom(), 9));
    else map.panTo([lat, lng]);
  }

  function reset(lat, lng) {
    showGeoError(null);
    $("geoResults").classList.add("d-none");
    $("geoResults").innerHTML = "";
    if (Number.isFinite(Number(lat)) && Number.isFinite(Number(lng))) {
      if (!ensureMap()) return;
      marker.setLatLng([Number(lat), Number(lng)]);
      map.setView([Number(lat), Number(lng)], Math.max(map.getZoom(), 9));
    } else if (ensureMap()) {
      marker.setLatLng(DEFAULT_VIEW);
      map.setView(DEFAULT_VIEW, DEFAULT_ZOOM);
    }
  }

  async function lookup() {
    showGeoError(null);
    const results = $("geoResults");
    results.classList.add("d-none");
    results.innerHTML = "";
    const name = ($("f_location").value || "").trim();
    if (!name) {
      showGeoError("Type a location name first, then search.");
      return;
    }
    const btn = $("geoLookupBtn");
    btn.disabled = true;
    const original = btn.innerHTML;
    btn.innerHTML = `<span class="spinner-border spinner-border-sm"></span> Searching…`;
    try {
      let data = await search(name + ", Sri Lanka");
      if (!data.length) data = await search(name); // fallback: worldwide
      if (!data.length) {
        showGeoError(`No coordinates found for "${name}". Check the spelling or enter them manually.`);
        return;
      }
      results.innerHTML = data.slice(0, 3).map((d, i) => `
        <button type="button" class="list-group-item list-group-item-action py-2" data-geo="${i}">
          <strong>${escapeHtml(shortName(d.display_name))}</strong><br />
          <small class="text-muted">${Number(d.lat).toFixed(4)}, ${Number(d.lon).toFixed(4)}</small>
        </button>`).join("");
      results.classList.remove("d-none");
      results.querySelectorAll("[data-geo]").forEach((el) => {
        el.addEventListener("click", () => {
          const d = data[Number(el.dataset.geo)];
          writeFields(d.lat, d.lon);
          syncMarkerFromFields(true);
          results.classList.add("d-none");
        });
      });
    } catch {
      showGeoError("Lookup unavailable (no connection?) — enter the coordinates manually.");
    } finally {
      btn.disabled = false;
      btn.innerHTML = original;
    }
  }

  async function search(query) {
    const url = `https://nominatim.openstreetmap.org/search?format=json&limit=5&q=${encodeURIComponent(query)}`;
    const res = await fetch(url, { headers: { Accept: "application/json" } });
    if (!res.ok) throw new Error(`lookup HTTP ${res.status}`);
    return res.json();
  }

  function shortName(displayName) {
    return String(displayName).split(",").slice(0, 3).join(",");
  }

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
  }

  function init() {
    $("geoLookupBtn").addEventListener("click", lookup);
    $("f_latitude").addEventListener("change", () => syncMarkerFromFields(false));
    $("f_longitude").addEventListener("change", () => syncMarkerFromFields(false));
    // Leaflet needs a visible container: refresh size every time the modal opens.
    $("stationModal").addEventListener("shown.bs.modal", () => {
      if (ensureMap()) {
        map.invalidateSize();
        syncMarkerFromFields(false);
      }
    });
  }

  document.addEventListener("DOMContentLoaded", init);

  window.SolarUI.picker = { reset };
})();
