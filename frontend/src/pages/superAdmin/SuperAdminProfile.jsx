import React, { useState, useEffect } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const SuperAdminProfile = () => {
  const { user, updateUser } = useAuth();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({ username: '', bio: '', phone: '' });
  const [toast, setToast] = useState(null);
  const [copyStatus, setCopyStatus] = useState({});

  const copyToClipboard = (text, key) => {
    navigator.clipboard.writeText(text);
    setCopyStatus(prev => ({ ...prev, [key]: true }));
    setToast({ message: 'Invitation link copied to clipboard!', type: 'success' });
    setTimeout(() => {
      setCopyStatus(prev => ({ ...prev, [key]: false }));
    }, 2000);
  };

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
        console.warn('Backend profile endpoint failed, loading mock super admin details.', err);
        const mockProfile = {
          username: user.username || 'Mock Super Admin',
          email: 'superadmin@lms.platform',
          bio: 'Global platform super administrator. Overseeing tenant provisioning, compliance status, course approvals, and system metrics.',
          phone: '+1 (555) 019-1002',
          roleMetrics: {
            totalOrganizations: 2,
            platformUptimeDays: 99
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
      if (updateUser) {
        updateUser({ username: response.data.username });
      }
      setToast({ message: 'Platform Profile updated successfully!', type: 'success' });
      setIsEditing(false);
    } catch (err) {
      console.warn('Backend update failed, saving in-memory.', err);
      setProfile(prev => ({
        ...prev,
        username: formData.username,
        bio: formData.bio,
        phone: formData.phone
      }));
      if (updateUser) {
        updateUser({ username: formData.username });
      }
      setToast({ message: 'Profile saved in-memory (Offline Mode)!', type: 'success' });
      setIsEditing(false);
    }
  };

  if (loading) return <div className="p-8 text-slate-800">Loading profile...</div>;
  if (error) return <div className="p-8 text-red-500">{error}</div>;
  if (!profile) return null;

  return (
    <div style={{ padding: '2rem 1rem', maxWidth: '1000px', margin: '0 auto' }}>
      <div style={{
        backgroundColor: 'var(--bg-secondary)',
        borderRadius: 'var(--radius-xl)',
        boxShadow: 'var(--shadow-lg)',
        border: '1px solid var(--border-color)',
        overflow: 'hidden',
        position: 'relative'
      }}>
        {/* Header Cover - Premium Gradient Cover with Details Inside */}
        <div style={{
          minHeight: '160px',
          background: 'linear-gradient(135deg, #2563eb 0%, #4f46e5 50%, #7c3aed 100%)',
          position: 'relative',
          padding: '2.5rem',
          display: 'flex',
          alignItems: 'center',
          gap: '2rem',
          flexWrap: 'wrap',
          color: '#ffffff'
        }}>
          <div style={{
            position: 'absolute',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.05)',
            zIndex: 1
          }}></div>

          {/* Avatar Inside Cover */}
          <div style={{
            width: '90px',
            height: '90px',
            backgroundColor: 'rgba(255, 255, 255, 0.15)',
            borderRadius: '50%',
            padding: '5px',
            border: '4px solid rgba(255, 255, 255, 0.25)',
            boxShadow: 'var(--shadow-md)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
            zIndex: 2
          }}>
            <div style={{
              width: '100%',
              height: '100%',
              background: '#ffffff',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '2rem',
              fontWeight: 800,
              color: '#4f46e5',
            }}>
              {profile.username.charAt(0).toUpperCase()}
            </div>
          </div>

          {/* Details Inside Cover */}
          <div style={{ flexGrow: 1, zIndex: 2, minWidth: '220px' }}>
            <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: '#ffffff', margin: 0, letterSpacing: '-0.5px', lineHeight: 1.2 }}>
              {profile.username}
            </h1>
            <p style={{ color: 'rgba(255, 255, 255, 0.9)', fontWeight: 500, margin: '0.4rem 0 0 0', fontSize: '0.95rem', display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
              <span style={{
                backgroundColor: 'rgba(255, 255, 255, 0.2)',
                color: '#ffffff',
                padding: '2px 8px',
                borderRadius: '6px',
                fontSize: '0.75rem',
                fontWeight: 700,
                textTransform: 'uppercase'
              }}>
                Super Admin
              </span>
              &bull; <span>{profile.email}</span>
            </p>
          </div>

          {/* Action Buttons Inside Cover */}
          <div style={{ zIndex: 2 }}>
            {!isEditing ? (
              <button 
                onClick={() => setIsEditing(true)}
                className="btn"
                style={{ 
                  fontWeight: 600, 
                  fontSize: '0.85rem', 
                  padding: '0.5rem 1.2rem',
                  backgroundColor: 'rgba(255, 255, 255, 0.2)', 
                  color: '#ffffff',
                  border: '1px solid rgba(255, 255, 255, 0.4)',
                  borderRadius: 'var(--radius-md)',
                  cursor: 'pointer'
                }}
              >
                Edit Profile
              </button>
            ) : (
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button 
                  onClick={() => setIsEditing(false)}
                  className="btn"
                  style={{ 
                    fontWeight: 600, 
                    fontSize: '0.85rem', 
                    padding: '0.5rem 1.2rem',
                    backgroundColor: 'rgba(255, 255, 255, 0.1)', 
                    color: '#ffffff',
                    border: '1px solid rgba(255, 255, 255, 0.3)',
                    borderRadius: 'var(--radius-md)',
                    cursor: 'pointer'
                  }}
                >
                  Cancel
                </button>
                <button 
                  onClick={handleSave}
                  className="btn"
                  style={{ 
                    fontWeight: 600, 
                    fontSize: '0.85rem', 
                    padding: '0.5rem 1.2rem', 
                    border: 'none', 
                    backgroundColor: '#ffffff',
                    color: '#4f46e5',
                    borderRadius: 'var(--radius-md)',
                    cursor: 'pointer'
                  }}
                >
                  Save Changes
                </button>
              </div>
            )}
          </div>
        </div>
        
        {/* Card Body */}
        <div style={{ padding: '2.5rem', position: 'relative' }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr', gap: '2.5rem', marginBottom: '2.5rem' }}>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              {isEditing ? (
                <div style={{
                  backgroundColor: 'rgba(0,0,0,0.01)',
                  padding: '1.5rem',
                  borderRadius: 'var(--radius-lg)',
                  border: '1px solid var(--border-color)',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '1.25rem'
                }}>
                  <h3 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '0.5rem' }}>
                    EDIT SUPER ADMIN DETAILS
                  </h3>
                  
                  <div className="form-group">
                    <label className="form-label">Display Name</label>
                    <input
                      type="text"
                      className="form-input"
                      value={formData.username}
                      onChange={(e) => setFormData({...formData, username: e.target.value})}
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Phone Number</label>
                    <input
                      type="text"
                      className="form-input"
                      value={formData.phone}
                      onChange={(e) => setFormData({...formData, phone: e.target.value})}
                      placeholder="+1 (555) 000-0000"
                    />
                  </div>

                  <div className="form-group">
                    <label className="form-label">Platform Bio / Overview</label>
                    <textarea
                      className="form-input"
                      rows="4"
                      value={formData.bio}
                      onChange={(e) => setFormData({...formData, bio: e.target.value})}
                      placeholder="Describe platform operational status..."
                      style={{ resize: 'vertical' }}
                    ></textarea>
                  </div>
                </div>
              ) : (
                <>
                  <div>
                    <h3 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '0.75rem' }}>
                      SUPER ADMIN BIO
                    </h3>
                    <p style={{ color: 'var(--text-primary)', whiteSpace: 'pre-wrap', lineHeight: 1.6, fontSize: '0.95rem' }}>
                      {profile.bio || "No biography added yet."}
                    </p>
                  </div>

                  {profile.phone && (
                    <div>
                      <h3 style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px', marginBottom: '0.25rem' }}>
                        CONTACT PHONE
                      </h3>
                      <p style={{ color: 'var(--text-primary)', fontWeight: 600, fontSize: '0.95rem' }}>{profile.phone}</p>
                    </div>
                  )}
                </>
              )}
            </div>
          </div>

          {/* Registration & Invitation Links Section */}
          <div style={{
            backgroundColor: 'rgba(0, 0, 0, 0.01)',
            borderRadius: 'var(--radius-xl)',
            border: '1px solid var(--border-color)',
            padding: '1.75rem',
            marginTop: '2rem'
          }}>
            <h3 style={{ fontSize: '0.95rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '0.25px', marginBottom: '0.25rem' }}>
              Registration & Invitation Links
            </h3>
            <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '1.5rem', lineHeight: 1.5 }}>
              Share these URLs with platform stakeholders to allow them to request account creation. Role registration links are restricted to invited users.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {[
                { label: 'Super Admin Registration Link', url: `${window.location.origin}/auth?mode=super-admin-signup`, key: 'sa' },
                { label: 'Company Admin Registration Link', url: `${window.location.origin}/auth?mode=company-admin-signup`, key: 'ca' },
                { label: 'Content Creator Registration Link', url: `${window.location.origin}/auth?mode=creator-signup`, key: 'cc' },
                { label: 'Employee Registration Link', url: `${window.location.origin}/auth?mode=employee-signup`, key: 'emp' }
              ].map(link => (
                <div 
                  key={link.key} 
                  style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '1rem 1.25rem',
                    backgroundColor: 'var(--bg-secondary)',
                    borderRadius: 'var(--radius-lg)',
                    border: '1px solid var(--border-color)',
                    gap: '1rem'
                  }}
                >
                  <div style={{ flex: '1', minWidth: '200px' }}>
                    <div style={{ fontSize: '0.85rem', fontWeight: 700, color: 'var(--text-primary)' }}>{link.label}</div>
                    <div style={{
                      fontSize: '0.75rem',
                      fontFamily: 'monospace',
                      color: 'var(--text-secondary)',
                      userSelect: 'all',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      marginTop: '0.25rem'
                    }}>{link.url}</div>
                  </div>
                  <button
                    onClick={() => copyToClipboard(link.url, link.key)}
                    className="btn"
                    style={{
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      padding: '0.4rem 0.8rem',
                      borderRadius: '6px',
                      border: '1px solid',
                      borderColor: copyStatus[link.key] ? 'var(--success-color)' : 'var(--border-color)',
                      backgroundColor: copyStatus[link.key] ? 'var(--success-color)' : 'var(--bg-primary)',
                      color: copyStatus[link.key] ? 'white' : 'var(--text-primary)',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    {copyStatus[link.key] ? 'Copied!' : 'Copy Link'}
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      {toast && (
        <Toast
          message={toast.message}
          type={toast.type}
          onClose={() => setToast(null)}
        />
      )}
    </div>
  );
};

export default SuperAdminProfile;
