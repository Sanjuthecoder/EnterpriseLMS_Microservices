import React, { useState, useEffect } from 'react';
import { Award, Download } from 'lucide-react';
import api from '../../services/api';

const CertificatesTab = () => {
  const [certificates, setCertificates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchCertificates = async () => {
      try {
        const response = await api.get('/employees/dashboard');
        // Filter approved and rejected courses
        const completed = response.data.filter(course => course.certificateStatus === 'APPROVED' || course.certificateStatus === 'REJECTED');
        setCertificates(completed);
      } catch (err) {
        console.error('Failed to load certificates:', err);
        if (err.response?.status === 403) {
          setError('Access Denied: Your session may have expired or your account lacks the required permissions. Please log out and log back in.');
        } else if (err.response?.status === 401) {
          setError('Your session has expired. Please log in again.');
        } else {
          setError('Could not load your certificates. Please try again later.');
        }
      } finally {
        setLoading(false);
      }
    };
    fetchCertificates();
  }, []);

  if (loading) return <div style={{ padding: '2rem' }}>Loading certificates...</div>;
  if (error) return <div style={{ padding: '2rem', color: 'var(--danger-color)' }}>{error}</div>;

  return (
    <div>
      <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>
        My Certificates
      </h2>
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Download your verified completion certificates for HR compliance.
      </p>
      
      {certificates.length === 0 ? (
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          You have no approved or rejected certificate requests. Keep learning!
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.5rem' }}>
          {certificates.map((cert) => (
            <div key={cert.enrollmentId} className="glass-panel" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center', border: cert.certificateStatus === 'REJECTED' ? '1px solid var(--danger-color)' : 'none' }}>
              <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: cert.certificateStatus === 'REJECTED' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(16, 185, 129, 0.1)', color: cert.certificateStatus === 'REJECTED' ? 'var(--danger-color)' : 'var(--success-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: '1.5rem' }}>
                <Award size={40} />
              </div>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem' }}>{cert.courseTitle}</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
                Status: {cert.certificateStatus}
              </p>
              {cert.certificateStatus === 'APPROVED' ? (
                <>
                  <span style={{ background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', padding: '4px 12px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: 700, marginBottom: '2rem' }}>
                    VERIFIED
                  </span>
                  <button className="btn btn-primary" style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                    <Download size={18} /> Download PDF
                  </button>
                </>
              ) : (
                <span style={{ background: 'rgba(239, 68, 68, 0.1)', color: 'var(--danger-color)', padding: '4px 12px', borderRadius: '12px', fontSize: '0.8rem', fontWeight: 700, marginBottom: '2rem' }}>
                  REQUEST REJECTED
                </span>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default CertificatesTab;
