"use strict";

const API = "/api/mobile/v1";
let accessToken = null;
let currentTab = "count";
let scanStream = null;
let scanTimer = null;
let scanTarget = null;
let scannerGeneration = 0;

const loginView = document.querySelector("#loginView");
const appView = document.querySelector("#appView");
const taskList = document.querySelector("#taskList");
const appMessage = document.querySelector("#appMessage");
const loginMessage = document.querySelector("#loginMessage");
const scannerDialog = document.querySelector("#scannerDialog");
const scannerVideo = document.querySelector("#scannerVideo");
const scannerMessage = document.querySelector("#scannerMessage");

document.querySelector("#loginForm").addEventListener("submit", login);
document.querySelector("#logoutButton").addEventListener("click", logout);
document.querySelector("#refreshButton").addEventListener("click", loadCurrentTab);
document.querySelectorAll("[data-tab]").forEach(button => button.addEventListener("click", () => {
  currentTab = button.dataset.tab;
  document.querySelectorAll("[data-tab]").forEach(tab => tab.setAttribute("aria-pressed", String(tab === button)));
  loadCurrentTab();
}));
document.querySelector("#closeScanner").addEventListener("click", closeScanner);
scannerDialog.addEventListener("close", stopCamera);

function message(target, text, kind = "") {
  target.textContent = text;
  target.className = `notice${kind ? ` ${kind}` : ""}`;
}

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("Accept", "application/json");
  headers.set("Cache-Control", "no-store");
  if (options.body) headers.set("Content-Type", "application/json");
  if (accessToken) headers.set("Authorization", `Bearer ${accessToken}`);
  const response = await fetch(`${API}${path}`, { ...options, headers, cache: "no-store" });
  if (response.status === 401 && accessToken) {
    accessToken = null;
    showLogin();
    throw new Error("Sesi berakhir. Silakan masuk kembali.");
  }
  if (!response.ok) {
    let detail = "Permintaan gagal. Periksa koneksi dan coba lagi.";
    try {
      const body = await response.json();
      detail = body.error?.message || detail;
    } catch { /* Response may not contain JSON. */ }
    throw new Error(detail);
  }
  if (response.status === 204) return null;
  return response.json();
}

async function login(event) {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  const button = event.currentTarget.querySelector("button[type=submit]");
  button.disabled = true;
  message(loginMessage, "Memeriksa akun...");
  try {
    const session = await request("/auth/sessions", {
      method: "POST",
      body: JSON.stringify({ email: form.get("email"), password: form.get("password") })
    });
    accessToken = session.accessToken;
    event.currentTarget.reset();
    document.querySelector("#greeting").textContent = `Halo, ${session.user.fullName}`;
    loginView.hidden = true;
    appView.hidden = false;
    message(loginMessage, "");
    await loadCurrentTab();
  } catch (error) {
    message(loginMessage, error.message, "error");
  } finally {
    button.disabled = false;
  }
}

async function logout() {
  try {
    if (accessToken) await request("/auth/sessions/current", { method: "DELETE" });
  } catch (error) {
    message(appMessage, error.message, "error");
  } finally {
    accessToken = null;
    showLogin();
  }
}

function showLogin() {
  stopCamera();
  appView.hidden = true;
  loginView.hidden = false;
  taskList.replaceChildren();
  document.querySelector("#password").value = "";
}

async function loadCurrentTab() {
  const names = {
    count: ["Hitung stok", "Pilih sesi opname yang tersedia."],
    putaway: ["Simpan barang", "Pilih receipt dan lokasi rak yang disarankan."],
    picking: ["Ambil pesanan", "Tugas yang diberikan kepada akun ini." ]
  };
  document.querySelector("#taskTitle").textContent = names[currentTab][0];
  document.querySelector("#taskHint").textContent = names[currentTab][1];
  taskList.replaceChildren();
  message(appMessage, "Memuat tugas...");
  try {
    if (currentTab === "count") await loadCounts();
    else if (currentTab === "putaway") await loadPutaway();
    else await loadPicking();
    message(appMessage, "");
  } catch (error) {
    message(appMessage, error.message, "error");
    showEmpty("Tugas tidak dapat dimuat.");
  }
}

async function loadPages(path, size = 100) {
  const first = await request(`${path}?page=0&size=${size}`);
  const rows = [...first.data];
  for (let page = 1; page < first.pagination.totalPages; page++) {
    const next = await request(`${path}?page=${page}&size=${size}`);
    rows.push(...next.data);
  }
  return rows;
}

