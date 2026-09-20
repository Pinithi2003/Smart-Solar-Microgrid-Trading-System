/* Solar Station UI state + rendering (Bootstrap 5 + vanilla JS).
   Mirrors the demo prototype screens (stations.js / app.js) but reads/writes
   the live Member 2 API instead of localStorage mock data.
   Enhancements: dark theme, count-up stats, sorting, pagination, CSV export,
   confirm-before-deactivate, capacity bar in details, read-only map hook. */
(function () {
  const api = window.SolarUI.api;
  const PAGE_SIZE = 5;
  const REDUCED_MOTION = window.matchMedia
    ? window.matchMedia("(prefers-reduced-motion: reduce)").matches
    : false;

  let stations = [];
  let editingId = null;
  let sortKey = "stationId";
  let sortDir = 1;
  let page = 1;
  let pendingDeactivateId = null;
  let detailsWasOpen = false;
  const prevStatus = new Map();
  const displayedStats = { total: 0, active: 0, maintenance: 0, available: 0 };

  const $ = (id) => document.getElementById(id);
  const STATUS_RANK = { Active: 0, Maintenance: 1, Inactive: 2 };

  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
  }

  function statusBadge(status, pop = false) {
    const map = { Active: "badge-soft-active", Inactive: "badge-soft-inactive", Maintenance: "badge-soft-maintenance" };
    return `<span class="${map[status] || "text-bg-dark"}${pop ? " badge-pop" : ""}">${esc(status)}</span>`;
  }

  /* ---------------- toasts / form errors ---------------- */

  function toast(message, type = "success") {
    const wrap = $("toastContainer");
    const el = document.createElement("div");
    el.className = `toast align-items-center text-bg-${type === "success" ? "success" : "danger"} border-0`;
    el.setAttribute("role", "alert");
    el.innerHTML = `<div class="d-flex"><div class="toast-body">${esc(message)}</div>
      <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    wrap.appendChild(el);
    bootstrap.Toast.getOrCreateInstance(el, { delay: 3000 }).show();
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

  /* ---------------- stats with count-up ---------------- */

  function countUp(el, to) {
    const from = Number(el.dataset.value || 0);
    el.dataset.value = to;
    if (REDUCED_MOTION || from === to) {
      el.textContent = to;
      return;
    }
    const start = performance.now();
    const duration = 600;
    function frame(now) {
      const t = Math.min((now - start) / duration, 1);
      const eased = 1 - Math.pow(1 - t, 3);
      el.textContent = Math.round(from + (to - from) * eased);
      if (t < 1) requestAnimationFrame(frame);
    }
    requestAnimationFrame(frame);
  }

  function renderStats() {
    const targets = {
      total: stations.length,
      active: stations.filter((s) => s.status === "Active").length,
      maintenance: stations.filter((s) => s.status === "Maintenance").length,
      available: stations.reduce((sum, s) => sum + Number(s.availableCapacity || 0), 0)
    };
    countUp($("statTotal"), targets.total);
    countUp($("statActive"), targets.active);
    countUp($("statMaintenance"), targets.maintenance);
    countUp($("statAvailable"), targets.available);
    Object.assign(displayedStats, targets);
  }

  /* ---------------- filter / sort / paginate ---------------- */

  function filteredStations() {
    const q = (($("topSearchInput")?.value ?? $("searchInput")?.value) || "").toLowerCase();
    const f = $("statusFilter").value;
    return stations.filter((s) => {
      const hay = `${s.stationId} ${s.stationName} ${s.location} ${s.operator}`.toLowerCase();
      return (!q || hay.includes(q)) && (f === "All" || s.status === f);
    });
  }

  function sortedStations(rows) {
    const dir = sortDir;
    return [...rows].sort((a, b) => {
      let va = a[sortKey];
      let vb = b[sortKey];
      if (sortKey === "status") {
        va = STATUS_RANK[va] ?? 99;
        vb = STATUS_RANK[vb] ?? 99;
      }
      if (typeof va === "number" && typeof vb === "number") return (va - vb) * dir;
      return String(va ?? "").localeCompare(String(vb ?? "")) * dir;
    });
  }

  function skeletonRows() {
    return Array.from({ length: PAGE_SIZE }, () => `
      <tr class="skeleton"><td><span class="sk"></span></td><td><span class="sk"></span></td>
      <td><span class="sk"></span></td><td><span class="sk"></span></td>
      <td><span class="sk"></span></td><td><span class="sk"></span></td>
      <td><span class="sk"></span></td><td><span class="sk"></span></td></tr>`).join("");
  }

  function renderTable() {
    const rows = sortedStations(filteredStations());
    const pages = Math.max(1, Math.ceil(rows.length / PAGE_SIZE));
    if (page > pages) page = pages;
    if (page < 1) page = 1;
    const slice = rows.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
    const tbody = $("stationsBody");

    document.querySelectorAll("th[data-sort]").forEach((th) => {
      th.classList.remove("sorted-asc", "sorted-desc");
      if (th.dataset.sort === sortKey) th.classList.add(sortDir === 1 ? "sorted-asc" : "sorted-desc");
    });

    if (!slice.length) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">No stations match.</td></tr>`;
    } else {
      tbody.innerHTML = slice.map((s, i) => {
        const changed = prevStatus.has(s.stationId) && prevStatus.get(s.stationId) !== s.status;
        prevStatus.set(s.stationId, s.status);
        // Last visible row opens upward so the menu never covers rows below
        // or gets cut by the card edge; viewport boundary escapes the
        // table-responsive overflow context (no clipping).
        const drop = i === slice.length - 1 ? " dropup" : "";
        return `
        <tr class="row-enter" style="animation-delay:${Math.min(i, 11) * 25}ms">
          <td><strong>${esc(s.stationId)}</strong></td>
          <td>${esc(s.stationName)}</td>
          <td>${esc(s.location)}</td>
          <td class="text-center">${esc(s.totalCapacity)}</td>
          <td class="text-center">${esc(s.availableCapacity)}</td>
          <td>${statusBadge(s.status, changed)}</td>
          <td>${esc(s.operator)}</td>
          <td class="text-nowrap text-end">
            <div class="dropdown d-inline-block${drop}">
              <button class="btn btn-sm btn-pill-view" data-bs-toggle="dropdown" data-bs-boundary="viewport" aria-expanded="false"
                title="Row actions" aria-label="Actions for ${esc(s.stationId)}">
                <i class="fa-solid fa-ellipsis"></i>
              </button>
              <ul class="dropdown-menu dropdown-menu-end">
                <li><button class="dropdown-item" data-action="view" data-id="${esc(s.stationId)}"><i class="fa-solid fa-eye fa-fw me-2 text-secondary"></i>View</button></li>
                <li><button class="dropdown-item text-primary" data-action="edit" data-id="${esc(s.stationId)}"><i class="fa-solid fa-pen fa-fw me-2 text-primary"></i>Edit</button></li>
                <li><button class="dropdown-item ${s.status === "Active" ? "text-danger" : "text-success"}" data-action="toggle" data-id="${esc(s.stationId)}"><i class="fa-solid ${s.status === "Active" ? "fa-ban" : "fa-circle-check"} fa-fw me-2"></i>${s.status === "Active" ? "Deactivate" : "Activate"}</button></li>
              </ul>
            </div>
          </td>
        </tr>`;
      }).join("");
    }

    tbody.querySelectorAll("button[data-action]").forEach((btn) => {
      const { action, id } = btn.dataset;
      if (action === "view") btn.addEventListener("click", () => showDetails(id));
      if (action === "edit") btn.addEventListener("click", () => openModal(id));
      if (action === "toggle") btn.addEventListener("click", () => toggleStatus(id));
    });

    renderPager(pages, rows.length);
  }

  function renderPager(pages, total) {
    const pager = $("pager");
    if (!total) {
      pager.innerHTML = "";
      return;
    }
    let html = "";
    if (page > 1) {
      html += `<li class="page-item">
        <button class="page-link" data-page="${page - 1}">Prev</button></li>`;
    }
    for (let p = 1; p <= pages; p++) {
      html += `<li class="page-item${p === page ? " active" : ""}">
        <button class="page-link" data-page="${p}">${p}</button></li>`;
    }
    if (page < pages) {
      html += `<li class="page-item">
        <button class="page-link" data-page="${page + 1}">Next</button></li>`;
    } else {
      html += `<li class="page-item disabled">
        <button class="page-link" data-page="${page + 1}" disabled>Next</button></li>`;
    }
    pager.innerHTML = html;
    pager.querySelectorAll("button[data-page]").forEach((btn) => {
      btn.addEventListener("click", () => {
        page = Number(btn.dataset.page);
        renderTable();
      });
    });
  }

  function renderAll() {
    renderStats();
    renderTable();
    $("lastRefreshed").textContent = `Last refreshed ${new Date().toLocaleTimeString()}`;
    if (window.SolarUI.map) {
      try {
        window.SolarUI.map.refresh(stations);
      } catch { /* map is best-effort preview */ }
    }
  }

  async function reload(options = {}) {
    $("stationsBody").innerHTML = skeletonRows();
    try {
      stations = await api.getAll();
      if (options.gotoLast) {
        // New stations sort last (ST008+), so jump to the last page to reveal them.
        page = Math.max(1, Math.ceil(stations.length / PAGE_SIZE));
      } else if (options.reset) {
        page = 1;
      }
      renderAll();
    } catch (err) {
      $("stationsBody").innerHTML =
        `<tr><td colspan="8" class="text-center text-danger py-4">Could not load stations: ${esc(err.message)}</td></tr>`;
    }
  }

  /* ---------------- CSV export ---------------- */

  function exportCsv() {
    const rows = sortedStations(filteredStations());
    const header = ["stationId", "stationName", "location", "latitude", "longitude",
      "totalCapacity", "availableCapacity", "status", "operator", "lastUpdated"];
    const q = (v) => `"${String(v ?? "").replace(/"/g, '""')}"`;
    const csv = [header.join(",")]
      .concat(rows.map((s) => header.map((k) => q(s[k])).join(",")))
      .join("\r\n");
    const blob = new Blob([csv], { type: "text/csv" });
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = "solar-stations.csv";
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
      URL.revokeObjectURL(a.href);
      a.remove();
    }, 500);
    toast(`Exported ${rows.length} station(s) to CSV.`);
  }

  /* ---------------- add / edit ---------------- */

  function readForm() {
    return {
      stationId: $("f_stationId").value.trim(),
      stationName: $("f_stationName").value.trim(),
      location: $("f_location").value.trim(),
      latitude: Number($("f_latitude").value),
      longitude: Number($("f_longitude").value),
      totalCapacity: Number($("f_totalCapacity").value),
      availableCapacity: Number($("f_availableCapacity").value),
      status: $("f_status").value,
      operator: $("f_operator").value.trim() || "Grid Operator"
    };
  }

  /* Next auto ID: max numeric suffix among ST### + 1, zero-padded.
     Soft-deletes keep their records, so IDs are never reused. */
  function nextStationId() {
    let max = 0;
    let width = 3;
    stations.forEach((x) => {
      const m = /^ST(\d+)$/.exec(x?.stationId ?? "");
      if (m) {
        max = Math.max(max, Number(m[1]));
        width = Math.max(width, m[1].length);
      }
    });
    return `ST${String(max + 1).padStart(width, "0")}`;
  }

  /* Filled modal inputs read ash like the locked Station ID. */
  function refreshFilled() {
    document.querySelectorAll("#stationModal .form-control, #stationModal .form-select").forEach((el) => {
      if (el.id === "f_stationId") return;
      el.classList.toggle("is-filled", String(el.value ?? "").trim() !== "");
    });
  }

  function openModal(stationId = null) {    editingId = stationId;
    showFormError(null);
    const s = stationId ? stations.find((x) => x.stationId === stationId) : null;
    $("modalTitle").textContent = s ? `Edit Station: ${s.stationName}` : "Add Solar Station";
    $("f_stationId").value = s?.stationId ?? nextStationId();
    $("f_stationId").disabled = Boolean(s);
    $("f_stationId").readOnly = !s; // add mode: system-generated, locked
    $("f_stationId").title = s ? "" : "Auto-generated from existing stations";
    $("stationIdHint").style.display = s ? "none" : "";
    $("f_stationName").value = s?.stationName ?? "";
    $("f_location").value = s?.location ?? "";
    $("f_latitude").value = s?.latitude ?? "";
    $("f_longitude").value = s?.longitude ?? "";
    $("f_totalCapacity").value = s?.totalCapacity ?? "";
    $("f_availableCapacity").value = s?.availableCapacity ?? "";
    $("f_status").value = s?.status ?? "Active";
    $("f_operator").value = s?.operator ?? "Grid Operator";
    refreshFilled();
    // Keep the mini-map pin in sync with the form (pins follow coordinates).
    if (window.SolarUI.picker) {
      window.SolarUI.picker.reset(s?.latitude, s?.longitude);
    }
    bootstrap.Modal.getOrCreateInstance($("stationModal")).show();
  }

  /* Client-side pre-checks mirroring the server rules (SolarStationInfo):
     instant feedback without a round trip. The server re-validates everything. */
  function validateForm(p) {
    if (!p.stationId) return "Station ID is required (e.g. ST008).";
    if (!/^ST\d{3,}$/.test(p.stationId)) return "Station ID must look like ST008 (ST followed by at least 3 digits).";
    if (!p.stationName) return "Station name cannot be empty.";
    if (!p.location) return "Location cannot be empty.";
    if (!Number.isFinite(p.latitude) || p.latitude < -90 || p.latitude > 90)
      return "Latitude must be between -90 and 90.";
    if (!Number.isFinite(p.longitude) || p.longitude < -180 || p.longitude > 180)
      return "Longitude must be between -180 and 180.";
    if (p.latitude === 0 && p.longitude === 0)
      return "Pick the station position on the map — coordinates cannot be 0, 0.";
    if (!(p.totalCapacity > 0)) return "Total capacity must be greater than 0.";
    if (!(p.availableCapacity >= 0)) return "Available capacity cannot be negative.";
    if (p.availableCapacity > p.totalCapacity)
      return "Available capacity must not be greater than total capacity.";
    return null;
  }

  async function submitForm(event) {
    event.preventDefault();
    showFormError(null);
    const payload = readForm();
    const localError = validateForm(payload);
    if (localError) {
      showFormError(localError);
      return;
    }
    // Prevent double-submits: lock the Save button until the request settles.
    const saveBtn = $("stationForm").querySelector('[type="submit"]');
    saveBtn.disabled = true;
    try {
      if (editingId) {
        await api.update(editingId, payload);
        toast(`Station ${editingId} updated.`);
        bootstrap.Modal.getInstance($("stationModal")).hide();
        await reload();
      } else {
        const created = await api.create(payload);
        toast(`Station ${created.stationId} added.`);
        bootstrap.Modal.getInstance($("stationModal")).hide();
        await reload({ gotoLast: true });
      }
    } catch (err) {
      showFormError(err.message);
    } finally {
      saveBtn.disabled = false;
    }
  }

  /* ---------------- status + deactivate (with confirm) ---------------- */

  async function toggleStatus(stationId) {
    const s = stations.find((x) => x.stationId === stationId);
    if (!s) return;
    if (s.status === "Active") {
      askDeactivate(stationId);
      return;
    }
    try {
      await api.setStatus(stationId, "Active");
      toast(`Station ${stationId} → Active.`);
      await reload();
    } catch (err) {
      toast(err.message, "error");
    }
  }

  function askDeactivate(stationId) {
    pendingDeactivateId = stationId;
    // Stacking two Bootstrap modals shares one backdrop, so the details
    // content bleeds through behind the confirm dialog. Hide details while
    // confirming; it is restored on cancel (see confirmModal hidden handler).
    detailsWasOpen = $("detailsModal").classList.contains("show");
    if (detailsWasOpen) {
      bootstrap.Modal.getInstance($("detailsModal"))?.hide();
    }
    $("confirmModalMsg").textContent = (() => {
      const s = stations.find((x) => x.stationId === stationId);
      return `Are you sure you want to deactivate ${stationId}${s ? ` (${s.stationName})` : ""}?`;
    })();
    bootstrap.Modal.getOrCreateInstance($("confirmModal")).show();
  }

  async function confirmDeactivate() {
    const id = pendingDeactivateId;
    pendingDeactivateId = null;
    detailsWasOpen = false;
    bootstrap.Modal.getInstance($("confirmModal")).hide();
    if (!id) return;
    try {
      await api.deactivate(id);
      toast(`Station ${id} deactivated (soft delete).`);
      await reload();
    } catch (err) {
      toast(err.message, "error");
    }
  }

  /* ---------------- details (capacity bar + edit + copy) ---------------- */

  function capacityBar(s) {
    const total = Number(s.totalCapacity || 0);
    const avail = Number(s.availableCapacity || 0);
    const pct = total > 0 ? Math.max(0, Math.min(100, (avail / total) * 100)) : 0;
    // Bar color follows station status (not the %): Active green, Maintenance orange, Inactive red.
    const color = s.status === "Maintenance" ? "bg-warning"
      : s.status === "Inactive" ? "bg-danger" : "bg-success";
    return `
      <div class="capacity-bar mb-1"><div class="${color}" style="width:${pct.toFixed(1)}%"></div></div>
      <small class="text-muted">${esc(avail)} kWh available of ${esc(total)} kWh (${pct.toFixed(0)}%)</small>`;
  }

  async function showDetails(stationId) {
    try {
      const s = await api.getOne(stationId);
      const updated = s.lastUpdated ? new Date(s.lastUpdated).toLocaleString() : "-";
      const coords = `${s.latitude}, ${s.longitude}`;
      $("detailsTitle").textContent = s.stationName;
      $("detailsBody").innerHTML = `
        <div class="d-flex align-items-center gap-2 mb-3">
          ${statusBadge(s.status)}
          <span class="text-muted">${esc(s.stationId)} • ${esc(s.location)}</span>
        </div>
        <div class="mb-3">${capacityBar(s)}</div>
        <dl class="row mb-0">
          <dt class="col-sm-4">Coordinates</dt>
          <dd class="col-sm-8">${esc(coords)}
            <button class="btn btn-sm btn-link p-0 ms-1" data-copy="${esc(coords)}" title="Copy coordinates">Copy</button>
          </dd>
          <dt class="col-sm-4">Total capacity</dt><dd class="col-sm-8">${esc(s.totalCapacity)} kWh</dd>
          <dt class="col-sm-4">Available</dt><dd class="col-sm-8">${esc(s.availableCapacity)} kWh</dd>
          <dt class="col-sm-4">Operator</dt><dd class="col-sm-8">${esc(s.operator)}</dd>
          <dt class="col-sm-4">Last updated</dt><dd class="col-sm-8">${esc(updated)}</dd>
        </dl>`;
      $("detailsBody").querySelector("[data-copy]")?.addEventListener("click", async (e) => {
        const text = e.currentTarget.dataset.copy;
        try {
          await navigator.clipboard.writeText(text);
          toast("Coordinates copied.");
        } catch {
          toast("Copy failed in this browser.", "error");
        }
      });
      const editBtn = $("detailsEdit");
      editBtn.onclick = () => {
        bootstrap.Modal.getInstance($("detailsModal")).hide();
        openModal(s.stationId);
      };
      const deactBtn = $("detailsDeactivate");
      // Inactive stations have nothing to deactivate: hide the button entirely
      // rather than showing a confusing "Already Inactive" state.
      // (Reactivation is via Edit Station → Status → Active.)
      deactBtn.textContent = "Deactivate Station";
      deactBtn.disabled = false;
      deactBtn.style.display = s.status === "Inactive" ? "none" : "";
      deactBtn.onclick = () => askDeactivate(s.stationId);
      bootstrap.Modal.getOrCreateInstance($("detailsModal")).show();
    } catch (err) {
      toast(err.message, "error");
    }
  }

  /* ---------------- init ---------------- */

  /* ---------------- sidebar ---------------- */

  function setSidebar(open) {
    $("sidebar").classList.toggle("open", open);
    $("sidebarBackdrop").classList.toggle("d-none", !open);
  }

  function initSidebar() {
    $("sidebarToggle").addEventListener("click", () => {
      setSidebar(!$("sidebar").classList.contains("open"));
    });
    $("sidebarBackdrop").addEventListener("click", () => setSidebar(false));
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") setSidebar(false);
    });
    // Placeholder modules (Members 1/3/4 + shared docs): toast the owner.
    // Swap a toast for a real link when the owning member delivers the page.
    document.querySelectorAll(".sidebar-nav .nav-item[data-owner]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const label = btn.querySelector("span")?.textContent?.trim() || btn.dataset.module;
        toast(`${label} belongs to ${btn.dataset.owner} — coming soon.`);
        setSidebar(false);
      });
    });
  }

  function init() {
    $("addStationBtn").addEventListener("click", () => openModal());
    $("stationForm").addEventListener("submit", submitForm);
    $("stationForm").addEventListener("input", refreshFilled);
    $("stationForm").addEventListener("change", refreshFilled);
    $("topSearchInput")?.addEventListener("input", () => {
      page = 1;
      renderTable();
    });
    $("statusFilter").addEventListener("change", () => { page = 1; renderTable(); });
    document.querySelectorAll("#statusDropdown .dropdown-item").forEach((item) => {
      item.addEventListener("click", () => {
        document.querySelectorAll("#statusDropdown .dropdown-item").forEach((x) => x.classList.remove("active"));
        item.classList.add("active");
        $("statusFilter").value = item.dataset.value;
        $("statusFilterLabel").textContent = item.textContent.trim();
        page = 1;
        renderTable();
      });
    });
    $("filterBtn")?.addEventListener("click", () => {
      const btn = $("statusFilterBtn");
      btn.scrollIntoView({ behavior: REDUCED_MOTION ? "auto" : "smooth", block: "center" });
      btn.focus({ preventScroll: true });
      try {
        bootstrap.Dropdown.getOrCreateInstance(btn).toggle();
      } catch { /* best-effort */ }
      btn.classList.remove("flash");
      $("filterBtn")?.classList.remove("flash");
      if ($("filterBtn")) void $("filterBtn").offsetWidth; // restart animation
      $("filterBtn")?.classList.add("flash");
    });
    $("exportCsvBtn").addEventListener("click", exportCsv);
    $("confirmModalBtn").addEventListener("click", confirmDeactivate);
    // Cancel/X on the confirm dialog: bring back the details modal underneath.
    // (No-op after a real confirm — pendingDeactivateId is already cleared.)
    $("confirmModal").addEventListener("hidden.bs.modal", () => {
      if (pendingDeactivateId && detailsWasOpen) {
        const id = pendingDeactivateId;
        pendingDeactivateId = null;
        detailsWasOpen = false;
        showDetails(id);
      }
    });
    $("refreshBtn").addEventListener("click", async () => {
      await reload();
    });

    document.querySelectorAll("th[data-sort]").forEach((th) => {
      const activate = () => {
        const key = th.dataset.sort;
        if (sortKey === key) {
          sortDir = -sortDir;
        } else {
          sortKey = key;
          sortDir = 1;
        }
        page = 1;
        renderTable();
      };
      th.addEventListener("click", activate);
      th.addEventListener("keydown", (e) => {
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          activate();
        }
      });
    });

    document.addEventListener("keydown", (e) => {
      if (e.key === "/" && !/^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement?.tagName || "")) {
        e.preventDefault();
        $("topSearchInput")?.focus();
      }
    });

    reload();
    initSidebar();
  }

  document.addEventListener("DOMContentLoaded", init);

  // Minimal bridge for the read-only map preview popups.
  window.SolarUI.ui = { showDetails, openModal };
})();
