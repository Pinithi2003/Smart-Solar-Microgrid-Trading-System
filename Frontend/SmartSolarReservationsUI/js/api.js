/* Fetch wrapper for the Member 3 Reservation API (/api/booking-slots, /api/reservations).
   Also reads (read-only) from Member 2's /api/solarstations to populate the station dropdown.
   Throws Error with .status and .data on non-2xx, with server messages extracted. */
(function () {
  const { API_BASE_URL } = window.SolarUI.config;

  function extractError(data, status) {
    if (!data) return `Request failed (HTTP ${status}).`;
    if (typeof data === "string") return data;
    if (data.message) return data.message;
    if (data.errors && typeof data.errors === "object") {
      return Object.entries(data.errors)
        .map(([k, v]) => `${k}: ${Array.isArray(v) ? v.join(", ") : v}`)
        .join(" | ");
    }
    if (data.title) return data.title;
    return `Request failed (HTTP ${status}).`;
  }

  async function request(url, options = {}) {
    const res = await fetch(url, {
      headers: { "Content-Type": "application/json" },
      ...options
    });
    const text = await res.text();
    let data = null;
    try {
      data = text ? JSON.parse(text) : null;
    } catch {
      data = text;
    }
    if (!res.ok) {
      const err = new Error(extractError(data, res.status));
      err.status = res.status;
      err.data = data;
      throw err;
    }
    return data;
  }

  const slotsBase = `${API_BASE_URL}/api/booking-slots`;
  const resBase = `${API_BASE_URL}/api/reservations`;
  const stationsBase = `${API_BASE_URL}/api/solarstations`;

  window.SolarUI.api = {
    // booking slots
    getSlots: (stationId, date) => {
      const params = new URLSearchParams();
      if (stationId) params.set("stationId", stationId);
      if (date) params.set("date", date);
      const qs = params.toString();
      return request(`${slotsBase}${qs ? `?${qs}` : ""}`);
    },
    getSlot: (slotId) => request(`${slotsBase}/${encodeURIComponent(slotId)}`),

    // reservations
    getAllReservations: () => request(resBase),
    getReservation: (id) => request(`${resBase}/${encodeURIComponent(id)}`),
    getUserReservations: (userId) => request(`${resBase}/user/${encodeURIComponent(userId)}`),
    createReservation: (payload) => request(resBase, { method: "POST", body: JSON.stringify(payload) }),
    cancelReservation: (id) => request(`${resBase}/${encodeURIComponent(id)}/cancel`, { method: "PATCH" }),

    // read-only: Member 2's stations, used only to populate the dropdown
    getStations: () => request(stationsBase)
  };
})();
