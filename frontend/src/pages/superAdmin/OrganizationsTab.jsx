import React, { useState, useEffect } from 'react';
import { Plus, Search, ShieldOff, RefreshCw, X, Check } from 'lucide-react';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const OrganizationsTab = () => {
  const [organizations, setOrganizations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [toast, setToast] = useState(null);
  
  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newOrg, setNewOrg] = useState({
    name: '',
    email: '',
    freeEmployees: 100,
    freeCourses: 10
  });
  const [modalError, setModalError] = useState('');
  const [modalSuccess, setModalSuccess] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const fetchOrgs = async () => {
    setLoading(true);
    try {
      // Backend is paginated: getAllOrganizations(Pageable pageable)
      // Pass a high limit for registry view
      const res = await api.get('/super-admin/organizations?size=100');
      if (res.data && res.data.content) {
        setOrganizations(res.data.content);
      } else {
        setOrganizations([]);
      }
    } catch (err) {
      console.error('Failed to load organizations', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchOrgs();
  }, []);

  const handleDeactivate = async (orgId) => {
    if (!window.confirm('Are you sure you want to deactivate this organization?')) return;
    try {
      await api.delete(`/super-admin/organizations/${orgId}`);
      setToast({ message: 'Organization deactivated successfully.', type: 'success' });
      fetchOrgs();
    } catch (err) {
      setToast({ message: 'Failed to deactivate organization.', type: 'error' });
    }
  };

  const handleProvisionSubmit = async (e) => {
    e.preventDefault();
    setModalError('');
    setModalSuccess('');
    
    if (!newOrg.name.trim() || !newOrg.email.trim() || newOrg.freeEmployees < 1 || newOrg.freeCourses < 1) {
      setModalError('All fields are mandatory and must be valid.');
      return;
    }
    
    setSubmitting(true);

    try {
      await api.post('/super-admin/organizations', newOrg);
      setModalSuccess('Organization provisioned successfully! Refreshing list...');
      setNewOrg({ name: '', email: '', freeEmployees: 100, freeCourses: 10 });
      setTimeout(() => {
        setIsModalOpen(false);
        setModalSuccess('');
        fetchOrgs();
      }, 1500);
    } catch (err) {
      setModalError('Failed to provision organization.');
    } finally {
      setSubmitting(false);
    }
  };

  const filteredOrgs = organizations.filter(o => {
    const matchesSearch = o.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                          o.email.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStatus = statusFilter === 'ALL' || o.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>Organization Registry</h2>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button 
            onClick={fetchOrgs} 
            className="btn btn-secondary" 
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
            title="Refresh list"
          >
            <RefreshCw size={16} />
          </button>
          <button 
            onClick={() => setIsModalOpen(true)} 
            className="btn btn-primary" 
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', border: 'none' }}
          >
            <Plus size={18} /> Provision Tenant
          </button>
        </div>
      </div>

      <div className="glass-panel" style={{ padding: 0, overflow: 'hidden' }}>
        {/* Toolbar */}
        <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border-color)', display: 'flex', gap: '1rem' }}>
          <div style={{ position: 'relative', flexGrow: 1, maxWidth: '400px' }}>
            <Search size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
            <input 
              type="text" 
              className="form-input" 
              placeholder="Search organizations..." 
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{ paddingLeft: '2.5rem', marginBottom: 0 }}
            />
          </div>
          <select 
            className="form-input" 
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            style={{ width: '180px', marginBottom: 0 }}
          >
            <option value="ALL">All Statuses</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="PENDING">PENDING</option>
            <option value="INACTIVE">INACTIVE</option>
          </select>
        </div>

        {/* Table */}
        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
            Loading organizations...
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
              <thead>
                <tr style={{ background: 'rgba(0,0,0,0.02)', color: 'var(--text-secondary)', fontSize: '0.85rem', textTransform: 'uppercase' }}>
                  <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Organization Name</th>
                  <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Primary Admin</th>
                  <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Employee Slots</th>
                  <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Course Quota</th>
                  <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Status</th>
                  <th style={{ padding: '1rem 1.5rem', fontWeight: 600, textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filteredOrgs.map((org) => (
                  <tr key={org.orgId} style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.95rem', transition: 'background 0.2s' }}>
                    <td style={{ padding: '1rem 1.5rem', fontWeight: 600, color: 'var(--text-primary)' }}>{org.name}</td>
                    <td style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)' }}>{org.email}</td>
                    <td style={{ padding: '1rem 1.5rem' }}>{org.freeEmployees}</td>
                    <td style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)' }}>{org.freeCourses}</td>
                    <td style={{ padding: '1rem 1.5rem' }}>
                      <span style={{
                        fontSize: '0.75rem',
                        background: org.status === 'ACTIVE' ? 'rgba(16, 185, 129, 0.1)' : org.status === 'PENDING' ? 'rgba(245, 158, 11, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                        color: org.status === 'ACTIVE' ? 'var(--success-color)' : org.status === 'PENDING' ? 'var(--warning-color)' : 'var(--danger-color)',
                        padding: '4px 10px',
                        borderRadius: '12px',
                        fontWeight: 700
                      }}>{org.status}</span>
                    </td>
                    <td style={{ padding: '1rem 1.5rem', textAlign: 'right' }}>
                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem' }}>
                        {org.status === 'ACTIVE' && (
                          <button 
                            onClick={() => handleDeactivate(org.orgId)} 
                            className="btn btn-secondary" 
                            style={{ padding: '0.4rem', color: 'var(--danger-color)', display: 'inline-flex' }} 
                            title="Deactivate"
                          >
                            <ShieldOff size={16} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {filteredOrgs.length === 0 && (
                  <tr>
                    <td colSpan="6" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                      No organizations found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Provision Tenant Modal */}
      {isModalOpen && (
        <div style={{
          position: 'fixed',
          top: 0,
          left: 0,
          right: 0,
          bottom: 0,
          background: 'rgba(15, 23, 42, 0.6)',
          backdropFilter: 'blur(4px)',
          display: 'flex',
          justifyContent: 'center',
          alignItems: 'center',
          zIndex: 1000,
          padding: '1rem'
        }}>
          <div className="glass-panel" style={{
            width: '100%',
            maxWidth: '500px',
            background: 'var(--bg-secondary)',
            color: 'var(--text-primary)',
            padding: '2rem',
            position: 'relative',
            border: '1px solid var(--border-color)',
            boxShadow: 'var(--shadow-lg)'
          }}>
            <button 
              onClick={() => setIsModalOpen(false)}
              style={{
                position: 'absolute',
                top: '1rem',
                right: '1rem',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                color: 'var(--text-secondary)'
              }}
            >
              <X size={20} />
            </button>

            <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
              Provision New Tenant (Organization)
            </h3>

            {modalError && (
              <div style={{
                background: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.2)',
                color: 'var(--danger-color)',
                padding: '0.75rem',
                borderRadius: '6px',
                fontSize: '0.85rem',
                marginBottom: '1rem'
              }}>
                {modalError}
              </div>
            )}

            {modalSuccess && (
              <div style={{
                background: 'rgba(16, 185, 129, 0.1)',
                border: '1px solid rgba(16, 185, 129, 0.2)',
                color: 'var(--success-color)',
                padding: '0.75rem',
                borderRadius: '6px',
                fontSize: '0.85rem',
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}>
                <Check size={16} />
                <span>{modalSuccess}</span>
              </div>
            )}

            <form onSubmit={handleProvisionSubmit}>
              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">Organization Name <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                <input 
                  type="text" 
                  required
                  placeholder="e.g. Wayne Enterprises"
                  className="form-input" 
                  value={newOrg.name}
                  onChange={(e) => setNewOrg(prev => ({ ...prev, name: e.target.value }))}
                />
              </div>

              <div className="form-group" style={{ marginBottom: '1rem' }}>
                <label className="form-label">Primary Admin Email <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                <input 
                  type="email" 
                  required
                  placeholder="e.g. admin@wayne.com"
                  className="form-input" 
                  value={newOrg.email}
                  onChange={(e) => setNewOrg(prev => ({ ...prev, email: e.target.value }))}
                />
              </div>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
                <div className="form-group">
                  <label className="form-label">Employee Limit <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                  <input 
                    type="number" 
                    required
                    min="1"
                    className="form-input" 
                    value={newOrg.freeEmployees}
                    onChange={(e) => setNewOrg(prev => ({ ...prev, freeEmployees: parseInt(e.target.value) || 0 }))}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">Course Quota <span style={{ color: 'var(--danger-color)' }}>*</span></label>
                  <input 
                    type="number" 
                    required
                    min="1"
                    className="form-input" 
                    value={newOrg.freeCourses}
                    onChange={(e) => setNewOrg(prev => ({ ...prev, freeCourses: parseInt(e.target.value) || 0 }))}
                  />
                </div>
              </div>

              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                <button 
                  type="button" 
                  className="btn btn-secondary" 
                  onClick={() => setIsModalOpen(false)}
                  disabled={submitting}
                >
                  Cancel
                </button>
                <button 
                  type="submit" 
                  className="btn btn-primary" 
                  disabled={submitting}
                  style={{ border: 'none' }}
                >
                  {submitting ? 'Provisioning...' : 'Provision Tenant'}
                </button>
              </div>
            </form>
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

export default OrganizationsTab;
