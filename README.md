
# **Helpdesk Web App**

A spring Boot + Thymeleaf web application to manage IT Helpdesk tickets.


## **Overview**
Registered users can add and manage events (issues) they have reported or view other users' reports and their statuses. Admins can resolve, edit and print events and users to PDFs.
## **Features**

**1. User Authentication**
- Users register with an email, username, name, surname, password and are assigned a basic “user” role.
- Passwords are encrypted
- Login/logout managed by Spring Security.
**2. Users Features**
- **Add Tickets:** Category, title, description, emergency status, detection date, downtime (in hours).
- **View Tickets:** Filter and sort the existing tickets or your own tickets.
- **Edit Tickets:** Users can edit only their own events.
- **Delete Tickets:** Users can delete only their own events.
**3. Admin Features**
- **Resolve Tickets:** They can change ticket status (Open, In Progress, Resolved, Closed, Reopened) with comment.
- **Edit Users:** Change user details including password and roles.
- **Print PDF:** Admin can generate PDF files of users and events (with date ranges for events and user roles). Useful for reporting and auditing.
## Tech Stack

* **Language:** Java 23
* **Framework:** Spring Boot (v3.4.0)
* **Templates:** Thymeleaf
* **Security:** Spring Security
* **Data:** PostgreSQL
* **Build Tool:** Maven
## Installation
**1. Clone the Repo**
* git clone https://github.com/WiktorG3/helpdeskSpringBoot
* cd helpdesk-dashboard
**2. Configure Database**
* Install or run a PostgreSQL instance.
* Create a database, e.g. helpdeskdb
* Update spring.datasource.url, username, password in application.properties.
**3. Build & Run**\
If using Maven:
* mvn clean install
* mvn spring-boot:run
If using Gradle:
* ./gradlew bootRun
**4. Access the App**
* Navigate to http://localhost:8080.
* Register a new user or log in with an existing account. For admin actions, create an account and change its role manually in the database.


## **Usage/Examples**
**1. Register/Login**
* http://localhost:8080/register
* http://localhost:8080/login
**2. Dashboard**
* Displays counts of emergency events, statuses, etc.
**3. Navbar**
* Add a new event or view events, users.
**4. Admin Panel**
* Resolve Events: Update status and add comment for users to see.
* Edit Users: Change their roles and data.
* Print Results: Generate PDFs of users or events.
**5. Logout**
* Ends the current session


## **Endpoints**
| **Endpoint**  | **Method** | **Description** |
| ------------- | ------------- | ------------- |
| /register  | GET  | Register Page  |
| /register  | POST  | Register new user  |
| /login  | GET  | Login Page  |
| /dashboard  | GET  | Main page with stats  |
| /addEvent  | GET  | Add ticket Form Page  |
| /addEvent  | POST  | Add new ticket  |
| /viewEvents  | GET  | Load View Events Page  |
| /viewComment/{eventId}  | GET  | Modal Data Load  |
| /userEvents | GET  | Load Logged User Events Page  |
| /userEvents/{id}  | POST  | User updates event  |
| /userEvents/{id}  | DELETE  | User deletes event  |
| /editEvent/{id}  | GET  | Modal Edit Event Load  |
| /editEvent/{id}  | POST  | Modal Edit Event Update  |
| /viewUsers  | GET  | Load View Users Page  |
| /editUsers  | GET  | Load Edit Users Page  |
| /editUsers  | POST  | Admin updates user data  |
| /resolveEvents  | GET  | Load Resolve Page  |
| /resolveEvents/{id}  | POST  | Admin updates event status and comment  |
| /printResults  | GET  | Load Print Page  |
| /printResults  | GET  | Admin PDF of users filtered by role |
| /printResults  | GET  | Admin PDF of events by date range  |

## **Running Tests**

**With Maven:**\
mvn test

**With Gradle:**\
./gradlew test
## **License**

MIT License

**Copyright (c) 2024 Wiktor Gronostaj**

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
## **Author**

Wiktor Gronostaj [@WiktorG3](https://github.com/WiktorG3)

