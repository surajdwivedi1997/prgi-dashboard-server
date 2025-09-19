// ✅ Auto-detect API base URL
const API =
  window.location.hostname === "localhost"
    ? "http://localhost:8080/api/applications"
    : "/api/applications"; // relative for deployed server

const ModuleLabels = {
  NEW_REG: "New Reg",
  NEW_EDITION: "New Edition",
  REVISED_REGISTRATION: "Revised Registration",
  OWNERSHIP: "Ownership",
  DISCONTINUATION_OF_PUBLICATION: "Discontinuation of Publication",
  NEWSPRINT_DECLARATION_AUTHENTICATION: "Newsprint Declaration Authentication",
};
const ModuleColors = {
  NEW_REG: "#FFE8CC",
  NEW_EDITION: "#D0EBFF",
  REVISED_REGISTRATION: "#E9FAC8",
  OWNERSHIP: "#FFD8E8",
  DISCONTINUATION_OF_PUBLICATION: "#FFF3BF",
  NEWSPRINT_DECLARATION_AUTHENTICATION: "#E5DBFF",
};
const StatusLabels = {
  NEW_APPLICATION: "New Application(s)",
  APPLICATION_RECEIVED_FROM_SA: "Application Received from SA",
  DEFICIENT_AWAITING_PUBLISHER: "Deficient – Awaiting Publisher",
  UNDER_PROCESS_AT_PRGI: "Under Process at PRGI",
  APPLICATION_REJECTED: "Application Rejected",
  REGISTRATION_GRANTED: "Registration Granted",
};
const StatusOrder = [
  "NEW_APPLICATION",
  "APPLICATION_RECEIVED_FROM_SA",
  "DEFICIENT_AWAITING_PUBLISHER",
  "UNDER_PROCESS_AT_PRGI",
  "APPLICATION_REJECTED",
  "REGISTRATION_GRANTED",
];
const ModuleOrder = Object.keys(ModuleLabels);

let selectedFrom = null;
let selectedTo = null;

document.getElementById("btnApply").addEventListener("click", () => {
  const val = document.getElementById("rangeSelect").value;
  if (!val) {
    alert("Please select a date range!");
    return;
  }
  const [from, to] = val.split("|");
  selectedFrom = from;
  selectedTo = to;
  loadSummary();
});

// ✅ Download Excel button
document
  .getElementById("btnDownloadExcel")
  .addEventListener("click", async () => {
    if (!selectedFrom || !selectedTo) {
      alert("Please select a date range first!");
      return;
    }

    const url = `${API}/export?fromDate=${selectedFrom}&toDate=${selectedTo}`;
    const res = await fetch(url);

    if (!res.ok) {
      alert("Failed to download Excel!");
      return;
    }

    const blob = await res.blob();
    const link = document.createElement("a");
    link.href = window.URL.createObjectURL(blob);
    link.download = `applications_${selectedFrom}_to_${selectedTo}.xlsx`;
    document.body.appendChild(link);
    link.click();
    link.remove();
  });

function buildShell() {
  const container = document.getElementById("modules");
  container.innerHTML = "";
  ModuleOrder.forEach((m) => {
    const section = document.createElement("section");
    section.className = "module";
    section.innerHTML = `
      <h2 style="background:${ModuleColors[m]}">${ModuleLabels[m]}</h2>
      <div class="grid" id="grid-${m}"></div>
    `;
    container.appendChild(section);
    const grid = section.querySelector(".grid");
    StatusOrder.forEach((s) => {
      const id = `${m}_${s}`;
      const card = document.createElement("div");
      card.className = `card ${s}`;
      card.id = `card-${id}`;
      card.innerHTML = `
        <div class="status">${StatusLabels[s]}</div>
        <div class="count" id="count-${id}">0</div>
      `;
      card.addEventListener("click", () => openDetails(m, s));
      grid.appendChild(card);
    });
  });
}

async function loadSummary() {
  if (!selectedFrom || !selectedTo) return;
  document.getElementById(
    "info"
  ).textContent = `Showing data from ${selectedFrom} to ${selectedTo}`;

  try {
    const res = await fetch(
      `${API}/summary?fromDate=${selectedFrom}&toDate=${selectedTo}`
    );
    if (!res.ok) throw new Error("Failed to fetch summary");
    const json = await res.json();
    ModuleOrder.forEach((m) => {
      StatusOrder.forEach((s) => {
        const cnt = json?.[m]?.[s] ?? 0;
        document.getElementById(`count-${m}_${s}`).textContent = cnt;
      });
    });
  } catch (e) {
    console.error("Error loading summary:", e);
  }
}

async function openDetails(moduleKey, statusKey) {
  if (!selectedFrom || !selectedTo) {
    alert("Please select a range first!");
    return;
  }
  try {
    const url = `${API}?module=${moduleKey}&status=${statusKey}&fromDate=${selectedFrom}&toDate=${selectedTo}`;
    const res = await fetch(url);
    if (!res.ok) throw new Error("Failed to fetch details");
    const rows = await res.json();
    document.getElementById(
      "modalTitle"
    ).textContent = `${ModuleLabels[moduleKey]} → ${StatusLabels[statusKey]}`;
    document.getElementById(
      "modalSub"
    ).textContent = `${rows.length} record(s)`;
    const tbody = document.getElementById("modalBody");
    tbody.innerHTML = rows
      .map(
        (r, i) => `
    <tr>
      <td>${i + 1}</td>
      <td>${escapeHtml(r.applicantName)}</td>
      <td>${escapeHtml(r.referenceNo)}</td>
      <td>${r.submittedDate}</td>
      <td>${escapeHtml(r.remarks || "")}</td>
    </tr>
  `
      )
      .join("");
    showModal();
  } catch (e) {
    console.error("Error loading details:", e);
  }
}

function escapeHtml(s) {
  return (s || "").replace(/[&<>"']/g, (m) =>
    ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
    }[m])
  );
}
function showModal() {
  document.getElementById("backdrop").style.display = "flex";
}
function hideModal() {
  document.getElementById("backdrop").style.display = "none";
}

// init
buildShell();
