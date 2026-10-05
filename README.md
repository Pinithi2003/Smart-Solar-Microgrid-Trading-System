# Smart Solar Microgrid Trading System

## SE4040 – Enterprise Application Development

**Year 4 Semester 2 – 2026**

A client-server based Smart Solar Microgrid Trading System developed as a group project for the SE4040 Enterprise Application Development module.

The system provides a centralized platform for managing solar microgrid stations, energy booking slots, reservations, users, and energy-transfer operations through a Web Application, Native Android Mobile Application, and centralized RESTful Web API.

---

## 1. Project Overview

The **Smart Solar Microgrid Trading System** is an end-to-end client-server application designed to support the management and trading of solar energy through a centralized microgrid service.

The system consists of three main components:

* Web Application
* Native Android Mobile Application
* Centralized Web API / Web Service

The Web Application provides interfaces for Backoffice users and Grid Operators, while the Android application provides functionality for Solar Prosumers and Grid Operators.

All major business logic and database operations are handled through the centralized Web API following the **FAT Service architecture pattern**.

The system uses **MongoDB** as the server-side NoSQL database and the Web API is designed to be hosted on a **Windows IIS Server**.

---

## 2. GitHub Repository

### Main Repository

**GitHub Repository:**

https://github.com/Pinithi2003/Smart-Solar-Microgrid-Trading-System

The repository contains the source code for the Web Application, Web API, and Native Android Mobile Application.

### Repository Structure

```text
Smart-Solar-Microgrid-Trading-System/
│
├── Backend/
│   └── SmartSolarMicrogridAPI/
│
├── Frontend/
│   ├── SmartSolarUsersUI/
│   ├── SmartSolarStationUI/
│   ├── SmartSolarReservationsUI/
│   └── SmartSolarFieldOpsUI/
│
├── Mobile/
│   └── AndroidApp/
│
├── .github/
│   └── workflows/
│
├── .gitignore
├── CONTRIBUTING.md
└── README.md
```

---

# 3. System Architecture

The system follows a **client-server architecture**.

```text
                   ┌─────────────────────────┐
                   │      Web Application     │
                   │                         │
                   │  Backoffice             │
                   │  Grid Operator          │
                   └────────────┬────────────┘
                                │
                                │ REST API
                                ▼
                   ┌─────────────────────────┐
                   │   ASP.NET Core Web API  │
                   │                         │
                   │ Authentication          │
                   │ Authorization            │
                   │ Business Logic            │
                   │ Validation                │
                   │ Reservation Rules         │
                   │ User Management           │
                   │ Station Management        │
                   └────────────┬────────────┘
                                │
                                │ MongoDB Driver
                                ▼
                   ┌─────────────────────────┐
                   │        MongoDB           │
                   │       NoSQL DB           │
                   │                         │
                   │ Users                    │
                   │ SolarStationInfo         │
                   │ EnergyBookingSlots       │
                   │ EnergyReservation        │
                   └─────────────────────────┘
                                ▲
                                │
                                │ REST API
                                │
                   ┌────────────┴────────────┐
                   │ Native Android App       │
                   │                         │
                   │ Solar Prosumer           │
                   │ Grid Operator            │
                   └─────────────────────────┘
```

The Web and Mobile applications act as user interface clients and communicate with the centralized Web API through RESTful API calls.

The clients do not directly access the server-side MongoDB database.

---

# 4. Main Technologies

## Backend / Web Service

* C#
* ASP.NET Core Web API
* .NET 8
* MongoDB
* MongoDB Atlas
* RESTful API
* JWT Authentication
* BCrypt Password Hashing
* Swagger / OpenAPI
* IIS Hosting

## Web Application

* HTML
* CSS
* JavaScript
* Bootstrap / responsive UI components
* REST API integration

## Mobile Application

* Native Android
* Kotlin
* XML layouts
* SQLite local persistence
* Retrofit / REST API communication
* Google Maps API
* QR Code functionality

## Development Tools

* Visual Studio Code
* Android Studio
* Git
* GitHub
* MongoDB Atlas
* Swagger

---

# 5. User Roles

The system supports multiple types of users.

## 5.1 Backoffice User

Backoffice users are responsible for administration and management functions.

Main responsibilities include:

* User management
* Prosumer management
* Account activation/deactivation management
* Managing system users
* Accessing administrative dashboards
* Managing microgrid-related information

---

## 5.2 Grid Operator

Grid Operators are responsible for operational activities.

Main responsibilities include:

* Monitoring energy bookings
* Managing operational activities
* Viewing solar stations
* Managing available energy slots
* Accessing booking information
* Scanning transaction QR codes
* Verifying energy-transfer transactions
* Finalizing completed energy-transfer operations

