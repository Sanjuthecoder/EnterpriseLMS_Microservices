import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import Assessment from '../Assessment';
import api from '../../../services/api';
import aiApi from '../../../services/aiApi';
import * as AuthContext from '../../../contexts/AuthContext';
import * as ToastProvider from '../../../components/shared/ToastProvider';

// Mock the APIs
jest.mock('../../../services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn(),
    post: jest.fn()
  }
}));

jest.mock('../../../services/aiApi', () => ({
  __esModule: true,
  default: {
    getPreQuiz: jest.fn(),
    getPostQuiz: jest.fn(),
    submitPreQuiz: jest.fn(),
    submitPostQuiz: jest.fn()
  },
  isPremiumGateError: jest.fn(),
  getAiErrorMessage: jest.fn()
}));

const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useParams: () => ({ courseId: '123' }),
  useLocation: () => ({ pathname: '/employee/courses/123/pre-quiz' })
}));

const mockToast = {
  premium: jest.fn(),
  error: jest.fn(),
  success: jest.fn()
};

describe('Assessment Component - AI Integration', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    jest.spyOn(ToastProvider, 'useToast').mockReturnValue(mockToast);
  });

  const standardQuestions = [
    { questionId: 1, questionText: 'Standard Q1', options: ['A', 'B'], concept: 'Testing' }
  ];

  const aiQuestions = {
    questions: [
      { questionId: 'ai-1', questionText: 'AI Generated Q1', options: ['1', '2'], conceptTag: 'AI' }
    ]
  };

  it('uses standard api when user is FREE', async () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: { userId: 1, username: 'free_user', subscriptionTier: 'FREE' }
    });

    api.get.mockResolvedValueOnce({ data: standardQuestions });

    render(<BrowserRouter><Assessment /></BrowserRouter>);

    await waitFor(() => {
      expect(api.get).toHaveBeenCalledWith('/employees/courses/123/pre-quiz');
      expect(aiApi.getPreQuiz).not.toHaveBeenCalled();
    });
  });

  it('uses aiApi when user is PREMIUM', async () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: { userId: 1, username: 'premium_user', companyId: 99, subscriptionTier: 'PREMIUM' }
    });

    aiApi.getPreQuiz.mockResolvedValueOnce({ data: aiQuestions });

    render(<BrowserRouter><Assessment /></BrowserRouter>);

    await waitFor(() => {
      expect(aiApi.getPreQuiz).toHaveBeenCalledWith('123', 1, 99);
      expect(api.get).not.toHaveBeenCalled();
    });
  });

  it('shows premium toast and locks when aiApi returns premium error', async () => {
    jest.spyOn(AuthContext, 'useAuth').mockReturnValue({
      user: { userId: 1, username: 'premium_user', companyId: 99, subscriptionTier: 'PREMIUM' }
    });

    const error = new Error('Premium Required');
    aiApi.getPreQuiz.mockRejectedValueOnce(error);
    
    // Simulate isPremiumGateError returning true
    const { isPremiumGateError, getAiErrorMessage } = require('../../../services/aiApi');
    isPremiumGateError.mockReturnValue(true);
    getAiErrorMessage.mockReturnValue('Premium Access Required');

    render(<BrowserRouter><Assessment /></BrowserRouter>);

    await waitFor(() => {
      expect(mockToast.premium).toHaveBeenCalled();
      expect(screen.getByText(/Premium Access Required/i)).toBeInTheDocument();
    });
  });
});
