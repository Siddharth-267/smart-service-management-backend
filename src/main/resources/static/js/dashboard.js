
// ==========================================
// SMART SERVICE MANAGEMENT
// Complete Dashboard and Request Management
// Including Request Status History
// ==========================================

const API_BASE_URL = "/api";

const token = sessionStorage.getItem("authToken");
const savedUser = sessionStorage.getItem("currentUser");

let currentUser = null;
let serviceRequests = [];

// ==========================================
// AUTHENTICATION CHECK
// ==========================================

if (!token || !savedUser) {

    window.location.replace("/");

} else {

    try {

        currentUser = JSON.parse(savedUser);

        if (!currentUser || !currentUser.role) {
            throw new Error("Invalid user session");
        }

        initializeDashboard();

    } catch (error) {

        console.error("Session error:", error);

        sessionStorage.removeItem("authToken");
        sessionStorage.removeItem("currentUser");

        window.location.replace("/");
    }
}

// ==========================================
// API HELPER
// ==========================================

async function apiRequest(endpoint, options = {}) {

    const headers = {
        "Content-Type": "application/json",
        ...options.headers
    };

    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    let response;

    try {

        response = await fetch(`${API_BASE_URL}${endpoint}`, {
            ...options,
            headers
        });

    } catch (error) {

        throw new Error(
            "Unable to connect to the server. Check whether Spring Boot is running."
        );
    }

    const responseText = await response.text();

    let data = {};

    if (responseText) {

        try {
            data = JSON.parse(responseText);
        } catch {
            data = { message: responseText };
        }
    }

    if (response.status === 401) {

        sessionStorage.removeItem("authToken");
        sessionStorage.removeItem("currentUser");

        window.location.replace("/");

        throw new Error("Session expired. Please sign in again.");
    }

    if (!response.ok) {

        throw new Error(
            data.message ||
            data.error ||
            `Request failed with status ${response.status}`
        );
    }

    return data;
}

// ==========================================
// NORMALIZE USER ROLE
// ==========================================

function getUserRole() {

    return String(currentUser.role)
        .toUpperCase()
        .replace(/^ROLE_/, "");
}

// ==========================================
// INITIALIZE DASHBOARD
// ==========================================

function initializeDashboard() {

    const role = getUserRole();

    // Display user information

    document.getElementById("headerUserName").textContent =
        currentUser.name || currentUser.email || "User";

    document.getElementById("headerUserRole").textContent = role;

    document.getElementById("welcomeName").textContent =
        currentUser.name || "User";

    document.getElementById("roleBadge").textContent = role;

    const descriptions = {

        USER: "Create service requests and monitor their progress.",

        STAFF: "Manage assigned service requests and update their status.",

        ADMIN: "Monitor service operations and manage the application."

    };

    document.getElementById("welcomeDescription").textContent =
        descriptions[role] || "Manage service requests.";

    // USER DASHBOARD

    if (role === "USER") {

        document.getElementById("userMenu").hidden = false;

        document.getElementById("userDashboard").hidden = false;

        initializeRequestForm();

    }

    // STAFF DASHBOARD

    else if (role === "STAFF") {

        document.getElementById("staffMenu").hidden = false;

        document.getElementById("staffDashboard").hidden = false;

        document.getElementById("refreshAvailableButton")
            .addEventListener("click", loadAvailableRequests);

        loadAvailableRequests();

    }

    // ADMIN DASHBOARD

    else if (role === "ADMIN") {

        document.getElementById("adminMenu").hidden = false;

        document.getElementById("adminDashboard").hidden = false;

        document.getElementById("runEscalationButton")
            .addEventListener("click", runEscalation);

    }

    // INVALID ROLE

    else {

        alert("Invalid user role. Please sign in again.");

        sessionStorage.removeItem("authToken");
        sessionStorage.removeItem("currentUser");

        window.location.replace("/");

        return;
    }

    // INITIALIZE HISTORY MODAL

    initializeHistoryModal();

    // COMMON LOGOUT

    document.getElementById("logoutButton")
        .addEventListener("click", logout);

    // LOAD ACCESSIBLE REQUESTS

    loadRequests();
}

// ==========================================
// USER: INITIALIZE REQUEST FORM
// ==========================================

