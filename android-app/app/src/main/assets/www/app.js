const defaults = {
  warpDensity: 55,
  warpCount: 14,
  warpPrice: 33000,
  weftDensity: 48,
  weftCount: 16,
  weftPrice: 32000,
  weft2Density: 0,
  weft2Count: 16,
  weft2Price: 33000,
  greyWidth: 108,
  finishedWidth: 250,
  taxFactor: 0.96,
  coefficient: 0.064,
  plainFee: 0.03,
  smallJacquardFee: 0.035,
  largeJacquardFee: 0.05
};

const STORAGE_KEY = "greyFabricCalculator.records.v1";
const fieldNames = Object.keys(defaults);
let latestResult = null;

const fields = {};
const calculator = document.querySelector("#calculator");
const resetBtn = document.querySelector("#resetBtn");
const toggleWeft2 = document.querySelector("#toggleWeft2");
const weft2Row = document.querySelector("#weft2Row");
const fabricName = document.querySelector("#fabricName");
const saveFabricBtn = document.querySelector("#saveFabricBtn");
const savedList = document.querySelector("#savedList");
const saveStatus = document.querySelector("#saveStatus");

document.querySelectorAll("[data-field]").forEach((input) => {
  fields[input.dataset.field] = input;
});

const output = {
  mainPrice: document.querySelector("#mainPrice"),
  plainTaxedTop: document.querySelector("#plainTaxedTop"),
  plainUntaxedTop: document.querySelector("#plainUntaxedTop"),
  smallTaxedTop: document.querySelector("#smallTaxedTop"),
  smallUntaxedTop: document.querySelector("#smallUntaxedTop"),
  largeTaxedTop: document.querySelector("#largeTaxedTop"),
  largeUntaxedTop: document.querySelector("#largeUntaxedTop"),
  greyGsmTop: document.querySelector("#greyGsmTop"),
  finishedGsmTop: document.querySelector("#finishedGsmTop"),
  plainTaxed: document.querySelector("#plainTaxed"),
  plainUntaxed: document.querySelector("#plainUntaxed"),
  smallTaxed: document.querySelector("#smallTaxed"),
  smallUntaxed: document.querySelector("#smallUntaxed"),
  largeTaxed: document.querySelector("#largeTaxed"),
  largeUntaxed: document.querySelector("#largeUntaxed"),
  meterWeight: document.querySelector("#meterWeight"),
  greyGsm: document.querySelector("#greyGsm"),
  finishedGsm: document.querySelector("#finishedGsm")
};

function numberValue(name) {
  const value = Number(fields[name].value);
  return Number.isFinite(value) ? value : 0;
}

function fmt(value, digits = 2) {
  if (!Number.isFinite(value)) return "0.00";
  return value.toFixed(digits);
}

function collectState() {
  return fieldNames.reduce((state, name) => {
    state[name] = numberValue(name);
    return state;
  }, {});
}

function applyState(state) {
  fieldNames.forEach((name) => {
    if (Object.prototype.hasOwnProperty.call(state, name)) {
      fields[name].value = state[name];
    }
  });

  const showWeft2 = numberValue("weft2Density") > 0;
  weft2Row.classList.toggle("visible", showWeft2);
  toggleWeft2.classList.toggle("active", showWeft2);
  calculate();
}

function readRecords() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
  } catch (error) {
    return [];
  }
}

function writeRecords(records) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(records));
}

function setSaveStatus(message) {
  saveStatus.textContent = message;
}

function yarnCost(density, count, width, coefficient, pricePerTon, taxFactor) {
  if (density <= 0 || count <= 0 || width <= 0 || coefficient <= 0 || pricePerTon <= 0) {
    return { weightPart: 0, taxed: 0, untaxed: 0 };
  }

  const weightPart = (density / count) * width * coefficient;
  const calcPrice = pricePerTon / 100000;
  const taxed = weightPart * calcPrice;
  const untaxed = taxed * taxFactor;

  return { weightPart, taxed, untaxed };
}

function machineResult(feeRate, weftDensity, weft2Density, yarnTaxed, yarnUntaxed) {
  const processing = (weftDensity + weft2Density) * feeRate;
  return {
    taxed: yarnTaxed + processing,
    untaxed: yarnUntaxed + processing
  };
}

