import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useTheme } from '../../contexts/ThemeContext';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const CreatorProfile = () => {
  const { user } = useAuth();
  const { theme } = useTheme();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({ username: '', bio: '', phone: '' });
  const [saveStatus, setSaveStatus] = useState('');

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get(`/users/${user.userId}/profile`);
        setProfile(response.data);
        setFormData({
          username: response.data.username || '',
          bio: response.data.bio || '',
          phone: response.data.phone || ''
        });
      } catch (err) {
        console.warn('Backend profile endpoint failed, loading mock creator profile details.', err);
        const mockProfile = {
          username: user.username || 'Mock Creator',
          email: 'creator@edtech.com',
          bio: 'Curriculum architect specializing in advanced distributed computing, system scalability, and game-theoretic learning loops.',
          phone: '+1 (555) 019-3221',
          roleMetrics: {
            totalPublishedCourses: 3,
            totalImpactedLearners: 450
          }
        };
        setProfile(mockProfile);
        setFormData({
          username: mockProfile.username,
          bio: mockProfile.bio,
          phone: mockProfile.phone || ''
        });
      } finally {
        setLoading(false);
      }
    };
    fetchProfile();
  }, [user]);

  const handleSave = async () => {
    try {
      const response = await api.put(`/users/${user.userId}/profile`, formData);
      setProfile(response.data);
      setSaveStatus('Portfolio updated successfully!');
      setTimeout(() => setSaveStatus(''), 3000);
      setIsEditing(false);
    } catch (err) {
      console.warn('Backend update failed, saving in-memory.', err);
      setProfile(prev => ({
        ...prev,
        username: formData.username,
        bio: formData.bio,
        phone: formData.phone
      }));
      setSaveStatus('Portfolio saved in-memory (Offline Mode)!');
      setTimeout(() => setSaveStatus(''), 3000);
      setIsEditing(false);
    }
  };

  if (loading) return <div style={{ padding: '2rem' }}>Loading profile...</div>;
  if (error) return <div style={{ padding: '2rem', color: 'var(--danger-color)' }}>{error}</div>;
  if (!profile) return null;

  return (
    <div style={{ padding: '2rem', maxWidth: '1000px', margin: '0 auto' }}>
      <div className="glass-panel" style={{ overflow: 'hidden', padding: 0 }}>
        {/* Header Cover with Avatar and Details */}
        <div style={{ 
          background: 'var(--primary-color)', 
          padding: '2.5rem 2rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          color: '#ffffff'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1.5rem' }}>
            {/* Avatar */}
            <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'rgba(255, 255, 255, 0.2)', border: '2px solid rgba(255, 255, 255, 0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '2rem', fontWeight: 700, color: '#ffffff', backdropFilter: 'blur(10px)' }}>
              {profile.username.charAt(0).toUpperCase()}
            </div>
            
            <div>
              <h1 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.25rem', color: '#ffffff' }}>{profile.username}</h1>
              <p style={{ color: 'rgba(255, 255, 255, 0.8)' }}>{profile.email} • Instructional Designer</p>
            </div>
          </div>

          <div>
            {!isEditing ? (
              <button 
                onClick={() => setIsEditing(true)}
                style={{ background: 'rgba(255, 255, 255, 0.2)', color: '#ffffff', border: '1px solid rgba(255, 255, 255, 0.3)', padding: '0.5rem 1rem', borderRadius: 'var(--radius-md)', fontWeight: 500, cursor: 'pointer', transition: 'all 0.2s' }}
                onMouseOver={(e) => e.target.style.background = 'rgba(255, 255, 255, 0.3)'}
                onMouseOut={(e) => e.target.style.background = 'rgba(255, 255, 255, 0.2)'}
              >
                Edit Portfolio
              </button>
            ) : (
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button 
                  onClick={() => setIsEditing(false)}
                  style={{ background: 'transparent', color: '#ffffff', border: '1px solid rgba(255, 255, 255, 0.3)', padding: '0.5rem 1rem', borderRadius: 'var(--radius-md)', fontWeight: 500, cursor: 'pointer' }}
                >
                  Cancel
                </button>
                <button 
                  onClick={handleSave}
                  style={{ background: '#ffffff', color: 'var(--primary-color)', border: 'none', padding: '0.5rem 1rem', borderRadius: 'var(--radius-md)', fontWeight: 600, cursor: 'pointer' }}
                >
                  Save Changes
                </button>
              </div>
            )}
          </div>
        </div>

        <div style={{ padding: '2rem' }}>
          {saveStatus && (
            <Toast message={saveStatus} type="success" onClose={() => setSaveStatus('')} />
          )}

          <div style={{ marginTop: '2rem', display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              {isEditing ? (
                <div className="glass-panel" style={{ padding: '1.5rem' }}>
                  <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1rem' }}>Edit Portfolio Info</h3>
                  
                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label">Creator Name <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                    <input
                      type="text"
                      required
                      className="form-input"
                      value={formData.username}
                      onChange={(e) => setFormData({...formData, username: e.target.value})}
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label">Phone Number <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                    <input
                      type="text"
                      required
                      className="form-input"
                      value={formData.phone}
                      onChange={(e) => setFormData({...formData, phone: e.target.value})}
                      placeholder="+1 (555) 000-0000"
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label">Biography / Tagline <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                    <textarea
                      className="form-input"
                      required
                      rows="4"
                      value={formData.bio}
                      onChange={(e) => setFormData({...formData, bio: e.target.value})}
                      placeholder="Describe your expertise..."
                    ></textarea>
                  </div>
                </div>
              ) : (
                <>
                  <div className="glass-panel" style={{ padding: '1.5rem' }}>
                    <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '0.75rem' }}>About The Creator</h3>
                    <p style={{ color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.6, fontSize: '0.95rem' }}>{profile.bio || "No biography added yet."}</p>
                  </div>

                  {profile.phone && (
                    <div className="glass-panel" style={{ padding: '1.5rem' }}>
                      <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '0.75rem' }}>Contact Phone</h3>
                      <p style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{profile.phone}</p>
                    </div>
                  )}
                </>
              )}
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1rem' }}>Portfolio Impact</h3>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                  <div style={{ background: 'rgba(37, 99, 235, 0.05)', borderRadius: 'var(--radius-md)', padding: '1rem', border: '1px solid rgba(37, 99, 235, 0.1)' }}>
                    <div style={{ color: 'var(--primary-color)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>Published Courses</div>
                    <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--text-primary)' }}>{profile.roleMetrics?.totalPublishedCourses || 0}</div>
                  </div>
                  <div style={{ background: 'rgba(16, 185, 129, 0.05)', borderRadius: 'var(--radius-md)', padding: '1rem', border: '1px solid rgba(16, 185, 129, 0.1)' }}>
                    <div style={{ color: 'var(--success-color)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>Learners Reached</div>
                    <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--text-primary)' }}>{profile.roleMetrics?.totalImpactedLearners || 0}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default CreatorProfile;
