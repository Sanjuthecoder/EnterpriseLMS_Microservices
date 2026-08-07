import React, { useState, useCallback, useContext, createContext } from 'react';

// ============================================================
// Toast Context
// ============================================================
const ToastContext = createContext(null);

export const useToast = () => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used inside ToastProvider');
  return ctx;
};

// ============================================================
// Visual config per type
// ============================================================
const CONFIGS = {
  success: { bg: 'linear-gradient(135deg,#059669,#10b981)', icon: '✓', glow: '#10b981' },
  error:   { bg: 'linear-gradient(135deg,#dc2626,#ef4444)', icon: '✕', glow: '#ef4444' },
  warning: { bg: 'linear-gradient(135deg,#d97706,#f59e0b)', icon: '⚠', glow: '#f59e0b' },
  info:    { bg: 'linear-gradient(135deg,#2563eb,#3b82f6)', icon: 'ℹ', glow: '#3b82f6' },
  premium: { bg: 'linear-gradient(135deg,#7c3aed,#a855f7)', icon: '★', glow: '#a855f7' },
};

// ============================================================
// Single Toast Item
// ============================================================
const ToastItem = ({ toast, onRemove }) => {
  const cfg = CONFIGS[toast.type] || CONFIGS.info;
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    const t = setTimeout(() => setMounted(true), 20);
    return () => clearTimeout(t);
  }, []);

  return (
    <div
      role="alert"
      aria-live="polite"
      onClick={() => onRemove(toast.id)}
      style={{
        position: 'relative',
        display: 'flex',
        alignItems: 'flex-start',
        gap: '12px',
        padding: '14px 18px',
        borderRadius: '14px',
        background: cfg.bg,
        boxShadow: `0 8px 32px rgba(0,0,0,0.35), 0 0 0 1px ${cfg.glow}30`,
        color: 'white',
        maxWidth: '400px',
        width: '100%',
        cursor: 'pointer',
        overflow: 'hidden',
        transform: mounted ? 'translateX(0) scale(1)' : 'translateX(120%) scale(0.9)',
        opacity: mounted ? 1 : 0,
        transition: 'transform 0.35s cubic-bezier(0.34,1.56,0.64,1), opacity 0.35s ease',
      }}
    >
      {/* Icon badge */}
      <div style={{
        width: 30, height: 30, borderRadius: '50%',
        background: 'rgba(255,255,255,0.2)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontWeight: 700, fontSize: 14, flexShrink: 0, marginTop: 1,
      }}>
        {cfg.icon}
      </div>

      {/* Text */}
      <div style={{ flex: 1 }}>
        {toast.title && (
          <div style={{ fontWeight: 700, fontSize: '0.88rem', marginBottom: 3 }}>
            {toast.title}
          </div>
        )}
        <div style={{ fontSize: '0.83rem', opacity: 0.95, lineHeight: 1.45 }}>
          {toast.message}
        </div>
        {toast.action && (
          <button
            onClick={e => { e.stopPropagation(); toast.action.onClick(); onRemove(toast.id); }}
            style={{
              marginTop: 8, padding: '4px 12px',
              background: 'rgba(255,255,255,0.25)',
              border: '1px solid rgba(255,255,255,0.3)',
              borderRadius: 6, color: 'white', cursor: 'pointer',
              fontSize: '0.78rem', fontWeight: 600,
            }}
          >
            {toast.action.label}
          </button>
        )}
      </div>

      {/* Progress bar */}
      <div style={{
        position: 'absolute', bottom: 0, left: 0, height: 3,
        background: 'rgba(255,255,255,0.35)',
        borderRadius: '0 0 14px 14px',
        animation: `toastProgress ${toast.duration}ms linear forwards`,
        width: '100%',
      }} />

      <style>{`@keyframes toastProgress{from{width:100%}to{width:0}}`}</style>
    </div>
  );
};

// ============================================================
// Provider
// ============================================================
let _id = 0;

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback(id => setToasts(p => p.filter(t => t.id !== id)), []);

  const show = useCallback(({ type = 'info', title, message, duration = 4500, action }) => {
    const id = ++_id;
    setToasts(p => [...p, { id, type, title, message, duration, action }]);
    setTimeout(() => remove(id), duration);
    return id;
  }, [remove]);

  const toast = {
    success: (msg, opts = {}) => show({ type: 'success', message: msg, ...opts }),
    error:   (msg, opts = {}) => show({ type: 'error',   message: msg, ...opts }),
    warning: (msg, opts = {}) => show({ type: 'warning', message: msg, ...opts }),
    info:    (msg, opts = {}) => show({ type: 'info',    message: msg, ...opts }),
    premium: (msg, opts = {}) => show({ type: 'premium', message: msg, ...opts }),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div style={{
        position: 'fixed', top: 24, right: 24, zIndex: 99999,
        display: 'flex', flexDirection: 'column', gap: 10,
        pointerEvents: 'none',
      }}>
        {toasts.map(t => (
          <div key={t.id} style={{ pointerEvents: 'all' }}>
            <ToastItem toast={t} onRemove={remove} />
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};

export default ToastProvider;
