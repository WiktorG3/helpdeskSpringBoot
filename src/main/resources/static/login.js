document.getElementById("loginForm").addEventListener("submit", async function (event) {
    event.preventDefault();

    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    const loginData = {
        username: username,
        password: password
    };

    try {
        const response = await fetch("http://localhost:8080/api/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(loginData),
        });

        const message = document.getElementById("message");
        if (response.ok) {
            const result = await response.text();
            message.style.color = "green";
            message.textContent = `Success: ${result}`;
            setTimeout(() => { window.location.href = "dashboard.html";}, 500);
        } else {
            const error = await response.text();
            message.style.color = "red";
            message.textContent = `Error: ${error}`;
        }
    } catch (error) {
        console.error("Error:", error);
        const message = document.getElementById("message");
        message.style.color = "red";
        message.textContent = "An error occurred. Please try again.";
    }
});