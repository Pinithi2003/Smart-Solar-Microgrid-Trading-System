/* Create a reservation: station, date inside 7 days, slot, then confirm. */
(function () {
  const { api, esc, dayKey, formatLong, userId } = window.ResUI;
  let stations = [];
  let slots = [];
  let selectedStation = "";
  let selectedDate = dayKey(new Date());
  let selectedSlot = null;

  function windowDays() {
    const days = [];
    const start = new Date();
    start.setHours(0, 0, 0, 0);
    for (let i = 0; i < 8; i += 1) {
      const d = new Date(start);
      d.setDate(start.getDate() + i);
      days.push(dayKey(d));
    }
    return days;
  }

  function renderStations() {
    document.getElementById("stations").innerHTML = stations.map((s) => `
      <button type="button" class="station-card ${s.stationId === selectedStation ? "selected" : ""}" data-station="${esc(s.stationId)}" ${s.status === "Active" ? "" : "disabled"}>
        <div class="sub">${esc(s.status)}</div>
        <b>${esc(s.stationName)}</b>
        <div class="sub">${esc(s.location)}</div>
        <div style="margin-top:8px">Capacity <b>${esc(s.availableCapacity)} kWh</b></div>
      </button>`).join("");
  }

  function renderCalendar() {
    const today = dayKey(new Date());
    document.getElementById("calendar").innerHTML = windowDays().map((key) => {
      const date = new Date(key + "T12:00:00");
      const count = slots.filter((s) => dayKey(s.date) === key && s.status === "Available" && s.availableEnergy > 0).length;
      const label = key === today ? "TODAY" : date.toLocaleDateString(undefined, { weekday: "short" }).toUpperCase();
      return `<button type="button" class="day ${key === selectedDate ? "selected" : ""}" data-date="${key}">
        <small>${label}</small><b>${date.getDate()}</b><small>${count ? `${count} open` : "None open"}</small>
      </button>`;
    }).join("");
  }

  function renderSlots() {
    const daySlots = slots.filter((s) => dayKey(s.date) === selectedDate);
    const host = document.getElementById("slotList");
    if (!selectedStation) {
      host.innerHTML = `<div class="empty">Select a station first.</div>`;
      return;
    }
    if (!daySlots.length) {
      host.innerHTML = `<div class="empty">No slots on this date.</div>`;
      return;
    }
    host.innerHTML = daySlots.map((s) => `
      <button type="button" class="slot ${selectedSlot?.slotId === s.slotId ? "selected" : ""}" data-slot="${esc(s.slotId)}" ${s.status === "Available" && s.availableEnergy > 0 ? "" : "disabled"}>
        <span><b>${esc(s.startTime)} – ${esc(s.endTime)}</b><div class="sub">${esc(s.slotId)}</div></span>
        <span>${s.availableEnergy} kWh left<br><span class="sub">of ${s.energyCapacity || s.availableEnergy}</span></span>
      </button>`).join("");
  }

  function renderSummary() {
    const station = stations.find((s) => s.stationId === selectedStation);
    document.getElementById("sumStation").textContent = station ? station.stationName : "—";
    document.getElementById("sumWhere").textContent = station?.location || "";
    document.getElementById("sumDate").textContent = selectedDate ? formatLong(selectedDate) : "—";
    document.getElementById("sumTime").textContent = selectedSlot ? `${selectedSlot.startTime} – ${selectedSlot.endTime}` : "—";
    document.getElementById("sumLeft").textContent = selectedSlot ? `${selectedSlot.availableEnergy} kWh remaining` : "—";
    const ready = station && selectedSlot && Number(document.getElementById("energy").value) > 0;
    document.getElementById("confirmBtn").disabled = !ready;
  }

  document.getElementById("stations").addEventListener("click", async (e) => {
    const btn = e.target.closest("[data-station]");
    if (!btn) return;
    selectedStation = btn.dataset.station;
    selectedSlot = null;
    renderStations();
    slots = await api.getSlots(selectedStation);
    renderCalendar();
    renderSlots();
    renderSummary();
  });

  document.getElementById("calendar").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-date]");
    if (!btn || btn.disabled) return;
    selectedDate = btn.dataset.date;
    selectedSlot = null;
    renderCalendar();
    renderSlots();
    renderSummary();
  });

  document.getElementById("slotList").addEventListener("click", (e) => {
    const btn = e.target.closest("[data-slot]");
    if (!btn || btn.disabled) return;
    selectedSlot = slots.find((s) => s.slotId === btn.dataset.slot) || null;
    renderSlots();
    renderSummary();
  });

  document.getElementById("energy").addEventListener("input", renderSummary);

  document.getElementById("confirmBtn").addEventListener("click", async () => {
    const box = document.getElementById("bookError");
    box.textContent = "";
    const btn = document.getElementById("confirmBtn");
    btn.disabled = true;
    try {
      const result = await api.createReservation({
        userId: document.getElementById("prosumer").value.trim() || userId(),
        stationId: selectedStation,
        slotId: selectedSlot.slotId,
        energyAmount: Number(document.getElementById("energy").value)
      });
      const id = result.reservation?.reservationId;
      window.location.href = `details.html?id=${encodeURIComponent(id)}&created=1`;
    } catch (err) {
      box.textContent = err.message;
      btn.disabled = false;
    }
  });

  document.addEventListener("DOMContentLoaded", async () => {
    document.getElementById("prosumer").value = userId();
    stations = await api.getStations();
    renderStations();
    renderCalendar();
    renderSlots();
    renderSummary();
  });
})();
