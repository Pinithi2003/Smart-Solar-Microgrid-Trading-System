/* One reservation: details, change slot, and cancel. The 12-hour rule closes both actions. */
(function () {
  const { api, esc, formatLong, formatShort, statusLabel, statusClass, hoursUntil, canChange, dayKey } = window.ResUI;
  const params = new URLSearchParams(window.location.search);
  const id = params.get("id");
  let reservation = null;
  let station = null;

  function renderClosed(open) {
    const note = document.getElementById("closedNote");
    note.hidden = open;
    document.getElementById("changeBtn").disabled = !open;
    document.getElementById("cancelBtn").disabled = !open;
    if (open) return;
    if (reservation.status !== "Confirmed") {
      note.innerHTML = `<b>Changes closed</b><div>Only an approved booking can be changed or cancelled. This one is ${esc(statusLabel(reservation.status))}.</div>`;
      return;
    }
    const start = `${formatShort(reservation.date)}, ${reservation.startTime || ""}`.trim();
    note.innerHTML = `<b>Changes closed</b><div>Changes close 12 hours before the booking starts. The deadline was ${esc(start)}.</div>`;
  }

  function render() {
    document.getElementById("title").textContent = station?.stationName || reservation.stationId;
    document.getElementById("when").textContent = `${formatLong(reservation.date)} · ${reservation.startTime} – ${reservation.endTime}`;
    document.getElementById("ref").textContent = reservation.reservationId;
    document.getElementById("who").textContent = reservation.userId;
    document.getElementById("station").textContent = station?.stationName || reservation.stationId;
    document.getElementById("where").textContent = station?.location || "";
    document.getElementById("date").textContent = formatLong(reservation.date);
    document.getElementById("time").textContent = `${reservation.startTime} – ${reservation.endTime}`;
    document.getElementById("energy").textContent = `${reservation.energyAmount} kWh`;
    document.getElementById("code").textContent = reservation.verificationCode || "—";
    const pill = document.getElementById("statusPill");
    pill.textContent = statusLabel(reservation.status);
    pill.className = `pill ${statusClass(reservation.status)}`;
    const hours = hoursUntil(reservation);
    const starts = document.getElementById("starts");
    if (hours <= 0) starts.textContent = "Started";
    else if (hours < 48) starts.textContent = `Starts in ${Math.floor(hours)}h ${Math.floor((hours % 1) * 60)}m`;
    else starts.textContent = `Starts ${formatShort(reservation.date)}`;
    renderClosed(canChange(reservation));
    if (params.get("created") === "1") {
      document.getElementById("createdNote").hidden = false;
    }
  }

  document.getElementById("changeBtn").addEventListener("click", async () => {
    const box = document.getElementById("slotChoices");
    box.hidden = false;
    const all = await api.getSlots(reservation.stationId);
    const today = dayKey(new Date());
    const choices = all.filter((s) => s.slotId !== reservation.slotId && dayKey(s.date) >= today && s.status === "Available" && s.availableEnergy >= reservation.energyAmount);
    box.innerHTML = choices.length
      ? choices.map((s) => `<button type="button" class="slot" data-slot="${esc(s.slotId)}"><span><b>${esc(s.startTime)} – ${esc(s.endTime)}</b><div class="sub">${esc(dayKey(s.date))}</div></span><span>${s.availableEnergy} kWh left</span></button>`).join("")
      : `<div class="empty">No other open slot can take this energy amount.</div>`;
  });

  document.getElementById("slotChoices").addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-slot]");
    if (!btn) return;
    try {
      await api.updateReservation(reservation.reservationId, { slotId: btn.dataset.slot });
      window.location.href = `details.html?id=${encodeURIComponent(reservation.reservationId)}`;
    } catch (err) {
      document.getElementById("actionError").textContent = err.message;
    }
  });

  document.getElementById("cancelBtn").addEventListener("click", async () => {
    if (!confirm("Cancel this booking? Energy is restored only when at least 12 hours remain.")) return;
    try {
      await api.cancelReservation(reservation.reservationId);
      window.location.reload();
    } catch (err) {
      document.getElementById("actionError").textContent = err.message;
    }
  });

  document.addEventListener("DOMContentLoaded", async () => {
    if (!id) {
      document.getElementById("title").textContent = "Reservation not found";
      return;
    }
    try {
      reservation = await api.getReservation(id);
      const stations = await api.getStations();
      station = stations.find((s) => s.stationId === reservation.stationId) || null;
      render();
    } catch (err) {
      document.getElementById("title").textContent = err.message;
    }
  });
})();
