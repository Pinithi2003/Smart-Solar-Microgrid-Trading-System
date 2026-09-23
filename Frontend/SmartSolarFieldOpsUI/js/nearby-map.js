/* Nearby Solar Stations & Leaflet Geospatial View (Member 4)
   Calculates distances using Haversine formula and maps Member 2 solar stations. */
(function () {
  let map = null;
  let stationsLayer = null;
  let operatorMarker = null;
  let radiusCircle = null;
  let allStations = [];

  // Default operator position: Colombo
  let currentPosition = {
    lat: 6.9271,
    lng: 79.8612,
    label: "Colombo (Grid HQ)"
  };

  const CITY_PRESETS = {
    colombo: { lat: 6.9271, lng: 79.8612, label: "Colombo (Grid HQ)" },
    kalutara: { lat: 6.5854, lng: 79.9607, label: "Kalutara" },
    kandy: { lat: 7.2906, lng: 80.6337, label: "Kandy" },
    galle: { lat: 6.0535, lng: 80.2210, label: "Galle" },
    negombo: { lat: 7.2083, lng: 79.8358, label: "Negombo" },
    matara: { lat: 5.9549, lng: 80.5550, label: "Matara" },
    malabe: { lat: 6.9061, lng: 79.9647, label: "Malabe" }
  };

  // Haversine formula to compute great-circle distance between two GPS coordinates in kilometers
  function haversineDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Earth's radius in km
    const dLat = (lat2 - lat1) * (Math.PI / 180);
    const dLon = (lon2 - lon1) * (Math.PI / 180);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
      Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  function formatDistance(km) {
    if (!Number.isFinite(km)) return "–";
    if (km < 1) return `${Math.round(km * 1000)} m`;
    return `${km.toFixed(1)} km`;
  }

  function initMap() {
    if (map || typeof window.L === "undefined") return;

    map = window.L.map("nearbyMap", { scrollWheelZoom: true }).setView([currentPosition.lat, currentPosition.lng], 9);

    window.L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 18,
      attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);

    stationsLayer = window.L.layerGroup().addTo(map);
    updateOperatorMarker();
  }

  function updateOperatorMarker() {
    if (!map || typeof window.L === "undefined") return;

    if (operatorMarker) map.removeLayer(operatorMarker);
    if (radiusCircle) map.removeLayer(radiusCircle);

    // Custom pulsing beacon for operator's field position
    const beaconIcon = window.L.divIcon({
      className: "",
      html: `<div class="pulse-beacon" title="Your Field Location"></div>`,
      iconSize: [16, 16],
      iconAnchor: [8, 8]
    });

    operatorMarker = window.L.marker([currentPosition.lat, currentPosition.lng], {
      icon: beaconIcon,
      zIndexOffset: 1000
    }).addTo(map).bindPopup(`<strong>Your Location</strong><br />${currentPosition.label}`);

    // Proximity 25 km service circle
    radiusCircle = window.L.circle([currentPosition.lat, currentPosition.lng], {
      radius: 25000,
      color: "#0284c7",
      fillColor: "#e0f2fe",
      fillOpacity: 0.15,
      weight: 1.5,
      dashArray: "4, 6"
    }).addTo(map);
  }

  function getStatusColor(status) {
    const s = String(status || "").toLowerCase();
    if (s === "active") return "#198754";
    if (s === "maintenance") return "#e0a800";
    if (s === "inactive") return "#dc3545";
    return "#475467";
  }

  function renderStations(stations) {
    allStations = stations || [];
    if (!map || !stationsLayer) initMap();
    if (!map || !stationsLayer) return;

    stationsLayer.clearLayers();

    // Compute distances from current operator location
    const enriched = allStations.map((s) => {
      const lat = Number(s.latitude);
      const lng = Number(s.longitude);
      const distance = (Number.isFinite(lat) && Number.isFinite(lng))
        ? haversineDistance(currentPosition.lat, currentPosition.lng, lat, lng)
        : Infinity;
      return { ...s, distance };
    });

    // Sort by closest distance first
    enriched.sort((a, b) => a.distance - b.distance);

    // Plot station markers on map
    enriched.forEach((s) => {
      if (!Number.isFinite(Number(s.latitude)) || !Number.isFinite(Number(s.longitude))) return;

      const color = getStatusColor(s.status);
      const markerHtml = `
        <span style="display:block;width:18px;height:18px;border-radius:50%;
                     background:${color};border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.35);"></span>
      `;
      const icon = window.L.divIcon({
        className: "",
        html: markerHtml,
        iconSize: [18, 18],
        iconAnchor: [9, 9]
      });

      const popupHtml = `
        <div style="min-width: 180px;">
          <h6 class="mb-1 fw-bold">${s.stationName}</h6>
          <div class="small text-muted mb-2">${s.stationId} • ${s.location}</div>
          <div class="d-flex justify-content-between mb-1">
            <span class="small">Distance:</span>
            <strong class="small text-primary">${formatDistance(s.distance)}</strong>
          </div>
          <div class="d-flex justify-content-between mb-2">
            <span class="small">Available:</span>
            <strong class="small text-success">${s.availableCapacity} / ${s.totalCapacity} kWh</strong>
          </div>
          <button class="btn btn-sm btn-outline-success w-100" data-filter-station="${s.stationId}">
            View Field Ops Here
          </button>
        </div>
      `;

      window.L.marker([s.latitude, s.longitude], { icon })
        .bindPopup(popupHtml)
        .bindTooltip(`<strong>${s.stationName}</strong> (${formatDistance(s.distance)})`, { direction: "top", offset: [0, -10] })
        .addTo(stationsLayer);
    });

    // Render the station cards list
    renderCardsList(enriched);
  }

  function renderCardsList(sortedStations) {
    const listContainer = document.getElementById("nearbyCardsList");
    if (!listContainer) return;

    if (!sortedStations.length) {
      listContainer.innerHTML = `<div class="text-center text-muted py-4">No solar stations found.</div>`;
      return;
    }

    listContainer.innerHTML = sortedStations.map((s) => {
      const statusBadgeClass = s.status === "Active" ? "badge-soft-completed"
        : s.status === "Maintenance" ? "badge-soft-confirmed" : "badge-soft-cancelled";
      return `
        <div class="nearby-station-card" data-station-id="${s.stationId}" data-lat="${s.latitude}" data-lng="${s.longitude}">
          <div class="d-flex justify-content-between align-items-start mb-1">
            <h6 class="mb-0 fw-bold">${s.stationName}</h6>
            <span class="distance-badge"><i class="fa-solid fa-route"></i> ${formatDistance(s.distance)}</span>
          </div>
          <div class="d-flex justify-content-between align-items-center small text-muted mb-2">
            <span>${s.stationId} • ${s.location}</span>
            <span class="${statusBadgeClass}">${s.status}</span>
          </div>
          <div class="d-flex justify-content-between align-items-center small">
            <span>Available Capacity:</span>
            <strong>${s.availableCapacity} kWh <span class="text-muted fw-normal">/ ${s.totalCapacity} kWh</span></strong>
          </div>
        </div>
      `;
    }).join("");

    // Clicking a card zooms map to station
    listContainer.querySelectorAll(".nearby-station-card").forEach((card) => {
      card.addEventListener("click", () => {
        listContainer.querySelectorAll(".nearby-station-card").forEach((c) => c.classList.remove("selected"));
        card.classList.add("selected");
        const lat = Number(card.dataset.lat);
        const lng = Number(card.dataset.lng);
        if (map && Number.isFinite(lat) && Number.isFinite(lng)) {
          map.flyTo([lat, lng], 13, { duration: 1.2 });
        }
      });
    });
  }

  function setOperatorPosition(lat, lng, label) {
    currentPosition = { lat, lng, label };
    const labelEl = document.getElementById("currentLocationLabel");
    if (labelEl) labelEl.textContent = label;

    if (map) {
      updateOperatorMarker();
      map.panTo([lat, lng]);
    }
    renderStations(allStations);
  }

  function init() {
    initMap();

    // Preset location buttons
    document.querySelectorAll("[data-city-preset]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const city = btn.dataset.cityPreset;
        const preset = CITY_PRESETS[city];
        if (preset) {
          setOperatorPosition(preset.lat, preset.lng, preset.label);
        }
      });
    });

    // Real GPS Geolocation button
    const gpsBtn = document.getElementById("useGpsBtn");
    if (gpsBtn) {
      gpsBtn.addEventListener("click", () => {
        if (!navigator.geolocation) {
          alert("Geolocation is not supported by your browser.");
          return;
        }
        gpsBtn.disabled = true;
        gpsBtn.innerHTML = `<span class="spinner-border spinner-border-sm"></span> Locating…`;
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            gpsBtn.disabled = false;
            gpsBtn.innerHTML = `<i class="fa-solid fa-location-crosshairs"></i> Use My GPS Location`;
            setOperatorPosition(pos.coords.latitude, pos.coords.longitude, "My Current GPS Location");
          },
          (err) => {
            gpsBtn.disabled = false;
            gpsBtn.innerHTML = `<i class="fa-solid fa-location-crosshairs"></i> Use My GPS Location`;
            alert("Could not obtain GPS position (" + err.message + "). Please select a preset city.");
          },
          { timeout: 10000, enableHighAccuracy: true }
        );
      });
    }

    // Popup "View Field Ops Here" button delegation
    document.addEventListener("click", (e) => {
      const btn = e.target.closest("[data-filter-station]");
      if (btn && window.SolarUI.ui) {
        window.SolarUI.ui.filterByStation(btn.dataset.filterStation);
      }
    });
  }

  window.SolarUI = window.SolarUI || {};
  window.SolarUI.map = {
    init,
    renderStations,
    invalidateSize: () => { if (map) map.invalidateSize(); }
  };
})();