Grid Operators can access both the Web Application and Mobile Application.

---

## 5.3 Solar Prosumer

Solar Prosumers are property owners or users who have solar energy resources.

Main functions include:

* Registering an account
* Logging into the mobile application
* Managing their profile
* Requesting account deactivation
* Viewing available solar microgrid stations
* Searching for stations
* Viewing station information
* Booking energy slots
* Updating reservations
* Cancelling reservations
* Viewing booking history
* Viewing pending and approved bookings
* Receiving reservation confirmation
* Accessing transaction QR information

---

# 6. Web Application

The Web Application provides browser-based interfaces for system management and operational activities.

## 6.1 Authentication

The authentication system provides:

* User login
* JWT-based authentication
* Role-based authorization
* Protected pages
* Unauthorized-access handling
* Session/token management

After authentication, users are redirected to the appropriate interface according to their role.

---

## 6.2 User Management

The User Management functionality allows authorized users to manage system accounts.

Features include:

* Creating users
* Viewing users
* Updating user information
* User role management
* Account activation/deactivation
* Validation of user information
* Protection of management endpoints

---

## 6.3 Prosumer Management

Prosumer accounts use the **National Identity Card (NIC)** as the primary identifier.

The system supports:

* Prosumer registration
* Profile management
* Account status management
* Pending account handling
* Account deactivation requests
* Backoffice activation/reactivation

---

## 6.4 Solar Microgrid Station Management

The station management functionality allows authorized users to manage solar microgrid nodes.

Station information includes:

* Station name
* Location
* GPS coordinates
* Capacity
* Available energy
* Operational status
* Battery/storage information
* Energy slot information

The system prevents the deactivation of a station when active reservations are associated with the station.

---

## 6.5 Energy Slot Management

Energy slots represent available time periods and energy capacity for microgrid operations.

The system allows authorized users to:

* Create energy slots
* View available slots
* Update slot information
* Manage slot availability
* Associate slots with solar stations

---

## 6.6 Reservation Management

The system supports energy reservation operations.

Users can:

* Create reservations
* View reservations
* Update reservations
* Cancel reservations
* View reservation status
* View reservation history

The system applies the required reservation business rules through the central Web API.

---

# 7. Mobile Application

The mobile application is developed as a **pure native Android application**.

The application provides functionality for Solar Prosumers and Grid Operators.

---

## 7.1 Mobile Authentication

The mobile application provides:

* Login
* Registration
* Role-based navigation
* Session management
* Profile management
* Account deactivation functionality

---

## 7.2 Prosumer Registration

New Solar Prosumers can create accounts through the mobile application.

The registration process uses the NIC as the primary identifier.

Registered users can manage their own profile information.

---

## 7.3 Prosumer Dashboard

The dashboard provides users with access to important system functions such as:

* Current bookings
* Pending bookings
* Booking history
* Nearby solar stations
* Profile
* Reservation functions
* QR-related functionality

---

# 8. Solar Station Map

The mobile application provides a map-based interface for viewing nearby solar microgrid stations.

The map uses station GPS coordinates received through the Web API.

Users can:

* View nearby stations
* Search stations
* View station information
* View station capacity/availability
* Select a station for further details

---

# 9. Energy Booking and Reservation

The reservation workflow allows Solar Prosumers to reserve available energy slots.

Typical workflow:

```text
Login
   ↓
View Solar Stations
   ↓
Select Station
   ↓
View Available Energy Slots
   ↓
Select Slot
   ↓
Create Reservation
   ↓
Reservation Confirmation
   ↓
View Booking Details
   ↓
QR Transaction
```

Users can also modify or cancel eligible reservations.

The required business rules are enforced by the central Web API.

---

# 10. QR Code Transaction

After an eligible reservation is confirmed, the system provides transaction information through a QR code.

The Grid Operator can:

1. Open the operator functionality.
2. Scan the Prosumer's transaction QR code.
3. Send the transaction information to the central API.
4. Verify the transaction against server-side data.
5. Complete/finalize the energy-transfer operation.

This allows the energy-transfer process to be verified using server-side information.

---

# 11. Grid Operator Mobile Functionality

Grid Operators can use the mobile application for operational activities.

The operator workflow includes:

```text
Operator Login
      ↓
Operator Dashboard
      ↓
Scan Prosumer QR
      ↓
Verify Transaction
      ↓
Retrieve Server Data
      ↓
Validate Booking
      ↓
Finalize Energy Transfer
```

---

# 12. Booking Dashboard and History

The system provides booking-related information including:

