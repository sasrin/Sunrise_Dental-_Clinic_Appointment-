package com.dental.servlet;

import com.dental.DBConnection;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet("/api/billing")
public class BillingServlet extends HttpServlet {
    
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
        PreparedStatement costStmt = null;
        PreparedStatement configStmt = null;
        ResultSet rs = null;
        ResultSet costRs = null;
        ResultSet configRs = null;
        
        try {
            conn = DBConnection.getConnection();
            
            // Get appointment details
            String sql = "SELECT appointment_number, patient_name, treatment_type, appointment_date " +
                       "FROM appointments WHERE appointment_number = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, appointmentNumber);
            rs = stmt.executeQuery();
            
            if (rs.next()) {
                String treatmentType = rs.getString("treatment_type");
                
                // Get treatment cost
                String costSql = "SELECT cost FROM treatment_costs WHERE treatment_type = ?";
                costStmt = conn.prepareStatement(costSql);
                costStmt.setString(1, treatmentType);
                costRs = costStmt.executeQuery();
                
                double treatmentCost = 0.0;
                if (costRs.next()) {
                    treatmentCost = costRs.getDouble("cost");
                }
                
                // Get consultation fee
                String configSql = "SELECT value FROM config WHERE key_name = 'consultation_fee'";
                configStmt = conn.prepareStatement(configSql);
                configRs = configStmt.executeQuery();
                
                double consultationFee = 500.0;
                if (configRs.next()) {
                    consultationFee = Double.parseDouble(configRs.getString("value"));
                }
                
                double total = treatmentCost + consultationFee;
                
                StringBuilder json = new StringBuilder();
                json.append("{\"success\": true, \"billing\": {");
                json.append("\"appointmentNumber\": \"").append(escapeJson(rs.getString("appointment_number"))).append("\", ");
                json.append("\"patientName\": \"").append(escapeJson(rs.getString("patient_name"))).append("\", ");
                json.append("\"treatmentType\": \"").append(escapeJson(treatmentType)).append("\", ");
                json.append("\"appointmentDate\": \"").append(rs.getDate("appointment_date")).append("\", ");
                json.append("\"treatmentCost\": \"").append(String.format("%.2f", treatmentCost)).append("\", ");
                json.append("\"consultationFee\": \"").append(String.format("%.2f", consultationFee)).append("\", ");
                json.append("\"total\": \"").append(String.format("%.2f", total)).append("\"");
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
                if (costRs != null) costRs.close();
                if (configRs != null) configRs.close();
                if (stmt != null) stmt.close();
                if (costStmt != null) costStmt.close();
                if (configStmt != null) configStmt.close();
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
