const express = require('express');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 5000;

// --- MIDDLEWARE ---
app.use(cors()); // Allows your frontend to access this API
app.use(express.json()); // Allows the server to read JSON data in requests

// --- ROUTES ---

// 1. A basic "GET" route (To test if it's working)
app.get('/', (req, res) => {
    res.json({ status: "success", message: "Backend is running!" });
});

// 2. A "POST" route (To receive data from a user/frontend)
app.post('/api/data', (req, res) => {
    const userData = req.body;
    console.log("Received data:", userData);

    // In a real app, you would save this to a database here
    res.status(201).json({
        message: "Data received successfully!",
        received: userData
    });
});

// --- ERROR HANDLING ---
app.use((req, res) => {
    res.status(404).json({ error: "Route not found" });
});

// --- START SERVER ---
app.listen(PORT, () => {
    console.log(`🚀 Server is flying at http://localhost:${PORT}`);
});