* Current bookings
* Pending bookings
* Approved reservations
* Booking history
* Search/filter functionality
* Reservation status
* Future reservation counts

This allows users and operators to monitor reservation activities.

---

# 13. Backend Web API

The central Web API is developed using **ASP.NET Core Web API with .NET 8**.

The API follows a layered approach where business logic is handled centrally.

Example API areas include:

```text
/api/Auth
/api/users
/api/stations
/api/reservations
/api/...
```

The API provides services for:

* Authentication
* Authorization
* User management
* Prosumer management
* Station management
* Energy slot management
* Reservation management
* Booking operations
* QR verification
* Operational functions

---

# 14. Authentication and Authorization

The application uses **JWT-based authentication**.

The authentication workflow is:

```text
User Login
    ↓
Credentials Sent to API
    ↓
Credentials Validated
    ↓
Password Verification
    ↓
JWT Token Generated
    ↓
Token Returned to Client
    ↓
Client Stores Session
    ↓
Token Sent With Protected API Requests
    ↓
API Validates Token
    ↓
Role-Based Authorization
```

Passwords are protected using BCrypt password hashing.

Role claims are included in authenticated requests to control access to protected functionality.

---

# 15. Database

The system uses **MongoDB** as the server-side NoSQL database.

The main collections required by the system are:

```text
Users
SolarStationInfo
EnergyBookingSlots
EnergyReservation
```

### Users

Stores information related to:

* System users
* Prosumer users
* Roles
* Account status
* Authentication-related information

### SolarStationInfo

Stores:

* Solar station information
* GPS coordinates
* Capacity
* Available energy
* Station status

### EnergyBookingSlots

Stores:

* Station slot information
* Available energy
* Schedule information
* Slot status

### EnergyReservation

Stores:

* User/prosumer information
* Station information
* Booking slot
* Reservation date/time
* Reservation status
* Transaction information

---

# 16. FAT Service Architecture

The project follows the required **FAT Service pattern**.

The central API is responsible for the application's business logic.

The clients mainly provide:

* User interface
* User input
* API requests
* API response presentation
* Local Android persistence where required

Business rules are handled by the central service rather than being duplicated across the clients.

This ensures that both the Web Application and Mobile Application use the same central business logic.

---

# 17. API and Client Communication

The communication architecture is:

```text
Web Client
     │
     │ HTTP / REST
     ▼
ASP.NET Core Web API
     │
     ▼
MongoDB
```

and:

```text
Android Client
     │
     │ HTTP / REST
     ▼
ASP.NET Core Web API
     │
     ▼
MongoDB
```

Neither client directly communicates with MongoDB.

---

# 18. Local Android Persistence

The Android application uses SQLite for local persistence as required by the assignment.

Local persistence is used for appropriate mobile-side information such as:

* User/session-related data
* Reference information
* Local application state

Server-side business data remains controlled by the central Web API.

---

# 19. Individual Contributions

The following section clearly identifies the work completed by each group member.

## Member 1 – Pinithi Ransiluni

**GitHub:** `Ransiluni2003` / `Pinithi2003`

### Identity & Access

Responsible for the Identity and Access functionality of the system.

Contributions include:

* JWT authentication
* User login functionality
* User registration
* BCrypt password hashing
* Authentication service
* Authorization
* Role-based access control
* Backoffice and Grid Operator access control
* Prosumer registration
* User management APIs
* Authentication DTOs
* AuthController implementation
* UsersController implementation
* AuthService implementation
* UserService integration
* User profile functionality
* Account status/deactivation handling
* Duplicate account/email validation
* Inactive-account validation
* Protected API endpoints
* JWT/environment configuration
* MongoDB integration related to user management
* Web UI authentication and protected navigation
* Login and registration interfaces
* Profile functionality
* Role-based dashboards
* Access-denied handling

### Web UI Responsibility

Member 1 worked on:

```text
Frontend/
└── SmartSolarUsersUI/
```

The user interface covers authentication and user/account-related functionality.

### Android Integration

Member 1 also contributed to the Android application's authentication/account/navigation integration, including session handling and navigation between major application sections.

---

## Member 2 – Station / Infrastructure Module

Responsible for the solar station and infrastructure-related functionality.

Main contribution area:

```text
Frontend/
└── SmartSolarStationUI/
```

The module covers functionality related to:

* Solar station management
* Station information
* Station availability
* Station capacity
* Station status
* Station listing
* Station search/filter functionality
* Station details
* Infrastructure-related API functionality
* Station data integration

The station functionality provides the data required for users to identify available microgrid locations and energy capacity.

---

## Member 3 – Reservation Module

