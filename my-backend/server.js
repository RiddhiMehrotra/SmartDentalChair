const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// Middleware
app.use(cors());
app.use(express.json());

// In-memory data storage (Reset when server restarts)
// In a production app, you would use a database like MongoDB or PostgreSQL
const users = [];
const activityLogs = [];

// --- Authentication Routes ---

// Signup Route
app.post('/api/auth/signup', (req, res) => {
    console.log("Signup API hit");
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ message: "Email and password are required" });
    }

    const userExists = users.find(u => u.email === email);
    if (userExists) {
        return res.status(400).json({ message: "User already exists" });
    }

    const newUser = { id: users.length + 1, email, password };
    users.push(newUser);

    console.log(`User registered: ${email}`);
    res.status(201).json({ message: "User created successfully", user: { email } });
});

app.post('/api/auth/login', (req, res) => {
    console.log("Login API hit");
    const { email, password } = req.body;

    const user = users.find(u => u.email === email && u.password === password);
    if (!user) {
        return res.status(401).json({ message: "Invalid credentials" });
    }

    console.log(`Login successful: ${email}`);
    res.json({ message: "Login successful", user: { email } });
});

// --- Chair Activity Routes ---

// Log a chair command
app.post('/api/logs', (req, res) => {
    const { action, status, timestamp } = req.body;

    const logEntry = {
        id: activityLogs.length + 1,
        action,
        status,
        timestamp: timestamp || new Date().toISOString()
    };

    activityLogs.push(logEntry);
    console.log(`Log recorded: ${action} - ${status}`);
    res.status(201).json(logEntry);
});

// Get all logs
app.get('/api/logs', (req, res) => {
    res.json(activityLogs);
});

// --- Health Check ---
app.get('/', (req, res) => {
    res.send('Smart Dental Chair Backend is running...');
});

app.listen(PORT, () => {
    console.log(`🚀 Server is running on port ${PORT}`);
    console.log(`🔗 Local: http://localhost:${PORT}`);
});
