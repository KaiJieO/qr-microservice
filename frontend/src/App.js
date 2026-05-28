import React, { useState, useEffect } from 'react';
import { QRCodeCanvas } from 'qrcode.react';
import { qrCodeAPI } from './services/api';
import QrGenerator from './components/QrGenerator';
import QrHistory from './components/QrHistory';
import './App.css';

function App() {
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [validQrCodes, setValidQrCodes] = useState([]);

  const handleQrGenerated = (qrData) => {
    setValidQrCodes(prev => [qrData, ...prev]);
    setRefreshTrigger(prev => prev + 1);
  };

  useEffect(() => {
    loadValidQrCodes();
  }, []);

  const loadValidQrCodes = async () => {
    try {
      const response = await qrCodeAPI.getQrCodesByStatus('VALID');
      setValidQrCodes(response.data);
    } catch (err) {
      console.error('Failed to load VALID QR codes', err);
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>QR Code Service</h1>
        <p>Generate, manage, and track QR codes</p>
      </header>

      <main className="app-main">
        <QrGenerator onGenerate={handleQrGenerated} />

        {validQrCodes.length > 0 && (
          <div className="generated-qr-display">
            <h2>Active QR Codes</h2>
            <div className="qr-cards-list">
              {validQrCodes.map((qr) => (
                <div key={qr.id} className="qr-result-card">
                  <div className="qr-card-content">
                    <div className="qr-code-container">
                      <QRCodeCanvas
                        value={qr.sublinkUrl}
                        size={200}
                        level="H"
                        includeMargin={true}
                      />
                    </div>
                    <div className="qr-result-info">
                      <p><strong>ID:</strong> <code>{qr.id.substring(0, 12)}...</code></p>
                      <p><strong>URL:</strong> <a href={qr.sublinkUrl} target="_blank" rel="noopener noreferrer" title={qr.sublinkUrl}>{qr.sublinkUrl.length > 35 ? qr.sublinkUrl.substring(0, 35) + '...' : qr.sublinkUrl}</a></p>
                      <p><strong>Created:</strong> {new Date(qr.createdAt).toLocaleString()}</p>
                      <p><strong>Expires:</strong> {new Date(qr.expiredAt).toLocaleString()}</p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <QrHistory refreshTrigger={refreshTrigger} />
      </main>

      <footer className="app-footer">
        <p>&copy; 2026 QR Code Service. Running locally.</p>
      </footer>
    </div>
  );
}

export default App;