function initializeRequestForm() {

    const createButton =
        document.getElementById("createRequestButton");

    const cancelButton =
        document.getElementById("cancelRequestButton");

    const requestForm =
        document.getElementById("createRequestForm");

    const message =
        document.getElementById("createRequestMessage");

    createButton.addEventListener("click", () => {

        requestForm.hidden = false;

        message.textContent = "";

        message.classList.remove("success");

        requestForm.scrollIntoView({
            behavior: "smooth",
            block: "center"
        });
    });

    cancelButton.addEventListener("click", () => {

        requestForm.reset();

        requestForm.hidden = true;

        message.textContent = "";

        message.classList.remove("success");
    });

    requestForm.addEventListener(
        "submit",
        createServiceRequest
    );
}

// ==========================================
// USER: CREATE SERVICE REQUEST
// POST /api/requests
// ==========================================

async function createServiceRequest(event) {

    event.preventDefault();

    const form = event.currentTarget;

    const submitButton =
        document.getElementById("submitRequestButton");

    const message =
        document.getElementById("createRequestMessage");

    message.textContent = "";

    message.classList.remove("success");

    // User ID is taken from authenticated JWT.

    const payload = {

        categoryId: Number(
            document.getElementById("requestCategory").value
        ),

        title:
            document.getElementById("requestTitle").value.trim(),

        description:
            document.getElementById("requestDescription").value.trim(),

        location:
            document.getElementById("requestLocation").value.trim()

    };

    if (
        !payload.categoryId ||
        !payload.title ||
        !payload.description ||
        !payload.location
    ) {

        message.textContent =
            "Please complete all required fields.";

        return;
    }

    submitButton.disabled = true;

    submitButton.textContent = "Submitting...";

    try {

        const data = await apiRequest("/requests", {

            method: "POST",

            body: JSON.stringify(payload)

        });

        message.textContent =
            `Request #${data.requestId} created successfully!`;

        message.classList.add("success");

        form.reset();

        await loadRequests();

    } catch (error) {

        message.textContent = error.message;

    } finally {

        submitButton.disabled = false;

        submitButton.textContent = "Submit Request";
    }
}

// ==========================================
// LOAD SERVICE REQUESTS
// GET /api/requests
// ==========================================

async function loadRequests() {

    const requestList =
        document.getElementById("requestList");

    requestList.textContent =
        "Loading service requests...";

    try {

        const data = await apiRequest("/requests");

        if (Array.isArray(data)) {

            serviceRequests = data;

        } else if (Array.isArray(data.requests)) {

            serviceRequests = data.requests;

        } else {

            throw new Error(
                "Unexpected service request response format."
            );
        }

        updateStatistics();

        displayRequests();

    } catch (error) {

        requestList.textContent = error.message;

        console.error(
            "Unable to load service requests:",
            error
        );
    }
}

// ==========================================
// STAFF: LOAD AVAILABLE REQUESTS
// GET /api/requests/available
// ==========================================

async function loadAvailableRequests() {

    const requestList =
        document.getElementById("availableRequestList");

    requestList.textContent =
        "Loading available requests...";

    try {

        const requests =
            await apiRequest("/requests/available");

        if (!Array.isArray(requests)) {

            throw new Error(
                "Unexpected available request response."
            );
        }

        displayAvailableRequests(requests);

    } catch (error) {

        requestList.textContent = error.message;

        console.error(
            "Unable to load available requests:",
            error
        );
    }
}

// ==========================================
// STAFF: DISPLAY AVAILABLE REQUESTS
// ==========================================

