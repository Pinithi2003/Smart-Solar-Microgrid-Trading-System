/* Reservations UI state + rendering (Bootstrap 5 + vanilla JS).
   Talks to the live Member 3 API (/api/booking-slots, /api/reservations)
   and read-only to Member 2's /api/solarstations for the station dropdown. */
(function () {
  const api = window.SolarUI.api;
  const $ = (id) => document.getElementById(id);

  let stations = [];
  let slots = [];
  let myReservations = [];
  let allReservations = [];
  let currentUserId = "";

  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
  }

  function statusBadge(status) {
    const map = {
      Confirmed: "text-bg-success",
      Verified: "text-bg-info",
      Completed: "text-bg-secondary",
      Cancelled: "text-bg-danger"
    };
    return `<span class="badge ${map[status] || "text-bg-dark"}">${esc(status)}</span>`;
  }

  function toast(message, type = "success") {
    const wrap = $("toastContainer");
    const el = document.createElement("div");
    el.className = `toast align-items-center text-bg-${type === "success" ? "success" : "danger"} border-0`;
    el.setAttribute("role", "alert");
    el.innerHTML = `<div class="d-flex"><div class="toast-body">${esc(message)}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    wrap.appendChild(el);
    bootstrap.Toast.getOrCreateInstance(el, { delay: 4000 }).show();
    el.addEventListener("hidden.bs.toast", () => el.remove());
  }

  function showFormError(message) {
    const box = $("formError");
    if (!message) {
      box.classList.add("d-none");
      box.textContent = "";
      return;
    }
    box.classList.remove("d-none");
    box.textContent = message;
  }

  /* ---------------- current user (reused from the Users module's login) ---------------- */

  function loadCurrentUser() {
    try {
      const stored = JSON.parse(localStorage.getItem("solarGridUser") || "null");
      currentUserId = stored?.id || "";
    } catch {
      currentUserId = "";
    }
    $("userIdInput").value = currentUserId;
  }

  /* ---------------- date bounds: today .. today+7 (7-day booking window) ---------------- */

  function setDateBounds() {
    const today = new Date();
    const max = new Date();
    max.setDate(today.getDate() + 7);
    const fmt = (d) => d.toISOString().slice(0, 10);
    $("dateInput").min = fmt(today);
    $("dateInput").max = fmt(max);
  }

  /* ---------------- stations dropdown ---------------- */

  async function loadStations() {
    try {
      stations = await api.getStations();
    } catch (err) {
      stations = [];
      toast(`Could not load stations: ${err.message}`, "error");
    }
    const select = $("stationSelect");
    select.innerHTML = '<option value="">Select a station…</option>' +
      stations
        .filter((s) => s.status === "Active")
        .map((s) => `<option value="${esc(s.stationId)}">${esc(s.stationId)} — ${esc(s.stationName)}</option>`)
        .join("");
  }

  /* ---------------- slots dropdown, filtered by station + date ---------------- */

  async function loadSlots() {
    const stationId = $("stationSelect").value;
    const date = $("dateInput").value;
    const slotSelect = $("slotSelect");

    if (!stationId || !date) {
      slots = [];
      slotSelect.innerHTML = '<option value="">Choose a station and date first…</option>';
      slotSelect.disabled = true;
      $("energyAmount").disabled = true;
      $("slotHint").textContent = "";
      return;
    }

    slotSelect.disabled = false;
    slotSelect.innerHTML = '<option value="">Loading slots…</option>';

    try {
      slots = await api.getSlots(stationId, date);
    } catch (err) {
      slots = [];
      toast(`Could not load slots: ${err.message}`, "error");
    }

    const available = slots.filter((s) => s.status === "Available" && s.availableEnergy > 0);
    slotSelect.innerHTML = available.length
      ? available.map((s) => `<option value="${esc(s.slotId)}">${esc(s.startTime)} – ${esc(s.endTime)} (${s.availableEnergy} kWh left)</option>`).join("")
      : '<option value="">No available slots for this date</option>';

    updateSlotHint();
    $("energyAmount").disabled = available.length === 0;
  }

  function updateSlotHint() {
    const slotId = $("slotSelect").value;
    const slot = slots.find((s) => s.slotId === slotId);
    if (!slot) {
      $("slotHint").textContent = "";
      $("energyAmount").removeAttribute("max");
      return;
    }
    $("slotHint").textContent = `Up to ${slot.availableEnergy} kWh available in this slot.`;
    $("energyAmount").max = slot.availableEnergy;
  }

  /* ---------------- create reservation ---------------- */

  async function submitReservation(e) {
    e.preventDefault();
    showFormError("");

    const payload = {
      userId: $("userIdInput").value.trim(),
      stationId: $("stationSelect").value,
      slotId: $("slotSelect").value,
      energyAmount: parseFloat($("energyAmount").value)
    };

    if (!payload.userId || !payload.stationId || !payload.slotId || !payload.energyAmount) {
      showFormError("Please fill in user, station, slot and energy amount.");
      return;
    }

    const btn = $("submitBtn");
    btn.disabled = true;
    btn.textContent = "Booking…";

    try {
      const result = await api.createReservation(payload);
      toast(`Reservation ${result.reservation.reservationId} confirmed (code ${result.reservation.verificationCode}).`);
      $("reservationForm").reset();
      loadCurrentUser();
      setDateBounds();
      await loadSlots();
      await refreshTables();
    } catch (err) {
      showFormError(err.message);
    } finally {
      btn.disabled = false;
      btn.textContent = "Reserve Energy";
    }
  }

  /* ---------------- reservation tables ---------------- */

  function renderReservationRows(list, { showUser }) {
    if (!list.length) {
      return `<tr><td colspan="${showUser ? 8 : 7}" class="text-center text-muted py-4">No reservations yet.</td></tr>`;
    }
    return list.map((r) => {
      const canCancel = r.status === "Confirmed";
      return `
        <tr>
          <td>${esc(r.reservationId)}</td>
          ${showUser ? `<td>${esc(r.userId)}</td>` : ""}
          <td>${esc(r.stationId)}</td>
          <td>${new Date(r.date).toLocaleDateString()}<br><small class="text-muted">${esc(r.startTime)}–${esc(r.endTime)}</small></td>
          <td>${r.energyAmount} kWh</td>
          <td>${statusBadge(r.status)}</td>
          <td><code>${esc(r.verificationCode)}</code></td>
          <td>
            <button class="btn btn-sm btn-outline-danger" data-cancel="${esc(r.reservationId)}" ${canCancel ? "" : "disabled"}>
              Cancel
            </button>
          </td>
        </tr>`;
    }).join("");
  }

  async function refreshTables() {
    const userId = $("userIdInput").value.trim() || currentUserId;

    try {
      myReservations = userId ? await api.getUserReservations(userId) : [];
    } catch (err) {
      myReservations = [];
      toast(`Could not load your reservations: ${err.message}`, "error");
    }
    $("myReservationsBody").innerHTML = renderReservationRows(myReservations, { showUser: false });

    try {
      allReservations = await api.getAllReservations();
    } catch (err) {
      allReservations = [];
    }
    $("allReservationsBody").innerHTML = renderReservationRows(allReservations, { showUser: true });
  }

  async function handleCancelClick(e) {
    const btn = e.target.closest("[data-cancel]");
    if (!btn) return;
    const reservationId = btn.dataset.cancel;
    if (!confirm(`Cancel reservation ${reservationId}? This is only allowed up to 12 hours before the booked time.`)) return;

    btn.disabled = true;
    try {
      const result = await api.cancelReservation(reservationId);
      toast(`Reservation ${reservationId} cancelled. Energy restored.`);
      await loadSlots();
      await refreshTables();
    } catch (err) {
      toast(err.message, "error");
      btn.disabled = false;
    }
  }

  /* ---------------- wire up ---------------- */

  document.addEventListener("DOMContentLoaded", async () => {
    loadCurrentUser();
    setDateBounds();
    await loadStations();
    await loadSlots();
    await refreshTables();

    $("stationSelect").addEventListener("change", loadSlots);
    $("dateInput").addEventListener("change", loadSlots);
    $("slotSelect").addEventListener("change", updateSlotHint);
    $("reservationForm").addEventListener("submit", submitReservation);
    $("userIdInput").addEventListener("change", refreshTables);
    $("myReservationsBody").addEventListener("click", handleCancelClick);
    $("allReservationsBody").addEventListener("click", handleCancelClick);
    $("refreshBtn").addEventListener("click", async () => {
      await loadStations();
      await loadSlots();
      await refreshTables();
      toast("Refreshed.");
    });
  });
})();
