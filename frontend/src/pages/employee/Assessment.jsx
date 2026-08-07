import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { useToast } from '../../components/shared/ToastProvider';
import Modal from '../../components/shared/Modal';
import QuizInstructions from '../../components/shared/QuizInstructions';
import ExitWarningOverlay from '../../components/shared/ExitWarningOverlay';
import { PartyPopper, Timer, CheckCircle2, XCircle, BrainCircuit } from 'lucide-react';
import api from '../../services/api';

const Assessment = () => {
  const { courseId } = useParams();
  const location = useLocation();
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const toast = useToast();

  const isPostQuiz = location.pathname.includes('/post-quiz');
  const quizType = isPostQuiz ? 'POST_QUIZ' : 'PRE_QUIZ';
  const isPremium = user?.subscriptionTier === 'PREMIUM';

  // ── State ──
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [phase, setPhase] = useState('instructions'); // 'instructions' | 'quiz' | 'results'
  const [currentIndex, setCurrentIndex] = useState(0);
  const [secondsElapsed, setSecondsElapsed] = useState(0);
  const [telemetry, setTelemetry] = useState({});
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [isReviewingQuiz, setIsReviewingQuiz] = useState(false);
  const [showExitWarning, setShowExitWarning] = useState(false);
  const [pendingNavPath, setPendingNavPath] = useState(null);

  // ── Refs ──
  const slideEntryTimeRef = useRef(null);
  const timerIntervalRef = useRef(null);
  const quizActiveRef = useRef(false);

  // ── Fetch Questions ──
  useEffect(() => {
    const fetchQuestions = async () => {
      try {
        // Token staleness check: If company is PREMIUM in DB but JWT says FREE, force re-login
        const statusRes = await api.get(`/v1/payments/${user.companyId || user.orgId}/status`).catch(() => ({ data: { isPremium: false } }));
        if (statusRes.data.isPremium && !isPremium) {
          toast.premium('Your company has upgraded to Premium! Please log in again to activate your AI features.');
          logout();
          return;
        }

        const res = await api.get(`/employees/courses/${courseId}/${quizType.toLowerCase().replace('_', '-')}`);
        const rawQuestions = res.data;

        const apiQuestions = rawQuestions.map(q => {
          let parsedOptions = [];
          if (Array.isArray(q.options)) {
            parsedOptions = q.options;
          } else if (typeof q.options === 'string') {
            try { parsedOptions = JSON.parse(q.options); } catch { parsedOptions = q.options.split(','); }
          }
          return {
            id: q.questionId,
            text: q.questionText,
            options: parsedOptions,
            correct: null,
            linkedLessonId: q.linkedLessonId,
            concept: q.concept || q.conceptTag
          };
        });

        if (apiQuestions.length === 0) {
          setError(`No questions available for this ${quizType.replace('_', ' ')}.`);
          setLoading(false);
          return;
        }

        setQuestions(apiQuestions);

        // Initialize telemetry for every question
        const initialTelemetry = {};
        apiQuestions.forEach(q => {
          initialTelemetry[q.id] = {
            timeMs: 0,
            changes: 0,       // counts every answer flip (selection change)
            finalAnswer: null,
            isVisited: false,
            isReview: false
          };
        });
        setTelemetry(initialTelemetry);
        setLoading(false);
      } catch (err) {
        console.error('Failed to load quiz', err);
        // The Gateway or CourseService might return 403 if AI features are locked
        if (err?.response?.status === 403) {
           toast.premium('Premium Access Required', { action: { label: 'Upgrade', onClick: () => navigate('/employee/upgrade') }});
           setError('Premium AI features are currently locked. Please upgrade to access this assessment.');
        } else {
           toast.error('Unable to load assessment. Please try again later.');
           setError('This assessment is temporarily unavailable while our system prepares the personalized content.');
        }
        setLoading(false);
      }
    };
    fetchQuestions();

    return () => clearInterval(timerIntervalRef.current);
  }, [courseId, quizType, isPremium, user, isPostQuiz, navigate]);

  // ── Start Quiz (called from Instructions screen) ──
  const handleStartQuiz = useCallback(() => {
    setPhase('quiz');
    quizActiveRef.current = true;
    slideEntryTimeRef.current = Date.now();

    // Mark first question visited
    if (questions.length > 0) {
      setTelemetry(prev => ({
        ...prev,
        [questions[0].id]: { ...prev[questions[0].id], isVisited: true }
      }));
    }

    // Start global timer
    timerIntervalRef.current = setInterval(() => {
      setSecondsElapsed(prev => prev + 1);
    }, 1000);
  }, [questions]);

  // ── Exit Prevention: beforeunload ──
  useEffect(() => {
    const handleBeforeUnload = (e) => {
      if (quizActiveRef.current) {
        e.preventDefault();
        e.returnValue = '';
      }
    };
    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => window.removeEventListener('beforeunload', handleBeforeUnload);
  }, []);

  // ── Exit Prevention: back/forward navigation via popstate ──
  useEffect(() => {
    if (phase !== 'quiz') return;

    // Push an extra history entry so pressing Back triggers popstate
    window.history.pushState({ quizGuard: true }, '');

    const handlePopState = (e) => {
      if (quizActiveRef.current) {
        // Re-push to keep them on the page
        window.history.pushState({ quizGuard: true }, '');
        setShowExitWarning(true);
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [phase]);

  // ── Exit Prevention: intercept react-router navigation ──
  // We override navigate so clicking sidebar/nav links triggers the warning
  useEffect(() => {
    if (phase !== 'quiz') return;

    const handleClick = (e) => {
      const link = e.target.closest('a[href]');
      if (link && quizActiveRef.current) {
        const href = link.getAttribute('href');
        if (href && !href.includes(courseId)) {
          e.preventDefault();
          e.stopPropagation();
          setPendingNavPath(href);
          setShowExitWarning(true);
        }
      }
    };
    document.addEventListener('click', handleClick, true);
    return () => document.removeEventListener('click', handleClick, true);
  }, [phase, courseId]);

  // ── Telemetry: Save time for current question ──
  const saveCurrentTime = useCallback(() => {
    if (!slideEntryTimeRef.current) return;
    const now = Date.now();
    const timeSpent = now - slideEntryTimeRef.current;
    const qId = questions[currentIndex]?.id;
    if (!qId) return;

    setTelemetry(prev => ({
      ...prev,
      [qId]: {
        ...prev[qId],
        timeMs: (prev[qId]?.timeMs || 0) + timeSpent
      }
    }));
    slideEntryTimeRef.current = now;
  }, [questions, currentIndex]);

  // ── Navigation between questions ──
  const navigateTo = useCallback((index) => {
    if (index < 0 || index >= questions.length) return;
    saveCurrentTime();
    setTelemetry(prev => ({
      ...prev,
      [questions[index].id]: {
        ...prev[questions[index].id],
        isVisited: true
      }
    }));
    setCurrentIndex(index);
    slideEntryTimeRef.current = Date.now();
  }, [questions, saveCurrentTime]);

  // ── Answer Selection (with correct flip counting) ──
  const handleOptionChange = useCallback((val) => {
    if (isReviewingQuiz) return;
    const newVal = parseInt(val);
    const qId = questions[currentIndex]?.id;
    if (!qId) return;

    setTelemetry(prev => {
      const current = prev[qId];
      // A flip = any time the user changes their selected answer
      // First selection (null -> value) is NOT a flip
      // Changing from one value to another IS a flip
      const isFlip = current.finalAnswer !== null && current.finalAnswer !== newVal;
      return {
        ...prev,
        [qId]: {
          ...current,
          finalAnswer: newVal,
          changes: isFlip ? current.changes + 1 : current.changes
        }
      };
    });
  }, [questions, currentIndex, isReviewingQuiz]);

  const toggleReview = useCallback(() => {
    const qId = questions[currentIndex]?.id;
    if (!qId) return;
    setTelemetry(prev => ({
      ...prev,
      [qId]: { ...prev[qId], isReview: !prev[qId].isReview }
    }));
  }, [questions, currentIndex]);

  const submitAssessment = async () => {
    setShowConfirmModal(false);
    
    // Manually calculate the time for the last question because setTelemetry is async
    const now = Date.now();
    const timeSpentOnLast = slideEntryTimeRef.current ? now - slideEntryTimeRef.current : 0;
    const lastQId = questions[currentIndex]?.id;

    saveCurrentTime();
    clearInterval(timerIntervalRef.current);
    quizActiveRef.current = false;

    const attempts = questions.map(q => {
      const td = telemetry[q.id];
      const additionalTime = (q.id === lastQId) ? timeSpentOnLast : 0;
      
      return {
        questionId: q.id,
        answer: td.finalAnswer !== null ? String(td.finalAnswer) : null,
        timeSpentMs: (td.timeMs || 0) + additionalTime,
        answerChanges: td.changes || 0,
        linkedLessonId: q.linkedLessonId,
        concept: q.concept
      };
    });

    try {
      const res = await api.post(`/employees/courses/${courseId}/${quizType.toLowerCase().replace('_', '-')}/submit`, attempts);
      
      if (res.data && res.data.correctAnswers) {
          setQuestions(prev => prev.map(q => ({
             ...q,
             correct: parseInt(res.data.correctAnswers[q.id], 10)
          })));
      }
      
      setPhase('results');
    } catch (err) {
      console.error('Failed to submit assessment', err);
      if (err?.response?.status === 403) {
        toast.premium('Upgrade to submit AI quizzes');
      } else {
        toast.error('Submission failed. Please try again.');
      }
      setError('Failed to submit assessment. Please try again.');
      setPhase('error'); // Show error state
    }
  };

  // ── Exit Warning Handlers ──
  const handleStay = () => {
    setShowExitWarning(false);
    setPendingNavPath(null);
  };

  const handleLeave = () => {
    quizActiveRef.current = false;
    clearInterval(timerIntervalRef.current);
    setShowExitWarning(false);
    if (pendingNavPath) {
      navigate(pendingNavPath);
    } else {
      navigate('/employee');
    }
  };

  // ── Render: Loading / Error ──
  if (loading) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-secondary)' }}>Loading Assessment...</div>;
  if (error) return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--danger-color)' }}>{error}</div>;

  // ── Render: Instructions Phase ──
  if (phase === 'instructions') {
    return <QuizInstructions quizType={quizType} questionCount={questions.length} onStart={handleStartQuiz} />;
  }

  // ── Render: Results Phase ──
  if (phase === 'results') {
    return (
      <div className="container mt-4 glass-panel" style={{ maxWidth: '700px', margin: '3rem auto', textAlign: 'center' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}><PartyPopper size={52} color="var(--success-color)" /></div>
        <h3 style={{ color: 'var(--primary-color)', marginBottom: '1rem', fontSize: '1.5rem', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px' }}>
          {quizType === 'PRE_QUIZ' ? 'Diagnostic Complete!' : 'Assessment Complete!'}
          {isPremium && <BrainCircuit size={28} color="var(--primary-color)" style={{ filter: 'drop-shadow(0 0 8px rgba(37,99,235,0.5))' }} />}
        </h3>
        <p style={{ marginBottom: '2rem', color: 'var(--text-secondary)' }}>
          {quizType === 'PRE_QUIZ'
            ? 'We have analyzed your cognitive load and curated a highly personalized learning path for you.'
            : 'Your post-quiz results have been recorded and your skill uplift has been calculated.'}
        </p>
        <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center' }}>
          <button onClick={() => { setPhase('quiz'); setIsReviewingQuiz(true); setCurrentIndex(0); }} className="btn btn-secondary">Review Quiz</button>
          {quizType === 'PRE_QUIZ' ? (
            <button onClick={() => navigate(`/employee/courses/${courseId}/player`)} className="btn btn-primary">Enter Course Player</button>
          ) : (
            <button onClick={() => navigate(`/employee/courses/${courseId}/uplift`)} className="btn btn-primary">View Uplift Results</button>
          )}
        </div>
      </div>
    );
  }

  // ── Render: Quiz Phase ──
  const currentQ = questions[currentIndex];
  const tData = telemetry[currentQ?.id] || {};
  const mins = String(Math.floor(secondsElapsed / 60)).padStart(2, '0');
  const secs = String(secondsElapsed % 60).padStart(2, '0');

  return (
    <section className="container mt-4">
      {showExitWarning && <ExitWarningOverlay onStay={handleStay} onLeave={handleLeave} />}

      <div className="glass-panel" style={{ maxWidth: '900px', margin: '3rem auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', paddingBottom: '1rem', borderBottom: '1px solid var(--border-color)' }}>
          <div style={{ fontWeight: 'bold', fontSize: '1.1rem' }}>
            {quizType === 'PRE_QUIZ' ? 'Adaptive Diagnostic' : 'Final Assessment'} {isReviewingQuiz ? '— Review Mode' : 'in Progress...'}
          </div>
          <div style={{ fontWeight: 'bold', color: 'var(--primary-color)', background: 'rgba(37,99,235,0.1)', padding: '4px 12px', borderRadius: '20px', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <Timer size={16} /> {mins}:{secs}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '3fr 1fr', gap: '2rem' }}>
          {/* Question Area */}
          <div>
            <div style={{ padding: '1.5rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-lg)', background: 'var(--bg-secondary)', position: 'relative', minHeight: '250px' }}>
              <span style={{ position: 'absolute', top: '1rem', right: '1rem', fontSize: '0.75rem', background: 'var(--bg-primary)', padding: '4px 8px', borderRadius: '12px', fontWeight: '600', color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                {currentQ?.concept}
              </span>
              <p style={{ fontWeight: '600', marginBottom: '1.5rem', fontSize: '1.1rem', marginTop: '0.5rem' }}>
                Q{currentIndex + 1}. {currentQ?.text}
              </p>
              <div style={{ display: 'grid', gap: '10px' }}>
                {currentQ?.options.map((opt, i) => {
                  const isChecked = tData.finalAnswer === i;
                  const isCorrectAns = isReviewingQuiz && currentQ.correct === i;
                  const isWrongAns = isReviewingQuiz && isChecked && currentQ.correct !== i;

                  let bg = isChecked ? 'rgba(37,99,235,0.05)' : 'transparent';
                  let border = isChecked ? 'var(--primary-color)' : 'var(--border-color)';

                  if (isReviewingQuiz) {
                    if (isCorrectAns) { bg = 'rgba(16,185,129,0.1)'; border = '#10b981'; }
                    else if (isWrongAns) { bg = 'rgba(239,68,68,0.1)'; border = '#ef4444'; }
                  }

                  return (
                    <label key={i} style={{ display: 'flex', alignItems: 'center', cursor: isReviewingQuiz ? 'default' : 'pointer', padding: '10px 14px', border: `2px solid ${border}`, borderRadius: 'var(--radius-md)', background: bg, transition: 'all 0.2s' }}>
                      <input type="radio" name={`q_${currentQ.id}`} value={i} checked={isChecked} onChange={() => handleOptionChange(i)} disabled={isReviewingQuiz} style={{ marginRight: '12px', transform: 'scale(1.2)' }} />
                      <span style={{ fontSize: '1rem', display: 'flex', alignItems: 'center', gap: '8px' }}>
                        {opt}
                        {isCorrectAns && <CheckCircle2 size={16} color="#10b981" />}
                        {isWrongAns && <XCircle size={16} color="#ef4444" />}
                      </span>
                    </label>
                  );
                })}
              </div>
            </div>

            {/* Navigation Buttons */}
            <div style={{ display: 'flex', gap: '10px', marginTop: '1.5rem' }}>
              <button className="btn btn-secondary" onClick={() => navigateTo(currentIndex - 1)} disabled={currentIndex === 0}>← Prev</button>
              {!isReviewingQuiz && (
                <button className="btn" style={{ background: tData.isReview ? 'var(--bg-primary)' : 'var(--warning-color)', color: tData.isReview ? 'var(--text-primary)' : 'white' }} onClick={toggleReview}>
                  {tData.isReview ? 'Unmark Review' : 'Mark for Review'}
                </button>
              )}
              {currentIndex < questions.length - 1 ? (
                <button className="btn btn-primary" style={{ flexGrow: 1 }} onClick={() => navigateTo(currentIndex + 1)}>Next →</button>
              ) : (
                isReviewingQuiz ? (
                  <button className="btn btn-secondary" style={{ flexGrow: 1 }} onClick={() => { setIsReviewingQuiz(false); setPhase('results'); }}>Back to Results</button>
                ) : (
                  <button className="btn btn-primary" style={{ flexGrow: 1, backgroundColor: 'var(--success-color)' }} onClick={() => setShowConfirmModal(true)}>Final Submit</button>
                )
              )}
            </div>
          </div>

          {/* Navigator Panel */}
          <div style={{ background: 'var(--bg-secondary)', padding: '1.5rem', borderRadius: 'var(--radius-lg)', border: '1px solid var(--border-color)', alignSelf: 'start' }}>
            <h4 style={{ marginTop: 0, marginBottom: '1rem', textAlign: 'center', fontSize: '1rem' }}>Navigator</h4>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
              {questions.map((q, i) => {
                const td = telemetry[q.id];
                let bgColor = 'var(--bg-primary)';
                let textColor = 'var(--text-primary)';
                let borderColor = 'var(--border-color)';

                if (td?.isVisited) {
                  if (td.isReview && td.finalAnswer !== null) { bgColor = '#8b5cf6'; textColor = 'white'; borderColor = '#8b5cf6'; }
                  else if (td.isReview && td.finalAnswer === null) { bgColor = 'var(--warning-color)'; textColor = 'white'; borderColor = 'var(--warning-color)'; }
                  else if (!td.isReview && td.finalAnswer !== null) { bgColor = 'var(--success-color)'; textColor = 'white'; borderColor = 'var(--success-color)'; }
                  else if (!td.isReview && td.finalAnswer === null) { bgColor = 'var(--danger-color)'; textColor = 'white'; borderColor = 'var(--danger-color)'; }
                }
                const isActive = i === currentIndex;
                if (isActive) borderColor = 'var(--primary-color)';

                return (
                  <div key={i} onClick={() => navigateTo(i)} style={{ height: '38px', width: '38px', backgroundColor: bgColor, color: textColor, border: `${isActive ? '3px' : '1px'} solid ${borderColor}`, borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontWeight: '600', cursor: 'pointer', fontSize: '0.9rem', margin: '0 auto' }}>
                    {i + 1}
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>

      {/* Submit Confirmation Modal */}
      <Modal
        isOpen={showConfirmModal}
        onClose={() => setShowConfirmModal(false)}
        title="Submit Assessment?"
        footer={<>
          <button className="btn btn-secondary" style={{ flex: 1 }} onClick={() => setShowConfirmModal(false)}>Back to Quiz</button>
          <button className="btn btn-primary" style={{ flex: 2, backgroundColor: 'var(--success-color)' }} onClick={submitAssessment}>Confirm Submit</button>
        </>}
      >
        <p>Are you sure you want to submit your assessment? You will not be able to change your answers after submission.</p>
      </Modal>
    </section>
  );
};

export default Assessment;
