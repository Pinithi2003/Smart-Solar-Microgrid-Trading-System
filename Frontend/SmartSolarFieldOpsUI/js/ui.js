/* Field Operations UI controller (Member 4)
   Handles operations dashboard, verification workflows, completion modals, and toasts. */
(function () {
  const api = window.SolarUI.fieldApi;
  const qr = window.SolarUI.qr;
  const nearbyMap = window.SolarUI.map;

  let operations = [];
  let stations = [];
  let currentStatusFilter = "All";
  let stationFilter = null;
  let completingReservationId = null;

  const $ = (id) => document.getElementById(id);

  function esc(v) {
    return String(v ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
  }

  function toast(message, type = "success") {
    const wrap = $("toastContainer");
    if (!wrap) return;
    const el = document.createElement("div");
    el.className = `toast align-items-center text-bg-${type === "success" ? "success" : type === "warning" ? "warning" : "danger"} border-0`;
    el.setAttribute("role", "alert");
    el.innerHTML = `
      <div class="d-flex">
        <div class="toast-body fw-medium">${esc(message)}</div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
      </div>`;
    wrap.appendChild(el);
    bootstrap.Toast.getOrCreateInstance(el, { delay: 3500 }).show();
    el.addEventListener("hidden.bs.toast", () => el.remove());
  }

  /* ---------------- Count-up Stats ---------------- */
  function countUp(el, to) {
    if (!el) return;
    const from = Number(el.dataset.value || 0);
    el.dataset.value = to;
    el.textContent = to;
  }

  function renderStats() {
    const confirmed = operations.filter((o) => o.status === "Confirmed").length;
    const verified = operations.filter((o) => o.status === "Verified").length;
    const completed = operations.filter((o) => o.status === "Completed").length;
    const totalEnergy = operations
      .filter((o) => o.status === "Verified" || o.status === "Completed")
      .reduce((sum, o) => sum + Number(o.energyAmount || 0), 0);

    countUp($("statPending"), confirmed);
    countUp($("statVerified"), verified);
    countUp($("statCompleted"), completed);
    countUp($("statEnergy"), totalEnergy.toFixed(1));
  }

  /* ---------------- Filter & Render Operations Table ---------------- */
  function getFilteredOperations() {
    const q = ($("topSearchInput")?.value || "").toLowerCase().trim();
    return operations.filter((o) => {
      const matchStatus = currentStatusFilter === "All" || o.status === currentStatusFilter;
      const matchStation = !stationFilter || o.stationId === stationFilter;
      const text = `${o.reservationId} ${o.verificationCode} ${o.stationName} ${o.customerName} ${o.status}`.toLowerCase();
      const matchQuery = !q || text.includes(q);
      return matchStatus && matchStation && matchQuery;
    });
  }

  function statusBadge(status) {
    if (status === "Confirmed") return `<span class="badge-soft-confirmed"><i class="fa-solid fa-clock"></i> Confirmed</span>`;
    if (status === "Verified") return `<span class="badge-soft-verified"><i class="fa-solid fa-circle-check"></i> Verified</span>`;
    if (status === "Completed") return `<span class="badge-soft-completed"><i class="fa-solid fa-circle-check"></i> Completed</span>`;
    if (status === "Cancelled") return `<span class="badge-soft-cancelled"><i class="fa-solid fa-ban"></i> Cancelled</span>`;
    return `<span class="badge bg-secondary">${esc(status)}</span>`;
  }

  function renderTable() {
    const tbody = $("operationsBody");
    if (!tbody) return;

    const list = getFilteredOperations();

    if (!list.length) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">No field operations match your filter.</td></tr>`;
      return;
    }

    tbody.innerHTML = list.map((o) => {
      let actionBtn = "";
      if (o.status === "Confirmed") {
        actionBtn = `<button class="btn btn-sm btn-action-verify" data-verify-code="${esc(o.verificationCode)}"><i class="fa-solid fa-qrcode"></i> Verify</button>`;
      } else if (o.status === "Verified") {
        actionBtn = `<button class="btn btn-sm btn-action-complete" data-complete-id="${esc(o.reservationId)}"><i class="fa-solid fa-bolt"></i> Complete</button>`;
      } else {
        actionBtn = `<button class="btn btn-sm btn-action-view" data-details-id="${esc(o.reservationId)}"><i class="fa-solid fa-eye"></i> Details</button>`;
      }

      return `
        <tr>
          <td><strong>${esc(o.reservationId)}</strong></td>
          <td>
            <div class="fw-semibold">${esc(o.stationName)}</div>
            <small class="text-muted">${esc(o.stationId)}</small>
          </td>
          <td>
            <div>${esc(o.customerName)}</div>
            <small class="text-muted">${esc(o.userId)}</small>
          </td>
          <td><code>${esc(o.verificationCode)}</code></td>
          <td class="text-center"><strong>${esc(o.energyAmount)}</strong> <small class="text-muted">kWh</small></td>
          <td><small>${esc(o.timeSlot)}</small></td>
          <td>${statusBadge(o.status)}</td>
          <td class="text-end text-nowrap">${actionBtn}</td>
        </tr>
      `;
    }).join("");

    // Attach row button events
    tbody.querySelectorAll("[data-verify-code]").forEach((btn) => {
      btn.addEventListener("click", () => inspectAndShowDetails(btn.dataset.verifyCode));
    });

    tbody.querySelectorAll("[data-complete-id]").forEach((btn) => {
      btn.addEventListener("click", () => openCompleteModal(btn.dataset.completeId));
    });

    tbody.querySelectorAll("[data-details-id]").forEach((btn) => {
      btn.addEventListener("click", () => inspectAndShowDetails(btn.dataset.detailsId));
    });
  }

  /* ---------------- Step 1: Inspect Code & Show Details First ---------------- */
  async function inspectAndShowDetails(codeOrId) {
    if (!codeOrId || !codeOrId.trim()) {
      toast("Please enter or scan a reservation or verification code.", "warning");
      return;
    }

    const q = codeOrId.trim().toLowerCase();
    let op = operations.find(
      (o) => (o.verificationCode && o.verificationCode.toLowerCase() === q) ||
             (o.reservationId && o.reservationId.toLowerCase() === q)
    );

    if (!op) {
      try {
        op = await api.getOne(codeOrId.trim());
      } catch {
        toast(`Invalid code: No reservation found for "${codeOrId.trim()}".`, "danger");
        return;
      }
    }

    if (!op) {
      toast(`Invalid code: No reservation found for "${codeOrId.trim()}".`, "danger");
      return;
    }

    showInspectionModal(op);
  }

  /* ---------------- Display Inspection Modal with Action Buttons ---------------- */
  function showInspectionModal(op) {
    $("detailsModalTitle").innerHTML = `<i class="fa-solid fa-id-card text-primary me-2"></i>Reservation Details: ${esc(op.reservationId)}`;

    let statusAlert = "";
    let actionBtnHtml = "";

    if (op.status === "Confirmed") {
      statusAlert = `
        <div class="alert alert-warning py-2 small mb-3">
          <i class="fa-solid fa-clock me-1"></i> <strong>Awaiting Field Verification.</strong> Review customer booking details below before authorizing energy transfer.
        </div>`;
      actionBtnHtml = `
        <button type="button" id="confirmVerifyModalBtn" class="btn btn-success">
          <i class="fa-solid fa-circle-check"></i> Confirm & Verify Reservation
        </button>`;
    } else if (op.status === "Verified") {
      statusAlert = `
        <div class="alert alert-info py-2 small mb-3">
          <i class="fa-solid fa-circle-check me-1"></i> <strong>Reservation Already Verified</strong> at ${op.verifiedAt ? new Date(op.verifiedAt).toLocaleTimeString() : ""}. Energy transfer currently authorized.
        </div>`;
      actionBtnHtml = `
        <button type="button" id="openCompleteFromModalBtn" class="btn btn-primary">
          <i class="fa-solid fa-bolt"></i> Complete Field Operation
        </button>`;
    } else if (op.status === "Completed") {
      statusAlert = `
        <div class="alert alert-success py-2 small mb-3">
          <i class="fa-solid fa-check-double me-1"></i> <strong>Operation Completed.</strong> Energy has been fully discharged.
        </div>`;
    } else if (op.status === "Cancelled") {
      statusAlert = `
        <div class="alert alert-danger py-2 small mb-3">
          <i class="fa-solid fa-ban me-1"></i> <strong>Booking Cancelled.</strong> This reservation cannot be verified or used.
        </div>`;
    }

    const verifiedTime = op.verifiedAt ? new Date(op.verifiedAt).toLocaleString() : "Not verified yet";
    const completedTime = op.completedAt ? new Date(op.completedAt).toLocaleString() : "Not completed yet";

    $("detailsModalBody").innerHTML = `
      ${statusAlert}
      <div class="d-flex justify-content-between align-items-center p-2 mb-3 bg-light rounded border">
        <div>
          <span class="text-muted small d-block">Verification Code</span>
          <code class="fs-6 fw-bold text-dark">${esc(op.verificationCode)}</code>
        </div>
        <div>${statusBadge(op.status)}</div>
      </div>
      <dl class="row mb-0">
        <dt class="col-sm-4 text-muted">Reservation ID</dt><dd class="col-sm-8 fw-semibold">${esc(op.reservationId)}</dd>
        <dt class="col-sm-4 text-muted">Solar Station</dt><dd class="col-sm-8 fw-semibold">${esc(op.stationName)} <small class="text-muted">(${esc(op.stationId)})</small></dd>
        <dt class="col-sm-4 text-muted">Customer</dt><dd class="col-sm-8">${esc(op.customerName)} <small class="text-muted">(${esc(op.userId)})</small></dd>
        <dt class="col-sm-4 text-muted">Energy Amount</dt><dd class="col-sm-8"><span class="badge bg-success fs-6">${esc(op.energyAmount)} kWh</span></dd>
        <dt class="col-sm-4 text-muted">Time Slot</dt><dd class="col-sm-8">${esc(op.timeSlot)}</dd>
        <dt class="col-sm-4 text-muted">Field Operator</dt><dd class="col-sm-8">${esc(op.operatorId)}</dd>
        <dt class="col-sm-4 text-muted">Verified At</dt><dd class="col-sm-8">${esc(verifiedTime)}</dd>
        <dt class="col-sm-4 text-muted">Completed At</dt><dd class="col-sm-8">${esc(completedTime)}</dd>
        ${op.notes ? `<dt class="col-sm-4 text-muted">Field Notes</dt><dd class="col-sm-8 fst-italic">${esc(op.notes)}</dd>` : ""}
      </dl>
    `;

    $("detailsModalFooter").innerHTML = `
      <button type="button" class="btn btn-secondary me-auto" data-bs-dismiss="modal">Close</button>
      ${actionBtnHtml}
    `;

    // Hook up verify confirm button
    const verifyBtn = $("confirmVerifyModalBtn");
    if (verifyBtn) {
      verifyBtn.addEventListener("click", () => executeVerify(op.verificationCode));
    }

    // Hook up complete operation button
    const completeBtn = $("openCompleteFromModalBtn");
    if (completeBtn) {
      completeBtn.addEventListener("click", () => {
        bootstrap.Modal.getInstance($("detailsModal")).hide();
        openCompleteModal(op.reservationId);
      });
    }

    bootstrap.Modal.getOrCreateInstance($("detailsModal")).show();
  }

  /* ---------------- Step 2: Confirm & Execute Verification ---------------- */
  async function executeVerify(code) {
    const btn = $("confirmVerifyModalBtn");
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Verifying…`;
    }

    try {
      const res = await api.verify(code.trim());
      toast(res.message, "success");
      await loadOperations();
      // Update the open inspection modal to reflect the newly Verified status
      if (res.operation) {
        showInspectionModal(res.operation);
      }
    } catch (err) {
      toast(err.message, "danger");
      if (btn) {
        btn.disabled = false;
        btn.innerHTML = `<i class="fa-solid fa-circle-check"></i> Confirm & Verify Reservation`;
      }
    }
  }

  /* ---------------- Complete Operation Workflow ---------------- */
  function openCompleteModal(reservationId) {
    completingReservationId = reservationId;
    const op = operations.find((o) => o.reservationId === reservationId);
    if (!op) return;

    $("completeResId").textContent = op.reservationId;
    $("completeStation").textContent = `${op.stationName} (${op.stationId})`;
    $("completeCustomer").textContent = `${op.customerName} (${op.userId})`;
    $("completeEnergy").textContent = `${op.energyAmount} kWh`;
    $("completeNotes").value = `Energy transfer of ${op.energyAmount} kWh completed successfully at station ${op.stationId}.`;

    bootstrap.Modal.getOrCreateInstance($("completeModal")).show();
  }

  async function submitComplete() {
    if (!completingReservationId) return;
    const notes = $("completeNotes").value.trim();
    const btn = $("confirmCompleteBtn");
    btn.disabled = true;

    try {
      const res = await api.complete(completingReservationId, notes);
      bootstrap.Modal.getInstance($("completeModal")).hide();
      toast(res.message, "success");
      await loadOperations();
    } catch (err) {
      toast(err.message, "danger");
    } finally {
      btn.disabled = false;
    }
  }

  /* ---------------- Data Fetching ---------------- */
  async function loadOperations() {
    try {
      operations = await api.getAll();
      renderStats();
      renderTable();
    } catch (err) {
      console.error("Failed to load operations:", err);
      toast("Could not connect to Field Operations API.", "danger");
    }
  }

  async function loadStations() {
    try {
      stations = await api.getStations();
      nearbyMap.renderStations(stations);
    } catch (err) {
      console.warn("Could not load stations from Member 2 API:", err);
    }
  }

  function filterByStation(stationId) {
    stationFilter = stationId;
    switchTab("operations");
    renderTable();
    toast(`Filtered operations for Station ${stationId}.`);
  }

  function switchTab(tabName) {
    document.querySelectorAll(".module-tab-btn").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.tab === tabName);
    });

    if (tabName === "operations") {
      $("tabOperationsView").classList.remove("d-none");
      $("tabNearbyView").classList.add("d-none");
    } else {
      $("tabOperationsView").classList.add("d-none");
      $("tabNearbyView").classList.remove("d-none");
      nearbyMap.invalidateSize();
    }
  }

  /* ---------------- Init ---------------- */
  function init() {
    // Quick verification form
    $("quickVerifyForm")?.addEventListener("submit", (e) => {
      e.preventDefault();
      const code = $("quickVerifyInput").value;
      inspectAndShowDetails(code);
      $("quickVerifyInput").value = "";
    });

    // Camera scanner modal trigger
    $("openScannerBtn")?.addEventListener("click", () => {
      const modal = bootstrap.Modal.getOrCreateInstance($("scannerModal"));
      modal.show();
    });

    // Scanner modal show/hide events
    $("scannerModal")?.addEventListener("shown.bs.modal", () => {
      qr.startScanner((decodedCode) => {
        bootstrap.Modal.getInstance($("scannerModal")).hide();
        inspectAndShowDetails(decodedCode);
      });
    });

    $("scannerModal")?.addEventListener("hidden.bs.modal", () => {
      qr.stopScanner();
    });

    // Demo QR generator modal trigger
    $("openDemoQrBtn")?.addEventListener("click", () => {
      qr.generateDemoQRCodes(operations);
      bootstrap.Modal.getOrCreateInstance($("demoQrModal")).show();
    });

    // Complete operation submit
    $("confirmCompleteBtn")?.addEventListener("click", submitComplete);

    // Filter by status dropdown
    document.querySelectorAll("[data-status-filter]").forEach((item) => {
      item.addEventListener("click", () => {
        currentStatusFilter = item.dataset.statusFilter;
        $("statusFilterLabel").textContent = item.textContent.trim();
        renderTable();
      });
    });

    // Search bar
    $("topSearchInput")?.addEventListener("input", renderTable);

    // Refresh button
    $("refreshBtn")?.addEventListener("click", async () => {
      stationFilter = null;
      await loadOperations();
      await loadStations();
      toast("Data refreshed.");
    });

    // Tab buttons
    document.querySelectorAll(".module-tab-btn").forEach((btn) => {
      btn.addEventListener("click", () => switchTab(btn.dataset.tab));
    });

    // Mobile sidebar toggle
    $("sidebarToggle")?.addEventListener("click", () => {
      $("sidebar")?.classList.toggle("open");
      $("sidebarBackdrop")?.classList.toggle("d-none");
    });

    $("sidebarBackdrop")?.addEventListener("click", () => {
      $("sidebar")?.classList.remove("open");
      $("sidebarBackdrop")?.classList.add("d-none");
    });

    // Sidebar navigation mock triggers
    document.querySelectorAll(".sidebar-nav .nav-item[data-module]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const mod = btn.dataset.module;
        if (mod === "stations") {
          window.location.href = "../SmartSolarStationUI/index.html";
          return;
        }
        if (mod === "reservations") {
          window.location.href = "../SmartSolarReservationsUI/index.html";
          return;
        }
        if (mod === "dashboard" || mod === "users") {
          window.location.href = "../SmartSolarUsersUI/index.html";
          return;
        }
        if (mod === "qr") {
          return;
        }
        const label = btn.querySelector("span")?.textContent || mod;
        toast(`${label} belongs to ${btn.dataset.owner || "team"} — coming soon.`, "warning");
      });
    });

    // Initial load
    nearbyMap.init();
    loadOperations();
    loadStations();

    // Auto-refresh whenever the browser tab is focused or every 10 seconds
    window.addEventListener("focus", () => {
      loadOperations();
      loadStations();
    });

    setInterval(() => {
      loadOperations();
      loadStations();
    }, 10000);
  }

  document.addEventListener("DOMContentLoaded", init);

  window.SolarUI = window.SolarUI || {};
  window.SolarUI.ui = {
    toast,
    inspectAndShowDetails,
    verifyCode: inspectAndShowDetails,
    filterByStation
  };
})();
