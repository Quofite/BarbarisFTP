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
        window.location.href = "http://localhost:8080/download?files=" + requestString;
    });

    request.send();
});