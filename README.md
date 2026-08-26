# Drive - A File Management System

A comprehensive, full-stack file management application that provides secure cloud storage capabilities. This project integrates seamless drag-and-drop file uploads and robust authentication mechanisms, serving as an advanced iteration of file management using external cloud APIs rather than relying solely on local system storage.

## Key Features

- **Advanced Authentication**: Secure login flow utilizing OAuth2 and standard credentials. Account creation includes OTP-based email verification, with passwords securely hashed using BCrypt. Role-based access control is implemented via Spring Security's FilterChain.
- **Google Drive Integration**: Utilizes the Google Drive API for remote cloud storage. It implements a Refresh Token strategy to maintain persistent access without requiring constant user re-consent.
- **Intuitive Uploads**: The frontend supports both modern drag-and-drop functionality and standard click-to-upload file selection.
- **Custom File Operations**: Complete custom logic for managing files including uploading, downloading, sharing, moving to trash, restoring, and permanent deletion.
- **Storage Management**: Tracks user storage limits and quotas explicitly defined in the application before interfacing with the Drive API.

## Tech Stack

**Backend:**
- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- MySQL
- Google Drive API (Storage)


**Frontend:**
- React 19+ (built with Vite)
- CSS for styling components (Filecard, Sidebar, Footer, etc.)
- Basic frontend integration designed specifically to seamlessly display and interact with backend API data.

## 📂 Project Structure

### Backend Structure
```text
com.project.drive
├── config
│   ├── DriveConfig
│   └── SecurityConfig
├── controller
│   ├── AuthController
│   └── FileController
├── entity
│   ├── FileEntity
│   ├── StorageTracker
│   └── UserEntity
├── repo
│   ├── FileRepository
│   ├── StorageTrackerRepository
│   └── UserRepository
├── service
│   ├── EmailService
│   └── FileServiceStorage
└── DriveApplication
```

### Frontend Structure
```text
drive_file_management
├── public
└── src
    ├── assets
    ├── Components
    │   ├── Filecard.css
    │   ├── Filecard.jsx
    │   ├── Footer.css
    │   ├── Footer.jsx
    │   ├── Sidebar.css
    │   └── Sidebar.jsx
    ├── App.css
    ├── App.jsx
    ├── DriveApp.css
    ├── DriveApp.jsx
    ├── index.css
    ├── SimpleLoginPage.css
    └── SimpleLoginPage.jsx
```

##  API Endpoints

### AuthController (`/api/auth`)
- `POST /register` -> Triggered when a new user registers.
- `POST /verify-otp` -> Verifies the OTP sent during registration.
- `POST /login` -> Triggered when logging in via password or via OAuth2.
- `GET /me` -> Access the authenticated user's profile.

### FileController (`/api/files`)
- `@RequestMapping("/api/files")` -> Base route to see files.
- `POST /upload` -> Handles file upload (supports drag and drop & simple upload).
- `GET /download/{id}` -> Download a specific file.
- `GET /home` -> Retrieve files for the home dashboard.
- `GET /recents` -> See recently used files.
- `PUT /share/{id}` -> Triggered when clicking share to share files.
- `GET /trash` -> Retrieve files that have been moved to the trash.
- `GET /storage` -> Checks storage capacity (custom limited storage logic integrated with Drive API).
- `PUT /trash/{id}` -> Find a file and move it to the trash.
- `PUT /restore/{id}` -> Restore a deleted file from the trash.
- `DELETE /delete/{id}` -> Permanently delete a file.

## Working

### Detailed Working
1. **Authentication Flow**: When a user registers, an OTP is generated and sent via `EmailService`. Upon verification, the user's password is encrypted via BCrypt and saved to MySQL. Users can also log in seamlessly using OAuth2.
2. **Role-Based Access**: The application uses a Spring Security `FilterChain` to intercept requests and ensure that operations like `/delete/{id}` or `/storage` are only accessible to authorized roles.
3. **Storage & Drive API**: Instead of local disk storage, `FileServiceStorage` connects to Google Drive. The application uses a configured Refresh Token in `DriveConfig` to obtain temporary access tokens automatically. The `StorageTracker` entity ensures users do not exceed the custom storage limits defined in the database.
4. **Drag & Drop Upload**: The React frontend (`DriveApp.jsx`) features a drag-and-drop zone. When a file is dropped (or selected normally), it is appended to a `FormData` object and posted to `/api/files/upload`.

### Local Setup Instructions

**Prerequisites:**
- Java 21 & Maven
- MySQL Server
- Node.js & npm
- Google Cloud Console Project (with Drive API enabled and OAuth credentials generated)

**Backend Setup:**
1. Create a MySQL database named
```text
drive_db
```

2. Configure `application.properties` with your MySQL credentials.
3. Add your Google Drive API credentials (Client ID, Client Secret, and Refresh Token) to `application.properties` and `DriveConfig`.


4. Cloning the Repository
Start by cloning the repository to your local machine:
```text
git clone https://github.com/Rajnish-chauhan/drive-file-manager
```
5. Docker Setup (Recommended)
  To run the entire stack (Database, Backend, and Frontend) using Docker, you can create a docker-compose.yml file in the root directory.
6. Create docker-compose.yml
7. Build and Run
8. Execute the following command in the root directory to build and spin up the containers:
```text
docker-compose up --build -d
```
9. Build and Run:
```text
mvn clean install
mvn spring-boot:run
```

The backend will be available at http://localhost:8080

**Frontend Setup:**
1. Navigate to Frontend: Open a new terminal window and navigate to the frontend folder.
```text
cd frontend
```
2. Install Dependencies:
```text
npm install
```
3. Start the Vite development server: 
```text
npm run dev
```

The frontend will be available at http://localhost:5173

---
## 🤝 Let's Connect

**Rajnish Chauhan** | Backend Software Engineer

*Engineered with a focus on scalable backend system design, clean code principles, and seamless third-party service integration.*

I am a Backend Developer passionate about building scalable APIs and robust backend systems using Java and Spring Boot. Check out my other projects or get in touch!

**🌐 Website & Portfolio:** [rajnishsystems.in](https://rajnishsystems.in)