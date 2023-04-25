document.getElementById("download").addEventListener("click", (e) => {
    e.preventDefault();

    var boxes = document.querySelectorAll(".check");
    var requestString = "";

    for (let i = 0; i < boxes.length; i++) {
        if(boxes[i].checked) {
            requestString += boxes[i].name + ";";
        }
    }

    var request = new XMLHttpRequest();
    request.open("GET", "http://localhost:8080/downloadFtp?files=" + requestString, false);

    request.addEventListener("load", (e) => {
        var code = request.status;
        var errorBox = document.getElementById("error");
        var errorMessage = document.getElementById("error_message");
        
        if(code === 404) {
            errorBox.removeAttribute("hidden");
            errorMessage.innerText = "Файл не найден";
        } else if(code === 500) {
            errorBox.removeAttribute("hidden");
            errorMessage.innerText = "Не удалось скачать файл";
        } else if(code === 504) {
            errorBox.removeAttribute("hidden");
            errorMessage.innerText = "Не удалось подключиться к серверу";
        } else {
            window.location.href = "http://localhost:8080/download?files=" + requestString;
        }
    });

    request.send();
});

document.getElementById("delete").addEventListener("click", (e) => {
    e.preventDefault();

    var boxes = document.querySelectorAll(".check");
    var requestString = "";

    for (let i = 0; i < boxes.length; i++) {
        if(boxes[i].checked) {
            requestString += boxes[i].name + ";";
        }
    }

    var confirmation = confirm("Удалить выбранные файлы?");

    if(confirmation) {
        var request = new XMLHttpRequest();
        request.open("GET", "/delete?files=" + requestString, false);

        request.addEventListener("load", (e) => {
            var code = request.status;
            var errorBox = document.getElementById("error");
            var errorMessage = document.getElementById("error_message");
        
            if(code === 404) {
                errorBox.removeAttribute("hidden");
                errorMessage.innerText = "Файл не найден";
            }

            window.location.reload();
        });

        request.send();
    }

});