function displayAvailableRequests(requests) {

    const requestList =
        document.getElementById("availableRequestList");

    requestList.replaceChildren();

    if (requests.length === 0) {

        const emptyMessage =
            document.createElement("p");

        emptyMessage.className = "empty-message";

        emptyMessage.textContent =
            "No unassigned OPEN requests are currently available.";

        requestList.appendChild(emptyMessage);

        return;
    }

    requests.forEach(request => {

        const card =
            document.createElement("article");

        card.className = "request-card";

        // Heading

        const heading =
            document.createElement("div");

        heading.className = "request-card-heading";

        const title =
            document.createElement("h3");

        title.textContent =
            request.title || "Untitled Request";

        const status =
            document.createElement("span");

        status.className = "status-badge";

        status.textContent =
            request.status || "OPEN";

        heading.append(title, status);

        // Description

        const description =
            document.createElement("p");

        description.className = "request-description";

        description.textContent =
            request.description || "No description";

        // Request metadata

        const details =
            document.createElement("div");

        details.className = "request-meta";

        const values = [

            `Request #${request.requestId}`,

            `Location: ${request.location || "Not provided"}`,

            `Priority: ${request.priority || "UNKNOWN"}`,

            `Created: ${formatDate(request.createdAt)}`

        ];

        values.forEach(value => {

            const item =
                document.createElement("span");

            item.textContent = value;

            details.appendChild(item);
        });

        // Take Request button

        const takeButton =
            document.createElement("button");

        takeButton.type = "button";

        takeButton.className = "primary-button";

        takeButton.textContent = "Take Request";

        takeButton.addEventListener("click", () => {

            assignRequest(
                request.requestId,
                takeButton
            );
        });

        card.append(
            heading,
            description,
            details,
            takeButton
        );

        requestList.appendChild(card);
    });
}

// ==========================================
// STAFF: SELF ASSIGNMENT
// POST /api/assignments
// ==========================================

async function assignRequest(requestId, button) {

    const confirmed = confirm(
        `Do you want to take service request #${requestId}?`
    );

    if (!confirmed) {
        return;
    }

    button.disabled = true;

    button.textContent = "Assigning...";

    try {

        await apiRequest("/assignments", {

            method: "POST",

            body: JSON.stringify({

                requestId: requestId

            })

        });

        alert(
            `Service request #${requestId} assigned successfully!`
        );

        await loadAvailableRequests();

        await loadRequests();

    } catch (error) {

        alert(`Assignment failed: ${error.message}`);

        button.disabled = false;

        button.textContent = "Take Request";
    }
}

// ==========================================
// UPDATE DASHBOARD STATISTICS
// ==========================================

function updateStatistics() {

    const total = serviceRequests.length;

    const open = serviceRequests.filter(request => {

        const status =
            String(request.status || "").toUpperCase();

        return !["RESOLVED", "CLOSED"].includes(status);

    }).length;

    const resolved = serviceRequests.filter(request => {

        const status =
            String(request.status || "").toUpperCase();

        return ["RESOLVED", "CLOSED"].includes(status);

    }).length;

    const highPriority = serviceRequests.filter(request =>

        String(request.priority || "").toUpperCase() === "HIGH"

    ).length;

    document.getElementById("totalRequests").textContent =
        total;

    document.getElementById("openRequests").textContent =
        open;

    document.getElementById("resolvedRequests").textContent =
        resolved;

    document.getElementById("highPriorityRequests").textContent =
        highPriority;
}

// ==========================================
// DISPLAY SERVICE REQUESTS
// WITH ROLE-BASED ACTION BUTTONS
// ==========================================

