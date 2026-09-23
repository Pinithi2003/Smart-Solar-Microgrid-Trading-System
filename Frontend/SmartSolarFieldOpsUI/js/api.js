/* API client for Member 4 Field Operations & Member 2 Solar Stations integration. */
(function () {
  const { API_BASE_URL } = window.SolarUI.config;
  const opsBase = `${API_BASE_URL}/api/field-operations`;
  const stationsBase = `${API_BASE_URL}/api/solarstations`;

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

  window.SolarUI.fieldApi = {
    // Field operations endpoints
    getAll: (status) => request(status && status !== "All" ? `${opsBase}?status=${encodeURIComponent(status)}` : opsBase),
    getPending: () => request(`${opsBase}/pending`),
    getCompleted: () => request(`${opsBase}/completed`),
    getOne: (id) => request(`${opsBase}/${encodeURIComponent(id)}`),
    verify: (verificationCode, operatorId = "OPR-FIELD-01") =>
      request(`${opsBase}/verify`, {
        method: "POST",
        body: JSON.stringify({ verificationCode, operatorId })
      }),
    complete: (reservationId, notes, operatorId = "OPR-FIELD-01") =>
      request(`${opsBase}/${encodeURIComponent(reservationId)}/complete`, {
        method: "PATCH",
        body: JSON.stringify({ notes, operatorId })
      }),

    // Reusing Member 2's Infrastructure API for Nearby Stations
    getStations: () => request(stationsBase)
  };
})();
