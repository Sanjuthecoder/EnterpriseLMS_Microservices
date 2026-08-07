import toast from 'react-hot-toast';
import React, { useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import TelemetryVisualizer from '../../components/analytics/TelemetryVisualizer';
import { Target, TrendingUp, AlertTriangle, RefreshCw, FileText, BrainCircuit, Lock } from 'lucide-react';
import api from '../../services/api';
import aiApi, { isPremiumGateError, getAiErrorMessage } from '../../services/aiApi';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../components/shared/ToastProvider';
import { useNavigate } from 'react-router-dom';

const AnalyticsOverview = () => {
  const location = useLocation();
  const { user } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const isPremium = user?.subscriptionTier === 'PREMIUM';

  const [employees, setEmployees] = useState([]);
  const [courses, setCourses] = useState([]);
  const [enrollments, setEnrollments] = useState([]);
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);

  // AI Insight State
  const [selectedAiCourse, setSelectedAiCourse] = useState('');
  const [aiInsight, setAiInsight] = useState(null);
  const [aiInsightLoading, setAiInsightLoading] = useState(false);

  // Drilldown State
  const [selectedEmployee, setSelectedEmployee] = useState('');
  const [selectedCourse, setSelectedCourse] = useState('');
  const [drilldownLoading, setDrilldownLoading] = useState(false);
  const [upliftData, setUpliftData] = useState(null);
  const [videoData, setVideoData] = useState([]);
  const [quizTelemetryData, setQuizTelemetryData] = useState(null);
  const [debugError, setDebugError] = useState(null);

  useEffect(() => {
    if (location.state?.selectedEmployee) {
      setSelectedEmployee(String(location.state.selectedEmployee));
      // Clear course if employee changes to avoid mismatch
      setSelectedCourse('');
    }
  }, [location.state]);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [empRes, coursesRes, enrollmentsRes, analyticsRes] = await Promise.all([
        api.get('/company-admin/employees'),
        api.get('/company-admin/courses'),
        api.get('/company-admin/enrollments'),
        api.get('/company-admin/analytics/roi')
      ]);
      setEmployees(empRes.data || []);
      setCourses(coursesRes.data || []);
      setEnrollments(enrollmentsRes.data || []);
      setAnalytics(analyticsRes.data || null);
    } catch (err) {
      console.error('Failed to load company analytics:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  // Fetch employee specific course telemetry when selection changes
  useEffect(() => {
    if (!selectedEmployee || !selectedCourse) {
      setUpliftData(null);
      setVideoData([]);
      return;
    }

    const fetchDrilldownData = async () => {
      setDrilldownLoading(true);
      try {
        const [upliftRes, videoRes, quizRes] = await Promise.all([
          api.get(`/company-admin/employees/${selectedEmployee}/courses/${selectedCourse}/uplift`),
          api.get(`/company-admin/employees/${selectedEmployee}/courses/${selectedCourse}/video-telemetry`),
          api.get(`/company-admin/employees/${selectedEmployee}/courses/${selectedCourse}/quiz-telemetry`)
        ]);
        setUpliftData(upliftRes.data || null);
        setVideoData(videoRes.data || []);
        setQuizTelemetryData(quizRes.data || null);
      } catch (err) {
        console.error('Failed to load employee drilldown metrics:', err);
        const errMsg = "An unexpected error occurred while loading analytics.";
        console.error('Backend error message:', errMsg);
        setDebugError(errMsg);
      } finally {
        setDrilldownLoading(false);
      }
    };

    fetchDrilldownData();
  }, [selectedEmployee, selectedCourse]);

  // Fetch AI Insights when a course is selected for AI analysis
  useEffect(() => {
    if (!selectedAiCourse) {
      setAiInsight(null);
      return;
    }
    
    if (!isPremium) {
      setAiInsight({
        premiumLocked: true
      });
      return;
    }

    const fetchAiInsights = async () => {
      setAiInsightLoading(true);
      try {
        const res = await aiApi.getCompanyInsights(selectedAiCourse, user.companyId || user.orgId);
        setAiInsight(res.data);
      } catch (err) {
        console.error('Failed to load AI insights:', err);
        if (isPremiumGateError(err)) {
          toast.premium('Premium Access Required', { action: { label: 'Upgrade', onClick: () => navigate('/company-admin/billing') }});
          setAiInsight({ premiumLocked: true });
        } else {
          toast.error(getAiErrorMessage(err));
        }
      } finally {
        setAiInsightLoading(false);
      }
    };
    fetchAiInsights();
  }, [selectedAiCourse, isPremium, user, toast, navigate]);

  if (loading) {
    return <div style={{ padding: '2rem', color: 'var(--text-secondary)' }}>Loading analytics portal...</div>;
  }

  // Filter out pending approvals
  const activeEmployees = employees.filter(e => e.status !== 'PENDING');

  // Convert skillGaps map into competency scores: competency = 100 - (gap * 100)
  const competencyData = analytics?.skillGaps 
    ? Object.entries(analytics.skillGaps).map(([skill, gap]) => ({
        label: skill,
        value: Math.max(0, Math.round((1 - gap) * 100))
      }))
    : [];

  // Filter critical gaps (competency below 60% or gap > 0.4)
  const criticalGaps = analytics?.skillGaps
    ? Object.entries(analytics.skillGaps)
        .filter(([_, gap]) => gap > 0.4)
        .map(([skill, gap]) => ({
          skill,
          mastery: Math.max(0, Math.round((1 - gap) * 100))
        }))
    : [];

  // Get courses enrolled for selected employee
  const enrolledCourses = enrollments
    .filter(e => String(e.employeeId) === String(selectedEmployee))
    .map(enrollment => {
      const course = courses.find(c => String(c.courseId) === String(enrollment.courseId));
      return {
        courseId: enrollment.courseId,
        title: course ? course.title : `Course #${enrollment.courseId}`
      };
    });

  // Helper to parse MongoDB raw events into summary metrics
  const analyzeVideoSession = (session) => {
    const events = session.events || [];
    const pauses = events.filter(e => e.type === 'pause').length;
    const rewinds = events.filter(e => e.type === 'rewind' || (e.type === 'seek' && e.fromTime > e.toTime)).length;
    
    let diagnosis = 'Optimal Pacing';
    if (rewinds > 3) {
      diagnosis = 'Confusion Hotspot';
    } else if (pauses > 3) {
      diagnosis = 'High Cognitive Load';
    }
    
    return {
      lessonId: session.lessonId,
      pauses,
      rewinds,
      diagnosis,
      completionPercentage: session.completionPercentage || 0
    };
  };

  // Extract tabular concept rows from JSON uplift report
  const getUpliftRows = () => {
    if (!upliftData || !upliftData.upliftReport) return [];
    const report = upliftData.upliftReport;
    const rows = [];

    if (report.conceptsGained) {
      report.conceptsGained.forEach(concept => {
        rows.push({ concept, status: 'Skill Gained', statusColor: 'var(--success-color)' });
      });
    }
    if (report.noChange) {
      report.noChange.forEach(concept => {
        rows.push({ concept, status: 'No Change', statusColor: 'var(--text-secondary)' });
      });
    }
    if (report.stillStruggling) {
      report.stillStruggling.forEach(concept => {
        rows.push({ concept, status: 'Still Struggling', statusColor: 'var(--danger-color)' });
      });
    }
    if (report.regression) {
      report.regression.forEach(concept => {
        rows.push({ concept, status: 'Regression', statusColor: 'var(--danger-color)' });
      });
    }

    return rows;
  };

  const upliftRows = getUpliftRows();

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
        <h2 style={{ fontSize: '1.75rem', fontWeight: 700, color: 'var(--text-primary)' }}>
          Analytics & ROI
        </h2>
        <button 
          onClick={fetchData}
          className="btn btn-secondary"
          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
        >
          <RefreshCw size={16} /> Refresh
        </button>
      </div>

      <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
        Track Time-to-Competency, department-wide skill gaps, and measure true pre/post quiz uplift to prove the ROI of your training initiatives.
      </p>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '2rem', marginBottom: '3rem' }}>
        <div>
          {competencyData.length > 0 ? (
            <TelemetryVisualizer 
              title="Company-Wide Skill Competency" 
              data={competencyData} 
            />
          ) : (
            <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
              No competency profile aggregated yet. Complete student quizzes to view competency visualizer.
            </div>
          )}
        </div>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <AlertTriangle size={18} color="var(--warning-color)" /> Critical Skill Gaps
          </h3>
          <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', marginBottom: '1.5rem' }}>
            The following concepts show high skill gaps across active company learning campaigns. Consider deploying targeted support.
          </p>
          <ul style={{ listStyle: 'none', padding: 0, display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {criticalGaps.map((item, idx) => (
              <li key={idx} style={{ padding: '0.75rem 1rem', background: 'rgba(245, 158, 11, 0.05)', border: '1px solid rgba(245, 158, 11, 0.2)', borderRadius: 'var(--radius-md)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{item.skill}</span>
                <span style={{ color: 'var(--warning-color)', fontWeight: 700, fontSize: '0.85rem' }}>{item.mastery}% Mastery</span>
              </li>
            ))}
      </ul>
    </div>
  </div>

  {/* AI Course Insights Section */}
  <div className="glass-panel" style={{ padding: '2rem', marginBottom: '3rem', position: 'relative', overflow: 'hidden' }}>
    {/* Premium Gradient Border Top */}
    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: '4px', background: 'var(--premium-gradient, linear-gradient(135deg, #7c3aed, #a855f7))' }} />
    
    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '2rem' }}>
      <div>
        <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)' }}>
          <BrainCircuit size={24} color="#8b5cf6" />
          AI-Powered Instructional Design Insights
        </h3>
        <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)' }}>
          Select a course to generate deep learning behavioral insights powered by Enterprise AI.
        </p>
      </div>
      
      <div style={{ minWidth: '250px' }}>
        <select 
          className="form-input" 
          value={selectedAiCourse} 
          onChange={(e) => setSelectedAiCourse(e.target.value)}
          style={{ borderColor: isPremium ? '#8b5cf6' : 'var(--border-color)', boxShadow: isPremium ? '0 0 0 1px rgba(139, 92, 246, 0.3)' : 'none' }}
        >
          <option value="">-- Analyze Course with AI --</option>
          {courses.map(c => (
            <option key={c.courseId} value={c.courseId}>{c.title}</option>
          ))}
        </select>
      </div>
    </div>

    {selectedAiCourse && (
      <div style={{ minHeight: '150px' }}>
        {aiInsightLoading ? (
          <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100px', color: 'var(--text-secondary)' }}>
            <RefreshCw className="spin" size={20} style={{ marginRight: '10px' }} /> Analyzing course telemetry...
          </div>
        ) : aiInsight?.premiumLocked ? (
          <div style={{ background: 'rgba(139, 92, 246, 0.05)', border: '1px solid rgba(139, 92, 246, 0.2)', borderRadius: 'var(--radius-lg)', padding: '3rem 2rem', textAlign: 'center' }}>
            <div style={{ width: '60px', height: '60px', background: 'linear-gradient(135deg, #7c3aed, #a855f7)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 1.5rem auto', boxShadow: '0 8px 16px rgba(124, 58, 237, 0.3)' }}>
              <Lock size={28} color="white" />
            </div>
            <h4 style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '0.5rem' }}>Premium Feature</h4>
            <p style={{ color: 'var(--text-secondary)', maxWidth: '500px', margin: '0 auto 1.5rem auto' }}>
              Unlock AI-driven instructional design insights to identify exactly where your employees are struggling and how to improve course content.
            </p>
            <button onClick={() => navigate('/company-admin/settings')} style={{ background: 'linear-gradient(135deg, #7c3aed, #a855f7)', color: 'white', border: 'none', padding: '0.75rem 1.5rem', borderRadius: 'var(--radius-md)', fontWeight: 600, cursor: 'pointer', boxShadow: '0 4px 12px rgba(124, 58, 237, 0.2)' }}>
              Upgrade to Premium
            </button>
          </div>
        ) : aiInsight ? (
          <div style={{ background: '#f8fafc', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-lg)', padding: '1.5rem' }}>
            <h4 style={{ fontSize: '1.1rem', fontWeight: 600, color: 'var(--text-primary)', marginBottom: '1.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
              Lesson Insights
            </h4>
            {Array.isArray(aiInsight) && aiInsight.length > 0 ? aiInsight.map((insight, idx) => (
              <div key={idx} style={{ marginBottom: '1.5rem', paddingBottom: '1.5rem', borderBottom: idx < aiInsight.length - 1 ? '1px dashed var(--border-color)' : 'none' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.75rem' }}>
                  <div style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--primary-color)' }}>Lesson {insight.lessonId}</div>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Analyzed: {insight.sessionsAnalyzed} sessions</div>
                </div>
                <div style={{ marginBottom: '0.5rem', fontSize: '0.95rem', color: 'var(--text-primary)' }}>
                  <strong>Summary:</strong> {insight.insightSummary}
                </div>
                {insight.creatorSuggestion && (
                  <div style={{ fontSize: '0.95rem', color: 'var(--text-secondary)' }}>
                    <strong>Action:</strong> {insight.creatorSuggestion}
                  </div>
                )}
              </div>
            )) : (
              <div style={{ textAlign: 'center', color: 'var(--text-secondary)' }}>No AI insights available for this course yet.</div>
            )}
          </div>
        ) : null}
      </div>
    )}
  </div>

  <hr style={{ border: 'none', borderTop: '1px solid var(--border-color)', marginBottom: '3rem' }} />

  <h2 style={{ fontSize: '1.5rem', fontWeight: 700, marginBottom: '1.5rem', color: 'var(--text-primary)' }}>
        Employee Detail Drill-down
      </h2>
      
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '2rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div className="form-group" style={{ marginBottom: '1.25rem' }}>
            <label className="form-label">Select Employee</label>
            <select 
              className="form-input" 
              value={selectedEmployee} 
              onChange={(e) => {
                setSelectedEmployee(e.target.value);
                setSelectedCourse('');
              }}
            >
              <option value="">-- Choose Employee --</option>
              {activeEmployees.map(emp => (
                <option key={emp.userId} value={emp.userId}>{emp.username}</option>
              ))}
            </select>
          </div>

          <div className="form-group" style={{ marginBottom: '1.5rem' }}>
            <label className="form-label">Select Course</label>
            <select 
              className="form-input" 
              value={selectedCourse} 
              disabled={!selectedEmployee}
              onChange={(e) => setSelectedCourse(e.target.value)}
            >
              <option value="">-- Choose Enrolled Course --</option>
              {enrolledCourses.map(c => (
                <option key={c.courseId} value={c.courseId}>{c.title}</option>
              ))}
            </select>
          </div>
          
          {selectedEmployee && selectedCourse && upliftData && (
            <div style={{ marginTop: '2rem' }}>
              <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem' }}>Pre vs Post Summary</h4>
              <div style={{ padding: '1rem', background: '#f8fafc', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-primary)', fontWeight: 600, marginBottom: '0.5rem' }}>
                  <TrendingUp size={16} /> 
                  Uplift Percent: {upliftData.upliftPercent ? `${Math.round(upliftData.upliftPercent)}%` : '0%'}
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Pre-quiz score: {upliftData.preQuizScore ? `${Math.round(upliftData.preQuizScore)}%` : '0%'} • Post-quiz score: {upliftData.postQuizScore ? `${Math.round(upliftData.postQuizScore)}%` : '0%'}
                </p>
              </div>
            </div>
          )}
        </div>

        <div>
          {drilldownLoading ? (
            <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
              Loading employee telemetry data...
            </div>
          ) : debugError ? (
            <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--warning-color)', border: '2px solid var(--warning-color)' }}>
              <h4>Analytics Maintenance</h4>
              <p>We are currently updating our telemetry systems to provide you with better insights. Please check back later.</p>
            </div>
          ) : selectedEmployee && selectedCourse ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
              {/* Uplift Report */}
              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <Target size={18} color="var(--primary-color)" /> Pre vs Post Quiz Uplift Report
                </h3>
                {upliftRows.length === 0 ? (
                  <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No concept metrics generated. Complete pre/post quizzes to populate.
                  </div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        <th style={{ padding: '0.75rem 0' }}>Concept</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'right' }}>Uplift Result</th>
                      </tr>
                    </thead>
                    <tbody>
                      {upliftRows.map((item, idx) => (
                        <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.9rem' }}>
                          <td style={{ padding: '1rem 0', fontWeight: 500 }}>{item.concept}</td>
                          <td style={{ padding: '1rem 0', textAlign: 'right', fontWeight: 600, color: item.statusColor }}>{item.status}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>

              {/* Video Telemetry */}
              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem' }}>Raw Video Telemetry</h3>
                {videoData.length === 0 ? (
                  <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No video interactions logged for this course.
                  </div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        <th style={{ padding: '0.75rem 0' }}>Lesson ID</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'center' }}>Pauses</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'center' }}>Rewinds</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'center' }}>Completion</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'right' }}>Diagnosis</th>
                      </tr>
                    </thead>
                    <tbody>
                      {videoData.map((session, idx) => {
                        const metrics = analyzeVideoSession(session);
                        return (
                          <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.9rem' }}>
                            <td style={{ padding: '1rem 0', fontWeight: 500 }}>Lesson #{metrics.lessonId}</td>
                            <td style={{ padding: '1rem 0', textAlign: 'center' }}>{metrics.pauses}</td>
                            <td style={{ padding: '1rem 0', textAlign: 'center' }}>{metrics.rewinds}</td>
                            <td style={{ padding: '1rem 0', textAlign: 'center' }}>{metrics.completionPercentage}%</td>
                            <td style={{ padding: '1rem 0', textAlign: 'right', color: metrics.diagnosis === 'Optimal Pacing' ? 'var(--success-color)' : 'var(--warning-color)', fontWeight: 600 }}>{metrics.diagnosis}</td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
              </div>

              {/* Pre-Quiz Telemetry Section */}
              <div className="glass-panel" style={{ padding: '1.5rem' }}>
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <FileText size={18} color="var(--primary-color)" /> Pre-Quiz Diagnostics Telemetry
                </h3>
                {(!quizTelemetryData || !quizTelemetryData.xapiStatements || quizTelemetryData.xapiStatements.length === 0) ? (
                  <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
                    No pre-quiz diagnostic telemetry logged for this course.
                  </div>
                ) : (
                  <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--border-color)', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                        <th style={{ padding: '0.75rem 0' }}>Concept</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'center' }}>Time Spent</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'center' }}>Changes</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'center' }}>Result</th>
                        <th style={{ padding: '0.75rem 0', textAlign: 'right' }}>Diagnosis Trigger</th>
                      </tr>
                    </thead>
                    <tbody>
                      {quizTelemetryData.xapiStatements.map((stmt, idx) => {
                        const isCorrect = stmt.result?.success;
                        const timeSpentMs = stmt.context?.extensions?.time_spent_ms || 0;
                        const answerChanges = stmt.context?.extensions?.answer_changes || 0;
                        const concept = stmt.context?.extensions?.concept || 'General';
                        const triggerReason = stmt.context?.extensions?.trigger_reason;

                        let resultColor = 'var(--text-secondary)';
                        let resultText = 'Unknown';
                        if (isCorrect === true) { resultColor = 'var(--success-color)'; resultText = 'Correct'; }
                        if (isCorrect === false) { resultColor = 'var(--danger-color)'; resultText = 'Incorrect'; }

                        return (
                          <tr key={idx} style={{ borderBottom: '1px solid var(--border-color)', fontSize: '0.9rem' }}>
                            <td style={{ padding: '1rem 0', fontWeight: 500 }}>{concept}</td>
                            <td style={{ padding: '1rem 0', textAlign: 'center' }}>{Math.round(timeSpentMs / 1000)}s</td>
                            <td style={{ padding: '1rem 0', textAlign: 'center' }}>{answerChanges}</td>
                            <td style={{ padding: '1rem 0', textAlign: 'center', color: resultColor, fontWeight: 600 }}>{resultText}</td>
                            <td style={{ padding: '1rem 0', textAlign: 'right' }}>
                              {triggerReason ? (
                                <span style={{ background: 'var(--warning-color)', color: 'white', padding: '2px 8px', borderRadius: '12px', fontSize: '0.75rem', fontWeight: 600 }}>
                                  {triggerReason}
                                </span>
                              ) : (
                                <span style={{ color: 'var(--text-secondary)', fontSize: '0.85rem' }}>None</span>
                              )}
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                )}
              </div>
            </div>
          ) : (
            <div className="glass-panel" style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>
              Select an employee and their enrolled course to view detailed pre/post quiz uplift and raw video telemetry.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AnalyticsOverview;