async function loadCounts() {
  const counts = await loadPages("/stock-counts", 100);
  if (!counts.length) return showEmpty("Tidak ada sesi opname aktif.");
  counts.forEach(count => {
    const card = taskCard(count.countCode, `${count.rackCode} · ${count.cycleType || "Opname"}`,
      `${count.status}${count.dueDate ? ` · jatuh tempo ${count.dueDate}` : ""}`);
    const action = button(count.status === "SCHEDULED" ? "Mulai opname" : "Buka hitungan", async () => {
      try {
        if (count.status === "SCHEDULED") await request(`/stock-counts/${count.id}/start`, { method: "POST" });
        await openCount(count.id, count.countCode, count.rackCode);
      } catch (error) { message(appMessage, error.message, "error"); }
    });
    card.append(action);
    taskList.append(card);
  });
}

async function openCount(id, code, rack) {
  currentTab = "count";
  taskList.replaceChildren();
  document.querySelector("#taskTitle").textContent = code;
  document.querySelector("#taskHint").textContent = `${rack} · input jumlah fisik. Hasil sebelumnya tidak ditampilkan.`;
  const lines = await loadPages(`/stock-counts/${id}/lines`, 100);
  if (!lines.length) {
    const card = workflowCard();
    card.append(text("p", "Semua baris sesi ini sudah dikirim."));
    card.append(button("Kembali ke daftar", loadCounts, "outline"));
    taskList.append(card);
    return;
  }
  const card = workflowCard();
  const fields = [];
  lines.forEach(line => {
    const row = element("div", "line-row");
    const description = element("div");
    description.append(text("p", line.itemCode, "line-title"), text("p", line.itemName));
    if (line.assignedTo) description.append(text("p", `Petugas: ${line.assignedTo}`));
    const entry = element("div");
    const qty = input("Jumlah fisik", "number", { min: "0", step: "1", inputmode: "numeric" });
    const barcode = input("QR barang (opsional)", "text", { autocomplete: "off", maxlength: "512" });
    const scan = button("Pindai", () => openScanner(barcode), "outline");
    const barcodeRow = fieldWithButton(barcode, scan);
    entry.append(qty, barcodeRow);
    row.append(description, entry);
    card.append(row);
    fields.push({ line, qty, barcode });
  });
  card.append(button("Kirim hitungan terisi", async () => {
    const counts = fields.filter(row => row.qty.value !== "").map(({ line, qty, barcode }) => ({
      lineId: line.id,
      quantity: Number(qty.value),
      ...(barcode.value.trim() ? { barcodePayload: barcode.value.trim() } : {})
    }));
    if (!counts.length || counts.some(row => !Number.isSafeInteger(row.quantity) || row.quantity < 0)) {
      return message(appMessage, "Isi jumlah bulat nol atau lebih pada sedikitnya satu baris.", "error");
    }
    try {
      const result = await request(`/stock-counts/${id}/lines`, { method: "PUT", body: JSON.stringify({ counts }) });
      message(appMessage, result.status === "COUNTED" ? "Opname selesai dikirim." : "Hitungan tersimpan.", "success");
      await openCount(id, code, rack);
    } catch (error) { message(appMessage, error.message, "error"); }
  }));
  card.append(button("Kembali ke daftar", loadCounts, "outline"));
  taskList.append(card);
}

async function loadPutaway() {
  const page = await request("/putaway/receipts?page=0&size=100");
  if (!page.data.length) return showEmpty("Tidak ada barang menunggu putaway.");
  page.data.forEach(receipt => {
    const card = taskCard(receipt.receiptCode, `${receipt.itemCode} · ${receipt.itemName}`,
      `${receipt.quantity} unit · batch ${receipt.batchCode}${receipt.expiryDate ? ` · kedaluwarsa ${receipt.expiryDate}` : ""}`);
    card.append(button("Pilih lokasi", () => openPutaway(receipt)));
    taskList.append(card);
  });
}

