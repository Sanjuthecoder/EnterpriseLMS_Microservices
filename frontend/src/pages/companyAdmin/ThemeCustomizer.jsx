import React, { useState, useEffect } from 'react';
import { Palette, Layout, RefreshCw, Type, Eye } from 'lucide-react';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';
import { useTheme } from '../../contexts/ThemeContext';

const ThemeCustomizer = () => {
  const { updateTheme } = useTheme();
  const [primaryColor, setPrimaryColor] = useState('#2563eb');
  const [secondaryColor, setSecondaryColor] = useState('#10b981');
  const [fontFamily, setFontFamily] = useState('Inter, sans-serif');
  const [portalName, setPortalName] = useState('');
  const [logoUrl, setLogoUrl] = useState('');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState(null);

  const fetchTheme = async () => {
    setLoading(true);
    try {
      const response = await api.get('/company-admin/company');
      const company = response.data;
      if (company) {
        setLogoUrl(company.logoUrl || '');
        if (company.themeConfig) {
          const config = company.themeConfig;
          setPortalName(config.portalName || '');
          setPrimaryColor(config.primaryColor || '#2563eb');
          setSecondaryColor(config.secondaryColor || '#10b981');
          setFontFamily(config.fontFamily || 'Inter, sans-serif');
          
          applyThemeVariables({
            primaryColor: config.primaryColor || '#2563eb',
            secondaryColor: config.secondaryColor || '#10b981',
            fontFamily: config.fontFamily || 'Inter, sans-serif'
          });
        }
      }
    } catch (err) {
      console.error('Failed to load company details and theme:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTheme();
  }, []);

  const applyThemeVariables = (config) => {
    const root = document.documentElement;
    if (config.primaryColor) {
      root.style.setProperty('--primary-color', config.primaryColor);
      const hex = config.primaryColor.replace('#', '');
      const r = parseInt(hex.substring(0, 2), 16);
      const g = parseInt(hex.substring(2, 4), 16);
      const b = parseInt(hex.substring(4, 6), 16);
      root.style.setProperty('--primary-rgb', `${r}, ${g}, ${b}`);
    }
    if (config.secondaryColor) {
      root.style.setProperty('--secondary-color', config.secondaryColor);
    }
    if (config.fontFamily) {
      root.style.setProperty('--font-family', config.fontFamily);
      document.body.style.fontFamily = config.fontFamily;
    }
  };

  const handleLogoUpload = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        setToast({ message: 'Logo image size must be less than 2MB.', type: 'error' });
        return;
      }
      const reader = new FileReader();
      reader.onload = (event) => {
        setLogoUrl(event.target.result); // Base64 encoding
      };
      reader.readAsDataURL(file);
    }
  };

  const handleSaveTheme = async () => {
    setSaving(true);
    try {
      const payload = {
        logoUrl: logoUrl || null,
        themeConfig: {
          portalName,
          primaryColor,
          secondaryColor,
          fontFamily
        }
      };
      const result = await updateTheme(payload);
      if (result && !result.success) {
        throw new Error(result.message || 'Failed to save theme');
      }
      setToast({ message: 'Branding and portal settings customized successfully!', type: 'success' });
    } catch (err) {
      setToast({ message: err.message || 'Failed to save theme customizations.', type: 'error' });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Brand & Theme Customization
        </h2>
        <button 
          onClick={fetchTheme}
          className="btn btn-secondary"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>
      
      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Customize the appearance and branding of your organization's learning portal. Changes saved here are instantly applied for all employees.
      </p>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Loading branding settings...
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr', gap: '2rem' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            
            {/* Colors Section */}
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Palette size={18} color="var(--primary-color)" /> Portal Branding Colors
              </h3>
              
              {/* Primary Color */}
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.9rem', fontWeight: 600, marginBottom: '0.75rem', color: 'var(--text-secondary)' }}>
                  Primary Accent Color (Buttons, Links, Primary highlights)
                </label>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    {['#2563eb', '#0284c7', '#10b981', '#ef4444', '#8b5cf6', '#f59e0b', '#ec4899'].map(color => (
                      <button 
                        key={color} 
                        onClick={() => { setPrimaryColor(color); applyThemeVariables({ primaryColor: color, secondaryColor, fontFamily }); }}
                        style={{
                          width: '32px', height: '32px', borderRadius: '50%',
                          background: color, 
                          border: primaryColor === color ? '3px solid var(--text-primary)' : '2px solid transparent',
                          boxShadow: '0 2px 5px rgba(0,0,0,0.1)',
                          cursor: 'pointer',
                          transition: 'transform 0.1s'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
                        onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                      />
                    ))}
                  </div>
                  <div style={{ height: '30px', width: '1px', background: 'var(--border-color)' }}></div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <input 
                      type="color" 
                      value={primaryColor} 
                      onChange={(e) => { setPrimaryColor(e.target.value); applyThemeVariables({ primaryColor: e.target.value, secondaryColor, fontFamily }); }} 
                      style={{ width: '32px', height: '32px', padding: 0, cursor: 'pointer', border: 'none', background: 'transparent' }} 
                    />
                    <span style={{ fontFamily: 'monospace', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>{primaryColor.toUpperCase()}</span>
                  </div>
                </div>
              </div>

              {/* Secondary Color */}
              <div style={{ marginBottom: '1.5rem' }}>
                <label style={{ display: 'block', fontSize: '0.9rem', fontWeight: 600, marginBottom: '0.75rem', color: 'var(--text-secondary)' }}>
                  Secondary Color (Success badges, alternative accents)
                </label>
                <div style={{ display: 'flex', gap: '1rem', alignItems: 'center', flexWrap: 'wrap' }}>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    {['#10b981', '#06b6d4', '#3b82f6', '#14b8a6', '#84cc16', '#a855f7', '#64748b'].map(color => (
                      <button 
                        key={color} 
                        onClick={() => { setSecondaryColor(color); applyThemeVariables({ primaryColor, secondaryColor: color, fontFamily }); }}
                        style={{
                          width: '32px', height: '32px', borderRadius: '50%',
                          background: color, 
                          border: secondaryColor === color ? '3px solid var(--text-primary)' : '2px solid transparent',
                          boxShadow: '0 2px 5px rgba(0,0,0,0.1)',
                          cursor: 'pointer',
                          transition: 'transform 0.1s'
                        }}
                        onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
                        onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                      />
                    ))}
                  </div>
                  <div style={{ height: '30px', width: '1px', background: 'var(--border-color)' }}></div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <input 
                      type="color" 
                      value={secondaryColor} 
                      onChange={(e) => { setSecondaryColor(e.target.value); applyThemeVariables({ primaryColor, secondaryColor: e.target.value, fontFamily }); }} 
                      style={{ width: '32px', height: '32px', padding: 0, cursor: 'pointer', border: 'none', background: 'transparent' }} 
                    />
                    <span style={{ fontFamily: 'monospace', fontSize: '0.9rem', color: 'var(--text-secondary)' }}>{secondaryColor.toUpperCase()}</span>
                  </div>
                </div>
              </div>
            </div>

            {/* Portal Identity Section */}
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Type size={18} color="var(--primary-color)" /> Portal Text & Typography
              </h3>
              
              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label className="form-label">Custom Portal Title</label>
                <input 
                  type="text" 
                  className="form-input" 
                  placeholder="e.g. Acme Academy" 
                  value={portalName} 
                  onChange={(e) => setPortalName(e.target.value)}
                />
                <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginTop: '0.35rem' }}>
                  Replaces the default "EnterpriseLMS" text logo.
                </p>
              </div>

              <div className="form-group" style={{ marginBottom: '0.5rem' }}>
                <label className="form-label">Font Family</label>
                <select 
                  className="form-input"
                  value={fontFamily}
                  onChange={(e) => { setFontFamily(e.target.value); applyThemeVariables({ primaryColor, secondaryColor, fontFamily: e.target.value }); }}
                  style={{ cursor: 'pointer' }}
                >
                  <option value="Inter, sans-serif">Inter (Sleek sans-serif)</option>
                  <option value="'Outfit', sans-serif">Outfit (Modern sans-serif)</option>
                  <option value="'Roboto', sans-serif">Roboto (Clean geometric)</option>
                  <option value="'Poppins', sans-serif">Poppins (Friendly rounded)</option>
                  <option value="system-ui, sans-serif">System UI default</option>
                </select>
              </div>
            </div>

            {/* Logo Upload Section */}
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Layout size={18} color="var(--primary-color)" /> Company Logo
              </h3>
              
              <div style={{ 
                border: '2px dashed var(--border-color)', 
                borderRadius: 'var(--radius-lg)', 
                padding: '2rem', 
                textAlign: 'center',
                background: '#f8fafc',
                marginBottom: '1rem',
                position: 'relative',
                cursor: 'pointer'
              }}>
                {logoUrl ? (
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
                    <img src={logoUrl} alt="Logo Preview" style={{ maxHeight: '60px', maxWidth: '200px', objectFit: 'contain' }} />
                    <button 
                      type="button"
                      onClick={() => setLogoUrl('')}
                      className="btn"
                      style={{ fontSize: '0.8rem', padding: '0.3rem 0.8rem', backgroundColor: 'var(--danger-color)', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
                    >
                      Clear Logo
                    </button>
                  </div>
                ) : (
                  <>
                    <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1rem' }}>
                      Drag and drop your company logo here, or click to choose file
                    </p>
                    <input 
                      type="file" 
                      accept="image/*"
                      onChange={handleLogoUpload}
                      style={{ 
                        position: 'absolute', 
                        inset: 0, 
                        opacity: 0, 
                        cursor: 'pointer',
                        width: '100%',
                        height: '100%'
                      }} 
                    />
                    <button type="button" className="btn btn-secondary" style={{ pointerEvents: 'none' }}>Select Logo File</button>
                  </>
                )}
              </div>
              <p style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Recommended format: SVG or PNG with transparent background. Max size 2MB.</p>
            </div>

            <button 
              onClick={handleSaveTheme}
              disabled={saving}
              className="btn btn-primary" 
              style={{ width: '100%', padding: '0.8rem', fontSize: '1rem', fontWeight: 600 }}
            >
              {saving ? 'Saving Customizations...' : 'Save Brand Settings'}
            </button>
          </div>

          {/* Live Preview Column */}
          <div>
            <div className="glass-panel" style={{ overflow: 'hidden', height: '100%', display: 'flex', flexDirection: 'column', minHeight: '500px' }}>
              <div style={{ background: '#0f172a', padding: '1rem', color: 'white', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ fontSize: '0.9rem', fontWeight: 700, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Eye size={16} /> Live Employee Portal Preview
                </div>
              </div>
              
              <div style={{ padding: '2rem', background: 'var(--bg-primary)', flexGrow: 1, fontFamily: fontFamily }}>
                
                {/* Simulated Header */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '1rem' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    {logoUrl ? (
                      <img src={logoUrl} alt="Logo Preview" style={{ maxHeight: '32px', maxWidth: '100px', objectFit: 'contain' }} />
                    ) : (
                      <div style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--primary-color)' }}></div>
                    )}
                    <span style={{ fontWeight: 700, fontSize: '1.15rem' }}>
                      {portalName || 'EnterpriseLMS'}
                    </span>
                  </div>
                  <div style={{ width: '36px', height: '36px', borderRadius: '50%', background: 'var(--primary-color)', color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '0.85rem', fontWeight: 600 }}>
                    SJ
                  </div>
                </div>

                {/* Simulated Content */}
                <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
                  <h4 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>Welcome back, Sarah!</h4>
                  
                  <div style={{ background: '#ffffff', padding: '1.25rem', borderRadius: 'var(--radius-md)', boxShadow: 'var(--shadow-sm)', border: '1px solid var(--border-color)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem', alignItems: 'center' }}>
                      <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Cybersecurity Fundamentals</span>
                      <span style={{ background: `rgba(var(--primary-rgb), 0.1)`, color: 'var(--primary-color)', padding: '2px 8px', borderRadius: '10px', fontSize: '0.7rem', fontWeight: 700 }}>IN PROGRESS</span>
                    </div>
                    <div style={{ width: '100%', height: '6px', background: 'var(--border-color)', borderRadius: '3px', overflow: 'hidden', marginBottom: '0.5rem' }}>
                      <div style={{ width: '60%', height: '100%', background: 'var(--primary-color)' }} />
                    </div>
                    <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', textAlign: 'right' }}>60% Complete</div>
                  </div>

                  <div style={{ background: '#ffffff', padding: '1.25rem', borderRadius: 'var(--radius-md)', boxShadow: 'var(--shadow-sm)', border: '1px solid var(--border-color)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem', alignItems: 'center' }}>
                      <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Quiz Completion status</span>
                      <span style={{ color: 'var(--secondary-color)', fontSize: '0.9rem', fontWeight: 700 }}>88% Avg score</span>
                    </div>
                  </div>
                  
                  <button className="btn btn-primary" style={{ width: '100%', padding: '0.6rem 1rem', background: 'var(--primary-color)', border: 'none', color: 'white', borderRadius: 'var(--radius-md)', fontWeight: 600 }}>
                    Resume Course
                  </button>
                </div>

              </div>
            </div>
          </div>

        </div>
      )}

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

export default ThemeCustomizer;
