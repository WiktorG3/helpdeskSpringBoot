document.getElementById("registerForm").addEventListener("submit", async function (event) {
    event.preventDefault();
    const username = document.getElementById("username").value;
    const email = document.getElementById("email").value;
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    const passwordError = document.getElementById("message");
    if (password !== confirmPassword) {
        passwordError.textContent = "Passwords do not match!";
        return;
    } else {
        passwordError.textContent = "";
    }

    const userData = {
        username: username,
        email: email,
        password: password
    };

    try {
        const response = await fetch("http://localhost:8080/api/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(userData),
        });

        const message = document.getElementById("message");
        if (response.ok) {
            const result = await response.text();
            message.style.color = "green";
            message.textContent = `Success: ${result}`;
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