function displayRequests() {

    const requestList =
        document.getElementById("requestList");

    requestList.replaceChildren();

    if (serviceRequests.length === 0) {

        const emptyMessage =
            document.createElement("p");

        emptyMessage.className = "empty-message";

        emptyMessage.textContent =
            "No service requests found.";

        requestList.appendChild(emptyMessage);

        return;
    }

    const role = getUserRole();

    serviceRequests.forEach(request => {

        const card =
            document.createElement("article");

        card.className = "request-card";

        // Heading

        const heading =
            document.createElement("div");

        heading.className = "request-card-heading";

        const title =
            document.createElement("h3");

        title.textContent =
            request.title || "Untitled Request";

        const status =
            document.createElement("span");

        status.className = "status-badge";

        status.textContent =
            request.status || "UNKNOWN";

        heading.append(title, status);

        // Description

        const description =
            document.createElement("p");

        description.className = "request-description";

        description.textContent =
            request.description || "No description";

        // Metadata

        const metadata =
            document.createElement("div");

        metadata.className = "request-meta";

        const details = [

            `Request #${request.requestId}`,

            `Location: ${request.location || "Not provided"}`,

            `Priority: ${request.priority || "UNKNOWN"}`,

            `Created: ${formatDate(request.createdAt)}`

        ];

        details.forEach(detail => {

            const span =
                document.createElement("span");

            span.textContent = detail;

            metadata.appendChild(span);
        });

        card.append(
            heading,
            description,
            metadata
        );

        // Recurring issue information

        if (request.recurringIssue) {

            const recurring =
                document.createElement("p");

            recurring.className = "recurring-label";

            recurring.textContent =
                `Recurring issue (${request.recurringIssueCount || 1} occurrences)`;

            card.appendChild(recurring);
        }

        // Role-specific actions

        const actions =
            document.createElement("div");

        actions.className = "request-actions";

        // VIEW HISTORY BUTTON

        const historyButton =
            document.createElement("button");

        historyButton.type = "button";

        historyButton.className = "secondary-button";

        historyButton.textContent = "View History";

        historyButton.addEventListener("click", () => {

            viewRequestHistory(request.requestId);

        });

        actions.appendChild(historyButton);

        const currentStatus =
            String(request.status || "").toUpperCase();

        // STAFF: OPEN -> IN_PROGRESS

        if (role === "STAFF" && currentStatus === "OPEN") {

            const startButton =
                document.createElement("button");

            startButton.type = "button";

            startButton.className = "primary-button";

            startButton.textContent = "Start Work";

            startButton.addEventListener("click", () => {

                updateRequestStatus(
                    request.requestId,
                    "IN_PROGRESS",
                    "Work has started.",
                    startButton
                );
            });

            actions.appendChild(startButton);
        }

        // STAFF: IN_PROGRESS -> RESOLVED

        if (role === "STAFF" && currentStatus === "IN_PROGRESS") {

            const resolveButton =
                document.createElement("button");

            resolveButton.type = "button";

            resolveButton.className = "primary-button";

            resolveButton.textContent = "Mark Resolved";

            resolveButton.addEventListener("click", () => {

                updateRequestStatus(
                    request.requestId,
                    "RESOLVED",
                    "Work completed. Awaiting user verification.",
                    resolveButton
                );
            });

            actions.appendChild(resolveButton);
        }

        // USER: RESOLVED -> CLOSED

        if (role === "USER" && currentStatus === "RESOLVED") {

            const closeButton =
                document.createElement("button");

            closeButton.type = "button";

            closeButton.className = "primary-button";

            closeButton.textContent = "Confirm Resolution";

            closeButton.addEventListener("click", () => {

                updateRequestStatus(
                    request.requestId,
                    "CLOSED",
                    "User confirmed that the issue has been resolved.",
                    closeButton
                );
            });

            const reopenButton =
                document.createElement("button");

            reopenButton.type = "button";

            reopenButton.className = "secondary-button";

            reopenButton.textContent = "Reopen Request";

            reopenButton.addEventListener("click", () => {

                updateRequestStatus(
                    request.requestId,
                    "OPEN",
                    "User reports that the issue is not fully resolved.",
                    reopenButton
                );
            });

            actions.append(
                closeButton,
                reopenButton
            );
        }

        if (actions.children.length > 0) {

            card.appendChild(actions);
        }

        requestList.appendChild(card);
    });
}

// ==========================================
// UPDATE REQUEST STATUS
// PATCH /api/requests/{requestId}/status
// ==========================================

async function updateRequestStatus(
    requestId,
    newStatus,
    remarks,
    button
) {

    const confirmed = confirm(
        `Are you sure you want to change request #${requestId} to ${newStatus}?`
    );

    if (!confirmed) {
        return;
    }

    const originalText = button.textContent;

    button.disabled = true;
    button.textContent = "Updating...";

    try {

        await apiRequest(`/requests/${requestId}/status`, {

            method: "PATCH",

            body: JSON.stringify({
                newStatus: newStatus,
                remarks: remarks
            })

        });

        alert(
            `Request #${requestId} successfully updated to ${newStatus}.`
        );

        await loadRequests();

    } catch (error) {

        alert(`Status update failed: ${error.message}`);

        button.disabled = false;
        button.textContent = originalText;
    }
}

// ==========================================
// REQUEST HISTORY MODAL INITIALIZATION
// ==========================================

