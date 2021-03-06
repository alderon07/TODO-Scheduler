const socket = io.connect("http://localhost:8080", {transports: ['websocket']});

socket.on('all_tasks', displayTasks);
socket.on('message', displayMessage);

function displayMessage(newMessage) {
    document.getElementById("message").innerHTML = newMessage;
}

function displayTasks(tasksJSON) {
    const tasks = JSON.parse(tasksJSON);
    let formattedTasks = "";
    for (const task of tasks) {
        formattedTasks += "<hr/>";
        formattedTasks += "<b>" + task['title'] + "</b> - " + task['description'] + " - @" + task['label'] + "<br/>" ;
        formattedTasks += "<button onclick='completeTask(\"" + task['id'] + "\")'>Task Complete</button>";
    }
    document.getElementById("tasks").innerHTML = formattedTasks;
}

function filterByLabel() {
    let label = document.getElementById("filter-by-label").value;
    socket.emit("filter_by_label", JSON.stringify({"label": label}));
    document.getElementById("filter-by-label").value = "";
}

function addTask() {
    let title = document.getElementById("title").value;
    let desc = document.getElementById("desc").value;
    let label = document.getElementById("label").value;
    socket.emit("add_task", JSON.stringify({"title": title, "description": desc, "label": label}));
    document.getElementById("title").value = "";
    document.getElementById("desc").value = "";
    document.getElementById("label").value = "";
}

function completeTask(taskId) {
    socket.emit("complete_task", taskId);
}
