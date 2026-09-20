/* Camera QR Code Scanner + Demo QR Generator for Field Operations (Member 4) */
(function () {
  let html5QrCode = null;
  let isScanning = false;

  // Web Audio API beep for successful scan feedback
  function playBeep() {
    try {
      const ctx = new (window.AudioContext || window.webkitAudioContext)();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = "sine";
      osc.frequency.setValueAtTime(880, ctx.currentTime); // A5 note
      gain.gain.setValueAtTime(0.15, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.15);
      osc.connect(gain);
      gain.connect(ctx.destination);
      osc.start();
      osc.stop(ctx.currentTime + 0.15);
    } catch {
      /* AudioContext not allowed or unsupported */
    }
  }

  async function startScanner(onScanSuccess) {
    const readerDiv = document.getElementById("qr-reader");
    if (!readerDiv) return;

    if (typeof Html5Qrcode === "undefined") {
      alert("QR Scanner library is loading. Please check your internet connection and retry.");
      return;
    }

    try {
      if (!html5QrCode) {
        html5QrCode = new Html5Qrcode("qr-reader");
      }

      const config = {
        fps: 10,
        qrbox: { width: 220, height: 220 },
        aspectRatio: 1.0
      };

      await html5QrCode.start(
        { facingMode: "environment" },
        config,
        (decodedText) => {
          playBeep();
          stopScanner();
          if (onScanSuccess) {
            onScanSuccess(decodedText.trim());
          }
        },
        (errorMessage) => {
          // Frame-level scan errors are expected while scanning empty camera feeds
        }
      );

      isScanning = true;
      document.getElementById("scannerStatus").textContent = "Camera active. Point at customer's reservation QR code.";
    } catch (err) {
      console.warn("Camera start failed:", err);
      document.getElementById("scannerStatus").innerHTML =
        `<span class="text-danger"><i class="fa-solid fa-triangle-exclamation"></i> Camera access denied or not available. Please use manual code entry.</span>`;
    }
  }

  async function stopScanner() {
    if (html5QrCode && isScanning) {
      try {
        await html5QrCode.stop();
      } catch (err) {
        console.warn("Camera stop error:", err);
      }
      isScanning = false;
    }
  }

  // Generates scannable QR cards inside the Demo QR Code Viewer modal
  function generateDemoQRCodes(reservations) {
    const container = document.getElementById("demoQrContainer");
    if (!container) return;

    container.innerHTML = "";
    if (!reservations || !reservations.length) {
      container.innerHTML = `<p class="text-muted text-center py-3">No reservations available.</p>`;
      return;
    }

    reservations.forEach((r) => {
      const card = document.createElement("div");
      card.className = "col-md-6 col-lg-4";
      const qrId = `qr-box-${r.reservationId}`;

      card.innerHTML = `
        <div class="card h-100 p-3 text-center border">
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="badge bg-dark">${r.reservationId}</span>
            <span class="small text-muted">${r.timeSlot}</span>
          </div>
          <div id="${qrId}" class="d-flex justify-content-center my-2 p-2 bg-white rounded border"></div>
          <h6 class="mb-1 fw-bold">${r.verificationCode}</h6>
          <small class="text-muted d-block mb-2">${r.customerName} • ${r.energyAmount} kWh</small>
          <div class="d-flex gap-1 justify-content-center">
            <button class="btn btn-sm btn-outline-primary" data-copy-code="${r.verificationCode}">
              <i class="fa-solid fa-copy"></i> Copy Code
            </button>
            <button class="btn btn-sm btn-success" data-fill-code="${r.verificationCode}">
              <i class="fa-solid fa-eye"></i> View Details
            </button>
          </div>
        </div>
      `;
      container.appendChild(card);

      // Render actual QR code image using QRCode.js
      if (typeof QRCode !== "undefined") {
        new QRCode(document.getElementById(qrId), {
          text: r.verificationCode,
          width: 128,
          height: 128,
          colorDark: "#111d2d",
          colorLight: "#ffffff",
          correctLevel: QRCode.CorrectLevel.M
        });
      }
    });

    // Delegate copy and test-verify buttons
    container.querySelectorAll("[data-copy-code]").forEach((btn) => {
      btn.addEventListener("click", () => {
        navigator.clipboard.writeText(btn.dataset.copyCode);
        window.SolarUI.ui.toast(`Copied ${btn.dataset.copyCode} to clipboard.`);
      });
    });

    container.querySelectorAll("[data-fill-code]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const modal = bootstrap.Modal.getInstance(document.getElementById("demoQrModal"));
        if (modal) modal.hide();
        window.SolarUI.ui.inspectAndShowDetails(btn.dataset.fillCode);
      });
    });
  }

  window.SolarUI = window.SolarUI || {};
  window.SolarUI.qr = {
    startScanner,
    stopScanner,
    generateDemoQRCodes
  };
})();
