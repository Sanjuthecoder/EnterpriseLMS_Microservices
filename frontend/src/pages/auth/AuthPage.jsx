import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { GraduationCap, Lock, Mail, User, Phone, ShieldCheck, Landmark, Briefcase, Feather, ArrowLeft, CheckCircle } from 'lucide-react';
import api from '../../services/api';

const AuthPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { login } = useAuth();

  const mode = searchParams.get('mode'); // can be super-admin-signup, creator-signup, company-admin-signup, employee-signup, or null (login)

  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    phone: '',
    orgId: '',
    companyId: ''
  });

  const [organizations, setOrganizations] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const [loading, setLoading] = useState(false);
  const [forgotPasswordStep, setForgotPasswordStep] = useState(0); // 0 = inactive, 1 = email, 2 = verify and reset
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  // Load organizations if signing up for org/company related roles
  useEffect(() => {
    setError('');
    setSuccessMsg('');
    if (mode && mode !== 'super-admin-signup') {
      const fetchOrganizations = async () => {
        try {
          const res = await api.get('/auth/organizations');
          setOrganizations(res.data);
        } catch (err) {
          console.error('Failed to load organizations', err);
          setError('Could not fetch active organizations. Please ensure the backend is running.');
        }
      };
      fetchOrganizations();
    }
  }, [mode]);

  const handleOrgChange = async (e) => {
    const selectedOrgId = e.target.value;
    setFormData(prev => ({ ...prev, orgId: selectedOrgId, companyId: '' }));
    setCompanies([]);

    if (!selectedOrgId) return;

    try {
      const res = await api.get(`/auth/organizations/${selectedOrgId}/companies`);
      setCompanies(res.data);
    } catch (err) {
      console.error('Failed to load companies for organization', err);
    }
  };

  const handleLoginSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await login({ email: formData.email, password: formData.password });
      if (res.success) {
        if (res.role === 'SUPER_ADMIN') {
          navigate('/super-admin');
        } else if (res.role === 'COMPANY_ADMIN') {
          navigate('/company-admin');
        } else if (res.role === 'CREATOR') {
          navigate('/creator');
        } else {
          navigate('/employee');
        }
      } else {
        setError(res.message);
      }
    } catch (err) {
      setError('An unexpected error occurred during login.');
    } finally {
      setLoading(false);
    }
  };

  const handleSignupSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setLoading(true);

    let endpoint = '';
    if (mode === 'super-admin-signup') endpoint = '/auth/super-admin/signup';
    else if (mode === 'creator-signup') endpoint = '/auth/creator/signup';
    else if (mode === 'company-admin-signup') endpoint = '/auth/company-admin/signup';
    else if (mode === 'employee-signup') endpoint = '/auth/employee/signup';

    const payload = {
      username: formData.username,
      email: formData.email,
      password: formData.password,
      phone: formData.phone,
      orgId: formData.orgId ? parseInt(formData.orgId) : null,
      companyId: formData.companyId ? parseInt(formData.companyId) : null
    };

    try {
      const res = await api.post(endpoint, payload);
      setSuccessMsg(res.data);
      // Reset form
      setFormData({
        username: '',
        email: '',
        password: '',
        phone: '',
        orgId: '',
        companyId: ''
      });
    } catch (err) {
      setError('Signup failed');
    } finally {
      setLoading(false);
    }
  };

  const handleForgotPasswordSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      await api.post('/auth/forgot-password', { email: formData.email });
      setSuccessMsg('Verification code sent to your email.');
      setForgotPasswordStep(2);
    } catch (err) {
      setError('Request failed');
    } finally {
      setLoading(false);
    }
  };

  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    setLoading(true);
    try {
      await api.post('/auth/reset-password', {
        email: formData.email,
        token: resetToken,
        newPassword
      });
      setSuccessMsg('Password reset successfully. You can now log in.');
      setForgotPasswordStep(0);
      setResetToken('');
      setNewPassword('');
    } catch (err) {
      setError('Reset failed');
    } finally {
      setLoading(false);
    }
  };

  const getRoleTitle = () => {
    if (mode === 'super-admin-signup') return 'Super Admin';
    if (mode === 'creator-signup') return 'Content Creator';
    if (mode === 'company-admin-signup') return 'Company Admin';
    if (mode === 'employee-signup') return 'Employee Learner';
    return '';
  };

  const isSignup = !!mode;

  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
      padding: '2rem'
    }}>
      <div className="glass-panel" style={{
        width: '100%',
        maxWidth: '500px',
        padding: '3rem 2.5rem',
        boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.5)',
        background: 'rgba(30, 41, 59, 0.7)',
        borderColor: 'rgba(255, 255, 255, 0.08)',
        color: '#f8fafc'
      }}>
        {/* Top Logo */}
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '0.75rem', marginBottom: '2.5rem' }}>
          <GraduationCap size={44} color="#3b82f6" />
          <span style={{ fontWeight: 800, fontSize: '1.75rem', letterSpacing: '-0.75px', color: '#ffffff' }}>
            Enterprise<span style={{ color: '#3b82f6' }}>LMS</span>
          </span>
        </div>

        {/* Back to Login option if in signup mode */}
        {isSignup && (
          <button
            onClick={() => navigate('/auth')}
            style={{
              background: 'none',
              border: 'none',
              color: '#94a3b8',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              fontSize: '0.875rem',
              marginBottom: '1.5rem',
              padding: 0
            }}
          >
            <ArrowLeft size={16} /> Back to Login
          </button>
        )}

        <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1.5rem', color: '#ffffff', textAlign: 'center' }}>
          {isSignup ? `Register as ${getRoleTitle()}` : 'Sign In to Your Account'}
        </h2>

        {error && (
          <div style={{
            background: 'rgba(239, 68, 68, 0.15)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            marginBottom: '1.5rem',
            color: '#fca5a5',
            fontSize: '0.875rem'
          }}>
            {error}
          </div>
        )}

        {successMsg && (
          <div style={{
            background: 'rgba(16, 185, 129, 0.15)',
            border: '1px solid rgba(16, 185, 129, 0.3)',
            borderRadius: 'var(--radius-md)',
            padding: '1rem',
            marginBottom: '1.5rem',
            color: '#6ee7b7',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}>
            <CheckCircle size={20} />
            <div>
              <strong>Success!</strong> {successMsg}
            </div>
          </div>
        )}

        {!isSignup ? (
          forgotPasswordStep === 1 ? (
            /* FORGOT PASSWORD STEP 1: Request Code */
            <form onSubmit={handleForgotPasswordSubmit}>
              <p style={{ color: '#cbd5e1', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                Enter your registered email address and we'll send you a 6-digit verification code to reset your password.
              </p>
              <div className="form-group" style={{ marginBottom: '2rem' }}>
                <label className="form-label" style={{ color: '#cbd5e1' }}>Email Address</label>
                <div style={{ position: 'relative' }}>
                  <Mail size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                  <input
                    type="email"
                    required
                    placeholder="name@company.com"
                    className="form-input"
                    value={formData.email}
                    onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                    style={{
                      paddingLeft: '2.75rem',
                      background: 'rgba(15, 23, 42, 0.6)',
                      borderColor: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary"
                style={{ width: '100%', padding: '0.85rem', borderRadius: 'var(--radius-lg)', fontSize: '1rem', fontWeight: '600', background: '#2563eb', border: 'none', color: '#ffffff', marginBottom: '1rem' }}
              >
                {loading ? 'Sending...' : 'Send Verification Code'}
              </button>

              <div style={{ textAlign: 'center' }}>
                <button type="button" onClick={() => setForgotPasswordStep(0)} style={{ background: 'none', border: 'none', color: '#94a3b8', fontSize: '0.875rem', cursor: 'pointer' }}>
                  Cancel
                </button>
              </div>
            </form>
          ) : forgotPasswordStep === 2 ? (
            /* FORGOT PASSWORD STEP 2: Verify and Reset */
            <form onSubmit={handleResetPasswordSubmit}>
              <p style={{ color: '#cbd5e1', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
                We've sent a verification code to your email. Please enter it below along with your new password.
              </p>

              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label className="form-label" style={{ color: '#cbd5e1' }}>Verification Code</label>
                <div style={{ position: 'relative' }}>
                  <ShieldCheck size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                  <input
                    type="text"
                    required
                    placeholder="000000"
                    maxLength={6}
                    className="form-input"
                    value={resetToken}
                    onChange={(e) => setResetToken(e.target.value)}
                    style={{ paddingLeft: '2.75rem', background: 'rgba(15, 23, 42, 0.6)', borderColor: 'rgba(255, 255, 255, 0.1)', color: '#ffffff', letterSpacing: '2px', fontWeight: 600 }}
                  />
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '2rem' }}>
                <label className="form-label" style={{ color: '#cbd5e1' }}>New Password</label>
                <div style={{ position: 'relative' }}>
                  <Lock size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    className="form-input"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    style={{ paddingLeft: '2.75rem', background: 'rgba(15, 23, 42, 0.6)', borderColor: 'rgba(255, 255, 255, 0.1)', color: '#ffffff' }}
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary"
                style={{ width: '100%', padding: '0.85rem', borderRadius: 'var(--radius-lg)', fontSize: '1rem', fontWeight: '600', background: '#10b981', border: 'none', color: '#ffffff', marginBottom: '1rem' }}
              >
                {loading ? 'Resetting...' : 'Reset Password'}
              </button>

              <div style={{ textAlign: 'center' }}>
                <button type="button" onClick={() => setForgotPasswordStep(0)} style={{ background: 'none', border: 'none', color: '#94a3b8', fontSize: '0.875rem', cursor: 'pointer' }}>
                  Back to Login
                </button>
              </div>
            </form>
          ) : (
            /* LOGIN FORM */
            <form onSubmit={handleLoginSubmit}>
              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label className="form-label" style={{ color: '#cbd5e1' }}>Email Address</label>
                <div style={{ position: 'relative' }}>
                  <Mail size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                  <input
                    type="email"
                    required
                    placeholder="name@company.com"
                    className="form-input"
                    value={formData.email}
                    onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                    style={{
                      paddingLeft: '2.75rem',
                      background: 'rgba(15, 23, 42, 0.6)',
                      borderColor: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  />
                </div>
              </div>

              <div className="form-group" style={{ marginBottom: '2rem' }}>
                <label className="form-label" style={{ color: '#cbd5e1' }}>Password</label>
                <div style={{ position: 'relative' }}>
                  <Lock size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                  <input
                    type="password"
                    required
                    placeholder="••••••••"
                    className="form-input"
                    value={formData.password}
                    onChange={(e) => setFormData(prev => ({ ...prev, password: e.target.value }))}
                    style={{
                      paddingLeft: '2.75rem',
                      background: 'rgba(15, 23, 42, 0.6)',
                      borderColor: 'rgba(255, 255, 255, 0.1)',
                      color: '#ffffff'
                    }}
                  />
                </div>
              </div>
              <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: '1.5rem', marginTop: '-1rem' }}>
                <button
                  type="button"
                  onClick={() => setForgotPasswordStep(1)}
                  style={{ background: 'none', border: 'none', color: '#3b82f6', fontSize: '0.85rem', cursor: 'pointer', fontWeight: 500 }}
                >
                  Forgot Password?
                </button>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="btn btn-primary"
                style={{
                  width: '100%',
                  padding: '0.85rem',
                  borderRadius: 'var(--radius-lg)',
                  fontSize: '1rem',
                  fontWeight: '600',
                  background: '#2563eb',
                  boxShadow: '0 4px 14px rgba(37, 99, 235, 0.4)',
                  border: 'none',
                  color: '#ffffff'
                }}
              >
                {loading ? 'Authenticating...' : 'Login'}
              </button>
            </form>
          )
        ) : (
          /* REGISTRATION FORM */
          <form onSubmit={handleSignupSubmit}>
            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ color: '#cbd5e1' }}>Username</label>
              <div style={{ position: 'relative' }}>
                <User size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                <input
                  type="text"
                  required
                  placeholder="johndoe"
                  className="form-input"
                  value={formData.username}
                  onChange={(e) => setFormData(prev => ({ ...prev, username: e.target.value }))}
                  style={{
                    paddingLeft: '2.75rem',
                    background: 'rgba(15, 23, 42, 0.6)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff'
                  }}
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ color: '#cbd5e1' }}>Email Address</label>
              <div style={{ position: 'relative' }}>
                <Mail size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                <input
                  type="email"
                  required
                  placeholder="name@company.com"
                  className="form-input"
                  value={formData.email}
                  onChange={(e) => setFormData(prev => ({ ...prev, email: e.target.value }))}
                  style={{
                    paddingLeft: '2.75rem',
                    background: 'rgba(15, 23, 42, 0.6)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff'
                  }}
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ color: '#cbd5e1' }}>Password</label>
              <div style={{ position: 'relative' }}>
                <Lock size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  className="form-input"
                  value={formData.password}
                  onChange={(e) => setFormData(prev => ({ ...prev, password: e.target.value }))}
                  style={{
                    paddingLeft: '2.75rem',
                    background: 'rgba(15, 23, 42, 0.6)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff'
                  }}
                />
              </div>
            </div>

            <div className="form-group" style={{ marginBottom: '1.25rem' }}>
              <label className="form-label" style={{ color: '#cbd5e1' }}>Phone Number</label>
              <div style={{ position: 'relative' }}>
                <Phone size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
                <input
                  type="text"
                  placeholder="+1 (555) 000-0000"
                  className="form-input"
                  value={formData.phone}
                  onChange={(e) => setFormData(prev => ({ ...prev, phone: e.target.value }))}
                  style={{
                    paddingLeft: '2.75rem',
                    background: 'rgba(15, 23, 42, 0.6)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff'
                  }}
                />
              </div>
            </div>

            {/* Organization Dropdown (Company Admin, Employee) */}
            {mode !== 'super-admin-signup' && mode !== 'creator-signup' && (
              <div className="form-group" style={{ marginBottom: '1.25rem' }}>
                <label className="form-label" style={{ color: '#cbd5e1' }}>Select Organization</label>
                <select
                  required
                  className="form-input"
                  value={formData.orgId}
                  onChange={handleOrgChange}
                  style={{
                    background: 'rgba(15, 23, 42, 0.6)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    height: '42px'
                  }}
                >
                  <option value="" style={{ background: '#1e293b' }}>-- Select Organization --</option>
                  {organizations.map(org => (
                    <option key={org.orgId} value={org.orgId} style={{ background: '#1e293b' }}>{org.name}</option>
                  ))}
                </select>
              </div>
            )}

            {/* Company Dropdown (Company Admin, Employee) */}
            {(mode === 'company-admin-signup' || mode === 'employee-signup') && (
              <div className="form-group" style={{ marginBottom: '2rem' }}>
                <label className="form-label" style={{ color: '#cbd5e1' }}>Select Company</label>
                <select
                  required
                  className="form-input"
                  value={formData.companyId}
                  onChange={(e) => setFormData(prev => ({ ...prev, companyId: e.target.value }))}
                  disabled={!formData.orgId}
                  style={{
                    background: 'rgba(15, 23, 42, 0.6)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    color: '#ffffff',
                    height: '42px'
                  }}
                >
                  <option value="" style={{ background: '#1e293b' }}>-- Select Company --</option>
                  {companies.map(comp => (
                    <option key={comp.companyId} value={comp.companyId} style={{ background: '#1e293b' }}>{comp.name}</option>
                  ))}
                </select>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="btn btn-primary"
              style={{
                width: '100%',
                padding: '0.85rem',
                borderRadius: 'var(--radius-lg)',
                fontSize: '1rem',
                fontWeight: '600',
                background: '#10b981',
                boxShadow: '0 4px 14px rgba(16, 185, 129, 0.4)',
                border: 'none',
                color: '#ffffff'
              }}
            >
              {loading ? 'Creating Account...' : 'Register'}
            </button>
          </form>
        )}

        {/* Small Notice / Demo helper */}
        {/* {!isSignup && (
          <div style={{ marginTop: '2rem', borderTop: '1px solid rgba(255,255,255,0.06)', paddingTop: '1.5rem', textAlign: 'center' }}>
            <p style={{ fontSize: '0.85rem', color: '#94a3b8', marginBottom: '0.75rem' }}>
              <strong>Demo Hint:</strong> Use the seeded Super Admin credentials to start:
            </p>
            <div style={{ fontSize: '0.8rem', background: 'rgba(15, 23, 42, 0.4)', padding: '0.5rem', borderRadius: '4px', fontFamily: 'monospace', color: '#cbd5e1', display: 'inline-block' }}>
              Email: superadmin@lms.com<br />
              Password: Password123
            </div>
          </div>
        )} */}
      </div>
    </div>
  );
};

export default AuthPage;
