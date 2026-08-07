import React, { useState, useEffect } from 'react';
import { Target, ShieldAlert, FileQuestion, Trash2, Plus, BookOpen } from 'lucide-react';
import { useAuth } from '../../contexts/AuthContext';
import api from '../../services/api';

const QuizBuilder = () => {
  const { user } = useAuth();
  const [courses, setCourses] = useState([]);
  const [selectedCourseId, setSelectedCourseId] = useState('');
  const [quizType, setQuizType] = useState('PRE_QUIZ');
  
  const [questions, setQuestions] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [newQuestion, setNewQuestion] = useState({
    questionText: '',
    options: ['', '', '', ''],
    correctOptionIndex: 0,
    testedConcept: '',
    linkedLessonId: ''
  });

  useEffect(() => {
    const fetchCourses = async () => {
      try {
        const response = await api.get(`/creator/courses?creatorId=${user.userId}`);
        setCourses(response.data || []);
        if (response.data && response.data.length > 0) {
          setSelectedCourseId(response.data[0].courseId);
        }
      } catch (err) {
        console.error("Failed to fetch courses", err);
      }
    };
    fetchCourses();
  }, [user]);

  useEffect(() => {
    if (!selectedCourseId) return;
    const fetchData = async () => {
      try {
        const qRes = await api.get(`/creator/courses/${selectedCourseId}/questions`);
        setQuestions(qRes.data || []);
        
        const lRes = await api.get(`/creator/courses/${selectedCourseId}/lessons`);
        setLessons(lRes.data || []);
        if (lRes.data && lRes.data.length > 0) {
          setNewQuestion(prev => ({ ...prev, linkedLessonId: lRes.data[0].lessonId }));
        }
      } catch (err) {
        console.error("Failed to fetch course data", err);
      }
    };
    fetchData();
  }, [selectedCourseId]);

  const handleAddQuestion = async () => {
    if (!newQuestion.questionText) {
      alert("Question text is required.");
      return;
    }

    try {
      const payload = {
        quizType: quizType,
        questionText: newQuestion.questionText,
        options: newQuestion.options,
        correctAnswer: newQuestion.correctOptionIndex.toString(),
        concept: newQuestion.testedConcept,
        lessonId: newQuestion.linkedLessonId || null
      };
      
      const response = await api.post(`/creator/courses/${selectedCourseId}/questions`, payload);
      setQuestions([...questions, response.data]);
      
      // Reset form
      setNewQuestion({
        ...newQuestion,
        questionText: '',
        options: ['', '', '', ''],
        correctOptionIndex: 0,
        testedConcept: ''
      });
    } catch (err) {
      console.error("Failed to add question", err);
      alert("Failed to add question.");
    }
  };

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
        <div>
          <h2 style={{ fontSize: '1.75rem', fontWeight: 700, marginBottom: '0.5rem', color: 'var(--text-primary)' }}>
            Assessment Architect
          </h2>
          <p style={{ color: 'var(--text-secondary)' }}>
            Construct the diagnostic Pre-Quiz or the measurement Post-Quiz to drive the platform's adaptive telemetry engine.
          </p>
        </div>
        
        {courses.length > 0 && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', background: 'var(--bg-secondary)', padding: '0.5rem 1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
            <BookOpen size={18} color="var(--text-secondary)" />
            <select 
              value={selectedCourseId} 
              onChange={(e) => setSelectedCourseId(e.target.value)}
              style={{ background: 'transparent', border: 'none', color: 'var(--text-primary)', fontWeight: 600, outline: 'none', cursor: 'pointer' }}
            >
              {courses.map(c => (
                <option key={c.courseId} value={c.courseId}>{c.title} (v{c.version})</option>
              ))}
            </select>
          </div>
        )}
      </div>

      {!selectedCourseId ? (
        <div style={{ padding: '3rem', textAlign: 'center', background: 'var(--bg-secondary)', borderRadius: 'var(--radius-lg)' }}>
          <p style={{ color: 'var(--text-secondary)' }}>You don't have any courses yet. Create one from the Overview tab.</p>
        </div>
      ) : (
        <>
          {/* Quiz Type Selector */}
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1.5rem', marginBottom: '3rem' }}>
            <div onClick={() => setQuizType('PRE_QUIZ')} style={{ padding: '1.5rem', borderRadius: 'var(--radius-lg)', border: quizType === 'PRE_QUIZ' ? '2px solid var(--primary-color)' : '1px solid var(--border-color)', background: quizType === 'PRE_QUIZ' ? 'rgba(37, 99, 235, 0.05)' : 'white', cursor: 'pointer', transition: 'all 0.2s' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                <ShieldAlert size={24} color={quizType === 'PRE_QUIZ' ? 'var(--primary-color)' : 'var(--text-secondary)'} />
                <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Pre-Quiz (Diagnostic)</h3>
              </div>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Fires before course content. Drives the Gating Engine to determine which lessons are marked <strong>RECOMMENDED</strong> vs <strong>OPTIONAL</strong>. Fails are expected.
              </p>
            </div>
            <div onClick={() => setQuizType('POST_QUIZ')} style={{ padding: '1.5rem', borderRadius: 'var(--radius-lg)', border: quizType === 'POST_QUIZ' ? '2px solid var(--success-color)' : '1px solid var(--border-color)', background: quizType === 'POST_QUIZ' ? 'rgba(16, 185, 129, 0.05)' : 'white', cursor: 'pointer', transition: 'all 0.2s' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.75rem' }}>
                <Target size={24} color={quizType === 'POST_QUIZ' ? 'var(--success-color)' : 'var(--text-secondary)'} />
                <h3 style={{ fontSize: '1.25rem', fontWeight: 600 }}>Post-Quiz (Measurement)</h3>
              </div>
              <p style={{ fontSize: '0.9rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                Fires after course content. Drives the Uplift ROI Report to prove training effectiveness. Scores are reported to Company Admins.
              </p>
            </div>
          </div>

          <div className="glass-panel" style={{ padding: '2rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <FileQuestion size={20} /> Question Bank ({quizType})
              </h3>
            </div>

            <div style={{ marginBottom: '2rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {questions.filter(q => q.quizType === quizType).map(q => {
                let parsedOptions = [];
                if (Array.isArray(q.options)) {
                  parsedOptions = q.options;
                } else if (typeof q.options === 'string') {
                  try {
                    parsedOptions = JSON.parse(q.options);
                  } catch (e) {
                    parsedOptions = q.options.split(',');
                  }
                }
                const correctIdx = parseInt(q.correctAnswer);

                return (
                  <div key={q.questionId} style={{ padding: '1.5rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', background: 'var(--bg-secondary)' }}>
                    <div style={{ fontWeight: 600, marginBottom: '0.5rem' }}>{q.questionText}</div>
                    <div style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1rem' }}>Concept: {q.concept || q.testedConcept || 'N/A'}</div>
                    <ul style={{ listStyleType: 'disc', paddingLeft: '1.5rem' }}>
                      {parsedOptions.map((opt, i) => (
                        <li key={i} style={{ color: i === correctIdx ? 'var(--success-color)' : 'inherit', fontWeight: i === correctIdx ? 'bold' : 'normal' }}>
                          {opt} {i === correctIdx && '(Correct)'}
                        </li>
                      ))}
                    </ul>
                  </div>
                );
              })}
              {questions.filter(q => q.quizType === quizType).length === 0 && (
                <p style={{ color: 'var(--text-secondary)', fontStyle: 'italic' }}>No questions added for {quizType} yet.</p>
              )}
            </div>

            <h4 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border-color)' }}>Add New Question</h4>
            <div style={{ padding: '1.5rem', border: '1px solid var(--border-color)', borderRadius: 'var(--radius-md)', background: 'white' }}>
              <div style={{ marginBottom: '1.5rem' }}>
                <label className="form-label">Question Text <span style={{ color: 'red' }}>*</span></label>
                <textarea className="form-input" required rows="2" value={newQuestion.questionText} onChange={(e) => setNewQuestion({...newQuestion, questionText: e.target.value})} placeholder="Enter the question..." />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '2rem' }}>
                <div>
                  <label className="form-label">Options (Check the correct answer) <span style={{ color: 'red' }}>*</span></label>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {newQuestion.options.map((opt, i) => (
                      <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <input type="radio" name="correctOption" checked={newQuestion.correctOptionIndex === i} onChange={() => setNewQuestion({...newQuestion, correctOptionIndex: i})} />
                        <input type="text" className="form-input" required value={opt} onChange={(e) => { const updated = [...newQuestion.options]; updated[i] = e.target.value; setNewQuestion({...newQuestion, options: updated}); }} placeholder={`Option ${i+1}`} style={{ marginBottom: 0 }} />
                      </div>
                    ))}
                  </div>
                </div>
                <div>
                  <label className="form-label">Telemetry Mapping</label>
                  <div style={{ marginBottom: '1rem' }}>
                    <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Tested Concept <span style={{ color: 'red' }}>*</span></label>
                    <input type="text" className="form-input" required value={newQuestion.testedConcept} onChange={(e) => setNewQuestion({...newQuestion, testedConcept: e.target.value})} placeholder="e.g. SDLC Phases" />
                  </div>
                  <div>
                    <label style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', display: 'block', marginBottom: '0.25rem' }}>Linked Lesson Module</label>
                    <select className="form-input" value={newQuestion.linkedLessonId} onChange={(e) => setNewQuestion({...newQuestion, linkedLessonId: e.target.value})}>
                      <option value="">No linked lesson</option>
                      {lessons.map(l => (
                        <option key={l.lessonId} value={l.lessonId}>Module {l.seqOrder}: {l.title}</option>
                      ))}
                    </select>
                    <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', fontStyle: 'italic', marginTop: '0.25rem' }}>
                      If the learner fails or hesitates on this question during the Pre-Quiz, the selected module will be marked RECOMMENDED.
                    </p>
                  </div>
                </div>
              </div>
              <button onClick={handleAddQuestion} className="btn btn-primary" style={{ marginTop: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem', width: '100%', justifyContent: 'center' }}>
                <Plus size={16} /> Save Question to Bank
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default QuizBuilder;
