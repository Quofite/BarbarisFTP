document.getElementById("show_pass").addEventListener("click", (e) => {
    var passBox = document.getElementById("password");
    var image = document.getElementById("img");

    if(passBox.type === "password") {
        passBox.type = "text";
        image.setAttribute("class", "bi bi-eye-slash-fill");
    } else {
        passBox.type = "password";
        image.setAttribute("class", "bi bi-eye-fill");
    }

});