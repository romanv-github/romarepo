package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class MyHandlers implements HttpHandler {

    public static final String ROOT_FOLDER = "C:\\tmp\\mavenLocal";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String requestMethod = exchange.getRequestMethod();
        switch (requestMethod) {
            case "GET" -> handleGet(exchange);
            case "PUT" -> handlePut(exchange);
            default -> handleError(exchange);
        }
    }

    public void handleGet(HttpExchange exchange) throws IOException {
        System.out.println("<< Received GET request >>");
        //String requestedFile = exchange.getRequestURI().getPath().substring("/files".length());
        String requestedFile = exchange.getRequestURI().getPath().replace("/myrepo", "");
        System.out.println("This file was requested: " + requestedFile);
        Path filePath = Path.of(ROOT_FOLDER, requestedFile);
        System.out.println("Local path to the file is: " + filePath);

        if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
            System.out.println("> The file exists!");
            byte[] fileBytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);

            exchange.getResponseHeaders().set("Content-Type", contentType != null ? contentType : "application/octet-stream");
            exchange.sendResponseHeaders(200, fileBytes.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(fileBytes);
            }
        } else {
            System.out.println("!!! File not found !!!");
            exchange.sendResponseHeaders(404, -1); // 404 Not Found
        }
    }

    public void handlePut(HttpExchange exchange) throws IOException {
        System.out.println("<< Received PUT request >>");
        String fileName = exchange.getRequestURI().getPath().replace("/myrepo", "");
        System.out.println("Attempting to upload the file: " + fileName);

        if (fileName.isEmpty()) {
            exchange.sendResponseHeaders(400, -1); // 400 Bad Request
            return;
        }

//        String[] Folders = fileName.split("/");
//        StringBuilder currentFolder = new StringBuilder(ROOT_FOLDER);
//        for (int i = 1; i < Folders.length - 2; i++) {
//            currentFolder.append("\\").append(Folders[i]);
//            System.out.println("> Current folder: " + currentFolder);
//            Path currentFolderPath = Path.of(currentFolder.toString());
//            if (!Files.exists(currentFolderPath) || !Files.isDirectory(currentFolderPath)) {
//                File createDirectory = new File(currentFolder.toString());
//                if (createDirectory.mkdirs()) {
//                    System.out.println(">>> Current folder DOES NOT exist and was created successfully.");
//                } else {
//                    System.err.println("!!! Failed to create directory !!!");
//                }
//            }
//        }

        int lastSlashIndex = fileName.lastIndexOf("/");
        String FolderPath = "placeholder";
        if (lastSlashIndex != -1) {
            FolderPath = fileName.substring(0, lastSlashIndex);
            System.out.println("Resulting string: " + FolderPath);
        } else {
            System.out.println("The string does not contain any '/'.");
        }
        Path filePath = Path.of(ROOT_FOLDER, FolderPath);
        System.out.println("Local path to the file is: " + filePath);
        File directory = new File(ROOT_FOLDER + fileName);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        filePath = Path.of(ROOT_FOLDER, fileName);
        try (InputStream inputStream = exchange.getRequestBody();
             FileOutputStream outputStream = new FileOutputStream(filePath.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        exchange.sendResponseHeaders(201, -1); // 201 Created
    }

    private void handleError(HttpExchange exchange) throws IOException {
        var responseBytes = "<html><body>Error 451</body></html>".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Length", Integer.toString(responseBytes.length));
        exchange.sendResponseHeaders(451, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}