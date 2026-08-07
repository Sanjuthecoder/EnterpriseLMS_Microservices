import toast from 'react-hot-toast';
import React, { useState, useEffect } from 'react';
import { CheckCircle, Award, XCircle } from 'lucide-react';
import api from '../../services/api';

const CertificatesTab = () => {
  const [requests, setRequests] = useState([]);
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const fetchRequests = async () => {
      try {
        const [reqRes, empRes] = await Promise.all([
          api.get('/company-admin/certificates/requests'),
          api.get('/company-admin/employees')
        ]);
        setRequests(reqRes.data || []);
        setEmployees(empRes.data || []);
      } catch (err) {
        console.error('Failed to fetch certificate requests', err);
        setError('Failed to load certificate requests.');
      } finally {
        setLoading(false);
      }
    };
    fetchRequests();
  }, []);

  const handleApprove = async (enrollmentId) => {
    try {
      await api.post(`/company-admin/certificates/${enrollmentId}/approve`);
      setRequests(requests.filter(r => r.enrollmentId !== enrollmentId));
    } catch (err) {
      toast.error("Failed to approve certificate: . Please try again.");
    }
  };

  const handleReject = async (enrollmentId) => {
    if (!window.confirm('Are you sure you want to reject this certificate request?')) return;
    try {
      await api.post(`/company-admin/certificates/${enrollmentId}/reject`);
      setRequests(requests.filter(r => r.enrollmentId !== enrollmentId));
    } catch (err) {
      toast.error("Failed to reject certificate: . Please try again.");
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading requests...</div>;
  if (error) return <div style={{ padding: '2rem', color: 'red' }}>{error}</div>;

  return (
    <div>
      <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>
        Certificate Approvals
      </h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Review and approve course completion certificates requested by employees.
      </p>

      {requests.length === 0 ? (
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          No pending certificate requests at this time.
        </div>
      ) : (
        <div style={{ display: 'grid', gap: '1rem' }}>
          {requests.map(req => {
            const emp = employees.find(e => String(e.userId) === String(req.employeeId));
            return (
              <div key={req.enrollmentId} className="glass-panel" style={{ padding: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
                  <div style={{ background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', padding: '1rem', borderRadius: '50%' }}>
                    <Award size={24} />
                  </div>
                  <div>
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                      {emp ? emp.username : `Employee ${req.employeeId}`}
                    </h3>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
                      Course: {req.courseTitle || `ID ${req.courseId}`}
                    </p>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginTop: '0.25rem' }}>
                      Requested: {req.requestedDate ? new Date(req.requestedDate).toLocaleDateString() : 'N/A'}
                    </p>
                  </div>
                </div>
                <div style={{ display: 'flex', gap: '0.75rem' }}>
                  <button 
                    onClick={() => handleReject(req.enrollmentId)}
                    className="btn"
                    style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'transparent', border: '1px solid var(--danger-color)', color: 'var(--danger-color)' }}
                  >
                    <XCircle size={18} /> Reject
                  </button>
                  <button 
                    onClick={() => handleApprove(req.enrollmentId)}
                    className="btn btn-primary"
                    style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'var(--success-color)', border: 'none' }}
                  >
                    <CheckCircle size={18} /> Approve
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default CertificatesTab;
