package com.dental.servlet;

import com.dental.DBConnection;
import com.dental.model.Appointment;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/api/appointments")
public class AppointmentServlet extends HttpServlet {
    
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        
        // Check authentication
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("authenticated") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.print("{\"success\": false, \"message\": \"Unauthorized access\"}");
            out.flush();
            return;
        }
        
        String appointmentNumber = request.getParameter("appointmentNumber");
        String patientName = request.getParameter("patientName");
        String address = request.getParameter("address");
        String contactNumber = request.getParameter("contactNumber");
        String dentistName = request.getParameter("dentistName");
        String treatmentType = request.getParameter("treatmentType");
        String appointmentDate = request.getParameter("appointmentDate");
        String appointmentTime = request.getParameter("appointmentTime");
        
        // Backend validation
        if (appointmentNumber == null || appointmentNumber.isEmpty() ||
            patientName == null || patientName.isEmpty() ||
            address == null || address.isEmpty() ||
            contactNumber == null || contactNumber.isEmpty() ||
            dentistName == null || dentistName.isEmpty() ||
            treatmentType == null || treatmentType.isEmpty() ||
            appointmentDate == null || appointmentDate.isEmpty() ||
            appointmentTime == null || appointmentTime.isEmpty()) {
            
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"All fields are required\"}");
            out.flush();
            return;
        }
        
        // Validate contact number format
        if (!contactNumber.matches("^[0-9]{10}$")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"Invalid contact number format\"}");
            out.flush();
            return;
        }
        
        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            
            // Check for duplicate appointment number
            String checkSql = "SELECT appointment_number FROM appointments WHERE appointment_number = ?";
            checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, appointmentNumber);
            rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                out.print("{\"success\": false, \"message\": \"Appointment number already exists\"}");
                out.flush();
                return;
            }
            
            // Insert new appointment
            String insertSql = "INSERT INTO appointments (appointment_number, patient_name, address, " +
                              "contact_number, dentist_name, treatment_type, appointment_date, appointment_time) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            insertStmt = conn.prepareStatement(insertSql);
            insertStmt.setString(1, appointmentNumber);
            insertStmt.setString(2, patientName);
            insertStmt.setString(3, address);
            insertStmt.setString(4, contactNumber);
            insertStmt.setString(5, dentistName);
            insertStmt.setString(6, treatmentType);
            insertStmt.setDate(7, Date.valueOf(LocalDate.parse(appointmentDate, dateFormatter)));
            insertStmt.setTime(8, Time.valueOf(LocalTime.parse(appointmentTime, timeFormatter)));
            
            int rowsAffected = insertStmt.executeUpdate();
            
            if (rowsAffected > 0) {
                out.print("{\"success\": true, \"message\": \"Appointment registered successfully\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.print("{\"success\": false, \"message\": \"Failed to register appointment\"}");
            }
            
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"Database error occurred\"}");
        } finally {
            try {
                if (rs != null) rs.close();
                if (checkStmt != null) checkStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        out.flush();
    }
}
