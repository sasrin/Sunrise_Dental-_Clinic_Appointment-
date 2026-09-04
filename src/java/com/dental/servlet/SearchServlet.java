package com.dental.servlet;

import com.dental.DBConnection;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/api/search")
public class SearchServlet extends HttpServlet {
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
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
        
        if (appointmentNumber == null || appointmentNumber.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.print("{\"success\": false, \"message\": \"Appointment number is required\"}");
            out.flush();
            return;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBConnection.getConnection();
            String sql = "SELECT appointment_number, patient_name, address, contact_number, " +
                       "dentist_name, treatment_type, appointment_date, appointment_time " +
                       "FROM appointments WHERE appointment_number = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, appointmentNumber);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                StringBuilder json = new StringBuilder();
                json.append("{\"success\": true, \"appointment\": {");
                json.append("\"appointmentNumber\": \"").append(escapeJson(rs.getString("appointment_number"))).append("\", ");
                json.append("\"patientName\": \"").append(escapeJson(rs.getString("patient_name"))).append("\", ");
                json.append("\"address\": \"").append(escapeJson(rs.getString("address"))).append("\", ");
                json.append("\"contactNumber\": \"").append(escapeJson(rs.getString("contact_number"))).append("\", ");
                json.append("\"dentistName\": \"").append(escapeJson(rs.getString("dentist_name"))).append("\", ");
                json.append("\"treatmentType\": \"").append(escapeJson(rs.getString("treatment_type"))).append("\", ");
                json.append("\"appointmentDate\": \"").append(rs.getDate("appointment_date")).append("\", ");
                json.append("\"appointmentTime\": \"").append(rs.getTime("appointment_time")).append("\"");
                json.append("}}");
                out.print(json.toString());
            } else {
                out.print("{\"success\": false, \"message\": \"Appointment not found\"}");
            }
            
        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.print("{\"success\": false, \"message\": \"Database error occurred\"}");
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        out.flush();
    }
    
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
