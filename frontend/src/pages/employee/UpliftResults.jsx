import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Target, ArrowLeft, CheckCircle, TrendingUp } from 'lucide-react';
import Navbar from '../../components/shared/Navbar';
import api from '../../services/api';
import { toast } from 'react-hot-toast';

const UpliftResults = () => {
  const navigate = useNavigate();
  const { courseId } = useParams();
  
  const [upliftReport, setUpliftReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [certificateStatus, setCertificateStatus] = useState('ELIGIBLE'); // Default from backend

  useEffect(() => {
    const fetchReport = async () => {
      try {
        const response = await api.get(`/employees/courses/${courseId}/uplift`);
        
        // Transform backend response
        const data = response.data;
        const concepts = [];
        if (data.upliftReport) {
            if (data.upliftReport.conceptsGained) {
                data.upliftReport.conceptsGained.forEach(c => {
                    concepts.push({ name: c, pre: 'Incorrect', post: 'Correct', result: 'Skill Gained', color: 'var(--success-color)' });
                });
            }
            if (data.upliftReport.stillStruggling) {
                data.upliftReport.stillStruggling.forEach(c => {
                    concepts.push({ name: c, pre: 'Incorrect', post: 'Incorrect', result: 'Needs Review', color: 'var(--danger-color)' });
                });
            }
        }
        
        setUpliftReport({
          course: data.courseTitle || 'Course Assessment',
          preQuizScore: data.preQuizScore + '%',
          postQuizScore: data.postQuizScore + '%',
          upliftPercent: (data.upliftPercent > 0 ? '+' : '') + data.upliftPercent + '%',
          concepts: concepts
        });
        
        // Also fetch dashboard to get certificate status
        const dashResponse = await api.get('/employees/dashboard');
        const course = dashResponse.data.find(c => c.courseId === parseInt(courseId));
        if (course) {
            setCertificateStatus(course.certificateStatus);
        }
      } catch (err) {
        console.error("Failed to load uplift report", err);
        setError("Failed to load your results. Please try again later.");
      } finally {
        setLoading(false);
      }
    };
    fetchReport();
  }, [courseId]);

  const handleRequestCertificate = async () => {
    try {
        await api.post(`/employees/courses/${courseId}/certificate/request`);
        setCertificateStatus('REQUESTED');
        toast.success('Certificate requested successfully.');
    } catch (err) {
        toast.error('Failed to request certificate. Please try again.');
        console.error('Certificate request error:', err);
    }
  };

  if (loading) return <div style={{ padding: '3rem', textAlign: 'center' }}>Loading results...</div>;
  if (error) return <div style={{ padding: '3rem', textAlign: 'center', color: 'red' }}>{error}</div>;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--bg-secondary)' }}>
      <Navbar roleTitle="Employee Learner" />
      <div style={{ maxWidth: '800px', margin: '3rem auto', padding: '0 2rem' }}>
        
        <button 
          onClick={() => navigate('/employee')} 
          className="btn btn-secondary"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '2rem' }}
        >
          <ArrowLeft size={16} /> Back to Dashboard
        </button>

        <div className="glass-panel" style={{ padding: '3rem' }}>
          <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
            <div style={{ width: '80px', height: '80px', borderRadius: '50%', background: 'rgba(37, 99, 235, 0.1)', color: 'var(--primary-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem auto' }}>
              <TrendingUp size={40} />
            </div>
            <h1 style={{ fontSize: '2rem', fontWeight: 800, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>Skill Uplift Report</h1>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem' }}>{upliftReport.course}</p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '1rem', marginBottom: '3rem' }}>
            <div style={{ background: '#f8fafc', padding: '1.5rem', borderRadius: 'var(--radius-md)', textAlign: 'center', border: '1px solid var(--border-color)' }}>
              <div style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.5rem' }}>Diagnostic Pre-Quiz</div>
              <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--text-primary)' }}>{upliftReport.preQuizScore}</div>
            </div>
            <div style={{ background: 'rgba(16, 185, 129, 0.05)', padding: '1.5rem', borderRadius: 'var(--radius-md)', textAlign: 'center', border: '1px solid rgba(16, 185, 129, 0.2)' }}>
              <div style={{ fontSize: '0.9rem', color: 'var(--success-color)', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.5rem' }}>Final Post-Quiz</div>
              <div style={{ fontSize: '2rem', fontWeight: 700, color: 'var(--success-color)' }}>{upliftReport.postQuizScore}</div>
            </div>
            <div style={{ background: 'var(--primary-color)', padding: '1.5rem', borderRadius: 'var(--radius-md)', textAlign: 'center', color: 'white' }}>
              <div style={{ fontSize: '0.9rem', fontWeight: 600, textTransform: 'uppercase', marginBottom: '0.5rem', opacity: 0.9 }}>Net Skill Gained</div>
              <div style={{ fontSize: '2rem', fontWeight: 700 }}>{upliftReport.upliftPercent}</div>
            </div>
          </div>

          {upliftReport.concepts.length > 0 && (
            <>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Target size={20} color="var(--primary-color)" /> Concept Breakdown
              </h3>
              
              <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                    <th style={{ padding: '1rem 0', width: '40%' }}>Core Concept</th>
                    <th style={{ padding: '1rem 0' }}>Pre-Quiz</th>
                    <th style={{ padding: '1rem 0' }}>Post-Quiz</th>
                    <th style={{ padding: '1rem 0', textAlign: 'right' }}>Result</th>
                  </tr>
                </thead>
                <tbody>
                  {upliftReport.concepts.map((concept, idx) => (
                    <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.95rem' }}>
                      <td style={{ padding: '1.25rem 0', fontWeight: 600 }}>{concept.name}</td>
                      <td style={{ padding: '1.25rem 0', color: concept.pre === 'Correct' ? 'var(--success-color)' : 'var(--danger-color)' }}>{concept.pre}</td>
                      <td style={{ padding: '1.25rem 0', color: concept.post === 'Correct' ? 'var(--success-color)' : 'var(--danger-color)' }}>{concept.post}</td>
                      <td style={{ padding: '1.25rem 0', textAlign: 'right', fontWeight: 700, color: concept.color }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.5rem' }}>
                          {concept.result === 'Skill Gained' && <CheckCircle size={16} />}
                          {concept.result}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </>
          )}

          <div style={{ marginTop: '3rem', textAlign: 'center' }}>
            {certificateStatus === 'ELIGIBLE' && (
                <button onClick={handleRequestCertificate} className="btn btn-primary" style={{ padding: '1rem 2.5rem', fontSize: '1.1rem' }}>
                    Request Certificate
                </button>
            )}
            {certificateStatus === 'REQUESTED' && (
                <button disabled className="btn" style={{ padding: '1rem 2.5rem', fontSize: '1.1rem', background: '#e2e8f0', color: '#64748b' }}>
                    Request Pending Approval
                </button>
            )}
            {certificateStatus === 'APPROVED' && (
                <button onClick={() => navigate('/employee/certificates')} className="btn btn-primary" style={{ padding: '1rem 2.5rem', fontSize: '1.1rem', background: 'var(--success-color)' }}>
                    Download Certificate
                </button>
            )}
          </div>

        </div>
      </div>
    </div>
  );
};

export default UpliftResults;