Responsible for energy booking and reservation functionality.

Main contribution area:

```text
Frontend/
└── SmartSolarReservationsUI/
```

Main contributions include:

* Energy booking
* Reservation creation
* Reservation update
* Reservation cancellation
* Reservation API functionality
* Reservation Web UI
* Android reservation activities
* Reservation repository
* Reservation business-rule handling
* Booking summaries
* Reservation-related layouts
* Integration of reservation functionality with the central API

The reservation functionality implements the required booking workflow and reservation rules.

---

## Member 4 – QR / Maps / Field Operations

Responsible for field-operation and transaction-related functionality.

Main contribution area:

```text
Frontend/
└── SmartSolarFieldOpsUI/
```

Main contributions include:

* QR code functionality
* QR scanning
* Transaction verification
* Grid Operator field operations
* Google Maps integration
* Nearby station display
* Station location visualization
* Field-operation functionality
* Mobile operator workflow
* Integration between QR/map features and the central API

---

# 20. Git Branches and Collaboration

The project was developed using Git and GitHub with separate branches for major areas of development.

Main development branches included:

```text
main

Member-1---Identity-&-Access
Member-2---Infrastructure
Member-3---Reservations
Member-4---QR-/-Maps-/-Integration
```

The project uses meaningful commits and branch-based development to separate individual contributions.

Completed functionality was merged into the main branch after development and integration.

---

# 21. Deployment

The Web API is designed to run on a Windows environment using **IIS**.

Deployment architecture:

```text
Internet / Local Network
          │
          ▼
     IIS Server
          │
          ▼
ASP.NET Core Web API
          │
          ▼
      MongoDB
```

The API can be accessed by both:

* Web Application
* Android Mobile Application

The clients communicate with the API using RESTful HTTP requests.

---

# 22. API Documentation

Swagger/OpenAPI is used for testing and documenting the Web API.

Swagger provides an interface to:

* View API endpoints
* View request models
* Test API requests
* Check API responses
* Verify authentication-protected endpoints
* Test backend functionality during development

---

# 23. Main Application Workflow

The overall system workflow is:

```text
                    START
                      │
                      ▼
               User Authentication
                      │
          ┌───────────┼────────────┐
          │           │            │
          ▼           ▼            ▼
      Backoffice   Operator     Prosumer
          │           │            │
          ▼           ▼            ▼
     User/Station   Monitor      View Stations
     Management     Bookings         │
          │           │               ▼
          │           │          Select Station
          │           │               │
          │           │               ▼
          │           │          Select Slot
          │           │               │
          │           │               ▼
          │           │        Create Reservation
          │           │               │
          │           │               ▼
          │           │        Reservation Approved
          │           │               │
          │           │               ▼
          │           └────────── QR Transaction
          │                           │
          │                           ▼
          │                    Operator Scans QR
          │                           │
          │                           ▼
          │                     Server Verification
          │                           │
          │                           ▼
          │                    Energy Transfer Done
          │
          ▼
       Management
```

---

# 24. Security Features

The system implements several security-related mechanisms.

These include:

* JWT authentication
* Role-based authorization
* BCrypt password hashing
* Protected API endpoints
* Authentication token validation
* Account status validation
* Duplicate account validation
* Server-side business-rule validation
* Centralized authorization
* Controlled access to administrative functions

---

# 25. Business Rules

The system follows the major business rules specified for the assignment.

### Reservation Window

Energy reservations must be scheduled within the allowed **7-day reservation window**.

### Update and Cancellation Notice

Reservation updates and cancellations require the required **12-hour advance notice**.

### Station Deactivation

A solar microgrid station cannot be deactivated when active energy reservations exist.

### Account Management

Prosumer accounts use NIC as the primary identifier.

Deactivated accounts require authorized Backoffice handling for reactivation.

### Centralized Business Logic

Business rules are implemented through the central Web API according to the FAT Service architecture.

---

# 26. Video Demonstration

A short video demonstrating the complete application workflow is provided below.

### Application Demonstration Video

**YouTube / OneDrive Link:**

`[ADD VIDEO LINK HERE]`

The video demonstrates the main functionality of:

* User authentication
* Role-based access
* Web Application
* User management
* Solar station management
* Mobile application
* Prosumer workflow
* Energy reservation
* Booking management
* Map functionality
* QR transaction workflow
* Grid Operator functionality
* API integration

**Video Duration:** Maximum 5 minutes

---

# 27. Suggested Video Demonstration Flow

The demonstration video follows this order:

### 1. Introduction

Briefly introduce the Smart Solar Microgrid Trading System.

### 2. Web Application

Demonstrate:

