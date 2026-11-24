// ==========================================
// SMART SERVICE MANAGEMENT
// Frontend Authentication
// ==========================================

const API_BASE_URL = "/api";

// Form sections
const loginSection = document.getElementById("loginSection");
const registerSection = document.getElementById("registerSection");

// Forms
const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");

// Messages
const loginMessage = document.getElementById("loginMessage");
const registerMessage = document.getElementById("registerMessage");

// Buttons
const loginButton = document.getElementById("loginButton");
const registerButton = document.getElementById("registerButton");

// Switch between login and registration
document.getElementById("showRegister").addEventListener("click", () => {
    loginSection.hidden = true;
    registerSection.hidden = false;
    clearMessages();
});

document.getElementById("showLogin").addEventListener("click", () => {
    registerSection.hidden = true;
    loginSection.hidden = false;
    clearMessages();
});

function clearMessages() {
    loginMessage.textContent = "";
    registerMessage.textContent = "";

    loginMessage.classList.remove("success");
    registerMessage.classList.remove("success");
}

function showMessage(element, message, isSuccess = false) {
    element.textContent = message;
    element.classList.toggle("success", isSuccess);
}

async function readResponse(response) {
    const responseText = await response.text();

    if (!responseText) {
        return {};
    }

    try {
        return JSON.parse(responseText);
    } catch {
        return { message: responseText };
    }
}

function getErrorMessage(data, fallback) {
    if (typeof data.message === "string") {
        return data.message;
    }

    if (typeof data.error === "string") {
        return data.error;
    }

    return fallback;
}

// ==========================================
// LOGIN
// POST /api/auth/login
// ==========================================

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    clearMessages();

    const email = document.getElementById("loginEmail").value.trim();
    const password = document.getElementById("loginPassword").value;

    loginButton.disabled = true;
    loginButton.textContent = "Signing in...";

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                email: email,
                password: password
            })
        });

        const data = await readResponse(response);

        if (!response.ok) {
            throw new Error(
                getErrorMessage(data, "Login failed. Please check your credentials.")
            );
        }

        const token = data.token || data.accessToken;

        if (!token) {
            throw new Error("Login response did not contain an authentication token.");
        }

        // Store login information for the current browser session
        sessionStorage.setItem("authToken", token);

        sessionStorage.setItem("currentUser", JSON.stringify({
            userId: data.userId,
            name: data.name,
            email: data.email || email,
            role: data.role
        }));

        showMessage(
            loginMessage,
            `Login successful! Welcome ${data.name || "back"}.`,
            true
        );

        // Dashboard will be created in the next implementation step
        setTimeout(() => {
            window.location.href = "/dashboard.html";
        }, 700);

    } catch (error) {
        showMessage(loginMessage, error.message);
    } finally {
        loginButton.disabled = false;
        loginButton.textContent = "Sign In";
    }
});

// ==========================================
// REGISTRATION
// POST /api/users/register
// ==========================================

registerForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    clearMessages();

    const name = document.getElementById("registerName").value.trim();
    const email = document.getElementById("registerEmail").value.trim();
    const password = document.getElementById("registerPassword").value;

    registerButton.disabled = true;
    registerButton.textContent = "Creating account...";

    try {
        const response = await fetch(`${API_BASE_URL}/users/register`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                name: name,
                email: email,
                password: password
            })
        });

        const data = await readResponse(response);

        if (!response.ok) {
            throw new Error(
                getErrorMessage(data, "Registration failed. Please try again.")
            );
        }

        showMessage(
            registerMessage,
            "Account created successfully! Please sign in.",
            true
        );

        registerForm.reset();

        document.getElementById("loginEmail").value = email;

        setTimeout(() => {
            registerSection.hidden = true;
            loginSection.hidden = false;
            clearMessages();
        }, 1500);

    } catch (error) {
        showMessage(registerMessage, error.message);
    } finally {
        registerButton.disabled = false;
        registerButton.textContent = "Create Account";
    }
});