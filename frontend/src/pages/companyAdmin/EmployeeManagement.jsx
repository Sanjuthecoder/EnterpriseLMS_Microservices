import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { UploadCloud, CheckCircle, Users, Search, AlertCircle, RefreshCw } from 'lucide-react';
import api from '../../services/api';
import Toast from '../../components/shared/Toast';

const EmployeeManagement = () => {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState('roster'); // 'roster', 'import', 'approvals'
  const [employees, setEmployees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [processing, setProcessing] = useState({});
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedDept, setSelectedDept] = useState('All Departments');
  const [toast, setToast] = useState(null);
  const [importReport, setImportReport] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const fetchEmployees = async (page = 0) => {
    setLoading(true);
    try {
      const response = await api.get(`/company-admin/employees/paged?page=${page}&size=10`);
      // Spring Data Page returns elements in .content
      if (response.data && response.data.content) {
        setEmployees(response.data.content);
        setTotalPages(response.data.totalPages);
        setCurrentPage(response.data.number);
      } else {
        setEmployees(response.data || []);
      }
    } catch (err) {
      console.error('Failed to fetch employees list:', err);
      setToast({ message: 'Failed to retrieve employees list.', type: 'error' });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchEmployees(0);
  }, []);

  const handleApprove = async (employeeId) => {
    setProcessing(prev => ({ ...prev, [employeeId]: true }));
    try {
      await api.post(`/company-admin/employees/${employeeId}/approve`);
      setToast({ message: 'Employee registration approved successfully!', type: 'success' });
      fetchEmployees();
    } catch (err) {
      setToast({ message: 'Failed to approve employee.', type: 'error' });
    } finally {
      setProcessing(prev => {
        const next = { ...prev };
        delete next[employeeId];
        return next;
      });
    }
  };

  const handleCsvUpload = async (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (event) => {
      const text = event.target.result;
      const lines = text.split('\n');
      const employeesToImport = [];

      for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;

        const cols = line.split(',');
        // Expected structure: username, email, phone, department, password
        if (cols.length >= 2) {
          employeesToImport.push({
            username: cols[0]?.trim(),
            email: cols[1]?.trim(),
            phone: cols[2]?.trim() || '',
            department: cols[3]?.trim() || 'General',
            password: cols[4]?.trim() || ''
          });
        }
      }

      if (employeesToImport.length === 0) {
        setToast({ message: 'No valid rows found in CSV.', type: 'error' });
        return;
      }

      try {
        const res = await api.post('/company-admin/employees/bulk-import', { employees: employeesToImport });
        setImportReport(res.data);
        if (res.status === 207 || res.data.failedImports > 0) {
          setToast({ message: `Bulk import completed with some failures: ${res.data.failedImports} failed.`, type: 'error' });
        } else {
          setToast({ message: `Successfully imported ${res.data.successfullyImported} employees!`, type: 'success' });
        }
        fetchEmployees();
      } catch (err) {
        setToast({ message: 'Bulk import failed.', type: 'error' });
      }
    };

    reader.readAsText(file);
    e.target.value = ''; // Reset file input
  };

  // Filter roster and pending lists
  const rosterList = employees.filter(emp => emp.status !== 'PENDING');
  const pendingApprovals = employees.filter(emp => emp.status === 'PENDING');

  const filteredRoster = rosterList.filter(emp => {
    const matchesSearch = 
      (emp.username && emp.username.toLowerCase().includes(searchTerm.toLowerCase())) ||
      (emp.email && emp.email.toLowerCase().includes(searchTerm.toLowerCase()));
    const matchesDept = 
      selectedDept === 'All Departments' || 
      (emp.department && emp.department.toLowerCase() === selectedDept.toLowerCase());
    return matchesSearch && matchesDept;
  });

  // Get distinct departments for filter dropdown
  const departments = ['All Departments', ...new Set(rosterList.map(e => e.department).filter(Boolean))];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Staff Management
        </h2>
        <button 
          onClick={() => fetchEmployees(currentPage)}
          className="btn btn-secondary"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: '1rem', borderBottom: '1px solid var(--border-color)', marginBottom: '2rem' }}>
        <button 
          onClick={() => setActiveTab('roster')}
          style={{ padding: '0.75rem 1rem', background: 'none', border: 'none', borderBottom: activeTab === 'roster' ? '2px solid var(--primary-color)' : '2px solid transparent', color: activeTab === 'roster' ? 'var(--primary-color)' : 'var(--text-secondary)', fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s' }}
        >
          Employee Roster ({rosterList.length})
        </button>
        <button 
          onClick={() => setActiveTab('import')}
          style={{ padding: '0.75rem 1rem', background: 'none', border: 'none', borderBottom: activeTab === 'import' ? '2px solid var(--primary-color)' : '2px solid transparent', color: activeTab === 'import' ? 'var(--primary-color)' : 'var(--text-secondary)', fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s' }}
        >
          Bulk Import (CSV)
        </button>
        <button 
          onClick={() => setActiveTab('approvals')}
          style={{ padding: '0.75rem 1rem', background: 'none', border: 'none', borderBottom: activeTab === 'approvals' ? '2px solid var(--primary-color)' : '2px solid transparent', color: activeTab === 'approvals' ? 'var(--primary-color)' : 'var(--text-secondary)', fontWeight: 600, cursor: 'pointer', transition: 'all 0.2s', display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          Pending Approvals 
          {pendingApprovals.length > 0 && (
            <span style={{ background: 'var(--danger-color)', color: 'white', padding: '2px 6px', borderRadius: '12px', fontSize: '0.7rem' }}>
              {pendingApprovals.length}
            </span>
          )}
        </button>
      </div>

      {loading ? (
        <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
          Loading staff directory...
        </div>
      ) : (
        <>
          {/* Tab Content: Roster */}
          {activeTab === 'roster' && (
            <div className="glass-panel">
              <div style={{ padding: '1.5rem', borderBottom: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
                <div style={{ position: 'relative', width: '300px' }}>
                  <Search size={18} style={{ position: 'absolute', left: '1rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-secondary)' }} />
                  <input 
                    type="text" 
                    className="form-input" 
                    placeholder="Search employees..." 
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    style={{ paddingLeft: '2.5rem', marginBottom: 0 }} 
                  />
                </div>
                <select 
                  className="form-input" 
                  value={selectedDept}
                  onChange={(e) => setSelectedDept(e.target.value)}
                  style={{ width: '200px', marginBottom: 0 }}
                >
                  {departments.map((dept, idx) => (
                    <option key={idx} value={dept}>{dept}</option>
                  ))}
                </select>
              </div>
              
              <div style={{ overflowX: 'auto' }}>
                {filteredRoster.length === 0 ? (
                  <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No registered employees matching filters.
                  </div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ background: 'rgba(0,0,0,0.02)', color: 'var(--text-secondary)', fontSize: '0.85rem', textTransform: 'uppercase' }}>
                        <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Name</th>
                        <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Email</th>
                        <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Department</th>
                        <th style={{ padding: '1rem 1.5rem', fontWeight: 600 }}>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredRoster.map(emp => (
                        <tr 
                          key={emp.userId} 
                          style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.95rem' }}
                        >
                          <td style={{ padding: '1rem 1.5rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                            <span>{emp.username}</span>
                          </td>
                          <td style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)' }}>{emp.email}</td>
                          <td style={{ padding: '1rem 1.5rem', color: 'var(--text-secondary)' }}>{emp.department || 'General'}</td>
                          <td style={{ padding: '1rem 1.5rem' }}>
                            <span style={{
                              fontSize: '0.75rem',
                              background: emp.status === 'ACTIVE' ? 'rgba(16, 185, 129, 0.1)' : 'rgba(239, 68, 68, 0.1)',
                              color: emp.status === 'ACTIVE' ? 'var(--success-color)' : 'var(--danger-color)',
                              padding: '4px 10px',
                              borderRadius: '12px',
                              fontWeight: 700
                            }}>{emp.status}</span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              {/* Pagination Controls */}
              {totalPages > 1 && (
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '1.5rem', borderTop: '1px solid var(--border-color)' }}>
                  <span style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
                    Page {currentPage + 1} of {totalPages}
                  </span>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <button 
                      disabled={currentPage === 0}
                      onClick={() => fetchEmployees(currentPage - 1)}
                      className="btn btn-secondary"
                      style={{ padding: '0.5rem 1rem', fontSize: '0.85rem' }}
                    >
                      Previous
                    </button>
                    <button 
                      disabled={currentPage === totalPages - 1}
                      onClick={() => fetchEmployees(currentPage + 1)}
                      className="btn btn-primary"
                      style={{ padding: '0.5rem 1rem', fontSize: '0.85rem' }}
                    >
                      Next
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Tab Content: Import */}
          {activeTab === 'import' && (
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem' }}>
              <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center' }}>
                <div style={{ width: '60px', height: '60px', borderRadius: '50%', background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem auto' }}>
                  <UploadCloud size={30} />
                </div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '0.5rem' }}>Upload Employee CSV</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem', marginBottom: '2rem' }}>
                  Upload a CSV file containing columns: `username`, `email`, `phone`, and `department`. The system will instantly register accounts and generate temporary login details.
                </p>
                <input 
                  type="file" 
                  accept=".csv" 
                  className="form-input" 
                  style={{ display: 'none' }} 
                  id="csvUpload" 
                  onChange={handleCsvUpload}
                />
                <label htmlFor="csvUpload" className="btn btn-primary" style={{ display: 'inline-block', cursor: 'pointer' }}>
                  Select CSV File
                </label>
              </div>

              <div className="glass-panel" style={{ padding: '2rem' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <AlertCircle size={18} color="var(--primary-color)" /> CSV File Template & Format
                </h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>
                  Please ensure your CSV uses comma separators. You can structure it using the example columns shown below (password is optional):
                </p>
                <pre style={{
                  padding: '1rem',
                  backgroundColor: '#f1f5f9',
                  borderRadius: 'var(--radius-md)',
                  fontFamily: 'monospace',
                  fontSize: '0.85rem',
                  lineHeight: '1.5',
                  overflowX: 'auto',
                  border: '1px solid var(--border-color)',
                  marginBottom: '1rem'
                }}>
{`username,email,phone,department,password
john.doe,john@acme.com,+15551234567,Engineering,MyPass123
jane.smith,jane@acme.com,,Sales,`}
                </pre>
                
                {importReport && (
                  <div style={{ marginTop: '1.5rem' }}>
                    <h4 style={{ fontSize: '0.95rem', fontWeight: 600, marginBottom: '1rem' }}>Latest Import Results:</h4>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                      <div style={{ padding: '0.75rem 1rem', borderLeft: '3px solid var(--success-color)', background: 'rgba(16, 185, 129, 0.05)', borderRadius: '4px' }}>
                        <strong style={{ fontSize: '0.9rem', color: 'var(--success-color)' }}>{importReport.successfullyImported} Rows Processed</strong>
                        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>Successfully added to employee list.</p>
                      </div>

                      {importReport.importedEmployees && importReport.importedEmployees.length > 0 && (
                        <div style={{ marginTop: '1rem', maxHeight: '200px', overflowY: 'auto', border: '1px solid var(--border-color)', borderRadius: '8px' }}>
                          <table style={{ width: '100%', fontSize: '0.85rem', borderCollapse: 'collapse' }}>
                            <thead>
                              <tr style={{ background: 'rgba(0,0,0,0.02)', borderBottom: '1px solid var(--border-color)' }}>
                                <th style={{ padding: '0.5rem', textAlign: 'left', fontWeight: 600 }}>Username</th>
                                <th style={{ padding: '0.5rem', textAlign: 'left', fontWeight: 600 }}>Email</th>
                                <th style={{ padding: '0.5rem', textAlign: 'left', fontWeight: 600 }}>Password</th>
                              </tr>
                            </thead>
                            <tbody>
                              {importReport.importedEmployees.map((emp, idx) => (
                                <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)' }}>
                                  <td style={{ padding: '0.5rem', color: 'var(--text-primary)' }}>{emp.username}</td>
                                  <td style={{ padding: '0.5rem', color: 'var(--text-secondary)' }}>{emp.email}</td>
                                  <td style={{ padding: '0.5rem', fontFamily: 'monospace', fontWeight: 'bold', color: 'var(--primary-color)' }}>{emp.temporaryPassword}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      )}
                      
                      {importReport.failedImports > 0 && (
                        <div style={{ padding: '0.75rem 1rem', borderLeft: '3px solid var(--danger-color)', background: 'rgba(239, 68, 68, 0.05)', borderRadius: '4px' }}>
                          <strong style={{ fontSize: '0.9rem', color: 'var(--danger-color)' }}>{importReport.failedImports} Rows Failed</strong>
                          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.25rem', marginBottom: '0.5rem' }}>Check validation rules: duplicates or invalid email formats.</p>
                          {importReport.errors && importReport.errors.length > 0 && (
                            <div style={{ maxHeight: '150px', overflowY: 'auto', border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '6px', background: '#fff' }}>
                              <table style={{ width: '100%', fontSize: '0.8rem', borderCollapse: 'collapse' }}>
                                <thead>
                                  <tr style={{ background: 'rgba(239, 68, 68, 0.05)' }}>
                                    <th style={{ padding: '0.5rem', textAlign: 'left', color: 'var(--danger-color)', borderBottom: '1px solid rgba(239, 68, 68, 0.2)' }}>Detailed Reason</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {importReport.errors.map((errStr, idx) => (
                                    <tr key={idx} style={{ borderBottom: '1px solid rgba(239, 68, 68, 0.1)' }}>
                                      <td style={{ padding: '0.5rem', color: 'var(--text-secondary)' }}>{errStr}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          )}
                        </div>
                      )}
                      
                      {importReport.emailsFailed > 0 && (
                        <div style={{ padding: '0.75rem 1rem', borderLeft: '3px solid #f59e0b', background: 'rgba(245, 158, 11, 0.05)', borderRadius: '4px', marginTop: '0.5rem' }}>
                          <strong style={{ fontSize: '0.9rem', color: '#f59e0b' }}>{importReport.emailsFailed} Emails Failed to Send</strong>
                          <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>Accounts were created, but onboarding emails could not be sent to these users. Please send them manually.</p>
                          {importReport.failedEmails && importReport.failedEmails.length > 0 && (
                            <div style={{ marginTop: '1rem', maxHeight: '150px', overflowY: 'auto', border: '1px solid rgba(245, 158, 11, 0.3)', borderRadius: '8px' }}>
                              <table style={{ width: '100%', fontSize: '0.85rem', borderCollapse: 'collapse' }}>
                                <thead>
                                  <tr style={{ background: 'rgba(245, 158, 11, 0.1)', borderBottom: '1px solid rgba(245, 158, 11, 0.3)' }}>
                                    <th style={{ padding: '0.5rem', textAlign: 'left', fontWeight: 600, color: '#b45309' }}>Username</th>
                                    <th style={{ padding: '0.5rem', textAlign: 'left', fontWeight: 600, color: '#b45309' }}>Email</th>
                                    <th style={{ padding: '0.5rem', textAlign: 'left', fontWeight: 600, color: '#b45309' }}>Reason</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {importReport.failedEmails.map((emp, idx) => (
                                    <tr key={idx} style={{ borderBottom: '1px solid rgba(245, 158, 11, 0.2)' }}>
                                      <td style={{ padding: '0.5rem', color: 'var(--text-primary)' }}>{emp.username}</td>
                                      <td style={{ padding: '0.5rem', color: 'var(--text-secondary)' }}>{emp.email}</td>
                                      <td style={{ padding: '0.5rem', color: 'var(--danger-color)', fontSize: '0.8rem' }}>{emp.reason}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Tab Content: Approvals */}
          {activeTab === 'approvals' && (
            <div className="glass-panel" style={{ padding: '1.5rem' }}>
              <p style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
                These employees registered using your organization context. Approve them to grant access to training courses and learning portals.
              </p>
              
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {pendingApprovals.map(emp => (
                  <div 
                    key={emp.userId} 
                    style={{ 
                      display: 'flex', 
                      justifyContent: 'space-between', 
                      alignItems: 'center', 
                      padding: '1.5rem', 
                      border: '1px solid var(--border-color)', 
                      borderRadius: 'var(--radius-md)', 
                      background: '#f8fafc',
                      flexWrap: 'wrap',
                      gap: '1rem'
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '1.1rem' }}>{emp.username}</div>
                      <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginTop: '0.25rem' }}>
                        {emp.email} • {emp.department || 'General'}
                      </div>
                    </div>
                    <button 
                      onClick={() => handleApprove(emp.userId)}
                      disabled={processing[emp.userId]}
                      className="btn btn-primary" 
                      style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', background: 'var(--success-color)', border: 'none' }}
                    >
                      <CheckCircle size={16} /> 
                      {processing[emp.userId] ? 'Approving...' : 'Approve & Onboard'}
                    </button>
                  </div>
                ))}
                
                {pendingApprovals.length === 0 && (
                  <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No pending self-registered users waiting for approval.
                  </div>
                )}
              </div>
            </div>
          )}
        </>
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

export default EmployeeManagement;