async function openPutaway(receipt) {
  taskList.replaceChildren();
  document.querySelector("#taskTitle").textContent = receipt.receiptCode;
  document.querySelector("#taskHint").textContent = `${receipt.itemCode} · ${receipt.itemName} · ${receipt.quantity} unit`;
  const suggestions = await request(`/putaway/receipts/${receipt.id}/suggestions`);
  const card = workflowCard();
  card.append(text("h3", "Pilih rak tujuan"));
  const choices = element("div", "recommendations");
  let selectedRack = null;
  if (!suggestions.length) card.append(text("p", "Belum ada rak yang memenuhi rekomendasi. Periksa kapasitas dan aturan penyimpanan."));
  suggestions.forEach((suggestion, index) => {
    const row = element("div", "recommendation");
    const radioId = `rack-${receipt.id}-${suggestion.rackId}`;
    const radio = document.createElement("input");
    radio.type = "radio";
    radio.name = "rack";
    radio.value = String(suggestion.rackId);
    radio.id = radioId;
    radio.checked = index === 0;
    radio.addEventListener("change", () => { selectedRack = suggestion.rackId; });
    if (index === 0) selectedRack = suggestion.rackId;
    const label = document.createElement("label");
    label.htmlFor = radioId;
    label.append(document.createTextNode(`${suggestion.rackCode} · kapasitas ${suggestion.availableCapacity}`));
    const reasons = [suggestion.categoryMatch && "kategori sesuai", suggestion.storageTypeMatch && "tipe simpan sesuai"]
      .filter(Boolean).join(" · ");
    if (reasons) label.append(text("small", reasons));
    row.append(radio, label);
    choices.append(row);
  });
  card.append(choices);
  const itemBarcode = input("Pindai QR barang", "text", { autocomplete: "off", maxlength: "512", required: "" });
  const locationBarcode = input("Pindai barcode lokasi SLOC", "text", { autocomplete: "off", maxlength: "512", required: "" });
  card.append(fieldWithButton(itemBarcode, button("Kamera", () => openScanner(itemBarcode), "outline")));
  card.append(fieldWithButton(locationBarcode, button("Kamera", () => openScanner(locationBarcode), "outline")));
  card.append(button("Selesaikan putaway", async () => {
    if (!selectedRack || !itemBarcode.value.trim() || !locationBarcode.value.trim()) {
      return message(appMessage, "Pilih rak dan isi kedua hasil scan.", "error");
    }
    try {
      await request(`/putaway/receipts/${receipt.id}/complete`, { method: "POST", body: JSON.stringify({
        rackId: selectedRack, itemBarcode: itemBarcode.value.trim(), locationBarcode: locationBarcode.value.trim()
      }) });
      message(appMessage, "Putaway berhasil dicatat.", "success");
      await loadPutaway();
    } catch (error) { message(appMessage, error.message, "error"); }
  }));
  card.append(button("Kembali ke daftar", loadPutaway, "outline"));
  taskList.append(card);
}

async function loadPicking() {
  const page = await request("/picking/tasks?page=0&size=100");
  if (!page.data.length) return showEmpty("Tidak ada tugas picking untuk akun ini.");
  page.data.forEach(task => {
    const card = taskCard(task.taskCode, `${task.orderCode} · section ${task.sectionCode}`, task.status);
    card.append(button("Buka tugas", () => openPicking(task.taskId)));
    taskList.append(card);
  });
}

async function openPicking(id) {
  const detail = await request(`/picking/tasks/${id}`);
  taskList.replaceChildren();
  document.querySelector("#taskTitle").textContent = detail.task.taskCode;
  document.querySelector("#taskHint").textContent = `${detail.task.orderCode} · section ${detail.task.sectionCode}`;
  const card = workflowCard();
  if (!detail.lines.length) card.append(text("p", "Tidak ada baris picking yang tersisa."));
  detail.lines.forEach(line => {
    const row = element("div", "line-row");
    const description = element("div");
    description.append(text("p", line.itemCode, "line-title"),
      text("p", `${line.itemName} · ${line.rackCode} · batch ${line.batchCode}`),
      text("p", `${line.pickedQuantity}/${line.reservedQuantity} unit dipilih · sisa ${line.remainingQuantity}`));
    if (line.remainingQuantity <= 0) {
      row.append(description, text("p", "Jumlah lot sudah terpenuhi.", "muted"));
      card.append(row);
      return;
    }
    const fields = element("div");
    const quantity = input("Jumlah scan", "number", { min: "1", max: String(line.remainingQuantity), step: "1", inputmode: "numeric" });
    const itemBarcode = input("QR barang", "text", { autocomplete: "off", maxlength: "512" });
    const locationBarcode = input("Barcode lokasi", "text", { autocomplete: "off", maxlength: "512" });
    fields.append(quantity, fieldWithButton(itemBarcode, button("Kamera", () => openScanner(itemBarcode), "outline")),
      fieldWithButton(locationBarcode, button("Kamera", () => openScanner(locationBarcode), "outline")),
      button("Catat scan", async () => {
        const amount = Number(quantity.value);
        if (!Number.isSafeInteger(amount) || amount < 1 || amount > line.remainingQuantity
            || !itemBarcode.value.trim() || !locationBarcode.value.trim()) {
          return message(appMessage, "Isi jumlah yang tersedia dan pindai item serta lokasi.", "error");
        }
        try {
          await request(`/picking/tasks/${id}/scans`, { method: "POST", body: JSON.stringify({
            lineId: line.lineId, quantity: amount, itemBarcode: itemBarcode.value.trim(),
            locationBarcode: locationBarcode.value.trim()
          }) });
          message(appMessage, "Scan picking berhasil dicatat.", "success");
          await openPicking(id);
        } catch (error) { message(appMessage, error.message, "error"); }
      }));
    row.append(description, fields);
    card.append(row);
  });
  card.append(button("Selesaikan picking", async () => {
    try {
      await request(`/picking/tasks/${id}/complete`, { method: "POST" });
      message(appMessage, "Tugas picking selesai.", "success");
      await loadPicking();
    } catch (error) { message(appMessage, error.message, "error"); }
  }));
  card.append(button("Kembali ke daftar", loadPicking, "outline"));
  taskList.append(card);
}