* Login
* Role-based access
* Dashboard
* User management
* Station management
* Reservation management

### 3. Mobile Application

Demonstrate:

* Mobile login
* Prosumer dashboard
* Station/map view
* Station selection
* Booking/reservation
* Booking history
* Profile

### 4. Grid Operator

Demonstrate:

* Operator login
* QR scanning
* Transaction verification
* Energy-transfer completion

### 5. Backend

Briefly show:

* Swagger
* API endpoints
* MongoDB data
* IIS-hosted API

### 6. Conclusion

Briefly summarize how the Web, Mobile, API and Database components work together.

---

# 28. Project Screens / UI Modules

The project contains interfaces for:

### Web

* Login
* Registration
* Dashboard
* User Management
* Prosumer Management
* Solar Station Management
* Energy Slot Management
* Reservation Management
* Booking Views
* Access Denied
* Profile

### Android

* Splash Screen
* Login
* Create Account
* Home/Dashboard
* Station List
* Station Search
* Station Map
* Station Details
* Booking
* Reservations
* Booking History
* QR Functionality
* Grid Operator Functions
* Profile
* Account Deactivation

---

# 29. Project Objectives

The main objectives of the system are:

1. To provide a centralized solar microgrid trading platform.
2. To allow Backoffice users to manage system users and microgrid information.
3. To allow Grid Operators to monitor and process energy transactions.
4. To allow Solar Prosumers to reserve available energy slots.
5. To provide mobile access through a native Android application.
6. To provide secure authentication and role-based authorization.
7. To centralize business logic within the Web API.
8. To provide map-based solar station discovery.
9. To support QR-based transaction verification.
10. To maintain centralized server-side data using MongoDB.

---

# 30. Challenges

During development, several technical challenges were addressed, including:

* Integrating multiple client applications with one centralized API
* Implementing JWT authentication and role-based authorization
* Managing user account status and validation
* Integrating MongoDB with the ASP.NET Core Web API
* Connecting the Android application to the API over the network
* Maintaining separation between client UI and server-side business logic
* Implementing reservation business rules
* Integrating Google Maps functionality
* Implementing QR-based transaction workflows
* Deploying and configuring the ASP.NET Core API on IIS
* Integrating independently developed modules through Git branches
* Resolving integration and navigation issues during development

---

# 31. Development and Version Control

GitHub was used for collaborative development and version control.

The development process included:

* Feature-based branches
* Meaningful commits
* Individual contribution tracking
* Pull requests
* Integration into the main branch
* Continuous source-code management

Repository:

https://github.com/Pinithi2003/Smart-Solar-Microgrid-Trading-System

---

# 32. Conclusion

The **Smart Solar Microgrid Trading System** provides an integrated client-server platform for managing solar microgrid operations, energy slots, reservations, users, and energy-transfer transactions.

The system combines:

```text
Web Application
       +
Native Android Application
       +
ASP.NET Core Web API
       +
MongoDB
       +
IIS
       +
Google Maps
       +
QR Transaction Verification
```

The centralized Web API provides the main business logic and data access layer, while the Web and Android applications provide user-friendly interfaces for different system roles.

The resulting system demonstrates the use of enterprise application development principles, RESTful services, NoSQL database integration, authentication and authorization, native Android development, web development, and client-server architecture.

---

# 33. Team Members

| Member                | GitHub                          | Main Responsibility                                               |
| --------------------- | ------------------------------- | ----------------------------------------------------------------- |
| **Pinithi Ransiluni** | `Ransiluni2003` / `Pinithi2003` | Identity & Access, Authentication, Authorization, User Management |
| **Member 2**          | `PRIYADHARSHANI-A-H`            | Station / Infrastructure Management                               |
| **Member 3**          | `sharfazafai`                   | Reservations / Booking Management                                 |
| **Member 4**          | `dhushirajendran`          | QR / Maps / Field Operations                                      |

> **Note:** The exact GitHub username/name and contribution description for each member should match the actual contribution records in the repository.



# 34. Repository

**GitHub:**
https://github.com/Pinithi2003/Smart-Solar-Microgrid-Trading-System

**Video Demonstration:**
`[ADD YOUTUBE OR ONEDRIVE VIDEO LINK]`



## Assignment Information

**Module:** SE4040 – Enterprise Application Development
**Project:** Smart Solar Microgrid Trading System
**Academic Year:** 2026
**Semester:** Year 4 Semester 2
**Project Type:** Group Assignment
**Architecture:** Client-Server
**Backend:** ASP.NET Core Web API
**Database:** MongoDB
**Mobile:** Native Android
**Hosting:** IIS
