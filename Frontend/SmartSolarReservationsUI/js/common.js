/* Shared helpers for the Member 3 reservation screens. */
(function () {
  const api = window.SolarUI.api;

  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[c]);
  }

  function dayKey(value) {
    if (!value) return "";
    if (typeof value === "string" && /^\d{4}-\d{2}-\d{2}/.test(value)) return value.slice(0, 10);
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return "";
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  }

  function parseDay(key) {
    const [y, m, d] = dayKey(key).split("-").map(Number);
    return new Date(y, (m || 1) - 1, d || 1);
  }

  function formatLong(key) {
    return parseDay(key).toLocaleDateString(undefined, { weekday: "long", month: "short", day: "numeric", year: "numeric" });
  }

  function formatShort(key) {
    const d = parseDay(key);
    return d.toLocaleDateString(undefined, { weekday: "short", month: "short", day: "numeric", year: "numeric" });
  }

  function user() {
    try {
      return JSON.parse(localStorage.getItem("solarGridUser") || "null");
    } catch {
      return null;
    }
  }

  function userId() {
    return user()?.id || "USR001";
  }

  function statusLabel(status) {
    if (status === "Confirmed") return "Approved";
    return status || "Unknown";
  }

  function statusClass(status) {
    if (status === "Confirmed" || status === "Verified") return "ok";
    if (status === "Cancelled") return "bad";
    if (status === "Completed") return "mute";
    return "wait";
  }

  function startOfBooking(reservation) {
    const base = parseDay(reservation.date || reservation.bookingDate);
    const match = String(reservation.startTime || "").match(/(\d{1,2}):(\d{2})\s*(AM|PM)/i);
    if (!match) return base;
    let hour = Number(match[1]) % 12;
    if (match[3].toUpperCase() === "PM") hour += 12;
    base.setHours(hour, Number(match[2]), 0, 0);
    return base;
  }

  function hoursUntil(reservation) {
    return (startOfBooking(reservation) - Date.now()) / 36e5;
  }

  function canChange(reservation) {
    return reservation.status === "Confirmed" && hoursUntil(reservation) >= 12;
  }

  function paintUser() {
    const person = user();
    const name = person?.fullName || person?.role || "Guest";
    const role = person?.role || "Guest";
    document.querySelectorAll("[data-user-name]").forEach((el) => { el.textContent = name; });
    document.querySelectorAll("[data-user-role]").forEach((el) => { el.textContent = role; });
    const out = document.getElementById("logoutBtn");
    if (out) {
      out.addEventListener("click", () => {
        localStorage.removeItem("solarGridToken");
        localStorage.removeItem("solarGridUser");
        window.location.href = "../index.html";
      });
    }
  }

  window.ResUI = {
    api, esc, dayKey, parseDay, formatLong, formatShort, user, userId,
    statusLabel, statusClass, hoursUntil, canChange, paintUser
  };
  document.addEventListener("DOMContentLoaded", paintUser);
})();
