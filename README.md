# Sunrise Dental Clinic Appointment and Patient Management System

A complete web-based appointment and patient management system for Sunrise Dental Clinic in Colombo.

## Technology Stack

- **Frontend**: HTML, CSS, JavaScript
- **Backend**: Java (Servlets)
- **Database**: MySQL

## Features

1. **User Authentication / Login** - Secure login with password hashing
2. **Register New Appointment** - Create patient appointments with validation
3. **Display Appointment Details** - Search and view appointment information
4. **Calculate and Print Bill** - Generate bills with treatment costs and consultation fees
5. **Help Section** - Step-by-step instructions for staff
6. **Exit / Logout** - Secure session management

## Prerequisites

- Java Development Kit (JDK) 11 or higher
- MySQL 8.0 or higher
- A web browser (Chrome, Firefox, Edge, etc.)

**For Tomcat deployment (optional):**
- Apache Maven 3.6 or higher
- Apache Tomcat 9.0 or higher

## Database Setup

1. **Install MySQL** if not already installed
2. **Create the database** by running the schema script:

```bash
mysql -u root -p < database/schema.sql
```

3. **Load sample data** (optional, for testing):

```bash
mysql -u root -p dental_clinic < database/sample_data.sql
```

4. **Update database credentials** in `src/java/com/dental/DBConnection.java` if needed:
   - Default URL: `jdbc:mysql://localhost:3306/dental_clinic`
   - Default User: `root`
   - Default Password: `""` (empty)

## Application Setup

### Option 1: Standalone Server (Recommended - No Tomcat Required)

This project includes a built-in HTTP server that runs without Tomcat.

1. **Setup Database** (follow Database Setup section above)
2. **Run the application**:
   - In IntelliJ IDEA: Right-click `Main.java` → Run 'Main.main()'
   - Or from command line:
     ```bash
     javac -cp lib/servlet-api.jar;lib/mysql-connector-java.jar -d out src/java/com/dental/*.java src/java/com/dental/server/*.java src/java/com/dental/model/*.java src/java/com/dental/util/*.java
     java -cp out;lib/servlet-api.jar;lib/mysql-connector-java.jar com.dental.server.HttpServer
     ```
3. **Access the application**: Open browser to `http://localhost:8080`

### Option 2: Tomcat Deployment

#### 1. Build the Project

Navigate to the project directory and run:

```bash
mvn clean package
```

This will create a WAR file in the `target/` directory.

#### 2. Deploy to Tomcat

**Option A: Manual Deployment**
- Copy `target/dental-appointment.war` to Tomcat's `webapps/` directory
- Start Tomcat server

**Option B: Using Maven Tomcat Plugin**
Add the following to your `pom.xml` and run `mvn tomcat7:deploy`

#### 3. Access the Application

Open your web browser and navigate to:

```
http://localhost:8080/dental-appointment/
```

## Test Account

For development and testing purposes, use the following credentials:

- **Username**: `staff`
- **Password**: `Staff@123`

**Important**: This is a development/test account only. Do not use in production.

## Project Structure

```
Dental_Appointment/
├── database/
│   ├── schema.sql          # Database schema
│   └── sample_data.sql     # Sample test data
├── lib/
│   ├── servlet-api.jar     # Servlet API library
│   └── mysql-connector-java.jar  # MySQL connector
├── src/
│   ├── Main.java           # Entry point for standalone server
│   ├── java/
│   │   └── com/
│   │       └── dental/
│   │           ├── DBConnection.java
│   │           ├── model/
│   │           │   └── Appointment.java
│   │           ├── server/
│   │           │   └── HttpServer.java  # Built-in HTTP server
│   │           ├── servlet/
│   │           │   ├── LoginServlet.java
│   │           │   ├── LogoutServlet.java
│   │           │   ├── AppointmentServlet.java
│   │           │   ├── SearchServlet.java
│   │           │   └── BillingServlet.java
│   │           └── util/
│   │               └── PasswordUtil.java
│   └── main/
│       └── webapp/
│           ├── WEB-INF/
│           │   └── web.xml
│           ├── css/
│           │   └── style.css
│           ├── index.html
│           ├── dashboard.html
│           ├── appointment.html
│           ├── search.html
│           ├── billing.html
│           ├── receipt.html
│           └── help.html
├── pom.xml
└── README.md
```

## Usage Instructions

### Login
1. Enter username and password
2. Click "Login"
3. Upon successful authentication, you'll be redirected to the dashboard

### Register New Appointment
1. Click "Register New Appointment" from dashboard
2. Fill in all required fields:
   - Appointment Number (must be unique)
   - Patient Name
   - Address
   - Contact Number (10 digits)
   - Dentist Name
   - Treatment Type
   - Appointment Date
   - Appointment Time
3. Click "Register Appointment"
4. Success message confirms registration

### Search Appointment
1. Click "Search Appointment" from dashboard
2. Enter appointment number
3. Click "Search"
4. View appointment details if found

### Calculate and Print Bill
1. Click "Calculate & Print Bill" from dashboard
2. Enter appointment number
3. Click "Calculate Bill"
4. View bill summary with:
   - Treatment Cost
   - Consultation Fee
   - Total Amount
5. Click "Print Bill" to view printable receipt
6. Use browser print functionality to print

### Help
1. Click "Help" from dashboard
2. Read step-by-step instructions for all functions

### Logout
1. Click "Logout" button on dashboard
2. Session will be terminated
3. Redirected to login page

## Security Features

- Password hashing using SHA-256 with salt
- Session-based authentication
- Protected API endpoints
- Input validation on both frontend and backend
- SQL injection prevention using prepared statements

## Treatment Costs

The system includes predefined treatment costs:

| Treatment Type | Cost (LKR) |
|----------------|------------|
| Cleaning | 2,000 |
| Filling | 3,500 |
| Root Canal | 15,000 |
| Extraction | 2,500 |
| Crown | 12,000 |
| Denture | 18,000 |
| Braces | 45,000 |
| Whitening | 8,000 |
| Checkup | 1,500 |
| X-Ray | 1,000 |

**Consultation Fee**: LKR 500 (fixed)

## Troubleshooting

### Database Connection Issues
- Ensure MySQL server is running
- Verify database credentials in `DBConnection.java`
- Check that the database `dental_clinic` exists

### Standalone Server Issues
- Ensure port 8080 is not already in use
- Check that Java 11 or higher is installed
- Verify that `lib/servlet-api.jar` and `lib/mysql-connector-java.jar` exist
- If using IntelliJ, reload the project after adding libraries

### Build Errors (Maven)
- Ensure JDK 11 or higher is installed
- Verify Maven is properly configured
- Check that all dependencies are available

### Deployment Issues (Tomcat)
- Verify Tomcat is running
- Check Tomcat logs for errors
- Ensure WAR file is properly deployed

## Development Notes

- This is a student project for assessment purposes
- Uses basic authentication (consider implementing more robust authentication for production)
- All monetary values are in Sri Lankan Rupees (LKR)
- Sample data is fictional and should not be used in production

## License

This project is created for educational purposes.

## Contact

For questions or issues, please contact the development team.
