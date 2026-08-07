import React from 'react';
import { ShieldCheck, Clock, AlertTriangle, CheckCircle, BookOpen, Ban, ArrowRight } from 'lucide-react';

const QuizInstructions = ({ quizType, questionCount, onStart }) => {
  const isPreQuiz = quizType === 'PRE_QUIZ';
  const title = isPreQuiz ? 'Adaptive Diagnostic Assessment' : 'Final Competency Assessment';
  const subtitle = isPreQuiz
    ? 'This pre-quiz will evaluate your existing knowledge and create a personalized learning path tailored to your skill gaps.'
    : 'This post-quiz measures your knowledge growth after completing the course. Your results will determine your official skill uplift score.';

  return (
    <div style={{
      minHeight: '100vh',
      background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #0f172a 100%)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '2rem'
    }}>
      <div style={{
        width: '100%',
        maxWidth: '720px',
        background: 'rgba(30, 41, 59, 0.85)',
        backdropFilter: 'blur(20px)',
        border: '1px solid rgba(255,255,255,0.08)',
        borderRadius: '20px',
        padding: '3rem 2.5rem',
        boxShadow: '0 25px 60px rgba(0,0,0,0.5)',
        color: '#f1f5f9'
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: '2rem' }}>
          <div style={{
            width: '72px', height: '72px', borderRadius: '50%',
            background: isPreQuiz ? 'rgba(37,99,235,0.15)' : 'rgba(16,185,129,0.15)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            margin: '0 auto 1.25rem'
          }}>
            <BookOpen size={36} color={isPreQuiz ? '#3b82f6' : '#10b981'} />
          </div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 800, marginBottom: '0.5rem', letterSpacing: '-0.5px' }}>{title}</h1>
          <p style={{ color: '#94a3b8', fontSize: '0.95rem', lineHeight: 1.6, maxWidth: '560px', margin: '0 auto' }}>{subtitle}</p>
        </div>

        {/* Stats Bar */}
        <div style={{
          display: 'flex', gap: '1rem', marginBottom: '2rem',
          background: 'rgba(15,23,42,0.5)', borderRadius: '12px', padding: '1rem'
        }}>
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#3b82f6' }}>{questionCount}</div>
            <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Questions</div>
          </div>
          <div style={{ width: '1px', background: 'rgba(255,255,255,0.08)' }} />
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#f59e0b' }}>MCQ</div>
            <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Single Correct</div>
          </div>
          <div style={{ width: '1px', background: 'rgba(255,255,255,0.08)' }} />
          <div style={{ flex: 1, textAlign: 'center' }}>
            <div style={{ fontSize: '1.5rem', fontWeight: 700, color: '#10b981' }}>Timed</div>
            <div style={{ fontSize: '0.8rem', color: '#94a3b8' }}>Tracked Live</div>
          </div>
        </div>

        {/* Instructions */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginBottom: '2rem' }}>
          <h3 style={{ fontSize: '0.85rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '1px', color: '#94a3b8', marginBottom: '0.25rem' }}>
            Important Instructions
          </h3>
          {[
            { icon: <Clock size={18} />, color: '#3b82f6', text: 'This assessment is timed. Your time spent on each question is tracked and analyzed.' },
            { icon: <CheckCircle size={18} />, color: '#10b981', text: 'Each question has only one correct answer. Select the best option carefully.' },
            { icon: <ShieldCheck size={18} />, color: '#8b5cf6', text: 'Answer changes (flips) are monitored. Excessive changes may indicate uncertainty.' },
            { icon: <AlertTriangle size={18} />, color: '#f59e0b', text: 'Once you start, you must complete the quiz. Leaving will trigger a warning.' },
            { icon: <Ban size={18} />, color: '#ef4444', text: 'Do not use external resources, switch tabs, or engage in any form of malpractice. All activity is monitored.' },
          ].map((item, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'flex-start', gap: '0.75rem',
              padding: '0.85rem 1rem',
              background: 'rgba(15,23,42,0.4)',
              borderRadius: '10px',
              borderLeft: `3px solid ${item.color}`
            }}>
              <div style={{ color: item.color, flexShrink: 0, marginTop: '1px' }}>{item.icon}</div>
              <span style={{ fontSize: '0.9rem', color: '#cbd5e1', lineHeight: 1.5 }}>{item.text}</span>
            </div>
          ))}
        </div>

        {/* Acknowledgement & Start */}
        <div style={{
          background: 'rgba(239,68,68,0.08)',
          border: '1px solid rgba(239,68,68,0.2)',
          borderRadius: '12px',
          padding: '1rem 1.25rem',
          marginBottom: '1.5rem'
        }}>
          <p style={{ fontSize: '0.85rem', color: '#fca5a5', lineHeight: 1.6 }}>
            <strong>⚠️ Academic Integrity:</strong> By clicking the button below, you acknowledge that you have read and understood all instructions. Any form of dishonesty may result in score invalidation.
          </p>
        </div>

        <button
          onClick={onStart}
          style={{
            width: '100%',
            padding: '1rem',
            borderRadius: '12px',
            border: 'none',
            background: isPreQuiz
              ? 'linear-gradient(135deg, #2563eb, #3b82f6)'
              : 'linear-gradient(135deg, #059669, #10b981)',
            color: '#ffffff',
            fontSize: '1.05rem',
            fontWeight: 700,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '0.75rem',
            boxShadow: isPreQuiz
              ? '0 8px 25px rgba(37,99,235,0.35)'
              : '0 8px 25px rgba(16,185,129,0.35)',
            transition: 'transform 0.2s, box-shadow 0.2s',
            fontFamily: 'var(--font-family)'
          }}
          onMouseOver={e => { e.currentTarget.style.transform = 'translateY(-2px)'; }}
          onMouseOut={e => { e.currentTarget.style.transform = 'translateY(0)'; }}
        >
          I Understand — Start {isPreQuiz ? 'Diagnostic' : 'Assessment'}
          <ArrowRight size={20} />
        </button>
      </div>
    </div>
  );
};

export default QuizInstructions;
