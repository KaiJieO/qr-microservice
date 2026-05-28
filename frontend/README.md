# QR Code Service - Frontend

React 18+ web UI for QR code management and testing.

## Features

- Auto-generate QR codes on page load
- Display visual QR codes (rendered as canvas)
- View QR code history with status tracking
- Filter by status (Valid, Expired)
- View QR code metadata (created date, expiration date, URL)

## Prerequisites

- Node.js 16+
- npm or yarn
- Backend running on `http://localhost:8080`

## Installation

```bash
npm install --legacy-peer-deps
```

Note: `--legacy-peer-deps` flag resolves dependency conflicts with qrcode.react library.

## Running

```bash
npm start
```

App runs on `http://localhost:3000`

## Build

```bash
npm run build
```

## Project Structure

```
src/
├── components/
│   ├── QrGenerator.js      # QR code generation form
│   ├── QrGenerator.css
│   ├── QrHistory.js        # QR code history & status table
│   └── QrHistory.css
├── services/
│   └── api.js              # Axios API client
├── App.js                  # Main app component
├── App.css
├── index.js                # React entry point
└── index.css
```

## API Integration

Frontend communicates with backend via `/api` endpoints:

- `POST /api/qr-codes` - Generate QR code
- `GET /api/qr-codes` - Get all QR codes
- `GET /api/qr-codes/{id}` - Get specific QR code
- `GET /api/qr-codes/status/{status}` - Filter by status

## Styling

Uses CSS Grid/Flexbox for responsive design. Gradient background with card-based layout.

## QR Code Display

QR codes are rendered using `qrcode.react` library (QRCodeCanvas component). When a QR code is generated, a visual canvas-based QR code is displayed above the metadata in the result card.

## Environment

Update API base URL in `src/services/api.js` if backend runs on different port.
