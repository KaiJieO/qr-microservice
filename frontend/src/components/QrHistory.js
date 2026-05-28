import React, { useState, useEffect } from 'react';
import { qrCodeAPI } from '../services/api';
import './QrHistory.css';

export default function QrHistory({ refreshTrigger }) {
  const [qrCodes, setQrCodes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  useEffect(() => {
    fetchQrCodes();
  }, [refreshTrigger, statusFilter]);

  const fetchQrCodes = async () => {
    setLoading(true);
    setError('');
    try {
      let response;
      if (statusFilter) {
        response = await qrCodeAPI.getQrCodesByStatus(statusFilter);
      } else {
        response = await qrCodeAPI.getAllQrCodes();
      }
      setQrCodes(response.data);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to fetch QR codes');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'VALID':
        return '#4caf50';
      case 'EXPIRED':
        return '#f44336';
      default:
        return '#999';
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const isExpired = (expiredAt) => {
    return new Date() > new Date(expiredAt);
  };

  return (
    <div className="qr-history">
      <div className="history-header">
        <h2>QR Code History</h2>
        <div className="filter-buttons">
          <button
            className={!statusFilter ? 'active' : ''}
            onClick={() => setStatusFilter('')}
          >
            All
          </button>
          <button
            className={statusFilter === 'VALID' ? 'active' : ''}
            onClick={() => setStatusFilter('VALID')}
          >
            Valid
          </button>
          <button
            className={statusFilter === 'EXPIRED' ? 'active' : ''}
            onClick={() => setStatusFilter('EXPIRED')}
          >
            Expired
          </button>
        </div>
      </div>

      {error && <div className="error-message">{error}</div>}
      {loading && <div className="loading">Loading...</div>}

      {!loading && qrCodes.length === 0 && (
        <div className="empty-state">No QR codes found</div>
      )}

      {!loading && qrCodes.length > 0 && (
        <div className="table-container">
          <table className="qr-table">
            <thead>
              <tr>
                <th>ID</th>
                <th>URL</th>
                <th>Status</th>
                <th>Created At</th>
                <th>Expires At</th>
                <th>Created By</th>
              </tr>
            </thead>
            <tbody>
              {qrCodes.map((qr) => (
                <tr key={qr.id}>
                  <td className="id-cell">
                    <code>{qr.id.substring(0, 8)}...</code>
                  </td>
                  <td className="url-cell">
                    <a href={qr.sublinkUrl} target="_blank" rel="noopener noreferrer">
                      {qr.sublinkUrl.length > 40
                        ? qr.sublinkUrl.substring(0, 40) + '...'
                        : qr.sublinkUrl}
                    </a>
                  </td>
                  <td>
                    <span
                      className="status-badge"
                      style={{ backgroundColor: getStatusColor(qr.status) }}
                    >
                      {qr.status}
                    </span>
                  </td>
                  <td>{formatDate(qr.createdAt)}</td>
                  <td>
                    <span className={isExpired(qr.expiredAt) ? 'expired' : ''}>
                      {formatDate(qr.expiredAt)}
                    </span>
                  </td>
                  <td>{qr.createdBy || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
