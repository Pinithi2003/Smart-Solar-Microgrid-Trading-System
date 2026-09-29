/* Slot lookup: create, update availability, and delete booking slots. */
(function () {
  const { api, esc, dayKey } = window.ResUI;
  let slots = [];
  let stations = [];
  let editing = "";

  function stationOptions(selected) {
    return `<option value="">Select a station…</option>` + stations
      .filter((s) => s.status === "Active")
      .map((s) => `<option value="${esc(s.stationId)}" ${s.stationId === selected ? "selected" : ""}>${esc(s.stationName)}</option>`)
      .join("");
  }

  function render() {
    const host = document.getElementById("slotRows");
    host.innerHTML = slots.length ? slots.map((s) => `
      <div class="row">
        <div><strong>${esc(s.slotId)}</strong><div class="sub">${esc(s.stationId)}</div></div>
        <div>${esc(dayKey(s.date))}<div class="sub">${esc(s.startTime)} – ${esc(s.endTime)}</div></div>
        <div>${s.availableEnergy} / ${s.energyCapacity} kWh<div class="sub">${esc(s.status)}</div></div>
        <div>
          <button class="btn-ghost" type="button" data-edit="${esc(s.slotId)}">Edit</button>
          <button class="btn-outline" type="button" data-del="${esc(s.slotId)}" style="width:auto;margin-top:0">Delete</button>
        </div>
      </div>`).join("") : `<div class="empty">No booking slots yet.</div>`;
  }

  document.getElementById("slotRows").addEventListener("click", (e) => {
    const edit = e.target.closest("[data-edit]");
    const del = e.target.closest("[data-del]");
    if (edit) {
      const slot = slots.find((s) => s.slotId === edit.dataset.edit);
      if (!slot) return;
      editing = slot.slotId;
      document.getElementById("slotStation").value = slot.stationId;
      document.getElementById("slotDate").value = dayKey(slot.date);
      document.getElementById("slotStart").value = slot.startTime;
      document.getElementById("slotEnd").value = slot.endTime;
      document.getElementById("slotCapacity").value = slot.energyCapacity;
      document.getElementById("slotSave").textContent = `Save ${slot.slotId}`;
    }
    if (del) deleteSlot(del.dataset.del);
  });

  async function deleteSlot(slotId) {
    if (!confirm(`Delete ${slotId}? A slot with a confirmed booking cannot be deleted.`)) return;
    try {
      await api.deleteSlot(slotId);
      slots = await api.getSlots();
      render();
    } catch (err) {
      document.getElementById("slotError").textContent = err.message;
    }
  }

  document.getElementById("slotForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = {
      stationId: document.getElementById("slotStation").value,
      date: document.getElementById("slotDate").value,
      startTime: document.getElementById("slotStart").value.trim(),
      endTime: document.getElementById("slotEnd").value.trim(),
      energyCapacity: Number(document.getElementById("slotCapacity").value)
    };
    const box = document.getElementById("slotError");
    box.textContent = "";
    try {
      if (editing) await api.updateSlot(editing, payload);
      else await api.createSlot(payload);
      editing = "";
      document.getElementById("slotSave").textContent = "Create slot";
      e.target.reset();
      slots = await api.getSlots();
      render();
    } catch (err) {
      box.textContent = err.message;
    }
  });

  document.addEventListener("DOMContentLoaded", async () => {
    stations = await api.getStations();
    document.getElementById("slotStation").innerHTML = stationOptions("");
    const today = new Date();
    const max = new Date();
    max.setDate(today.getDate() + 7);
    document.getElementById("slotDate").min = dayKey(today);
    document.getElementById("slotDate").max = dayKey(max);
    slots = await api.getSlots();
    render();
  });
})();
