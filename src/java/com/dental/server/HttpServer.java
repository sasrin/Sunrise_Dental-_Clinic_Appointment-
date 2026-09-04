package com.dental.server;

import com.dental.DBConnection;
import com.dental.util.PasswordUtil;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HttpServer {
    private static final int PORT = 8080;
    private static final String WEBAPP_DIR = "src/main/webapp";
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    
    private static Map<String, HttpSession> sessions = new HashMap<>();
    
    public static void main(String[] args) throws IOException {
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(PORT), 0);
        
        // Static file handler
        server.createContext("/", new StaticFileHandler());
        
        // API handlers
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/logout", new LogoutHandler());
        server.createContext("/api/appointments", new AppointmentHandler());
        server.createContext("/api/search", new SearchHandler());
        server.createContext("/api/billing", new BillingHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Server started on http://localhost:" + PORT);
        System.out.println("Press Ctrl+C to stop the server");
    }
    
    static class StaticFileHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            
            // Default to index.html for root
            if (path.equals("/") || path.equals("")) {
                path = "/index.html";
            }
            
            // Remove leading slash
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            File file = new File(WEBAPP_DIR, path);
            
            if (!file.exists() || file.isDirectory()) {
                // Try index.html if directory
                File indexFile = new File(file, "index.html");
                if (indexFile.exists()) {
                    file = indexFile;
                } else {
                    sendResponse(exchange, 404, "text/plain", "File not found");
                    return;
                }
            }
            
            String contentType = getContentType(file.getName());
            byte[] content = Files.readAllBytes(file.toPath());
            
            sendResponse(exchange, 200, contentType, content);
        }
    }
    
    static class LoginHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\": false, \"message\": \"Method not allowed\"}");
                return;
            }
            
            String body = new String(exchange.getRequestBody().readAllBytes());
            Map<String, String> params = parseFormData(body);
            
            String username = params.get("username");
            String password = params.get("password");
            
            if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"success\": false, \"message\": \"Username and password are required\"}");
                return;
            }
            
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "SELECT password_hash FROM users WHERE username = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    if (PasswordUtil.verifyPassword(password, storedHash)) {
                        String sessionId = UUID.randomUUID().toString();
                        sessions.put(sessionId, new HttpSession(username));
                        
                        // Set session cookie
                        exchange.getResponseHeaders().set("Set-Cookie", "session=" + sessionId + "; Path=/");
                        sendResponse(exchange, 200, "application/json", "{\"success\": true, \"message\": \"Login successful\"}");
                    } else {
                        sendResponse(exchange, 401, "application/json", "{\"success\": false, \"message\": \"Invalid username or password\"}");
                    }
                } else {
                    sendResponse(exchange, 401, "application/json", "{\"success\": false, \"message\": \"Invalid username or password\"}");
                }
                
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                System.err.println("Database error in LoginHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\": false, \"message\": \"Database error: " + e.getMessage() + "\"}");
            } catch (Exception e) {
                System.err.println("Error in LoginHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\": false, \"message\": \"Error: " + e.getMessage() + "\"}");
            }
        }
    }
    
    static class LogoutHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\": false, \"message\": \"Method not allowed\"}");
                return;
            }
            
            String sessionId = getSessionId(exchange);
            if (sessionId != null) {
                sessions.remove(sessionId);
            }
            
            exchange.getResponseHeaders().set("Set-Cookie", "session=; Path=/; Max-Age=0");
            sendResponse(exchange, 200, "application/json", "{\"success\": true, \"message\": \"Logged out successfully\"}");
        }
    }
    
    static class AppointmentHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\": false, \"message\": \"Method not allowed\"}");
                return;
            }
            
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, 401, "application/json", "{\"success\": false, \"message\": \"Unauthorized access\"}");
                return;
            }
            
            String body = new String(exchange.getRequestBody().readAllBytes());
            Map<String, String> params = parseFormData(body);
            
            String appointmentNumber = params.get("appointmentNumber");
            String patientName = params.get("patientName");
            String address = params.get("address");
            String contactNumber = params.get("contactNumber");
            String dentistName = params.get("dentistName");
            String treatmentType = params.get("treatmentType");
            String appointmentDate = params.get("appointmentDate");
            String appointmentTime = params.get("appointmentTime");
            
            // Validation
            if (appointmentNumber == null || appointmentNumber.isEmpty() ||
                patientName == null || patientName.isEmpty() ||
                address == null || address.isEmpty() ||
                contactNumber == null || contactNumber.isEmpty() ||
                dentistName == null || dentistName.isEmpty() ||
                treatmentType == null || treatmentType.isEmpty() ||
                appointmentDate == null || appointmentDate.isEmpty() ||
                appointmentTime == null || appointmentTime.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"success\": false, \"message\": \"All fields are required\"}");
                return;
            }
            
            if (!contactNumber.matches("^[0-9]{10}$")) {
                sendResponse(exchange, 400, "application/json", "{\"success\": false, \"message\": \"Invalid contact number format\"}");
                return;
            }
            
            try {
                Connection conn = DBConnection.getConnection();
                
                // Check for duplicate
                String checkSql = "SELECT appointment_number FROM appointments WHERE appointment_number = ?";
                PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                checkStmt.setString(1, appointmentNumber);
                ResultSet rs = checkStmt.executeQuery();
                
                if (rs.next()) {
                    sendResponse(exchange, 409, "application/json", "{\"success\": false, \"message\": \"Appointment number already exists\"}");
                    rs.close();
                    checkStmt.close();
                    conn.close();
                    return;
                }
                rs.close();
                checkStmt.close();
                
                // Insert
                String insertSql = "INSERT INTO appointments (appointment_number, patient_name, address, " +
                                  "contact_number, dentist_name, treatment_type, appointment_date, appointment_time) " +
                                  "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertSql);
                insertStmt.setString(1, appointmentNumber);
                insertStmt.setString(2, patientName);
                insertStmt.setString(3, address);
                insertStmt.setString(4, contactNumber);
                insertStmt.setString(5, dentistName);
                insertStmt.setString(6, treatmentType);
                insertStmt.setDate(7, java.sql.Date.valueOf(LocalDate.parse(appointmentDate, dateFormatter)));
                insertStmt.setTime(8, Time.valueOf(LocalTime.parse(appointmentTime, timeFormatter)));
                
                int rowsAffected = insertStmt.executeUpdate();
                insertStmt.close();
                conn.close();
                
                if (rowsAffected > 0) {
                    sendResponse(exchange, 200, "application/json", "{\"success\": true, \"message\": \"Appointment registered successfully\"}");
                } else {
                    sendResponse(exchange, 500, "application/json", "{\"success\": false, \"message\": \"Failed to register appointment\"}");
                }
            } catch (SQLException e) {
                System.err.println("Database error in AppointmentHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\": false, \"message\": \"Database error: " + e.getMessage() + "\"}");
            } catch (Exception e) {
                System.err.println("Error in AppointmentHandler: " + e.getMessage());
                e.printStackTrace();
                sendResponse(exchange, 500, "application/json", "{\"success\": false, \"message\": \"Error: " + e.getMessage() + "\"}");
            }
        }
    }
    
    static class SearchHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\": false, \"message\": \"Method not allowed\"}");
                return;
            }
            
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, 401, "application/json", "{\"success\": false, \"message\": \"Unauthorized access\"}");
                return;
            }
            
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String appointmentNumber = params.get("appointmentNumber");
            
            if (appointmentNumber == null || appointmentNumber.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"success\": false, \"message\": \"Appointment number is required\"}");
                return;
            }
            
            try {
                Connection conn = DBConnection.getConnection();
                String sql = "SELECT appointment_number, patient_name, address, contact_number, " +
                           "dentist_name, treatment_type, appointment_date, appointment_time " +
                           "FROM appointments WHERE appointment_number = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, appointmentNumber);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String json = String.format(
                        "{\"success\": true, \"appointment\": {" +
                        "\"appointmentNumber\": \"%s\", " +
                        "\"patientName\": \"%s\", " +
                        "\"address\": \"%s\", " +
                        "\"contactNumber\": \"%s\", " +
                        "\"dentistName\": \"%s\", " +
                        "\"treatmentType\": \"%s\", " +
                        "\"appointmentDate\": \"%s\", " +
                        "\"appointmentTime\": \"%s\"}}",
                        escapeJson(rs.getString("appointment_number")),
                        escapeJson(rs.getString("patient_name")),
                        escapeJson(rs.getString("address")),
                        escapeJson(rs.getString("contact_number")),
                        escapeJson(rs.getString("dentist_name")),
                        escapeJson(rs.getString("treatment_type")),
                        rs.getDate("appointment_date"),
                        rs.getTime("appointment_time")
                    );
                    sendResponse(exchange, 200, "application/json", json);
                } else {
                    sendResponse(exchange, 200, "application/json", "{\"success\": false, \"message\": \"Appointment not found\"}");
                }
                
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                sendResponse(exchange, 500, "application/json", "{\"success\": false, \"message\": \"Database error occurred\"}");
            }
        }
    }
    
    static class BillingHandler implements com.sun.net.httpserver.HttpHandler {
        @Override
        public void handle(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
            if (!"GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"success\": false, \"message\": \"Method not allowed\"}");
                return;
            }
            
            if (!isAuthenticated(exchange)) {
                sendResponse(exchange, 401, "application/json", "{\"success\": false, \"message\": \"Unauthorized access\"}");
                return;
            }
            
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            String appointmentNumber = params.get("appointmentNumber");
            
            if (appointmentNumber == null || appointmentNumber.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"success\": false, \"message\": \"Appointment number is required\"}");
                return;
            }
            
            try {
                Connection conn = DBConnection.getConnection();
                
                // Get appointment
                String sql = "SELECT appointment_number, patient_name, treatment_type, appointment_date " +
                           "FROM appointments WHERE appointment_number = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, appointmentNumber);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    String treatmentType = rs.getString("treatment_type");
                    
                    // Get treatment cost
                    String costSql = "SELECT cost FROM treatment_costs WHERE treatment_type = ?";
                    PreparedStatement costStmt = conn.prepareStatement(costSql);
                    costStmt.setString(1, treatmentType);
                    ResultSet costRs = costStmt.executeQuery();
                    
                    double treatmentCost = 0.0;
                    if (costRs.next()) {
                        treatmentCost = costRs.getDouble("cost");
                    }
                    costRs.close();
                    costStmt.close();
                    
                    // Get consultation fee
                    String configSql = "SELECT value FROM config WHERE key_name = 'consultation_fee'";
                    PreparedStatement configStmt = conn.prepareStatement(configSql);
                    ResultSet configRs = configStmt.executeQuery();
                    
                    double consultationFee = 500.0;
                    if (configRs.next()) {
                        consultationFee = Double.parseDouble(configRs.getString("value"));
                    }
                    configRs.close();
                    configStmt.close();
                    
                    double total = treatmentCost + consultationFee;
                    
                    String json = String.format(
                        "{\"success\": true, \"billing\": {" +
                        "\"appointmentNumber\": \"%s\", " +
                        "\"patientName\": \"%s\", " +
                        "\"treatmentType\": \"%s\", " +
                        "\"appointmentDate\": \"%s\", " +
                        "\"treatmentCost\": \"%.2f\", " +
                        "\"consultationFee\": \"%.2f\", " +
                        "\"total\": \"%.2f\"}}",
                        escapeJson(rs.getString("appointment_number")),
                        escapeJson(rs.getString("patient_name")),
                        escapeJson(treatmentType),
                        rs.getDate("appointment_date"),
                        treatmentCost,
                        consultationFee,
                        total
                    );
                    sendResponse(exchange, 200, "application/json", json);
                } else {
                    sendResponse(exchange, 200, "application/json", "{\"success\": false, \"message\": \"Appointment not found\"}");
                }
                
                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                sendResponse(exchange, 500, "application/json", "{\"success\": false, \"message\": \"Database error occurred\"}");
            }
        }
    }
    
    private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
    
    private static void sendResponse(com.sun.net.httpserver.HttpExchange exchange, int statusCode, String contentType, byte[] response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
    
    private static String getContentType(String filename) {
        if (filename.endsWith(".html")) return "text/html";
        if (filename.endsWith(".css")) return "text/css";
        if (filename.endsWith(".js")) return "application/javascript";
        if (filename.endsWith(".json")) return "application/json";
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) return "image/jpeg";
        if (filename.endsWith(".gif")) return "image/gif";
        if (filename.endsWith(".svg")) return "image/svg+xml";
        return "text/plain";
    }
    
    private static Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    params.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
    
    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) return params;
        
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                try {
                    params.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return params;
    }
    
    private static String getSessionId(com.sun.net.httpserver.HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                cookie = cookie.trim();
                if (cookie.startsWith("session=")) {
                    return cookie.substring(8);
                }
            }
        }
        return null;
    }
    
    private static boolean isAuthenticated(com.sun.net.httpserver.HttpExchange exchange) {
        String sessionId = getSessionId(exchange);
        return sessionId != null && sessions.containsKey(sessionId);
    }
    
    private static String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    static class HttpSession {
        private String username;
        private long createdAt;
        
        public HttpSession(String username) {
            this.username = username;
            this.createdAt = System.currentTimeMillis();
        }
        
        public String getUsername() {
            return username;
        }
    }
}
