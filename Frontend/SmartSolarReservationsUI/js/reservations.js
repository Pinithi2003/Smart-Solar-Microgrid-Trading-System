/* All-reservations list. Shows every booking from the API, not only today's. */
(function () {
  const { api, esc, dayKey, formatShort, statusLabel, statusClass } = window.ResUI;
  let reservations = [];
  let stations = [];

  function stationName(id) {
    return stations.find((s) => s.stationId === id)?.stationName || id;
  }

  function filtered() {
    const q = (document.getElementById("search").value || "").trim().toLowerCase();
    const status = document.getElementById("statusFilter").value;
    const station = document.getElementById("stationFilter").value;
    const view = document.getElementById("viewFilter").value;
    const today = dayKey(new Date());
    return reservations.filter((r) => {
      const day = dayKey(r.date);
      if (view === "current" && !((r.status === "Confirmed" || r.status === "Verified") && day === today)) return false;
      if (view === "pending" && r.status !== "Confirmed") return false;
      if (view === "future" && !((r.status === "Confirmed" || r.status === "Verified") && day >= today)) return false;
      if (view === "history" && !(r.status === "Completed" || r.status === "Cancelled" || day < today)) return false;
      if (status && r.status !== status) return false;
      if (station && r.stationId !== station) return false;
      if (!q) return true;
      return [r.reservationId, r.userId, r.stationId, stationName(r.stationId), r.verificationCode, statusLabel(r.status)]
        .join(" ").toLowerCase().includes(q);
    });
  }

  function render() {
    const rows = filtered();
    document.getElementById("count").textContent = `${rows.length} of ${reservations.length}`;
    const host = document.getElementById("rows");
    if (!rows.length) {
      host.innerHTML = `<div class="empty">No reservations match this view.</div>`;
      return;
    }
    host.innerHTML = rows.map((r) => `
      <a class="row" href="details.html?id=${encodeURIComponent(r.reservationId)}">
        <div class="person">
          <span class="initials">${esc((r.userId || "?").slice(0, 2).toUpperCase())}</span>
          <div><strong>${esc(r.userId)}</strong><div class="sub">${esc(r.reservationId)}</div></div>
        </div>
        <div><strong>${esc(stationName(r.stationId))}</strong><div class="sub">${esc(formatShort(r.date))} · ${esc(r.startTime)} – ${esc(r.endTime)}</div></div>
        <span class="pill ${statusClass(r.status)}">${esc(statusLabel(r.status))}</span>
        <span>›</span>
      </a>`).join("");
  }

  document.getElementById("openBtn").addEventListener("click", () => {
    const id = document.getElementById("lookup").value.trim();
    if (!id) return;
    window.location.href = `details.html?id=${encodeURIComponent(id)}`;
  });
  ["search", "statusFilter", "stationFilter", "viewFilter"].forEach((id) => {
    document.getElementById(id).addEventListener("input", render);
    document.getElementById(id).addEventListener("change", render);
  });

  document.addEventListener("DOMContentLoaded", async () => {
    try {
      [reservations, stations] = await Promise.all([api.getAllReservations(), api.getStations()]);
    } catch (err) {
      document.getElementById("rows").innerHTML = `<div class="empty">${esc(err.message)}</div>`;
      return;
    }
    const stationFilter = document.getElementById("stationFilter");
    stations.forEach((s) => {
      const opt = document.createElement("option");
      opt.value = s.stationId;
      opt.textContent = s.stationName || s.stationId;
      stationFilter.appendChild(opt);
    });
    const pending = reservations.filter((r) => r.status === "Confirmed").length;
    const today = dayKey(new Date());
    const future = reservations.filter((r) => (r.status === "Confirmed" || r.status === "Verified") && dayKey(r.date) >= today).length;
    document.getElementById("counts").textContent = `Pending ${pending} · Approved future ${future}`;
    render();
  });
})();