function taskCard(title, detail, status) {
  const card = element("article", "task-card");
  card.append(text("h3", title), text("p", detail), text("p", status));
  return card;
}

function workflowCard() { return element("article", "workflow-card"); }

function showEmpty(label) {
  taskList.replaceChildren(text("p", label, "empty-state"));
}

function input(labelText, type, attributes = {}) {
  const field = document.createElement("input");
  field.type = type;
  field.setAttribute("aria-label", labelText);
  field.placeholder = labelText;
  Object.entries(attributes).forEach(([key, value]) => field.setAttribute(key, value));
  return field;
}

function fieldWithButton(field, action) {
  const row = element("div", "field-row");
  row.append(field, action);
  return row;
}

function button(label, action, style = "primary") {
  const control = document.createElement("button");
  control.type = "button";
  control.className = `button ${style}`;
  control.textContent = label;
  control.addEventListener("click", async event => {
    control.disabled = true;
    try { await action(event); }
    catch (error) { message(appMessage, error.message, "error"); }
    finally { control.disabled = false; }
  });
  return control;
}

function element(tag, className = "") {
  const node = document.createElement(tag);
  if (className) node.className = className;
  return node;
}

function text(tag, content, className = "") {
  const node = element(tag, className);
  node.textContent = content ?? "";
  return node;
}

async function openScanner(target) {
  const generation = ++scannerGeneration;
  scanTarget = target;
  scannerDialog.showModal();
  scannerMessage.textContent = "Arahkan kamera ke barcode.";
  if (!navigator.mediaDevices?.getUserMedia || !window.BarcodeDetector) {
    scannerMessage.textContent = "Pemindaian kamera tidak tersedia. Gunakan pemindai barcode perangkat atau isi kolom secara manual.";
    return;
  }
  try {
    const supported = await BarcodeDetector.getSupportedFormats();
    const formats = ["qr_code", "code_128", "ean_13", "ean_8", "upc_a", "upc_e"]
      .filter(format => supported.includes(format));
    if (!formats.length) throw new Error("Format barcode tidak didukung.");
    const detector = new BarcodeDetector({ formats });
    const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: { ideal: "environment" } }, audio: false });
    if (generation !== scannerGeneration) {
      stream.getTracks().forEach(track => track.stop());
      return;
    }
    scanStream = stream;
    scannerVideo.srcObject = scanStream;
    await scannerVideo.play();
    scannerMessage.textContent = "Arahkan kamera belakang ke barcode.";
    const scan = async () => {
      if (!scanStream || scannerDialog.open === false) return;
      try {
        const matches = await detector.detect(scannerVideo);
        if (matches.length) {
          scanTarget.value = matches[0].rawValue;
          closeScanner();
          message(appMessage, "Barcode terbaca.", "success");
          return;
        }
      } catch { /* Keep scanning while the camera remains available. */ }
      scanTimer = window.setTimeout(scan, 180);
    };
    scan();
  } catch {
    if (generation !== scannerGeneration) return;
    stopCamera();
    scannerMessage.textContent = "Kamera tidak dapat dibuka. Izinkan akses kamera atau gunakan pemindai perangkat.";
  }
}

function closeScanner() {
  if (scannerDialog.open) scannerDialog.close();
  stopCamera();
}

function stopCamera() {
  scannerGeneration++;
  if (scanTimer) window.clearTimeout(scanTimer);
  scanTimer = null;
  if (scanStream) scanStream.getTracks().forEach(track => track.stop());
  scanStream = null;
  scannerVideo.srcObject = null;
  scanTarget = null;
}
