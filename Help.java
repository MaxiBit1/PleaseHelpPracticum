package http;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import data.Epic;
import data.SubTask;
import data.Task;
import manager.Managers;
import manager.TaskManager;

import java.io.*;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;


public class HttpTaskServer {

    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(PORT), 0);
        httpServer.createContext("/tasks", new TaskHander());
        httpServer.start();

        System.out.println("Server start!!!");
    }

    static class TaskHander implements HttpHandler {
        private Gson gson = new Gson();
        private TaskManager taskManager = Managers.FileBackedTaskManagersDefault();
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            switch (method) {
                case "GET": {
                    getMethod(exchange);
                    break;
                }
                case "POST": {
                    postMethod(exchange);
                    break;
                }
            }


        }


        private void getMethod(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] arrPath = path.split("/");
            if(arrPath[2].equals("task")) {
                if(arrPath[3].isBlank()) {
                    writeResponse(exchange, taskManager.getStorageTask().toString(), 200);
                } else {
                    int idTask = Integer.parseInt(arrPath[3].substring(4));
                    writeResponse(exchange,taskManager.getTask(idTask).toString(), 200);
                }
            } else if (arrPath[2].equals("subtask")) {
                if(arrPath[3].isBlank()) {
                    writeResponse(exchange, taskManager.getStorageSubtask().toString(), 200);
                } else if(arrPath[3].equals("epic")) {
                    int idTask = Integer.parseInt(arrPath[4].substring(4));
                    writeResponse(exchange,taskManager.getEpicSubtask(idTask).toString(), 200);
                } else {
                    int idTask = Integer.parseInt(arrPath[3].substring(4));
                    writeResponse(exchange,taskManager.getSubtask(idTask).toString(), 200);
                }
            } else if(arrPath[2].equals("epic")) {
                if(arrPath[3].isBlank()) {
                    writeResponse(exchange, taskManager.getStorageEpic().toString(), 200);
                } else {
                    int idTask = Integer.parseInt(arrPath[3].substring(4));
                    writeResponse(exchange,taskManager.getEpic(idTask).toString(), 200);
                }
            } else if (arrPath[2].equals("history")) {
                writeResponse(exchange, taskManager.getHistory().toString(), 200);
            } else {
                writeResponse(exchange, taskManager.getPrioritizedTask().toString(), 200);
            }
        }

        private void postMethod(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String[] arrPath = path.split("/");
            InputStream inputStream = exchange.getRequestBody();
            String jsonTask = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            Gson gsonBuild = new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
                    .create();
            if(arrPath[2].equals("task")) {

                Task task = gsonBuild.fromJson(jsonTask, Task.class);

                if(task.getId() != 0) {
                    taskManager.setUpdateTask(task);
                } else {
                    taskManager.createTasks(task);
                }
            } else if (arrPath[2].equals("subtask")) {
                SubTask subTask = gson.fromJson(jsonTask, SubTask.class);
                if(subTask.getId() != 0) {
                    taskManager.setUpdateSubtask(subTask);
                } else {
                    taskManager.createSubtacks(subTask);
                }
            } else  {
                Epic epic = gson.fromJson(jsonTask, Epic.class);
                if(epic.getId() != 0) {
                    taskManager.setUpdateEpic(epic);
                } else {
                    taskManager.createEpic(epic);
                }
            }
        }

        private void writeResponse(HttpExchange exchange,
                                   String response,
                                   int responseCode) throws IOException {
            if(response.isBlank()) {
                exchange.sendResponseHeaders(responseCode, 0);
            } else {
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try(OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
            exchange.close();
        }
    }

    private static class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
        private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy; HH:mm");
        @Override
        public void write(JsonWriter jsonWriter, LocalDateTime localDateTime) throws IOException {
            jsonWriter.value(LocalDateTime.MAX.format(formatter));
        }

        @Override
        public LocalDateTime read(JsonReader jsonReader) throws IOException {
            return LocalDateTime.parse(jsonReader.nextString(), formatter);
        }
    }
}
