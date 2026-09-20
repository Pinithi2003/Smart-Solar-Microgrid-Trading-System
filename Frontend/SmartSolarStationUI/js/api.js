/* Fetch wrapper for the Member 2 Infrastructure API (/api/solarstations).
   Throws Error with .status and .data on non-2xx, with server messages extracted. */
(function () {
  const { API_BASE_URL } = window.SolarUI.config;
  const base = `${API_BASE_URL}/api/solarstations`;

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

  async function request(path, options = {}) {
    const res = await fetch(base + path, {
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

  window.SolarUI.api = {
    getAll: () => request(""),
    getOne: (id) => request(`/${encodeURIComponent(id)}`),
    create: (station) => request("", { method: "POST", body: JSON.stringify(station) }),
    update: (id, station) => request(`/${encodeURIComponent(id)}`, { method: "PUT", body: JSON.stringify(station) }),
    setStatus: (id, status) =>
      request(`/${encodeURIComponent(id)}/status`, { method: "PATCH", body: JSON.stringify({ status }) }),
    deactivate: (id) => request(`/${encodeURIComponent(id)}`, { method: "DELETE" })
  };
})();
