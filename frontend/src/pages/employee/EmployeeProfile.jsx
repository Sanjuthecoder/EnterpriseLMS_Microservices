import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { useTheme } from '../../contexts/ThemeContext';
import api from '../../services/api';
import { CheckCircle } from 'lucide-react';
import Toast from '../../components/shared/Toast';

const EmployeeProfile = () => {
  const { user } = useAuth();
  const { theme } = useTheme();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({ username: '', bio: '', phone: '', avatarUrl: '' });
  const [saveStatus, setSaveStatus] = useState('');

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const response = await api.get(`/users/${user.userId}/profile`);
        setProfile(response.data);
        setFormData({
          username: response.data.username || '',
          bio: response.data.bio || '',
          phone: response.data.phone || '',
          avatarUrl: response.data.avatarUrl || ''
        });
      } catch (err) {
        console.warn('Backend profile endpoint failed, loading mock employee profile details.', err);
        const mockProfile = {
          username: user.username || 'Mock Employee',
          email: 'employee@company.com',
          department: 'Product Delivery',
          bio: 'Product engineer passionate about building accessible and high performance web applications.',
          phone: '+1 (555) 019-2834',
          roleMetrics: {
            completedCourses: 4,
            skillGaps: [
              { conceptName: 'System Architecture', targetLevel: 5, currentLevel: 3 },
              { conceptName: 'Database Tuning', targetLevel: 4, currentLevel: 3 }
            ]
          }
        };
        setProfile(mockProfile);
        setFormData({
          username: mockProfile.username,
          bio: mockProfile.bio,
          phone: mockProfile.phone || '',
          avatarUrl: ''
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
      setSaveStatus('Changes saved successfully!');
      setTimeout(() => setSaveStatus(''), 3000);
      setIsEditing(false);
    } catch (err) {
      console.warn('Backend update failed, saving in-memory.', err);
      setProfile(prev => ({
        ...prev,
        username: formData.username,
        bio: formData.bio,
        phone: formData.phone,
        avatarUrl: formData.avatarUrl
      }));
      setSaveStatus('Changes saved in-memory (Offline Mode)!');
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
              <p style={{ color: 'rgba(255, 255, 255, 0.8)' }}>{profile.email}</p>
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
                Edit Profile
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
                  <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1rem' }}>Edit Personal Information</h3>
                  
                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label">Display Name</label>
                    <input
                      type="text"
                      className="form-input"
                      value={formData.username}
                      onChange={(e) => setFormData({...formData, username: e.target.value})}
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label">Phone Number</label>
                    <input
                      type="text"
                      className="form-input"
                      value={formData.phone}
                      onChange={(e) => setFormData({...formData, phone: e.target.value})}
                      placeholder="+1 (555) 000-0000"
                    />
                  </div>

                  <div className="form-group" style={{ marginBottom: '1rem' }}>
                    <label className="form-label">Biography</label>
                    <textarea
                      className="form-input"
                      rows="4"
                      value={formData.bio}
                      onChange={(e) => setFormData({...formData, bio: e.target.value})}
                      placeholder="Tell us about yourself..."
                    ></textarea>
                  </div>
                </div>
              ) : (
                <>
                  <div className="glass-panel" style={{ padding: '1.5rem' }}>
                    <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '0.75rem' }}>About Me</h3>
                    <p style={{ color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.6, fontSize: '0.95rem' }}>{profile.bio || "No bio added yet."}</p>
                  </div>

                  {profile.phone && (
                    <div className="glass-panel" style={{ padding: '1.5rem' }}>
                      <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '0.75rem' }}>Contact Phone</h3>
                      <p style={{ color: 'var(--text-primary)', fontWeight: 500 }}>{profile.phone}</p>
                    </div>
                  )}
                </>
              )}

              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1rem' }}>Organizational Details</h3>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                  <div>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Department</p>
                    <p style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{profile.department || 'Not specified'}</p>
                  </div>
                  <div>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Role Type</p>
                    <p style={{ fontWeight: 500, color: 'var(--text-primary)' }}>Learner / Employee</p>
                  </div>
                  <div style={{ gridColumn: 'span 2' }}>
                    <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>Status</p>
                    <span style={{ display: 'inline-flex', alignItems: 'center', padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600, background: 'rgba(16, 185, 129, 0.1)', color: 'var(--success-color)' }}>
                      Active
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1rem' }}>My Learning Stats</h3>
                <div style={{ background: 'rgba(37, 99, 235, 0.05)', borderRadius: 'var(--radius-md)', padding: '1rem', border: '1px solid rgba(37, 99, 235, 0.1)' }}>
                  <div style={{ color: 'var(--primary-color)', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>Completed Courses</div>
                  <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--text-primary)' }}>{profile.roleMetrics?.completedCourses || 0}</div>
                </div>
              </div>

              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '0.85rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '1px', marginBottom: '1rem' }}>Top Skill Gaps</h3>
                {profile.roleMetrics?.skillGaps && profile.roleMetrics.skillGaps.length > 0 ? (
                  <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {profile.roleMetrics.skillGaps.slice(0, 3).map((gap, i) => (
                      <li key={i} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.9rem', paddingBottom: '0.5rem', borderBottom: '1px solid var(--border-color)' }}>
                        <span style={{ color: 'var(--text-secondary)' }}>{gap.conceptName}</span>
                        <span style={{ color: 'var(--danger-color)', fontWeight: 600 }}>{gap.targetLevel - gap.currentLevel} pt gap</span>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>No skill gaps recorded.</p>
                )}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default EmployeeProfile;
