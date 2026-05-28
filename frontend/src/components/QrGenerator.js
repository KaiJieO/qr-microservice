import React, { useState } from 'react';
import { qrCodeAPI } from '../services/api';
import './QrGenerator.css';

export default function QrGenerator({ onGenerate }) {
  const [formData, setFormData] = useState({
    sublinkUrl: '',
    expiresInMinutes: 60,
    createdBy: 'admin',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: name === 'expiresInMinutes' ? parseInt(value) : value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (!formData.sublinkUrl) {
        throw new Error('URL is required');
      }

      const response = await qrCodeAPI.generateQrCode(formData);
      setFormData({ sublinkUrl: '', expiresInMinutes: 60, createdBy: 'admin' });
      onGenerate(response.data);
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to generate QR code');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="qr-generator">
      <h2>Generate QR Code</h2>
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label htmlFor="sublinkUrl">URL</label>
          <input
            type="url"
            id="sublinkUrl"
            name="sublinkUrl"
            value={formData.sublinkUrl}
            onChange={handleChange}
            placeholder="https://example.com/resource"
            required
          />
        </div>

        <div className="form-group">
          <label htmlFor="expiresInMinutes">Expires In (Minutes)</label>
          <input
            type="number"
            id="expiresInMinutes"
            name="expiresInMinutes"
            value={formData.expiresInMinutes}
            onChange={handleChange}
            min="1"
            max="10080"
          />
        </div>

        <div className="form-group">
          <label htmlFor="createdBy">Created By</label>
          <input
            type="text"
            id="createdBy"
            name="createdBy"
            value={formData.createdBy}
            onChange={handleChange}
          />
        </div>

        {error && <div className="error">{error}</div>}

        <button type="submit" disabled={loading}>
          {loading ? 'Generating...' : 'Generate QR Code'}
        </button>
      </form>
    </div>
  );
}
