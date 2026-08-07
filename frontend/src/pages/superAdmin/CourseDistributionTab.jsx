import React, { useState, useEffect } from 'react';
import { RefreshCw, Search, CheckCircle, XCircle, Share2 } from 'lucide-react';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const CourseDistributionTab = () => {
  const [courses, setCourses] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [toast, setToast] = useState(null);

  // Modal State
  const [selectedCourse, setSelectedCourse] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [courseAvailability, setCourseAvailability] = useState([]);
  const [modalLoading, setModalLoading] = useState(false);
  const [processingCompanyId, setProcessingCompanyId] = useState(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [coursesRes, companiesRes] = await Promise.all([
        api.get('/super-admin/courses'),
        api.get('/super-admin/companies')
      ]);
      
      // Only show PUBLISHED courses for distribution
      const publishedCourses = (coursesRes.data || []).filter(c => c.status === 'PUBLISHED');
      setCourses(publishedCourses);
      setCompanies(companiesRes.data || []);
    } catch (err) {
      console.error('Failed to load distribution data', err);
      setToast({ message: 'Failed to load courses or companies.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const openDistributionModal = async (course) => {
    setSelectedCourse(course);
    setIsModalOpen(true);
    setModalLoading(true);
    try {
      const res = await api.get(`/super-admin/courses/${course.courseId}/availability`);
      // We map the response into an array of enabled company IDs
      const enabledCompanyIds = res.data.map(ca => ca.companyId);
      setCourseAvailability(enabledCompanyIds);
    } catch (err) {
      console.error('Failed to load course availability', err);
      setToast({ message: 'Failed to load existing distribution rules.', type: 'error' });
    } finally {
      setModalLoading(false);
    }
  };

  const toggleAvailability = async (companyId, isCurrentlyEnabled) => {
    setProcessingCompanyId(companyId);
    try {
      if (isCurrentlyEnabled) {
        // Disable
        await api.delete(`/super-admin/companies/${companyId}/courses/${selectedCourse.courseId}`);
        setCourseAvailability(prev => prev.filter(id => id !== companyId));
      } else {
        // Enable
        await api.post(`/super-admin/companies/${companyId}/courses/${selectedCourse.courseId}`);
        setCourseAvailability(prev => [...prev, companyId]);
      }
    } catch (err) {
      console.error('Failed to update availability', err);
      setToast({ message: 'Failed to update course availability.', type: 'error' });
    } finally {
      setProcessingCompanyId(null);
    }
  };

  const filteredCourses = courses.filter(c => 
    c.title.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>Course Distribution</h2>
        <button 
          onClick={fetchData} 
          className="btn btn-secondary" 
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          title="Refresh list"
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      <div style={{ marginBottom: '1.5rem', display: 'flex', gap: '1rem' }}>
        <div style={{ position: 'relative', flexGrow: 1, maxWidth: '400px' }}>
          <Search size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
          <input 
            type="text" 
            placeholder="Search published courses..." 
            className="form-input"
            style={{ paddingLeft: '2.5rem' }}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Loading courses...
        </div>
      ) : (
        <div className="glass-panel" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <thead>
              <tr style={{ background: 'rgba(0,0,0,0.02)', borderBottom: '1px solid var(--border-color)', textAlign: 'left' }}>
                <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Course Title</th>
                <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Category</th>
                <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Version</th>
                <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-secondary)' }}>Created</th>
                <th style={{ padding: '1rem', fontWeight: 600, color: 'var(--text-secondary)', textAlign: 'right' }}>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredCourses.map(course => (
                <tr key={course.courseId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                  <td style={{ padding: '1rem', fontWeight: 600 }}>{course.title}</td>
                  <td style={{ padding: '1rem' }}>{course.category || 'General'}</td>
                  <td style={{ padding: '1rem' }}>v{course.version}</td>
                  <td style={{ padding: '1rem', color: 'var(--text-secondary)' }}>{new Date(course.createdAt).toLocaleDateString()}</td>
                  <td style={{ padding: '1rem', textAlign: 'right' }}>
                    <button 
                      onClick={() => openDistributionModal(course)}
                      className="btn btn-primary"
                      style={{ padding: '0.4rem 0.75rem', fontSize: '0.85rem', display: 'inline-flex', alignItems: 'center', gap: '0.5rem' }}
                    >
                      <Share2 size={14} /> Manage Access
                    </button>
                  </td>
                </tr>
              ))}
              {filteredCourses.length === 0 && (
                <tr>
                  <td colSpan={5} style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No published courses found.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Distribution Modal */}
      {isModalOpen && selectedCourse && (
        <div style={{ position: 'fixed', top: 0, left: 0, right: 0, bottom: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 }}>
          <div style={{ background: 'var(--bg-primary)', padding: '2rem', borderRadius: 'var(--radius-lg)', width: '600px', maxWidth: '90vw', maxHeight: '80vh', overflowY: 'auto', boxShadow: '0 10px 25px rgba(0,0,0,0.1)' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)' }}>
              <div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 700 }}>Manage Access: {selectedCourse.title}</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>Select which companies can use this course.</p>
              </div>
              <button onClick={() => setIsModalOpen(false)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)' }}>
                <XCircle size={24} />
              </button>
            </div>

            {modalLoading ? (
              <div style={{ padding: '2rem', textAlign: 'center' }}>Loading distribution settings...</div>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {companies.map(company => {
                  const isEnabled = courseAvailability.includes(company.companyId);
                  const isProcessing = processingCompanyId === company.companyId;

                  return (
                    <div key={company.companyId} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1rem', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-md)', border: `1px solid ${isEnabled ? 'var(--primary-color)' : 'var(--border-color)'}` }}>
                      <div>
                        <div style={{ fontWeight: 600 }}>{company.name}</div>
                      </div>
                      <button
                        onClick={() => toggleAvailability(company.companyId, isEnabled)}
                        disabled={isProcessing}
                        style={{
                          padding: '0.5rem 1rem',
                          borderRadius: 'var(--radius-md)',
                          fontWeight: 600,
                          fontSize: '0.85rem',
                          cursor: isProcessing ? 'wait' : 'pointer',
                          display: 'flex',
                          alignItems: 'center',
                          gap: '0.5rem',
                          border: isEnabled ? '1px solid var(--danger-color)' : '1px solid var(--primary-color)',
                          background: isEnabled ? 'rgba(239, 68, 68, 0.1)' : 'var(--primary-color)',
                          color: isEnabled ? 'var(--danger-color)' : 'white'
                        }}
                      >
                        {isProcessing ? 'Processing...' : isEnabled ? 'Revoke Access' : 'Enable Course'}
                      </button>
                    </div>
                  );
                })}
                {companies.length === 0 && (
                  <div style={{ padding: '1rem', textAlign: 'center', color: 'var(--text-secondary)' }}>No companies found in the registry.</div>
                )}
                
                <div style={{ marginTop: '1.5rem', display: 'flex', justifyContent: 'flex-end', borderTop: '1px solid var(--border-color)', paddingTop: '1.5rem' }}>
                  <button 
                    onClick={() => setIsModalOpen(false)} 
                    className="btn btn-secondary"
                  >
                    Close
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}
    </div>
  );
};

export default CourseDistributionTab;
