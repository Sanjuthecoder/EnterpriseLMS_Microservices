import React, { useEffect } from 'react';
import { CheckCircle, XCircle, X } from 'lucide-react';

const Toast = ({ message, type = 'success', onClose, duration = 4000 }) => {
  useEffect(() => {
    if (duration > 0) {
      const timer = setTimeout(() => {
        onClose();
      }, duration);
      return () => clearTimeout(timer);
    }
  }, [duration, onClose]);

  const bgColor = type === 'success' ? 'bg-emerald-50 border-emerald-200' : 'bg-red-50 border-red-200';
  const textColor = type === 'success' ? 'text-emerald-800' : 'text-red-800';
  const Icon = type === 'success' ? CheckCircle : XCircle;
  const iconColor = type === 'success' ? 'text-emerald-500' : 'text-red-500';

  return (
    <div className={`fixed bottom-4 right-4 flex items-center p-4 rounded-lg border shadow-lg ${bgColor} ${textColor} z-50 animate-fade-in-up`} style={{ minWidth: '300px', animation: 'fadeInUp 0.3s ease-out' }}>
      <style>{`
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(1rem); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
      <Icon className={`w-5 h-5 mr-3 ${iconColor}`} />
      <div className="flex-grow font-medium text-sm">
        {message}
      </div>
      <button onClick={onClose} className="ml-4 text-slate-400 hover:text-slate-600 transition-colors">
        <X className="w-4 h-4" />
      </button>
    </div>
  );
};

export default Toast;
