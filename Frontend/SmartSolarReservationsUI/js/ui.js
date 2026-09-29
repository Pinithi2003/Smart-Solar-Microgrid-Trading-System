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
      Cancelled: "text-bg-danger",
      Available: "text-bg-success",
      Full: "text-bg-secondary",
      Inactive: "text-bg-warning",
      Unavailable: "text-bg-warning"
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
    if (!currentUserId) currentUserId = "USR001";
    if ($("userIdInput")) $("userIdInput").value = currentUserId;
  }

  /* ---------------- date bounds: today .. today+7 (7-day booking window) ---------------- */

  function localDateValue(d) {
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  }

  function slotDay(value) {
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? "" : localDateValue(d);
  }

  function setDateBounds() {
    const today = new Date();
    const max = new Date();
    max.setDate(today.getDate() + 7);
    const min = localDateValue(today);
    const end = localDateValue(max);
    if ($("dateInput") && !$("dateInput").value) $("dateInput").value = min;
    if ($("slotDate")) {
      $("slotDate").min = min;
      $("slotDate").max = end;
      if (!$("slotDate").value) $("slotDate").value = min;
    }
  }

  /* ---------------- stations dropdown ---------------- */

  async function loadStations() {
    try {
      stations = await api.getStations();
    } catch (err) {
      stations = [];
      toast(`Could not load stations: ${err.message}`, "error");
    }
    const option = (s) => {
      const blocked = s.status === "Active" ? "" : ` (${esc(s.status)} — booking rejected)`;
      return `<option value="${esc(s.stationId)}">${esc(s.stationId)} — ${esc(s.stationName)}${blocked}</option>`;
    };
    const options = stations.map(option).join("");
    if ($("stationSelect")) $("stationSelect").innerHTML = '<option value="">Select a station…</option>' + options;
    if ($("slotStation")) $("slotStation").innerHTML = '<option value="">Select a station…</option>' + stations.filter((s) => s.status === "Active").map(option).join("");
  }

  /* ---------------- slots dropdown, filtered by station + date ---------------- */

  let editingSlotId = "";
  let slotCatalog = [];

  async function loadSlots() {
    if (!$("stationSelect") || !$("slotSelect")) return;
    const stationId = $("stationSelect").value;
    const slotSelect = $("slotSelect");

    if (!stationId) {
      slots = [];
      slotSelect.innerHTML = '<option value="">Choose a station and date first…</option>';
      slotSelect.disabled = true;
      $("energyAmount").disabled = true;
      $("slotHint").textContent = "";
      return;
    }

    if (!$("dateInput").value) $("dateInput").value = localDateValue(new Date());
    slotSelect.disabled = false;
    slotSelect.innerHTML = '<option value="">Loading slots…</option>';

    let stationSlots = [];
    try {
      stationSlots = await api.getSlots(stationId);
    } catch (err) {
      toast(`Could not load slots: ${err.message}`, "error");
    }

    const today = localDateValue(new Date());
    const upcoming = stationSlots
      .filter((s) => slotDay(s.date) >= today)
      .sort((a, b) => slotDay(a.date).localeCompare(slotDay(b.date)) || a.startTime.localeCompare(b.startTime));

    let date = $("dateInput").value;
    let daySlots = upcoming.filter((s) => slotDay(s.date) === date);
    if (!daySlots.length && upcoming.length) {
      date = slotDay(upcoming[0].date);
      $("dateInput").value = date;
      daySlots = upcoming.filter((s) => slotDay(s.date) === date);
    }

    slots = daySlots;
    const available = daySlots.filter((s) => s.status === "Available" && s.availableEnergy > 0);
    slotSelect.innerHTML = available.length
      ? available.map((s) => `<option value="${esc(s.slotId)}">${esc(s.startTime)} – ${esc(s.endTime)} (${s.availableEnergy} kWh left)</option>`).join("")
      : '<option value="">No available slots for this date</option>';

    const openDays = [...new Set(upcoming.filter((s) => s.status === "Available" && s.availableEnergy > 0).map((s) => slotDay(s.date)))];
    $("slotHint").textContent = available.length
      ? `${available.length} open slot(s) on ${date}.`
      : (openDays.length ? `No open slots on ${date}. Open days: ${openDays.join(", ")}` : "This station has no open slots in the next 7 days.");

    updateSlotHint();
    $("energyAmount").disabled = available.length === 0;
    updateStats();
  }

  async function loadSlotCatalog() {
    try {
      slotCatalog = await api.getSlots();
    } catch (err) {
      slotCatalog = [];
      toast(`Could not load booking slots: ${err.message}`, "error");
    }
    const body = $("slotsBody");
    if (!body) return;
    if (!slotCatalog.length) {
      body.innerHTML = '<tr><td colspan="6" class="text-center text-muted py-4">No booking slots yet.</td></tr>';
      return;
    }
    body.innerHTML = slotCatalog.map((s) => `
      <tr>
        <td><strong>${esc(s.slotId)}</strong></td>
        <td>${esc(s.stationId)}</td>
        <td>${slotDay(s.date)}<br><small class="text-muted">${esc(s.startTime)} – ${esc(s.endTime)}</small></td>
        <td>${s.availableEnergy} / ${s.energyCapacity || s.availableEnergy} kWh</td>
        <td>${statusBadge(s.status)}</td>
        <td class="text-end text-nowrap">
          <button class="btn btn-sm btn-outline-secondary me-1" data-edit-slot="${esc(s.slotId)}">Edit</button>
          <button class="btn btn-sm btn-outline-danger" data-delete-slot="${esc(s.slotId)}">Delete</button>
        </td>
      </tr>`).join("");
  }

  function updateSlotHint() {
    if (!$("slotSelect") || !$("energyAmount")) return;
    const slotId = $("slotSelect").value;
    const slot = slots.find((s) => s.slotId === slotId);
    if (!slot) {
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
      const booked = result.reservation;
      toast(`Reservation ${booked.reservationId} confirmed.`);
      await showSummary("Reservation created", booked);
      $("reservationForm").reset();
      loadCurrentUser();
      setDateBounds();
      await loadSlots();
    } catch (err) {
      showFormError(err.message);
    } finally {
      btn.disabled = false;
      btn.textContent = "Reserve Energy";
    }
  }

  /* ---------------- reservation tables ---------------- */

  function bookingDay(reservation) {
    return slotDay(reservation.date || reservation.bookingDate);
  }

  function isPending(reservation) {
    return reservation.status === "Confirmed";
  }

  function isApprovedFuture(reservation) {
    const today = localDateValue(new Date());
    return (reservation.status === "Confirmed" || reservation.status === "Verified") && bookingDay(reservation) >= today;
  }

  function isCurrent(reservation) {
    const today = localDateValue(new Date());
    return (reservation.status === "Confirmed" || reservation.status === "Verified") && bookingDay(reservation) === today;
  }

  function isHistory(reservation) {
    return reservation.status === "Completed" || reservation.status === "Cancelled" || bookingDay(reservation) < localDateValue(new Date());
  }

  function matchesView(reservation, view) {
    if (view === "pending") return isPending(reservation);
    if (view === "history") return isHistory(reservation);
    if (view === "current") return isCurrent(reservation);
    return true;
  }

  function applyReservationFilters(list) {
    const view = $("viewFilter")?.value || "current";
    const status = $("statusFilter")?.value || "";
    const query = ($("reservationSearch")?.value || "").trim().toLowerCase();
    return list.filter((reservation) => {
      if (!matchesView(reservation, view)) return false;
      if (status && reservation.status !== status) return false;
      if (!query) return true;
      return [reservation.reservationId, reservation.userId, reservation.stationId, reservation.slotId, reservation.verificationCode, reservation.status]
        .join(" ")
        .toLowerCase()
        .includes(query);
    });
  }

  function renderReservationRows(list, { showUser }) {
    const visible = applyReservationFilters(list);
    if (!visible.length) {
      return `<tr><td colspan="${showUser ? 8 : 7}" class="text-center text-muted py-4">No reservations in this view.</td></tr>`;
    }
    list = visible;
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
          <td class="text-end text-nowrap">
            <button class="btn btn-sm btn-outline-secondary me-1" data-details="${esc(r.reservationId)}">Edit</button>
            <button class="btn btn-sm btn-outline-danger me-1" data-cancel="${esc(r.reservationId)}" ${canCancel ? "" : "disabled"}>Cancel</button>
            <button class="btn btn-sm btn-outline-dark" data-delete="${esc(r.reservationId)}">Delete</button>
          </td>
        </tr>`;
    }).join("");
  }

  async function refreshTables() {
    if (!$("myReservationsBody") && !$("allReservationsBody")) return;
    const userId = ($("userIdInput")?.value || "").trim() || currentUserId;

    try {
      myReservations = userId ? await api.getUserReservations(userId) : [];
    } catch (err) {
      myReservations = [];
      toast(`Could not load your reservations: ${err.message}`, "error");
    }
    if ($("myReservationsBody")) {
      $("myReservationsBody").innerHTML = renderReservationRows(myReservations, { showUser: false });
    }

    try {
      allReservations = await api.getAllReservations();
    } catch (err) {
      allReservations = [];
    }
    if ($("allReservationsBody")) {
      $("allReservationsBody").innerHTML = renderReservationRows(allReservations, { showUser: true });
    }
    updateStats();
  }

  /* ---------------- stat cards ---------------- */

  function updateStats() {
    if (!$("statPending")) return;
    $("statPending").textContent = allReservations.filter(isPending).length;
    $("statApprovedFuture").textContent = allReservations.filter(isApprovedFuture).length;
    $("statCurrent").textContent = allReservations.filter(isCurrent).length;
    $("statHistory").textContent = allReservations.filter(isHistory).length;
  }

  function showTextSummary(title, rows) {
    $("summaryTitle").textContent = title;
    $("summaryBody").innerHTML = rows
      .map(([label, value]) => `<div class="detail-row"><span>${esc(label)}</span><strong>${esc(value)}</strong></div>`)
      .join("");
    const qrBox = $("summaryQr");
    qrBox.innerHTML = "";
    qrBox.classList.add("d-none");
    bootstrap.Modal.getOrCreateInstance($("summaryModal")).show();
  }

  async function showSummary(title, reservation) {
    $("summaryTitle").textContent = title;
    $("summaryBody").innerHTML = `
      <div class="detail-row"><span>Reservation</span><strong>${esc(reservation.reservationId)}</strong></div>
      <div class="detail-row"><span>User</span><strong>${esc(reservation.userId)}</strong></div>
      <div class="detail-row"><span>Station / slot</span><strong>${esc(reservation.stationId)} · ${esc(reservation.slotId)}</strong></div>
      <div class="detail-row"><span>When</span><strong>${bookingDay(reservation)} ${esc(reservation.startTime)}–${esc(reservation.endTime)}</strong></div>
      <div class="detail-row"><span>Energy</span><strong>${reservation.energyAmount} kWh</strong></div>
      <div class="detail-row"><span>Status</span><strong>${statusBadge(reservation.status)}</strong></div>
      <div class="detail-row"><span>Verification code</span><strong><code>${esc(reservation.verificationCode)}</code></strong></div>`;
    const qrBox = $("summaryQr");
    qrBox.innerHTML = "";
    if (reservation.verificationCode && window.QRCode) {
      qrBox.classList.remove("d-none");
      new QRCode(qrBox, { text: reservation.verificationCode, width: 180, height: 180 });
    } else {
      qrBox.classList.add("d-none");
    }
    bootstrap.Modal.getOrCreateInstance($("summaryModal")).show();
  }

  /* ---------------- sidebar (mobile) ---------------- */

  function setSidebar(open) {
    $("sidebar").classList.toggle("open", open);
    $("sidebarBackdrop").classList.toggle("d-none", !open);
  }

  function initSidebar() {
    $("sidebarToggle")?.addEventListener("click", () => {
      setSidebar(!$("sidebar").classList.contains("open"));
    });
    $("sidebarBackdrop")?.addEventListener("click", () => setSidebar(false));
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") setSidebar(false);
    });
    document.querySelectorAll(".sidebar-nav .nav-item[data-module]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const mod = btn.dataset.module;
        if (mod === "stations") window.location.href = "../SmartSolarStationUI/index.html";
        else if (mod === "qr") window.location.href = "../SmartSolarFieldOpsUI/index.html";
        else if (mod === "dashboard" || mod === "users") window.location.href = "../SmartSolarUsersUI/index.html";
        else if (mod === "booking") window.location.href = "index.html";
        else if (mod === "reservations") window.location.href = "reservations.html";
      });
    });
  }

  let detailsReservationId = "";

  async function submitSlot(e) {
    e.preventDefault();
    const box = $("slotFormError");
    box.classList.add("d-none");
    const payload = {
      stationId: $("slotStation").value,
      date: $("slotDate").value,
      startTime: $("slotStart").value.trim(),
      endTime: $("slotEnd").value.trim(),
      energyCapacity: parseFloat($("slotCapacity").value)
    };
    const btn = $("slotSubmitBtn");
    btn.disabled = true;
    try {
      if (editingSlotId) {
        const result = await api.updateSlot(editingSlotId, payload);
        const slot = result.slot || { slotId: editingSlotId, ...payload };
        toast(`Slot ${editingSlotId} updated.`);
        showTextSummary("Booking slot updated", [
          ["Slot", slot.slotId || editingSlotId],
          ["Station", slot.stationId || payload.stationId],
          ["When", `${payload.date} ${payload.startTime}–${payload.endTime}`],
          ["Capacity", `${payload.energyCapacity} kWh`],
          ["Status", slot.status || "Updated"]
        ]);
      } else {
        const result = await api.createSlot(payload);
        const slot = result.slot;
        toast(`Slot ${slot.slotId} created (${slot.availableEnergy} kWh available).`);
        showTextSummary("Booking slot created", [
          ["Slot", slot.slotId],
          ["Station", slot.stationId],
          ["When", `${slotDay(slot.date)} ${slot.startTime}–${slot.endTime}`],
          ["Available energy", `${slot.availableEnergy} kWh`],
          ["Status", slot.status]
        ]);
      }
      editingSlotId = "";
      btn.textContent = "Create slot";
      $("slotForm").reset();
      setDateBounds();
      await loadStations();
      await loadSlotCatalog();
      if ($("stationSelect")) await loadSlots();
    } catch (err) {
      box.textContent = err.message;
      box.classList.remove("d-none");
    } finally {
      btn.disabled = false;
    }
  }

  async function openDetails(reservationId) {
    let reservation;
    try {
      reservation = await api.getReservation(reservationId);
    } catch (err) {
      toast(err.message, "error");
      return;
    }
    detailsReservationId = reservation.reservationId;
    $("detailsTitle").textContent = reservation.reservationId;
    $("detailsBody").innerHTML = `
      <p class="mb-1"><strong>${esc(reservation.stationId)}</strong> · ${esc(reservation.slotId)}</p>
      <p class="mb-1">${new Date(reservation.date || reservation.bookingDate).toLocaleDateString()} ${esc(reservation.startTime)}–${esc(reservation.endTime)}</p>
      <p class="mb-1">${reservation.energyAmount} kWh · ${statusBadge(reservation.status)}</p>
      <p class="mb-0">Verification code: <code>${esc(reservation.verificationCode)}</code></p>`;
    $("detailsUpdate").classList.toggle("d-none", reservation.status !== "Confirmed");
    $("updateEnergy").value = reservation.energyAmount;
    $("markVerifiedBtn").classList.toggle("d-none", reservation.status !== "Confirmed");
    $("markCompletedBtn").classList.toggle("d-none", reservation.status !== "Verified");
    bootstrap.Modal.getOrCreateInstance($("detailsModal")).show();
  }

  async function changeStatus(status) {
    try {
      await api.updateStatus(detailsReservationId, status);
      const updated = await api.getReservation(detailsReservationId);
      toast(`Reservation ${detailsReservationId} is now ${status}.`);
      bootstrap.Modal.getOrCreateInstance($("detailsModal")).hide();
      await refreshTables();
      await showSummary("Reservation status updated", updated);
    } catch (err) {
      toast(err.message, "error");
    }
  }

  async function submitUpdate(e) {
    e.preventDefault();
    try {
      await api.updateReservation(detailsReservationId, { energyAmount: parseFloat($("updateEnergy").value) });
      const updated = await api.getReservation(detailsReservationId);
      toast(`Reservation ${detailsReservationId} updated.`);
      bootstrap.Modal.getOrCreateInstance($("detailsModal")).hide();
      if ($("stationSelect")) await loadSlots();
      await refreshTables();
      await showSummary("Reservation updated", updated);
    } catch (err) {
      toast(err.message, "error");
    }
  }

  function beginSlotEdit(slotId) {
    const slot = slotCatalog.find((s) => s.slotId === slotId);
    if (!slot) return;
    editingSlotId = slotId;
    $("slotStation").value = slot.stationId;
    $("slotDate").value = slotDay(slot.date);
    $("slotStart").value = slot.startTime;
    $("slotEnd").value = slot.endTime;
    $("slotCapacity").value = slot.energyCapacity || slot.availableEnergy;
    $("slotSubmitBtn").textContent = `Save ${slotId}`;
    $("slotForm").scrollIntoView({ behavior: "smooth", block: "center" });
  }

  async function deleteSlot(slotId) {
    if (!confirm(`Delete booking slot ${slotId}?`)) return;
    try {
      await api.deleteSlot(slotId);
      toast(`Slot ${slotId} deleted.`);
      showTextSummary("Booking slot deleted", [
        ["Slot", slotId],
        ["Result", "The slot was removed. Confirmed reservations must be cancelled first."]
      ]);
      if (editingSlotId === slotId) {
        editingSlotId = "";
        $("slotSubmitBtn").textContent = "Create slot";
        $("slotForm").reset();
        setDateBounds();
      }
      await loadSlotCatalog();
      if ($("stationSelect")) await loadSlots();
    } catch (err) {
      toast(err.message, "error");
    }
  }

  async function deleteReservation(reservationId) {
    if (!confirm(`Delete reservation ${reservationId}? A confirmed booking is cancelled first, which is only allowed up to 12 hours before the start time.`)) return;
    try {
      await api.deleteReservation(reservationId);
      toast(`Reservation ${reservationId} deleted.`);
      showTextSummary("Reservation deleted", [
        ["Reservation", reservationId],
        ["Result", "The reservation was removed. A confirmed booking is cancelled first when the 12-hour rule allows it."]
      ]);
      await loadSlotCatalog();
      if ($("stationSelect")) await loadSlots();
      await refreshTables();
    } catch (err) {
      toast(err.message, "error");
    }
  }

  async function handleRowClick(e) {
    const details = e.target.closest("[data-details]");
    if (details) {
      openDetails(details.dataset.details);
      return;
    }
    const remove = e.target.closest("[data-delete]");
    if (remove) {
      deleteReservation(remove.dataset.delete);
      return;
    }
    await handleCancelClick(e);
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
      if ($("stationSelect")) await loadSlots();
      await loadSlotCatalog();
      await refreshTables();
      if (result.reservation) await showSummary("Reservation cancelled", result.reservation);
    } catch (err) {
      toast(err.message, "error");
      btn.disabled = false;
    }
  }

  /* ---------------- wire up ---------------- */

  document.addEventListener("DOMContentLoaded", async () => {
    initSidebar();
    loadCurrentUser();
    setDateBounds();
    await loadStations();
    await loadSlotCatalog();
    if ($("stationSelect")) await loadSlots();
    await refreshTables();

    $("stationSelect")?.addEventListener("change", loadSlots);
    $("dateInput")?.addEventListener("change", loadSlots);
    $("slotSelect")?.addEventListener("change", updateSlotHint);
    $("reservationForm")?.addEventListener("submit", submitReservation);
    $("slotForm")?.addEventListener("submit", submitSlot);
    $("detailsUpdate")?.addEventListener("submit", submitUpdate);
    $("markVerifiedBtn")?.addEventListener("click", () => changeStatus("Verified"));
    $("markCompletedBtn")?.addEventListener("click", () => changeStatus("Completed"));
    $("userIdInput")?.addEventListener("change", refreshTables);
    $("viewFilter")?.addEventListener("change", refreshTables);
    $("statusFilter")?.addEventListener("change", refreshTables);
    $("reservationSearch")?.addEventListener("input", () => {
      if ($("myReservationsBody")) $("myReservationsBody").innerHTML = renderReservationRows(myReservations, { showUser: false });
      if ($("allReservationsBody")) $("allReservationsBody").innerHTML = renderReservationRows(allReservations, { showUser: true });
    });
    $("myReservationsBody")?.addEventListener("click", handleRowClick);
    $("allReservationsBody")?.addEventListener("click", handleRowClick);
    $("slotsBody")?.addEventListener("click", (e) => {
      const edit = e.target.closest("[data-edit-slot]");
      const remove = e.target.closest("[data-delete-slot]");
      if (edit) beginSlotEdit(edit.dataset.editSlot);
      if (remove) deleteSlot(remove.dataset.deleteSlot);
    });
    $("refreshBtn")?.addEventListener("click", async () => {
      await loadStations();
      await loadSlotCatalog();
      if ($("stationSelect")) await loadSlots();
      await refreshTables();
      toast("Refreshed.");
    });
  });
})();
