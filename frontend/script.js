const API_URL = "api/students";

const form = document.querySelector("#student-form");
const studentIdInput = document.querySelector("#student-id");
const fullNameInput = document.querySelector("#full-name");
const emailInput = document.querySelector("#email");
const courseInput = document.querySelector("#course");
const yearInput = document.querySelector("#year");
const submitButton = document.querySelector("#submit-button");
const cancelEditButton = document.querySelector("#cancel-edit");
const rows = document.querySelector("#student-rows");
const emptyState = document.querySelector("#empty-state");
const toast = document.querySelector("#toast");
const connectionStatus = document.querySelector("#connection-status");
let students = [];
let toastTimer;

async function requestStudents() {
  const response = await fetch(API_URL, { headers: { Accept: "application/json" } });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || "Could not load students.");
  students = result.students;
  connectionStatus.innerHTML = '<span class="status-dot"></span> Connected to your database';
  connectionStatus.classList.remove("connection-error");
  renderStudents();
}

function renderStudents() {
  rows.replaceChildren();
  document.querySelector("#student-count").textContent = students.length;
  document.querySelector("#course-count").textContent = new Set(students.map((student) => student.course)).size;
  document.querySelector("#year-count").textContent = new Set(students.map((student) => student.year)).size;
  document.querySelector("#record-count").textContent = `${students.length} ${students.length === 1 ? "record" : "records"}`;
  emptyState.hidden = students.length > 0;

  for (const student of students) {
    const row = document.createElement("tr");
    row.dataset.id = student.id;
    const studentCell = document.createElement("td");
    const identity = document.createElement("div");
    identity.className = "student-cell";

    const avatar = document.createElement("span");
    avatar.className = "avatar";
    avatar.textContent = initials(student.fullName);

    const identityText = document.createElement("div");
    const name = document.createElement("div");
    name.className = "student-name";
    name.textContent = student.fullName;
    const email = document.createElement("div");
    email.className = "student-email";
    email.textContent = student.email;
    identityText.append(name, email);
    identity.append(avatar, identityText);
    studentCell.append(identity);

    const courseCell = document.createElement("td");
    courseCell.className = "course-name";
    courseCell.textContent = student.course;
    const yearCell = document.createElement("td");
    const yearBadge = document.createElement("span");
    yearBadge.className = "year-badge";
    yearBadge.textContent = `Year ${student.year}`;
    yearCell.append(yearBadge);

    const actionsCell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";
    actions.append(
      createActionButton("Edit", "✎", () => startEditing(student)),
      createActionButton("Delete", "×", () => deleteStudent(student))
    );
    actionsCell.append(actions);
    row.append(studentCell, courseCell, yearCell, actionsCell);
    rows.append(row);
  }
}

function createActionButton(label, symbol, action) {
  const button = document.createElement("button");
  button.className = `icon-button${label === "Delete" ? " delete" : ""}`;
  button.type = "button";
  button.setAttribute("aria-label", `${label} ${label === "Delete" ? "student" : "student record"}`);
  button.title = label;
  button.textContent = symbol;
  button.addEventListener("click", action);
  return button;
}

function initials(fullName) {
  return fullName.trim().split(/\s+/).slice(0, 2).map((part) => part[0]).join("").toUpperCase();
}

function startEditing(student) {
  studentIdInput.value = student.id;
  fullNameInput.value = student.fullName;
  emailInput.value = student.email;
  courseInput.value = student.course;
  yearInput.value = student.year;
  document.querySelector("#form-title").textContent = "Edit student";
  submitButton.innerHTML = "<span aria-hidden=\"true\">✓</span> Save changes";
  cancelEditButton.hidden = false;
  fullNameInput.focus();
  document.querySelector("#student-form").scrollIntoView({ behavior: "smooth", block: "start" });
}

function resetForm() {
  form.reset();
  studentIdInput.value = "";
  document.querySelector("#form-title").textContent = "Add a student";
  submitButton.innerHTML = "<span aria-hidden=\"true\">＋</span> Add student";
  cancelEditButton.hidden = true;
}

async function deleteStudent(student) {
  if (!window.confirm(`Delete ${student.fullName}'s student record? This cannot be undone.`)) return;
  try {
    await submitAction({ action: "delete", id: student.id }, "Student deleted.");
  } catch (error) {
    showToast(error.message, true);
  }
}

async function submitAction(values, successMessage) {
  const response = await fetch(API_URL, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", Accept: "application/json" },
    body: new URLSearchParams(values)
  });
  const result = await response.json();
  if (!response.ok) throw new Error(result.error || "The request could not be completed.");
  await requestStudents();
  showToast(successMessage);
}

function showToast(message, isError = false) {
  window.clearTimeout(toastTimer);
  toast.textContent = message;
  toast.classList.toggle("error", isError);
  toast.classList.add("visible");
  toastTimer = window.setTimeout(() => toast.classList.remove("visible"), 3200);
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  submitButton.disabled = true;
  const isEditing = Boolean(studentIdInput.value);
  const values = Object.fromEntries(new FormData(form).entries());
  values.action = isEditing ? "update" : "add";

  try {
    await submitAction(values, isEditing ? "Student updated." : "Student added.");
    resetForm();
  } catch (error) {
    showToast(error.message, true);
  } finally {
    submitButton.disabled = false;
  }
});

cancelEditButton.addEventListener("click", resetForm);

requestStudents().catch((error) => {
  connectionStatus.innerHTML = '<span class="status-dot"></span> Backend connection unavailable';
  connectionStatus.classList.add("connection-error");
  showToast(error.message, true);
  emptyState.hidden = false;
  emptyState.querySelector("h3").textContent = "Could not load students";
  emptyState.querySelector("p").textContent = "Check that the Java server and database are running.";
});
