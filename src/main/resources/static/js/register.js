document.getElementById("registerForm").addEventListener("submit",  function (event) {
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;

    const passwordError = document.getElementById("message");
    if (password !== confirmPassword) {
        event.preventDefault();
        passwordError.textContent = "Passwords do not match!";
    } else {
        passwordError.textContent = "";
    }
});