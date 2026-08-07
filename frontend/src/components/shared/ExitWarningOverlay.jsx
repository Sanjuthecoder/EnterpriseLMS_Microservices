import React from 'react';
import { AlertTriangle, ArrowLeft, LogOut } from 'lucide-react';

const ExitWarningOverlay = ({ onStay, onLeave }) => {
  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      background: 'rgba(0,0,0,0.75)',
      backdropFilter: 'blur(8px)',
      zIndex: 9999,
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      animation: 'fadeIn 0.25s ease-out'
    }}>
      <style>{`
        @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
        @keyframes slideUp { from { opacity: 0; transform: translateY(20px) scale(0.97); } to { opacity: 1; transform: translateY(0) scale(1); } }
        @keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.05); } }
      `}</style>
      <div style={{
        width: '100%',
        maxWidth: '480px',
        background: 'linear-gradient(145deg, #1e293b, #0f172a)',
        border: '1px solid rgba(239,68,68,0.3)',
        borderRadius: '20px',
        padding: '2.5rem 2rem',
        textAlign: 'center',
        boxShadow: '0 0 60px rgba(239,68,68,0.15), 0 25px 50px rgba(0,0,0,0.5)',
        animation: 'slideUp 0.3s ease-out'
      }}>
        <div style={{
          width: '72px', height: '72px', borderRadius: '50%',
          background: 'rgba(239,68,68,0.12)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          margin: '0 auto 1.5rem',
          animation: 'pulse 2s infinite'
        }}>
          <AlertTriangle size={36} color="#ef4444" />
        </div>

        <h2 style={{ color: '#f1f5f9', fontSize: '1.5rem', fontWeight: 700, marginBottom: '0.75rem' }}>
          Assessment In Progress!
        </h2>
        <p style={{ color: '#94a3b8', fontSize: '0.95rem', lineHeight: 1.6, marginBottom: '1.5rem' }}>
          You are <strong style={{ color: '#fca5a5' }}>not allowed to leave</strong> during an active assessment. Leaving now will result in:
        </p>

        <div style={{
          background: 'rgba(239,68,68,0.08)',
          border: '1px solid rgba(239,68,68,0.15)',
          borderRadius: '12px',
          padding: '1rem',
          marginBottom: '2rem',
          textAlign: 'left'
        }}>
          {[
            'Your attempt may be flagged for review',
            'Unanswered questions will be marked incorrect',
            'Time spent data will be recorded as suspicious',
          ].map((item, i) => (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: '0.5rem',
              padding: '0.4rem 0',
              color: '#fca5a5',
              fontSize: '0.85rem'
            }}>
              <span style={{ color: '#ef4444' }}>•</span> {item}
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button
            onClick={onStay}
            style={{
              flex: 2,
              padding: '0.85rem',
              borderRadius: '12px',
              border: 'none',
              background: 'linear-gradient(135deg, #2563eb, #3b82f6)',
              color: '#fff',
              fontSize: '0.95rem',
              fontWeight: 700,
              cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
              boxShadow: '0 4px 15px rgba(37,99,235,0.3)',
              fontFamily: 'var(--font-family)'
            }}
          >
            <ArrowLeft size={18} /> Return to Quiz
          </button>
          <button
            onClick={onLeave}
            style={{
              flex: 1,
              padding: '0.85rem',
              borderRadius: '12px',
              border: '1px solid rgba(239,68,68,0.3)',
              background: 'rgba(239,68,68,0.1)',
              color: '#fca5a5',
              fontSize: '0.85rem',
              fontWeight: 600,
              cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem',
              fontFamily: 'var(--font-family)'
            }}
          >
            <LogOut size={16} /> Leave
          </button>
        </div>
      </div>
    </div>
  );
};

export default ExitWarningOverlay;
