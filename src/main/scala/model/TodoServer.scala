package model

import com.corundumstudio.socketio.listener.{ConnectListener, DataListener}
import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}
import model.database.{Database, DatabaseAPI, TestingDatabase}
import play.api.libs.json.{JsValue, Json}

class TodoServer() {

  val database: DatabaseAPI = if (Configuration.DEV_MODE) {
    new TestingDatabase
  } else {
    new Database
  }

  setNextId()

  var usernameToSocket: Map[String, SocketIOClient] = Map()
  var socketToUsername: Map[SocketIOClient, String] = Map()

  val config: Configuration = new Configuration {
    setHostname("0.0.0.0")
    setPort(8080)
  }

  val server: SocketIOServer = new SocketIOServer(config)

  server.addConnectListener(new ConnectionListener(this))
  server.addEventListener("add_task", classOf[String], new AddTaskListener(this))
  server.addEventListener("complete_task", classOf[String], new CompleteTaskListener(this))
  server.addEventListener("filter_by_label", classOf[String], new FilterListener(this))

  server.start()

  def tasksJSON(): String = {
    val tasks: List[Task] = database.getTasks
    val tasksJSON: List[JsValue] = tasks.map((entry: Task) => entry.asJsValue())
    Json.stringify(Json.toJson(tasksJSON))
  }

  def filteredTasksJSON(label: String): String = {
    val filteredtasks: List[Task] = database.filterByLabel(label)
    val filteredtasksJSON: List[JsValue] = filteredtasks.map((entry: Task) => entry.asJsValue())

    Json.stringify(Json.toJson(filteredtasksJSON))

  }

  def setNextId(): Unit = {
    val tasks = database.getTasks
    if (tasks.nonEmpty) {
      Task.nextId = tasks.map(_.id.toInt).max + 1
    }
  }

}

object TodoServer {
  def main(args: Array[String]): Unit = {
    new TodoServer()
  }
}


class ConnectionListener(server: TodoServer) extends ConnectListener {

  override def onConnect(socket: SocketIOClient): Unit = {
    socket.sendEvent("all_tasks", server.tasksJSON())
  }

}


class AddTaskListener(server: TodoServer) extends DataListener[String] {

  override def onData(socket: SocketIOClient, taskJSON: String, ackRequest: AckRequest): Unit = {
    val task: JsValue = Json.parse(taskJSON)
    val title: String = (task \ "title").as[String]
    val description: String = (task \ "description").as[String]
    val label: String = (task \ "label").as[String]

    server.database.addTask(Task(title, description, label))
    server.server.getBroadcastOperations.sendEvent("all_tasks", server.tasksJSON())
  }

}

class CompleteTaskListener(server: TodoServer) extends DataListener[String] {

  override def onData(socket: SocketIOClient, taskId: String, ackRequest: AckRequest): Unit = {
    server.database.completeTask(taskId)
    server.server.getBroadcastOperations.sendEvent("all_tasks", server.tasksJSON())
  }

}

class FilterListener(server: TodoServer) extends DataListener[String]{
  override def onData(client: SocketIOClient, labelJSON: String, ackSender: AckRequest): Unit = {

    val labelJson: JsValue = Json.parse(labelJSON)
    val label: String = (labelJson \ "label").as[String]
    server.server.getBroadcastOperations.sendEvent("all_tasks", server.filteredTasksJSON(label))
  }
}