function calculate() {
  const coefficient = numberValue("coefficient");
  const taxFactor = numberValue("taxFactor");
  const greyWidth = numberValue("greyWidth");
  const finishedWidth = numberValue("finishedWidth");
  const weftDensity = numberValue("weftDensity");
  const weft2Density = numberValue("weft2Density");

  const warp = yarnCost(
    numberValue("warpDensity"),
    numberValue("warpCount"),
    greyWidth,
    coefficient,
    numberValue("warpPrice"),
    taxFactor
  );
  const weft = yarnCost(
    weftDensity,
    numberValue("weftCount"),
    greyWidth,
    coefficient,
    numberValue("weftPrice"),
    taxFactor
  );
  const weft2 = yarnCost(
    weft2Density,
    numberValue("weft2Count"),
    greyWidth,
    coefficient,
    numberValue("weft2Price"),
    taxFactor
  );

  const yarnTaxed = warp.taxed + weft.taxed + weft2.taxed;
  const yarnUntaxed = warp.untaxed + weft.untaxed + weft2.untaxed;
  const meterWeight = (warp.weightPart + weft.weightPart + weft2.weightPart) * 10;
  const greyGsm = greyWidth > 0 ? (meterWeight / (greyWidth * 2.54)) * 100 : 0;
  const finishedGsm = finishedWidth > 0 ? (meterWeight / finishedWidth) * 100 : 0;

  const plain = machineResult(numberValue("plainFee"), weftDensity, weft2Density, yarnTaxed, yarnUntaxed);
  const small = machineResult(numberValue("smallJacquardFee"), weftDensity, weft2Density, yarnTaxed, yarnUntaxed);
  const large = machineResult(numberValue("largeJacquardFee"), weftDensity, weft2Density, yarnTaxed, yarnUntaxed);

  latestResult = {
    plainTaxed: plain.taxed,
    plainUntaxed: plain.untaxed,
    smallTaxed: small.taxed,
    smallUntaxed: small.untaxed,
    largeTaxed: large.taxed,
    largeUntaxed: large.untaxed,
    meterWeight,
    greyGsm,
    finishedGsm
  };

  output.plainTaxedTop.textContent = fmt(plain.taxed, 2);
  output.plainUntaxedTop.textContent = fmt(plain.untaxed, 2);
  output.smallTaxedTop.textContent = fmt(small.taxed, 2);
  output.smallUntaxedTop.textContent = fmt(small.untaxed, 2);
  output.largeTaxedTop.textContent = fmt(large.taxed, 2);
  output.largeUntaxedTop.textContent = fmt(large.untaxed, 2);
  output.greyGsmTop.textContent = fmt(greyGsm, 2);
  output.finishedGsmTop.textContent = fmt(finishedGsm, 2);
  output.plainTaxed.textContent = fmt(plain.taxed, 2);
  output.plainUntaxed.textContent = fmt(plain.untaxed, 2);
  output.smallTaxed.textContent = fmt(small.taxed, 2);
  output.smallUntaxed.textContent = fmt(small.untaxed, 2);
  output.largeTaxed.textContent = fmt(large.taxed, 2);
  output.largeUntaxed.textContent = fmt(large.untaxed, 2);
  output.meterWeight.textContent = fmt(meterWeight, 2);
  output.greyGsm.textContent = fmt(greyGsm, 2);
  output.finishedGsm.textContent = fmt(finishedGsm, 2);
}

function loadDefaults() {
  applyState(defaults);
}

function formatTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function renderSavedList() {
  const records = readRecords();

  if (!records.length) {
    savedList.innerHTML = '<div class="empty-state">还没有保存过的面料。</div>';
    return;
  }

  savedList.innerHTML = records.map((record) => `
    <article class="saved-item">
      <div>
        <strong>${escapeHtml(record.name)}</strong>
        <span>${formatTime(record.updatedAt)} · 平机含税 ${fmt(record.result?.plainTaxed || 0, 2)} · ${fmt(record.result?.finishedGsm || 0, 2)}g/m²</span>
      </div>
      <div class="saved-actions">
        <button type="button" data-action="load" data-id="${record.id}">调用</button>
        <button type="button" data-action="delete" data-id="${record.id}">删除</button>
      </div>
    </article>
  `).join("");
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function saveCurrentFabric() {
  const name = fabricName.value.trim();
  if (!name) {
    setSaveStatus("先给这个面料起个名字，再保存。");
    fabricName.focus();
    return;
  }

  const now = new Date().toISOString();
  const records = readRecords();
  const existingIndex = records.findIndex((record) => record.name === name);
  const nextRecord = {
    id: existingIndex >= 0 ? records[existingIndex].id : `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    name,
    state: collectState(),
    result: latestResult,
    updatedAt: now
  };

  if (existingIndex >= 0) {
    records.splice(existingIndex, 1, nextRecord);
  } else {
    records.unshift(nextRecord);
  }

  writeRecords(records);
  renderSavedList();
  setSaveStatus(`已保存：${name}`);
}

function loadRecord(id) {
  const record = readRecords().find((item) => item.id === id);
  if (!record) return;

  fabricName.value = record.name;
  applyState(record.state);
  setSaveStatus(`已调用：${record.name}`);
}

function deleteRecord(id) {
  const records = readRecords();
  const record = records.find((item) => item.id === id);
  if (!record) return;

  if (!confirm(`删除“${record.name}”？`)) return;

  writeRecords(records.filter((item) => item.id !== id));
  renderSavedList();
  setSaveStatus(`已删除：${record.name}`);
}

calculator.addEventListener("input", calculate);

resetBtn.addEventListener("click", () => {
  loadDefaults();
});

toggleWeft2.addEventListener("click", () => {
  const visible = weft2Row.classList.toggle("visible");
  toggleWeft2.classList.toggle("active", visible);
});

saveFabricBtn.addEventListener("click", saveCurrentFabric);

savedList.addEventListener("click", (event) => {
  const button = event.target.closest("button[data-action]");
  if (!button) return;

  if (button.dataset.action === "load") {
    loadRecord(button.dataset.id);
  }

  if (button.dataset.action === "delete") {
    deleteRecord(button.dataset.id);
  }
});

loadDefaults();
renderSavedList();

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("./sw.js").catch(() => {});
  });
}
