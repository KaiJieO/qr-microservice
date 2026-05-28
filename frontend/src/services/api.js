import axios from 'axios';

const API_BASE = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const qrCodeAPI = {
  generateQrCode: (data) => api.post('/qr-codes', data),
  getAllQrCodes: () => api.get('/qr-codes'),
  getQrCode: (id) => api.get(`/qr-codes/${id}`),
  getQrCodesByStatus: (status) => api.get(`/qr-codes/status/${status}`),
};

export default api;