function initializeHistoryModal() {

    const modal =
        document.getElementById("historyModal");

    const closeButton =
        document.getElementById("closeHistoryButton");

    const backdrop =
        document.getElementById("historyBackdrop");

    if (!modal || !closeButton || !backdrop) {

        console.error("History modal elements are missing.");

        return;
    }

    closeButton.addEventListener(
        "click",
        closeHistoryModal
    );

    backdrop.addEventListener(
        "click",
        closeHistoryModal
    );

    document.addEventListener("keydown", event => {

        if (
            event.key === "Escape" &&
            !modal.hidden
        ) {

            closeHistoryModal();

        }

    });
}

// ==========================================
// VIEW REQUEST HISTORY
// GET /api/requests/{requestId}/history
// ==========================================


async function viewRequestHistory(requestId) {

    const modal =
        document.getElementById("historyModal");

    const title =
        document.getElementById("historyTitle");

    const requestIdElement =
        document.getElementById("historyRequestId");

    const content =
        document.getElementById("historyContent");

    title.textContent = "Request Status History";

    requestIdElement.textContent =
        `Request #${requestId}`;

    content.replaceChildren();

    const loading =
        document.createElement("p");

    loading.className = "empty-message";
    loading.textContent = "Loading request history...";

    content.appendChild(loading);

    modal.hidden = false;

    try {

        const data = await apiRequest(
            `/requests/${requestId}/history`
        );

        const history = Array.isArray(data)
            ? data
            : Array.isArray(data.history)
                ? data.history
                : [];

        content.replaceChildren();

        if (history.length === 0) {

            const emptyMessage =
                document.createElement("p");

            emptyMessage.className = "empty-message";

            emptyMessage.textContent =
                "No status history is available for this request.";

            content.appendChild(emptyMessage);

            return;
        }

        history.forEach(record => {

            const item =
                document.createElement("article");

            item.className = "history-item";

            const statusHeading =
                document.createElement("h3");

            const oldStatus =
                record.oldStatus || "CREATED";

            const newStatus =
                record.newStatus || "UNKNOWN";

            statusHeading.textContent =
                `${oldStatus} → ${newStatus}`;

            const changedAt =
                document.createElement("p");

            changedAt.className = "history-date";

            changedAt.textContent =
                `Changed: ${formatDate(record.changedAt)}`;

            const changedBy =
                document.createElement("p");

            const actorName =
                record.changedByName ||
                (record.changedBy
                    ? `User #${record.changedBy}`
                    : "System");

            changedBy.textContent =
                `Changed by: ${actorName}`;

            const remarks =
                document.createElement("p");

            remarks.className = "history-remarks";

            remarks.textContent =
                record.remarks || "No remarks provided.";

            item.append(
                statusHeading,
                changedAt,
                changedBy,
                remarks
            );

            content.appendChild(item);

        });

    } catch (error) {

        content.replaceChildren();

        const errorMessage =
            document.createElement("p");

        errorMessage.className = "empty-message";

        errorMessage.textContent =
            `Unable to load history: ${error.message}`;

        content.appendChild(errorMessage);

        console.error(
            "Request history error:",
            error
        );
    }
}


// ==========================================
// CLOSE REQUEST HISTORY MODAL
// ==========================================

function closeHistoryModal() {

    const modal =
        document.getElementById("historyModal");

    if (modal) {
        modal.hidden = true;
    }
}

// ==========================================
// DATE FORMAT
// ==========================================

function formatDate(dateValue) {

    if (!dateValue) {
        return "Unavailable";
    }

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) {
        return "Unknown";
    }

    return date.toLocaleString("en-IN", {
        dateStyle: "medium",
        timeStyle: "short"
    });
}

// ==========================================
// ADMIN: RUN OVERDUE ESCALATION
// ==========================================

async function runEscalation() {

    const button =
        document.getElementById("runEscalationButton");

    if (!confirm(
        "Run the overdue service request escalation check?"
    )) {
        return;
    }

    button.disabled = true;
    button.textContent = "Running...";

    try {

        await apiRequest("/escalation/run", {
            method: "POST"
        });

        alert("Escalation check completed successfully.");

        await loadRequests();

    } catch (error) {

        alert(`Escalation failed: ${error.message}`);

    } finally {

        button.disabled = false;
        button.textContent = "Run Escalation";
    }
}

// ==========================================
// LOGOUT
// ==========================================

function logout() {

    sessionStorage.removeItem("authToken");
    sessionStorage.removeItem("currentUser");

    window.location.replace("/");
}
